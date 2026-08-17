package com.example.ui.components.generation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GenerationFormState
import com.example.ui.SoraMainViewModel
import com.example.ui.components.SoraBadge
import com.example.ui.theme.*

/**
 * Interactive duration selector that spans from 1 second up to 24+ hours.
 * Offers Seconds mode (1s–59s), Minutes mode (1m–59m), Hours mode (1h–24h),
 * and an Exact Timecode (HH:MM:SS) editor with long-form continuous rendering telemetry.
 */
@Composable
fun VideoDurationSelector(
    viewModel: SoraMainViewModel,
    form: GenerationFormState,
    modifier: Modifier = Modifier
) {
    val durationSec = form.durationSec.coerceAtLeast(1)

    // Determine default active tab based on current duration value
    var durationUnitTab by remember(durationSec) {
        mutableStateOf(
            when {
                durationSec >= 3600 -> "HOURS"
                durationSec >= 60 -> "MINUTES"
                else -> "SECONDS"
            }
        )
    }

    var showExactTimeDialog by remember { mutableStateOf(false) }

    // Decompose duration into hours, minutes, and seconds
    val currentHours = durationSec / 3600
    val currentMinutes = (durationSec % 3600) / 60
    val currentSeconds = durationSec % 60

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Header with Current Duration Spotlight HUD ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Video Duration Target",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = DurationFormatters.formatLongLabel(durationSec),
                    fontSize = 11.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Digital Timecode Tag
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GlassSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = DurationFormatters.formatHms(durationSec),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        // --- Duration Unit Tab Selector ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GlassSurface)
                .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DurationTabButton(
                title = "⚡ Seconds (1s–59s)",
                isSelected = durationUnitTab == "SECONDS",
                modifier = Modifier.weight(1f),
                onClick = {
                    durationUnitTab = "SECONDS"
                    if (durationSec >= 60) viewModel.updateDuration(15)
                }
            )
            DurationTabButton(
                title = "🎬 Minutes (1m–59m)",
                isSelected = durationUnitTab == "MINUTES",
                modifier = Modifier.weight(1f),
                onClick = {
                    durationUnitTab = "MINUTES"
                    if (durationSec < 60 || durationSec >= 3600) viewModel.updateDuration(120) // 2 min default
                }
            )
            DurationTabButton(
                title = "🎥 Hours (1h–24h)",
                isSelected = durationUnitTab == "HOURS",
                modifier = Modifier.weight(1f),
                onClick = {
                    durationUnitTab = "HOURS"
                    if (durationSec < 3600) viewModel.updateDuration(3600) // 1 hr default
                }
            )
        }

        // --- Interactive Controls for Active Tab ---
        when (durationUnitTab) {
            "SECONDS" -> {
                SecondsDurationControl(
                    currentSeconds = if (durationSec < 60) durationSec else 15,
                    onSecondsChanged = { viewModel.updateDuration(it) }
                )
            }
            "MINUTES" -> {
                val effectiveMin = if (durationSec in 60..3599) durationSec / 60 else if (durationSec >= 3600) (durationSec / 60).coerceIn(1, 59) else 2
                MinutesDurationControl(
                    currentMinutes = effectiveMin,
                    currentRemainderSec = durationSec % 60,
                    onMinutesChanged = { m, s -> viewModel.updateDuration(m * 60 + s) }
                )
            }
            "HOURS" -> {
                val effectiveHours = if (durationSec >= 3600) durationSec / 3600 else 1
                val effectiveMinRem = (durationSec % 3600) / 60
                HoursDurationControl(
                    currentHours = effectiveHours,
                    currentMinutes = effectiveMinRem,
                    onHoursChanged = { h, m -> viewModel.updateDuration(h * 3600 + m * 60) }
                )
            }
        }

        // --- Direct Timecode Input Button & Long-Form Pipeline Telemetry ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showExactTimeDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("custom_timecode_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Custom Timecode (HH:MM:SS)", fontSize = 11.sp, color = NeonCyan)
            }

            // Frame count calculation badge
            val totalFrames = durationSec.toLong() * form.fps
            Text(
                text = "${totalFrames} frames @ ${form.fps}fps",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }

        // --- Long-Form Multi-Segment Pipeline Architecture Card ---
        val segmentCount = DurationFormatters.getSegmentCount(durationSec, 10)
        val estimatedSize = DurationFormatters.formatEstimatedSize(durationSec, form.resolution, form.videoCodec)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (durationSec >= 3600) NeonPurple.copy(alpha = 0.12f) else GlassSurface)
                .border(
                    1.dp,
                    if (durationSec >= 3600) NeonPurple.copy(alpha = 0.5f) else GlassSurfaceVariant,
                    RoundedCornerShape(10.dp)
                )
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (durationSec >= 3600) Icons.Default.AutoAwesome else Icons.Default.Memory,
                            contentDescription = null,
                            tint = if (durationSec >= 3600) NeonPurple else NeonCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                durationSec >= 3600 -> "⚡ Continuous Long-Form Architecture (${durationSec / 3600}h)"
                                durationSec >= 60 -> "⚡ Progressive Segmented Stitching Engine"
                                else -> "⚡ Turbo Single-Pass Neural Diffusion"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (durationSec >= 3600) NeonPurple else NeonCyan
                        )
                    }

                    SoraBadge(
                        text = "Est. $estimatedSize",
                        color = if (durationSec >= 3600) ElectricPink else AccentGreen
                    )
                }

                Text(
                    text = when {
                        durationSec >= 3600 -> "Rendering in $segmentCount continuous 10s chained passes with optical flow latent preservation. Checkpoint auto-saved every 5 minutes. Low-VRAM buffer active (<3GB RAM)."
                        durationSec >= 60 -> "Rendering in $segmentCount continuous 10s passes. Keyframe continuity latents seamlessly stitched without memory leaks."
                        else -> "Direct keyframe diffusion in a single continuous temporal batch."
                    },
                    fontSize = 10.sp,
                    color = TextSecondary,
                    lineHeight = 13.sp
                )
            }
        }
    }

    // --- Custom Exact Timecode Input Dialog ---
    if (showExactTimeDialog) {
        CustomTimecodeDialog(
            initialHours = currentHours,
            initialMinutes = currentMinutes,
            initialSeconds = currentSeconds,
            onDismiss = { showExactTimeDialog = false },
            onApply = { h, m, s ->
                val total = (h * 3600) + (m * 60) + s
                viewModel.updateDuration(total.coerceAtLeast(1))
                showExactTimeDialog = false
            }
        )
    }
}

