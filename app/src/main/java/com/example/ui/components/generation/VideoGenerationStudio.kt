package com.example.ui.components.generation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun VideoGenerationStudio(
    viewModel: SoraMainViewModel,
    form: com.example.ui.GenerationFormState = viewModel.generationForm.collectAsState().value
) {
    val hardwareProfile by viewModel.hardwareProfile.collectAsState()
    val loadedModel by viewModel.activeLoadedModel.collectAsState()
    var showAdvancedPrompts by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Video Generation Mode & Hardware Suitability Banner
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

            // 1. Video Render Quality Modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityModeCard(
                    title = "⚡ Fast Mode",
                    desc = "Turbo 24fps mobile diffusion · Low VRAM",
                    modeKey = "FAST",
                    selectedMode = form.mode,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateMode("FAST") }
                )
                QualityModeCard(
                    title = "⚖️ Balanced",
                    desc = "30fps 1080p spatial-temporal smoothing",
                    modeKey = "BALANCED",
                    selectedMode = form.mode,
                    color = NeonPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateMode("BALANCED") }
                )
                QualityModeCard(
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

        // Dedicated Video Parameters Card
        SoraGlassCard(borderColor = GlassSurfaceVariant) {
            Text(
                text = "🎞️ Video Generation Parameters",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Video Duration Controller (Full Range 1 Second to 24+ Hours)
            VideoDurationSelector(
                viewModel = viewModel,
                form = form
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. FPS & Resolution & Aspect Ratio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // FPS
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

                // Resolution
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Aspect Ratio
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

                // Video Codec & Audio
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Codec & Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
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

            // 4. Camera Paths & Movement
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

            // 5. Motion Strength & Temporal Consistency Sliders
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

        // Advanced Cinematography Director Prompts
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
                        placeholder = { Text("Motion & Action Prompt (e.g. 'Fast martial arts kick with cape flutter')", fontSize = 11.sp, color = TextSecondary) },
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
}
