package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.MediaClipTrack
import com.example.editor.VideoEditorProject
import com.example.editor.VideoFrameItem
import com.example.ui.SoraMainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TimelineEditorView(
    project: VideoEditorProject,
    selectedClip: MediaClipTrack?,
    onSelectClip: (MediaClipTrack?) -> Unit,
    viewModel: SoraMainViewModel,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadMs by remember { mutableStateOf(0L) }
    var zoomLevel by remember { mutableStateOf(1.0f) } // 1.0x, 1.5x, 2.5x
    var activeTimelineTab by remember { mutableStateOf("TIMELINE") } // TIMELINE, FRAMES, DURATION

    val totalDurationMs = remember(project.videoClips) {
        project.videoClips.sumOf { (it.durationMs / it.playbackSpeed).toLong() }.coerceAtLeast(1000L)
    }

    // Playback scrubber loop
    LaunchedEffect(isPlaying, totalDurationMs) {
        if (isPlaying) {
            while (isPlaying) {
                delay(50)
                currentPlayheadMs = (currentPlayheadMs + 50)
                if (currentPlayheadMs >= totalDurationMs) {
                    currentPlayheadMs = 0L
                    isPlaying = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Timeline Header: Timecode & Transport Controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AccentGreen else ElectricPink)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TIMELINE STUDIO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                SoraBadge(
                    text = "${project.videoClips.size} Clips",
                    color = NeonPurple.copy(alpha = 0.25f),
                    textColor = NeonPurple
                )
            }

            // Timecode Ticker (00:02.4 / 00:12.0)
            val currentSec = currentPlayheadMs / 1000f
            val totalSec = totalDurationMs / 1000f
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DeepDarkBg.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Text(
                    text = String.format("%02d:%04.1fs / %02d:%04.1fs", (currentSec / 60).toInt(), currentSec % 60, (totalSec / 60).toInt(), totalSec % 60),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        // --- Transport Controls Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Scrubber Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Back 0.5s
                IconButton(
                    onClick = {
                        currentPlayheadMs = (currentPlayheadMs - 500L).coerceAtLeast(0L)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSurfaceVariant)
                        .testTag("timeline_step_back_btn")
                ) {
                    Icon(imageVector = Icons.Default.Replay5, contentDescription = "Step -0.5s", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                // Play / Pause
                FilledIconButton(
                    onClick = { isPlaying = !isPlaying },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlaying) ElectricPink else NeonCyan
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("timeline_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DeepDarkBg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Step Forward 0.5s
                IconButton(
                    onClick = {
                        currentPlayheadMs = (currentPlayheadMs + 500L).coerceAtMost(totalDurationMs)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSurfaceVariant)
                        .testTag("timeline_step_forward_btn")
                ) {
                    Icon(imageVector = Icons.Default.Forward5, contentDescription = "Step +0.5s", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                // Stop / Reset to 0
                IconButton(
                    onClick = {
                        isPlaying = false
                        currentPlayheadMs = 0L
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSurfaceVariant)
                        .testTag("timeline_stop_btn")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Reset Playhead", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // Timeline Zoom Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Zoom:", fontSize = 10.sp, color = TextSecondary)
                listOf(1.0f to "1x", 1.5f to "1.5x", 2.5f to "2.5x").forEach { (zVal, zLabel) ->
                    val isSel = zoomLevel == zVal
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) NeonCyan else GlassSurfaceVariant)
                            .clickable { zoomLevel = zVal }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = zLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) DeepDarkBg else TextSecondary
                        )
                    }
                }
            }
        }

        // --- Timeline Multi-Track Ribbon View (Horizontal Scrollable) ---
        val scrollState = rememberScrollState()
        val basePxPerSecond = (65 * zoomLevel).dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DeepDarkBg.copy(alpha = 0.9f))
                .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp)
            ) {
                // 1. Timecode Ruler with Graduated Tick Marks
                val totalSecondsCount = (totalDurationMs / 1000).toInt() + 1
                Row(
                    modifier = Modifier
                        .height(22.dp)
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    (0..totalSecondsCount).forEach { sec ->
                        Box(
                            modifier = Modifier.width(basePxPerSecond),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(10.dp)
                                        .background(NeonCyan.copy(alpha = 0.6f))
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${sec}s",
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // 2. Playhead Visual Cursor Scrubber (Interactive Time Marker)
                val playheadFraction = (currentPlayheadMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                val timelineWidthDp = basePxPerSecond * (totalDurationMs / 1000f)

                // 3. Primary Video Track Clips Ribbon
                if (project.videoClips.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No video clips in timeline. Add clips or generate scenes.", fontSize = 12.sp, color = TextSecondary)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .height(84.dp)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        project.videoClips.forEachIndexed { index, clip ->
                            val isSelected = selectedClip?.id == clip.id
                            val clipWidth = ((clip.durationMs / 1000f) * basePxPerSecond.value).dp.coerceAtLeast(110.dp)

                            TimelineClipBlock(
                                clip = clip,
                                index = index,
                                totalClips = project.videoClips.size,
                                isSelected = isSelected,
                                width = clipWidth,
                                onSelect = { onSelectClip(clip) },
                                onMoveLeft = { viewModel.moveClipLeft(clip.id) },
                                onMoveRight = { viewModel.moveClipRight(clip.id) },
                                onNudgeDuration = { deltaMs -> viewModel.adjustClipDurationBy(clip.id, deltaMs) }
                            )
                        }
                    }
                }

                // 4. Audio Sub-Track Ribbon
                if (project.audioClips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        project.audioClips.forEach { audio ->
                            val audioWidth = ((audio.durationMs / 1000f) * basePxPerSecond.value).dp.coerceAtLeast(100.dp)
                            Box(
                                modifier = Modifier
                                    .width(audioWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeonPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.5f))
                                        )
                                    )
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = audio.title, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Quick Mode Tabs: TIMELINE (Reorder/Actions), FRAMES (Filmstrip), DURATION (Precision Adjust) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = activeTimelineTab == "TIMELINE",
                onClick = { activeTimelineTab = "TIMELINE" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ViewTimeline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Timeline & Order", fontSize = 11.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonCyan, selectedLabelColor = DeepDarkBg),
                modifier = Modifier.weight(1f).testTag("tab_timeline_order")
            )
            FilterChip(
                selected = activeTimelineTab == "FRAMES",
                onClick = { activeTimelineTab = "FRAMES" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.BurstMode, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Frame Strip", fontSize = 11.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).testTag("tab_frame_strip")
            )
            FilterChip(
                selected = activeTimelineTab == "DURATION",
                onClick = { activeTimelineTab = "DURATION" },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Duration Trim", fontSize = 11.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonPurple, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).testTag("tab_duration_trim")
            )
        }

        // --- Active Sub-View Panels ---
        when (activeTimelineTab) {
            "TIMELINE" -> {
                TimelineReorderInspector(
                    project = project,
                    selectedClip = selectedClip,
                    onSelectClip = onSelectClip,
                    viewModel = viewModel
                )
            }
            "FRAMES" -> {
                FrameSequenceFilmstrip(
                    clip = selectedClip ?: project.videoClips.firstOrNull(),
                    viewModel = viewModel
                )
            }
            "DURATION" -> {
                ClipDurationAdjusterPanel(
                    clip = selectedClip ?: project.videoClips.firstOrNull(),
                    viewModel = viewModel
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Timeline Clip Block on the Visual Ribbon
// -------------------------------------------------------------
@Composable
fun TimelineClipBlock(
    clip: MediaClipTrack,
    index: Int,
    totalClips: Int,
    isSelected: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onSelect: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onNudgeDuration: (Long) -> Unit
) {
    val hue = (clip.id.hashCode() % 360).let { if (it < 0) it + 360 else it }.toFloat()
    val baseGrad = Brush.horizontalGradient(
        listOf(
            Color.hsl(hue, 0.65f, 0.25f),
            Color.hsl((hue + 40f) % 360f, 0.70f, 0.18f)
        )
    )

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(baseGrad)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricPink else NeonCyan.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Sequence Badge, Title, Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) ElectricPink else DeepDarkBg)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = clip.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DeepDarkBg.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "${clip.durationMs / 1000}.${(clip.durationMs % 1000) / 100}s",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            // Filmstrip Perforation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 4.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
                    )
                }
            }

            // Bottom Action Bar: Reorder buttons & duration nudge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reorder Left / Right buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeepDarkBg.copy(alpha = 0.8f))
                                .clickable { onMoveLeft() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Move Left", tint = NeonCyan, modifier = Modifier.size(12.dp))
                        }
                    }
                    if (index < totalClips - 1) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeepDarkBg.copy(alpha = 0.8f))
                                .clickable { onMoveRight() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Move Right", tint = NeonCyan, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // Quick duration adjustment +/- 0.5s
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepDarkBg.copy(alpha = 0.8f))
                            .clickable { onNudgeDuration(-500L) }
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-0.5s", fontSize = 8.sp, color = AccentRed)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeepDarkBg.copy(alpha = 0.8f))
                            .clickable { onNudgeDuration(500L) }
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+0.5s", fontSize = 8.sp, color = AccentGreen)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Timeline Reorder Inspector & Clip Sequence Manager
// -------------------------------------------------------------
@Composable
fun TimelineReorderInspector(
    project: VideoEditorProject,
    selectedClip: MediaClipTrack?,
    onSelectClip: (MediaClipTrack?) -> Unit,
    viewModel: SoraMainViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sequence Order (${project.videoClips.size} Clips)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { viewModel.resetTimelineToDefaults() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp).testTag("timeline_reset_btn")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reset Sequence", fontSize = 10.sp, color = TextSecondary)
                }

                Button(
                    onClick = {
                        val newTitle = "✨ Scene ${project.videoClips.size + 1}"
                        viewModel.addClipToEditor("renders/scene_extra.mp4", newTitle)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp).testTag("timeline_add_clip_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = DeepDarkBg)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add Clip", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Clips with direct Up / Down / Left / Right Reorder actions
        project.videoClips.forEachIndexed { index, clip ->
            val isSelected = selectedClip?.id == clip.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, if (isSelected) ElectricPink else GlassSurfaceVariant, RoundedCornerShape(10.dp))
                    .clickable { onSelectClip(clip) },
                color = if (isSelected) ElectricPink.copy(alpha = 0.12f) else GlassSurfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ElectricPink else NeonPurple.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = clip.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ElectricPink else TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "Duration: ${(clip.durationMs / 1000f)}s • ${clip.frames.size} frames • ${clip.velocityCurve}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Reorder Controls & Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Move Up / Left
                        IconButton(
                            onClick = { viewModel.moveClipLeft(clip.id) },
                            enabled = index > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Earlier",
                                tint = if (index > 0) NeonCyan else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Move Down / Right
                        IconButton(
                            onClick = { viewModel.moveClipRight(clip.id) },
                            enabled = index < project.videoClips.lastIndex,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Later",
                                tint = if (index < project.videoClips.lastIndex) NeonCyan else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Duplicate
                        IconButton(
                            onClick = { viewModel.duplicateClip(clip.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate Clip", tint = NeonPurple, modifier = Modifier.size(16.dp))
                        }

                        // Delete
                        IconButton(
                            onClick = {
                                if (project.videoClips.size > 1) {
                                    viewModel.deleteClip(clip.id)
                                    if (selectedClip?.id == clip.id) onSelectClip(null)
                                }
                            },
                            enabled = project.videoClips.size > 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Clip",
                                tint = if (project.videoClips.size > 1) AccentRed else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Frame Sequence Filmstrip (Interactive Keyframes & Frame Reordering)
// -------------------------------------------------------------
@Composable
fun FrameSequenceFilmstrip(
    clip: MediaClipTrack?,
    viewModel: SoraMainViewModel
) {
    if (clip == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a clip to inspect and reorder frames", fontSize = 12.sp, color = TextSecondary)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🎞️ Frame Filmstrip: ${clip.title}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricPink
                )
                Text(
                    text = "${clip.frames.size} Keyframes • Drag & Reorder frames to modify motion sequence",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            // Reverse Motion Button
            OutlinedButton(
                onClick = { viewModel.reverseClipFrames(clip.id) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp).testTag("reverse_frames_btn")
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reverse Motion", fontSize = 10.sp, color = NeonCyan)
            }
        }

        // Horizontal Filmstrip of Frames
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DeepDarkBg)
                .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(clip.frames) { fIdx, frame ->
                KeyframeCard(
                    frame = frame,
                    frameIndex = fIdx,
                    totalFrames = clip.frames.size,
                    onMoveLeft = { viewModel.moveFrameLeft(clip.id, frame.id) },
                    onMoveRight = { viewModel.moveFrameRight(clip.id, frame.id) },
                    onDuplicate = { viewModel.duplicateFrame(clip.id, frame.id) },
                    onDelete = { viewModel.deleteFrame(clip.id, frame.id) }
                )
            }

            item {
                // Add Keyframe Card
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSurfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.addKeyframeToClip(clip.id) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add Frame", tint = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("+ Add Keyframe", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Individual Keyframe Card in the Filmstrip
// -------------------------------------------------------------
@Composable
fun KeyframeCard(
    frame: VideoFrameItem,
    frameIndex: Int,
    totalFrames: Int,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val frameColor = Color.hsl(frame.visualHue, 0.75f, 0.35f)
    val grad = Brush.verticalGradient(
        listOf(frameColor, frameColor.copy(alpha = 0.4f), DeepDarkBg)
    )

    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GlassSurfaceVariant)
            .border(1.dp, if (frame.isKeyframe) ElectricPink.copy(alpha = 0.6f) else GlassSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Thumbnail Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(grad),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (frame.isKeyframe) Icons.Default.Diamond else Icons.Default.Image,
                    contentDescription = null,
                    tint = if (frame.isKeyframe) ElectricPink else NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Frame #${frame.frameIndex}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${frame.timestampMs / 1000}.${(frame.timestampMs % 1000) / 100}s",
                    fontSize = 8.sp,
                    color = TextSecondary
                )
            }
        }

        // Frame Reorder Arrows (◀ Move Earlier / Move Later ▶)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoveLeft,
                enabled = frameIndex > 0,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Move Left",
                    tint = if (frameIndex > 0) NeonCyan else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onDuplicate,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate Hold", tint = NeonPurple, modifier = Modifier.size(12.dp))
            }

            IconButton(
                onClick = onMoveRight,
                enabled = frameIndex < totalFrames - 1,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Move Right",
                    tint = if (frameIndex < totalFrames - 1) NeonCyan else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Clip Duration Adjuster Panel
// -------------------------------------------------------------
@Composable
fun ClipDurationAdjusterPanel(
    clip: MediaClipTrack?,
    viewModel: SoraMainViewModel
) {
    if (clip == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a clip to adjust duration and trimming", fontSize = 12.sp, color = TextSecondary)
        }
        return
    }

    var sliderValue by remember(clip.id, clip.durationMs) { mutableStateOf((clip.durationMs / 1000f)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⏱️ Duration Inspector: ${clip.title}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
                Text(
                    text = "Current: ${clip.durationMs / 1000}.${(clip.durationMs % 1000) / 100}s (${clip.durationMs} ms)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            SoraBadge(
                text = "${String.format("%.1f", sliderValue)}s",
                color = NeonCyan,
                textColor = DeepDarkBg
            )
        }

        // Live Slider for Duration Adjustment (0.5s to 15.0s)
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                viewModel.updateClipDuration(clip.id, (it * 1000).toLong())
            },
            valueRange = 0.5f..15.0f,
            steps = 29,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = GlassSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().testTag("duration_slider")
        )

        // Quick Preset Duration Buttons
        Text(text = "Quick Duration Presets:", fontSize = 11.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0.5f to "0.5s (Flash)",
                1.0f to "1.0s (Quick)",
                2.5f to "2.5s (Standard)",
                4.0f to "4.0s (Scene)",
                8.0f to "8.0s (Long)"
            ).forEach { (presetSec, presetLabel) ->
                val isSel = (clip.durationMs / 1000f) == presetSec
                OutlinedButton(
                    onClick = {
                        sliderValue = presetSec
                        viewModel.updateClipDuration(clip.id, (presetSec * 1000).toLong())
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSel) NeonCyan.copy(alpha = 0.2f) else Color.Transparent
                    )
                ) {
                    Text(text = presetLabel, fontSize = 9.sp, color = if (isSel) NeonCyan else TextPrimary, maxLines = 1)
                }
            }
        }

        // Precision Trim Handles (Start / End Trim)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Trim Start
            OutlinedButton(
                onClick = { viewModel.trimClipStart(clip.id, 500L) },
                modifier = Modifier.weight(1f).testTag("trim_start_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.FastRewind, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Trim Start (-0.5s)", fontSize = 10.sp, color = TextPrimary)
            }

            // Trim End
            OutlinedButton(
                onClick = { viewModel.trimClipEnd(clip.id, 500L) },
                modifier = Modifier.weight(1f).testTag("trim_end_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentRed)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Trim End (-0.5s)", fontSize = 10.sp, color = TextPrimary)
            }
        }
    }
}
