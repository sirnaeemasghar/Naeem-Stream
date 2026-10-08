package com.naeem.streamnest

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.UUID

interface MediaEngine {
    suspend fun inspect(url: String, limit: Int = 100, single: Boolean = false): MediaResult
    suspend fun formats(item: MediaItem): MediaItem
    suspend fun download(job: DownloadJob, directory: File, onProgress: (Float, String) -> Unit): File
    fun cancel(id: String)
}

class YtDlpEngine(private val context: Context) : MediaEngine {
    private val operations = Mutex()
    private suspend fun <T> exclusive(block: suspend () -> T): T = operations.withLock { withContext(Dispatchers.IO) { block() } }
    private val initLock = Any()
    @Volatile private var initialized = false
    private fun ready() = synchronized(initLock) {
        if (!initialized) {
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            ensureBundledEngine()
            initialized = true
        }
    }
    private fun ensureBundledEngine() {
        runCatching {
            val dir = File(context.noBackupFilesDir, "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}")
            val target = File(dir, YoutubeDL.ytdlpBin)
            val prefs = context.getSharedPreferences("engine", Context.MODE_PRIVATE)
            val currentVersion = prefs.getString("version", "") ?: ""
            val bundledVersion = "2026.08.19"
            if (!target.exists() || currentVersion < bundledVersion) {
                dir.mkdirs()
                val temp = File.createTempFile("ytdlp-bundle-", ".tmp", dir)
                context.resources.openRawResource(R.raw.ytdlp).use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                temp.setExecutable(true, false)
                target.delete()
                if (temp.renameTo(target)) {
                    target.setExecutable(true, false)
                    prefs.edit().putString("version", bundledVersion).apply()
                } else {
                    temp.delete()
                }
            } else if (!target.canExecute()) {
                target.setExecutable(true, false)
            }
        }
    }
    private fun request(url: String) = YoutubeDLRequest(url).apply {
        addOption("--ignore-config")
        addOption("--no-warnings")
        addOption("--socket-timeout", "25")
        addOption("--retries", "3")
        addOption("--extractor-retries", "2")
        addOption("--extractor-args", "youtube:player_client=tv_downgraded,android,web")
    }
    override suspend fun inspect(url: String, limit: Int, single: Boolean): MediaResult = exclusive {
        ready()
        val normalized = SourceUrl.normalize(url)
        val isCollection = normalized.contains("list=") || normalized.contains("/channel/") || normalized.contains("/c/") || normalized.contains("/user/")
        val req = request(normalized).apply {
            addOption("--dump-single-json")
            addOption("--skip-download")
            if (isCollection && !single) {
                addOption("--flat-playlist")
                addOption("--playlist-end", limit.toString())
            } else {
                addOption("--no-playlist")
            }
        }
        val json = JSONObject(runInterruptible { YoutubeDL.getInstance().execute(req, redirectErrorStream = false).out })
        val entries = json.optJSONArray("entries")
        if (entries == null) MediaResult(json.optString("title", "Video"), listOf(parseItem(json)))
        else {
            val items = LinkedHashMap<String, MediaItem>()
            fun visit(node: JSONObject) {
                val children = node.optJSONArray("entries")
                if (children != null) {
                    for (i in 0 until children.length()) children.optJSONObject(i)?.let(::visit)
                } else if (node.optString("_type") !in listOf("playlist", "multi_video")) {
                    val item = parseItem(node)
                    if (item.id.isNotBlank()) items[item.id] = item
                }
            }
            visit(json)
            MediaResult(json.optString("title", "Your collection"), items.values.toList(), entries.length() >= limit)
        }
    }
    override suspend fun formats(item: MediaItem): MediaItem = exclusive {
        ready()
        val req = request(item.url).apply { addOption("--dump-single-json"); addOption("--skip-download"); addOption("--no-playlist") }
        parseItem(JSONObject(runInterruptible { YoutubeDL.getInstance().execute(req, redirectErrorStream = false).out }))
    }
    private fun parseItem(json: JSONObject): MediaItem {
        val id = json.optString("id", "")
        val rawUrl = json.optString("webpage_url").ifBlank { json.optString("url").ifBlank { json.optString("original_url") } }
        val url = if (rawUrl.startsWith("https://") || rawUrl.startsWith("http://")) rawUrl
            else if (id.isNotBlank()) "https://www.youtube.com/watch?v=$id"
            else ""
        val thumbnails = json.optJSONArray("thumbnails")
        val thumbnail = json.optString("thumbnail").ifBlank {
            if (thumbnails != null && thumbnails.length() > 0) thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url").orEmpty() else ""
        }
        val formats = json.optJSONArray("formats")
        val heights = buildSet {
            if (formats != null) for (i in 0 until formats.length()) {
                val fmt = formats.optJSONObject(i) ?: continue
                var h = fmt.optInt("height", 0)
                if (h <= 0) {
                    val res = fmt.optString("resolution")
                    val match = Regex("(\\d+)x(\\d+)").find(res)
                    if (match != null) {
                        val w = match.groupValues[1].toIntOrNull() ?: 0
                        val hVal = match.groupValues[2].toIntOrNull() ?: 0
                        h = minOf(w, hVal).takeIf { it > 0 } ?: maxOf(w, hVal)
                    }
                }
                if (h <= 0) {
                    val note = fmt.optString("format_note")
                    h = Regex("(\\d{3,4})p?").find(note)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                }
                if (h > 0 && fmt.optString("vcodec") != "none") add(h)
            }
            val rootH = json.optInt("height", 0)
            if (rootH > 0) add(rootH)
        }.sortedDescending()
        val title = json.optString("title", "Media")
        val creator = json.optString("uploader").ifBlank {
            json.optString("channel").ifBlank {
                json.optString("extractor_key").ifBlank { "Naeem Videos" }
            }
        }
        return MediaItem(id, url, title, thumbnail, creator,
            json.optDouble("duration", 0.0).takeIf { it.isFinite() }?.toLong() ?: 0L, heights,
            json.optString("availability") !in listOf("private", "premium_only", "subscriber_only") &&
                title !in listOf("[Private video]", "[Deleted video]"))
    }
    override suspend fun download(job: DownloadJob, directory: File, onProgress: (Float, String) -> Unit): File = exclusive {
        ready()
        check(directory.exists() || directory.mkdirs()) { "Could not create temporary download storage." }
        val height = job.choice.height.coerceIn(144, 4320)
        val req = request(job.media.url).apply {
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--newline")
            addOption("--continue")
            addOption("--no-overwrites")
            addOption("--fragment-retries", "3")
            addOption("--concurrent-fragments", "2")
            addOption("-o", File(directory, "media.%(ext)s").absolutePath)
            if (job.choice.audioOnly) {
                addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                addOption("-x")
                addOption("--audio-format", "m4a")
            } else {
                addOption("-f", "bestvideo[height<=$height][ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/bestvideo[height<=$height][ext=mp4]+bestaudio[ext=m4a]/best[height<=$height][ext=mp4]/bestvideo[height<=$height]+bestaudio/best[height<=$height]/bestvideo+bestaudio/best")
                addOption("--merge-output-format", "mp4")
                addOption("--remux-video", "mp4")
            }
        }
        currentCoroutineContext().ensureActive()
        runInterruptible { YoutubeDL.getInstance().execute(req, job.id, redirectErrorStream = false) { progress, eta, line ->
            val percentage = (progress / 100f).coerceIn(0f, 1f)
            val speed = Regex("at\\s+([^\\s]+/s)").find(line)?.groupValues?.getOrNull(1).orEmpty()
            val remaining = if (eta > 0) "${durationLabel(eta)} remaining" else ""
            onProgress(percentage, listOf(speed, remaining).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Receiving media" })
        }
        }
        directory.listFiles()?.firstOrNull { it.isFile && it.extension == (if (job.choice.audioOnly) "m4a" else "mp4") && it.length() > 0 }
            ?: error("The download finished without a valid media file. Please retry.")
    }
    suspend fun update(): String = exclusive {
        ready()
        VerifiedEngineUpdate.install(context)
    }
    override fun cancel(id: String) { runCatching { YoutubeDL.getInstance().destroyProcessById(id) } }
}
