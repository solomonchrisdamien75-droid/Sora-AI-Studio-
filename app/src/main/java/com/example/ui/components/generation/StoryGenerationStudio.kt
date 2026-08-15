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
fun StoryGenerationStudio(
    viewModel: SoraMainViewModel,
    form: GenerationFormState
) {
    SoraGlassCard(borderColor = AccentGreen) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Dedicated Story & Screenplay AI Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Multi-scene script breakdown, character arcs & narrative pacing", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "STORY SYSTEM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = GlassSurfaceVariant)

            // Screenplay Format
            Column {
                Text(text = "Script Architecture Format", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val formats = listOf(
                        "SCREENPLAY" to "🎬 Hollywood Screenplay",
                        "YOUTUBE_SCRIPT" to "📹 YouTube Storyboard",
                        "NOVEL_CHAPTER" to "📖 Novel Chapter",
                        "SHOT_LIST" to "🎥 Shot & Camera Plan"
                    )
                    items(formats.size) { i ->
                        val (key, label) = formats[i]
                        val isSel = form.storyFormat == key
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateStoryFormat(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGreen,
                                selectedLabelColor = DeepDarkBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Genre & Tone
            Column {
                Text(text = "Narrative Tone & Genre", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tones = listOf(
                        "SCI_FI" to "🚀 Sci-Fi Odyssey",
                        "DARK_FANTASY" to "⚔️ Dark Fantasy",
                        "CYBERPUNK_ACTION" to "⚡ Cyberpunk Thriller",
                        "MYSTERY" to "🕵️ Mystery & Suspense",
                        "ROMANCE" to "💖 Drama & Romance"
                    )
                    items(tones.size) { i ->
                        val (key, label) = tones[i]
                        val isSel = form.storyTone == key
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateStoryTone(key) },
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

            // Scene Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Target Scene Count (${form.storySceneCount} Scenes)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 4, 6, 8, 10).forEach { cnt ->
                        val isSel = form.storySceneCount == cnt
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.updateStorySceneCount(cnt) },
                            label = { Text("$cnt", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricPink,
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
