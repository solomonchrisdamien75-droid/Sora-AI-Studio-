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
import com.example.ui.components.generation.DurationFormatters
import com.example.ui.components.generation.VideoDurationSelector
import com.example.ui.theme.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File

val VideoStudioFeatureItems = listOf(
    StudioFeatureItem("TEXT_TO_VIDEO", 1, "Text-to-Video Synthesis", "Cinematic neural prompt-to-motion engine", "CORE AI", Icons.Default.Videocam, "Video"),
    StudioFeatureItem("IMAGE_TO_VIDEO", 2, "Image-to-Video Animation", "Animate first frame into fluid high-fps video", "ANIMATE", Icons.Default.Image, "Animation"),
    StudioFeatureItem("VIDEO_TO_VIDEO", 3, "Video-to-Video Style Transfer", "Neural style morphing & structural transfer", "TRANSFER", Icons.Default.Transform, "Transfer"),
    StudioFeatureItem("VIDEO_CONTINUATION", 4, "Continuation & Infinite Extend", "Seamless forward/backward temporal continuation", "EXTEND", Icons.Default.FastForward, "Extend"),
    StudioFeatureItem("VIDEO_INPAINTING", 5, "Video Inpainting & Object Removal", "Temporal consistency brush masking & generative fill", "INPAINT", Icons.Default.Brush, "Inpaint"),
    StudioFeatureItem("VIDEO_UPSCALING", 6, "4K / 60fps Neural Super-Resolution", "Real-time spatial upscaling & AI motion smoothing", "UPSCALE", Icons.Default.ZoomIn, "Upscale"),
    StudioFeatureItem("MOTION_TRANSFER", 7, "Motion Transfer & Pose Warping", "Transfer actor motion dynamics to generated avatars", "MOTION", Icons.Default.DirectionsRun, "Motion"),
    StudioFeatureItem("NEURAL_LIP_SYNC", 8, "Neural Voice Lip-Sync Alignment", "Phoneme-to-mouth geometry speech synchronization", "LIP SYNC", Icons.Default.RecordVoiceOver, "Lip Sync"),
    StudioFeatureItem("CAMERA_TRAJECTORY", 9, "Cinematic 3D Camera Trajectory", "Dolly, orbit, drone dive, boom & custom paths", "CAMERA", Icons.Default.CameraAlt, "Camera"),
    StudioFeatureItem("SLOW_MOTION_RIFE", 10, "RIFE Slow-Motion & Frame Interpolator", "2x, 4x, 8x (120 FPS) temporal frame synthesis", "SLOW-MO", Icons.Default.SlowMotionVideo, "Slow-Mo"),
    StudioFeatureItem("SPATIAL_3D_VIDEO", 11, "Spatial 3D Stereoscopic VR Video", "Side-by-side stereoscopy & depth parallax", "SPATIAL 3D", Icons.Default.ViewInAr, "3D VR"),
    StudioFeatureItem("SCRIPT_TO_VIDEO", 12, "Multi-Scene Script Storyboard Director", "Multi-scene sequence rendering & automated continuity", "DIRECTOR", Icons.Default.Movie, "Storyboard")
)

