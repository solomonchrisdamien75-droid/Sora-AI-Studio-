package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiModelEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun ModelsScreen(viewModel: SoraMainViewModel) {
    val models by viewModel.allModels.collectAsState()
    val hardware by viewModel.hardwareProfile.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredModels = models.filter { model ->
        val matchesSearch = model.name.contains(searchQuery, ignoreCase = true) || model.description.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "DOWNLOADED" -> model.isDownloaded
            "GGUF" -> model.format.equals("GGUF", ignoreCase = true)
            "LITERET" -> model.format.equals("LITERET", ignoreCase = true)
            "ONNX" -> model.format.equals("ONNX", ignoreCase = true)
            "SAFETENSORS" -> model.format.contains("SAFE", ignoreCase = true)
            "MNN_NCNN" -> model.format.contains("MNN", ignoreCase = true) || model.format.contains("NCNN", ignoreCase = true)
            "VIDEO_MODELS" -> model.description.contains("VIDEO", ignoreCase = true) || model.name.contains("Video", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Model Manager & Storage Analyzer",
                subtitle = "Manage GGUF, LiteRT, ONNX, Safetensors & ComfyUI models",
                icon = Icons.Default.FolderZip,
                actionText = "Folder Scan",
                onActionClick = { viewModel.refreshHardwareProfile() }
            )
        }

        // Folder Scan & Storage Location Toolbar
        item {
            SoraGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Scan Locations", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        Text(text = "Internal Storage • SD Card • USB Storage", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = { viewModel.refreshHardwareProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan SD/USB", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search local or remote models...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                modifier = Modifier.fillMaxWidth().testTag("model_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = GlassSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Filter Tabs
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { ModelFilterChip("All (${models.size})", "ALL", selectedFilter) { selectedFilter = "ALL" } }
                item { ModelFilterChip("Downloaded", "DOWNLOADED", selectedFilter) { selectedFilter = "DOWNLOADED" } }
                item { ModelFilterChip("GGUF", "GGUF", selectedFilter) { selectedFilter = "GGUF" } }
                item { ModelFilterChip("LiteRT", "LITERET", selectedFilter) { selectedFilter = "LITERET" } }
                item { ModelFilterChip("ONNX", "ONNX", selectedFilter) { selectedFilter = "ONNX" } }
                item { ModelFilterChip("Safetensors", "SAFETENSORS", selectedFilter) { selectedFilter = "SAFETENSORS" } }
                item { ModelFilterChip("MNN / NCNN", "MNN_NCNN", selectedFilter) { selectedFilter = "MNN_NCNN" } }
                item { ModelFilterChip("Video AI", "VIDEO_MODELS", selectedFilter) { selectedFilter = "VIDEO_MODELS" } }
            }
        }

        // Models List
        items(filteredModels) { model ->
            val availRamMb = ((hardware?.availableRamGb ?: 4.0f) * 1024).toInt()
            val isCompatible = availRamMb >= model.ramRequiredMb

            SoraGlassCard(
                borderColor = if (isCompatible) NeonCyan.copy(alpha = 0.3f) else AccentRed.copy(alpha = 0.4f),
                modifier = Modifier.testTag("model_card_${model.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(text = model.format, color = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            SoraBadge(
                                text = "${model.ramRequiredMb}MB RAM",
                                color = if (isCompatible) AccentGreen else AccentRed,
                                textColor = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = model.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = model.description, fontSize = 12.sp, color = TextSecondary)
                    }

                    if (model.isDownloaded) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Installed", tint = AccentGreen, modifier = Modifier.size(24.dp))
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.selectTab(com.example.ui.SoraTab.DOWNLOADS) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("download_model_btn_${model.id}")
                        ) {
                            Text("Download", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = GlassSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Recommended: ${model.recommendedResolution} @ ${model.recommendedFps}fps", fontSize = 11.sp, color = TextSecondary)
                    Text(text = if (isCompatible) "Ready for Inference" else "RAM Exceeded (${availRamMb}MB Avail)", fontSize = 11.sp, color = if (isCompatible) AccentGreen else AccentRed)
                }
            }
        }
    }
}

@Composable
fun ModelFilterChip(label: String, key: String, selectedKey: String, onClick: () -> Unit) {
    val isSelected = key == selectedKey
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else GlassSurface)
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
