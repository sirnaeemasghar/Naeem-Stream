package com.naeem.streamnest

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DeviceIntegrationTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun durableQueueDeduplicatesAndRecoversInterruptedWork() = runBlocking {
        val database = "test-${UUID.randomUUID()}.db"
        val item = MediaItem("test", "https://youtu.be/test", "Test item")
        val interrupted = DownloadJob(media = item, status = JobStatus.DOWNLOADING)
        JobStore(context, database).use { it.save(interrupted) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DownloadRepository(context, FailingEngine(), Preferences(context), scope, database)
        try {
            repository.awaitReady()
            assertEquals(JobStatus.INTERRUPTED, repository.jobs.value.single().status)
            val request = item.copy(id = "second") to DownloadChoice(720)
            assertEquals(1, repository.enqueue(listOf(request, request)))
            assertEquals(0, repository.enqueue(listOf(request)))
            val job = repository.jobs.value.first { it.media.id == "second" }
            repository.cancel(job.id)
            assertEquals(JobStatus.CANCELLED, repository.jobs.value.first { it.id == job.id }.status)
            repository.retry(job.id)
            assertEquals(JobStatus.QUEUED, repository.jobs.value.first { it.id == job.id }.status)
            repository.close()
            JobStore(context, database).use { assertEquals(2, it.all().size) }
        } finally { scope.cancel(); repository.close(); context.deleteDatabase(database) }
    }

    @Test fun engineFailureNeverBecomesCompleted() = runBlocking {
        val database = "test-${UUID.randomUUID()}.db"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repository = DownloadRepository(context, FailingEngine(), Preferences(context), scope, database)
        try {
            repository.awaitReady()
            assertTrue("Network required for this queue integration test", repository.networkAllowed())
            repository.enqueue(listOf(MediaItem("failure", "https://youtu.be/failure", "Failure test") to DownloadChoice()))
            repository.runQueue { }
            val job = repository.jobs.value.single()
            assertEquals(JobStatus.FAILED, job.status)
            assertEquals("", job.outputUri)
            assertFalse(job.detail.contains("Saved"))
        } finally { scope.cancel(); repository.close(); context.deleteDatabase(database) }
    }

    @Test fun nativeFfmpegProducesMediaAndMediaStorePublishesIt() = runBlocking(Dispatchers.IO) {
        YoutubeDL.init(context)
        FFmpeg.getInstance().init(context)
        val directory = File(context.cacheDir, "test-${UUID.randomUUID()}").apply { mkdirs() }
        var published: Uri? = null
        try {
            val file = File(directory, "fixture.mp4")
            val binaryDir = context.applicationInfo.nativeLibraryDir
            val packages = File(context.noBackupFilesDir, "youtubedl-android/packages")
            val process = ProcessBuilder(
                "$binaryDir/libffmpeg.so", "-y", "-f", "lavfi", "-i", "color=c=green:s=320x240:d=1",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-c:v", "mpeg4", "-c:a", "aac", "-shortest", file.absolutePath
            ).redirectErrorStream(true).apply {
                environment()["LD_LIBRARY_PATH"] = "${packages.absolutePath}/ffmpeg/usr/lib:${packages.absolutePath}/python/usr/lib"
            }.start()
            val log = process.inputStream.bufferedReader().readText()
            assertEquals(log, 0, process.waitFor())
            val storage = MediaStorage(context)
            storage.validate(file, false)
            val job = DownloadJob(media = MediaItem("fixture", "https://youtu.be/fixture", "StreamNest device test"))
            published = storage.publish(file, job) { }
            context.contentResolver.query(published!!, arrayOf(MediaStore.MediaColumns.IS_PENDING, MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.SIZE), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
                assertEquals("Download/StreamNest/", cursor.getString(1))
                assertEquals(file.length(), cursor.getLong(2))
            }
            val reader = MediaMetadataRetriever()
            try {
                reader.setDataSource(context, published)
                assertTrue(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() > 0)
            } finally { reader.release() }
        } finally {
            published?.let { context.contentResolver.delete(it, null, null) }
            directory.deleteRecursively()
        }
    }

    @Test fun liveYoutubeMetadataDiagnostic() = runBlocking {
        val url = InstrumentationRegistry.getArguments().getString("liveUrl") ?: return@runBlocking
        val result = YtDlpEngine(context).inspect(url, single = true)
        assertTrue(result.items.isNotEmpty())
    }

    private class FailingEngine : MediaEngine {
        override suspend fun inspect(url: String, limit: Int, single: Boolean) = error("Not used")
        override suspend fun formats(item: MediaItem) = error("Not used")
        override suspend fun download(job: DownloadJob, directory: File, onProgress: (Float, String) -> Unit): File = error("Deliberate test failure")
        override fun cancel(id: String) = Unit
    }
}
