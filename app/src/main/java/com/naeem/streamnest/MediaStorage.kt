package com.naeem.streamnest

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

import androidx.documentfile.provider.DocumentFile

class MediaStorage(private val context: Context, private val preferences: Preferences? = null) {
    fun validate(file: File, audioOnly: Boolean) {
        require(file.length() > 0) { "The downloaded file is empty." }
        val reader = MediaMetadataRetriever()
        try {
            reader.setDataSource(file.absolutePath)
            val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            require(duration > 0) { "The downloaded media could not be verified." }
            if (!audioOnly) require(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") {
                "The downloaded file does not contain a video track."
            }
        } finally { reader.release() }
    }
    suspend fun publish(file: File, job: DownloadJob, onPending: suspend (Uri) -> Unit): Uri {
        val resolver = context.contentResolver
        val name = "${safeFileStem(job.media.title)} [${safeFileStem(job.media.id)}] ${job.id.take(6)}.${file.extension}"
        val mime = if (job.choice.audioOnly) "audio/mp4" else "video/mp4"
        val customTree = preferences?.state?.value?.customTreeUri.orEmpty()
        if (customTree.isNotBlank()) {
            val treeUri = Uri.parse(customTree)
            val dir = DocumentFile.fromTreeUri(context, treeUri)
            if (dir != null && dir.canWrite()) {
                val created = dir.createFile(mime, name) ?: error("Android could not create the file in the selected folder.")
                val docUri = created.uri
                try {
                    onPending(docUri)
                    resolver.openOutputStream(docUri, "w")?.use { output ->
                        file.inputStream().use { input ->
                            val buffer = ByteArray(128 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                            }
                        }
                    } ?: error("The selected download folder is not writable.")
                    return docUri
                } catch (e: Throwable) {
                    runCatching { created.delete() }
                    throw e
                }
            }
        }
        val folder = preferences?.state?.value?.downloadFolder?.ifBlank { "NaeemVideos" } ?: "NaeemVideos"
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: error("Android could not create a file in Downloads.")
        try {
            onPending(uri)
            resolver.openOutputStream(uri, "w")?.use { output ->
                file.inputStream().use { input ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            } ?: error("The Downloads folder is not writable.")
            currentCoroutineContext().ensureActive()
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) > 0) {
                "Android could not finish saving the file."
            }
            return uri
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }
}
