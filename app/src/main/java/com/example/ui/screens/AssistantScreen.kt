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
    val isLoading by viewModel.isAssistantLoading.collectAsState()

    var chatInputText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Activity Result Launchers for Image, PDF, and Generic Files
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
        // Top Header
        SoraSectionHeader(
            title = "🤖 AI Chat & Action Assistant",
            subtitle = "Upload images, PDFs, & files. Ask AI to execute device actions & generate scripts",
            icon = Icons.Default.Chat
        )

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
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Quick Action & Upload Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("upload_image_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Upload Image", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
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
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("upload_pdf_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Upload PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                        .clickable { filePickerLauncher.launch("*/*") }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("upload_file_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Upload File", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                    }
                }
            }
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
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                            .widthIn(max = 300.dp)
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

                        // Render attachments if attached to message
                        if (msg.attachments.isNotEmpty()) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                msg.attachments.forEach { att ->
                                    when (att.type) {
                                        AttachmentType.IMAGE -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                                    .padding(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = att.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                        Text(text = "Image Keyframe Upload", fontSize = 9.sp, color = TextSecondary)
                                                    }
                                                    SoraBadge(text = "IMG", color = NeonCyan)
                                                }
                                            }
                                        }
                                        AttachmentType.PDF -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                                    .padding(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = att.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                        Text(text = "PDF Document Upload", fontSize = 9.sp, color = TextSecondary)
                                                    }
                                                    SoraBadge(text = "PDF", color = AccentRed)
                                                }
                                            }
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                                    .padding(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = att.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                        Text(text = "Attached File", fontSize = 9.sp, color = TextSecondary)
                                                    }
                                                    SoraBadge(text = "FILE", color = NeonPurple)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

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

        Spacer(modifier = Modifier.height(6.dp))

        // Staged Attachments Strip (shows files selected before sending)
        if (stagedAttachments.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                color = GlassSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (att.fileName.length > 18) att.fileName.take(18) + "..." else att.fileName,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.removeChatAttachment(att.id) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Input Bar with Attachment Dropdown & Voice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GlassSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassSurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upload Attachment (+) Button with Dropdown Menu
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier.testTag("chat_attachment_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Attach File", tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }

                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🖼️ Upload Image (Photos/Gallery)") },
                            onClick = {
                                showAttachmentMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📄 Upload PDF Document") },
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

                IconButton(onClick = { viewModel.triggerWakeWordEvent("Skra! Open YouTube") }) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Command", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }

                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text("Ask AI or attach photos/PDFs...", fontSize = 12.sp, color = TextSecondary) },
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
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message", tint = ElectricPink, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

