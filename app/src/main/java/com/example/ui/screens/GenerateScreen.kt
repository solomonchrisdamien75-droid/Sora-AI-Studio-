package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.data.GalleryItemEntity

@Composable
fun GenerateScreen(viewModel: SoraMainViewModel) {
    val form by viewModel.generationForm.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val latestResult by viewModel.latestGeneratedResult.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            SoraSectionHeader(
                title = "AI Studio Workbench",
                subtitle = "Configure generation mode and parameters",
                icon = Icons.Default.VideoCall
            )
        }

        // Output Generation Result Preview Card (Appears instantly on completion)
        val result = latestResult
        if (result != null) {
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
                            Text(text = "Generation Successful!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.dismissLatestGeneratedResult() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurfaceVariant)
                            .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SoraBadge(text = result.resolutionLabel, color = AccentGreen)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "READY TO PLAY", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column {
                                Text(text = result.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = result.prompt, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.addClipToEditor(result.filePath, result.title)
                                viewModel.selectTab(SoraTab.EDITOR)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("edit_generated_in_studio_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepDarkBg)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit in Studio", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.selectTab(SoraTab.GALLERY) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Gallery", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Error message if any
        val err = form.errorMessage
        if (err != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentRed.copy(alpha = 0.2f))
                        .border(1.dp, AccentRed, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(text = err, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Active Real-time Render Progress Monitor
        val job = activeJob
        if (job != null && job.status == "RUNNING") {
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { job.progressPercent / 100f },
                                modifier = Modifier.size(24.dp),
                                color = NeonCyan,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rendering Frames: ${job.currentFrame} / ${job.totalFrames}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        SoraBadge(text = "${job.progressPercent}%", color = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { job.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonCyan,
                        trackColor = GlassSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "FPS: ${String.format("%.1f", job.fps)}", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "Backend: ${job.backendUsed}", fontSize = 12.sp, color = NeonPurple)
                        Text(text = "Resolution: ${job.resolution}", fontSize = 12.sp, color = TextSecondary)
                    }

                    if (form.isSegmented) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "⚡ Segmented Scene Assembly Active (Auto Stitching)", fontSize = 11.sp, color = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.togglePauseRender() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (form.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (form.isPaused) "Resume" else "Pause", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.saveCheckpoint() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (form.checkpointSaved) "Checkpoint Saved" else "Save Checkpoint", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelActiveJob(job.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("cancel_render_button")
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Mode Switcher (All 30 Core Modes Categorized)
        item {
            Column {
                Text(text = "Generation Mode (30 AI Engines)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Video & Film Generation Modes
                Text(text = "Video & Cinema", fontSize = 11.sp, color = NeonCyan)
                // 🔥 Manhwa & Webtoon Studio
                Text(text = "🔥 Manhwa & Webtoon Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricPink)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { TypeChip("🔥 Manhwa Recap Studio", "MANHWA_RECAP", form.generationType) { viewModel.updateGenerationType("MANHWA_RECAP") } }
                    item { TypeChip("Lip Sync Engine", "MANHWA_LIP_SYNC", form.generationType) { viewModel.updateGenerationType("MANHWA_LIP_SYNC") } }
                    item { TypeChip("Story Continuation", "MANHWA_CONTINUATION", form.generationType) { viewModel.updateGenerationType("MANHWA_CONTINUATION") } }
                    item { TypeChip("Action Audio Filter", "ACTION_AUDIO_FILTER", form.generationType) { viewModel.updateGenerationType("ACTION_AUDIO_FILTER") } }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Standard Video Generation Modes
                Text(text = "Standard Video Generation", fontSize = 11.sp, color = NeonCyan)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { TypeChip("Text-to-Video", "TEXT_TO_VIDEO", form.generationType) { viewModel.updateGenerationType("TEXT_TO_VIDEO") } }
                    item { TypeChip("Image-to-Video", "IMAGE_TO_VIDEO", form.generationType) { viewModel.updateGenerationType("IMAGE_TO_VIDEO") } }
                    item { TypeChip("Video-to-Video", "VIDEO_TO_VIDEO", form.generationType) { viewModel.updateGenerationType("VIDEO_TO_VIDEO") } }
                    item { TypeChip("Short Film", "SHORT_FILM", form.generationType) { viewModel.updateGenerationType("SHORT_FILM") } }
                    item { TypeChip("Long Film", "LONG_FILM", form.generationType) { viewModel.updateGenerationType("LONG_FILM") } }
                    item { TypeChip("YouTube Generator", "YOUTUBE_GEN", form.generationType) { viewModel.updateGenerationType("YOUTUBE_GEN") } }
                    item { TypeChip("Realistic Movie", "REALISTIC_MOVIE", form.generationType) { viewModel.updateGenerationType("REALISTIC_MOVIE") } }
                    item { TypeChip("Documentary", "DOCUMENTARY", form.generationType) { viewModel.updateGenerationType("DOCUMENTARY") } }
                    item { TypeChip("Music Video", "MUSIC_VIDEO", form.generationType) { viewModel.updateGenerationType("MUSIC_VIDEO") } }
                    item { TypeChip("Anime", "ANIME", form.generationType) { viewModel.updateGenerationType("ANIME") } }
                    item { TypeChip("Cartoon", "CARTOON", form.generationType) { viewModel.updateGenerationType("CARTOON") } }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Image & Editing AI Modes
                Text(text = "Image & Enhancements", fontSize = 11.sp, color = NeonPurple)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { TypeChip("Image Generation", "IMAGE_GEN", form.generationType) { viewModel.updateGenerationType("IMAGE_GEN") } }
                    item { TypeChip("AI Image Editing", "IMAGE_EDIT", form.generationType) { viewModel.updateGenerationType("IMAGE_EDIT") } }
                    item { TypeChip("AI Upscaling", "UPSCALING", form.generationType) { viewModel.updateGenerationType("UPSCALING") } }
                    item { TypeChip("AI Inpainting", "INPAINTING", form.generationType) { viewModel.updateGenerationType("INPAINTING") } }
                    item { TypeChip("AI Outpainting", "OUTPAINTING", form.generationType) { viewModel.updateGenerationType("OUTPAINTING") } }
                    item { TypeChip("Background Removal", "BG_REMOVAL", form.generationType) { viewModel.updateGenerationType("BG_REMOVAL") } }
                    item { TypeChip("Motion Transfer", "MOTION_TRANSFER", form.generationType) { viewModel.updateGenerationType("MOTION_TRANSFER") } }
                    item { TypeChip("Video Enhancement", "VIDEO_ENHANCE", form.generationType) { viewModel.updateGenerationType("VIDEO_ENHANCE") } }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Story, Scripting & Offline Speech AI
                Text(text = "Story, Script & Voice AI", fontSize = 11.sp, color = ElectricPink)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { TypeChip("Story Generator", "STORY_GEN", form.generationType) { viewModel.updateGenerationType("STORY_GEN") } }
                    item { TypeChip("Script Writer", "SCRIPT_WRITER", form.generationType) { viewModel.updateGenerationType("SCRIPT_WRITER") } }
                    item { TypeChip("Scene Builder", "SCENE_BUILDER", form.generationType) { viewModel.updateGenerationType("SCENE_BUILDER") } }
                    item { TypeChip("Shot Planner", "SHOT_PLANNER", form.generationType) { viewModel.updateGenerationType("SHOT_PLANNER") } }
                    item { TypeChip("Character Creator", "CHARACTER_CREATOR", form.generationType) { viewModel.updateGenerationType("CHARACTER_CREATOR") } }
                    item { TypeChip("Offline Voice Clone", "VOICE_CLONE", form.generationType) { viewModel.updateGenerationType("VOICE_CLONE") } }
                    item { TypeChip("Voice Generator", "VOICE_GEN", form.generationType) { viewModel.updateGenerationType("VOICE_GEN") } }
                    item { TypeChip("Subtitle Generator", "SUBTITLES", form.generationType) { viewModel.updateGenerationType("SUBTITLES") } }
                    item { TypeChip("AI Translation", "TRANSLATION", form.generationType) { viewModel.updateGenerationType("TRANSLATION") } }
                    item { TypeChip("Lip Sync", "LIP_SYNC", form.generationType) { viewModel.updateGenerationType("LIP_SYNC") } }
                }
            }
        }

        // Title Input
        item {
            OutlinedTextField(
                value = form.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Project Title") },
                modifier = Modifier.fillMaxWidth().testTag("title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassSurfaceVariant,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Prompt Input
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Prompt Input", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    TextButton(onClick = {
                        viewModel.updatePrompt("${form.prompt}, 8k octane render, volumetric lighting, masterpiece cinematic frame")
                    }) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Enhance Prompt", fontSize = 12.sp, color = NeonCyan)
                    }
                }

                OutlinedTextField(
                    value = form.prompt,
                    onValueChange = { viewModel.updatePrompt(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassSurfaceVariant,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        val isManhwaMode = form.generationType in listOf("MANHWA_RECAP", "MANHWA_LIP_SYNC", "MANHWA_CONTINUATION", "ACTION_AUDIO_FILTER")

        if (isManhwaMode) {
            item {
                ManhwaRecapStudioSection(viewModel = viewModel, form = form)
            }
        }

        // Dynamic Asset Input Section based on Generation Type
        val requiresImageInput = form.generationType in listOf("IMAGE_TO_VIDEO", "IMAGE_GEN", "IMAGE_EDIT", "INPAINTING", "OUTPAINTING", "BG_REMOVAL", "MOTION_TRANSFER", "LIP_SYNC")
        val requiresVideoInput = form.generationType in listOf("VIDEO_TO_VIDEO", "VIDEO_ENHANCE", "UPSCALING")
        val requiresAudioInput = form.generationType in listOf("VOICE_CLONE", "VOICE_GEN", "SUBTITLES", "TRANSLATION", "LIP_SYNC")
        val requiresCharacterInput = form.generationType in listOf("CHARACTER_CREATOR", "STORY_GEN", "SCRIPT_WRITER", "SCENE_BUILDER", "SHOT_PLANNER")

        if (requiresImageInput) {
            item {
                val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                    viewModel.updateSourceImageUri(uri?.toString())
                }

                SoraGlassCard(borderColor = NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Source Image Asset", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Upload reference image for video generation or editing", fontSize = 11.sp, color = TextSecondary)
                        }

                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upload_image_btn")
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Image", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val imgUri = form.sourceImageUri
                    if (imgUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Selected Source Image", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = imgUri, fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                IconButton(onClick = { viewModel.updateSourceImageUri(null) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        Text(text = "Preset Sample Images:", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceImageUri("assets/samples/cyberpunk_portrait.png") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Cyberpunk Portrait", fontSize = 10.sp)
                                }
                            }
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceImageUri("assets/samples/alien_landscape.jpg") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Alien Landscape", fontSize = 10.sp)
                                }
                            }
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceImageUri("assets/samples/anime_character.png") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Anime Character", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (form.generationType == "INPAINTING" || form.generationType == "OUTPAINTING") {
                        Spacer(modifier = Modifier.height(12.dp))
                        val maskPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                            viewModel.updateMaskImageUri(uri?.toString())
                        }
                        Text(text = "Inpainting Mask Layer", fontSize = 12.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { maskPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(6.dp)) {
                                Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Mask Layer", fontSize = 10.sp)
                            }
                            OutlinedButton(onClick = { viewModel.updateMaskImageUri("assets/masks/center_subject.png") }, shape = RoundedCornerShape(6.dp)) {
                                Text("Center Mask Preset", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        if (requiresVideoInput) {
            item {
                val videoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                    viewModel.updateSourceVideoUri(uri?.toString())
                }

                SoraGlassCard(borderColor = NeonPurple) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Source Video File Asset", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Upload input video for AI video-to-video or upscaling", fontSize = 11.sp, color = TextSecondary)
                        }

                        OutlinedButton(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upload_video_btn")
                        ) {
                            Icon(imageVector = Icons.Default.VideoFile, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Video", fontSize = 11.sp, color = NeonPurple)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val vidUri = form.sourceVideoUri
                    if (vidUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Selected Source Video", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = vidUri, fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                IconButton(onClick = { viewModel.updateSourceVideoUri(null) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        Text(text = "Preset Sample Video Clips:", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceVideoUri("assets/samples/drone_flight_1080p.mp4") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Drone Flight (1080p)", fontSize = 10.sp)
                                }
                            }
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceVideoUri("assets/samples/street_walk_720p.mp4") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Street Walk (720p)", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val editorProject by viewModel.editorProject.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Auto-Erase Watermarks on Upload", fontSize = 12.sp, color = TextPrimary)
                        }
                        Switch(
                            checked = editorProject.globalWatermarkEraser,
                            onCheckedChange = { viewModel.toggleGlobalWatermarkEraser(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricPink, checkedTrackColor = ElectricPink.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("gen_watermark_eraser_switch")
                        )
                    }
                }
            }
        }

        if (requiresAudioInput) {
            item {
                val audioPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
                    viewModel.updateSourceAudioUri(uri?.toString())
                }

                SoraGlassCard(borderColor = ElectricPink) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Source Voice / Audio Asset", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Upload audio specimen for voice cloning, subtitles, or lip sync", fontSize = 11.sp, color = TextSecondary)
                        }

                        OutlinedButton(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upload_audio_btn")
                        ) {
                            Icon(imageVector = Icons.Default.AudioFile, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Audio", fontSize = 11.sp, color = ElectricPink)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val audUri = form.sourceAudioUri
                    if (audUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Selected Voice Audio Sample", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = audUri, fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                IconButton(onClick = { viewModel.updateSourceAudioUri(null) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        Text(text = "Preset Sample Voice Audio:", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceAudioUri("assets/samples/voice_specimen_a.wav") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Voice Specimen A (WAV)", fontSize = 10.sp)
                                }
                            }
                            item {
                                OutlinedButton(onClick = { viewModel.updateSourceAudioUri("assets/samples/narration_en.mp3") }, shape = RoundedCornerShape(6.dp)) {
                                    Text("Narration English (MP3)", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (requiresCharacterInput) {
            item {
                SoraGlassCard(borderColor = AccentGreen) {
                    Text(text = "Character Profile & Lore Context", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Provide persona, appearance, traits, and background lore for story generation.", fontSize = 11.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = form.characterProfileText ?: "",
                        onValueChange = { viewModel.updateCharacterProfileText(it) },
                        placeholder = { Text("e.g. Captain Vance, 34-year-old shuttle pilot with cybernetic left eye...") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = GlassSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Character Archetype Presets:", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            OutlinedButton(onClick = { viewModel.updateCharacterProfileText("Cyberpunk Hacker: Female, neon hair, wearing augmented reality visor, expert stealth coder.") }, shape = RoundedCornerShape(6.dp)) {
                                Text("Cyberpunk Hacker", fontSize = 10.sp)
                            }
                        }
                        item {
                            OutlinedButton(onClick = { viewModel.updateCharacterProfileText("Sci-Fi Explorer: Male, space suit, rugged armor, dual energy sidearms, stoic commander.") }, shape = RoundedCornerShape(6.dp)) {
                                Text("Sci-Fi Explorer", fontSize = 10.sp)
                            }
                        }
                        item {
                            OutlinedButton(onClick = { viewModel.updateCharacterProfileText("Fantasy Sorcerer: Aged mage, glowing blue robes, ancient rune staff, elemental fire spellcaster.") }, shape = RoundedCornerShape(6.dp)) {
                                Text("Fantasy Sorcerer", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quality Modes (Fast Mode, Balanced Mode, Cinema Mode)
        item {
            Text(text = "Quality Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityModeCard(
                    title = "Fast Mode",
                    desc = "Lowest RAM (LiteRT)",
                    modeKey = "FAST",
                    selectedMode = form.mode,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                ) { viewModel.updateMode("FAST") }

                QualityModeCard(
                    title = "Balanced",
                    desc = "Medium Quality (ONNX)",
                    modeKey = "BALANCED",
                    selectedMode = form.mode,
                    color = NeonPurple,
                    modifier = Modifier.weight(1f)
                ) { viewModel.updateMode("BALANCED") }

                QualityModeCard(
                    title = "Cinema Mode",
                    desc = "High Quality (Vulkan)",
                    modeKey = "CINEMA",
                    selectedMode = form.mode,
                    color = ElectricPink,
                    modifier = Modifier.weight(1f)
                ) { viewModel.updateMode("CINEMA") }
            }
        }

        // Duration, Aspect Ratio & Resolution Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Duration selection (1s to Several Hours)
                Text(text = "Target Video Duration", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { DurationChip("1s", 1, form.durationSec) { viewModel.updateDurationWithLabel("1 second", 1) } }
                    item { DurationChip("5s", 5, form.durationSec) { viewModel.updateDurationWithLabel("5 seconds", 5) } }
                    item { DurationChip("10s", 10, form.durationSec) { viewModel.updateDurationWithLabel("10 seconds", 10) } }
                    item { DurationChip("30s", 30, form.durationSec) { viewModel.updateDurationWithLabel("30 seconds", 30) } }
                    item { DurationChip("1 min", 60, form.durationSec) { viewModel.updateDurationWithLabel("1 minute", 60) } }
                    item { DurationChip("5 min", 300, form.durationSec) { viewModel.updateDurationWithLabel("5 minutes", 300) } }
                    item { DurationChip("10 min", 600, form.durationSec) { viewModel.updateDurationWithLabel("10 minutes", 600) } }
                    item { DurationChip("30 min", 1800, form.durationSec) { viewModel.updateDurationWithLabel("30 minutes", 1800) } }
                    item { DurationChip("1 hour", 3600, form.durationSec) { viewModel.updateDurationWithLabel("1 hour", 3600) } }
                    item { DurationChip("Several Hours", 10800, form.durationSec) { viewModel.updateDurationWithLabel("3 hours (Segmented)", 10800) } }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Aspect Ratio
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Aspect Ratio", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { ResolutionChip("16:9", form.aspectRatio) { viewModel.updateAspectRatio("16:9") } }
                            item { ResolutionChip("9:16", form.aspectRatio) { viewModel.updateAspectRatio("9:16") } }
                            item { ResolutionChip("1:1", form.aspectRatio) { viewModel.updateAspectRatio("1:1") } }
                            item { ResolutionChip("4:3", form.aspectRatio) { viewModel.updateAspectRatio("4:3") } }
                            item { ResolutionChip("21:9", form.aspectRatio) { viewModel.updateAspectRatio("21:9") } }
                        }
                    }

                    // Resolution
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Resolution", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item { ResolutionChip("480p", form.resolution) { viewModel.updateResolution("480p") } }
                            item { ResolutionChip("720p", form.resolution) { viewModel.updateResolution("720p") } }
                            item { ResolutionChip("1080p", form.resolution) { viewModel.updateResolution("1080p") } }
                            item { ResolutionChip("4K", form.resolution) { viewModel.updateResolution("4K") } }
                        }
                    }
                }
            }
        }

        // Launch Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SoraGradientButton(
                text = if (form.isGenerating) "Generation In Progress..." else "START AI GENERATION",
                icon = Icons.Default.PlayArrow,
                enabled = !form.isGenerating,
                modifier = Modifier.fillMaxWidth().testTag("start_generation_button"),
                onClick = { viewModel.startGeneration() }
            )
        }
    }
}

@Composable
fun TypeChip(label: String, typeKey: String, selectedType: String, onClick: () -> Unit) {
    val isSelected = typeKey == selectedType
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else GlassSurface)
            .border(1.dp, if (isSelected) NeonCyan else GlassSurfaceVariant, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) DeepDarkBg else TextPrimary
        )
    }
}

