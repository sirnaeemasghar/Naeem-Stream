@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.naeem.streamnest

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable fun StreamNestScreen(model: MainViewModel, showDownloads: Boolean, onDownloadsShown: () -> Unit, requestNotifications: () -> Unit) {
    val home by model.home.collectAsStateWithLifecycle()
    val jobs by model.jobs.collectAsStateWithLifecycle()
    val prefs by model.preferences.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var bulkFormat by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { model.messages.collect { snack.showSnackbar(it) } }
    LaunchedEffect(showDownloads) { if (showDownloads) { page = 1; onDownloadsShown() } }
    val activeCount = jobs.count { it.status.active || it.status == JobStatus.QUEUED }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    listOf("Home" to Icons.Rounded.Home, "Downloads" to Icons.Rounded.Download, "Settings" to Icons.Rounded.Tune).forEachIndexed { index, (label, icon) ->
                        val selected = page == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { page = index },
                            icon = {
                                if (index == 1 && activeCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(activeCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }) {
                                        Icon(icon, label)
                                    }
                                } else {
                                    Icon(icon, label)
                                }
                            },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(targetState = page, label = "screenTransition", modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) { current ->
                when (current) {
                    0 -> HomeScreen(home, model, jobs, prefs, onDownload = { confirm = true }, onBulk = { bulkFormat = true }, onQueue = { page = 1 })
                    1 -> DownloadsScreen(jobs, model)
                    else -> SettingsScreen(prefs, home, model)
                }
            }
        }
    }

    if (confirm) {
        val folder = prefs.downloadFolder.ifBlank { "NaeemVideos" }
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Start Download", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${home.selected.size} selected ${if (home.selected.size == 1) "item" else "items"} will be queued for download.")
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Downloads / $folder", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        if (prefs.wifiOnly) "Downloads start on unmetered Wi-Fi."
                        else "Downloads will start immediately using current network connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirm = false
                        requestNotifications()
                        model.enqueue { page = 1 }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Add to queue") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            }
        )
    }

    if (bulkFormat || home.formatItem != null) {
        val item = home.formatItem
        ModalBottomSheet(
            onDismissRequest = { bulkFormat = false; model.closeFormats() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Text(if (bulkFormat) "Batch Quality Preset" else "Select Quality", style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    if (bulkFormat) "Apply this resolution limit to all selected videos." else item?.title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (home.formatBusy && !bulkFormat) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                        Text("Detecting stream resolutions…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (home.formatError != null && !bulkFormat) {
                    ErrorNotice(home.formatError!!)
                    OutlinedButton(onClick = { item?.let(model::showFormats) }, shape = RoundedCornerShape(10.dp)) {
                        Text("Try again")
                    }
                } else {
                    val detected = item?.heights.orEmpty()
                    val standardPresets = listOf(2160, 1440, 1080, 720, 480, 360, 240)
                    val heights = if (bulkFormat) standardPresets.filter { it >= 360 } else (detected + standardPresets).distinct().sortedDescending()

                    Text("Video Resolutions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        heights.forEach { height ->
                            val selected = item?.let(model::choice) == DownloadChoice(height)
                            val badge = when {
                                height >= 2160 -> " · 4K"
                                height >= 1440 -> " · 2K"
                                height >= 1080 -> " · Full HD"
                                height >= 720 -> " · HD"
                                else -> "p"
                            }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    model.setChoice(if (bulkFormat) null else item?.id, DownloadChoice(height))
                                    bulkFormat = false
                                    model.closeFormats()
                                },
                                label = { Text("${height}${if (badge.startsWith(" ·")) badge else "p"}") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            model.setChoice(if (bulkFormat) null else item?.id, DownloadChoice(audioOnly = true))
                            bulkFormat = false
                            model.closeFormats()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Headphones, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Audio Only (M4A)", style = MaterialTheme.typography.titleMedium)
                                Text("Extract high-quality audio track", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        "The app downloads the highest quality available up to your choice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable private fun BrandHeader() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2FE69A), Color(0xFF10B981))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF031A0F),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "Naeem Stream",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    "Your media, your way",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Text(
                    "ALL PLATFORMS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable private fun HomeScreen(
    home: HomeState,
    model: MainViewModel,
    jobs: List<DownloadJob>,
    prefs: UserPreferences,
    onDownload: () -> Unit,
    onBulk: () -> Unit,
    onQueue: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { BrandHeader() }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "Download any video,\nany platform.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "YouTube, Reels, Facebook, TikTok, X & Reddit. Up to 4K or Audio.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Link input card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 1.dp
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "QUICK DOWNLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
                                if (!text.isNullOrBlank()) model.url(text) else toast(context, "Copy a link first")
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Rounded.ContentPaste, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Paste", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = home.url,
                        onValueChange = model::url,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste video, reel, or post link", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (home.url.isNotBlank()) {
                                IconButton(onClick = { model.url("") }) {
                                    Icon(Icons.Rounded.Close, "Clear link")
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Platform pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("YouTube", "Instagram", "TikTok", "Facebook", "X / Twitter", "Reddit").forEach { platform ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    platform,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (home.url.contains("list=")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = home.singleOnly, onCheckedChange = model::setSingle)
                            Text("Only this video", Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Button(
                        onClick = { if (home.busy) model.cancelLookup() else model.inspect() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (home.busy) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Finding media · Tap to cancel", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Find media", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        home.error?.let { error -> item { ErrorNotice(error) } }

        val result = home.result
        if (result != null && result.items.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(result.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${result.items.size} found · ${home.selected.size} selected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = model::selectAll,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (home.selected.size == result.items.count { it.available }) "Select none" else "Select all")
                            }
                            OutlinedButton(
                                onClick = onBulk,
                                enabled = home.selected.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Tune, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Batch quality")
                            }
                        }
                        Button(
                            onClick = onDownload,
                            enabled = home.selected.isNotEmpty() && !home.enqueueing,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download ${home.selected.size} selected", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            items(result.items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    selected = item.id in home.selected,
                    choice = model.choice(item),
                    onSelect = { model.select(item.id) },
                    onQuality = { model.showFormats(item) }
                )
            }

            if (result.mayHaveMore) {
                item {
                    OutlinedButton(
                        onClick = { model.inspect(more = true) },
                        enabled = !home.busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Load more videos")
                    }
                }
            }
        } else {
            // Feature Highlights
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CapabilityCard(Modifier.weight(1f), Icons.Rounded.HighQuality, "Up to 4K", "Crisp video")
                    CapabilityCard(Modifier.weight(1f), Icons.AutoMirrored.Rounded.PlaylistPlay, "Playlists", "Batch save")
                    CapabilityCard(Modifier.weight(1f), Icons.Rounded.Headphones, "Audio M4A", "Music & pods")
                }
            }

            // Storage banner
            item {
                val folder = prefs.downloadFolder.ifBlank { "NaeemVideos" }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            Text("Fast local storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            "Downloads are placed directly in your device's Downloads / $folder directory. Ready to watch and share anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            val recent = jobs.firstOrNull { it.status == JobStatus.COMPLETED }
            if (recent != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recently saved", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = onQueue) { Text("View all") }
                        }
                        MediaCard(recent.media, false, recent.choice, onQueue, onQueue, showSelection = false)
                    }
                }
            }

            item {
                Text(
                    "Only download media you own or have permission to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable private fun CapabilityCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun MediaCard(
    item: MediaItem,
    selected: Boolean,
    choice: DownloadChoice,
    onSelect: () -> Unit,
    onQuality: () -> Unit,
    showSelection: Boolean = true
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        label = "cardBorder"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        tonalElevation = if (selected) 2.dp else 1.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Thumbnail(item, Modifier.width(96.dp).height(68.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.creator, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (showSelection) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onSelect() },
                        enabled = item.available,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
            if (item.available) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    Surface(
                        onClick = onQuality,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(if (choice.audioOnly) Icons.Rounded.Headphones else Icons.Rounded.Tune, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(choice.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.ExpandMore, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Text("Unavailable", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable internal fun Thumbnail(item: MediaItem, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (item.thumbnail.isNotBlank()) {
            AsyncImage(model = item.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
        if (item.duration > 0) {
            Text(
                durationLabel(item.duration),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable internal fun ErrorNotice(message: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

internal fun launch(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }.onFailure { toast(context, "No app is available to open this item.") }
}
