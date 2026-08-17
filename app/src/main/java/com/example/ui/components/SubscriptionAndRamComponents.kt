package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.ai.hardware.DeviceHardwareProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.userSubscriptionDataStore by preferencesDataStore(name = "subscription_prefs")
val WATCHED_VIDEO_KEY = booleanPreferencesKey("watched_video")

@Composable
fun RamUsageMonitor(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var availRamPercent by remember { mutableStateOf(0f) }
    var availRamMb by remember { mutableStateOf(0L) }
    var totalRamMb by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        while (true) {
            activityManager?.getMemoryInfo(memoryInfo)
            val totalBytes = memoryInfo.totalMem
            val availBytes = memoryInfo.availMem
            if (totalBytes > 0) {
                totalRamMb = totalBytes / (1024 * 1024)
                availRamMb = availBytes / (1024 * 1024)
                availRamPercent = (availBytes.toFloat() / totalBytes.toFloat()) * 100f
            }
            delay(2000)
        }
    }

    SoraGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = NeonCyan.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "RAM Usage",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Real-time RAM Monitor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Free: ${availRamMb} MB / ${totalRamMb} MB (${String.format("%.1f", availRamPercent)}% free)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            SoraBadge(
                text = "${String.format("%.0f", availRamPercent)}% FREE",
                color = if (availRamPercent > 20f) AccentGreen else ElectricPink,
                textColor = DeepDarkBg
            )
        }
    }
}

@Composable
fun RamUsageMonitorCard(
    hardwareProfile: DeviceHardwareProfile?,
    modifier: Modifier = Modifier
) {
    val totalRamGb = hardwareProfile?.totalRamGb ?: 4.0f
    val availRamGb = hardwareProfile?.availableRamGb ?: 2.1f
    val usedRamGb = (totalRamGb - availRamGb).coerceAtLeast(0f)
    val ramUsagePercent = ((usedRamGb / totalRamGb) * 100f).coerceIn(0f, 100f)

    val isOptimalForOnnx = availRamGb >= 1.5f
    val statusColor = when {
        availRamGb >= 2.5f -> AccentGreen
        availRamGb >= 1.2f -> AccentYellow
        else -> ElectricPink
    }

    SoraGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = statusColor.copy(alpha = 0.5f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "RAM Usage",
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ONNX Model RAM Monitor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                SoraBadge(
                    text = if (isOptimalForOnnx) "ONNX READY" else "LOW RAM",
                    color = statusColor,
                    textColor = DeepDarkBg
                )
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used: ${String.format("%.1f", usedRamGb)} GB / ${String.format("%.1f", totalRamGb)} GB",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Free: ${String.format("%.1f", availRamGb)} GB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                LinearProgressIndicator(
                    progress = { ramUsagePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = GlassSurfaceVariant
                )
            }

            Text(
                text = if (isOptimalForOnnx)
                    "✔ Sufficient memory available for local ONNX model inference execution on CPU/GPU."
                else
                    "⚠ Memory is constrained. Local ONNX inference may run in quantized or low-RAM mode.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SubscriptionGate(
    modifier: Modifier = Modifier,
    content: @Composable (isWatched: Boolean, onWatchConfirmed: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val watchedVideoState = context.userSubscriptionDataStore.data
        .map { prefs -> prefs[WATCHED_VIDEO_KEY] ?: false }
        .collectAsState(initial = false)

    val isWatched = watchedVideoState.value

    val onWatchConfirmed: () -> Unit = {
        scope.launch {
            context.userSubscriptionDataStore.edit { prefs ->
                prefs[WATCHED_VIDEO_KEY] = true
            }
        }
    }

    content(isWatched, onWatchConfirmed)
}

@Composable
fun SubscriptionGateOverlay(
    isUnlocked: Boolean,
    onConfirmWatched: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Box(modifier = modifier) {
        // Underlying Screen Content
        content()

        // Subscription Gate Overlay if locked
        if (!isUnlocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                SoraGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricPink
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(ElectricPink.copy(alpha = 0.2f))
                                .border(1.dp, ElectricPink, RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Feature",
                                tint = ElectricPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "Generation Locked",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Text(
                            text = "To enable AI generation features, please subscribe to @OneFactEndlessWonder and watch the YouTube Short.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        // Channel and Video Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    uriHandler.openUri("https://www.youtube.com/@OneFactEndlessWonder")
                                },
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(ElectricPink, NeonPurple)))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subscriptions,
                                    contentDescription = null,
                                    tint = ElectricPink,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Subscribe", fontSize = 12.sp, color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    uriHandler.openUri("https://youtube.com/shorts/iseGrWemeZw?is=hRw6b8l2tjrZpvYh")
                                },
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Watch Short", fontSize = 12.sp, color = TextPrimary)
                            }
                        }

                        Button(
                            onClick = onConfirmWatched,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Watched & Unlock All Features", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
