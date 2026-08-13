package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AssistantScreen(viewModel: SoraMainViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val activeTimers by viewModel.activeTimers.collectAsState()
    val scriptPkg by viewModel.generatedScript.collectAsState()
    val isLoading by viewModel.isAssistantLoading.collectAsState()

    var chatInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        SoraSectionHeader(
            title = "🤖 AI Chat & Action Assistant",
            subtitle = "Interact with AI to execute device commands (Open YouTube, set timers, manhwa recaps)",
            icon = Icons.Default.Chat
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Active Timers Bar (if any timer is running)
        if (activeTimers.isNotEmpty()) {
            SoraGlassCard(borderColor = AccentGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Active Timers (${activeTimers.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeTimers.forEach { timer ->
                        val minutes = timer.remainingSeconds / 60
                        val secs = timer.remainingSeconds % 60
                        val timeFormatted = String.format("%02d:%02d", minutes, secs)
                        val progress = if (timer.totalSeconds > 0) timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat() else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurfaceVariant)
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = timer.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = if (timer.isFinished) "🔔 TIMER ALARM FINISHED!" else "Remaining: $timeFormatted", fontSize = 11.sp, color = if (timer.isFinished) AccentRed else NeonCyan)
                                    }
                                    IconButton(
                                        onClick = { viewModel.cancelTimer(timer.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Timer", tint = AccentRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = if (timer.isFinished) AccentRed else AccentGreen,
                                    trackColor = GlassSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Quick Action Command Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                TypeChip("▶️ Open YouTube", "YT", "ACTION") {
                    viewModel.sendChatMessage("Open YouTube")
                }
            }
            item {
                TypeChip("⏱️ Set 5 Min Timer", "TIMER", "ACTION") {
                    viewModel.sendChatMessage("Set timer for 5 minutes")
                }
            }
            item {
                TypeChip("📖 Manhwa Recap", "MANHWA", "ACTION") {
                    viewModel.sendChatMessage("Create manhwa recap")
                }
            }
            item {
                TypeChip("🎬 Write Sci-Fi Script", "SCRIPT", "ACTION") {
                    viewModel.sendChatMessage("Write sci-fi movie script about solar pilots")
                }
            }
            item {
                TypeChip("⚙️ Check Status", "STATUS", "ACTION") {
                    viewModel.sendChatMessage("Check system status and wake word")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message History Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = false
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.sender == "USER"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUser) ElectricPink else GlassSurface)
                            .border(1.dp, if (isUser) ElectricPink else NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isUser) "You" else "Sora AI Assistant",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) Color.White.copy(alpha = 0.8f) else NeonCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            color = if (isUser) Color.White else TextPrimary
                        )

                        if (!msg.actionType.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SoraBadge(
                                    text = msg.actionTitle ?: msg.actionType,
                                    color = when (msg.actionType) {
                                        "OPEN_YOUTUBE" -> AccentRed
                                        "SET_TIMER" -> AccentGreen
                                        "MANHWA_RECAP" -> ElectricPink
                                        else -> NeonPurple
                                    }
                                )
                            }
                        }
                    }

                    if (isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ElectricPink)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // If a script package was generated, display script breakdown in chat
            val pkg = scriptPkg
            if (pkg != null) {
                item {
                    SoraGlassCard(borderColor = NeonPurple) {
                        Text(text = "📜 Script Production Package: ${pkg.title}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = pkg.scriptText, fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        pkg.shots.forEach { shot ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GlassSurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Shot ${shot.shotNumber}: ${shot.title}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                        Text(text = shot.promptText, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updatePrompt(shot.promptText)
                                            viewModel.selectTab(SoraTab.GENERATE)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("send_shot_${shot.shotNumber}")
                                    ) {
                                        Text("Send Shot", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GlassSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.triggerWakeWordEvent("Skra! Open YouTube") }) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Command", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }

                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text("Ask AI to open YouTube, set timer, write script...", fontSize = 12.sp, color = TextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (chatInputText.isNotBlank()) {
                            viewModel.sendChatMessage(chatInputText)
                            chatInputText = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_btn")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message", tint = ElectricPink, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

