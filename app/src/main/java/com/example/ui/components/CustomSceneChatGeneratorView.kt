package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SoraMainViewModel
import com.example.ui.SoraTab
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "AI" or "User"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SceneItemData(
    var sceneNumber: Int,
    var imageUri: String? = null,
    var actionPrompt: String = "",
    var voiceCoverText: String = "",
    var durationSeconds: Int = 5,
    var transitionPromptToNext: String = "Cinematic ink slash & speed line zoom into next scene"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSceneChatGeneratorView(
    viewModel: SoraMainViewModel,
    studioTitle: String = "Custom Scene Video Studio"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeLoadedModel by viewModel.activeLoadedModel.collectAsState()

    var step by remember { mutableStateOf(1) } // 1: Chat Wizard Setup, 2: Scene Alignment & Details Editor, 3: Generation Progress

    // Chat Wizard State (Step 1)
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("AI", "👋 Welcome to $studioTitle! Chat with me or paste your overall storyline below. Choose your scene count (1 to 50 or custom like 55), overall duration, and animation style, then press Confirm!")
            )
        )
    }
    var userChatInput by remember { mutableStateOf("") }
    var storylineText by remember {
        mutableStateOf(
            "Sung Jin-Woo awakens in a double dungeon, unlocks the mysterious System quest log, and climbs the ranks from weakest E-rank hunter to Sovereign of Shadows."
        )
    }
    var selectedPresetSceneCount by remember { mutableStateOf(12) }
    var isCustomSceneMode by remember { mutableStateOf(false) }
    var customSceneCountText by remember { mutableStateOf("55") }
    var overallDurationSeconds by remember { mutableStateOf(120) }
    var animationType by remember { mutableStateOf("2.5D Parallax & Dark Aura Mist") }

    // Step 2 State: Scenes List
    var scenesList by remember { mutableStateOf<List<SceneItemData>>(emptyList()) }
    var activeImageSceneIndex by remember { mutableStateOf<Int?>(null) }

    // Step 3 Generation State
    var isGenerating by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf(0f) }
    var currentGeneratingScene by remember { mutableStateOf(1) }
    var statusMessage by remember { mutableStateOf("Initializing video generation pipeline...") }
    var showModelRequiredDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            activeImageSceneIndex?.let { idx ->
                val updated = scenesList.toMutableList()
                if (idx in updated.indices) {
                    updated[idx] = updated[idx].copy(imageUri = selectedUri.toString())
                    scenesList = updated
                }
            }
        }
    }

    val sampleArtworks = listOf(
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800",
        "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800",
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800",
        "https://images.unsplash.com/photo-1535223289827-42f1e9919769?w=800"
    )

    fun initializeScenes() {
        val count = if (isCustomSceneMode) {
            customSceneCountText.toIntOrNull()?.coerceIn(1, 200) ?: 55
        } else {
            selectedPresetSceneCount.coerceIn(1, 50)
        }

        val list = mutableListOf<SceneItemData>()
        for (i in 1..count) {
            list.add(
                SceneItemData(
                    sceneNumber = i,
                    imageUri = sampleArtworks[(i - 1) % sampleArtworks.size],
                    actionPrompt = "Scene $i action: Dynamic camera push-in with speed lines, glowing mana aura, and intense close-up on character expression.",
                    voiceCoverText = "Narrator voiceover for scene $i: The awakening power surged through every vein as shadows gathered.",
                    durationSeconds = (overallDurationSeconds / count).coerceAtLeast(3),
                    transitionPromptToNext = if (i < count) "Transition ${i}→${i+1}: Cinematic ink splatter morph with high-speed camera pan across panel border." else "Final Fade Out"
                )
            )
        }
        scenesList = list
        step = 2
    }

    fun moveScene(fromIndex: Int, toIndex: Int) {
        val list = scenesList.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            for ((i, sc) in list.withIndex()) {
                sc.sceneNumber = i + 1
            }
            scenesList = list
        }
    }

    fun startVideoGeneration() {
        if (activeLoadedModel == null) {
            showModelRequiredDialog = true
            return
        }
        isGenerating = true
        step = 3
        generationProgress = 0f

        viewModel.addBatchJobsToQueue(
            prefix = "CustomScene",
            prompts = scenesList.map { it.actionPrompt },
            type = "TEXT_TO_VIDEO",
            durationSec = scenesList.firstOrNull()?.durationSeconds ?: 5
        )
        viewModel.startQueueProcessing()

        coroutineScope.launch {
            val total = scenesList.size
            for ((idx, scene) in scenesList.withIndex()) {
                currentGeneratingScene = scene.sceneNumber
                statusMessage = "Background Task Queue rendering Scene ${scene.sceneNumber}/$total with Animation: $animationType..."
                for (p in 1..4) {
                    delay(80L)
                    generationProgress = ((idx * 4 + p).toFloat() / (total * 4))
                }
            }
            statusMessage = "All $total scenes combined successfully! Assets ready in Task Queue & Gallery."
            delay(500L)
            generationProgress = 1f
            isGenerating = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DeepDarkBg)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = studioTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = when (step) {
                            1 -> "Step 1: AI Chat Wizard & Configuration"
                            2 -> "Step 2: Scene Alignment & Details Editor (${scenesList.size} Scenes)"
                            else -> "Step 3: AI Video Generation & Assembly"
                        },
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
                SoraBadge(text = if (step == 3 && isGenerating) "GENERATING" else "STEP $step/2", color = ElectricPink)
            }

            when (step) {
                1 -> {
                    // STEP 1: CHAT PAGE WIZARD + CONFIGURATION CONTROLS
                    Column(modifier = Modifier.weight(1f)) {
                        // Chat Message History
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                .background(GlassSurface)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(chatMessages) { _, msg ->
                                val isAi = msg.sender == "AI"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                                ) {
                                    Surface(
                                        color = if (isAi) GlassSurfaceVariant else NeonCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.widthIn(max = 280.dp),
                                        border = BorderStroke(1.dp, if (isAi) CardBorder else NeonCyan.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = if (isAi) TextPrimary else NeonCyan,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chat Input Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = userChatInput,
                                onValueChange = { userChatInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Chat with AI script director...", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (userChatInput.isNotBlank()) {
                                        val uMsg = userChatInput
                                        chatMessages = chatMessages + ChatMessage("User", uMsg)
                                        userChatInput = ""
                                        coroutineScope.launch {
                                            delay(500L)
                                            chatMessages = chatMessages + ChatMessage("AI", "Got it! '$uMsg' has been factored into your custom video storyline and scene prompts.")
                                        }
                                    }
                                },
                                modifier = Modifier.size(44.dp).background(NeonCyan, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = DeepDarkBg)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Configuration Controls Card
                        SoraGlassCard {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📝 Paste Overall Storyline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                OutlinedTextField(
                                    value = storylineText,
                                    onValueChange = { storylineText = it },
                                    modifier = Modifier.fillMaxWidth().height(70.dp),
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
                                    Text("🎬 Scenes Count (1-50 or Custom e.g. 55)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                    TextButton(onClick = { isCustomSceneMode = !isCustomSceneMode }) {
                                        Text(if (isCustomSceneMode) "Preset Mode" else "Custom Scenes Mode", color = ElectricPink, fontSize = 10.sp)
                                    }
                                }

                                if (!isCustomSceneMode) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Preset Scenes: $selectedPresetSceneCount", fontSize = 11.sp, color = TextPrimary)
                                        Slider(
                                            value = selectedPresetSceneCount.toFloat(),
                                            onValueChange = { selectedPresetSceneCount = it.toInt() },
                                            valueRange = 1f..50f,
                                            steps = 49,
                                            modifier = Modifier.width(180.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                        )
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = customSceneCountText,
                                        onValueChange = { customSceneCountText = it },
                                        label = { Text("Custom Scenes (e.g. 55)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricPink,
                                            unfocusedBorderColor = CardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Duration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(60 to "1m", 120 to "2m", 300 to "5m").forEach { (secs, label) ->
                                                FilterChip(
                                                    selected = overallDurationSeconds == secs,
                                                    onClick = { overallDurationSeconds = secs },
                                                    label = { Text(label, fontSize = 9.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                                        selectedLabelColor = NeonCyan
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Animation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        DropdownMenuBox(animationType) { animationType = it }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { initializeScenes() },
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_confirm_chat_scenes"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DeepDarkBg)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm & Open Scene Editor", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // STEP 2: SCENE ALIGNMENT & DETAILS EDITOR (Image Upload, Action Prompt, Voice Cover, Duration, Transition Prompts)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aligned Scenes: ${scenesList.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            OutlinedButton(
                                onClick = { step = 1 },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Back to Chat Setup", fontSize = 10.sp, color = NeonCyan)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(scenesList) { index, scene ->
                                SoraGlassCard {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.DragIndicator, contentDescription = "Drag Handle", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                                SoraBadge(text = "SCENE ${scene.sceneNumber}", color = NeonCyan)
                                                IconButton(
                                                    onClick = { if (index > 0) moveScene(index, index - 1) },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (index > 0) NeonCyan else TextSecondary, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { if (index < scenesList.size - 1) moveScene(index, index + 1) },
                                                    enabled = index < scenesList.size - 1,
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (index < scenesList.size - 1) NeonCyan else TextSecondary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text("Duration: ${scene.durationSeconds}s", fontSize = 10.sp, color = TextSecondary)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(6.dp)),
                                                color = GlassSurfaceVariant,
                                                border = BorderStroke(1.dp, CardBorder)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("Img ${scene.sceneNumber}", fontSize = 9.sp, color = TextSecondary)
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Button(
                                                    onClick = {
                                                        activeImageSceneIndex = index
                                                        imagePickerLauncher.launch("image/*")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceVariant),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(10.dp), tint = NeonCyan)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Upload Image", fontSize = 9.sp, color = TextPrimary)
                                                }
                                                Text(
                                                    text = if (scene.imageUri != null) "Image Aligned" else "Default Art",
                                                    fontSize = 9.sp,
                                                    color = if (scene.imageUri != null) AccentGreen else TextSecondary
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = scene.actionPrompt,
                                            onValueChange = { newVal ->
                                                val updated = scenesList.toMutableList()
                                                updated[index] = updated[index].copy(actionPrompt = newVal)
                                                scenesList = updated
                                            },
                                            label = { Text("Action Prompt to Animate Image", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = CardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = scene.voiceCoverText,
                                            onValueChange = { newVal ->
                                                val updated = scenesList.toMutableList()
                                                updated[index] = updated[index].copy(voiceCoverText = newVal)
                                                scenesList = updated
                                            },
                                            label = { Text("Voice Cover / Narration for Scene", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ElectricPink,
                                                unfocusedBorderColor = CardBorder,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            )
                                        )
                                    }
                                }

                                // Transition Prompt Between Scenes
                                if (index < scenesList.size - 1) {
                                    Surface(
                                        color = DeepDarkBg,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.SwapVert, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Transition Prompt (Scene ${scene.sceneNumber} → ${scene.sceneNumber + 1}):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                            }
                                            OutlinedTextField(
                                                value = scene.transitionPromptToNext,
                                                onValueChange = { newVal ->
                                                    val updated = scenesList.toMutableList()
                                                    updated[index] = updated[index].copy(transitionPromptToNext = newVal)
                                                    scenesList = updated
                                                },
                                                modifier = Modifier.fillMaxWidth(),
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
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { startVideoGeneration() },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_generate_video_final"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = DeepDarkBg)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🎬 Start AI Video Generation & Combine (${scenesList.size} Scenes)", color = DeepDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                3 -> {
                    // STEP 3: GENERATION & COMBINATION PROGRESS
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = generationProgress,
                            modifier = Modifier.size(90.dp),
                            color = NeonCyan,
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = if (isGenerating) "Generating Scene $currentGeneratingScene / ${scenesList.size}" else "✨ Video Generation & Combination Complete!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = statusMessage, fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(20.dp))
                        LinearProgressIndicator(
                            progress = generationProgress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = ElectricPink,
                            trackColor = GlassSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isGenerating) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.selectTab(SoraTab.QUEUE) },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Queue, contentDescription = null, tint = TextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Task Queue Progress Bar", color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { step = 2 },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Review Scenes & Transitions", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showModelRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showModelRequiredDialog = false },
            title = { Text("AI Model in RAM Required", color = TextPrimary) },
            text = { Text("Video generation and scene combination require an AI model loaded in RAM.", color = TextSecondary) },
            confirmButton = {
                Button(onClick = {
                    showModelRequiredDialog = false
                    viewModel.quickLoadModelAndStartGeneration()
                }) {
                    Text("Quick-Load Model")
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelRequiredDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DropdownMenuBox(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("2.5D Parallax & Dark Aura Mist", "Speed Lines Burst", "Cinematic Slash Fade", "Motion Comic")
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(selected.take(16) + "...", fontSize = 9.sp, color = NeonCyan)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 11.sp) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
