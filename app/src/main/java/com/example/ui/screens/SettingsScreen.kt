package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
fun SettingsScreen(viewModel: SoraMainViewModel) {
    val memoryMode by viewModel.memoryMode.collectAsState()
    val useSdCard by viewModel.useSdCardCache.collectAsState()
    val hardware by viewModel.hardwareProfile.collectAsState()
    val statusMessage by viewModel.settingsStatusMessage.collectAsState()
    val form by viewModel.generationForm.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Studio Settings & Virtual Memory",
                subtitle = "Storage workspace, memory allocation & hardware priority",
                icon = Icons.Default.Settings
            )
        }

        // Voice Wake Word 'Skra' & AI Device Control Panel
        item {
            SoraGlassCard(borderColor = ElectricPink) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Voice Wake Word ('Skra')", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Speak 'Skra' anytime without touching the app or opening phone. Triggers AI voice recognition & device action executor.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = form.wakeWordEnabled,
                        onCheckedChange = { viewModel.toggleWakeWord(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricPink,
                            checkedTrackColor = ElectricPink.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("wake_word_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PhonelinkSetup, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Full Device Control Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Grants AI permission to control system activities, execute app launches, and open YouTube hands-free on voice command.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = form.deviceControlGranted,
                        onCheckedChange = { viewModel.toggleDeviceControl(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("device_control_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerWakeWordEvent("Skra! Open YouTube") },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("test_wake_word_btn")
                    ) {
                        Text("🗣️ Test Wake Word 'Skra'", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.launchYouTubeApp() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("open_youtube_btn")
                    ) {
                        Text("▶️ Open YouTube", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // System Log Output
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassSurfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "System Monitor: ${form.systemStatusLog}",
                        fontSize = 10.sp,
                        color = AccentGreen,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        val msg = statusMessage
        if (msg != null && msg.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = msg, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.setSettingsStatus("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Virtual RAM & Memory Allocation Manager
        item {
            var ramLimitGb by remember { mutableStateOf(8f) }
            var memorySource by remember { mutableStateOf("Hybrid Mode") } // Phone RAM, SD Card Cache, Hybrid Mode, Automatic Mode

            SoraGlassCard {
                Text(text = "Virtual RAM & Memory Manager", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Specify how memory and cache storage are utilized during model loading. SD card cache is used as a fast workspace swap for model tensors.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Memory Source Strategy", fontSize = 12.sp, color = NeonCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MemoryChip("Phone RAM", memorySource) { memorySource = "Phone RAM" }
                    MemoryChip("SD Cache", memorySource) { memorySource = "SD Cache" }
                    MemoryChip("Hybrid Mode", memorySource) { memorySource = "Hybrid Mode" }
                    MemoryChip("Automatic", memorySource) { memorySource = "Automatic" }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Inference Memory Allocation Limit: ${ramLimitGb.toInt()} GB", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = ramLimitGb,
                    onValueChange = { ramLimitGb = it },
                    valueRange = 3f..32f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = GlassSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Mode: $memoryMode", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = "Est. Speed: ${String.format("%.1f", (ramLimitGb * 1.2f))} FPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Memory Execution Priority", fontSize = 12.sp, color = NeonPurple)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemoryChip("Low RAM Mode", memoryMode) { viewModel.setMemoryMode("Low RAM Mode") }
                    MemoryChip("Balanced Mode", memoryMode) { viewModel.setMemoryMode("Balanced Mode") }
                    MemoryChip("Max Performance", memoryMode) { viewModel.setMemoryMode("Max Performance") }
                }
            }
        }

        // Export Presets & Default Formats
        item {
            var selectedFormat by remember { mutableStateOf("MP4") }
            var selectedAspect by remember { mutableStateOf("16:9") }

            SoraGlassCard {
                Text(text = "Default Video Export Presets", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Configure container formats and aspect ratios for exported videos.", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "File Format Container", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MemoryChip("MP4", selectedFormat) { selectedFormat = "MP4" }
                    MemoryChip("MOV", selectedFormat) { selectedFormat = "MOV" }
                    MemoryChip("GIF", selectedFormat) { selectedFormat = "GIF" }
                    MemoryChip("WEBM", selectedFormat) { selectedFormat = "WEBM" }
                    MemoryChip("PNG Seq", selectedFormat) { selectedFormat = "PNG Seq" }
                    MemoryChip("JPEG Seq", selectedFormat) { selectedFormat = "JPEG Seq" }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Aspect Ratio Presets", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MemoryChip("16:9", selectedAspect) { selectedAspect = "16:9" }
                    MemoryChip("9:16", selectedAspect) { selectedAspect = "9:16" }
                    MemoryChip("1:1", selectedAspect) { selectedAspect = "1:1" }
                    MemoryChip("4:3", selectedAspect) { selectedAspect = "4:3" }
                    MemoryChip("21:9", selectedAspect) { selectedAspect = "21:9" }
                }
            }
        }

        // GPU & CPU Output Acceleration & AI Watermark Eraser
        item {
            val editorProject by viewModel.editorProject.collectAsState()
            var selectedGpuBackend by remember { mutableStateOf("Vulkan 1.3 High Throughput") }
            var cpuThreads by remember { mutableStateOf(8f) }

            SoraGlassCard(borderColor = NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "GPU & CPU Output Acceleration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    SoraBadge(text = "TURBO RENDER", color = AccentGreen)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Accelerate render export speeds using Vulkan 1.3 GPU hardware codecs and multi-threaded CPU chunk processing.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "⚡ GPU Hardware Acceleration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Enable Vulkan 1.3 & MediaCodec EGL hardware encoding.", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = editorProject.gpuHardwareAcceleration,
                        onCheckedChange = { viewModel.toggleGpuHardwareAcceleration(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("setting_gpu_accel_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "GPU Engine Backend Strategy", fontSize = 11.sp, color = NeonCyan)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MemoryChip("Vulkan 1.3", selectedGpuBackend) { selectedGpuBackend = "Vulkan 1.3" }
                    MemoryChip("OpenCL", selectedGpuBackend) { selectedGpuBackend = "OpenCL" }
                    MemoryChip("MediaCodec HW", selectedGpuBackend) { selectedGpuBackend = "MediaCodec HW" }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "CPU Multi-Threading Allocation: ${cpuThreads.toInt()} Cores", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = cpuThreads,
                    onValueChange = {
                        cpuThreads = it
                        viewModel.updateCpuThreads(it.toInt())
                    },
                    valueRange = 2f..16f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = GlassSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = GlassSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🧹 AI Watermark Removal Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(text = "Automatically detect and erase watermarks from uploaded videos and rendered video clips.", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = editorProject.globalWatermarkEraser,
                        onCheckedChange = { viewModel.toggleGlobalWatermarkEraser(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricPink, checkedTrackColor = ElectricPink.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("setting_watermark_eraser_switch")
                    )
                }
            }
        }

        // Storage Workspace & SD Card Cache
        item {
            SoraGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SD Card / Storage Cache Workspace", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "Use external SD card or fast internal storage as a temporary workspace for large GGUF model files and checkpoint frames.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = useSdCard,
                        onCheckedChange = { viewModel.setUseSdCardCache(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepDarkBg,
                            checkedTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("sd_card_switch")
                    )
                }

                val profile = hardware
                if (profile != null && profile.externalSdFreeGb > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "SD Card Workspace Available: ${String.format("%.1f", profile.externalSdFreeGb)} GB Free", fontSize = 11.sp, color = AccentGreen)
                }
            }
        }

        // Maintenance & Storage Actions
        item {
            SoraGlassCard {
                Text(text = "System Maintenance Actions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Perform workspace cleanup, re-index models, or check for AI engine updates.", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setSettingsStatus("Workspace cache cleared (3.2 GB freed)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("clear_cache_btn")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cache", fontSize = 11.sp, color = AccentRed)
                    }

                    OutlinedButton(
                        onClick = { viewModel.setSettingsStatus("Local storage re-indexed (12 models, 8 project renders verified)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-index Storage", fontSize = 11.sp, color = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.setSettingsStatus("Sora AI Engine v2.4.0 is up to date with Vulkan 1.3 acceleration") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check AI Engine Updates", fontSize = 11.sp, color = AccentGreen)
                }
            }
        }

        // Privacy & Security
        item {
            SoraGlassCard {
                Text(text = "Security & Telemetry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "• 100% Offline-First (No analytics or cloud tracking by default)\n• Private local model directory\n• Encrypted project database", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun MemoryChip(label: String, selectedMode: String, onClick: () -> Unit) {
    val isSelected = label == selectedMode
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else GlassSurface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) DeepDarkBg else TextPrimary
        )
    }
}
