package com.naeem.streamnest

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class DownloadRepository(
    private val context: Context, private val engine: MediaEngine,
    private val preferences: Preferences, scope: CoroutineScope, databaseName: String = "transfers.db"
) {
    private val store = JobStore(context, databaseName)
    private val storage = MediaStorage(context, preferences)
    private val lock = Any()
    private val execution = Mutex()
    private val mutable = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs = mutable.asStateFlow()
    @Volatile private var active: Pair<String, Job>? = null
    private val initialized = scope.async(Dispatchers.IO) {
        val recovered = store.all().map { job ->
            if (!job.status.active) job else {
                val published = job.outputUri.isNotBlank() && runCatching {
                    context.contentResolver.query(Uri.parse(job.outputUri), arrayOf(MediaStore.MediaColumns.IS_PENDING), null, null, null)?.use {
                        it.moveToFirst() && it.getInt(0) == 0
                    } == true
                }.getOrDefault(false)
                if (published) job.copy(status = JobStatus.COMPLETED, progress = 1f, detail = "Saved to Downloads/StreamNest")
                else {
                    if (job.outputUri.isNotBlank()) runCatching { context.contentResolver.delete(Uri.parse(job.outputUri), null, null) }
                    job.copy(status = JobStatus.INTERRUPTED, detail = "Interrupted. Tap retry to recover this download.", outputUri = "")
                }
            }
        }
        synchronized(lock) { recovered.forEach(store::save); mutable.value = recovered }
    }
    suspend fun awaitReady() { initialized.await() }
    fun close() { store.close() }
    suspend fun enqueue(items: List<Pair<MediaItem, DownloadChoice>>): Int = withContext(Dispatchers.IO) {
        initialized.await()
        synchronized(lock) {
            var added = 0
            val current = mutable.value.toMutableList()
            items.filter { it.first.available }.forEach { (media, choice) ->
                if (current.none { shouldDeduplicate(it, media, choice) }) {
                    val job = DownloadJob(media = media, choice = choice)
                    store.save(job); current.add(0, job); added++
                }
            }
            mutable.value = current
            added
        }
    }
    private fun change(id: String, transform: (DownloadJob) -> DownloadJob) = synchronized(lock) {
        mutable.value = mutable.value.map {
            if (it.id == id) transform(it).also(store::save) else it
        }
    }
    suspend fun retry(id: String) = withContext(Dispatchers.IO) {
        initialized.await()
        change(id) { if (it.status.active || it.status == JobStatus.COMPLETED) it else it.copy(status = JobStatus.QUEUED, detail = "Waiting to start", progress = 0f, outputUri = "") }
    }
    suspend fun cancel(id: String) = withContext(Dispatchers.IO) {
        initialized.await()
        change(id) { if (it.status == JobStatus.COMPLETED) it else it.copy(status = JobStatus.CANCELLED, detail = "Cancelled. You can retry anytime.") }
        active?.takeIf { it.first == id }?.second?.cancel()
        engine.cancel(id)
    }
    suspend fun cancelAll() { jobs.value.filter { it.status.active || it.status == JobStatus.QUEUED }.forEach { cancel(it.id) } }
    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        initialized.await()
        synchronized(lock) {
            val job = mutable.value.find { it.id == id } ?: return@synchronized
            if (job.status.active || job.status == JobStatus.QUEUED) return@synchronized
            store.remove(id); mutable.value = mutable.value.filterNot { it.id == id }
            File(context.filesDir, "transfers/$id").deleteRecursively()
        }
    }
    fun networkAllowed(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            (!preferences.state.value.wifiOnly || (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !manager.isActiveNetworkMetered))
    }
    suspend fun runQueue(onUpdate: (DownloadJob) -> Unit): Boolean = execution.withLock {
        initialized.await()
        while (true) {
            currentCoroutineContext().ensureActive()
            val next = jobs.value.lastOrNull { it.status == JobStatus.QUEUED } ?: return@withLock false
            if (!networkAllowed()) {
                change(next.id) { it.copy(detail = if (preferences.state.value.wifiOnly) "Waiting for unmetered Wi-Fi" else "Waiting for a connection") }
                return@withLock true
            }
            supervisorScope {
                val task = async(start = CoroutineStart.LAZY) { execute(next, onUpdate) }
                active = next.id to task
                if (jobs.value.find { it.id == next.id }?.status != JobStatus.QUEUED) task.cancel()
                try { task.await() } catch (e: CancellationException) { currentCoroutineContext().ensureActive() }
                finally { active = null }
            }
        }
        @Suppress("UNREACHABLE_CODE") false
    }
    private suspend fun execute(job: DownloadJob, onUpdate: (DownloadJob) -> Unit) {
        val directory = File(context.filesDir, "transfers/${job.id}")
        var pendingUri: Uri? = null
        fun report(status: JobStatus, progress: Float = 0f, detail: String) {
            change(job.id) { if (it.status == JobStatus.CANCELLED) it else it.copy(status = status, progress = progress, detail = detail) }
            jobs.value.find { it.id == job.id }?.let(onUpdate)
        }
        try {
            currentCoroutineContext().ensureActive()
            report(JobStatus.RESOLVING, detail = "Finding the best available format")
            require(StatFs(context.filesDir.absolutePath).availableBytes > 150L * 1024 * 1024) {
                "Storage is almost full. Free some space and retry."
            }
            var lastUpdate = 0L
            var networkChanged = false
            val file = engine.download(job, directory) { progress, detail ->
                if (jobs.value.find { it.id == job.id }?.status == JobStatus.CANCELLED) engine.cancel(job.id)
                else if (!networkAllowed()) {
                    engine.cancel(job.id)
                    networkChanged = true
                }
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 800 || progress >= 1f) {
                    report(if (progress >= 1f) JobStatus.MERGING else JobStatus.DOWNLOADING, progress, if (progress >= 1f) "Combining and verifying media" else detail)
                    lastUpdate = now
                }
            }
            currentCoroutineContext().ensureActive()
            check(!networkChanged) { "Connection changed. Reconnect to your allowed network and retry." }
            storage.validate(file, job.choice.audioOnly)
            require(StatFs(context.filesDir.absolutePath).availableBytes > file.length() + 10L * 1024 * 1024) {
                "More free space is needed to save the finished file. Free space and retry."
            }
            report(JobStatus.SAVING, 1f, "Saving to Downloads/StreamNest")
            val uri = storage.publish(file, job) { pending ->
                pendingUri = pending
                change(job.id) { it.copy(outputUri = pending.toString(), bytes = file.length()) }
            }
            // Once publication commits, persist success even if a late cancel arrives.
            withContext(NonCancellable + Dispatchers.IO) {
                change(job.id) { it.copy(status = JobStatus.COMPLETED, progress = 1f, detail = "Saved to Downloads/StreamNest", outputUri = uri.toString(), bytes = file.length()) }
                directory.deleteRecursively()
                jobs.value.find { it.id == job.id }?.let(onUpdate)
            }
        } catch (e: CancellationException) {
            engine.cancel(job.id)
            withContext(NonCancellable + Dispatchers.IO) {
                pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                change(job.id) { if (it.status == JobStatus.CANCELLED) it.copy(outputUri = "") else it.copy(status = JobStatus.INTERRUPTED, detail = "Interrupted. Tap retry to continue.", outputUri = "") }
            }
            throw e
        } catch (e: Exception) {
            pendingUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            change(job.id) { if (it.status == JobStatus.CANCELLED) it.copy(outputUri = "") else it.copy(status = JobStatus.FAILED, detail = friendlyError(e), outputUri = "") }
            jobs.value.find { it.id == job.id }?.let(onUpdate)
        }
    }
}
fun friendlyError(error: Throwable): String {
    val text = error.message.orEmpty()
    return when {
        text.contains("403") || text.contains("Sign in", true) || text.contains("bot", true) -> "YouTube declined this request. Try again later or try another public video."
        text.contains("format", true) && text.contains("available", true) -> "This format is unavailable. Try a different quality."
        text.contains("private", true) || text.contains("unavailable", true) -> "This video is unavailable or needs account access."
        text.contains("space", true) || text.contains("ENOSPC", true) -> "Not enough storage. Free space and retry."
        text.contains("timed out", true) || text.contains("resolve", true) || text.contains("network", true) -> "Connection interrupted. Check your network and retry."
        text.contains("runtime", true) || text.contains("JavaScript", true) -> "The media engine needs a compatible runtime. See the engine information in Settings."
        else -> text.lineSequence().firstOrNull { it.startsWith("ERROR:") }?.removePrefix("ERROR:")?.trim()?.take(220)
            ?: text.take(220).ifBlank { "The operation could not finish. Please retry." }
    }
}
