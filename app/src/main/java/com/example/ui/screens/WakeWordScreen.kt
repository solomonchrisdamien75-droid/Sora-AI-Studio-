package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.wakeword.VoiceActionType
import com.example.ai.wakeword.VoiceEventItem
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.SoraGlassCard
import com.example.ui.components.SoraSectionHeader
import com.example.ui.theme.*

@Composable
fun WakeWordScreen(viewModel: SoraMainViewModel) {
    val context = LocalContext.current

    val isRunning by viewModel.isWakeWordServiceRunning.collectAsState()
    val isListening by viewModel.isWakeWordListening.collectAsState()
    val consentGranted by viewModel.wakeWordConsentGranted.collectAsState()
    val currentWakeWord by viewModel.currentWakeWord.collectAsState()
    val sensitivity by viewModel.wakeWordSensitivity.collectAsState()
    val amplitude by viewModel.audioAmplitude.collectAsState()
    val lastCommand by viewModel.lastDetectedVoiceCommand.collectAsState()
    val lastResponse by viewModel.lastAiVoiceResponse.collectAsState()
    val voiceLogs by viewModel.voiceLogHistory.collectAsState()
    val ttsEnabled by viewModel.isTtsVoiceEnabled.collectAsState()
    val continuous by viewModel.continuousListening.collectAsState()

    var showConsentDialog by remember { mutableStateOf(false) }
    var testManualInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<VoiceActionType?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.toggleWakeWordService(true)
                Toast.makeText(context, "Microphone access granted! Background wake-word is active.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Pulsing animation for audio visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Sora Voice & Wake-Word Engine",
                subtitle = "Hands-free voice intelligence with on-device background detection surpassing Alexa",
                icon = Icons.Default.GraphicEq
            )
        }

        // Background Privacy & Consent Status Banner
        item {
            SoraGlassCard(borderColor = if (consentGranted) AccentGreen else ElectricPink) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (consentGranted) AccentGreen else ElectricPink)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (consentGranted) "Privacy Consent Granted (100% Local On-Device)" else "Background Microphone Consent Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (consentGranted)
                                    "Zero cloud uploads. Real-time acoustic neural net running on Snapdragon NPU."
                                else
                                    "Review and agree to local voice terms before activating background wake-word.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (consentGranted) {
                                viewModel.revokeWakeWordConsent()
                                Toast.makeText(context, "Voice consent revoked. Wake-word service disabled.", Toast.LENGTH_SHORT).show()
                            } else {
                                showConsentDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (consentGranted) CardBorder else ElectricPink
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("wake_word_consent_btn")
                    ) {
                        Text(
                            text = if (consentGranted) "Revoke" else "Review Terms",
                            color = if (consentGranted) TextPrimary else DeepDarkBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Main Live Voice Orb & Status Controller
        item {
            SoraGlassCard(borderColor = if (isRunning) NeonCyan else CardBorder) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRunning) "ACTIVE LISTENING" else "VOICE ENGINE STANDBY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) NeonCyan else TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Glowing Pulsing Orb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .scale(if (isRunning) (1f + (amplitude * 0.4f).coerceIn(0f, 0.5f)) else 1f)
                    ) {
                        // Outer Glow Ring
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            if (isRunning) NeonCyan.copy(alpha = 0.5f) else NeonPurple.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Middle Pulsing Ring
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(if (isRunning) pulseScale else 1f)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (isRunning) NeonCyan else NeonPurple.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .background(GlassSurface)
                        )

                        // Center Icon
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Status",
                            tint = if (isRunning) NeonCyan else TextSecondary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Wake Phrase: \"$currentWakeWord\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRunning)
                            "Listening continuously in background... Speak \"$currentWakeWord\" + command"
                        else
                            "Service paused. Toggle switch below to start background listening.",
                        fontSize = 12.sp,
                        color = if (isRunning) AccentGreen else TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (!consentGranted) {
                                    showConsentDialog = true
                                } else {
                                    if (!isRunning) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.toggleWakeWordService(true)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        viewModel.toggleWakeWordService(false)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) AccentRed else NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_wake_word_service_btn")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = DeepDarkBg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "Stop Background Service" else "Start Background Service",
                                color = DeepDarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Audio Amplitude & Speech Feedback Banner
        if (isRunning || lastCommand != null) {
            item {
                SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Acoustic Telemetry & Last Response", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text(
                                text = "Mic Level: ${(amplitude * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Amplitude Bar
                        LinearProgressIndicator(
                            progress = { amplitude.coerceIn(0.05f, 1.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = CardBorder
                        )

                        lastCommand?.let { cmd ->
                            Text(
                                text = "🗣️ Detected: \"$cmd\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentYellow
                            )
                        }

                        lastResponse?.let { resp ->
                            Text(
                                text = "🤖 Sora: $resp",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Wake-Word Configuration & Customization
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Wake-Word Settings & Acoustic Thresholds", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Wake-Word selector chips
                    Column {
                        Text("Select Trigger Phrase:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Hey Sora", "Sora", "Computer", "Jarvis", "Private Agent")) { phrase ->
                                val isSelected = currentWakeWord == phrase
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonPurple else GlassSurface)
                                        .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setWakeWordPhrase(phrase) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = phrase,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Acoustic Sensitivity", fontSize = 12.sp, color = TextSecondary)
                            Text("${(sensitivity * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        }
                        Slider(
                            value = sensitivity,
                            onValueChange = { viewModel.setWakeWordSensitivity(it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = CardBorder
                            )
                        )
                    }

                    // Text-To-Speech Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spoken Voice Answers (TTS)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Speak responses out loud using on-device neural voice synthesizer", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { viewModel.toggleTtsVoice(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                        )
                    }

                    // Continuous Background Listening
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Background Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Keep listening even when screen is locked or another app is open", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = continuous,
                            onCheckedChange = { viewModel.toggleContinuousListening(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // Alexa-Surpassing Skills & Quick Voice Testing Simulator
        item {
            SoraGlassCard(borderColor = ElectricPink.copy(alpha = 0.4f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hands-Free Skills (Surpassing Alexa)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Text(
                        text = "Tap any command below to test how Sora responds, executes, or queues tasks:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    // Quick Command Chips
                    val sampleCommands = listOf(
                        "🎬 Generate a futuristic cyberpunk flying car video in 4K" to VoiceActionType.GENERATE_VIDEO,
                        "📱 Scroll down and open gallery" to VoiceActionType.SCREEN_CONTROL,
                        "📞 Call Technical Support" to VoiceActionType.PHONE_COMMUNICATION,
                        "💬 Text Sarah: The video render is complete!" to VoiceActionType.SMS_MESSAGING,
                        "✂️ Trim current clip to 5 seconds and add synthwave" to VoiceActionType.VIDEO_EDITING,
                        "⚡ What is my Snapdragon NPU and GPU temperature?" to VoiceActionType.SYSTEM_DIAGNOSTICS,
                        "⏱️ Set a video render timer for 15 minutes" to VoiceActionType.PRODUCTIVITY_ROUTINE,
                        "🧠 Explain how Sora diffusion transformer generates frames" to VoiceActionType.CONVERSATIONAL_AI
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sampleCommands.forEach { (cmd, type) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.executeVoiceCommand(cmd)
                                        Toast.makeText(context, "Executed: $cmd", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = GlassSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (type) {
                                                VoiceActionType.GENERATE_VIDEO -> Icons.Default.VideoCall
                                                VoiceActionType.SCREEN_CONTROL -> Icons.Default.TouchApp
                                                VoiceActionType.PHONE_COMMUNICATION -> Icons.Default.Phone
                                                VoiceActionType.SMS_MESSAGING -> Icons.Default.Message
                                                VoiceActionType.VIDEO_EDITING -> Icons.Default.ContentCut
                                                VoiceActionType.SYSTEM_DIAGNOSTICS -> Icons.Default.Memory
                                                VoiceActionType.PRODUCTIVITY_ROUTINE -> Icons.Default.Alarm
                                                VoiceActionType.CONVERSATIONAL_AI -> Icons.Default.Psychology
                                            },
                                            contentDescription = null,
                                            tint = when (type) {
                                                VoiceActionType.GENERATE_VIDEO -> NeonCyan
                                                VoiceActionType.SCREEN_CONTROL -> AccentGreen
                                                VoiceActionType.PHONE_COMMUNICATION -> AccentYellow
                                                VoiceActionType.SMS_MESSAGING -> ElectricPink
                                                else -> NeonPurple
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cmd, fontSize = 12.sp, color = TextPrimary)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Test",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Manual Text Input for Voice Command Simulator
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testManualInput,
                            onValueChange = { testManualInput = it },
                            placeholder = { Text("Or type a custom voice command...", fontSize = 12.sp, color = TextSecondary) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_voice_input"),
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
                            onClick = {
                                if (testManualInput.isNotBlank()) {
                                    viewModel.executeVoiceCommand(testManualInput)
                                    testManualInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("send_voice_cmd_btn")
                        ) {
                            Text("Send", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Voice Command History & Execution Trace Log
        item {
            SoraGlassCard(borderColor = CardBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voice Interaction Log (${voiceLogs.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        if (voiceLogs.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearVoiceLogHistory() }) {
                                Text("Clear", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    if (voiceLogs.isEmpty()) {
                        Text(
                            text = "No voice interactions recorded yet. Speak \"$currentWakeWord\" or use the simulator above.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            voiceLogs.forEach { logItem ->
                                VoiceLogCard(
                                    log = logItem,
                                    onSpeakAgain = {
                                        viewModel.wakeWordEngine.speak(logItem.responseText)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Consent Agreement Dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            containerColor = DeepDarkBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sora Voice & Privacy Consent", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "To enable hands-free voice control and background wake-word detection (\"Hey Sora\"), the app requires your explicit permission.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    SoraGlassCard(borderColor = AccentGreen.copy(alpha = 0.5f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("100% On-Device Local Acoustic AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("Audio frames are analyzed locally in RAM. No voice recordings or acoustic streams are ever sent to remote servers.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Background Service & Battery Impact", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("A lightweight low-power foreground notification keeps the wake-word listener responsive even when your screen is locked.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Text(
                        text = "You can revoke this consent or mute the microphone at any time from this screen or through the notification bar.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.grantWakeWordConsent()
                        showConsentDialog = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.toggleWakeWordService(true)
                            Toast.makeText(context, "Voice consent granted! Background wake-word is active.", Toast.LENGTH_SHORT).show()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I Agree & Enable", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun VoiceLogCard(
    log: VoiceEventItem,
    onSpeakAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (log.actionType) {
                                    VoiceActionType.GENERATE_VIDEO -> NeonCyan.copy(alpha = 0.2f)
                                    VoiceActionType.SCREEN_CONTROL -> AccentGreen.copy(alpha = 0.2f)
                                    VoiceActionType.PHONE_COMMUNICATION -> AccentYellow.copy(alpha = 0.2f)
                                    VoiceActionType.SMS_MESSAGING -> ElectricPink.copy(alpha = 0.2f)
                                    else -> NeonPurple.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.actionType.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (log.actionType) {
                                VoiceActionType.GENERATE_VIDEO -> NeonCyan
                                VoiceActionType.SCREEN_CONTROL -> AccentGreen
                                VoiceActionType.PHONE_COMMUNICATION -> AccentYellow
                                VoiceActionType.SMS_MESSAGING -> ElectricPink
                                else -> NeonPurple
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trigger: \"${log.triggerPhrase}\"",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(log.confidence * 100).toInt()}% conf",
                        fontSize = 10.sp,
                        color = AccentGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSpeakAgain,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "“${log.commandText}”",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = log.responseText,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
