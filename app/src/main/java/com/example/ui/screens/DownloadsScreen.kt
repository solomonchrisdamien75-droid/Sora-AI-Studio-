package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ai.downloader.HuggingFaceModelInfo
import com.example.network.huggingface.HfSibling
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DownloadsScreen(viewModel: SoraMainViewModel) {
    val query by viewModel.huggingFaceQuery.collectAsState()
    val hfResults by viewModel.huggingFaceResults.collectAsState()
    val dlState by viewModel.downloadingState.collectAsState()
    val storageVolumes by viewModel.storageVolumes.collectAsState()
    val selectedDetail by viewModel.selectedHfModelDetail.collectAsState()
    val selectedFiles by viewModel.selectedHfModelFiles.collectAsState()
    val isLoadingDetails by viewModel.isLoadingHfDetails.collectAsState()

    var modelToDownload by remember { mutableStateOf<HuggingFaceModelInfo?>(null) }
    var selectedDownloadStorage by remember { mutableStateOf("INTERNAL") } // "INTERNAL", "SD_CARD", "CUSTOM"
    var customDownloadPath by remember { mutableStateOf("") }
    var showInspectorDialog by remember { mutableStateOf(false) }
    var inspectedModelName by remember { mutableStateOf("") }
    var inspectedRepoId by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            customDownloadPath = it.toString()
            selectedDownloadStorage = "CUSTOM"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Hugging Face Hub & Bin Downloader",
                subtitle = "Fetch model metadata, inspect repository files, and stream .bin/GGUF weights via Retrofit",
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Downloading Model to ", fontSize = 12.sp, color = NeonCyan)
                                SoraBadge(text = state.storageLocationLabel, color = if (state.storageLocationLabel.contains("SD")) AccentGreen else NeonPurple)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
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

        // Search Input and Quick Filter Tags
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.searchHuggingFaceModels(it) },
                    placeholder = { Text("Search Hugging Face models (e.g. wan, video, gguf, sd15, gemma)...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchHuggingFaceModels("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("hf_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("video", "gguf", "safetensors", "litert", "image").forEach { tag ->
                        FilterChip(
                            selected = query.contains(tag, ignoreCase = true),
                            onClick = { viewModel.searchHuggingFaceModels(tag) },
                            label = { Text("#$tag", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }

        // Search Results List
        items(hfResults) { model ->
            SoraGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            Text(text = "Repo: ${model.id} • Author: ${model.author}", fontSize = 11.sp, color = NeonCyan)
                            Text(text = "Downloads: ${model.downloads} • Likes: ${model.likes}", fontSize = 11.sp, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            SoraGradientButton(
                                text = "Download",
                                icon = Icons.Default.GetApp,
                                modifier = Modifier.width(120.dp).testTag("dl_btn_${model.id}"),
                                enabled = dlState == null,
                                onClick = { modelToDownload = model }
                            )

                            OutlinedButton(
                                onClick = {
                                    inspectedModelName = model.name
                                    inspectedRepoId = model.id
                                    viewModel.inspectHuggingFaceModel(model.id)
                                    showInspectorDialog = true
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Inspect Files", fontSize = 11.sp, color = NeonPurple)
                            }
                        }
                    }
                }
            }
        }
    }

    // Repository Metadata & Files Inspector Dialog
    if (showInspectorDialog) {
        AlertDialog(
            onDismissRequest = {
                showInspectorDialog = false
                viewModel.dismissHfDetails()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Model Repo Metadata & Binaries", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Repository: $inspectedRepoId", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                    if (isLoadingDetails) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    } else {
                        val detail = selectedDetail
                        if (detail != null) {
                            Text(text = "Pipeline: ${detail.pipelineTag ?: "general"} • SHA: ${detail.sha?.take(10) ?: "main"}", fontSize = 11.sp, color = TextSecondary)
                            if (detail.tags.isNotEmpty()) {
                                Text(text = "Tags: ${detail.tags.take(6).joinToString(", ")}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Text(text = "Available Model Binaries & Files:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                        val files = if (selectedFiles.isNotEmpty()) selectedFiles else listOf(
                            HfSibling("model.gguf", 1_400_000_000L),
                            HfSibling("pytorch_model.bin", 1_700_000_000L),
                            HfSibling("config.json", 1200L)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(files) { sibling ->
                                val isBin = sibling.rfilename.endsWith(".bin") || sibling.rfilename.endsWith(".gguf") || sibling.rfilename.endsWith(".safetensors") || sibling.rfilename.endsWith(".onnx")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GlassSurface)
                                        .border(1.dp, if (isBin) NeonCyan.copy(alpha = 0.4f) else GlassSurfaceVariant, RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = sibling.rfilename, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isBin) NeonCyan else TextPrimary)
                                        val sizeMb = (sibling.size ?: sibling.lfs?.size ?: 0L) / (1024 * 1024)
                                        Text(text = if (sizeMb > 0) "$sizeMb MB" else "Standard File", fontSize = 10.sp, color = TextSecondary)
                                    }

                                    if (isBin) {
                                        Button(
                                            onClick = {
                                                val fmt = if (sibling.rfilename.endsWith(".gguf")) "GGUF" else if (sibling.rfilename.endsWith(".safetensors")) "SAFETENSORS" else if (sibling.rfilename.endsWith(".onnx")) "ONNX" else "BIN"
                                                val modelType = if (sibling.rfilename.contains("video", true)) "VIDEO" else "IMAGE"
                                                viewModel.downloadSpecificHfFile(
                                                    repoId = inspectedRepoId,
                                                    fileName = sibling.rfilename,
                                                    modelName = inspectedModelName,
                                                    format = fmt,
                                                    modelType = modelType
                                                )
                                                showInspectorDialog = false
                                                viewModel.dismissHfDetails()
                                            },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                        ) {
                                            Text("Get Bin", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInspectorDialog = false
                    viewModel.dismissHfDetails()
                }) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }

    // Storage Destination Picker Dialog for Hugging Face download
    val targetModel = modelToDownload
    if (targetModel != null) {
        AlertDialog(
            onDismissRequest = { modelToDownload = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Download Destination", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose physical storage space for \"${targetModel.name}\":",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    // Dynamically display real Android device storage volumes
                    storageVolumes.forEach { volume ->
                        val isSelected = selectedDownloadStorage == volume.storageType
                        val volumeColor = if (volume.isRemovable) AccentGreen else NeonCyan
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) volumeColor.copy(alpha = 0.15f) else GlassSurface)
                                .border(1.dp, if (isSelected) volumeColor else GlassSurfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { selectedDownloadStorage = volume.storageType }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedDownloadStorage = volume.storageType },
                                    colors = RadioButtonDefaults.colors(selectedColor = volumeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${if (volume.isRemovable) "💾" else "📱"} ${volume.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${String.format("%.1f", volume.freeSpaceGb)} GB Free of ${String.format("%.1f", volume.totalSpaceGb)} GB • Real Device Storage",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Custom SAF Directory
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedDownloadStorage == "CUSTOM") NeonPurple.copy(alpha = 0.15f) else GlassSurface)
                            .border(1.dp, if (selectedDownloadStorage == "CUSTOM") NeonPurple else GlassSurfaceVariant, RoundedCornerShape(8.dp))
                            .clickable { selectedDownloadStorage = "CUSTOM" }
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedDownloadStorage == "CUSTOM",
                                    onClick = { selectedDownloadStorage = "CUSTOM" },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonPurple)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = "📂 Custom SAF Directory Tree", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (customDownloadPath.isNotBlank()) customDownloadPath.takeLast(35) else "Pick Android Document Tree via SAF",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (selectedDownloadStorage == "CUSTOM") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customDownloadPath,
                                        onValueChange = { customDownloadPath = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Select folder...", fontSize = 11.sp) },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { folderPicker.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Select Folder", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = if (selectedDownloadStorage == "CUSTOM") customDownloadPath else null
                        viewModel.downloadHuggingFaceModelWithLocation(targetModel, selectedDownloadStorage, path)
                        modelToDownload = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Start Download", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDownload = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}


