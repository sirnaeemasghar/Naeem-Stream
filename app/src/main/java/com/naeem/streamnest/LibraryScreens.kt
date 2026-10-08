@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.naeem.streamnest

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun DownloadsScreen(jobs: List<DownloadJob>, model: MainViewModel) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val saved = jobs.count { it.status == JobStatus.COMPLETED }
    val active = jobs.count { it.status.active || it.status == JobStatus.QUEUED }
    val visible = jobs.filter { when (filter) { 0 -> it.status != JobStatus.COMPLETED; 1 -> it.status == JobStatus.COMPLETED; else -> true } }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    "COLLECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Downloads",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$active in queue · $saved saved to device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Queue" to active, "Saved" to saved, "All" to jobs.size).forEachIndexed { index, (label, count) ->
                        val selected = filter == index
                        val containerColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            label = "tabContainer"
                        )
                        val contentColor by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tabContent"
                        )
                        Surface(
                            onClick = { filter = index },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = containerColor
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                                if (count > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            count.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (jobs.any { it.status == JobStatus.QUEUED } && jobs.none { it.status.active }) item {
            OutlinedButton(
                onClick = model::resumeQueue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Resume queue", fontWeight = FontWeight.SemiBold)
            }
        }
        if (visible.isEmpty()) item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (filter == 1) Icons.Rounded.VideoLibrary else Icons.Rounded.Download,
                            null,
                            Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        if (filter == 1) "No saved videos yet" else "Your queue is empty",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (filter == 1) "Completed downloads will appear here ready to open or share."
                        else "Paste a link on the Home screen to download videos at your desired quality.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(visible, key = { it.id }) { job -> DownloadCard(job, model) }
        if (saved > 0 && filter != 0) item {
            val context = LocalContext.current
            Surface(
                onClick = { launch(context, Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.FolderOpen, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Open Android Downloads Folder",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable private fun DownloadCard(job: DownloadJob, model: MainViewModel) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Thumbnail(job.media, Modifier.width(92.dp).height(66.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(job.media.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                job.choice.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (job.status == JobStatus.COMPLETED && job.bytes > 0) {
                            Text(
                                prettyBytes(job.bytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Download options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("View original link") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp)) },
                            onClick = { menu = false; openBrowserUrl(context, job.media.url) }
                        )
                        if (!job.status.active && job.status != JobStatus.QUEUED) {
                            DropdownMenuItem(
                                text = { Text("Remove from history") },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp)) },
                                onClick = { menu = false; model.remove(job.id) }
                            )
                        }
                    }
                }
            }

            // Status bar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val warning = job.status in setOf(JobStatus.FAILED, JobStatus.INTERRUPTED)
                val statusColor = when {
                    job.status == JobStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    warning -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                Icon(
                    when {
                        job.status == JobStatus.COMPLETED -> Icons.Rounded.CheckCircle
                        warning -> Icons.Rounded.ErrorOutline
                        else -> Icons.Rounded.Circle
                    },
                    null,
                    Modifier.size(if (job.status.active) 8.dp else 16.dp),
                    tint = statusColor
                )
                Text(
                    job.status.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    modifier = Modifier.weight(1f)
                )
                if (job.status == JobStatus.DOWNLOADING) {
                    Text("${(job.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            if (job.status == JobStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { job.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else if (job.status.active) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Text(
                job.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when {
                    job.status == JobStatus.COMPLETED -> {
                        Button(
                            onClick = { openMedia(context, job, false) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { openMedia(context, job, true) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.Share, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                    job.status.active || job.status == JobStatus.QUEUED -> {
                        OutlinedButton(
                            onClick = { model.cancel(job.id) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel download")
                        }
                    }
                    else -> {
                        Button(
                            onClick = { model.retry(job.id) },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry download", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private fun openMedia(context: Context, job: DownloadJob, share: Boolean) {
    val uri = Uri.parse(job.outputUri)
    val exists = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false }.getOrDefault(false)
    if (!exists) {
        toast(context, "This file was moved or deleted. Remove its history entry to download it again.")
        return
    }
    val mime = if (job.choice.audioOnly) "audio/mp4" else "video/mp4"
    val intent = if (share) {
        Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).apply {
            clipData = android.content.ClipData.newUri(context.contentResolver, job.media.title, uri)
        }
    } else {
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    launch(context, if (share) Intent.createChooser(intent, "Share download") else intent)
}

@Composable fun SettingsScreen(prefs: UserPreferences, home: HomeState, model: MainViewModel) {
    val context = LocalContext.current
    var about by remember { mutableStateOf(false) }
    var folderDialog by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Customize playback, storage, and engine options.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Appearance
        item {
            SettingsGroup("APPEARANCE") {
                Text("Theme mode", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Dark", "Light", "System").forEach { theme ->
                        val selected = prefs.theme == theme
                        FilterChip(
                            selected = selected,
                            onClick = { model.preferences(prefs.copy(theme = theme)) },
                            label = {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(theme, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Downloads
        item {
            SettingsGroup("DOWNLOADS") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Wi-Fi only", style = MaterialTheme.typography.titleMedium)
                        Text("Only download when connected to unmetered Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = prefs.wifiOnly, onCheckedChange = { model.preferences(prefs.copy(wifiOnly = it)) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text("Default video quality", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(360, 480, 720, 1080, 2160).forEach { height ->
                        FilterChip(
                            selected = prefs.defaultHeight == height,
                            onClick = { model.preferences(prefs.copy(defaultHeight = height)) },
                            label = { Text("${height}p") },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
                Text("Selects best stream available up to your preset.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                val currentFolder = prefs.downloadFolder.ifBlank { "NaeemVideos" }
                SettingsLink(Icons.Rounded.FolderOpen, "Save location", "Downloads / $currentFolder") { folderDialog = true }
            }
        }

        // Media Engine
        item {
            SettingsGroup("MEDIA ENGINE") {
                SettingsLink(Icons.Rounded.SystemUpdateAlt, "yt-dlp & FFmpeg core", home.engineVersion) {
                    openBrowserUrl(context, "https://github.com/yt-dlp/yt-dlp")
                }
                Text("Engine updates keep video extraction compatible with platform changes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = model::updateEngine,
                    enabled = !home.updating && !home.busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (home.updating) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (home.updating) "Checking official release…" else "Check for engine update")
                }
                Text("Official yt-dlp releases only. Cryptographically verified.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // About & Support
        item {
            SettingsGroup("ABOUT AND SUPPORT") {
                SettingsLink(Icons.Rounded.Info, "About Naeem Stream", "Version ${BuildConfig.VERSION_NAME} · What's inside") {
                    about = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                SettingsLink(Icons.Rounded.Email, "Support & inquiries", "contact.naeemasghar@gmail.com") {
                    openEmail(context, "contact.naeemasghar@gmail.com")
                }
            }
        }

        // Developer & Credits
        item {
            SettingsGroup("DEVELOPER & CREDITS") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Terminal, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Developed by Naeem Asghar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Creator & Software Engineer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SettingsLink(
                    icon = Icons.Rounded.Code,
                    title = "GitHub Profile",
                    subtitle = "github.com/sirnaeemasghar"
                ) {
                    openBrowserUrl(context, "https://github.com/sirnaeemasghar")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                SettingsLink(
                    icon = Icons.Rounded.Email,
                    title = "Email",
                    subtitle = "contact.naeemasghar@gmail.com"
                ) {
                    openEmail(context, "contact.naeemasghar@gmail.com")
                }
            }
        }

        // Footer
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Made in Pakistan ❤️",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (folderDialog) {
        var inputFolder by remember { mutableStateOf(prefs.downloadFolder.ifBlank { "NaeemVideos" }) }
        AlertDialog(
            onDismissRequest = { folderDialog = false },
            icon = { Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Save Location", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose or enter a folder name inside your device's Downloads directory:")
                    OutlinedTextField(
                        value = inputFolder,
                        onValueChange = { inputFolder = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Quick presets:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NaeemVideos", "NaeemStream", "Videos", "Reels", "Downloads").forEach { preset ->
                            FilterChip(
                                selected = inputFolder == preset,
                                onClick = { inputFolder = preset },
                                label = { Text(preset) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Text("Files will be saved to Downloads / $inputFolder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = inputFolder.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "NaeemVideos" }
                        model.preferences(prefs.copy(downloadFolder = cleaned))
                        folderDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { folderDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (about) {
        AlertDialog(
            onDismissRequest = { about = false },
            icon = { Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Naeem Stream", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your media, your way.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("A premium, personal video downloader for YouTube, Facebook, Instagram Reels, TikTok, Twitter/X, and more.")
                    Text("Developed by Naeem Asghar", fontWeight = FontWeight.SemiBold)
                    Text("All downloads process directly on your device with high performance. No analytics, no accounts, no subscriptions.")
                    Text("Version ${BuildConfig.VERSION_NAME} · Powered by Jetpack Compose, yt-dlp & FFmpeg.")
                    Text("Made in Pakistan ❤️", fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(onClick = { about = false }, shape = RoundedCornerShape(10.dp)) { Text("Done") }
            }
        )
    }
}

@Composable private fun SettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable private fun SettingsLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        toast(context, "No web browser found to open link.")
    }
}

fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    } catch (_: Exception) {
        toast(context, "No email client found on this device.")
    }
}
