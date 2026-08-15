package com.example.ui.components.generation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GenerationFormState
import com.example.ui.SoraMainViewModel
import com.example.ui.components.SoraGlassCard
import com.example.ui.theme.*

@Composable
fun ImageGenerationStudio(
    viewModel: SoraMainViewModel,
    form: GenerationFormState
) {
    SoraGlassCard(borderColor = NeonPurple) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Dedicated Image Synthesis Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Independent high-res diffusion parameters (No video overhead)", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeonPurple.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "IMAGE SYSTEM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = GlassSurfaceVariant)

            // 1. Style Preset Selection
            Column {
                Text(text = "Visual Art Style", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    items(styles.size) { idx ->
                        val (key, label) = styles[idx]
                        val isSelected = form.imageStyle == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateImageStyle(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("image_style_${key.lowercase()}")
                        )
                    }
                }
            }

            // 2. Aspect Ratio & Resolution
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Image Aspect Ratio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val ratios = listOf("1:1" to "Square", "16:9" to "Landscape", "9:16" to "Story", "4:3" to "Standard", "3:2" to "Photo")
                        items(ratios.size) { i ->
                            val (r, desc) = ratios[i]
                            val isSel = form.imageAspectRatio == r
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateImageAspectRatio(r) },
                                label = { Text("$r ($desc)", fontSize = 10.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = DeepDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Image Resolution", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val resolutions = listOf("512x512", "768x768", "1024x1024", "1536x1024", "2048x2048")
                        items(resolutions.size) { i ->
                            val res = resolutions[i]
                            val isSel = form.imageResolution == res
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateImageResolution(res) },
                                label = { Text(res, fontSize = 10.5.sp) },
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

            // 3. Sampling Steps & CFG Guidance Scale
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Diffusion Sampling Steps (${form.imageSteps})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Text(text = "CFG Scale: ${String.format("%.1f", form.imageCfgScale)}", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val stepOptions = listOf(10, 20, 30, 50, 100)
                    items(stepOptions.size) { i ->
                        val s = stepOptions[i]
                        val isSel = form.imageSteps == s
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateImageSteps(s) },
                            label = { Text("$s steps", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen,
                                selectedLabelColor = DeepDarkBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = form.imageCfgScale,
                    onValueChange = { viewModel.updateImageCfgScale(it) },
                    valueRange = 1.0f..20.0f,
                    steps = 38,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            // 4. Negative Prompt
            Column {
                Text(text = "Negative Prompt (What to exclude)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = form.imageNegativePrompt,
                    onValueChange = { viewModel.updateImageNegativePrompt(it) },
                    placeholder = { Text("e.g. blurry, low quality, distorted, extra limbs, bad anatomy, artifacts") },
                    modifier = Modifier.fillMaxWidth().testTag("negative_prompt_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = GlassSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // 5. Sampler Algorithm & Batch Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(text = "Sampler Algorithm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val samplers = listOf("Euler a", "DPM++ 2M Karras", "DDIM", "UniPC", "LCM Turbo")
                        items(samplers.size) { i ->
                            val s = samplers[i]
                            val isSel = form.imageSampler == s
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateImageSampler(s) },
                                label = { Text(s, fontSize = 10.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricPink,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(0.8f)) {
                    Text(text = "Batch Count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 4).forEach { count ->
                            val isSel = form.imageBatchCount == count
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateImageBatchCount(count) },
                                label = { Text("${count}x", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = DeepDarkBg
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // High-Res Fix switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "High-Res Fix & Neural Detailer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Upscales latents to eliminate blur and sharpen fine textures", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = form.imageHighResFix,
                    onCheckedChange = { viewModel.toggleImageHighResFix(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = NeonPurple.copy(alpha = 0.4f))
                )
            }
        }
    }
}