/**
 * Dedicated VIDEO GENERATION STUDIO.
 * Full 12-Feature navigation via top-left 3-line hamburger menu and horizontal chip bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    var showMenuModal by remember { mutableStateOf(false) }

    val currentFeature = VideoStudioFeatureItems.firstOrNull { it.id == form.generationType } ?: VideoStudioFeatureItems.first()

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

    Scaffold(
        topBar = {
            StudioFeatureTopBar(
                studioTitle = "Video Studio",
                currentFeature = currentFeature,
                totalFeatures = 12,
                accentColor = NeonCyan,
                onMenuClick = { showMenuModal = true },
                actions = {
                    IconButton(onClick = { viewModel.selectTab(SoraTab.QUEUE) }) {
                        Icon(Icons.Default.Queue, contentDescription = "Task Queue", tint = NeonCyan)
                    }
                    IconButton(onClick = { viewModel.selectTab(SoraTab.EDITOR) }) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Editor Studio", tint = NeonPurple)
                    }
                }
            )
        },
        containerColor = DeepDarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick 12-Feature horizontal chip selector + 3-line drawer trigger
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        AssistChip(
                            onClick = { showMenuModal = true },
                            label = { Text("☰ All 12 Features", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = NeonCyan.copy(alpha = 0.15f), labelColor = NeonCyan)
                        )
                    }
                    items(VideoStudioFeatureItems) { feature ->
                        val isSelected = form.generationType == feature.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateGenerationType(feature.id) },
                            label = { Text("${feature.index}. ${feature.title}", fontSize = 11.sp) },
                            leadingIcon = { Icon(feature.icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = DeepDarkBg,
                                selectedLeadingIconColor = DeepDarkBg
                            )
                        )
                    }
                }
            }

            // Feature Section Header
            item {
                StudioFeatureSectionHeader(
                    title = currentFeature.title,
                    subtitle = currentFeature.subtitle,
                    badgeText = currentFeature.badge,
                    icon = currentFeature.icon,
                    accentColor = NeonCyan
                )
            }

            // Feature Details & Technical Specifications
            item {
                val videoSpecs = when (form.generationType) {
                    "TEXT_TO_VIDEO" -> listOf(
                        "Synthesis Architecture" to "Sora Diffusion Transformer (DiT-Vulkan)",
                        "Target Framerate" to "${form.fps} FPS Constant",
                        "Resolution & Ratio" to "${form.resolution} (${form.aspectRatio})",
                        "Motion Flow Strength" to "${(form.motionStrength * 100).toInt()}%"
                    )
                    "IMAGE_TO_VIDEO" -> listOf(
                        "First-Frame Conditioning" to (form.sourceImageUri?.takeLast(30) ?: "Not Selected"),
                        "Motion Flow Strength" to "0.82 Latent Motion Guidance",
                        "Preserve Subject Geometry" to "Strict Face & Texture Lock",
                        "Output Duration" to "${form.durationSec} Seconds"
                    )
                    "VIDEO_TO_VIDEO" -> listOf(
                        "Source Video Asset" to (form.sourceVideoUri?.takeLast(30) ?: "Not Selected"),
                        "Style Morphing Method" to "ControlNet Temporal Pose Transfer",
                        "Frame Retention Rate" to "100% Structural Consistency",
                        "Style Prompt" to (form.prompt.take(30).ifEmpty { "Default Cinematic" })
                    )
                    "VIDEO_CONTINUATION" -> listOf(
                        "Continuation Mode" to "Forward Extrapolation (+5s)",
                        "Keyframe Overlap" to "12 Frames Optical Flow Blend",
                        "Context Memory" to "Last 3.5s Temporal Attention Buffer",
                        "Seamless Stitching" to "Active"
                    )
                    "VIDEO_INPAINTING" -> listOf(
                        "Inpainting Mask" to "Temporal Tracking Mask",
                        "Object Removal Method" to "ProPainter Optical Flow Generative Fill",
                        "Background Healing" to "Consistent 3D Depth Infill",
                        "Resolution" to form.resolution
                    )
                    "VIDEO_UPSCALING" -> listOf(
                        "Neural Super-Resolution" to "Real-CUGAN 4K Master Engine",
                        "Temporal De-flicker" to "High Precision Inter-frame Align",
                        "Hardware NPU Acceleration" to "Active (Vulkan Shader)",
                        "Target Resolution" to "3840x2160 UHD (60 FPS)"
                    )
                    "MOTION_TRANSFER" -> listOf(
                        "Pose Extraction" to "DensePose / OpenPose Tracker",
                        "Target Avatar" to "Prompt Guided Neural Synthesis",
                        "Skeletal Rig Match" to "Real-Time 3D Bone Estimation",
                        "Dynamic Dampening" to "0.15 Jitter Filter"
                    )
                    "NEURAL_LIP_SYNC" -> listOf(
                        "Lip Sync Alignment" to "Wav2Lip / SadTalker GAN Engine",
                        "Audio Phoneme Sync" to "16 kHz Mono High-Res Audio Input",
                        "Facial Geometry Lock" to "3D Morphable Model (3DMM)",
                        "Target Character" to (form.title.ifEmpty { "Protagonist" })
                    )
                    "CAMERA_TRAJECTORY" -> listOf(
                        "Camera Movement Path" to form.cameraMotion,
                        "3D Virtual Sensor" to "35mm Anamorphic Lens (f/1.8)",
                        "Trajectory Speed" to "Smooth Bézier Interpolation",
                        "Volumetric Parallax" to "True 3D Depth Buffer"
                    )
                    "SLOW_MOTION_RIFE" -> listOf(
                        "Interpolation Engine" to "RIFE v4.6 (Real-Time Intermediate Flow)",
                        "Slow-Motion Multiplier" to "4x Slow Motion (96 FPS Output)",
                        "Motion Blur Synthesis" to "Sub-pixel Directional Shutter",
                        "Artifact Suppression" to "High Gradient Motion Mask"
                    )
                    "SPATIAL_3D_VIDEO" -> listOf(
                        "Stereoscopic Format" to "Side-by-Side Left/Right Eye (VR180)",
                        "Interpupillary Distance" to "64mm Natural Stereo Depth",
                        "3D Convergence" to "Auto-focusing Zero Parallax Plane",
                        "VR Headset Ready" to "Apple Vision Pro & Meta Quest 3"
                    )
                    else -> listOf(
                        "Storyboard Scenes" to "${scriptScenes.size} Planned Scenes",
                        "Total Narrative Duration" to "${scriptScenes.sumOf { it.durationSeconds }} Seconds",
                        "Continuity Pipeline" to "Character & Lighting Seed Inheritance",
                        "Auto-Stitch Sequence" to "Automated Video Editor Export"
                    )
                }

                StudioDetailsCard(
                    title = "Feature Technical Specifications",
                    details = videoSpecs,
                    accentColor = NeonCyan
                )
            }

            // Task Queue Live Quick Monitor Banner
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

            // Active Background Render Job Monitor
            val job = activeJob
            if (job != null) {
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
                                    modifier = Modifier.size(22.dp),
                                    color = NeonCyan,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Rendering: ${job.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "${job.status} • Frame ${job.currentFrame}/${job.totalFrames}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            SoraBadge(text = "${job.progressPercent}%", color = NeonCyan)
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

                        Spacer(modifier = Modifier.height(10.dp))

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

            // Output Generation Result Preview Card
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

                        val isImage = result.mediaType == "IMAGE" || result.filePath.endsWith(".png", true) || result.filePath.endsWith(".jpg", true) || result.filePath.endsWith(".jpeg", true)
                        val localFile = File(result.filePath)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlassSurfaceVariant)
                                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            if (isImage && localFile.exists()) {
                                AsyncImage(
                                    model = localFile,
                                    contentDescription = "Generated Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Transparent overlay gradient to make text legible
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                            )
                                        )
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    SoraBadge(text = result.resolutionLabel, color = AccentGreen)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = if (isImage) Icons.Default.Image else Icons.Default.PlayArrow, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = if (isImage) "IMAGE COMPLETED" else "READY TO PLAY", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
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
                                val paths = listOf("DYNAMIC_PAN", "ZOOM_IN", "ZOOM_OUT", "ORBIT_360", "TILT_UP", "DRONE_FLYTHROUGH")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(paths) { path ->
                                        val isSelected = form.cameraMotion == path
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.updateCameraMotion(path) },
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
                "SCRIPT_TO_VIDEO" -> {
                    item {
                        SoraGlassCard(borderColor = NeonCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Multi-Scene Script Storyboard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    SoraBadge(text = "${scriptScenes.size} Scenes", color = NeonCyan)
                                }
                                scriptScenes.forEach { scene ->
                                    Surface(
                                        color = GlassSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(scene.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                            Text(scene.videoPrompt, fontSize = 11.sp, color = TextSecondary)
                                            Spacer(Modifier.height(4.dp))
                                            Text("Camera: ${scene.cameraMovement} • ${scene.durationSeconds}s", fontSize = 10.sp, color = NeonCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Image-to-Video & Video-to-Video Media Input pickers
            if (form.generationType in listOf("IMAGE_TO_VIDEO", "VIDEO_TO_VIDEO", "VIDEO_INPAINTING", "VIDEO_UPSCALING", "MOTION_TRANSFER", "NEURAL_LIP_SYNC")) {
                item {
                    SoraGlassCard(borderColor = NeonPurple) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Source Reference Media Asset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (form.sourceImageUri != null) "Change Image" else "Select Frame", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (form.sourceVideoUri != null) "Change Video" else "Select Video", fontSize = 11.sp)
                                }
                            }
                            if (form.sourceImageUri != null) {
                                Text("✓ Image Selected: ${form.sourceImageUri?.takeLast(35)}", fontSize = 11.sp, color = AccentGreen)
                            }
                            if (form.sourceVideoUri != null) {
                                Text("✓ Video Selected: ${form.sourceVideoUri?.takeLast(35)}", fontSize = 11.sp, color = AccentGreen)
                            }
                        }
                    }
                }
            }

            // Video Prompt & Title Controls
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Video Synthesis Prompt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            SoraBadge(text = form.generationType, color = NeonCyan)
                        }

                        OutlinedTextField(
                            value = form.title,
                            onValueChange = { viewModel.updateTitle(it) },
                            label = { Text("Video Title") },
                            modifier = Modifier.fillMaxWidth().testTag("video_title_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = form.prompt,
                            onValueChange = { viewModel.updatePrompt(it) },
                            label = { Text("Cinematic Prompt (Describe subject, lighting, angle, motion)") },
                            modifier = Modifier.fillMaxWidth().height(110.dp).testTag("video_prompt_input"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 4
                        )

                        OutlinedTextField(
                            value = form.motionPrompt,
                            onValueChange = { viewModel.updateMotionPrompt(it) },
                            label = { Text("Motion & Camera Prompt (e.g. dynamic sweeping pan, high speed)") },
                            modifier = Modifier.fillMaxWidth().testTag("video_motion_prompt_input"),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }
                }
            }

            // Video Hyperparameters: Duration (1s to Hours), Resolution, FPS, Motion Bucket
            item {
                SoraGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Video Dynamics & Hyperparameters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                        // 1. Comprehensive Duration Selector (1s to 24+ Hours)
                        VideoDurationSelector(
                            viewModel = viewModel,
                            form = form
                        )

                        // 2. FPS & Resolution
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Framerate (FPS)", fontSize = 11.sp, color = TextSecondary)
                                val fpsList = listOf(24, 30, 60)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    fpsList.forEach { f ->
                                        FilterChip(
                                            selected = form.fps == f,
                                            onClick = { viewModel.updateFps(f) },
                                            label = { Text("${f}fps", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Resolution", fontSize = 11.sp, color = TextSecondary)
                                val resList = listOf("720p", "1080p", "4K")
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    resList.forEach { r ->
                                        FilterChip(
                                            selected = form.resolution == r,
                                            onClick = { viewModel.updateResolution(r) },
                                            label = { Text(r, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Generate Video & Queue Video Job
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.startGeneration() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_video_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        enabled = activeJob == null
                    ) {
                        if (activeJob != null) {
                            CircularProgressIndicator(color = DeepDarkBg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rendering Video...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepDarkBg)
                        } else {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = DeepDarkBg)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Render Video Now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepDarkBg)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.addCurrentFormToQueue() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("queue_video_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple)
                    ) {
                        Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Video to Task Queue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showMenuModal) {
        StudioFeatureMenuModal(
            studioName = "Video Studio",
            features = VideoStudioFeatureItems,
            selectedFeatureId = form.generationType,
            accentColor = NeonCyan,
            onFeatureSelected = { feature -> viewModel.updateGenerationType(feature.id) },
            onDismiss = { showMenuModal = false }
        )
    }
}
