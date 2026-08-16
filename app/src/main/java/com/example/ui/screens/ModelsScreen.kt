package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.quantization.QuantizationConfig
import com.example.ai.quantization.QuantizationPrecision
import com.example.ai.quantization.QuantizationProgressState
import com.example.ai.quantization.QuantizationTradeoffObjective
import com.example.data.AiModelEntity
import com.example.data.QuantizationHistoryEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ModelsScreen(viewModel: SoraMainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val models by viewModel.allModels.collectAsState()
    val activeLoadedModel by viewModel.activeLoadedModel.collectAsState()
    val loadedModelsPool by viewModel.loadedModelsPool.collectAsState()
    val quantizationHistory by viewModel.quantizationHistory.collectAsState()
    val realtimeTelemetry by viewModel.realtimeTelemetry.collectAsState()
    val serverState by viewModel.serverState.collectAsState()
    val hardware by viewModel.hardwareProfile.collectAsState()
    val dlState by viewModel.downloadingState.collectAsState()
    val quantState by viewModel.quantizationState.collectAsState()
    val storageVolumes by viewModel.storageVolumes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedSectionTab by remember { mutableStateOf("MODELS") } // "MODELS", "HISTORY", "MEMORY_POOL"

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
    var selectedDownloadStorage by remember { mutableStateOf("INTERNAL") }
    var customDownloadPath by remember { mutableStateOf("") }

    // Dialog state for Model Quantization Utility
    var modelToQuantize by remember { mutableStateOf<AiModelEntity?>(null) }
    var showQuantLogTerminal by remember { mutableStateOf(true) }

    // File picker launcher for opening model files from storage or SD card
    val modelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
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
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            customDownloadPath = it.toString()
            selectedDownloadStorage = "CUSTOM"
        }
    }

    val availRamMb = ((hardware?.availableRamGb ?: 4.0f) * 1024).toInt()
    val downloadedModels = models.filter { it.isDownloaded }
    val heavyModelsExceedingRam = downloadedModels.filter { it.ramRequiredMb > availRamMb && !it.name.contains("Q4") && !it.name.contains("Q3") && !it.name.contains("Q2") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SoraSectionHeader(
                    title = "Model Manager & Multi-Model Engine",
                    subtitle = "Load concurrent models • Recursive Quantization (5-5000x) • Live RAM/CPU telemetry",
                    icon = Icons.Default.FolderZip,
                    actionText = "+ Import Model",
                    onActionClick = { showImportDialog = true }
                )
            }

            // Real-time Hardware Telemetry Bar
            item {
                SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (realtimeTelemetry.isThermalThrottled) AccentRed else AccentGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE SYSTEM TELEMETRY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "CPU: ${realtimeTelemetry.cpuUsagePercent}% (${realtimeTelemetry.activeCores} cores) • RAM: ${realtimeTelemetry.usedRamMb}MB / ${realtimeTelemetry.totalRamMb}MB (${realtimeTelemetry.freeRamMb}MB Free)",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(
                                text = "${realtimeTelemetry.inferenceFpsBenchmark} FPS",
                                color = AccentGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.exportExecutionLogs(context) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export Logs", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Storage Verification & Physical File Integrity Banner
            item {
                val scanState by viewModel.storageScanProgress.collectAsState()
                SoraGlassCard(borderColor = if (downloadedModels.isEmpty()) TextSecondary.copy(alpha = 0.4f) else AccentGreen) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (downloadedModels.isNotEmpty()) Icons.Default.Verified else Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = if (downloadedModels.isNotEmpty()) AccentGreen else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Physical Storage Integrity",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Verified on disk: ${downloadedModels.size} model(s) • Magic-bytes verified",
                                        fontSize = 11.sp,
                                        color = if (downloadedModels.isNotEmpty()) AccentGreen else TextSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.scanStorageForModels() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                enabled = !scanState.isScanning
                            ) {
                                if (scanState.isScanning) {
                                    CircularProgressIndicator(color = DeepDarkBg, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scanning...", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify Storage", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (scanState.isScanning) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = NeonCyan,
                                trackColor = GlassSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Main Screen Section Selector Tabs (Models / History / Memory Pool)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPill(
                        label = "All Models (${models.size})",
                        icon = Icons.Default.Widgets,
                        isSelected = selectedSectionTab == "MODELS",
                        onClick = { selectedSectionTab = "MODELS" },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        label = "Memory Pool (${loadedModelsPool.size})",
                        icon = Icons.Default.Layers,
                        isSelected = selectedSectionTab == "MEMORY_POOL",
                        onClick = { selectedSectionTab = "MEMORY_POOL" },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        label = "History (${quantizationHistory.size})",
                        icon = Icons.Default.History,
                        isSelected = selectedSectionTab == "HISTORY",
                        onClick = { selectedSectionTab = "HISTORY" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // =========================================================================
            // SECTION: QUANTIZATION HISTORY VIEW
            // =========================================================================
            if (selectedSectionTab == "HISTORY") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantization Audit Log & Tradeoff History",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        if (quantizationHistory.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearAllQuantizationHistory() }) {
                                Text("Clear History", fontSize = 11.sp, color = AccentRed)
                            }
                        }
                    }
                }

                if (quantizationHistory.isEmpty()) {
                    item {
                        SoraGlassCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.HistoryEdu, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "No Quantization Jobs Executed Yet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Quantize any downloaded model to view before vs after metrics here", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(quantizationHistory) { item ->
                        SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.4f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SoraBadge(text = item.precisionFormat, color = NeonCyan)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SoraBadge(text = "${item.iterationsCount} PASSES", color = NeonPurple)
                                        if (item.isRequantized) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            SoraBadge(text = "🔁 RE-QUANTIZED", color = ElectricPink)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = item.quantizedModelName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "Source: ${item.sourceModelName}", fontSize = 11.sp, color = TextSecondary)
                                }

                                IconButton(onClick = { viewModel.deleteQuantizationHistoryEntry(item.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = GlassSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "RAM Reduction", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = "${item.originalRamMb}MB ➔ ${item.quantizedRamMb}MB (-${item.ramSavedPercent}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen
                                    )
                                }
                                Column {
                                    Text(text = "Disk Size", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = "${item.originalSizeBytes / (1024 * 1024)}MB ➔ ${item.quantizedSizeBytes / (1024 * 1024)}MB",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                                Column {
                                    Text(text = "Inference Speed", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = "${item.benchmarkSpeedBefore.take(7)} ➔ ${item.benchmarkSpeedAfter.take(7)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricPink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION: MULTI-MODEL MEMORY POOL VIEW (LOAD 2 TO INFINITE MODELS)
            // =========================================================================
            if (selectedSectionTab == "MEMORY_POOL") {
                item {
                    SoraGlassCard(borderColor = AccentGreen) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "⚡ Concurrent Multi-Model Memory Pool", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                    Text(text = "Load 2 to multiple AI models concurrently in RAM without unloading", fontSize = 11.sp, color = TextSecondary)
                                }
                                if (loadedModelsPool.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.unloadActiveModel() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                                    ) {
                                        Text("Unload All", fontSize = 11.sp)
                                    }
                                }
                            }

                            val totalPoolRamMb = loadedModelsPool.sumOf { it.ramRequiredMb }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlassSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Active Models in RAM: ${loadedModelsPool.size}", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "Combined RAM Footprint: ${totalPoolRamMb}MB", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (loadedModelsPool.isEmpty()) {
                    item {
                        SoraGlassCard {
                            Text(
                                text = "No models currently loaded in memory. Tap 'Load' or '⚡ Force' on any model in the list to load it into the concurrent pool.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(loadedModelsPool) { model ->
                        SoraGlassCard(borderColor = AccentGreen.copy(alpha = 0.6f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SoraBadge(text = model.format, color = NeonCyan)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SoraBadge(text = "${model.ramRequiredMb}MB RAM", color = AccentGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = model.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = model.description, fontSize = 11.sp, color = TextSecondary)
                                }

                                Button(
                                    onClick = { viewModel.unloadSpecificModel(model.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Unload", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECTION: ALL MODELS / DEFAULT VIEW
            // =========================================================================
            if (selectedSectionTab == "MODELS") {
                // Active Quantization Card
                val currentQuant = quantState
                if (currentQuant != null) {
                    item {
                        SoraGlassCard(borderColor = if (currentQuant.isFinished) AccentGreen else NeonCyan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (currentQuant.isFinished) Icons.Default.CheckCircle else Icons.Default.Compress,
                                            contentDescription = null,
                                            tint = if (currentQuant.isFinished) AccentGreen else NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (currentQuant.isFinished) "Quantization Complete!" else "Quantizing Model...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentQuant.isFinished) AccentGreen else NeonCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${currentQuant.originalModelName} ➔ ${currentQuant.targetPrecision.id} (${currentQuant.totalIterations} passes)",
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                                SoraBadge(text = "${currentQuant.progressPercent}%", color = if (currentQuant.isFinished) AccentGreen else NeonCyan)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { currentQuant.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (currentQuant.isFinished) AccentGreen else NeonCyan,
                                trackColor = GlassSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "RAM Footprint: ${currentQuant.originalRamMb}MB ➔ ${currentQuant.estimatedQuantizedRamMb}MB (-${currentQuant.ramSavedPercent}%) • Speed: ${currentQuant.benchmarkSpeedBefore} ➔ ${currentQuant.benchmarkSpeedAfter}",
                                fontSize = 11.sp,
                                color = AccentGreen
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                if (currentQuant.isFinished) {
                                    Button(
                                        onClick = {
                                            currentQuant.resultingModel?.let { viewModel.loadModelForServer(it, keepOthers = true) }
                                            viewModel.clearQuantizationState()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Load in Memory Pool", color = DeepDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(onClick = { viewModel.clearQuantizationState() }) {
                                        Text("Dismiss", fontSize = 11.sp)
                                    }
                                } else {
                                    OutlinedButton(onClick = { viewModel.cancelModelQuantization() }) {
                                        Text("Cancel", fontSize = 11.sp, color = AccentRed)
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Download Progress Monitor
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
                        }
                    }
                }

                // Low-RAM Warning Banner
                if (heavyModelsExceedingRam.isNotEmpty()) {
                    item {
                        SoraGlassCard(borderColor = ElectricPink) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(ElectricPink.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Low-RAM Device (${availRamMb}MB Free)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${heavyModelsExceedingRam.size} models exceed RAM. Quantize with 5-5000 iterations to run smoothly.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_search_input"),
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
                        item { ModelFilterChip("Downloaded (${downloadedModels.size})", "DOWNLOADED", selectedFilter) { selectedFilter = "DOWNLOADED" } }
                        item { ModelFilterChip("Quantized", "QUANTIZED", selectedFilter) { selectedFilter = "QUANTIZED" } }
                        item { ModelFilterChip("GGUF", "GGUF", selectedFilter) { selectedFilter = "GGUF" } }
                        item { ModelFilterChip("LiteRT", "LITERET", selectedFilter) { selectedFilter = "LITERET" } }
                        item { ModelFilterChip("ONNX", "ONNX", selectedFilter) { selectedFilter = "ONNX" } }
                    }
                }

                // Model list
                val filteredModels = models.filter { model ->
                    val matchesSearch = model.name.contains(searchQuery, ignoreCase = true) || model.description.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = when (selectedFilter) {
                        "DOWNLOADED" -> model.isDownloaded
                        "QUANTIZED" -> model.name.contains("Q4") || model.name.contains("Q3") || model.name.contains("Q2") || model.name.contains("INT8") || model.description.contains("Quantized", ignoreCase = true)
                        "GGUF" -> model.format.equals("GGUF", ignoreCase = true)
                        "LITERET" -> model.format.equals("LITERET", ignoreCase = true)
                        "ONNX" -> model.format.equals("ONNX", ignoreCase = true)
                        else -> true
                    }
                    matchesSearch && matchesFilter
                }

                items(filteredModels) { model ->
                    val isCompatible = availRamMb >= model.ramRequiredMb
                    val isLoadedInPool = loadedModelsPool.any { it.id == model.id }
                    val isQuantizedVariant = model.name.contains("Q4") || model.name.contains("Q3") || model.name.contains("Q2") || model.name.contains("INT8")

                    SoraGlassCard(
                        borderColor = when {
                            isLoadedInPool -> AccentGreen
                            isQuantizedVariant -> NeonCyan.copy(alpha = 0.5f)
                            isCompatible -> GlassSurfaceVariant
                            else -> ElectricPink.copy(alpha = 0.4f)
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
                                        color = if (isCompatible) AccentGreen else ElectricPink,
                                        textColor = TextPrimary
                                    )
                                    if (isQuantizedVariant) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SoraBadge(text = "⚡ QUANTIZED", color = NeonCyan)
                                    }
                                    if (isLoadedInPool) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SoraBadge(text = "ACTIVE IN RAM", color = AccentGreen)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = model.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = model.description, fontSize = 11.sp, color = TextSecondary)

                                if (model.isDownloaded && model.localPath != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📁 ${model.storageLocation} • ${String.format("%.1f", model.sizeBytes / (1024f * 1024f))} MB • Verified Magic Bytes",
                                        fontSize = 10.sp,
                                        color = AccentGreen
                                    )
                                    Text(
                                        text = model.localPath?.takeLast(40) ?: "",
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (model.isDownloaded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.deleteModelPermanently(model) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete Model File",
                                            tint = AccentRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { modelToQuantize = model },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                        modifier = Modifier.testTag("quantize_model_btn_${model.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isQuantizedVariant) "Re-Quantize" else "Quantize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (isLoadedInPool) {
                                        OutlinedButton(
                                            onClick = { viewModel.unloadSpecificModel(model.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                                        ) {
                                            Text("Unload", fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.loadModelForServer(model, keepOthers = true) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isCompatible) AccentGreen else NeonPurple)
                                        ) {
                                            Text(
                                                text = if (isCompatible) "+ Load" else "⚡ Force",
                                                fontSize = 11.sp,
                                                color = DeepDarkBg,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { modelToDownload = model },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                                ) {
                                    Icon(imageVector = Icons.Default.GetApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
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

    // =========================================================================
    // ADVANCED RECURSIVE QUANTIZATION DIALOG (5-5000 Iterations & 4 Tradeoffs)
    // =========================================================================
    val quantModel = modelToQuantize
    if (quantModel != null) {
        var selectedPrecision by remember { mutableStateOf(QuantizationPrecision.Q4_K_M) }
        var selectedObjective by remember { mutableStateOf(QuantizationTradeoffObjective.BALANCED_MULTI_OBJECTIVE) }
        var iterationsCount by remember { mutableStateOf(if (quantModel.sizeBytes > 1_500_000_000L) 250 else 50) }
        var selectedStorage by remember { mutableStateOf("INTERNAL") }
        var chunkSizeMb by remember { mutableStateOf(64) }
        var cpuThreads by remember { mutableStateOf(4) }

        val origRam = quantModel.ramRequiredMb
        val targetRam = ((origRam * selectedPrecision.ramReductionFactor) * selectedObjective.ramFactorMultiplier).toInt().coerceAtLeast(350)
        val ramSavedMb = (origRam - targetRam).coerceAtLeast(0)
        val ramSavedPct = if (origRam > 0) ((ramSavedMb.toFloat() / origRam.toFloat()) * 100).toInt() else 50
        val isAlreadyQuant = quantModel.name.contains("Q4") || quantModel.name.contains("Q3") || quantModel.name.contains("Q2") || quantModel.name.contains("INT8")

        AlertDialog(
            onDismissRequest = { modelToQuantize = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Compress, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isAlreadyQuant) "Recursive Re-Quantization Utility" else "Advanced Model Quantization",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAlreadyQuant) {
                        Surface(
                            color = ElectricPink.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPink.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "This model is already quantized. Re-quantization will apply recursive multi-pass calibration to compress it further.",
                                    fontSize = 10.5.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Target Precision Format
                    Text(text = "Target Precision Format:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(QuantizationPrecision.entries) { precision ->
                            val isSelected = selectedPrecision == precision
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonCyan else GlassSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else GlassSurfaceVariant),
                                modifier = Modifier.clickable { selectedPrecision = precision }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(text = precision.id, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) DeepDarkBg else TextPrimary)
                                    Text(text = "${precision.bitsPerWeight} bits", fontSize = 9.sp, color = if (isSelected) DeepDarkBg.copy(alpha = 0.8f) else TextSecondary)
                                }
                            }
                        }
                    }

                    // 4 Quantization Tradeoff Dimensions
                    Text(text = "Quantization Trade-off Objective:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(QuantizationTradeoffObjective.entries) { objective ->
                            val isSelected = selectedObjective == objective
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonPurple else GlassSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonPurple else GlassSurfaceVariant),
                                modifier = Modifier.clickable { selectedObjective = objective }
                            ) {
                                Text(
                                    text = objective.badgeLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = selectedObjective.description,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )

                    // Optimization Iterations (5 to 5000 for large, 3 to 1000 for low)
                    val maxIter = if (quantModel.sizeBytes > 1_500_000_000L) 5000 else 1000
                    val minIter = if (quantModel.sizeBytes > 1_500_000_000L) 5 else 3

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Iterations ($minIter to $maxIter passes):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "$iterationsCount passes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(minIter, 25, 100, 500, if (maxIter == 5000) 2500 else 1000).forEach { count ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (iterationsCount == count) NeonCyan else GlassSurface,
                                modifier = Modifier.clickable { iterationsCount = count }
                            ) {
                                Text(
                                    text = "${count}x",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (iterationsCount == count) DeepDarkBg else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // RAM & Savings Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Memory Footprint", fontSize = 10.sp, color = TextSecondary)
                                Text(text = "${origRam}MB ➔ ${targetRam}MB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                            }
                            Column {
                                Text(text = "RAM Saved", fontSize = 10.sp, color = TextSecondary)
                                Text(text = "-$ramSavedPct% ($ramSavedMb MB)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricPink)
                            }
                            Column {
                                Text(text = "Estimated Speed", fontSize = 10.sp, color = TextSecondary)
                                Text(text = "+${((selectedObjective.speedMultiplier - 1f) * 100).toInt()}% FPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                        }
                    }

                    // Storage Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedStorage == "INTERNAL") NeonCyan.copy(alpha = 0.2f) else GlassSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedStorage == "INTERNAL") NeonCyan else GlassSurfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStorage = "INTERNAL" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "📱 Phone Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedStorage == "SD_CARD") AccentGreen.copy(alpha = 0.2f) else GlassSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedStorage == "SD_CARD") AccentGreen else GlassSurfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedStorage = "SD_CARD" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "💾 SD Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startModelQuantization(
                            model = quantModel,
                            precision = selectedPrecision,
                            tradeoffObjective = selectedObjective,
                            iterationsCount = iterationsCount,
                            storageType = selectedStorage,
                            chunkSizeMb = chunkSizeMb,
                            cpuThreads = cpuThreads
                        )
                        modelToQuantize = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Execute Quantization ($iterationsCount Passes)", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToQuantize = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Manual Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Model from Phone / SD Card", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { modelFilePicker.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱 Phone Files", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { modelFilePicker.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 SD Card", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
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
                        label = { Text("File Path / URI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
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

    // Choose Download Destination Dialog
    val targetModel = modelToDownload
    if (targetModel != null) {
        AlertDialog(
            onDismissRequest = { modelToDownload = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Destination", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Save \"${targetModel.name}\" to:", fontSize = 13.sp, color = TextPrimary)

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
                                        text = "${String.format("%.1f", volume.freeSpaceGb)} GB Free of ${String.format("%.1f", volume.totalSpaceGb)} GB • Real Storage",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Custom SAF directory
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
fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) NeonCyan else GlassSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else GlassSurfaceVariant),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) DeepDarkBg else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) DeepDarkBg else TextPrimary
            )
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
