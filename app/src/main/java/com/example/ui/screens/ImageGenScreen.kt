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

        // Mode Selector: Text to Image, Image to Image, Inpainting, Outpainting, Background Removal, Upscaling
        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Image Synthesis Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    val modes = listOf(
                        "TEXT_TO_IMAGE" to "🎨 Text to Image",
                        "IMAGE_TO_IMAGE" to "🖼️ Image to Image",
                        "INPAINTING" to "🖌️ Inpainting",
                        "OUTPAINTING" to "📐 Outpainting",
                        "BG_REMOVAL" to "✂️ BG Removal",
                        "UPSCALING" to "🔍 4K Upscale"
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
        if (form.mode in listOf("IMAGE_TO_IMAGE", "INPAINTING", "OUTPAINTING", "BG_REMOVAL", "UPSCALING")) {
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
                            if (form.mode == "INPAINTING") {
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
                        if (form.maskImageUri != null) {
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

        // Latest Generated Preview Modal Card
        if (latestResult != null && latestResult?.mediaType == "IMAGE") {
            item {
                latestResult?.let { item ->
                    SoraGlassCard(borderColor = AccentGreen) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