@Composable
private fun DurationTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DeepDarkBg else TextSecondary,
            maxLines = 1
        )
    }
}

/**
 * Seconds Mode (1s – 59s)
 */
@Composable
private fun SecondsDurationControl(
    currentSeconds: Int,
    onSecondsChanged: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Second Precision: ${currentSeconds}s", fontSize = 11.5.sp, color = TextSecondary)
            Text(
                text = "${currentSeconds} seconds",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        Slider(
            value = currentSeconds.toFloat().coerceIn(1f, 59f),
            onValueChange = { onSecondsChanged(it.toInt().coerceIn(1, 59)) },
            valueRange = 1f..59f,
            steps = 57,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = GlassSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("seconds_slider")
        )

        // Seconds Presets Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val presets = listOf(
                1 to "1s (Micro)",
                2 to "2s",
                3 to "3s",
                5 to "5s (Shot)",
                10 to "10s",
                15 to "15s (Shorts)",
                30 to "30s (Promo)",
                45 to "45s",
                59 to "59s"
            )
            items(presets) { (sec, label) ->
                val isSelected = currentSeconds == sec
                FilterChip(
                    selected = isSelected,
                    onClick = { onSecondsChanged(sec) },
                    label = { Text(label, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = DeepDarkBg
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("sec_chip_$sec")
                )
            }
        }
    }
}

/**
 * Minutes Mode (1m – 59m)
 */
@Composable
private fun MinutesDurationControl(
    currentMinutes: Int,
    currentRemainderSec: Int,
    onMinutesChanged: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Minute Slider: ${currentMinutes} min", fontSize = 11.5.sp, color = TextSecondary)
            Text(
                text = "$currentMinutes min (${currentMinutes * 60}s)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPurple
            )
        }

        Slider(
            value = currentMinutes.toFloat().coerceIn(1f, 59f),
            onValueChange = { onMinutesChanged(it.toInt().coerceIn(1, 59), 0) },
            valueRange = 1f..59f,
            steps = 57,
            colors = SliderDefaults.colors(
                thumbColor = NeonPurple,
                activeTrackColor = NeonPurple,
                inactiveTrackColor = GlassSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("minutes_slider")
        )

        // Minutes Presets Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val minPresets = listOf(
                1 to "1 min (Scene)",
                2 to "2 min",
                3 to "3 min",
                5 to "5 min (Short Film)",
                10 to "10 min (YouTube)",
                15 to "15 min",
                20 to "20 min",
                30 to "30 min (Episode)",
                45 to "45 min",
                59 to "59 min"
            )
            items(minPresets) { (m, label) ->
                val isSelected = currentMinutes == m && currentRemainderSec == 0
                FilterChip(
                    selected = isSelected,
                    onClick = { onMinutesChanged(m, 0) },
                    label = { Text(label, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("min_chip_$m")
                )
            }
        }
    }
}

