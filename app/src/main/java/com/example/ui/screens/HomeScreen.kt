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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.hardware.PerformanceTier
import com.example.data.AiModelEntity
import com.example.data.GalleryItemEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun HomeScreen(viewModel: SoraMainViewModel) {
    val hardware by viewModel.hardwareProfile.collectAsState()
    val models by viewModel.downloadedModels.collectAsState()
    val gallery by viewModel.galleryItems.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SORA AI STUDIO",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Offline AI Video Generator Workstation",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                StatusIndicator(isConnected = true)
            }
        }

        // Hardware Stats Widget
        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Hardware",
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hardware Engine Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    SoraBadge(
                        text = when (hardware?.performanceTier) {
                            PerformanceTier.HIGH_END_12GB_PLUS -> "12GB+ Ultra Tier"
                            PerformanceTier.MID_RANGE_6GB -> "6GB Mid Tier"
                            else -> "3-4GB Low RAM Tier"
                        },
                        color = NeonPurple,
                        textColor = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val profile = hardware
                if (profile != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HardwareMetricItem("RAM", "${String.format("%.1f", profile.availableRamGb)}GB / ${String.format("%.1f", profile.totalRamGb)}GB Free", Icons.Default.Memory)
                        HardwareMetricItem("CPU Cores", "${profile.cpuCores} Cores (${profile.cpuAbi.take(7)})", Icons.Default.Speed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HardwareMetricItem("GPU API", if (profile.gpuVulkanSupported) "Vulkan 1.3 Ready" else "OpenGL ES", Icons.Default.VideogameAsset)
                        HardwareMetricItem("Thermal", profile.thermalStatus, Icons.Default.Thermostat)
                    }
                }
            }
        }

        // Quick Launch Workbench
        item {
            SoraSectionHeader(
                title = "Quick Generation Workbench",
                subtitle = "Choose an offline AI creation mode",
                icon = Icons.Default.AutoAwesome
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Text to Video",
                    icon = Icons.Default.VideoCall,
                    color = NeonCyan,
                    modifier = Modifier.weight(1f).testTag("quick_text_to_video"),
                    onClick = {
                        viewModel.updateGenerationType("TEXT_TO_VIDEO")
                        viewModel.selectTab(SoraTab.GENERATE)
                    }
                )
                QuickActionCard(
                    title = "Manhwa Recap",
                    icon = Icons.Default.MovieFilter,
                    color = ElectricPink,
                    modifier = Modifier.weight(1f).testTag("quick_manhwa_recap"),
                    onClick = {
                        viewModel.updateGenerationType("MANHWA_RECAP")
                        viewModel.selectTab(SoraTab.GENERATE)
                    }
                )
                QuickActionCard(
                    title = "AI Assistant",
                    icon = Icons.Default.Psychology,
                    color = NeonPurple,
                    modifier = Modifier.weight(1f).testTag("quick_assistant"),
                    onClick = {
                        viewModel.selectTab(SoraTab.ASSISTANT)
                    }
                )
            }
        }

        // Dedicated Featured Banner for Manhwa Recap Studio
        item {
            SoraGlassCard(borderColor = ElectricPink) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(text = "NEW FEATURE", color = ElectricPink)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "🔥 Manhwa Recap Studio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload Manhwa panels & voiceovers. AI auto-animates panels, performs character lip-syncing, filters out redundant action narration, and generates story continuations!",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            viewModel.updateGenerationType("MANHWA_RECAP")
                            viewModel.selectTab(SoraTab.GENERATE)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("launch_manhwa_studio_btn")
                    ) {
                        Text("Launch Studio", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Installed Local Models
        item {
            SoraSectionHeader(
                title = "Installed On-Device Models",
                subtitle = "${models.size} models downloaded locally",
                icon = Icons.Default.FolderZip,
                actionText = "Manage Models",
                onActionClick = { viewModel.selectTab(SoraTab.MODELS) }
            )

            if (models.isEmpty()) {
                SoraGlassCard {
                    Text(
                        text = "No models downloaded yet. Open Download Manager to fetch GGUF or LiteRT models.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(models) { model ->
                        LocalModelCard(model = model, onClick = { viewModel.selectTab(SoraTab.MODELS) })
                    }
                }
            }
        }

        // Recent Generated Media Gallery
        item {
            SoraSectionHeader(
                title = "Recent AI Renders",
                subtitle = "Saved locally on device",
                icon = Icons.Default.PermMedia,
                actionText = "Full Gallery",
                onActionClick = { viewModel.selectTab(SoraTab.GALLERY) }
            )

            if (gallery.isEmpty()) {
                SoraGlassCard {
                    Text(
                        text = "Your generated videos and images will appear here.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(gallery) { item ->
                        GalleryPreviewCard(item = item, onClick = { viewModel.selectTab(SoraTab.GALLERY) })
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareMetricItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun LocalModelCard(model: AiModelEntity, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SoraBadge(text = model.format, color = NeonCyan)
                Text(text = "${model.ramRequiredMb}MB RAM", fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = model.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = model.description, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
        }
    }
}

@Composable
fun GalleryPreviewCard(item: GalleryItemEntity, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurfaceVariant)
            .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SoraBadge(text = item.resolutionLabel, color = ElectricPink)
                Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            }
            Text(text = item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 2)
        }
    }
}
