package com.naeem.streamnest

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Installs only a checksum-verified official release, with an atomic replacement. */
object VerifiedEngineUpdate {
    suspend fun install(context: Context): String = runInterruptible(Dispatchers.IO) {
        fun connection(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000; readTimeout = 30_000
            setRequestProperty("User-Agent", "StreamNest/0.1 Android")
        }
        val api = connection("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
        val release = try {
            check(api.responseCode == 200) { "Could not check engine updates. Try again later." }
            JSONObject(api.inputStream.bufferedReader().use { it.readText() })
        } finally { api.disconnect() }
        val assets = release.getJSONArray("assets")
        val assetList = (0 until assets.length()).map { assets.getJSONObject(it) }
        val asset = assetList.first { it.getString("name") == "yt-dlp" }
        val version = release.getString("tag_name")
        val downloadUrl = asset.getString("browser_download_url")
        check(downloadUrl.startsWith("https://github.com/yt-dlp/yt-dlp/releases/download/")) { "Unexpected engine download source." }
        var expected = asset.optString("digest").removePrefix("sha256:")
        if (!expected.matches(Regex("[a-fA-F0-9]{64}"))) {
            val sumsAsset = assetList.firstOrNull { it.getString("name") == "SHA2-256SUMS" }
            if (sumsAsset != null) {
                val sumsConn = connection(sumsAsset.getString("browser_download_url"))
                try {
                    if (sumsConn.responseCode == 200) {
                        val text = sumsConn.inputStream.bufferedReader().use { it.readText() }
                        val match = Regex("([a-fA-F0-9]{64})\\s+yt-dlp(\\s|$)").find(text)
                        if (match != null) expected = match.groupValues[1]
                    }
                } finally { sumsConn.disconnect() }
            }
        }
        check(expected.matches(Regex("[a-fA-F0-9]{64}"))) { "This release has no verifiable checksum. The existing engine was kept." }
        val dir = File(context.noBackupFilesDir, "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}")
        check(dir.exists() || dir.mkdirs())
        val temporary = File.createTempFile("engine-", ".tmp", dir)
        val download = connection(downloadUrl)
        try {
            check(download.responseCode == 200) { "Engine download failed. The existing engine was kept." }
            val digest = MessageDigest.getInstance("SHA-256")
            var bytes = 0L
            temporary.outputStream().use { output -> download.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    val count = input.read(buffer)
                    if (count < 0) break
                    bytes += count
                    check(bytes < 30L * 1024 * 1024) { "Unexpected engine size. The existing engine was kept." }
                    digest.update(buffer, 0, count); output.write(buffer, 0, count)
                }
            } }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(actual.equals(expected, true)) { "Engine checksum did not match. The existing engine was kept." }
            val target = File(dir, YoutubeDL.ytdlpBin)
            temporary.setExecutable(true, false)
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            target.setExecutable(true, false)
            context.getSharedPreferences("engine", Context.MODE_PRIVATE).edit().putString("version", version).apply()
            version
        } finally { download.disconnect(); temporary.delete() }
    }
}
