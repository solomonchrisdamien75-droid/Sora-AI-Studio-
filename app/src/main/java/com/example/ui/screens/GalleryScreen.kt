package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GalleryItemEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.components.generation.DurationFormatters
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(viewModel: SoraMainViewModel) {
    val items by viewModel.galleryItems.collectAsState()
    var selectedItemForPlayer by remember { mutableStateOf<GalleryItemEntity?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SoraSectionHeader(
            title = "Generated Gallery",
            subtitle = "${items.size} offline AI video(s) and media saved on device",
            icon = Icons.Default.PermMedia
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            SoraGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = NeonCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Generated Videos Yet",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start a new AI video rendering task in the Workbench or Chat tab.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.selectTab(SoraTab.GENERATE) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Video in Workbench", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items) { item ->
                    GalleryCardItem(
                        item = item,
                        onPlayClick = { selectedItemForPlayer = item },
                        onEditClick = {
                            viewModel.addClipToEditor(item.filePath, item.title, if (item.durationMs > 0) item.durationMs else 5000L)
                            Toast.makeText(context, "Loaded '${item.title}' into Video Editor!", Toast.LENGTH_SHORT).show()
                            viewModel.selectTab(SoraTab.EDITOR)
                        }
                    )
                }
            }
        }

        // Dedicated Full Interactive Video Player Dialog right inside Gallery
        selectedItemForPlayer?.let { item ->
            GalleryVideoPlayerModal(
                item = item,
                onDismiss = { selectedItemForPlayer = null },
                onSendToEditor = {
                    viewModel.addClipToEditor(item.filePath, item.title, if (item.durationMs > 0) item.durationMs else 5000L)
                    selectedItemForPlayer = null
                    Toast.makeText(context, "Sent '${item.title}' to Video Editor!", Toast.LENGTH_SHORT).show()
                    viewModel.selectTab(SoraTab.EDITOR)
                }
            )
        }
    }
}

