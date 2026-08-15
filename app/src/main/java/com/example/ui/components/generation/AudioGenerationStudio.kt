package com.example.ui.components.generation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GenerationFormState
import com.example.ui.SoraMainViewModel
import com.example.ui.components.SoraGlassCard
import com.example.ui.theme.*

@Composable
fun AudioGenerationStudio(
    viewModel: SoraMainViewModel,
    form: GenerationFormState
) {
    SoraGlassCard(borderColor = ElectricPink) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Dedicated Voice & Audio AI Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Voice clone, neural speech synthesis & harmonic acoustics", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricPink.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "AUDIO SYSTEM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPink,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = GlassSurfaceVariant)

            // Voice Archetype Selection
            Column {
                Text(text = "Voice Archetype Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val archetypes = listOf(
                        "MALE_DEEP" to "🎙️ Deep Narrator (Male)",
                        "FEMALE_MELODIC" to "🌸 Melodic (Female)",
                        "AI_ASSISTANT" to "🤖 Cyber Assistant",
                        "ANIME_HERO" to "⚡ Anime Protagonist",
                        "DRAMATIC_NARRATOR" to "🎬 Cinema Trailer Voice"
                    )
                    items(archetypes.size) { i ->
                        val (key, label) = archetypes[i]
                        val isSel = form.audioVoiceArchetype == key
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateAudioVoiceArchetype(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricPink,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Emotion Tone Selection
            Column {
                Text(text = "Vocal Emotion Tone", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val emotions = listOf(
                        "NEUTRAL" to "😐 Neutral",
                        "DRAMATIC" to "🔥 Dramatic",
                        "ENTHUSIASTIC" to "🎉 Enthusiastic",
                        "WHISPERING" to "🤫 Whispering",
                        "ANGRY" to "💢 Intense / Angry"
                    )
                    items(emotions.size) { i ->
                        val (key, label) = emotions[i]
                        val isSel = form.audioEmotion == key
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateAudioEmotion(key) },
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

            // Pitch and Speed Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Pitch Shift", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(text = "${form.audioPitch} st", fontSize = 12.sp, color = NeonCyan)
                    }
                    Slider(
                        value = form.audioPitch.toFloat(),
                        onValueChange = { viewModel.updateAudioPitch(it.toInt()) },
                        valueRange = -10f..10f,
                        steps = 20,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Speech Rate", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text(text = "${String.format("%.1f", form.audioSpeed)}x", fontSize = 12.sp, color = ElectricPink)
                    }
                    Slider(
                        value = form.audioSpeed,
                        onValueChange = { viewModel.updateAudioSpeed(it) },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = ElectricPink, activeTrackColor = ElectricPink)
                    )
                }
            }

            // Audio Format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Audio Output Format", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WAV (Lossless)", "MP3 (Standard)", "AAC (High-Efficiency)").forEach { fmt ->
                        val key = fmt.take(3)
                        val isSel = form.audioFormat == key
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateAudioFormat(key) },
                            label = { Text(fmt, fontSize = 10.5.sp) },
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
}
