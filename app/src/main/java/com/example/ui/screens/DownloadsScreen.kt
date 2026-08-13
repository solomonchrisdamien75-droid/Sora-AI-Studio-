package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun DownloadsScreen(viewModel: SoraMainViewModel) {
    val query by viewModel.huggingFaceQuery.collectAsState()
    val hfResults by viewModel.huggingFaceResults.collectAsState()
    val dlState by viewModel.downloadingState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Hugging Face Browser",
                subtitle = "Browse & download open-source AI models directly",
                icon = Icons.Default.CloudDownload
            )
        }

        // Active Download Progress Monitor Widget
        val state = dlState
        if (state != null) {
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Downloading Model", fontSize = 12.sp, color = NeonCyan)
                            Text(text = state.modelName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        SoraBadge(text = "${state.progressPercent}%", color = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
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
                        Text(
                            text = "${state.bytesDownloaded / (1024 * 1024)}MB / ${state.totalBytes / (1024 * 1024)}MB",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Speed: ${String.format("%.1f", state.downloadSpeedKbps / 1024f)} MB/s",
                            fontSize = 12.sp,
                            color = NeonPurple
                        )
                        Text(
                            text = "ETA: ${state.etaSeconds}s",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchHuggingFaceModels(it) },
                placeholder = { Text("Search Hugging Face models (e.g. video, gguf, litert)...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                modifier = Modifier.fillMaxWidth().testTag("hf_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Search Results List
        items(hfResults) { model ->
            SoraGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(text = model.format, color = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            SoraBadge(text = model.modelType, color = NeonPurple)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = model.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Author: ${model.author} • Downloads: ${model.downloads} • Likes: ${model.likes}", fontSize = 11.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SoraGradientButton(
                        text = "Download",
                        icon = Icons.Default.GetApp,
                        modifier = Modifier.width(120.dp).testTag("dl_btn_${model.id}"),
                        enabled = dlState == null,
                        onClick = { viewModel.downloadHuggingFaceModel(model) }
                    )
                }
            }
        }
    }
}
