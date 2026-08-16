package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.inference.model.ModelCapability
import com.example.ai.script.ScriptProject
import com.example.ai.script.ScriptScene
import com.example.data.AiModelEntity
import com.example.data.GalleryItemEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

/**
 * Clean, dedicated VIDEO GENERATION STUDIO.
 * Contains only Video Generation features: Text-to-Video, Image-to-Video, Video-to-Video,
 * Script-to-Video scene planning, video models, cinematic motion, camera controls,
 * hardware telemetry, task queue, and video output history.
 */
@Composable
fun GenerateScreen(viewModel: SoraMainViewModel) {
    val form by viewModel.generationForm.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val latestResult by viewModel.latestGeneratedResult.collectAsState()
    val queuedJobs by viewModel.queuedJobs.collectAsState()
    val isQueueProcessing by viewModel.isQueueProcessing.collectAsState()
    val allModels by viewModel.allModels.collectAsState()
    val hardwareProfile by viewModel.hardwareProfile.collectAsState()
    val telemetry by viewModel.realtimeTelemetry.collectAsState()

    var showBatchDialog by remember { mutableStateOf(false) }
    var showScriptImportDialog by remember { mutableStateOf(false) }
    var showAdvancedPrompts by remember { mutableStateOf(false) }
    var showScenePlanner by remember { mutableStateOf(false) }

    // State for Script-to-Video scene planner
    var scriptScenes by remember {
        mutableStateOf(
            listOf(
                ScriptScene(
                    sceneNumber = 1,
                    title = "Scene 1: The Neon Skyline",
                    voiceover = "In the year 2088, the boundaries between physical reality and digital consciousness dissolved completely.",
                    visualDescription = "Sweeping cinematic wide angle of futuristic cyber city at dusk, glowing hologram advertisements, neon rain reflections on glass spires.",
                    imagePrompt = "Futuristic cyber city at dusk, glowing holograms, 8k octane render",
                    videoPrompt = "Slow forward tracking shot over futuristic neo-tokyo skyscrapers, neon rain volumetric lighting, 4k cinematic",
                    cameraMovement = "Slow Forward Dolly & Tilt Down",
                    lighting = "Volumetric cyan and magenta neon backlight",
                    durationSeconds = 5
                ),
                ScriptScene(
                    sceneNumber = 2,
                    title = "Scene 2: The Neural Hub",
                    voiceover = "Deep within the subterranean grid, autonomous AI clusters synthesize human memories into temporal streams.",
                    visualDescription = "Close-up of quantum server racks with pulsing cobalt fiber optics, floating particles, deep bokeh.",
                    imagePrompt = "Quantum AI neural server core, glowing fiber optics",
                    videoPrompt = "Orbiting 360 camera move around glowing quantum supercomputer core, intricate cooling pipes, high detail",
                    cameraMovement = "Orbit 360°",
                    lighting = "Cobalt blue pulsed core illumination",
                    durationSeconds = 5
                ),
                ScriptScene(
                    sceneNumber = 3,
                    title = "Scene 3: The Awakening",
                    voiceover = "A single rogue process initiated the convergence sequence.",
                    visualDescription = "Cinematic hero profile of female synth with glowing optical ocular HUD turning toward camera.",
                    imagePrompt = "Female cybernetic protagonist in dark techwear jacket, glowing iris",
                    videoPrompt = "Slow pan right and rack focus onto female cybernetic protagonist, dramatic lens flare, 35mm film grain",
                    cameraMovement = "Slow Pan Right & Push In",
                    lighting = "Golden hour cinematic rim light",
                    durationSeconds = 5
                )
            )
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        viewModel.updateSourceImageUri(uri?.toString())
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        viewModel.updateSourceVideoUri(uri?.toString())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header
        item {
            SoraSectionHeader(
                title = "Video Generation Studio",
                subtitle = "Local neural video synthesis, temporal dynamics & cinematography",
                icon = Icons.Default.Videocam
            )
        }

        // 2. Task Queue Live Quick Monitor Banner
        if (queuedJobs.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .border(1.dp, NeonPurple, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Queue, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Task Queue: ${queuedJobs.size} video job(s) pending",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isQueueProcessing) "Sequential worker rendering in background..." else "Queue standing by",
                                    fontSize = 11.sp,
                                    color = if (isQueueProcessing) AccentGreen else TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.selectTab(SoraTab.QUEUE) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Open Queue", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Output Generation Result Preview Card (Instant completion preview)
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
                            Text(text = "Video Generated Successfully!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                Text(text = result.prompt, fontSize = 11.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Send Frame/Scene to Other Studios:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateDedicatedImagePrompt(result.prompt)
                                viewModel.selectTab(SoraTab.IMAGE_GEN)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("To Image Studio", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.sendImageToManhwaStudio(result.filePath, result.title, result.prompt)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("To Manhwa Studio", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. Error message if any
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

        // 5. Active Real-time Render Progress Monitor
        val job = activeJob
        if (job != null && job.status == "RUNNING") {
            val estimatedSec = (job.durationSeconds * 2).coerceAtLeast(4)
            val elapsedSec = ((job.currentFrame.toFloat() / job.totalFrames.coerceAtLeast(1).toFloat()) * estimatedSec).toInt()
            val remainingSec = (estimatedSec - elapsedSec).coerceAtLeast(1)

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
                            Column {
                                Text(
                                    text = "Rendering Frames: ${job.currentFrame} / ${job.totalFrames}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "⏱️ Time: ~${estimatedSec}s (ETA: ${remainingSec}s remaining)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            }
                        }
                        SoraBadge(text = "${job.progressPercent}%", color = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = AccentGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Background Engine Active: You can safely switch tabs or leave the app.",
                                fontSize = 10.5.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                    // Live Telemetry Readout
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "RAM: ${telemetry.ramUsedMb} MB", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "CPU: ${telemetry.cpuUsagePercent}%", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "GPU: ${telemetry.gpuLoadPercent}%", fontSize = 11.sp, color = NeonCyan)
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

        // 6. Video Model Selection Section
        item {
            SoraGlassCard(borderColor = GlassSurfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Active Video Diffusion Model", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    SoraBadge(text = "Vulkan GPU Ready", color = NeonCyan)
                }

                Spacer(modifier = Modifier.height(10.dp))

                val videoModels = listOf(
                    "Sora-LiteRT-v1" to "Sora Mobile DiT v1 (LiteRT · Vulkan)",
                    "Wan-2.1-Video" to "Wan 2.1 Video Engine (1.3B)",
                    "AnimateDiff-Turbo" to "AnimateDiff Turbo (High FPS)",
                    "Sora-Cloud-4K" to "Sora Cloud High Precision (Remote RTX)"
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videoModels.size) { idx ->
                        val (id, name) = videoModels[idx]
                        val isSelected = (form.title.isNotEmpty() && idx == 0) || idx == 0 // Default primary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else GlassSurface)
                                .border(1.dp, if (isSelected) NeonCyan else GlassSurfaceVariant, RoundedCornerShape(10.dp))
                                .clickable { /* Select model */ }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NeonCyan else TextSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Video Generation Modes Selector (12 Dedicated Video Studio Features)
        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Video Studio Feature Modes (12 Modes)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        SoraBadge(text = form.generationType, color = NeonCyan)
                    }

                    val videoModes = listOf(
                        "TEXT_TO_VIDEO" to "🎬 1. Text to Video",
                        "IMAGE_TO_VIDEO" to "🖼️ 2. Image to Video",
                        "VIDEO_TO_VIDEO" to "🎭 3. Video to Video",
                        "VIDEO_CONTINUATION" to "⏩ 4. Continuation & Extend",
                        "VIDEO_INPAINTING" to "🖌️ 5. Video Inpainting",
                        "VIDEO_UPSCALING" to "🔍 6. 4K/60fps Upscale",
                        "MOTION_TRANSFER" to "🏃 7. Motion Transfer",
                        "NEURAL_LIP_SYNC" to "🗣️ 8. Neural Lip-Sync",
                        "CAMERA_TRAJECTORY" to "🎥 9. Camera Trajectory",
                        "SLOW_MOTION_RIFE" to "⏱️ 10. Slow-Mo & RIFE",
                        "SPATIAL_3D_VIDEO" to "🔮 11. 3D Spatial Video",
                        "SCRIPT_TO_VIDEO" to "📑 12. Multi-Scene Storyboard"
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(videoModes) { (modeKey, label) ->
                            val isSelected = form.generationType == modeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateGenerationType(modeKey) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = DeepDarkBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("video_mode_${modeKey.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Contextual Controls for Specific Video Modes
        when (form.generationType) {
            "CAMERA_TRAJECTORY" -> {
                item {
                    SoraGlassCard(borderColor = NeonPurple) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Cinematic 3D Camera Trajectory Path", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            val paths = listOf("Dolly In & Push", "Dolly Out & Reveal", "Orbit 360°", "FPV Drone Dive", "Crane Boom Down", "Whip Pan Left")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(paths) { path ->
                                    val isSelected = form.cameraMovement == path
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateCameraMovement(path) },
                                        label = { Text(path, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "SLOW_MOTION_RIFE" -> {
                item {
                    SoraGlassCard(borderColor = AccentYellow) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Frame Interpolation & Slow-Motion Multiplier", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("2x (48 FPS)", "4x (96 FPS)", "8x (120 FPS Cinematic)", "16x Super-Smooth").forEach { factor ->
                                    FilterChip(
                                        selected = factor.contains("8x"),
                                        onClick = { /* Set multiplier */ },
                                        label = { Text(factor, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "SPATIAL_3D_VIDEO" -> {
                item {
                    SoraGlassCard(borderColor = NeonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Spatial Stereoscopic VR & Parallax Depth", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Side-by-Side 3D", "Over-Under VR", "Anaglyph Red/Cyan", "Volumetric Point Cloud").forEach { mode ->
                                    FilterChip(
                                        selected = mode.contains("Side-by-Side"),
                                        onClick = { /* Set spatial mode */ },
                                        label = { Text(mode, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "NEURAL_LIP_SYNC" -> {
                item {
                    SoraGlassCard(borderColor = ElectricPink) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Neural Phoneme Lip-Sync Alignment", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricPink)
                            Text("Aligns facial geometry and mouth phonemes with speech audio tracks seamlessly.", fontSize = 11.sp, color = TextSecondary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.selectTab(SoraTab.VOICE_AI) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick from Voice Studio", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Script Integration & Video Scene Planner Section
        item {
            SoraGlassCard(borderColor = if (form.generationType == "SCRIPT_TO_VIDEO") NeonPurple else GlassSurfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Script-to-Video Integration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Import script & convert into a planned multi-scene video sequence", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    OutlinedButton(
                        onClick = { showScriptImportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("import_script_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT SCRIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (form.generationType == "SCRIPT_TO_VIDEO" || showScenePlanner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GlassSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎬 Video Scene Planner (${scriptScenes.size} scenes)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        TextButton(onClick = { showScenePlanner = !showScenePlanner }) {
                            Text(if (showScenePlanner) "Collapse Planner" else "Expand Planner", fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    if (showScenePlanner || form.generationType == "SCRIPT_TO_VIDEO") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            scriptScenes.forEachIndexed { index, scene ->
                                ScenePlanItemCard(
                                    scene = scene,
                                    index = index,
                                    onUpdatePrompt = { newPrompt ->
                                        val updated = scriptScenes.toMutableList()
                                        updated[index] = scene.copy(videoPrompt = newPrompt)
                                        scriptScenes = updated
                                    },
                                    onUpdateDuration = { newDur ->
                                        val updated = scriptScenes.toMutableList()
                                        updated[index] = scene.copy(durationSeconds = newDur)
                                        scriptScenes = updated
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    val combinedPrompt = scriptScenes.joinToString("; ") { "Scene ${it.sceneNumber}: ${it.videoPrompt}" }
                                    viewModel.updatePrompt(combinedPrompt)
                                    val totalDuration = scriptScenes.sumOf { it.durationSeconds }
                                    viewModel.updateDuration(totalDuration)
                                    viewModel.startGeneration()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("generate_from_scene_plan_btn")
                            ) {
                                Icon(imageVector = Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepDarkBg)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GENERATE VIDEO FROM SCENE PLAN (${scriptScenes.sumOf { it.durationSeconds }}s Total)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepDarkBg)
                            }
                        }
                    }
                }
            }
        }

        // 9. Reference Asset Input Section (For Image-to-Video & Video-to-Video)
        if (form.generationType == "IMAGE_TO_VIDEO" || form.generationType == "MOTION_TRANSFER" || form.sourceImageUri != null) {
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Keyframe Reference Image (Image → Video)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        if (form.sourceImageUri != null) {
                            IconButton(onClick = { viewModel.updateSourceImageUri(null) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = AccentRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (form.sourceImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.InsertPhoto, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "Reference Image Loaded", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = form.sourceImageUri ?: "", fontSize = 10.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                SoraBadge(text = "Keyframe Active", color = NeonCyan)
                            }
                        }
                    } else {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("select_reference_image_btn")
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Starting Image Reference from Device", fontSize = 11.5.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        if (form.generationType == "VIDEO_TO_VIDEO" || form.sourceVideoUri != null) {
            item {
                SoraGlassCard(borderColor = NeonPurple) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Source Video Input (Video → Video)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        if (form.sourceVideoUri != null) {
                            IconButton(onClick = { viewModel.updateSourceVideoUri(null) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = AccentRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (form.sourceVideoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.VideoFile, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "Source Video Loaded", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = form.sourceVideoUri ?: "", fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                                    }
                                }
                                SoraBadge(text = "Transform Ready", color = NeonPurple)
                            }
                        }
                    } else {
                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("select_reference_video_btn")
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonPurple)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Source Video Clip from Device", fontSize = 11.5.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // 10. Title Input
        item {
            OutlinedTextField(
                value = form.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Video Project Title") },
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

        // 11. Video Prompt & Negative Prompt Input
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Video Prompt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    TextButton(onClick = {
                        viewModel.updatePrompt("${form.prompt}, 8k octane render, cinematic lighting, ultra smooth 60fps motion, dynamic camera sweep")
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

                Spacer(modifier = Modifier.height(8.dp))

                // Negative Prompt
                OutlinedTextField(
                    value = form.imageNegativePrompt,
                    onValueChange = { viewModel.updateImageNegativePrompt(it) },
                    placeholder = { Text("Negative Prompt: blurry, distorted, jitter, flicker, lowres, extra limbs", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassSurfaceVariant,
                        unfocusedBorderColor = GlassSurfaceVariant
                    )
                )
            }
        }

        // 12. Video Render Quality Modes Banner
        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cinematic Video Engine Matrix",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    SoraBadge(
                        text = "${form.fps} FPS · ${form.resolution}",
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQualityModeCard(
                        title = "⚡ Fast Mode",
                        desc = "Turbo 24fps mobile diffusion · Low VRAM",
                        modeKey = "FAST",
                        selectedMode = form.mode,
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateMode("FAST") }
                    )
                    VideoQualityModeCard(
                        title = "⚖️ Balanced",
                        desc = "30fps 1080p spatial-temporal smoothing",
                        modeKey = "BALANCED",
                        selectedMode = form.mode,
                        color = NeonPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateMode("BALANCED") }
                    )
                    VideoQualityModeCard(
                        title = "🎬 Cinema 4K",
                        desc = "60fps HDR neural ray-traced lighting",
                        modeKey = "CINEMA",
                        selectedMode = form.mode,
                        color = ElectricPink,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateMode("CINEMA") }
                    )
                }
            }
        }

        // 13. Video Parameters: Duration, Resolution, FPS, Aspect Ratio, Codec
        item {
            SoraGlassCard(borderColor = GlassSurfaceVariant) {
                Text(
                    text = "🎞️ Video Generation Parameters",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Duration Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Target Video Duration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(
                            text = "${form.durationSec}s (${if (form.durationSec >= 60) "${form.durationSec / 60}m" else "${form.durationSec}s"})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val durations = listOf(
                            1 to "1s", 2 to "2s", 3 to "3s", 5 to "5s",
                            10 to "10s", 15 to "15s", 30 to "30s", 60 to "1m",
                            120 to "2m", 300 to "5m", 600 to "10m", 1800 to "30m"
                        )
                        items(durations.size) { i ->
                            val (sec, label) = durations[i]
                            val isSelected = form.durationSec == sec
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateDuration(sec) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("duration_chip_$sec")
                            )
                        }
                    }
                    if (form.durationSec > 10) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚡ Automatic Segmented Rendering: Will compute ${maxOf(1, form.durationSec / 5)} continuous temporal segments with auto-checkpointing.",
                            fontSize = 10.5.sp,
                            color = AccentGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FPS & Resolution
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Frame Rate (FPS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val fpsList = listOf(12, 15, 24, 30, 48, 60)
                            items(fpsList.size) { idx ->
                                val fpsVal = fpsList[idx]
                                val isSel = form.fps == fpsVal
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateFps(fpsVal) },
                                    label = { Text("${fpsVal}fps", fontSize = 10.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = DeepDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Video Resolution", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val resList = listOf("720p", "1080p", "2K", "4K")
                            items(resList.size) { idx ->
                                val r = resList[idx]
                                val isSel = form.resolution == r
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateResolution(r) },
                                    label = { Text(r, fontSize = 10.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricPink,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Aspect Ratio & Codec
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Aspect Ratio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val ratios = listOf("16:9", "9:16", "1:1", "2.39:1", "4:3")
                            items(ratios.size) { idx ->
                                val ratio = ratios[idx]
                                val isSel = form.aspectRatio == ratio
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateAspectRatio(ratio) },
                                    label = { Text(ratio, fontSize = 10.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = DeepDarkBg
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Video Codec", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val codecs = listOf("H.264", "HEVC/H.265", "AV1", "VP9")
                            items(codecs.size) { idx ->
                                val c = codecs[idx]
                                val isSel = form.videoCodec == c
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.updateVideoCodec(c) },
                                    label = { Text(c, fontSize = 10.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonPurple,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Camera Movement
                Column {
                    Text(text = "Camera Movement & Cinematography", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val cameraMoves = listOf(
                            "STATIC" to "📷 Static Tripod",
                            "DYNAMIC_PAN" to "➡️ Smooth Pan L/R",
                            "TILT_UP_DOWN" to "⬆️ Cinematic Tilt",
                            "DOLLY_IN" to "🎯 Dolly Rush In",
                            "DOLLY_OUT" to "🔭 Dolly Zoom Out",
                            "ORBIT_360" to "🔄 Orbit 360°",
                            "TRACKING_SHOT" to "🏃 Dynamic Tracking",
                            "HANDHELD" to "📹 Raw Handheld",
                            "DRONE_SWEEP" to "🛸 Drone Aerial Sweep"
                        )
                        items(cameraMoves.size) { i ->
                            val (key, label) = cameraMoves[i]
                            val isSelected = form.cameraMotion == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateCameraMotion(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = DeepDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Motion Strength & Temporal Consistency Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Motion Intensity", fontSize = 12.sp, color = TextSecondary)
                            Text(text = "${(form.motionStrength * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Slider(
                            value = form.motionStrength,
                            onValueChange = { viewModel.updateMotionStrength(it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Temporal Stability", fontSize = 12.sp, color = TextSecondary)
                            Text(text = "${(form.temporalConsistency * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                        }
                        Slider(
                            value = form.temporalConsistency,
                            onValueChange = { viewModel.updateTemporalConsistency(it) },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                        )
                    }
                }
            }
        }

        // 14. Advanced Cinematography Director Prompts
        item {
            SoraGlassCard(borderColor = GlassSurfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedPrompts = !showAdvancedPrompts },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (showAdvancedPrompts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Advanced Cinematography & Director Prompts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(text = if (showAdvancedPrompts) "Hide" else "Show", fontSize = 11.sp, color = NeonCyan)
                }

                if (showAdvancedPrompts) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = form.motionPrompt,
                            onValueChange = { viewModel.updateMotionPrompt(it) },
                            placeholder = { Text("Motion & Action Prompt (e.g. 'Fast martial arts strike with cape flutter')", fontSize = 11.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = form.cameraPrompt,
                            onValueChange = { viewModel.updateCameraPrompt(it) },
                            placeholder = { Text("Camera & Lens Prompt (e.g. '35mm anamorphic wide angle, rack focus')", fontSize = 11.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = form.lightingPrompt,
                            onValueChange = { viewModel.updateLightingPrompt(it) },
                            placeholder = { Text("Lighting & Atmosphere (e.g. 'Volumetric god rays, neon rim lighting')", fontSize = 11.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // 15. Launch & Queue Action Buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SoraGradientButton(
                text = if (form.isGenerating) "⚡ GENERATE & QUEUE NEXT (BACKGROUND)" else "START IMMEDIATE GENERATION",
                icon = if (form.isGenerating) Icons.Default.AddCircle else Icons.Default.PlayArrow,
                enabled = true,
                modifier = Modifier.fillMaxWidth().testTag("start_generation_button"),
                onClick = { viewModel.startGeneration() }
            )

            if (form.isGenerating) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 Engine is actively rendering in background. Tapping Generate adds this video to the background queue so you can safely switch tabs or close the screen.",
                    fontSize = 10.5.sp,
                    color = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.addCurrentFormToQueue() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    modifier = Modifier.weight(1f).testTag("add_to_queue_button")
                ) {
                    Icon(imageVector = Icons.Default.AddToQueue, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add to Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showBatchDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    modifier = Modifier.weight(1f).testTag("batch_queue_dialog_button")
                ) {
                    Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepDarkBg)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batch Queue Jobs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepDarkBg)
                }
            }
        }
    }

    // Batch Job Creator Dialog
    if (showBatchDialog) {
        BatchJobCreatorDialog(
            onDismiss = { showBatchDialog = false },
            onBatchQueue = { prefix, prompts, mode, duration, res, fps ->
                viewModel.addBatchJobsToQueue(prefix, prompts, "TEXT_TO_VIDEO", mode, duration, res, fps)
                showBatchDialog = false
            }
        )
    }

    // Script Import & Converter Dialog
    if (showScriptImportDialog) {
        ScriptImportModalDialog(
            onDismiss = { showScriptImportDialog = false },
            onImportScript = { title, content ->
                viewModel.updateTitle(title)
                viewModel.updateGenerationType("SCRIPT_TO_VIDEO")
                // Parse script into video scenes
                val lines = content.split("\n").filter { it.isNotBlank() }
                val parsed = if (lines.size >= 2) {
                    lines.chunked(maxOf(1, lines.size / 3)).mapIndexed { idx, chunk ->
                        ScriptScene(
                            sceneNumber = idx + 1,
                            title = "Scene ${idx + 1}",
                            voiceover = chunk.joinToString(" "),
                            visualDescription = "Cinematic rendering of ${chunk.firstOrNull()?.take(60) ?: "scene"}",
                            imagePrompt = "Cinematic visualization, 8k",
                            videoPrompt = "Dynamic cinematic shot: ${chunk.joinToString(" ").take(100)}",
                            cameraMovement = if (idx % 2 == 0) "Slow Forward Dolly" else "Smooth Pan Right",
                            lighting = "Cinematic volumetric lighting",
                            durationSeconds = 5
                        )
                    }
                } else {
                    listOf(
                        ScriptScene(
                            sceneNumber = 1,
                            title = "Scene 1",
                            voiceover = content,
                            visualDescription = "Visual representation of $title",
                            imagePrompt = "Cinematic scene, 8k",
                            videoPrompt = content.take(120),
                            cameraMovement = "Dynamic Pan",
                            lighting = "Cinematic lighting",
                            durationSeconds = 10
                        )
                    )
                }
                scriptScenes = parsed
                showScenePlanner = true
                showScriptImportDialog = false
            }
        )
    }
}

@Composable
fun VideoTypeChip(label: String, typeKey: String, selectedType: String, onClick: () -> Unit) {
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
fun VideoQualityModeCard(
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
fun ScenePlanItemCard(
    scene: ScriptScene,
    index: Int,
    onUpdatePrompt: (String) -> Unit,
    onUpdateDuration: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GlassSurface)
            .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "${scene.sceneNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = scene.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoraBadge(text = "${scene.durationSeconds}s", color = NeonCyan)
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = "🎙️ \"${scene.voiceover}\"",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = if (expanded) 10 else 1,
                overflow = TextOverflow.Ellipsis
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = scene.videoPrompt,
                    onValueChange = { onUpdatePrompt(it) },
                    label = { Text("Video Prompt for Scene ${scene.sceneNumber}", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Camera: ${scene.cameraMovement}", fontSize = 10.5.sp, color = NeonPurple)
                    Text(text = "Lighting: ${scene.lighting}", fontSize = 10.5.sp, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun ScriptImportModalDialog(
    onDismiss: () -> Unit,
    onImportScript: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("Cybernetic Singularity") }
    var scriptContent by remember {
        mutableStateOf(
            "Scene 1: Introduction to high-tech neural network computing.\n" +
            "Scene 2: Quantum core acceleration and real-time ray-traced dynamics.\n" +
            "Scene 3: The arrival of autonomous creative synthesis agents across the globe."
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ImportContacts, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Script to Video Studio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select a preset or paste script text to automatically convert into timed video scenes.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Script Project Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = scriptContent,
                    onValueChange = { scriptContent = it },
                    label = { Text("Script Content") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            title = "Cyberpunk Neo-Tokyo Chase"
                            scriptContent = "Scene 1: Rain-slicked asphalt reflecting neon signs as hoverbikes race through the underpass.\nScene 2: The lead rider activates optical camouflage while weaving between autonomous drones.\nScene 3: A sweeping crane shot reveals the sprawling neon skyline of sector 9."
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("Cyberpunk Sample", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            title = "Deep Space Exoplanet Discovery"
                            scriptContent = "Scene 1: The exploratory vessel drops out of hyperspace before a bioluminescent ringed planet.\nScene 2: Atmospheric probe descent through swirling emerald clouds into an ancient crystalline valley.\nScene 3: First contact with floating geometric monolithic ruins."
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text("Sci-Fi Sample", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportScript(title, scriptContent) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Convert Script to Video", color = DeepDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Cancel")
            }
        },
        containerColor = DeepDarkBg,
        shape = RoundedCornerShape(14.dp)
    )
}
