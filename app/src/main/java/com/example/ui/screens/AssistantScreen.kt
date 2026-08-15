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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AttachmentType
import com.example.ui.ChatAttachment
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AssistantScreen(viewModel: SoraMainViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val activeTimers by viewModel.activeTimers.collectAsState()
    val stagedAttachments by viewModel.stagedChatAttachments.collectAsState()
    val scriptPkg by viewModel.generatedScript.collectAsState()
    val isStreaming by viewModel.isChatStreaming.collectAsState()
    val activeModel by viewModel.activeLoadedModel.collectAsState()
    val chatSource by viewModel.chatModelSource.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var chatInputText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    // Activity Result Launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "image_${System.currentTimeMillis()}.png"
            viewModel.addChatAttachment(
                ChatAttachment(
                    uri = it.toString(),
                    fileName = fileName,
                    mimeType = "image/*",
                    type = AttachmentType.IMAGE
                )
            )
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "document_${System.currentTimeMillis()}.pdf"
            viewModel.addChatAttachment(
                ChatAttachment(
                    uri = it.toString(),
                    fileName = fileName,
                    mimeType = "application/pdf",
                    type = AttachmentType.PDF
                )
            )
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "file_${System.currentTimeMillis()}"
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
            val isImg = fileName.endsWith(".png", ignoreCase = true) || fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true)
            viewModel.addChatAttachment(
                ChatAttachment(
                    uri = it.toString(),
                    fileName = fileName,
                    mimeType = if (isPdf) "application/pdf" else if (isImg) "image/*" else "application/octet-stream",
                    type = if (isPdf) AttachmentType.PDF else if (isImg) AttachmentType.IMAGE else AttachmentType.FILE
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header with New Conversation & Model Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🤖 AI Chat & Universal Assistant",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Live streaming dialogue, file comprehension & multi-model routing",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // New Chat Button
                IconButton(
                    onClick = { viewModel.clearChatMessages() },
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Chat", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }

                // Clear Chat History Button
                IconButton(
                    onClick = { viewModel.clearChatMessages() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Active Model / Backend Switcher Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = GlassSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showModelMenu = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (chatSource) {
                            "UNIVERSAL_SERVER" -> Icons.Default.Dns
                            "CLOUD_API" -> Icons.Default.Cloud
                            "COMPOSITE_ROUTER" -> Icons.Default.Hub
                            else -> Icons.Default.Memory
                        },
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (chatSource) {
                            "UNIVERSAL_SERVER" -> "Server: Local HTTP (Port 8080)"
                            "CLOUD_API" -> "Cloud: Sora Cloud / OpenAI API"
                            "COMPOSITE_ROUTER" -> "Model Fusion: Composite Router"
                            else -> "Local Model: ${activeModel?.name ?: "LiteRT / GGUF Universal"}"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Change", fontSize = 10.5.sp, color = NeonCyan)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("⚡ Local Model (${activeModel?.name ?: "Loaded in RAM"})") },
                        onClick = {
                            viewModel.setChatModelSource("LOCAL_ENGINE")
                            showModelMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🌐 Universal Server (/v1/chat/completions)") },
                        onClick = {
                            viewModel.setChatModelSource("UNIVERSAL_SERVER")
                            showModelMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("☁️ Cloud API (Sora Cloud / OpenAI)") },
                        onClick = {
                            viewModel.setChatModelSource("CLOUD_API")
                            showModelMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🧬 Model Fusion Composite Router") },
                        onClick = {
                            viewModel.setChatModelSource("COMPOSITE_ROUTER")
                            showModelMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Timers Bar (if any timer is running)
        if (activeTimers.isNotEmpty()) {
            SoraGlassCard(borderColor = AccentGreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Active Timers (${activeTimers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    activeTimers.forEach { timer ->
                        val minutes = timer.remainingSeconds / 60
                        val secs = timer.remainingSeconds % 60
                        val timeFormatted = String.format("%02d:%02d", minutes, secs)
                        val progress = if (timer.totalSeconds > 0) timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat() else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(GlassSurfaceVariant)
                                .padding(6.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = timer.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = if (timer.isFinished) "🔔 TIMER ALARM FINISHED!" else "Remaining: $timeFormatted", fontSize = 10.sp, color = if (timer.isFinished) AccentRed else NeonCyan)
                                    }
                                    IconButton(
                                        onClick = { viewModel.cancelTimer(timer.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel Timer", tint = AccentRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                    color = if (timer.isFinished) AccentRed else AccentGreen,
                                    trackColor = GlassSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Quick Suggestion Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("upload_image_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Image", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentRed.copy(alpha = 0.2f))
                        .border(1.dp, AccentRed, RoundedCornerShape(8.dp))
                        .clickable { pdfPickerLauncher.launch("application/pdf") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("upload_pdf_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                    }
                }
            }
            item {
                TypeChip("🎬 Write Sci-Fi Script", "SCRIPT", "ACTION") {
                    viewModel.sendChatMessage("Write a sci-fi movie script and 3 cinematic camera shots about pilot exploring deep space")
                }
            }
            item {
                TypeChip("⏱️ Set 5 Min Timer", "TIMER", "ACTION") {
                    viewModel.sendChatMessage("Set timer for 5 minutes")
                }
            }
            item {
                TypeChip("▶️ Open YouTube", "YT", "ACTION") {
                    viewModel.sendChatMessage("Open YouTube")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Message History Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = NeonCyan.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Start a real conversation with Sora AI", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Ask questions, write movie scripts, brainstorm scenes, or attach files", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            items(chatMessages) { msg ->
                val isUser = msg.sender == "USER"
                val isLastAi = !isUser && chatMessages.lastOrNull { !it.sender.equals("USER", true) }?.id == msg.id

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUser) ElectricPink else GlassSurface)
                            .border(1.dp, if (isUser) ElectricPink else NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUser) "You" else "Sora AI Assistant",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) Color.White.copy(alpha = 0.8f) else NeonCyan
                            )

                            // Quick Copy & Regenerate Actions on message
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(msg.text)) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = if (isUser) Color.White.copy(alpha = 0.7f) else TextSecondary, modifier = Modifier.size(12.dp))
                                }
                                if (isLastAi && !isStreaming) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.regenerateLastChat() },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = NeonCyan, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))

                        // Render attachments if attached to message
                        if (msg.attachments.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                msg.attachments.forEach { att ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .padding(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when (att.type) {
                                                    AttachmentType.IMAGE -> Icons.Default.Image
                                                    AttachmentType.PDF -> Icons.Default.PictureAsPdf
                                                    else -> Icons.Default.InsertDriveFile
                                                },
                                                contentDescription = null,
                                                tint = when (att.type) {
                                                    AttachmentType.IMAGE -> NeonCyan
                                                    AttachmentType.PDF -> AccentRed
                                                    else -> NeonPurple
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = att.fileName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                            SoraBadge(text = if (att.type == AttachmentType.IMAGE) "IMG" else if (att.type == AttachmentType.PDF) "PDF" else "FILE", color = NeonCyan)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Selectable Text Container
                        SelectionContainer {
                            Text(
                                text = if (msg.text.isBlank() && isStreaming && !isUser) "Thinking..." else msg.text,
                                fontSize = 12.5.sp,
                                color = if (isUser) Color.White else TextPrimary
                            )
                        }

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
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(ElectricPink)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // If a script package was generated, display script breakdown in chat
            val pkg = scriptPkg
            if (pkg != null) {
                item {
                    SoraGlassCard(borderColor = NeonPurple) {
                        Text(text = "📜 Script Production Package: ${pkg.title}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = pkg.scriptText, fontSize = 11.5.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        pkg.shots.forEach { shot ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GlassSurfaceVariant)
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Shot ${shot.shotNumber}: ${shot.title}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                        Text(text = shot.promptText, fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updatePrompt(shot.promptText)
                                            viewModel.selectTab(SoraTab.GENERATE)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("send_shot_${shot.shotNumber}")
                                    ) {
                                        Text("Send", fontSize = 9.5.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stop Generation Floating Strip (when streaming)
        if (isStreaming) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.stopChatGeneration() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed),
                    modifier = Modifier.testTag("stop_generation_btn")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Generating", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                }
            }
        }

        // Staged Attachments Strip
        if (stagedAttachments.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                color = GlassSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📎 Ready to send (${stagedAttachments.size} attachment${if (stagedAttachments.size > 1) "s" else ""})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "Clear All",
                            fontSize = 10.sp,
                            color = AccentRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.clearStagedChatAttachments() }
                                .padding(2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stagedAttachments) { att ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (att.type) {
                                            AttachmentType.IMAGE -> Icons.Default.Image
                                            AttachmentType.PDF -> Icons.Default.PictureAsPdf
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = when (att.type) {
                                            AttachmentType.IMAGE -> NeonCyan
                                            AttachmentType.PDF -> AccentRed
                                            else -> NeonPurple
                                        },
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (att.fileName.length > 16) att.fileName.take(16) + "..." else att.fileName,
                                        fontSize = 10.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.removeChatAttachment(att.id) },
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GlassSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload Attachment (+) Button
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier.testTag("chat_attachment_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Attach File", tint = NeonCyan, modifier = Modifier.size(22.dp))
                    }

                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🖼️ Upload Image (Keyframe/Storyboard)") },
                            onClick = {
                                showAttachmentMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📄 Upload PDF Document (Script/Notes)") },
                            onClick = {
                                showAttachmentMenu = false
                                pdfPickerLauncher.launch("application/pdf")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📁 Upload File (Any Format)") },
                            onClick = {
                                showAttachmentMenu = false
                                filePickerLauncher.launch("*/*")
                            }
                        )
                    }
                }

                IconButton(onClick = { viewModel.triggerWakeWordEvent("Hey Sora! Write a scene script") }) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Command", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }

                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text("Message AI or ask to script video...", fontSize = 12.sp, color = TextSecondary) },
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
                        if (chatInputText.isNotBlank() || stagedAttachments.isNotEmpty()) {
                            viewModel.sendChatMessage(chatInputText, stagedAttachments)
                            chatInputText = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = if (chatInputText.isNotBlank() || stagedAttachments.isNotEmpty()) ElectricPink else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