/**
 * Hours Mode (1h – 24h)
 */
@Composable
private fun HoursDurationControl(
    currentHours: Int,
    currentMinutes: Int,
    onHoursChanged: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Hours Slider: ${currentHours} Hour(s)", fontSize = 11.5.sp, color = TextSecondary)
            Text(
                text = "$currentHours hr(s) (${currentHours * 3600}s)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricPink
            )
        }

        Slider(
            value = currentHours.toFloat().coerceIn(1f, 24f),
            onValueChange = { onHoursChanged(it.toInt().coerceIn(1, 24), 0) },
            valueRange = 1f..24f,
            steps = 22,
            colors = SliderDefaults.colors(
                thumbColor = ElectricPink,
                activeTrackColor = ElectricPink,
                inactiveTrackColor = GlassSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hours_slider")
        )

        // Hours Presets Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val hourPresets = listOf(
                1 to "1 Hour (Feature)",
                2 to "2 Hours (Movie)",
                3 to "3 Hours (Epic)",
                4 to "4 Hours (Ambient)",
                6 to "6 Hours (Atmosphere)",
                8 to "8 Hours (Loop Stream)",
                12 to "12 Hours (Half-Day)",
                24 to "24 Hours (Broadcast)"
            )
            items(hourPresets) { (h, label) ->
                val isSelected = currentHours == h && currentMinutes == 0
                FilterChip(
                    selected = isSelected,
                    onClick = { onHoursChanged(h, 0) },
                    label = { Text(label, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricPink,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("hour_chip_$h")
                )
            }
        }
    }
}

/**
 * Dedicated Custom Timecode Dialog (HH:MM:SS)
 */
@Composable
private fun CustomTimecodeDialog(
    initialHours: Int,
    initialMinutes: Int,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onApply: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    var hours by remember { mutableStateOf(initialHours.coerceIn(0, 24)) }
    var minutes by remember { mutableStateOf(initialMinutes.coerceIn(0, 59)) }
    var seconds by remember { mutableStateOf(initialSeconds.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Custom Video Duration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Configure exact hours, minutes, and seconds for video synthesis.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // 3 Numeric Spinners
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Box
                    TimeComponentPicker(
                        label = "Hours",
                        value = hours,
                        range = 0..24,
                        onValueChange = { hours = it }
                    )

                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                    // Minutes Box
                    TimeComponentPicker(
                        label = "Minutes",
                        value = minutes,
                        range = 0..59,
                        onValueChange = { minutes = it }
                    )

                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeonCyan)

                    // Seconds Box
                    TimeComponentPicker(
                        label = "Seconds",
                        value = seconds,
                        range = 0..59,
                        onValueChange = { seconds = it }
                    )
                }

                // Summary Calculation
                val totalSec = (hours * 3600) + (minutes * 60) + seconds
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GlassSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total: $totalSec seconds (${DurationFormatters.formatLongLabel(totalSec.coerceAtLeast(1))})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }

                // Quick Add Chips
                Text("Quick Adjustment:", fontSize = 11.sp, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        OutlinedButton(
                            onClick = { seconds = (seconds + 10).coerceAtMost(59) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+10s", fontSize = 10.5.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { minutes = (minutes + 1).coerceAtMost(59) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+1m", fontSize = 10.5.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { minutes = (minutes + 10).coerceAtMost(59) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+10m", fontSize = 10.5.sp)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { hours = (hours + 1).coerceAtMost(24) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("+1h", fontSize = 10.5.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(hours, minutes, seconds) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Apply Duration", color = DeepDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TimeComponentPicker(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { onValueChange((value + 1).coerceIn(range)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increment $label", tint = NeonCyan)
        }

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GlassSurface)
                .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(java.util.Locale.US, "%02d", value),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        IconButton(
            onClick = { onValueChange((value - 1).coerceIn(range)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrement $label", tint = NeonCyan)
        }

        Text(text = label, fontSize = 10.5.sp, color = TextSecondary)
    }
}
