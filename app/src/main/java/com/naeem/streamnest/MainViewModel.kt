package com.naeem.streamnest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class HomeState(
    val url: String = "", val busy: Boolean = false, val error: String? = null,
    val result: MediaResult? = null, val selected: Set<String> = emptySet(),
    val choices: Map<String, DownloadChoice> = emptyMap(), val singleOnly: Boolean = false,
    val limit: Int = 100, val formatItem: MediaItem? = null, val formatBusy: Boolean = false,
    val formatError: String? = null, val enqueueing: Boolean = false, val updating: Boolean = false,
    val engineVersion: String = "Bundled engine"
)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StreamNestApp
    val jobs = app.downloads.jobs
    val preferences = app.preferences.state
    private val mutable = MutableStateFlow(HomeState(engineVersion = app.getSharedPreferences("engine", Application.MODE_PRIVATE).getString("version", "2026.08.19 · bundled") ?: "2026.08.19 · bundled"))
    val home = mutable.asStateFlow()
    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()
    private var lookup: Job? = null
    private var formatLookup: Job? = null
    fun url(value: String) { mutable.update { it.copy(url = value, error = null) } }
    fun setSingle(value: Boolean) { mutable.update { it.copy(singleOnly = value) } }
    fun shared(value: String) { url(value) }
    fun inspect(more: Boolean = false) {
        val normalized = runCatching { SourceUrl.normalize(home.value.url) }.getOrElse { error ->
            mutable.update { it.copy(error = error.message) }; return
        }
        lookup?.cancel()
        val single = home.value.singleOnly
        val limit = if (more) home.value.limit + 100 else 100
        lookup = viewModelScope.launch {
            mutable.update { it.copy(busy = true, error = null, limit = limit) }
            try {
                val result = app.engine.inspect(normalized, limit, single)
                ensureActive()
                mutable.update { old -> old.copy(
                    busy = false, result = result, error = if (result.items.isEmpty()) "No videos were found at this link." else null,
                    selected = if (more) old.selected else result.items.filter { it.available }.map { it.id }.toSet(),
                    choices = if (more) old.choices else emptyMap()
                ) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("StreamNest", "Metadata lookup failed", e)
                mutable.update { it.copy(busy = false, error = friendlyError(e)) }
            }
        }
    }
    fun cancelLookup() { lookup?.cancel(); mutable.update { it.copy(busy = false) } }
    fun select(id: String) { mutable.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) } }
    fun selectAll() { mutable.update { old ->
        val all = old.result?.items.orEmpty().filter { it.available }.map { it.id }.toSet()
        old.copy(selected = if (old.selected == all) emptySet() else all)
    } }
    fun choice(item: MediaItem) = home.value.choices[item.id] ?: DownloadChoice(preferences.value.defaultHeight)
    fun setChoice(id: String?, choice: DownloadChoice) {
        mutable.update { old ->
            val ids = if (id == null) old.selected else setOf(id)
            old.copy(choices = old.choices + ids.associateWith { choice })
        }
    }
    fun showFormats(item: MediaItem) {
        formatLookup?.cancel()
        mutable.update { it.copy(formatItem = item, formatBusy = true, formatError = null) }
        formatLookup = viewModelScope.launch {
            try {
                val resolved = app.engine.formats(item)
                ensureActive()
                mutable.update { it.copy(formatItem = resolved, formatBusy = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(formatBusy = false, formatError = friendlyError(e)) } }
        }
    }
    fun closeFormats() { formatLookup?.cancel(); mutable.update { it.copy(formatItem = null, formatBusy = false, formatError = null) } }
    fun enqueue(onAdded: () -> Unit) {
        if (home.value.enqueueing) return
        val snapshot = home.value
        val selected = snapshot.result?.items.orEmpty().filter { it.id in snapshot.selected && it.available }
        if (selected.isEmpty()) return
        val requests = selected.map { it to (snapshot.choices[it.id] ?: DownloadChoice(preferences.value.defaultHeight)) }
        viewModelScope.launch {
            mutable.update { it.copy(enqueueing = true) }
            try {
                val added = app.downloads.enqueue(requests)
                if (added > 0) {
                    runCatching { TransferScheduler.start(app) }.onFailure { messagesChannel.send(it.message ?: "Tap Resume queue to start.") }
                    messagesChannel.send("$added ${if (added == 1) "download" else "downloads"} added to your queue")
                    onAdded()
                } else messagesChannel.send("These downloads are already queued or saved.")
            } catch (e: Exception) { messagesChannel.send(friendlyError(e)) }
            finally { mutable.update { it.copy(enqueueing = false) } }
        }
    }
    fun cancel(id: String) { viewModelScope.launch { app.downloads.cancel(id) } }
    fun retry(id: String) { viewModelScope.launch {
        app.downloads.retry(id)
        runCatching { TransferScheduler.start(app) }.onFailure { messagesChannel.send(it.message.orEmpty()) }
    } }
    fun resumeQueue() { viewModelScope.launch {
        runCatching { TransferScheduler.start(app) }.onFailure { messagesChannel.send(it.message.orEmpty()) }
    } }
    fun remove(id: String) { viewModelScope.launch { app.downloads.remove(id); messagesChannel.send("Removed from history. Saved files are kept.") } }
    fun preferences(value: UserPreferences) { app.preferences.update(value) }
    fun updateEngine() {
        if (home.value.updating || home.value.busy || jobs.value.any { it.status.active }) {
            viewModelScope.launch { messagesChannel.send("Wait for the current operation to finish before updating.") }
            return
        }
        viewModelScope.launch {
            mutable.update { it.copy(updating = true) }
            try {
                val version = app.engine.update()
                mutable.update { it.copy(engineVersion = version) }
                messagesChannel.send("Media engine is up to date")
            } catch (e: Exception) { messagesChannel.send(friendlyError(e)) }
            finally { mutable.update { it.copy(updating = false) } }
        }
    }
}
