package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiModelEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ModelsScreen(viewModel: SoraMainViewModel) {
    val models by viewModel.allModels.collectAsState()
    val activeLoadedModel by viewModel.activeLoadedModel.collectAsState()
    val hardware by viewModel.hardwareProfile.collectAsState()
    val dlState by viewModel.downloadingState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Dialog state for manual model import (+)
    var showImportDialog by remember { mutableStateOf(false) }
    var importModelName by remember { mutableStateOf("") }
    var importFormat by remember { mutableStateOf("GGUF") }
    var importModelType by remember { mutableStateOf("VIDEO") }
    var importRamMb by remember { mutableStateOf("2048") }
    var importPath by remember { mutableStateOf("") }
    var importStorageSource by remember { mutableStateOf("Phone Storage") }

    // Dialog state for choosing download storage location
    var modelToDownload by remember { mutableStateOf<AiModelEntity?>(null) }
    var selectedDownloadStorage by remember { mutableStateOf("INTERNAL") } // "INTERNAL" or "SD_CARD" or "CUSTOM"
    var customDownloadPath by remember { mutableStateOf("/sdcard/ai_models") }

    // File picker launcher for opening model files from storage or SD card
    val modelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "custom_model.gguf"
            importModelName = fileName.substringBeforeLast('.')
            importPath = it.toString()
            val lower = fileName.lowercase()
            when {
                lower.endsWith(".gguf") -> importFormat = "GGUF"
                lower.endsWith(".safetensors") -> importFormat = "SAFETENSORS"
                lower.endsWith(".tflite") || lower.endsWith(".task") -> importFormat = "LITERET"
                lower.endsWith(".onnx") -> importFormat = "ONNX"
                lower.endsWith(".mnn") -> importFormat = "MNN"
                lower.endsWith(".ncnn") -> importFormat = "NCNN"
            }
            if (it.toString().contains("sdcard", ignoreCase = true) || it.toString().contains("external", ignoreCase = true)) {
                importStorageSource = "SD Card Storage"
            } else {
                importStorageSource = "Phone Storage"
            }
        }
    }

    // Directory picker launcher for selecting SD card or internal folders
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            customDownloadPath = it.toString()
            selectedDownloadStorage = "CUSTOM"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SoraSectionHeader(
                    title = "Model Manager & Storage Space",
                    subtitle = "Manage models on Phone Storage & SD Card • GGUF, LiteRT, ONNX & Safetensors",
                    icon = Icons.Default.FolderZip,
                    actionText = "+ Import Model",
                    onActionClick = { showImportDialog = true }
                )
            }

            // Active Download Progress Monitor (if downloading)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonCyan,
                            trackColor = GlassSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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

            // Storage Destinations Info Card (Phone Storage vs SD Card)
            item {
                SoraGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "💾 Storage Drives & Model Targets", fontSize = 13.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                            Text(text = "📱 Phone Storage (~58 GB Free) • 💾 SD Card (~124 GB Free)", fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("import_model_top_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Model", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Active Model in Memory Status Banner
            if (activeLoadedModel != null) {
                item {
                    SoraGlassCard(borderColor = AccentGreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoraBadge(text = "CURRENTLY LOADED IN MEMORY", color = AccentGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SoraBadge(text = activeLoadedModel!!.format, color = NeonCyan)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeLoadedModel!!.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Ready for on-device generation & Local REST API server",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = { viewModel.selectTab(SoraTab.SORA_CLOUD) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Server", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
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
                    item { ModelFilterChip("Video AI", "VIDEO_MODELS", selectedFilter) { selectedFilter = "VIDEO_MODELS" } }
                }
            }

            // Filtered Models List
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

            items(filteredModels) { model ->
                val availRamMb = ((hardware?.availableRamGb ?: 4.0f) * 1024).toInt()
                val isCompatible = availRamMb >= model.ramRequiredMb
                val isActiveModel = activeLoadedModel?.id == model.id

                SoraGlassCard(
                    borderColor = when {
                        isActiveModel -> AccentGreen
                        isCompatible -> NeonCyan.copy(alpha = 0.3f)
                        else -> AccentRed.copy(alpha = 0.4f)
                    },
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
                                Spacer(modifier = Modifier.width(6.dp))
                                SoraBadge(
                                    text = "${model.ramRequiredMb}MB RAM",
                                    color = if (isCompatible) AccentGreen else AccentRed,
                                    textColor = TextPrimary
                                )
                                if (model.isDownloaded) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val isSd = model.localPath?.contains("sdcard", ignoreCase = true) == true
                                    SoraBadge(
                                        text = if (isSd) "💾 SD CARD" else "📱 PHONE STORAGE",
                                        color = if (isSd) AccentGreen else NeonPurple
                                    )
                                }
                                if (isActiveModel) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SoraBadge(text = "ACTIVE IN MEMORY", color = AccentGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = model.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = model.description, fontSize = 12.sp, color = TextSecondary)
                            if (model.localPath != null) {
                                Text(text = "Path: ${model.localPath}", fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        if (model.isDownloaded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActiveModel) {
                                    OutlinedButton(
                                        onClick = { viewModel.unloadActiveModel() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                                        modifier = Modifier.testTag("unload_model_btn_${model.id}")
                                    ) {
                                        Text("Unload", fontSize = 11.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.loadModelForServer(model) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        modifier = Modifier.testTag("load_model_btn_${model.id}")
                                    ) {
                                        Text("Load Model", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    modelToDownload = model
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                                modifier = Modifier.testTag("download_model_btn_${model.id}")
                            ) {
                                Icon(imageVector = Icons.Default.GetApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = GlassSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Recommended: ${model.recommendedResolution} @ ${model.recommendedFps}fps", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = if (isActiveModel) "Active for Local API Server & Inference"
                            else if (isCompatible) "Ready for Inference"
                            else "RAM Exceeded (${availRamMb}MB Avail)",
                            fontSize = 11.sp,
                            color = if (isActiveModel || isCompatible) AccentGreen else AccentRed
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Floating Action Button (+) for manual upload of models from phone storage or SD card
        FloatingActionButton(
            onClick = { showImportDialog = true },
            containerColor = NeonCyan,
            contentColor = DeepDarkBg,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_model")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Model from Storage or SD Card", modifier = Modifier.size(28.dp))
        }
    }

    // ==========================================
    // 1. DIALOG: Manual Model Import (+)
    // ==========================================
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Model from Storage / SD Card", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pick an AI model file from your Phone Storage or SD Card folder (.gguf, .safetensors, .litert, .onnx):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // File picker trigger buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                modelFilePicker.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📱 Phone Files", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                modelFilePicker.launch(arrayOf("*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.SdCard, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("💾 SD Card", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick Preset choices
                    Text(text = "Or quick load sample models:", fontSize = 11.sp, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            TypeChip("📱 Phone: Sora-LiteRT-v2", "PHONE", "MODEL") {
                                importModelName = "Sora-LiteRT-v2"
                                importFormat = "LITERET"
                                importPath = "/storage/emulated/0/ai_models/sora_litert_v2.tflite"
                                importStorageSource = "Phone Storage"
                            }
                        }
                        item {
                            TypeChip("💾 SD Card: Llama-3.2-3B.gguf", "SD", "MODEL") {
                                importModelName = "Llama-3.2-3B-Q4_K_M"
                                importFormat = "GGUF"
                                importPath = "/storage/sdcard/models/llama-3.2-3b.gguf"
                                importStorageSource = "SD Card Storage"
                            }
                        }
                        item {
                            TypeChip("💾 SD Card: AnimateDiff-v3", "SD", "MODEL") {
                                importModelName = "AnimateDiff-v3-SD15"
                                importFormat = "SAFETENSORS"
                                importPath = "/storage/sdcard/ai_models/animatediff_v3.safetensors"
                                importStorageSource = "SD Card Storage"
                            }
                        }
                    }

                    OutlinedTextField(
                        value = importModelName,
                        onValueChange = { importModelName = it },
                        label = { Text("Model Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = importFormat,
                            onValueChange = { importFormat = it },
                            label = { Text("Format (GGUF, LiteRT...)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = importRamMb,
                            onValueChange = { importRamMb = it },
                            label = { Text("RAM (MB)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = importPath,
                        onValueChange = { importPath = it },
                        label = { Text("File Path / URI on Storage") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Storage Source: ", fontSize = 11.sp, color = TextSecondary)
                        SoraBadge(
                            text = importStorageSource,
                            color = if (importStorageSource.contains("SD")) AccentGreen else NeonPurple
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importModelName.isNotBlank() && importPath.isNotBlank()) {
                            val ram = importRamMb.toIntOrNull() ?: 2048
                            viewModel.importCustomModelFromStorage(
                                name = importModelName,
                                format = importFormat,
                                modelType = importModelType,
                                ramMb = ram,
                                localPath = importPath,
                                storageSource = importStorageSource
                            )
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Import & Register", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // =======================================================
    // 2. DIALOG: Choose Download Storage Location for Model
    // =======================================================
    val targetModel = modelToDownload
    if (targetModel != null) {
        AlertDialog(
            onDismissRequest = { modelToDownload = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Download Destination", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Select where to save \"${targetModel.name}\" on your device:",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    // Option 1: Internal Phone Storage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedDownloadStorage == "INTERNAL") NeonCyan.copy(alpha = 0.15f) else GlassSurface)
                            .border(1.dp, if (selectedDownloadStorage == "INTERNAL") NeonCyan else GlassSurfaceVariant, RoundedCornerShape(8.dp))
                            .clickable { selectedDownloadStorage = "INTERNAL" }
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedDownloadStorage == "INTERNAL",
                                onClick = { selectedDownloadStorage = "INTERNAL" },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "📱 Phone Internal Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "/data/user/0/.../files/ai_models (58.4 GB Free)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Option 2: SD Card Storage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedDownloadStorage == "SD_CARD") AccentGreen.copy(alpha = 0.15f) else GlassSurface)
                            .border(1.dp, if (selectedDownloadStorage == "SD_CARD") AccentGreen else GlassSurfaceVariant, RoundedCornerShape(8.dp))
                            .clickable { selectedDownloadStorage = "SD_CARD" }
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedDownloadStorage == "SD_CARD",
                                onClick = { selectedDownloadStorage = "SD_CARD" },
                                colors = RadioButtonDefaults.colors(selectedColor = AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "💾 Removable SD Card Storage", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "/storage/sdcard/ai_models (124.8 GB Free)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Option 3: Custom Folder Path
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
                                    Text(text = "📂 Custom Directory Path", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "Choose any folder on Phone or SD card", fontSize = 11.sp, color = TextSecondary)
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
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { folderPicker.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Browse", fontSize = 10.sp)
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
                        viewModel.downloadModelEntityWithLocation(targetModel, selectedDownloadStorage, path)
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
