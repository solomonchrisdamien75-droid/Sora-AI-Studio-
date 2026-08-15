package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.editor.AspectRatioPreset
import com.example.editor.ExportResolution
import com.example.editor.MediaClipTrack
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(viewModel: SoraMainViewModel) {
    val project by viewModel.editorProject.collectAsState()
    val activeClipIdFromVm by viewModel.activeEditorClipId.collectAsState()
    var selectedClip by remember { mutableStateOf<MediaClipTrack?>(null) }
    var selectedRatio by remember { mutableStateOf(AspectRatioPreset.RATIO_16_9) }
    var selectedResolution by remember { mutableStateOf(ExportResolution.RES_1080P) }
    val latestExport by viewModel.latestExportedResult.collectAsState()

    // Sync selected clip when ViewModel updates active clip
    LaunchedEffect(activeClipIdFromVm, project.videoClips) {
        if (activeClipIdFromVm != null) {
            val matching = project.videoClips.find { it.id == activeClipIdFromVm }
            if (matching != null) {
                selectedClip = matching
            }
        }
    }

    val activeSelectedClip = project.videoClips.find { it.id == selectedClip?.id } 
        ?: project.videoClips.firstOrNull()

    var activeSubToolTab by remember { mutableStateOf("VELOCITY") } // DURATION, FRAMES, VELOCITY, AI_EFFECTS, TRANSITIONS, SUBTITLES, AUDIO_VOICE, CUTOUT

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "CapCut Video Editing Studio",
                subtitle = "Velocity ramping, 3D zoom, beat sync, auto-captions & AI voice changer",
                icon = Icons.Default.MovieFilter
            )
        }

        // Export Completed Banner
        val export = latestExport
        if (export != null) {
            item {
                SoraGlassCard(borderColor = AccentGreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Project Export Successful!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.dismissLatestExportedResult() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Saved to: ${export.filePath}", fontSize = 11.sp, color = NeonCyan)
                    Text(text = "Resolution: ${export.resolutionLabel} • Duration: ${(export.durationMs / 1000)}s", fontSize = 11.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.selectTab(SoraTab.GALLERY) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepDarkBg)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Gallery", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.dismissLatestExportedResult() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continue Editing", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // CapCut One-Tap Preset Templates Banner
        item {
            SoraGlassCard(borderColor = ElectricPink) {
                Text(text = "🔥 CapCut One-Tap Preset Style Templates", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Apply trending velocity edit curves, 3D zoom effects, and transitions automatically", fontSize = 11.sp, color = TextSecondary)
                
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Button(
                            onClick = { viewModel.applyCapCutPresetTemplate("TIKTOK_VELOCITY") },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("template_tiktok_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⚡ TikTok Velocity Edit", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    item {
                        Button(
                            onClick = { viewModel.applyCapCutPresetTemplate("ANIME_BEAT_SYNC") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("template_anime_btn")
                        ) {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🎌 Anime Beat Sync", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    item {
                        Button(
                            onClick = { viewModel.applyCapCutPresetTemplate("CINEMATIC_TRAILER") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("template_cinematic_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🎬 Cinematic Trailer", fontSize = 11.sp, color = Color.Black)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { viewModel.autoGenerateCaptionsFromAudio() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("template_autocaptions_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✨ Auto-Subtitles", fontSize = 11.sp, color = AccentGreen)
                        }
                    }
                }
            }
        }

        // Live Interactive Video Player & Preview Viewport Canvas
        item {
            EditorStudioPlayerViewport(
                project = project,
                activeClip = activeSelectedClip,
                selectedRatio = selectedRatio,
                viewModel = viewModel
            )
        }

        // Aspect Ratio & Resolution Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Canvas Framing Ratio", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RatioChip("9:16 Shorts", AspectRatioPreset.RATIO_9_16, selectedRatio) { selectedRatio = AspectRatioPreset.RATIO_9_16 }
                        RatioChip("16:9 YT", AspectRatioPreset.RATIO_16_9, selectedRatio) { selectedRatio = AspectRatioPreset.RATIO_16_9 }
                        RatioChip("1:1 Insta", AspectRatioPreset.RATIO_1_1, selectedRatio) { selectedRatio = AspectRatioPreset.RATIO_1_1 }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Export Quality", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ResChip("1080p", ExportResolution.RES_1080P, selectedResolution) { selectedResolution = ExportResolution.RES_1080P }
                        ResChip("4K Ultra", ExportResolution.RES_4K, selectedResolution) { selectedResolution = ExportResolution.RES_4K }
                    }
                }
            }
        }

        // Dedicated Interactive Timeline Studio Component
        item {
            TimelineEditorView(
                project = project,
                selectedClip = activeSelectedClip,
                onSelectClip = { selectedClip = it },
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Track Clips List
        item {
            SoraSectionHeader(
                title = "CapCut Video Tracks",
                subtitle = "Select clip to edit velocity curves, AI 3D effects, voice changers & captions",
                icon = Icons.Default.VideoLibrary
            )
        }

        if (project.videoClips.isEmpty()) {
            item {
                SoraGlassCard {
                    Text(
                        text = "No video clips in timeline. Generate a video or apply a CapCut template above.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(project.videoClips) { clip ->
                val isSelected = selectedClip?.id == clip.id
                SoraGlassCard(
                    borderColor = if (isSelected) ElectricPink else GlassSurfaceVariant,
                    onClick = { selectedClip = clip }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = clip.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "Duration: ${clip.durationMs / 1000}s • Speed: ${clip.playbackSpeed}x", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                            SoraBadge(text = "${clip.durationMs / 1000}s", color = ElectricPink)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // CapCut Active FX Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (clip.velocityCurve != "NONE") SoraBadge(text = "⚡ ${clip.velocityCurve}", color = NeonCyan)
                            if (clip.aiStyleEffect != "NONE") SoraBadge(text = "🌟 ${clip.aiStyleEffect}", color = ElectricPink)
                            if (clip.transitionType != "NONE") SoraBadge(text = "🎬 ${clip.transitionType}", color = NeonPurple)
                            if (clip.voiceChangerPreset != "NONE") SoraBadge(text = "🎙️ ${clip.voiceChangerPreset}", color = AccentGreen)
                            if (clip.bgRemovalCutout) SoraBadge(text = "✂️ Cutout", color = AccentRed)
                        }
                    }
                }
            }
        }

        // Multi-Track Layers Toolbar
        item {
            SoraGlassCard {
                Text(text = "Multi-Track Layers", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.addAudioTrackToEditor() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_audio_btn")
                        ) {
                            Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Audio Track", fontSize = 11.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { viewModel.addVoiceoverTrackToEditor() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_voiceover_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voice-over", fontSize = 11.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { viewModel.addSubtitleLayerToEditor() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_subtitles_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Subtitles / Captions", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // CapCut Editing Tool Suite for Selected Clip
        if (activeSelectedClip != null) {
            item {
                val clip = activeSelectedClip

                SoraGlassCard(borderColor = ElectricPink) {
                    Text(text = "CapCut Tool Suite: ${clip.title}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricPink)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Split / Trim / Reverse Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.splitClip(clip.id)
                                selectedClip = null
                            },
                            modifier = Modifier.weight(1f).testTag("split_clip_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Split Cut", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.trimClip(clip.id) },
                            modifier = Modifier.weight(1f).testTag("trim_clip_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trim 1s", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.reverseClip(clip.id) },
                            modifier = Modifier.weight(1f).testTag("reverse_clip_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FastRewind, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reverse", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sub-Tool Category Selector Bar
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "DURATION",
                                onClick = { activeSubToolTab = "DURATION" },
                                label = { Text("⏱️ Duration", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "FRAMES",
                                onClick = { activeSubToolTab = "FRAMES" },
                                label = { Text("🎞️ Frames", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "VELOCITY",
                                onClick = { activeSubToolTab = "VELOCITY" },
                                label = { Text("⚡ Velocity", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "AI_EFFECTS",
                                onClick = { activeSubToolTab = "AI_EFFECTS" },
                                label = { Text("🌟 3D & AI FX", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "TRANSITIONS",
                                onClick = { activeSubToolTab = "TRANSITIONS" },
                                label = { Text("🎬 Transitions", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "SUBTITLES",
                                onClick = { activeSubToolTab = "SUBTITLES" },
                                label = { Text("🔤 Captions", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "AUDIO_VOICE",
                                onClick = { activeSubToolTab = "AUDIO_VOICE" },
                                label = { Text("🎙️ Voice & SFX", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "CUTOUT",
                                onClick = { activeSubToolTab = "CUTOUT" },
                                label = { Text("✂️ Cutout", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeSubToolTab == "WATERMARK",
                                onClick = { activeSubToolTab = "WATERMARK" },
                                label = { Text("🧹 Watermark Remover", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Content
                    when (activeSubToolTab) {
                        "DURATION" -> {
                            ClipDurationAdjusterPanel(
                                clip = clip,
                                viewModel = viewModel
                            )
                        }

                        "FRAMES" -> {
                            FrameSequenceFilmstrip(
                                clip = clip,
                                viewModel = viewModel
                            )
                        }
                        "VELOCITY" -> {
                            Text(text = "Smooth Speed Ramping & Velocity Curves:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        "AUTO_VELOCITY" to "⚡ Auto Velocity (Beat Sync)",
                                        "MONTAGE_RAMP" to "🚀 Montage Curve",
                                        "HERO_PULSE" to "🎯 Hero Pulse (0.2x Zoom)",
                                        "BULLET_TIME" to "⏱️ Bullet Time (0.1x)",
                                        "FLASH_FREEZE" to "⚡ Flash Freeze",
                                        "NONE" to "Normal Speed"
                                    )
                                ) { (vKey, vLabel) ->
                                    val isSel = clip.velocityCurve == vKey
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.updateClipVelocityCurve(clip.id, vKey) },
                                        label = { Text(vLabel, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        "AI_EFFECTS" -> {
                            Text(text = "CapCut AI Visual Effects & Style Filters:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        "ZOOM_3D_PARALLAX" to "🌟 3D Zoom Parallax",
                                        "ANIME_CONVERSION" to "🎨 AI Anime Style",
                                        "CYBERPUNK_GLOW" to "🌆 Cyberpunk Neon",
                                        "MANGA_SKETCH" to "✏️ Manga Ink Sketch",
                                        "RETRO_VHS" to "📼 Retro 90s VHS Glitch",
                                        "LIGHT_LEAKS" to "💥 Light Leaks",
                                        "BODY_EDGE_GLOW" to "✨ Body Edge Glow",
                                        "NONE" to "No Effect"
                                    )
                                ) { (eKey, eLabel) ->
                                    val isSel = clip.aiStyleEffect == eKey
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.updateClipAiStyleEffect(clip.id, eKey) },
                                        label = { Text(eLabel, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        "TRANSITIONS" -> {
                            Text(text = "Dynamic Seamless Transitions:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        "WHIP_PAN" to "🌀 Whip Pan Blur",
                                        "FLASH_WHITE" to "⚡ Flash White Glow",
                                        "BLUR_SLIDE" to "🔀 Blur Slide",
                                        "GLITCH_TEAR" to "📺 Glitch Tear",
                                        "ZOOM_IN_OUT" to "🔍 Smooth Zoom",
                                        "PAGE_FLIP" to "📖 3D Page Flip",
                                        "NONE" to "Cut Transition"
                                    )
                                ) { (tKey, tLabel) ->
                                    val isSel = clip.transitionType == tKey
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.updateClipTransition(clip.id, tKey) },
                                        label = { Text(tLabel, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        "SUBTITLES" -> {
                            Text(text = "Auto Captions & Subtitle Styling:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        "KINETIC_BOUNCE" to "🔤 Kinetic Bouncing",
                                        "NEON_GLOW" to "💡 Neon Glow Captions",
                                        "KARAOKE_HIGHLIGHT" to "🎤 Karaoke Word Sync",
                                        "COMIC_BUBBLE" to "🗯️ Comic Anime Bubble",
                                        "NONE" to "Standard Text"
                                    )
                                ) { (sKey, sLabel) ->
                                    val isSel = clip.subtitleStyle == sKey
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.updateClipSubtitleStyle(clip.id, sKey) },
                                        label = { Text(sLabel, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        "AUDIO_VOICE" -> {
                            Text(text = "AI Voice Changer Presets:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        "DEEP_TRAILER" to "🎙️ Deep Trailer Narrator",
                                        "ANIME_HERO" to "🎌 Anime Protagonist",
                                        "CYBER_ROBOT" to "🤖 Cyber Robot Voice",
                                        "CHIPMUNK" to "🐿️ High Pitch Chipmunk",
                                        "NONE" to "Original Voice"
                                    )
                                ) { (vKey, vLabel) ->
                                    val isSel = clip.voiceChangerPreset == vKey
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.updateClipVoiceChanger(clip.id, vKey) },
                                        label = { Text(vLabel, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = "Trending CapCut Sound Effects (SFX):", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(
                                    listOf(
                                        "BASS_DROP" to "🔊 Bass Drop Pulse",
                                        "WHOOSH_SWIPE" to "💨 Whoosh Swipe",
                                        "GLITCH_STATIC" to "⚡ Glitch Static",
                                        "LASER_FIRE" to "🔫 Laser Blast",
                                        "SWORD_SLASH" to "⚔️ Sword Slash",
                                        "NONE" to "No SFX"
                                    )
                                ) { (sfxKey, sfxLabel) ->
                                    val isSel = clip.sfxPreset == sfxKey
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { viewModel.updateClipSfxPreset(clip.id, sfxKey) },
                                        label = { Text(sfxLabel, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        "CUTOUT" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "AI Smart Cutout (One-Click Background Removal)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "Automatically isolates human subjects and foreground elements from background.", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = clip.bgRemovalCutout,
                                    onCheckedChange = { viewModel.toggleClipBgCutout(clip.id) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentRed, checkedTrackColor = AccentRed.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("cutout_switch")
                                )
                            }
                        }

                        "WATERMARK" -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "🧹 AI Watermark Eraser & Clean Canvas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = "Automatically detects and removes TikTok, Douyin, YouTube, or stock watermarks.", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = clip.removeWatermark,
                                        onCheckedChange = { viewModel.toggleClipWatermarkRemover(clip.id) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f)),
                                        modifier = Modifier.testTag("watermark_switch")
                                    )
                                }

                                if (clip.removeWatermark) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Eraser Method:", fontSize = 11.sp, color = NeonCyan)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(
                                            listOf(
                                                "AI_INPAINT_ERASER" to "✨ AI Inpaint Clean",
                                                "EDGE_CROP" to "✂️ Edge Logo Crop",
                                                "SMART_BLUR_MASK" to "💧 Smart Mask Blur"
                                            )
                                        ) { (mKey, mLabel) ->
                                            val isSel = clip.watermarkMethod == mKey
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { viewModel.updateClipWatermarkMethod(clip.id, mKey) },
                                                label = { Text(mLabel, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // GPU & CPU Output Acceleration Bar Card
        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "⚡ GPU & CPU Hardware Acceleration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Vulkan 1.3 Hardware Video Codec • 8 CPU Cores Allocated", fontSize = 10.sp, color = AccentGreen)
                        }
                    }
                    SoraBadge(text = "3.2x SPEED", color = AccentGreen)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "GPU Acceleration", fontSize = 11.sp, color = TextSecondary)
                            Switch(
                                checked = project.gpuHardwareAcceleration,
                                onCheckedChange = { viewModel.toggleGpuHardwareAcceleration(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.scale(0.8f).testTag("gpu_accel_switch")
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "NPU Tensor Core", fontSize = 11.sp, color = TextSecondary)
                            Switch(
                                checked = project.npuTensorAcceleration,
                                onCheckedChange = { viewModel.toggleNpuTensorAcceleration(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = NeonPurple.copy(alpha = 0.4f)),
                                modifier = Modifier.scale(0.8f).testTag("npu_accel_switch")
                            )
                        }
                    }
                }
            }
        }

        // Export Render Button
        item {
            SoraGradientButton(
                text = "EXPORT CAPCUT VIDEO (${selectedResolution.label})",
                icon = Icons.Default.FileDownload,
                modifier = Modifier.fillMaxWidth().testTag("export_video_btn"),
                onClick = {
                    viewModel.exportEditorProject {
                        viewModel.selectTab(SoraTab.GALLERY)
                    }
                }
            )
        }
    }
}

@Composable
fun RatioChip(label: String, targetRatio: AspectRatioPreset, currentRatio: AspectRatioPreset, onClick: () -> Unit) {
    val isSelected = targetRatio == currentRatio
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan else GlassSurface)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) DeepDarkBg else TextSecondary)
    }
}

@Composable
fun ResChip(label: String, targetRes: ExportResolution, currentRes: ExportResolution, onClick: () -> Unit) {
    val isSelected = targetRes == currentRes
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonPurple else GlassSurface)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) TextPrimary else TextSecondary)
    }
}

@Composable
fun EditorStudioPlayerViewport(
    project: com.example.editor.VideoEditorProject,
    activeClip: MediaClipTrack?,
    selectedRatio: AspectRatioPreset,
    viewModel: SoraMainViewModel
) {
    val clipDuration = activeClip?.durationMs ?: if (project.videoClips.isNotEmpty()) project.videoClips.sumOf { it.durationMs } else 5000L
    val totalDurationMs = if (clipDuration > 0) clipDuration else 5000L

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadMs by remember { mutableStateOf(0L) }
    var isLooping by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    val clipTitle = activeClip?.title ?: if (project.videoClips.isNotEmpty()) project.videoClips.first().title else "No Clip Loaded"
    val baseHue = remember(clipTitle) {
        (clipTitle.hashCode() % 360).let { if (it < 0) it + 360 else it }.toFloat()
    }

    // Dynamic Playback Speed based on active clip's curve or playbackSpeed
    val speedMultiplier = activeClip?.playbackSpeed ?: 1.0f

    // Playback loop
    LaunchedEffect(isPlaying, totalDurationMs, speedMultiplier, isLooping) {
        if (isPlaying) {
            val stepMs = 50L
            while (isPlaying) {
                delay(stepMs)
                val increment = (stepMs * speedMultiplier).toLong()
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

    // Animation sweep
    val infiniteTransition = rememberInfiniteTransition(label = "editor_player_scanner")
    val scanPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_phase"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Top Header of Viewport
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Player Canvas • ${selectedRatio.label}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (activeClip != null && activeClip.filterName != "Normal") {
                    SoraBadge(text = activeClip.filterName, color = ElectricPink)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DeepDarkBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) AccentGreen else TextSecondary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isPlaying) "● LIVE" else "PAUSED",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isPlaying) AccentGreen else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Screen Canvas Viewport Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DeepDarkBg)
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            // Dynamic Video Rendering Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = (currentPlayheadMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                val dynamicHue = (baseHue + progress * 80f) % 360f

                // Color grading filter tinting
                val filterColor = when (activeClip?.filterName) {
                    "Cyberpunk Cyan" -> Color(0xFF00E5FF)
                    "Vintage Film" -> Color(0xFFFFB300)
                    "Noir Monochrome" -> Color(0xFF9E9E9E)
                    "Neon Vivid" -> Color(0xFFFF007F)
                    "CapCut Teal/Orange" -> Color(0xFF00B4D8)
                    else -> Color.hsl(dynamicHue, 0.75f, 0.25f)
                }

                val grad = Brush.radialGradient(
                    colors = listOf(
                        filterColor.copy(alpha = 0.75f),
                        Color.hsl((dynamicHue + 30f) % 360f, 0.5f, 0.12f),
                        DeepDarkBg
                    ),
                    center = Offset(size.width * (0.35f + 0.3f * progress), size.height * 0.5f),
                    radius = size.width * 0.75f
                )
                drawRect(brush = grad)

                // Scanline effect
                val scanY = size.height * scanPhase
                drawLine(
                    color = NeonCyan.copy(alpha = 0.2f),
                    start = Offset(0f, scanY),
                    end = Offset(size.width, scanY),
                    strokeWidth = 1.5f
                )

                // Aspect Ratio Framing Pillarboxes/Letterboxes
                when (selectedRatio) {
                    AspectRatioPreset.RATIO_9_16 -> {
                        val verticalWidth = size.height * (9f / 16f)
                        val sideMargin = (size.width - verticalWidth) / 2f
                        if (sideMargin > 0f) {
                            drawRect(color = Color.Black.copy(alpha = 0.65f), size = androidx.compose.ui.geometry.Size(sideMargin, size.height))
                            drawRect(
                                color = Color.Black.copy(alpha = 0.65f),
                                topLeft = Offset(size.width - sideMargin, 0f),
                                size = androidx.compose.ui.geometry.Size(sideMargin, size.height)
                            )
                            drawRect(
                                color = NeonCyan.copy(alpha = 0.4f),
                                topLeft = Offset(sideMargin, 0f),
                                size = androidx.compose.ui.geometry.Size(verticalWidth, size.height),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                    AspectRatioPreset.RATIO_1_1 -> {
                        val squareSide = size.height
                        val sideMargin = (size.width - squareSide) / 2f
                        if (sideMargin > 0f) {
                            drawRect(color = Color.Black.copy(alpha = 0.6f), size = androidx.compose.ui.geometry.Size(sideMargin, size.height))
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(size.width - sideMargin, 0f),
                                size = androidx.compose.ui.geometry.Size(sideMargin, size.height)
                            )
                        }
                    }
                    else -> {}
                }
            }

            // Overlay Metadata Badges
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag: Active Clip Name & FX indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "🎬 $clipTitle",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (activeClip?.velocityCurve != null && activeClip.velocityCurve != "NONE") {
                            Surface(shape = RoundedCornerShape(4.dp), color = ElectricPink.copy(alpha = 0.85f)) {
                                Text(
                                    text = "⚡ ${activeClip.velocityCurve}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (activeClip?.aiStyleEffect != null && activeClip.aiStyleEffect != "NONE") {
                            Surface(shape = RoundedCornerShape(4.dp), color = NeonPurple.copy(alpha = 0.85f)) {
                                Text(
                                    text = "🌟 ${activeClip.aiStyleEffect}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Center Big Play/Pause Toggle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(DeepDarkBg.copy(alpha = 0.7f))
                        .border(1.dp, NeonCyan, CircleShape)
                        .clickable { isPlaying = !isPlaying }
                        .testTag("editor_player_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Bottom Subtitle & Timecode overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Kinetic Subtitle banner if captions active on clip
                    if (!activeClip?.textOverlay.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentGreen)
                        ) {
                            Text(
                                text = "💬 ${activeClip!!.textOverlay}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Timecode HUD
                    val curSec = currentPlayheadMs / 1000f
                    val totSec = totalDurationMs / 1000f
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = String.format("%02d:%04.1fs / %02d:%04.1fs", (curSec / 60).toInt(), curSec % 60, (totSec / 60).toInt(), totSec % 60),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrubber Slider
        Slider(
            value = currentPlayheadMs.toFloat(),
            onValueChange = { currentPlayheadMs = it.toLong() },
            valueRange = 0f..totalDurationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = GlassSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .testTag("editor_player_scrubber")
        )

        // Transport Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { currentPlayheadMs = (currentPlayheadMs - 500L).coerceAtLeast(0L) },
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(GlassSurfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.Replay5, contentDescription = "Step -0.5s", tint = TextPrimary, modifier = Modifier.size(14.dp))
                }

                FilledIconButton(
                    onClick = { isPlaying = !isPlaying },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isPlaying) ElectricPink else NeonCyan),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = DeepDarkBg,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { currentPlayheadMs = (currentPlayheadMs + 500L).coerceAtMost(totalDurationMs) },
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(GlassSurfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.Forward5, contentDescription = "Step +0.5s", tint = TextPrimary, modifier = Modifier.size(14.dp))
                }

                IconButton(
                    onClick = { isLooping = !isLooping },
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(if (isLooping) NeonCyan.copy(alpha = 0.2f) else GlassSurfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.Repeat, contentDescription = "Loop", tint = if (isLooping) NeonCyan else TextSecondary, modifier = Modifier.size(14.dp))
                }
            }

            // Quick Info summary
            Text(
                text = "${project.videoClips.size} Clip(s) • ${speedMultiplier}x Speed",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

