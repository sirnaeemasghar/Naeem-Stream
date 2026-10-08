package com.naeem.streamnest

import java.net.URI
import java.util.Locale
import java.util.UUID

object SourceUrl {
    fun normalize(input: String): String {
        val text = input.trim()
        val candidate = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(text)?.value
            ?: if (text.startsWith("www.") || (text.contains(".") && !text.contains(" ") && !text.startsWith("file:"))) "https://$text" else text
        val uri = runCatching { URI(candidate) }.getOrNull()
            ?: throw IllegalArgumentException("Paste a valid video or social media link.")
        val host = uri.host?.lowercase(Locale.ROOT)
        require(!host.isNullOrBlank() && host.contains(".")) {
            "Paste a valid web link with a domain name."
        }
        require(uri.scheme in listOf("https", "http") && uri.userInfo == null && (uri.port == -1 || uri.port == 443 || uri.port == 80)) {
            "Use a standard web link."
        }
        require(!uri.path.isNullOrBlank() && uri.path != "/") { "Paste a link to a video, reel, post, or collection." }
        return URI("https://${uri.rawAuthority}${uri.rawPath}${uri.rawQuery?.let { "?$it" }.orEmpty()}").toASCIIString()
    }
}

data class MediaItem(
    val id: String, val url: String, val title: String,
    val thumbnail: String = "", val creator: String = "YouTube", val duration: Long = 0,
    val heights: List<Int> = emptyList(), val available: Boolean = true
)
data class MediaResult(val title: String, val items: List<MediaItem>, val mayHaveMore: Boolean = false)
data class DownloadChoice(val height: Int = 720, val audioOnly: Boolean = false) {
    val label: String get() = if (audioOnly) "Audio · M4A" else "Up to ${height}p"
}
enum class JobStatus {
    QUEUED, RESOLVING, DOWNLOADING, MERGING, SAVING, COMPLETED, FAILED, CANCELLED, INTERRUPTED;
    val active: Boolean get() = this in setOf(RESOLVING, DOWNLOADING, MERGING, SAVING)
    val label: String get() = when (this) {
        QUEUED -> "Queued"; RESOLVING -> "Preparing"; DOWNLOADING -> "Downloading"
        MERGING -> "Finishing media"; SAVING -> "Saving to Downloads"; COMPLETED -> "Saved"
        FAILED -> "Needs attention"; CANCELLED -> "Cancelled"; INTERRUPTED -> "Interrupted"
    }
}
data class DownloadJob(
    val id: String = UUID.randomUUID().toString(), val media: MediaItem,
    val choice: DownloadChoice = DownloadChoice(), val status: JobStatus = JobStatus.QUEUED,
    val progress: Float = 0f, val detail: String = "Waiting to start", val outputUri: String = "",
    val bytes: Long = 0, val createdAt: Long = System.currentTimeMillis()
)
fun shouldDeduplicate(existing: DownloadJob, media: MediaItem, choice: DownloadChoice): Boolean =
    existing.media.id == media.id && existing.choice == choice &&
        (existing.status.active || existing.status == JobStatus.QUEUED || existing.status == JobStatus.COMPLETED)
fun safeFileStem(title: String): String = title.replace(Regex("[\\p{Cntrl}/\\\\:*?\"<>|]"), "_").trim().take(100).ifBlank { "Video" }
fun prettyBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    else -> "${bytes / 1024} KB"
}
fun durationLabel(seconds: Long): String = if (seconds <= 0) "" else if (seconds >= 3600)
    String.format(Locale.US, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    else String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
