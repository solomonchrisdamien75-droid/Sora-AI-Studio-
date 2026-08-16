package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ai.inference.model.ModelCapability
import com.example.ai.jobs.AIJobStatus
import com.example.ai.script.ScriptProject
import com.example.ai.script.ScriptScene
import com.example.ui.SoraMainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptWriterScreen(
    viewModel: SoraMainViewModel,
    onBack: () -> Unit = {}
) {
    val scriptEngine = viewModel.scriptEngine
    val scriptProject by scriptEngine.currentScript.collectAsState()
    val isGenerating by scriptEngine.isGenerating.collectAsState()
    val generationPhase by scriptEngine.generationPhase.collectAsState()
    val statusMessage by scriptEngine.statusMessage.collectAsState()
    val activeModel by viewModel.activeLoadedModel.collectAsState()
    val unifiedJobs by viewModel.unifiedJobs.collectAsState()
    val activeScriptJob = unifiedJobs.values.firstOrNull { it.type == com.example.ai.jobs.AIJobType.SCRIPT_GENERATION && it.status == AIJobStatus.RUNNING }

    val coroutineScope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(0) } // 0: AV Production Matrix, 1: Idea & Parameters, 2: Audio & Visual Export
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Script Writer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "AV PRODUCTION PIPELINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = scriptProject.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("script_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("script_export_button")) {
                        Icon(Icons.Default.Share, contentDescription = "Export Script")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Model capability badge
            ScriptModelCapabilityHeader(activeModel = activeModel, viewModel = viewModel)

            // Live progress banner
            if (isGenerating || activeScriptJob != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎬 Synthesizing Production AV Script",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activeScriptJob?.checkpointPhase ?: generationPhase,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        activeScriptJob?.let { job: com.example.ai.jobs.UnifiedAIJob ->
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { job.progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Tab row
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("AV Script Matrix") },
                    icon = { Icon(Icons.Default.ViewStream, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Idea & Format") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Pipeline Actions") },
                    icon = { Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (activeTab) {
                0 -> ScriptScenesMatrixTab(
                    project = scriptProject,
                    isGenerating = isGenerating,
                    onGenerate = {
                        coroutineScope.launch {
                            scriptEngine.generateFullScript(scriptProject, activeModel)
                        }
                    },
                    onSendToVideo = {
                        coroutineScope.launch {
                            scriptEngine.sendToVideoGenerator(scriptProject)
                        }
                    },
                    onGenerateVoiceover = {
                        coroutineScope.launch {
                            scriptEngine.generateVoiceoverForScript(scriptProject)
                        }
                    }
                )
                1 -> ScriptIdeaAndSetupTab(
                    project = scriptProject,
                    onUpdate = { scriptEngine.updateScriptProject(it) },
                    onGenerate = {
                        coroutineScope.launch {
                            scriptEngine.generateFullScript(scriptProject, activeModel)
                            activeTab = 0
                        }
                    },
                    isGenerating = isGenerating
                )
                2 -> ScriptPipelineActionsTab(
                    project = scriptProject,
                    viewModel = viewModel,
                    onSendToVideo = {
                        coroutineScope.launch {
                            scriptEngine.sendToVideoGenerator(scriptProject)
                        }
                    },
                    onGenerateVoiceover = {
                        coroutineScope.launch {
                            scriptEngine.generateVoiceoverForScript(scriptProject)
                        }
                    },
                    onExport = { showExportDialog = true }
                )
            }
        }
    }

    if (showExportDialog) {
        ExportScriptModalDialog(
            project = scriptProject,
            storageManager = viewModel.projectStorageManager,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun ScriptModelCapabilityHeader(
    activeModel: com.example.data.AiModelEntity?,
    viewModel: SoraMainViewModel
) {
    val compCheck = remember(activeModel) {
        viewModel.aiInferenceManager.validateCapability(activeModel, ModelCapability.SCRIPT_WRITING)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (compCheck.isCompatible) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (compCheck.isCompatible) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = activeModel?.name ?: "Auto AI Model",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (compCheck.isCompatible) "Ready for Audio/Visual Matrix & Scene Directives" else (compCheck.errorMessage ?: "Model check required"),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScriptScenesMatrixTab(
    project: ScriptProject,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onSendToVideo: () -> Unit,
    onGenerateVoiceover: () -> Unit
) {
    if (project.scenes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No Script Generated Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Specify your topic, video format, and duration, then synthesize your full Audio/Visual production script.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("script_generate_initial_btn")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isGenerating) "Generating..." else "Generate Production Script")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                // Script Overview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(project.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${project.videoType} • ${project.scenes.size} Scenes • Total Duration: ${project.scenes.sumOf { it.durationSeconds }}s • ${project.platform}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (project.hook.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("🎯 Hook: ${project.hook}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }
                }
            }

            items(project.scenes) { scene ->
                ScriptSceneMatrixCard(scene = scene)
            }

            item {
                // Call to Action Card
                if (project.callToAction.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📣 Call To Action", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(project.callToAction, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }

        // Quick Bottom Action Row
        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGenerateVoiceover,
                    modifier = Modifier.weight(1f).testTag("script_gen_voiceover_btn")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Voiceover", fontSize = 11.sp)
                }
                Button(
                    onClick = onSendToVideo,
                    modifier = Modifier.weight(1f).testTag("script_send_video_btn")
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Send to Video", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ScriptSceneMatrixCard(scene: ScriptScene) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCENE ${scene.sceneNumber}: ${scene.title}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${scene.durationSeconds}s",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Two-Column AV Matrix representation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Left Column: Voiceover & Audio
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text("AUDIO / VO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(scene.voiceover, fontSize = 12.sp, lineHeight = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("🎵 ${scene.musicCue}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("💥 ${scene.soundEffects}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Right Column: Visual & Camera Direction
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(4.dp))
                            Text("VISUAL / CAMERA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(scene.visualDescription, fontSize = 12.sp, lineHeight = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("🎥 ${scene.cameraMovement}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("💡 ${scene.lighting}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("✂️ ${scene.transition}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            // Generated Video Prompt Inspector
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬 Video Prompt: ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(scene.videoPrompt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun ScriptIdeaAndSetupTab(
    project: ScriptProject,
    onUpdate: (ScriptProject) -> Unit,
    onGenerate: () -> Unit,
    isGenerating: Boolean
) {
    val videoTypes = listOf(
        "YouTube Explainer", "Short-form (TikTok/Reels/Shorts)", "Documentary", "Film / Short Film",
        "Anime Story", "Manhwa Recap", "Educational", "History & Lore", "Dark Psychology", "What-If Scenario",
        "Narration Story", "Podcast Segment", "Advertisement", "Technical Deep Dive"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = project.title,
            onValueChange = { onUpdate(project.copy(title = it)) },
            label = { Text("Script Title") },
            modifier = Modifier.fillMaxWidth().testTag("script_title_input")
        )

        OutlinedTextField(
            value = project.topic,
            onValueChange = { onUpdate(project.copy(topic = it)) },
            label = { Text("Topic & Core Concept") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Format / Script Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(videoTypes) { type ->
                FilterChip(
                    selected = project.videoType == type,
                    onClick = { onUpdate(project.copy(videoType = type)) },
                    label = { Text(type, fontSize = 11.sp) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = project.tone,
                onValueChange = { onUpdate(project.copy(tone = it)) },
                label = { Text("Tone") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = project.narratorStyle,
                onValueChange = { onUpdate(project.copy(narratorStyle = it)) },
                label = { Text("Narrator Voice Style") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = project.visualStyle,
            onValueChange = { onUpdate(project.copy(visualStyle = it)) },
            label = { Text("Visual Aesthetics & Art Style") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = project.callToAction,
            onValueChange = { onUpdate(project.copy(callToAction = it)) },
            label = { Text("Call To Action (CTA)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Duration & Scene Sliders
        Column {
            Text("Target Duration: ${project.targetDurationSeconds}s (${project.targetDurationSeconds / 60}m ${project.targetDurationSeconds % 60}s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Slider(
                value = project.targetDurationSeconds.toFloat(),
                onValueChange = { onUpdate(project.copy(targetDurationSeconds = it.toInt(), targetWordCount = (it.toInt() * 2.8).toInt())) },
                valueRange = 15f..300f,
                steps = 18
            )
        }

        Column {
            Text("Scene Count: ${project.sceneCount} Scenes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Slider(
                value = project.sceneCount.toFloat(),
                onValueChange = { onUpdate(project.copy(sceneCount = it.toInt())) },
                valueRange = 2f..12f,
                steps = 9
            )
        }

        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("script_full_generate_btn")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isGenerating) "Synthesizing Script..." else "Generate Production AV Script")
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun ScriptPipelineActionsTab(
    project: ScriptProject,
    viewModel: SoraMainViewModel,
    onSendToVideo: () -> Unit,
    onGenerateVoiceover: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Production Pipeline Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎬 Send to Video Studio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Enqueues all ${project.scenes.size} scenes into Task Queue and creates a unified video project.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onSendToVideo, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Movie, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enqueue All Scenes into Task Queue")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎙️ Generate Spoken Voiceover", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Synthesizes neural speech for all narration beats using the on-device acoustic vocoder.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onGenerateVoiceover, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Synthesize Script Audio Track")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📄 Export Project File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Export formatted AV Markdown, TXT, or JSON to SoraProjects/Scripts/.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Script Package")
                }
            }
        }
    }
}

@Composable
fun ExportScriptModalDialog(
    project: ScriptProject,
    storageManager: com.example.data.ProjectStorageManager,
    onDismiss: () -> Unit
) {
    var exportStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Script") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select export format for SoraProjects/Scripts/:", style = MaterialTheme.typography.bodySmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val fullMd = "# ${project.title}\n\n${project.scenes.joinToString("\n\n") { "### Scene ${it.sceneNumber}\n**VO:** ${it.voiceover}\n**Visual:** ${it.visualDescription}" }}"
                            val file = storageManager.exportContent(project.title, fullMd, "Scripts", "md")
                            exportStatus = "Saved Markdown to ${file.fileName} (${file.fileSizeFormatted})"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Markdown (.md)")
                    }
                    Button(
                        onClick = {
                            val fullTxt = "${project.title}\n\n${project.scenes.joinToString("\n\n") { "Scene ${it.sceneNumber}\nVO: ${it.voiceover}\nVisual: ${it.visualDescription}" }}"
                            val file = storageManager.exportContent(project.title, fullTxt, "Scripts", "txt")
                            exportStatus = "Saved Text to ${file.fileName} (${file.fileSizeFormatted})"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Text (.txt)")
                    }
                }

                exportStatus?.let {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
