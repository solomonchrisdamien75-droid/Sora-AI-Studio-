package com.example.ui.screens

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
fun SoraCloudScreen(viewModel: SoraMainViewModel) {
    val servers by viewModel.cloudServers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Sora Cloud Network",
                subtitle = "Local Wi-Fi & Private Compute Server Architecture",
                icon = Icons.Default.Cloud
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Sora Cloud Box Discovery", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "Offload heavy video renders to nearby local AI hardware over zero-latency Wi-Fi.", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SoraGradientButton(
                    text = "SCAN LOCAL NETWORK (mDNS)",
                    icon = Icons.Default.WifiTethering,
                    modifier = Modifier.fillMaxWidth().testTag("scan_cloud_btn"),
                    onClick = { viewModel.scanSoraCloudServers() }
                )
            }
        }

        item {
            Text(text = "Detected Compute Servers (${servers.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (servers.isEmpty()) {
            item {
                SoraGlassCard {
                    Text(
                        text = "No local Sora Cloud servers detected on this Wi-Fi network yet. Tap 'SCAN LOCAL NETWORK'.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(servers) { server ->
                SoraGlassCard(
                    borderColor = if (server.isConnected) AccentGreen else GlassSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = server.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "IP: ${server.ipAddress}:${server.port} • Latency: ${server.latencyMs}ms", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "GPU: ${server.gpuModel}", fontSize = 11.sp, color = NeonPurple)
                        }

                        StatusIndicator(isConnected = server.isConnected)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "RAM: ${server.availableRamGb}GB / ${server.totalRamGb}GB Free", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "Active Workers: ${server.activeUsers}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
