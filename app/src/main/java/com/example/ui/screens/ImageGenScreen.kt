package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GalleryItemEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ImageGenScreen(viewModel: SoraMainViewModel) {
    val form by viewModel.imageGenerationForm.collectAsState()
    val activeJob by viewModel.activeJob.collectAsState()
    val latestResult by viewModel.latestGeneratedResult.collectAsState()
    val queuedJobs by viewModel.queuedJobs.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateDedicatedImageSourceUri(it.toString()) }
    }

    val maskPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateDedicatedImageMaskUri(it.toString()) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        item {
            SoraSectionHeader(
                title = "Image Studio",
                subtitle = "Dedicated High-Resolution Diffusion & Inpainting Engine",
                icon = Icons.Default.Palette
            )
        }

        // Mode Selector: All 12 Image Studio Modes
        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Image Studio Mode (12 Modes)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        SoraBadge(text = form.mode, color = NeonCyan)
                    }

                    val modes = listOf(
                        "TEXT_TO_IMAGE" to "🎨 1. Text to Image",
                        "AI_IMAGE_EDITING" to "✨ 2. AI Image Editing",
                        "AI_UPSCALING" to "🔍 3. AI Upscaling",
                        "AI_INPAINTING" to "🖌️ 4. AI Inpainting",
                        "AI_OUTPAINTING" to "📐 5. AI Outpainting",
                        "BACKGROUND_REMOVAL" to "✂️ 6. BG Removal",
                        "MOTION_TRANSFER" to "🏃 7. Motion Transfer",
                        "VIDEO_ENHANCEMENT" to "📹 8. Video Enhancement",
                        "CHARACTER_3D_GEN" to "🧍 9. 3D Character Gen",
                        "IMAGE_3D_GEN" to "🔮 10. 3D Image Gen",
                        "DONGHUA_CHARACTER" to "⚔️ 11. Donghua Creator",
                        "IMAGE_SCENE_GEN" to "🌄 12. Scene Generator"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(modes) { (modeKey, label) ->
                            val isSelected = form.mode == modeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateDedicatedImageMode(modeKey) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("image_mode_${modeKey.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Mode-Specific Contextual Parameter Panels
        when (form.mode) {
            "AI_IMAGE_EDITING" -> {
                item {
                    SoraGlassCard(borderColor = NeonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("AI Image Editing Controls", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            OutlinedTextField(
                                value = form.editInstruction,
                                onValueChange = { viewModel.updateDedicatedImageEditInstruction(it) },
                                label = { Text("Editing Instruction (e.g. Add glowing dragon aura, change hair to platinum silver)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                            )
                        }
                    }
                }
            }
            "AI_UPSCALING", "VIDEO_ENHANCEMENT" -> {
                item {
                    SoraGlassCard(borderColor = NeonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Upscaling & Enhancement Factor", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("2x", "4x Ultra HD", "8x Master").forEach { factor ->
                                    val isSelected = form.upscaleFactor == factor
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImageUpscaleFactor(factor) },
                                        label = { Text(factor, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "AI_OUTPAINTING" -> {
                item {
                    SoraGlassCard(borderColor = NeonPurple) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Outpainting Canvas Expansion Direction", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ALL (360°)", "HORIZONTAL", "VERTICAL", "UP", "DOWN", "LEFT", "RIGHT").forEach { dir ->
                                    val isSelected = form.outpaintDirection == dir
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImageOutpaintDirection(dir) },
                                        label = { Text(dir, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "MOTION_TRANSFER" -> {
                item {
                    SoraGlassCard(borderColor = AccentYellow) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Motion Transfer Dynamic Strength", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                                Text("${(form.motionStrength * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                            }
                            Slider(
                                value = form.motionStrength,
                                onValueChange = { viewModel.updateDedicatedImageMotionStrength(it) },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = AccentYellow, activeTrackColor = AccentYellow)
                            )
                        }
                    }
                }
            }
            "CHARACTER_3D_GEN", "IMAGE_3D_GEN" -> {
                item {
                    SoraGlassCard(borderColor = NeonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("3D Mesh Angle & Turntable Perspective", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Front View", "3/4 Isometric", "Side Profile", "Turntable 360°").forEach { angle ->
                                    val isSelected = form.character3DView == angle
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImage3DView(angle) },
                                        label = { Text(angle, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "DONGHUA_CHARACTER" -> {
                item {
                    SoraGlassCard(borderColor = ElectricPink) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Donghua Cultivation Realm & Qi Rank", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricPink)
                            val ranks = listOf("Qi Condensation", "Foundation Establishment", "Golden Core", "Nascent Soul", "Immortal Sovereign")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(ranks) { rank ->
                                    val isSelected = form.donghuaCultivationRank == rank
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImageDonghuaRank(rank) },
                                        label = { Text(rank, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricPink, selectedLabelColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            "IMAGE_SCENE_GEN" -> {
                item {
                    SoraGlassCard(borderColor = NeonPurple) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Scene Atmosphere & Environment", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                            val atmospheres = listOf("Celestial Dao Realm", "Cyberpunk Metropolis", "Ancient Shrine", "Neon Shibuya", "Void Nebula")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(atmospheres) { atmos ->
                                    val isSelected = form.sceneAtmosphere == atmos
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImageSceneAtmosphere(atmos) },
                                        label = { Text(atmos, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Prompt Input & Negative Prompt
        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.6f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Artwork Title & Prompt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        SoraBadge(text = form.mode, color = NeonPurple)
                    }

                    OutlinedTextField(
                        value = form.title,
                        onValueChange = { viewModel.updateDedicatedImageTitle(it) },
                        label = { Text("Artwork Title") },
                        modifier = Modifier.fillMaxWidth().testTag("image_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = GlassSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = form.prompt,
                        onValueChange = { viewModel.updateDedicatedImagePrompt(it) },
                        label = { Text("Prompt (Describe subject, lighting, angle, details)") },
                        modifier = Modifier.fillMaxWidth().height(110.dp).testTag("image_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = GlassSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 4
                    )

                    // Negative Prompt
                    OutlinedTextField(
                        value = form.negativePrompt,
                        onValueChange = { viewModel.updateDedicatedImageNegativePrompt(it) },
                        label = { Text("Negative Prompt (What to avoid)") },
                        modifier = Modifier.fillMaxWidth().testTag("image_neg_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPink,
                            unfocusedBorderColor = GlassSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 2
                    )
                }
            }
        }

        // Image-to-Image / Inpainting source image uploaders if active mode requires it
        if (form.mode in listOf("IMAGE_TO_IMAGE", "AI_IMAGE_EDITING", "AI_INPAINTING", "AI_OUTPAINTING", "BACKGROUND_REMOVAL", "AI_UPSCALING", "MOTION_TRANSFER", "VIDEO_ENHANCEMENT")) {
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Source Reference Image & Masks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (form.sourceImageUri != null) "Change Image" else "Select Source", fontSize = 11.sp)
                            }
                            if (form.mode == "AI_INPAINTING" || form.mode == "INPAINTING") {
                                OutlinedButton(
                                    onClick = { maskPicker.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Brush, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (form.maskImageUri != null) "Change Mask" else "Select Mask", fontSize = 11.sp)
                                }
                            }
                        }
                        if (form.sourceImageUri != null) {
                            Text(
                                text = "✓ Source Loaded: ${form.sourceImageUri?.takeLast(35)}",
                                fontSize = 11.sp,
                                color = AccentGreen
                            )
                        }
                        if (form.maskImageUri != null && (form.mode == "AI_INPAINTING" || form.mode == "INPAINTING")) {
                            Text(
                                text = "✓ Inpaint Mask Loaded: ${form.maskImageUri?.takeLast(35)}",
                                fontSize = 11.sp,
                                color = ElectricPink
                            )
                        }
                    }
                }
            }
        }

        // Style Presets
        item {
            SoraGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Visual Art Style", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    val styles = listOf(
                        "PHOTOREALISTIC" to "📸 Photorealistic",
                        "ANIME" to "✨ Anime Masterpiece",
                        "CYBERPUNK" to "🌆 Cyberpunk Neon",
                        "OCTANE_3D" to "🔮 3D Octane Render",
                        "FANTASY_CINEMATIC" to "🧙 Fantasy Cinematic",
                        "OIL_PAINTING" to "🎨 Oil Painting",
                        "CONCEPT_ART" to "🖌️ Concept Art",
                        "WATERCOLOR" to "💧 Watercolor"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(styles) { (key, label) ->
                            val isSelected = form.style == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateDedicatedImageStyle(key) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Aspect Ratio & Resolution Grid
        item {
            SoraGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Dimensions & Aspect Ratio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Aspect Ratios
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Aspect Ratio", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            val ratios = listOf("1:1", "16:9", "9:16", "4:3", "3:2")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(ratios) { r ->
                                    val isSelected = form.aspectRatio == r
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImageAspectRatio(r) },
                                        label = { Text(r, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                        // Resolutions
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Resolution", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            val resList = listOf("512x512", "768x768", "1024x1024", "1536x1024", "2048x2048")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(resList) { res ->
                                    val isSelected = form.resolution == res
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateDedicatedImageResolution(res) },
                                        label = { Text(res, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Advanced Sampling Controls: Steps, CFG, Sampler, Seed
        item {
            SoraGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Diffusion Parameters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                    // Sampling Steps Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Sampling Steps", fontSize = 12.sp, color = TextPrimary)
                            Text(text = "${form.steps} steps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                        }
                        Slider(
                            value = form.steps.toFloat(),
                            onValueChange = { viewModel.updateDedicatedImageSteps(it.toInt()) },
                            valueRange = 10f..80f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                        )
                    }

                    // CFG Scale Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "CFG Scale (Prompt Guidance)", fontSize = 12.sp, color = TextPrimary)
                            Text(text = String.format("%.1f", form.cfgScale), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Slider(
                            value = form.cfgScale,
                            onValueChange = { viewModel.updateDedicatedImageCfgScale(it) },
                            valueRange = 1.0f..20.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // Sampler Selector
                    Column {
                        Text(text = "Diffusion Sampler", fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val samplers = listOf("Euler a", "DPM++ 2M Karras", "DDIM", "UniPC", "LCM Turbo")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(samplers) { s ->
                                val isSelected = form.sampler == s
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateDedicatedImageSampler(s) },
                                    label = { Text(s, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(6.dp)
                                )
                            }
                        }
                    }

                    // Seed & Randomizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = form.isRandomSeed,
                                onCheckedChange = { viewModel.toggleDedicatedImageRandomSeed(it) }
                            )
                            Text(text = "Random Seed (-1)", fontSize = 12.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = form.highResFix,
                                onCheckedChange = { viewModel.toggleDedicatedImageHighResFix(it) }
                            )
                            Text(text = "High-Res Fix", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Hardware Load & Memory Safety Assessment
        item {
            val telemetry by viewModel.realtimeTelemetry.collectAsState()
            val isLowRam = telemetry.freeRamMb < 1500
            val loadColor = if (isLowRam) AccentYellow else AccentGreen
            val loadLabel = if (isLowRam) "WARNING (LOW RAM)" else "SAFE (OPTIMAL)"

            SoraGlassCard(borderColor = loadColor) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = loadColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Hardware Readiness", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = loadColor)
                        }
                        SoraBadge(text = loadLabel, color = loadColor)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Estimated RAM: ~1.2 GB", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "Free RAM: ${telemetry.freeRamMb} MB", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "GPU Acceleration: Vulkan NPU", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "Est. Duration: ~4s", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    if (isLowRam) {
                        Text(
                            text = "Low-RAM optimization active: resolution and batch size clamped to prevent out-of-memory errors.",
                            fontSize = 11.sp,
                            color = AccentYellow
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateDedicatedImageResolution("512x512")
                                    viewModel.updateDedicatedImageSteps(15)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reduce Quality", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.addDedicatedImageJobToQueue() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Queue Task", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: Generate Now & Queue Task
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.startDedicatedImageGeneration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_image_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    enabled = !form.isGenerating
                ) {
                    if (form.isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing Image...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Image Now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.addDedicatedImageJobToQueue() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("queue_image_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Task Queue", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Error Banner if any
        if (form.errorMessage != null) {
            item {
                SoraGlassCard(borderColor = AccentRed) {
                    Text(text = "Error: ${form.errorMessage}", color = AccentRed, fontSize = 12.sp)
                }
            }
        }

        // Latest Generated Preview Modal Card with Cross-Studio Actions
        if (latestResult != null && latestResult?.mediaType == "IMAGE") {
            item {
                latestResult?.let { item ->
                    SoraGlassCard(borderColor = AccentGreen) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Image Synthesis Complete!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                }
                                IconButton(onClick = { viewModel.dismissLatestGeneratedResult() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(text = item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Saved to Gallery • ${item.resolutionLabel} • Style: ${form.style}", fontSize = 11.sp, color = TextSecondary)

                            // Cross Studio Dispatch Actions
                            Text(text = "Send Asset to Another Studio:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.sendImageToVideoStudio(item.filePath, item.prompt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("To Video Studio", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.sendImageToManhwaStudio(item.filePath, item.title, item.prompt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("To Manhwa Studio", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.selectTab(SoraTab.GALLERY) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View in Gallery", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
