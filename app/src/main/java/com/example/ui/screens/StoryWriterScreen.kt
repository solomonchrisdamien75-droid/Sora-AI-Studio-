package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.inference.model.ModelCapability
import com.example.ai.jobs.AIJobStatus
import com.example.ai.story.StoryCharacter
import com.example.ai.story.StoryEditOperation
import com.example.ai.story.StoryProject
import com.example.ui.SoraMainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryWriterScreen(
    viewModel: SoraMainViewModel,
    onBack: () -> Unit = {}
) {
    val storyEngine = viewModel.storyEngine
    val storyProject by storyEngine.currentStory.collectAsState()
    val isGenerating by storyEngine.isGenerating.collectAsState()
    val generationPhase by storyEngine.generationPhase.collectAsState()
    val statusMessage by storyEngine.statusMessage.collectAsState()
    val activeModel by viewModel.activeLoadedModel.collectAsState()
    val unifiedJobs by viewModel.unifiedJobs.collectAsState()
    val activeStoryJob = unifiedJobs.firstOrNull { it.type == com.example.ai.jobs.AIJobType.STORY_GENERATION && it.status == AIJobStatus.RUNNING }

    val coroutineScope = rememberCoroutineScope()
    var showCharacterDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showContinueDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedTextToEdit by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Editor & Reader, 1: Premise & Structure, 2: Characters & Lore

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Story Writer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "NEURAL PROSE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = storyProject.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("story_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("story_export_button")) {
                        Icon(Icons.Default.Share, contentDescription = "Export Manuscript")
                    }
                    IconButton(onClick = { showContinueDialog = true }, modifier = Modifier.testTag("story_continue_button")) {
                        Icon(Icons.Default.FastForward, contentDescription = "Continue Story")
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
            // Model Status Bar with real capability detection
            StoryModelCapabilityHeader(
                activeModel = activeModel,
                viewModel = viewModel
            )

            // Live Background Generation Banner if running
            if (isGenerating || activeStoryJob != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
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
                                text = "⚡ Generating Story Manuscript",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activeStoryJob?.checkpointPhase ?: generationPhase,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        activeStoryJob?.let { job: com.example.ai.jobs.UnifiedAIJob ->
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

            // Studio Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Manuscript & Reader") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Outline & Setup") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Characters & Lore") },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (activeTab) {
                0 -> StoryReaderAndEditorTab(
                    project = storyProject,
                    storyEngine = storyEngine,
                    isGenerating = isGenerating,
                    onGenerate = {
                        coroutineScope.launch {
                            storyEngine.generateFullStory(storyProject, activeModel)
                        }
                    },
                    onOpenEdit = { text ->
                        selectedTextToEdit = text
                        showEditSheet = true
                    },
                    onContinue = {
                        showContinueDialog = true
                    }
                )
                1 -> StorySetupAndOutlineTab(
                    project = storyProject,
                    onUpdate = { storyEngine.updateStoryProject(it) },
                    onGenerate = {
                        coroutineScope.launch {
                            storyEngine.generateFullStory(storyProject, activeModel)
                            activeTab = 0
                        }
                    },
                    isGenerating = isGenerating
                )
                2 -> StoryCharactersAndLoreTab(
                    project = storyProject,
                    onUpdate = { storyEngine.updateStoryProject(it) },
                    onAddCharacter = { showCharacterDialog = true },
                    onRemoveCharacter = { storyEngine.removeCharacter(it) }
                )
            }
        }
    }

    // Add Character Dialog
    if (showCharacterDialog) {
        AddCharacterModalDialog(
            onDismiss = { showCharacterDialog = false },
            onConfirm = { char ->
                storyEngine.addCharacter(char)
                showCharacterDialog = false
            }
        )
    }

    // Story Edit & Refinement Bottom Sheet
    if (showEditSheet) {
        StoryEditModalSheet(
            initialText = selectedTextToEdit,
            onDismiss = { showEditSheet = false },
            onApply = { op, param ->
                coroutineScope.launch {
                    val res = storyEngine.applyEditOperation(selectedTextToEdit, op, param, activeModel)
                    if (res.isSuccess) {
                        val newText = res.getOrThrow()
                        val currentCh = storyProject.chapters.getOrNull(storyProject.activeChapterIndex)
                        if (currentCh != null) {
                            val updatedProse = if (selectedTextToEdit.isNotBlank() && currentCh.fullProse.contains(selectedTextToEdit)) {
                                currentCh.fullProse.replace(selectedTextToEdit, newText)
                            } else {
                                newText
                            }
                            val updatedChapters = storyProject.chapters.toMutableList().apply {
                                set(storyProject.activeChapterIndex, currentCh.copy(fullProse = updatedProse))
                            }
                            storyEngine.updateStoryProject(storyProject.copy(chapters = updatedChapters))
                        }
                    }
                    showEditSheet = false
                }
            }
        )
    }

    // Continue Story Dialog
    if (showContinueDialog) {
        ContinueStoryModalDialog(
            project = storyProject,
            onDismiss = { showContinueDialog = false },
            onConfirm = { fromChapter ->
                coroutineScope.launch {
                    storyEngine.continueStory(storyProject, fromChapter, activeModel)
                    showContinueDialog = false
                    activeTab = 0
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportManuscriptModalDialog(
            project = storyProject,
            storageManager = viewModel.projectStorageManager,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun StoryModelCapabilityHeader(
    activeModel: com.example.data.AiModelEntity?,
    viewModel: SoraMainViewModel
) {
    val compCheck = remember(activeModel) {
        viewModel.aiInferenceManager.validateCapability(activeModel, ModelCapability.STORY_WRITING)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = if (compCheck.isCompatible) "Ready for Long-Form Narrative & Multi-Chapter Prose" else (compCheck.errorMessage ?: "Model check required"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StoryReaderAndEditorTab(
    project: StoryProject,
    storyEngine: com.example.ai.story.StoryEngine,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    onOpenEdit: (String) -> Unit,
    onContinue: () -> Unit
) {
    if (project.chapters.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No Story Generated Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Configure your premise and characters, then generate your multi-chapter story manuscript.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.testTag("story_generate_initial_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isGenerating) "Generating..." else "Generate Story")
                }
            }
        }
        return
    }

    val activeIndex = project.activeChapterIndex.coerceIn(0, project.chapters.lastIndex)
    val activeChapter = project.chapters[activeIndex]

    Column(modifier = Modifier.fillMaxSize()) {
        // Chapter selector bar
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(project.chapters.size) { index ->
                val ch = project.chapters[index]
                FilterChip(
                    selected = index == activeIndex,
                    onClick = { storyEngine.setActiveChapter(index) },
                    label = { Text("Ch ${ch.chapterIndex}: ${ch.title.take(15)}") },
                    leadingIcon = if (index == activeIndex) {
                        { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
            item {
                IconButton(onClick = onContinue) {
                    Icon(Icons.Default.Add, contentDescription = "Add Next Chapter", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Manuscript Reader & Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = activeChapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Word Count: ${activeChapter.wordCount} words • Summary: ${activeChapter.summary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Full Chapter Prose with Paragraph Interactive Editing
            val paragraphs = activeChapter.fullProse.split("\n\n").filter { it.isNotBlank() }
            paragraphs.forEachIndexed { pIdx, paragraph ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onOpenEdit(paragraph) }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Tap to refine with AI",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // Bottom Action Bar
        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenEdit(activeChapter.fullProse) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refine Chapter", fontSize = 12.sp)
                }
                Button(
                    onClick = onContinue,
                    enabled = !isGenerating,
                    modifier = Modifier.weight(1f).testTag("story_continue_next_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Next Chapter", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StorySetupAndOutlineTab(
    project: StoryProject,
    onUpdate: (StoryProject) -> Unit,
    onGenerate: () -> Unit,
    isGenerating: Boolean
) {
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
            label = { Text("Story Title") },
            modifier = Modifier.fillMaxWidth().testTag("story_title_input")
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = project.genre,
                onValueChange = { onUpdate(project.copy(genre = it)) },
                label = { Text("Genre") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = project.tone,
                onValueChange = { onUpdate(project.copy(tone = it)) },
                label = { Text("Tone") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = project.writingStyle,
                onValueChange = { onUpdate(project.copy(writingStyle = it)) },
                label = { Text("Writing Style") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = project.pointOfView,
                onValueChange = { onUpdate(project.copy(pointOfView = it)) },
                label = { Text("Point of View") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = project.setting,
            onValueChange = { onUpdate(project.copy(setting = it)) },
            label = { Text("Setting & World Location") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = project.mainConflict,
            onValueChange = { onUpdate(project.copy(mainConflict = it)) },
            label = { Text("Main Central Conflict") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = project.customInstructions,
            onValueChange = { onUpdate(project.copy(customInstructions = it)) },
            label = { Text("Custom Author Directives") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        // Chapter count slider
        Column {
            Text("Chapter Count: ${project.chapterCount} Chapters", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Slider(
                value = project.chapterCount.toFloat(),
                onValueChange = { onUpdate(project.copy(chapterCount = it.toInt())) },
                valueRange = 1f..10f,
                steps = 8
            )
        }

        if (project.outline.isNotBlank()) {
            Text("Story Outline & 3-Act Structure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = project.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("story_full_generate_btn")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isGenerating) "Generating Story..." else "Generate Story Structure & Manuscript")
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun StoryCharactersAndLoreTab(
    project: StoryProject,
    onUpdate: (StoryProject) -> Unit,
    onAddCharacter: () -> Unit,
    onRemoveCharacter: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Characters (${project.characters.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onAddCharacter, modifier = Modifier.testTag("story_add_char_btn")) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Character")
            }
        }

        project.characters.forEach { char ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(char.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(char.role, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                            IconButton(onClick = { onRemoveCharacter(char.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (char.personality.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Personality: ${char.personality}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (char.backstory.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text("Backstory: ${char.backstory}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("World Lore & Continuity Memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = project.worldMemory,
            onValueChange = { onUpdate(project.copy(worldMemory = it)) },
            label = { Text("World Rules, Magic/Tech Laws, Continuity Lore") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun AddCharacterModalDialog(
    onDismiss: () -> Unit,
    onConfirm: (StoryCharacter) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Protagonist") }
    var personality by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var backstory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Character") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (Protagonist, Antagonist, etc.)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("Personality & Traits") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = appearance, onValueChange = { appearance = it }, label = { Text("Visual Appearance") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = backstory, onValueChange = { backstory = it }, label = { Text("Backstory & Motivation") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(StoryCharacter(name = name, role = role, personality = personality, appearance = appearance, backstory = backstory))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditModalSheet(
    initialText: String,
    onDismiss: () -> Unit,
    onApply: (StoryEditOperation, String) -> Unit
) {
    var selectedOp by remember { mutableStateOf(StoryEditOperation.REWRITE) }
    var customParam by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Refine Prose with AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = initialText.take(200) + if (initialText.length > 200) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Text("Select Operation:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(StoryEditOperation.entries) { op ->
                    FilterChip(
                        selected = op == selectedOp,
                        onClick = { selectedOp = op },
                        label = { Text(op.label, fontSize = 11.sp) }
                    )
                }
            }

            if (selectedOp == StoryEditOperation.CHANGE_TONE || selectedOp == StoryEditOperation.CHANGE_GENRE || selectedOp == StoryEditOperation.TRANSLATE) {
                OutlinedTextField(
                    value = customParam,
                    onValueChange = { customParam = it },
                    label = { Text("Target (${if (selectedOp == StoryEditOperation.TRANSLATE) "Language" else "Tone/Genre"})") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { onApply(selectedOp, customParam) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Apply ${selectedOp.label}")
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun ContinueStoryModalDialog(
    project: StoryProject,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Continue Story") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Continue the narrative of '${project.title}' seamlessly from Chapter ${project.chapters.size} without restarting from scratch.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Contextual rolling memory and character goals will be injected into the next chapter generation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(project.chapters.size) }) {
                Text("Write Next Chapter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExportManuscriptModalDialog(
    project: StoryProject,
    storageManager: com.example.data.ProjectStorageManager,
    onDismiss: () -> Unit
) {
    var exportStatus by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Manuscript") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select format to save into SoraProjects/Stories/:", style = MaterialTheme.typography.bodySmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val content = "# ${project.title}\n\n${project.chapters.joinToString("\n\n") { "## " + it.title + "\n" + it.fullProse }}"
                            val file = storageManager.exportContent(project.title, content, "Stories", "md")
                            exportStatus = "Saved Markdown to ${file.fileName} (${file.fileSizeFormatted})"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Markdown (.md)")
                    }
                    Button(
                        onClick = {
                            val content = "${project.title}\n\n${project.chapters.joinToString("\n\n") { it.title + "\n" + it.fullProse }}"
                            val file = storageManager.exportContent(project.title, content, "Stories", "txt")
                            exportStatus = "Saved TXT to ${file.fileName} (${file.fileSizeFormatted})"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Text (.txt)")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val content = "${project.title}\n\n${project.chapters.joinToString("\n\n") { it.title + "\n" + it.fullProse }}"
                            val file = storageManager.exportContent(project.title, content, "Stories", "pdf")
                            exportStatus = "Saved Document to ${file.fileName} (${file.fileSizeFormatted})"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Document (.pdf)")
                    }
                }

                exportStatus?.let {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