@Composable
fun GalleryCardItem(
    item: GalleryItemEntity,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val baseHue = (item.title.hashCode() % 360).let { if (it < 0) it + 360 else it }.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GlassSurface)
            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { onPlayClick() }
            .testTag("gallery_item_${item.id}")
    ) {
        // Thumbnail simulated background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grad = Brush.verticalGradient(
                colors = listOf(
                    Color.hsl(baseHue, 0.45f, 0.15f),
                    DeepDarkBg.copy(alpha = 0.95f)
                )
            )
            drawRect(brush = grad)

            // Dynamic grid lines for cyber/filmstrip aesthetic
            val step = 24.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color.hsl(baseHue, 0.7f, 0.4f, 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Badges and Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoraBadge(
                    text = item.resolutionLabel.ifBlank { "1080p" },
                    color = ElectricPink
                )

                // Quick Play Circle Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.25f))
                        .border(1.dp, NeonCyan, CircleShape)
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Center: Video Timecode overlay badge
            val durSecInt = if (item.durationMs > 0) (item.durationMs / 1000).toInt() else 5
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "▶ ${DurationFormatters.formatDisplay(durSecInt)}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom metadata & Quick Editor Button
            Column {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.prompt.ifBlank { "Offline AI Generated Video" },
                    fontSize = 9.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Action Bar: Tap to Play & Send to Editor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onPlayClick() }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶ Play",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricPink.copy(alpha = 0.15f))
                            .border(1.dp, ElectricPink.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onEditClick() }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✂️ Edit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricPink
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryVideoPlayerModal(
    item: GalleryItemEntity,
    onDismiss: () -> Unit,
    onSendToEditor: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val totalDurationMs = if (item.durationMs > 0) item.durationMs else 5000L

    var isPlaying by remember { mutableStateOf(true) }
    var currentPlayheadMs by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    val baseHue = remember(item.title) {
        (item.title.hashCode() % 360).let { if (it < 0) it + 360 else it }.toFloat()
    }

    // Playback loop ticker
    LaunchedEffect(isPlaying, totalDurationMs, playbackSpeed, isLooping) {
        if (isPlaying) {
            val stepMs = 50L
            while (isPlaying) {
                delay(stepMs)
                val increment = (stepMs * playbackSpeed).toLong()
                val next = currentPlayheadMs + increment
                if (next >= totalDurationMs) {
                    if (isLooping) {
                        currentPlayheadMs = 0L
                    } else {
                        currentPlayheadMs = totalDurationMs
                        isPlaying = false
                    }
                } else {
                    currentPlayheadMs = next
                }
            }
        }
    }

    // Animated scanner line effect
    val infiniteTransition = rememberInfiniteTransition(label = "player_scanner")
    val scanPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_phase"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            color = GlassSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Modal Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "In-Gallery Video Playback",
                                fontSize = 10.sp,
                                color = AccentGreen
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoraBadge(text = item.resolutionLabel.ifBlank { "1080p" }, color = ElectricPink)
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Live Video Player Screen Viewport ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepDarkBg)
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    // Video Rendering Canvas with dynamic frame visualizer
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val playProgress = (currentPlayheadMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        val dynamicHue = (baseHue + playProgress * 60f) % 360f

                        // Dynamic multi-layer gradient background
                        val grad = Brush.radialGradient(
                            colors = listOf(
                                Color.hsl(dynamicHue, 0.75f, 0.28f),
                                Color.hsl((dynamicHue + 40f) % 360f, 0.6f, 0.12f),
                                DeepDarkBg
                            ),
                            center = Offset(size.width * (0.3f + 0.4f * playProgress), size.height * 0.5f),
                            radius = size.width * 0.8f
                        )
                        drawRect(brush = grad)

                        // Film grain / Scanline sweep
                        val scanY = size.height * scanPhase
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.25f),
                            start = Offset(0f, scanY),
                            end = Offset(size.width, scanY),
                            strokeWidth = 2f
                        )

                        // Center focal graphic: Playhead visualization ring
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = 38.dp.toPx()

                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.15f),
                            radius = radius,
                            center = Offset(centerX, centerY)
                        )

                        drawArc(
                            color = NeonCyan,
                            startAngle = -90f,
                            sweepAngle = playProgress * 360f,
                            useCenter = false,
                            topLeft = Offset(centerX - radius, centerY - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Floating Watermark & Status Tag
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isPlaying) "● PLAYING (${playbackSpeed}x)" else "❚❚ PAUSED",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) AccentGreen else ElectricPink,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "${item.resolutionLabel} • 24 FPS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Center Big Play/Pause Overlay Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(DeepDarkBg.copy(alpha = 0.75f))
                            .border(1.5.dp, NeonCyan, CircleShape)
                            .clickable { isPlaying = !isPlaying }
                            .testTag("gallery_modal_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NeonCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Bottom Timecode HUD
                    val curSec = currentPlayheadMs / 1000f
                    val totSec = totalDurationMs / 1000f
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%04.1fs / %02d:%04.1fs", (curSec / 60).toInt(), curSec % 60, (totSec / 60).toInt(), totSec % 60),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Scrubber Slider ---
                Slider(
                    value = currentPlayheadMs.toFloat(),
                    onValueChange = {
                        currentPlayheadMs = it.toLong()
                    },
                    valueRange = 0f..totalDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = GlassSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("gallery_player_slider")
                )

                // --- Transport Control Bar ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step back 0.5s
                        IconButton(
                            onClick = { currentPlayheadMs = (currentPlayheadMs - 500L).coerceAtLeast(0L) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                        ) {
                            Icon(imageVector = Icons.Default.Replay5, contentDescription = "Step -0.5s", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Play/Pause
                        FilledIconButton(
                            onClick = { isPlaying = !isPlaying },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isPlaying) ElectricPink else NeonCyan),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DeepDarkBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Step forward 0.5s
                        IconButton(
                            onClick = { currentPlayheadMs = (currentPlayheadMs + 500L).coerceAtMost(totalDurationMs) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                        ) {
                            Icon(imageVector = Icons.Default.Forward5, contentDescription = "Step +0.5s", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Loop toggle
                        IconButton(
                            onClick = { isLooping = !isLooping },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLooping) NeonCyan.copy(alpha = 0.2f) else GlassSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Loop",
                                tint = if (isLooping) NeonCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Mute toggle
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMuted) AccentRed.copy(alpha = 0.2f) else GlassSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Audio",
                                tint = if (isMuted) AccentRed else AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Speed Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                            val isSel = playbackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonCyan else GlassSurfaceVariant)
                                    .clickable { playbackSpeed = speed }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${speed}x",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) DeepDarkBg else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Filmstrip Keyframe Snapshots ---
                Text(text = "Filmstrip Frame Snapshots", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val frameCount = 6
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(frameCount) { idx ->
                        val frameTs = if (frameCount > 1) idx * (totalDurationMs / (frameCount - 1)) else 0L
                        val isNearPlayhead = kotlin.math.abs(currentPlayheadMs - frameTs) < (totalDurationMs / frameCount)

                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isNearPlayhead) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    currentPlayheadMs = frameTs
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "F${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isNearPlayhead) NeonCyan else TextPrimary)
                                Text(text = String.format("%.1fs", frameTs / 1000f), fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Prompt & Metadata Card ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(text = "Generation Prompt & Info:", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.prompt.ifBlank { "Offline Local Sora Video Render" },
                            fontSize = 12.sp,
                            color = TextPrimary,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "📁 ${item.filePath.takeLast(32)}", fontSize = 9.sp, color = TextSecondary)
                            Text(text = "⚡ LiteRT/GGUF Local Engine", fontSize = 9.sp, color = AccentGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Bottom Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Send to Editor Button (Primary CTA)
                    Button(
                        onClick = onSendToEditor,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("gallery_send_to_editor_btn")
                    ) {
                        Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send to Editor", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Copy Path / Export
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(item.filePath))
                            Toast.makeText(context, "Saved to clipboard: ${item.filePath}", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