@Composable
fun QualityModeCard(
    title: String,
    desc: String,
    modeKey: String,
    selectedMode: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSelected = modeKey == selectedMode
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else GlassSurface)
            .border(2.dp, if (isSelected) color else GlassSurfaceVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) color else TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = desc, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun DurationChip(label: String, sec: Int, selectedSec: Int, onClick: () -> Unit) {
    val isSelected = sec == selectedSec
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
fun ResolutionChip(label: String, selectedRes: String, onClick: () -> Unit) {
    val isSelected = label == selectedRes
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
fun ManhwaRecapStudioSection(
    viewModel: SoraMainViewModel,
    form: com.example.ui.GenerationFormState
) {
    val panelPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.addManhwaPanel(
                title = "Uploaded Panel ${form.manhwaPanels.size + 1}",
                panelType = "COMBAT",
                actionDesc = "Dynamic action panel imported from gallery",
                spokenDialogue = "Character dialogue",
                imageUri = uri.toString()
            )
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.updateManhwaVoiceoverUri(uri.toString())
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Studio Title Header & Style Selector
        SoraGlassCard(borderColor = ElectricPink) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.MovieFilter, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "🔥 Manhwa Recap Studio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Auto-animate panels, lip sync, filter redundant narration & continue stories", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = form.manhwaChapterTitle,
                onValueChange = { viewModel.updateManhwaChapterTitle(it) },
                label = { Text("Manhwa Title & Chapter") },
                modifier = Modifier.fillMaxWidth().testTag("manhwa_chapter_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricPink,
                    unfocusedBorderColor = GlassSurfaceVariant,
                    focusedLabelColor = ElectricPink,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Panel Animation & Motion Style:", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { TypeChip("Cinematic Flow", "MANHWA_CINEMATIC_FLOW", form.manhwaAnimationStyle) { viewModel.updateManhwaAnimationStyle("MANHWA_CINEMATIC_FLOW") } }
                item { TypeChip("Speed Lines & FX", "ANIME_SPEED_LINES", form.manhwaAnimationStyle) { viewModel.updateManhwaAnimationStyle("ANIME_SPEED_LINES") } }
                item { TypeChip("3D Parallax Zoom", "3D_PARALLAX_ZOOM", form.manhwaAnimationStyle) { viewModel.updateManhwaAnimationStyle("3D_PARALLAX_ZOOM") } }
                item { TypeChip("Dynamic Action", "DYNAMIC_ACTION", form.manhwaAnimationStyle) { viewModel.updateManhwaAnimationStyle("DYNAMIC_ACTION") } }
            }
        }

        // Section 1: Upload Manhwa Panels & Voice Cover Audio
        SoraGlassCard(borderColor = NeonCyan) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "1. Manhwa Panels & Voice Cover Tracks", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "${form.manhwaPanels.size} Panels loaded • Voice Cover attached", fontSize = 11.sp, color = TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { panelPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("upload_panel_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Panel", fontSize = 11.sp, color = Color.Black)
                    }
                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("upload_voiceover_btn")
                    ) {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice Cover", fontSize = 11.sp, color = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Audio Track Preview Card
            val voiceUri = form.manhwaVoiceoverUri
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassSurfaceVariant)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Voiceover Track: ${voiceUri?.substringAfterLast("/") ?: "narrator_dub.mp3"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Duration: 04:20 • Sync mode: Auto Panel Timing", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    SoraBadge(text = "AUDIO LOADED", color = ElectricPink)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Uploaded Comic Panels (${form.manhwaPanels.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                form.manhwaPanels.forEachIndexed { idx, panel ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(8.dp))
                            .background(GlassSurface)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = panel.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SoraBadge(
                                            text = panel.panelType,
                                            color = when(panel.panelType) {
                                                "COMBAT" -> AccentRed
                                                "DIALOGUE" -> NeonCyan
                                                else -> NeonPurple
                                            }
                                        )
                                    }
                                    Text(text = "Action: ${panel.actionDescription}", fontSize = 10.sp, color = TextSecondary)
                                    if (!panel.spokenDialogue.isNullOrBlank()) {
                                        Text(text = "💬 Speech: \"${panel.spokenDialogue}\"", fontSize = 10.sp, color = ElectricPink)
                                    }
                                }
                            }
                            IconButton(onClick = { viewModel.removeManhwaPanel(panel.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Panel", tint = AccentRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section 2: AI Lip Sync & Smart Action Voiceover Removal Engine
        SoraGlassCard(borderColor = NeonPurple) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "2. AI Auto-Animation, Lip Sync & Audio Filter Engine", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch 1: Smart Action Voiceover Removal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Remove Redundant Spoken Action Narration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = "AI automatically mutes narrator action phrases once rendered on screen (e.g. 'he slashes with shadow blades'). Keeps character speech with lip-sync and action SFX/noises.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = form.manhwaFilterActionNarration,
                    onCheckedChange = { viewModel.toggleManhwaAudioFilter(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("action_audio_filter_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch 2: Lip Syncing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "AI Character Lip-Sync Engine", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Animates facial expressions & mouth movements of characters in panels in sync with dialogue.", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = form.manhwaLipSyncEnabled,
                    onCheckedChange = { viewModel.toggleManhwaLipSync(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricPink, checkedTrackColor = ElectricPink.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("lip_sync_switch")
                )
            }
        }

        // Section 3: Resume Recap Progress Checkpoint
        SoraGlassCard(borderColor = AccentGreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "3. Auto-Saved Checkpoint (Resume Recap)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Paused at Panel ${form.manhwaCheckpointPanelIndex + 1} / ${form.manhwaPanels.size} • Time: 02:25s", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Button(
                    onClick = { viewModel.resumeManhwaRecapFromCheckpoint() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("resume_recap_checkpoint_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume Recap", fontSize = 11.sp, color = Color.Black)
                }
            }
        }

        // Section 4: AI Story Continuation Generator
        SoraGlassCard(borderColor = ElectricPink) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "4. AI Story Continuation Generator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generate original chapter story continuations beyond where the manhwa ended. AI writes continuation scripts, designs panel action prompts, and appends new panels automatically.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = form.manhwaContinuationPrompt,
                onValueChange = { viewModel.updateManhwaContinuationPrompt(it) },
                label = { Text("Story Continuation Prompt") },
                modifier = Modifier.fillMaxWidth().testTag("continuation_prompt_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricPink,
                    unfocusedBorderColor = GlassSurfaceVariant,
                    focusedLabelColor = ElectricPink,
                    unfocusedLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.generateManhwaStoryContinuation() },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("generate_continuation_btn")
            ) {
                if (form.isGeneratingManhwaContinuation) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Writing Chapter Continuation...", fontSize = 12.sp)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✨ Generate Story Continuation & Append Panels", fontSize = 12.sp, color = Color.White)
                }
            }

            val contScript = form.manhwaContinuationScript
            if (!contScript.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(text = contScript, fontSize = 11.sp, color = TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}
