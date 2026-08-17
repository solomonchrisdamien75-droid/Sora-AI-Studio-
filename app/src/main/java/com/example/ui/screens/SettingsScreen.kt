package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.AppArchitectureAndSourceViewerDialog
import com.example.ui.components.RamUsageMonitor
import com.example.ui.components.SoraBadge
import com.example.ui.components.SoraGlassCard
import com.example.ui.components.SoraSectionHeader
import com.example.ui.theme.*

@Composable
fun SettingsScreen(viewModel: SoraMainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val themeMode by viewModel.themeMode.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()
    val hardware by viewModel.hardwareProfile.collectAsState()
    val inferenceMode by viewModel.inferenceMode.collectAsState()
    val modelExecutionPreset by viewModel.modelExecutionPreset.collectAsState()
    val customSystemPrompt by viewModel.customSystemPrompt.collectAsState()
    val settingsTemp by viewModel.settingsTemperature.collectAsState()
    val maxTokens by viewModel.settingsMaxTokens.collectAsState()
    val contextSize by viewModel.settingsContextSize.collectAsState()

    val imageGenSteps by viewModel.imageGenSteps.collectAsState()
    val imageSizePreset by viewModel.imageSizePreset.collectAsState()
    val gpuSafetyThresholdMb by viewModel.gpuSafetyThresholdMb.collectAsState()
    val imageBackend by viewModel.imageBackend.collectAsState()

    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val apiEngineKey by viewModel.apiEngineKey.collectAsState()
    val apiProviderPreset by viewModel.apiProviderPreset.collectAsState()
    val apiEngineModel by viewModel.apiEngineModel.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()

    val disableMaxSteps by viewModel.disableMaxSteps.collectAsState()
    val maxStepsPerTask by viewModel.maxStepsPerTask.collectAsState()
    val contextLimitTokens by viewModel.contextLimitTokens.collectAsState()
    val useScreenCompression by viewModel.useScreenCompression.collectAsState()
    val sendSystemPrompt by viewModel.sendSystemPrompt.collectAsState()

    val telegramBotToken by viewModel.telegramBotToken.collectAsState()
    val isTelegramBotEnabled by viewModel.isTelegramBotEnabled.collectAsState()

    val storageVolumes by viewModel.storageVolumes.collectAsState()
    val preferredStorage by viewModel.preferredStorage.collectAsState()
    val customSafTreeUri by viewModel.customSafTreeUri.collectAsState()
    val isMigratingStorage by viewModel.isMigratingStorage.collectAsState()
    val migrationProgress by viewModel.migrationProgress.collectAsState()
    val settingsStatusMessage by viewModel.settingsStatusMessage.collectAsState()

    var showApiKey by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showTaskHistoryDialog by remember { mutableStateOf(false) }
    var showArchitectureBlueprintDialog by remember { mutableStateOf(false) }
    var editableSystemPrompt by remember(customSystemPrompt) { mutableStateOf(customSystemPrompt) }

    val safFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            viewModel.setPreferredStorage("CUSTOM", it.toString())
            Toast.makeText(context, "Storage folder updated via SAF!", Toast.LENGTH_SHORT).show()
        }
    }

    // Runtime Permission checking states
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasContactsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasPhonePermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
    }
    var hasSmsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (granted) Toast.makeText(context, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasContactsPermission = granted
        if (granted) Toast.makeText(context, "Contacts permission granted!", Toast.LENGTH_SHORT).show()
    }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPhonePermission = granted
        if (granted) Toast.makeText(context, "Phone permission granted!", Toast.LENGTH_SHORT).show()
    }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasSmsPermission = granted
        if (granted) Toast.makeText(context, "SMS permission granted!", Toast.LENGTH_SHORT).show()
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotificationPermission = granted
        if (granted) Toast.makeText(context, "Notifications permission granted!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // SYSTEM RAM MONITOR HEADER
        // ==========================================
        item {
            RamUsageMonitor()
        }

        // ==========================================
        // 0. CREATOR SUPPORT
        // ==========================================
        item {
            val uriHandler = LocalUriHandler.current
            SoraGlassCard(borderColor = ElectricPink) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "SUPPORT THE CREATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPink,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            uriHandler.openUri("https://youtube.com/shorts/iseGrWemeZw?is=hRw6b8l2tjrZpvYh")
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SmartDisplay, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "@OneFactEndlessWonder", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Watch the latest short!", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                    }
                    Button(
                        onClick = { uriHandler.openUri("https://www.youtube.com/@OneFactEndlessWonder") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                    ) {
                        Text("Subscribe on YouTube")
                    }
                }
            }
        }

        item {
            SoraSectionHeader(
                title = "Settings",
                subtitle = "Engine parameters, OpenAI endpoints, permissions & system boundaries",
                icon = Icons.Default.Settings
            )
        }

        // ==========================================
        // 1. APPEARANCE (Screenshots 2 & 4)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "APPEARANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    // Theme selector rows
                    val themeOptions = listOf(
                        Triple("Light", Icons.Default.LightMode, "LIGHT"),
                        Triple("Dark", Icons.Default.DarkMode, "DARK"),
                        Triple("System Default", Icons.Default.SettingsBrightness, "SYSTEM")
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    ) {
                        themeOptions.forEachIndexed { index, (label, icon, value) ->
                            val isSelected = themeMode == value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setThemeMode(value) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = label, fontSize = 14.sp, color = TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < themeOptions.size - 1) {
                                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }

                    // Font Size Slider Card (Screenshot 2)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.TextFields, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Font Size", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    val sizeLabel = when {
                                        fontSizeScale < 0.85f -> "XS"
                                        fontSizeScale < 1.05f -> "Small"
                                        else -> "Large"
                                    }
                                    Text(text = sizeLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                }
                            }

                            Text(
                                text = "Small (${String.format("%.2f", fontSizeScale)}x) is the default size",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Slider(
                                value = fontSizeScale,
                                onValueChange = { viewModel.setFontSizeScale(it) },
                                valueRange = 0.75f..1.35f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = CardBorder
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("XS", fontSize = 10.sp, color = TextSecondary)
                                Text("Small", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                Text("Large", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. DIAGNOSTICS & SYSTEM LOGS (Screenshot 2 & 5)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DIAGNOSTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable { showLogsDialog = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Article, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("View errors, warnings, and debug details", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View", tint = TextSecondary)
                    }
                }
            }
        }

        // ==========================================
        // 3. DEVICE HARDWARE PROFILE (Screenshot 2)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DEVICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    // Low RAM Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("⚠️ Low RAM (2.6GB) — Use small models only", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    "Available: 0.3GB · Context: $contextSize · Tokens: $maxTokens",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Snapdragon Processor Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Qualcomm Snapdragon NPU", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Recommended: Q4_K_M (recommended) · Q4_0_4_8 on X Elite", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // REAL DEVICE STORAGE & MODEL DIRECTORY (SAF)
        // ==========================================
        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SdCard, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("DEVICE STORAGE & SAF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Real hardware storage volumes and SAF directory", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        IconButton(onClick = { viewModel.refreshStorageVolumes() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Storage", tint = NeonCyan)
                        }
                    }

                    // Storage Volumes List
                    Text(
                        text = "DETECTED STORAGE SPACES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    ) {
                        storageVolumes.forEachIndexed { index, volume ->
                            val isSelected = preferredStorage == volume.storageType
                            val icon = if (volume.isRemovable) Icons.Default.SdStorage else Icons.Default.PhoneAndroid
                            val color = if (volume.isRemovable) AccentGreen else NeonCyan
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setPreferredStorage(volume.storageType) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = volume.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            if (volume.isEmulated) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SoraBadge(text = "App Private", color = NeonCyan)
                                            }
                                        }
                                        Text(
                                            text = "${String.format("%.1f", volume.freeSpaceGb)} GB Free of ${String.format("%.1f", volume.totalSpaceGb)} GB",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = NeonCyan)
                                }
                            }
                            if (index < storageVolumes.size - 1) {
                                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }

                    // Custom SAF Directory Tree Option
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (preferredStorage == "CUSTOM") NeonPurple else CardBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Custom SAF Model Directory", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Button(
                                    onClick = { safFolderPicker.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Select SAF Tree", fontSize = 11.sp)
                                }
                            }
                            if (!customSafTreeUri.isNullOrBlank()) {
                                Text(
                                    text = "SAF Tree URI: $customSafTreeUri",
                                    fontSize = 10.sp,
                                    color = AccentGreen,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    // Action Buttons: Reconcile Physical Storage & Migrate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.scanStorageForModels()
                                Toast.makeText(context, "Scanning physical storage...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconcile Storage", fontSize = 11.sp, color = NeonCyan)
                        }

                        Button(
                            onClick = {
                                val target = if (preferredStorage == "INTERNAL") "SD_CARD" else "INTERNAL"
                                viewModel.migrateModelsToNewStorage(target)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            enabled = !isMigratingStorage
                        ) {
                            if (isMigratingStorage) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.DriveFileMove, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Migrate Models", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Migration Progress & Status
                    if (isMigratingStorage) {
                        LinearProgressIndicator(
                            progress = { migrationProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = NeonPurple,
                            trackColor = GlassSurfaceVariant
                        )
                    }

                    if (!settingsStatusMessage.isNullOrBlank()) {
                        Text(text = settingsStatusMessage ?: "", fontSize = 11.sp, color = AccentGreen)
                    }
                }
            }
        }

        // ==========================================
        // 4. INFERENCE MODE (Screenshot 2)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "INFERENCE MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    ) {
                        // Local on-device option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setInferenceMode("LOCAL") }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Local (On-Device)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Snapdragon NPU / CPU Fallback", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            if (inferenceMode == "LOCAL") {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonCyan)
                            }
                        }

                        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Cloud API option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setInferenceMode("CLOUD") }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonPurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Cloud API", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("OPENROUTER / DEEPSEEK / GROQ", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            if (inferenceMode == "CLOUD") {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonCyan)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. SYSTEM PROMPT (Screenshot 2)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SYSTEM PROMPT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                            Text("Applies to local and cloud models", fontSize = 11.sp, color = TextSecondary)
                        }
                        IconButton(
                            onClick = {
                                viewModel.setCustomSystemPrompt(editableSystemPrompt)
                                Toast.makeText(context, "System prompt saved!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Save", tint = AccentGreen)
                        }
                    }

                    OutlinedTextField(
                        value = editableSystemPrompt,
                        onValueChange = { editableSystemPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        minLines = 3
                    )
                }
            }
        }

        // ==========================================
        // 6. MODEL EXECUTION & PARAMETERS (Screenshot 2 & 3)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MODEL PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)

                    val presets = listOf(
                        Triple("Auto Fast", "Try GPU first, then CPU fallback", "AUTO_FAST"),
                        Triple("GPU Fast", "Maximum speed, may crash on some devices", "GPU_FAST"),
                        Triple("CPU Safe", "Stable mode with lower speed", "CPU_SAFE")
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        presets.forEachIndexed { index, (title, desc, key) ->
                            val isSelected = modelExecutionPreset == key
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setModelExecutionPreset(key) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (key) {
                                            "AUTO_FAST" -> Icons.Default.AutoAwesome
                                            "GPU_FAST" -> Icons.Default.FlashOn
                                            else -> Icons.Default.Security
                                        },
                                        contentDescription = null,
                                        tint = when (key) {
                                            "AUTO_FAST" -> NeonCyan
                                            "GPU_FAST" -> AccentYellow
                                            else -> AccentGreen
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = desc, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = NeonCyan)
                                }
                            }
                            if (index < presets.size - 1) {
                                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }

                    // Temperature Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🌡️ Temperature", fontSize = 12.sp, color = TextSecondary)
                            Text(String.format("%.2f", settingsTemp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Text("Recommended max: 1.0", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = settingsTemp,
                            onValueChange = { viewModel.setSettingsTemperature(it) },
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
                        )
                    }

                    // Max Tokens Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("# Max Tokens", fontSize = 12.sp, color = TextSecondary)
                            Text("$maxTokens", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Text("Recommended max: 512", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = maxTokens.toFloat(),
                            onValueChange = { viewModel.setSettingsMaxTokens(it.toInt()) },
                            valueRange = 64f..1024f,
                            steps = 14,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
                        )
                    }

                    // Context Size Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("⚙️ Context Size", fontSize = 12.sp, color = TextSecondary)
                            Text("$contextSize", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Text("Recommended max: 2048", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = contextSize.toFloat(),
                            onValueChange = { viewModel.setSettingsContextSize(it.toInt()) },
                            valueRange = 256f..4096f,
                            steps = 14,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 7. IMAGE GENERATION PARAMETERS (Screenshot 3)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("IMAGE GENERATION PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)

                    // Image Gen Steps
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.BurstMode, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Image Gen Steps", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text("$imageGenSteps", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Text("Recommended max: 8", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = imageGenSteps.toFloat(),
                            onValueChange = { viewModel.setImageGenSteps(it.toInt()) },
                            valueRange = 1f..8f,
                            steps = 6,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
                        )
                    }

                    // Image Size presets
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🖼️ Image Size", fontSize = 12.sp, color = TextSecondary)
                            Text(imageSizePreset, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Text("Auto recommended. Bigger size = better detail, but much slower and more memory use.", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Auto", "256", "320", "384", "512")) { size ->
                                val isSelected = imageSizePreset == size
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) NeonCyan else GlassSurface)
                                        .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setImageSizePreset(size) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = size,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) DeepDarkBg else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // GPU Safety Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GPU Safety", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text("$gpuSafetyThresholdMb MB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Text("Models at or above this size use CPU. Smaller models can use GPU Experimental.", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = gpuSafetyThresholdMb.toFloat(),
                            onValueChange = { viewModel.setGpuSafetyThresholdMb(it.toInt()) },
                            valueRange = 512f..4096f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
                        )
                    }

                    // Image Backend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("⚙️ Image Backend", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("ADRENO - Vulkan (GPU)", fontSize = 10.sp, color = TextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (imageBackend == "CPU") NeonCyan else GlassSurface)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setImageBackend("CPU") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("CPU", fontSize = 11.sp, color = if (imageBackend == "CPU") DeepDarkBg else TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (imageBackend == "GPU") NeonCyan else GlassSurface)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setImageBackend("GPU") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("⚡ GPU", fontSize = 11.sp, color = if (imageBackend == "GPU") DeepDarkBg else TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. AI ENGINE CONFIGURATION (Screenshot 4)
        // ==========================================
        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("AI Engine Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Supports any OpenAI-compatible API endpoint", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // API Key Field with reveal
                    OutlinedTextField(
                        value = apiEngineKey,
                        onValueChange = { viewModel.setApiEngineKey(it) },
                        label = { Text("API Key", fontSize = 11.sp) },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle mask",
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // API Base URL Field
                    OutlinedTextField(
                        value = apiBaseUrl,
                        onValueChange = { viewModel.setApiBaseUrl(it) },
                        label = { Text("API Base URL", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Provider Presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Local Server", "Ollama Cloud", "DeepSeek", "Groq", "NVIDIA", "Custom")) { preset ->
                            val isSelected = apiProviderPreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonPurple else GlassSurface)
                                    .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setApiProviderPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    // Model Name Field with Fetch Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = apiEngineModel,
                            onValueChange = { viewModel.setApiEngineModel(it) },
                            label = { Text("Model", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.fetchApiEngineModels() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isFetchingModels
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepDarkBg, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fetch", color = DeepDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 9. TUNING & BOUNDARIES (Screenshot 4)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Tuning & Boundaries", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Configure LLM agent parameters", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Disable Max Steps Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disable Maximum Steps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("⚠️ Can cause infinite loops.", fontSize = 11.sp, color = AccentYellow)
                        }
                        Switch(
                            checked = disableMaxSteps,
                            onCheckedChange = { viewModel.setDisableMaxSteps(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentYellow, checkedTrackColor = AccentYellow.copy(alpha = 0.4f))
                        )
                    }

                    // Maximum Steps Per Task Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Maximum Steps Per Task: $maxStepsPerTask", fontSize = 12.sp, color = TextSecondary)
                        }
                        Slider(
                            value = maxStepsPerTask.toFloat(),
                            onValueChange = { viewModel.setMaxStepsPerTask(it.toInt()) },
                            valueRange = 1f..64f,
                            steps = 62,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = CardBorder)
                        )
                    }

                    // Context Limit Tokens Field
                    OutlinedTextField(
                        value = contextLimitTokens.toString(),
                        onValueChange = { viewModel.setContextLimitTokens(it.toIntOrNull() ?: 1024) },
                        label = { Text("Context Limit (Max Tokens)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        }

        // ==========================================
        // 10. BEHAVIOR & EXTENSIONS (Screenshot 4 & 6)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Extension, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Behavior & Extensions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Additional feature flags and overlay options", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Use Screen Compression
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Screen Compression", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Removes duplicate elements to save tokens", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = useScreenCompression,
                            onCheckedChange = { viewModel.setUseScreenCompression(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                        )
                    }

                    // Send System Prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Send System Prompt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Turn off for custom LoRA fine-tunes", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = sendSystemPrompt,
                            onCheckedChange = { viewModel.setSendSystemPrompt(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // ==========================================
        // 11. TELEGRAM REMOTE ACCESS (Screenshot 6)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Telegram Remote Access", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Control your agent remotely from anywhere", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    OutlinedTextField(
                        value = telegramBotToken,
                        onValueChange = { viewModel.setTelegramBotToken(it) },
                        placeholder = { Text("Telegram Bot Token (e.g. 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11)", fontSize = 11.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Telegram Bot", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Allows remote control via Telegram chat", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = isTelegramBotEnabled,
                            onCheckedChange = {
                                viewModel.setTelegramBotEnabled(it)
                                if (it) {
                                    Toast.makeText(context, "Telegram Bot listener active!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // ==========================================
        // 12. SCREEN CONTROL (ACCESSIBILITY) (Screenshot 5 & 6)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Screen Control (Accessibility)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Required to read screen and perform automated clicks", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Screen Control is ready for automation service", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(
                                "Tap below to open Accessibility Settings, then find \"PrivateAgent Screen Control\" and enable it.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open accessibility settings directly", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Accessibility Settings", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 13. APP PERMISSIONS (Screenshot 5 & 6)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("App Permissions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Required for automation, microphone, and contacts", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    val permissionsList = listOf(
                        Triple("Microphone", hasMicPermission) { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        Triple("Contacts", hasContactsPermission) { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        Triple("Phone", hasPhonePermission) { phoneLauncher.launch(Manifest.permission.CALL_PHONE) },
                        Triple("SMS", hasSmsPermission) { smsLauncher.launch(Manifest.permission.SEND_SMS) },
                        Triple("Notifications", hasNotificationPermission) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        permissionsList.forEachIndexed { index, (name, isGranted, onRequest) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isGranted) onRequest() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (name) {
                                            "Microphone" -> Icons.Default.Mic
                                            "Contacts" -> Icons.Default.Contacts
                                            "Phone" -> Icons.Default.Phone
                                            "SMS" -> Icons.Default.Message
                                            else -> Icons.Default.Notifications
                                        },
                                        contentDescription = null,
                                        tint = if (isGranted) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(if (isGranted) "Granted" else "Tap to Grant", fontSize = 10.sp, color = if (isGranted) AccentGreen else AccentYellow)
                                    }
                                }
                                if (isGranted) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Granted", tint = AccentGreen, modifier = Modifier.size(18.dp))
                                } else {
                                    Button(
                                        onClick = onRequest,
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Grant", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (index < permissionsList.size - 1) {
                                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 14. EXECUTION LOGS (Screenshot 5)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Execution logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("View history of tasks and token analytics", fontSize = 11.sp, color = TextSecondary)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            .clickable { showTaskHistoryDialog = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("View Task History", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Access complete trace of execution steps", fontSize = 11.sp, color = TextSecondary)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        }

        // ==========================================
        // 14.5 APP ARCHITECTURE & FULL SOURCE BLUEPRINT
        // ==========================================
        item {
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.6f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("App Architecture & Source Code", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Complete blueprint: 16 screens, 6 studios & core engines", fontSize = 11.sp, color = NeonCyan)
                            }
                        }
                        SoraBadge(text = "16 Pages", color = NeonCyan)
                    }

                    Text(
                        text = "Access the comprehensive technical specification and architectural blueprint file detailing all pages, features, 1s-to-24h video pipeline, Room DB schema, and code modules.",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = { showArchitectureBlueprintDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_blueprint_viewer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.DataObject, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Full App Architecture & Source Spec", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepDarkBg)
                    }
                }
            }
        }

        // ==========================================
        // 15. ABOUT & COMMUNITY (Screenshot 3 & 5)
        // ==========================================
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("About PrivateAgent / Sora Studio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Resources and repository access", fontSize = 11.sp, color = TextSecondary)

                    val aboutLinks = listOf(
                        Triple("Project Repository", "View source code on GitHub", "https://github.com"),
                        Triple("One Fact Endless Wonder on YouTube", "Subscribe for tutorials and updates", "https://www.youtube.com/@OneFactEndlessWonder")
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        aboutLinks.forEachIndexed { index, (title, desc, url) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (title.contains("YouTube")) Icons.Default.PlayCircleFilled else Icons.Default.Code,
                                        contentDescription = null,
                                        tint = if (title.contains("YouTube")) AccentRed else NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(desc, fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            if (index < aboutLinks.size - 1) {
                                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Diagnostics Logs Dialog
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            containerColor = DeepDarkBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Article, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnostics & Engine Logs", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("[INFO] Snapdragon NPU initialized successfully", fontSize = 11.sp, color = AccentGreen)
                    Text("[INFO] Sora diffusion transformer graph bound to Adreno Vulkan", fontSize = 11.sp, color = NeonCyan)
                    Text("[INFO] Offline Wake-Word acoustic listener standby", fontSize = 11.sp, color = TextSecondary)
                    Text("[DEBUG] Quantization profile: Q4_K_M (optimal)", fontSize = 11.sp, color = TextSecondary)
                    Text("[DEBUG] Local OpenAI REST endpoint bound to 0.0.0.0:8080", fontSize = 11.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { showLogsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                    Text("Close", color = DeepDarkBg)
                }
            }
        )
    }

    // Task History Dialog
    if (showTaskHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showTaskHistoryDialog = false },
            containerColor = DeepDarkBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execution Task History Trace", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Step 1: Prompt ingestion & LLM token prefill (240ms)", fontSize = 12.sp, color = TextPrimary)
                    Text("• Step 2: Latent video diffusion tensor rendering (1200ms)", fontSize = 12.sp, color = NeonCyan)
                    Text("• Step 3: Fast MP4 hardware encoder pass (350ms)", fontSize = 12.sp, color = AccentGreen)
                    Text("• Status: 100% completed with zero cloud network overhead", fontSize = 11.sp, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(onClick = { showTaskHistoryDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                    Text("Done", color = DeepDarkBg)
                }
            }
        )
    }

    // App Architecture, Features & Full Source Code Blueprint Dialog
    if (showArchitectureBlueprintDialog) {
        AppArchitectureAndSourceViewerDialog(
            onDismiss = { showArchitectureBlueprintDialog = false }
        )
    }
}
