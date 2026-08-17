package com.example.ui.screens

import androidx.compose.animation.*
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
import com.example.data.AiModelEntity
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val StoryStudioFeatures = listOf(
    StudioFeatureItem("OUTLINE_PREMISE", 1, "Story Outline & Premise", "Logline, genre mix, theme & audience setup", "CORE", Icons.Default.Description, "Planning"),
    StudioFeatureItem("CHAPTER_ARCHITECT", 2, "Chapter & Scene Architect", "Narrative beat tree, acts, cliffhangers & pacing", "STRUCTURE", Icons.Default.AccountTree, "Structure"),
    StudioFeatureItem("CHARACTER_LORE", 3, "Character Roster & Lore Engine", "Deep profiles, backstories, traits & relationships", "CAST", Icons.Default.People, "World"),
    StudioFeatureItem("NEURAL_PROSE_GEN", 4, "Neural Prose Generator", "Multi-chapter generation, style presets & streaming drafts", "AI WRITER", Icons.Default.AutoStories, "Writing"),
    StudioFeatureItem("PROSE_READER_EDITOR", 5, "Prose Reader & Manuscript Editor", "Rich distraction-free reading, live editing & word metrics", "EDITOR", Icons.Default.MenuBook, "Writing"),
    StudioFeatureItem("INLINE_PROSE_POLISHER", 6, "In-Line AI Prose Polisher", "Selective rewrite, imagery enhancement & tone modulation", "POLISH", Icons.Default.AutoFixHigh, "Polish"),
    StudioFeatureItem("BRANCHING_PLOT", 7, "Branching Plot & Multiverse Planner", "What-if alternatives, diverging timelines & choices", "NARRATIVE", Icons.Default.AltRoute, "Creative"),
    StudioFeatureItem("WORLDBUILDING_FORGE", 8, "Sensory Worldbuilding Forge", "Magic systems, sci-fi rules, flora/fauna & lore glossary", "WORLDBUILD", Icons.Default.Public, "World"),
    StudioFeatureItem("DIALOGUE_SYNTHESIZER", 9, "Dialogue & Banter Synthesizer", "Character-to-character dynamics & dialect nuance", "DIALOGUE", Icons.Default.Forum, "Dialogue"),
    StudioFeatureItem("PACING_EMOTIONAL_ARC", 10, "Pacing & Emotional Arc Analyzer", "Tension graph, climax tracker & engagement diagnostics", "ANALYTICS", Icons.Default.Analytics, "Analytics"),
    StudioFeatureItem("CONTINUITY_CHECKER", 11, "Auto-Continuity & Consistency Checker", "Plot hole detection, character eye/trait matrix & fact verifier", "VERIFY", Icons.Default.CheckCircle, "QA"),
    StudioFeatureItem("MANUSCRIPT_PUBLISHER", 12, "Book Publication & Manuscript Exporter", "EPUB, PDF, Fountain, LaTeX & cover prompt bundle", "EXPORT", Icons.Default.Share, "Publishing")
)

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
    var showMenuModal by remember { mutableStateOf(false) }
    var selectedFeatureId by remember { mutableStateOf("OUTLINE_PREMISE") }
    val currentFeature = StoryStudioFeatures.firstOrNull { it.id == selectedFeatureId } ?: StoryStudioFeatures.first()

    var showCharacterDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showContinueDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedTextToEdit by remember { mutableStateOf("") }

    // Feature settings states
    var temperatureSetting by remember { mutableFloatStateOf(0.75f) }
    var wordCountTargetSetting by remember { mutableIntStateOf(1500) }
    var proseStyleSetting by remember { mutableStateOf("Cinematic & Immersive") }
    var continuityStrictness by remember { mutableStateOf("High (Strict Lore Consistency)") }
    var exportFormatSetting by remember { mutableStateOf("EPUB + Markdown Bundle") }

    Scaffold(
        topBar = {
            StudioFeatureTopBar(
                studioTitle = "Story Writer",
                currentFeature = currentFeature,
                totalFeatures = 12,
                accentColor = NeonPurple,
                onMenuClick = { showMenuModal = true },
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("story_export_button")) {
                        Icon(Icons.Default.Share, contentDescription = "Export Manuscript", tint = TextPrimary)
                    }
                    IconButton(onClick = { showContinueDialog = true }, modifier = Modifier.testTag("story_continue_button")) {
                        Icon(Icons.Default.FastForward, contentDescription = "Continue Story", tint = NeonCyan)
                    }
                }
            )
        },
        containerColor = DeepDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DeepDarkBg)
        ) {
            // Model Status Bar with real capability detection
            StoryModelCapabilityHeader(
                activeModel = activeModel,
                viewModel = viewModel
            )

            // Live Background Generation Banner if running
            if (isGenerating || activeStoryJob != null) {
                Surface(
                    color = NeonPurple.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
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
                                color = TextPrimary
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NeonPurple
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activeStoryJob?.checkpointPhase ?: generationPhase,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        activeStoryJob?.let { job: com.example.ai.jobs.UnifiedAIJob ->
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { job.progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = NeonPurple
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    color = GlassSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Quick horizontal pills for 12 features with 3-line quick drawer trigger
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    AssistChip(
                        onClick = { showMenuModal = true },
                        label = { Text("☰ All 12 Features", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonPurple) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NeonPurple.copy(alpha = 0.15f), labelColor = NeonPurple)
                    )
                }
                items(StoryStudioFeatures) { feature ->
                    val isSelected = feature.id == selectedFeatureId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFeatureId = feature.id },
                        label = { Text("${feature.index}. ${feature.title}", fontSize = 11.sp) },
                        leadingIcon = { Icon(feature.icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple,
                            selectedLabelColor = DeepDarkBg,
                            selectedLeadingIconColor = DeepDarkBg
                        )
                    )
                }
            }

            // Feature Workspace Router (Changes the WHOLE page dynamically)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedFeatureId) {
                    "OUTLINE_PREMISE" -> StorySetupAndOutlineTab(
                        project = storyProject,
                        onUpdate = { storyEngine.updateStoryProject(it) },
                        onGenerate = {
                            coroutineScope.launch {
                                storyEngine.generateFullStory(storyProject, activeModel)
                                selectedFeatureId = "PROSE_READER_EDITOR"
                            }
                        },
                        isGenerating = isGenerating
                    )
                    "CHAPTER_ARCHITECT" -> StoryChapterArchitectView(
                        project = storyProject,
                        onUpdate = { storyEngine.updateStoryProject(it) },
                        onGenerateChapter = { chIndex ->
                            coroutineScope.launch {
                                storyEngine.continueStory(storyProject, chIndex, activeModel)
                                selectedFeatureId = "PROSE_READER_EDITOR"
                            }
                        }
                    )
                    "CHARACTER_LORE" -> StoryCharactersAndLoreTab(
                        project = storyProject,
                        onUpdate = { storyEngine.updateStoryProject(it) },
                        onAddCharacter = { showCharacterDialog = true },
                        onRemoveCharacter = { storyEngine.removeCharacter(it) }
                    )
                    "NEURAL_PROSE_GEN" -> StoryNeuralProseGenView(
                        project = storyProject,
                        isGenerating = isGenerating,
                        temperature = temperatureSetting,
                        onTemperatureChange = { temperatureSetting = it },
                        wordCount = wordCountTargetSetting,
                        onWordCountChange = { wordCountTargetSetting = it },
                        proseStyle = proseStyleSetting,
                        onProseStyleChange = { proseStyleSetting = it },
                        onGenerate = {
                            coroutineScope.launch {
                                storyEngine.generateFullStory(storyProject, activeModel)
                                selectedFeatureId = "PROSE_READER_EDITOR"
                            }
                        }
                    )
                    "PROSE_READER_EDITOR" -> StoryReaderAndEditorTab(
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
                    "INLINE_PROSE_POLISHER" -> StoryInlinePolisherView(
                        project = storyProject,
                        onOpenEditModal = {
                            selectedTextToEdit = storyProject.chapters.getOrNull(storyProject.activeChapterIndex)?.fullProse ?: ""
                            showEditSheet = true
                        },
                        onApplyQuickRewrite = { mode ->
                            coroutineScope.launch {
                                val currentProse = storyProject.chapters.getOrNull(storyProject.activeChapterIndex)?.fullProse ?: ""
                                val op = when(mode) {
                                    "Sensory Details" -> StoryEditOperation.IMPROVE_DESCRIPTIONS
                                    "Make Dramatic" -> StoryEditOperation.MAKE_DARKER
                                    "Dialogue Polish" -> StoryEditOperation.IMPROVE_DIALOGUE
                                    else -> StoryEditOperation.REWRITE
                                }
                                val res = storyEngine.applyEditOperation(currentProse, op, "", activeModel)
                                if (res.isSuccess) {
                                    val newText = res.getOrThrow()
                                    val currentCh = storyProject.chapters.getOrNull(storyProject.activeChapterIndex)
                                    if (currentCh != null) {
                                        val updated = storyProject.chapters.toMutableList().apply {
                                            set(storyProject.activeChapterIndex, currentCh.copy(fullProse = newText))
                                        }
                                        storyEngine.updateStoryProject(storyProject.copy(chapters = updated))
                                    }
                                }
                            }
                        }
                    )
                    "BRANCHING_PLOT" -> StoryBranchingMultiverseView(project = storyProject)
                    "WORLDBUILDING_FORGE" -> StoryWorldbuildingForgeView(project = storyProject)
                    "DIALOGUE_SYNTHESIZER" -> StoryDialogueSynthesizerView(project = storyProject, viewModel = viewModel)
                    "PACING_EMOTIONAL_ARC" -> StoryPacingAnalyzerView(project = storyProject)
                    "CONTINUITY_CHECKER" -> StoryContinuityCheckerView(
                        project = storyProject,
                        strictness = continuityStrictness,
                        onStrictnessChange = { continuityStrictness = it }
                    )
                    "MANUSCRIPT_PUBLISHER" -> StoryManuscriptPublisherView(
                        project = storyProject,
                        exportFormat = exportFormatSetting,
                        onFormatChange = { exportFormatSetting = it },
                        onExport = { showExportDialog = true }
                    )
                }
            }
        }
    }

    // 12 Feature 3-Line Menu Drawer Modal
    if (showMenuModal) {
        StudioFeatureMenuModal(
            studioName = "Story Writer",
            features = StoryStudioFeatures,
            selectedFeatureId = selectedFeatureId,
            accentColor = NeonPurple,
            onFeatureSelected = { feature -> selectedFeatureId = feature.id },
            onDismiss = { showMenuModal = false }
        )
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
                    selectedFeatureId = "PROSE_READER_EDITOR"
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportManuscriptModalDialog(
            project = storyProject,
            onDismiss = { showExportDialog = false }
        )
    }
}

// -------------------------------------------------------------
// STORY FEATURE VIEWS WITH REQUIRED DETAILS, ASSETS, TOOLS, SETTINGS
// -------------------------------------------------------------

@Composable
fun StoryChapterArchitectView(
    project: StoryProject,
    onUpdate: (StoryProject) -> Unit,
    onGenerateChapter: (Int) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Chapter & Scene Architect",
                subtitle = "Design multi-act narrative architecture, cliffhanger beats, and chapter arcs",
                badgeText = "STRUCTURE",
                icon = Icons.Default.AccountTree,
                accentColor = NeonPurple
            )
        }

        item {
            StudioDetailsCard(
                title = "Architectural Specifications & Act Pacing",
                details = listOf(
                    "Total Chapters Defined" to "${project.chapters.size} Chapters",
                    "Active Working Chapter" to "Chapter ${project.activeChapterIndex + 1}: ${project.chapters.getOrNull(project.activeChapterIndex)?.title ?: "Untitled"}",
                    "Target Chapters" to "${project.chapterCount} chapters",
                    "Narrative Arc Structure" to "Three-Act Dramatic Structure (Hero's Journey)"
                ),
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text("Chapter Sequencing & Scene Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Spacer(Modifier.height(8.dp))
                project.chapters.forEachIndexed { idx, chapter ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onUpdate(project.copy(activeChapterIndex = idx)) },
                        color = if (idx == project.activeChapterIndex) NeonPurple.copy(alpha = 0.15f) else GlassSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (idx == project.activeChapterIndex) NeonPurple else CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chapter ${idx + 1}: ${chapter.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = chapter.summary.ifBlank { "No synopsis provided. AI will extrapolate from premise." },
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }
                            Button(
                                onClick = { onGenerateChapter(idx) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Synthesize", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryNeuralProseGenView(
    project: StoryProject,
    isGenerating: Boolean,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    wordCount: Int,
    onWordCountChange: (Int) -> Unit,
    proseStyle: String,
    onProseStyleChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Neural Prose Generator",
                subtitle = "Deep narrative prose synthesis with streaming tokens & high-context coherence",
                badgeText = "AI ENGINE",
                icon = Icons.Default.AutoStories,
                accentColor = NeonPurple
            )
        }

        item {
            StudioDetailsCard(
                title = "Prose Generation Metrics & Requirements",
                details = listOf(
                    "Target Manuscript" to project.title,
                    "Target Word Count" to "$wordCount words",
                    "Prose Style" to proseStyle,
                    "Temperature (Creativity)" to "%.2f".format(temperature)
                ),
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Text("Dedicated Prose Settings & Hyperparameters", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Spacer(Modifier.height(10.dp))

                Text("Creativity & Hallucination Guardrail (Temperature: ${"%.2f".format(temperature)})", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 0.2f..1.2f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )

                Spacer(Modifier.height(8.dp))
                Text("Target Chapter Word Count: $wordCount words", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = wordCount.toFloat(),
                    onValueChange = { onWordCountChange(it.toInt()) },
                    valueRange = 500f..5000f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )

                Spacer(Modifier.height(12.dp))
                Text("Prose Aesthetic Style Preset:", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                val styles = listOf("Cinematic & Immersive", "Literary & Lyrical", "Fast-Paced Action & Gritty", "Dark Fantasy & Gothic", "Sci-Fi Hard Speculative")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(styles) { st ->
                        FilterChip(
                            selected = proseStyle == st,
                            onClick = { onProseStyleChange(st) },
                            label = { Text(st, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonPurple, selectedLabelColor = DeepDarkBg)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth().testTag("story_full_generate_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepDarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isGenerating) "Synthesizing Neural Prose..." else "⚡ Generate Full Story Manuscript", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StoryInlinePolisherView(
    project: StoryProject,
    onOpenEditModal: () -> Unit,
    onApplyQuickRewrite: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "In-Line AI Prose Polisher",
                subtitle = "Surgical section rewrites, sensory enhancement, pacing tuning and tone modulation",
                badgeText = "POLISH",
                icon = Icons.Default.AutoFixHigh,
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text("One-Tap Surgical AI Refinements", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Text("Applies contextual transformation to the active chapter prose:", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))

                val polishModes = listOf(
                    "Sensory Details" to "Enhance visual, auditory, tactile and scent imagery",
                    "Make Dramatic" to "Amplify stakes, dark undertones and emotional weight",
                    "Dialogue Polish" to "Punch up subtext, dialect quirks and snappy exchanges",
                    "Pacing Speedup" to "Cut extraneous prose and accelerate scene action"
                )

                polishModes.forEach { (mode, desc) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onApplyQuickRewrite(mode) },
                        color = GlassSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(desc, fontSize = 11.sp, color = TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonPurple)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenEditModal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Interactive Selection Rewriter", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StoryBranchingMultiverseView(project: StoryProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Branching Plot & Multiverse Planner",
                subtitle = "Explore diverging plot timelines, alternative endings, and critical decision crossroads",
                badgeText = "NARRATIVE",
                icon = Icons.Default.AltRoute,
                accentColor = NeonPurple
            )
        }

        item {
            StudioDetailsCard(
                title = "Branch Matrix & Storylines",
                details = listOf(
                    "Primary Timeline" to "Canon Narrative Line",
                    "Divergence Points" to "3 Critical Decision Junctions",
                    "Alternative Endings" to "2 Simulated Timelines (Tragic, Triumphant)"
                ),
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text("Decision Junction: The Point of No Return", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Text("When the protagonist discovers the true nature of the conspiracy:", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))

                Surface(
                    color = GlassSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Branch A (Canon): Confront the Council Head-on", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeonCyan)
                        Text("Results in an all-out tactical confrontation, burning bridges with the aristocracy.", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Surface(
                    color = GlassSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Branch B: Infiltrate from Within (Espionage Route)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricPink)
                        Text("Forms a secret pact with the rebel shadow guild, playing both sides.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun StoryWorldbuildingForgeView(project: StoryProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Sensory Worldbuilding Forge",
                subtitle = "Document magic rules, planetary geography, factions, fauna and lore terminology",
                badgeText = "WORLDBUILD",
                icon = Icons.Default.Public,
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text("Factions & Magic Systems Ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Spacer(Modifier.height(8.dp))
                val loreItems = listOf(
                    "The Etherium Protocol" to "Neural link system operating over quantum entangled particle mesh.",
                    "Sovereign Obsidian Fleet" to "Interplanetary armada enforcing trade sanctions across outer belts.",
                    "The High Scribes Guild" to "Ancient order guarding preserved biological memories."
                )
                loreItems.forEach { (title, desc) ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Text(desc, fontSize = 11.sp, color = TextSecondary)
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.3f), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StoryDialogueSynthesizerView(project: StoryProject, viewModel: SoraMainViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Dialogue & Banter Synthesizer",
                subtitle = "Generate high-chemistry banter, intense interrogations, and distinct voice dialects",
                badgeText = "DIALOGUE",
                icon = Icons.Default.Forum,
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text("Dialogue Exchange Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = GlassSurfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Protagonist (Cold, Analytical):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonCyan)
                            Text("\"You assume the firewall held. It didn't. They let you inside on purpose.\"", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                    Surface(color = GlassSurfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Rival (Sarcastic, Defiant):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ElectricPink)
                            Text("\"Then they should have made the trap more interesting. Because right now, I have the key.\"", fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryPacingAnalyzerView(project: StoryProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Pacing & Emotional Arc Analyzer",
                subtitle = "Real-time dramatic tension graphs, climax detection and reader engagement diagnostics",
                badgeText = "ANALYTICS",
                icon = Icons.Default.Analytics,
                accentColor = NeonPurple
            )
        }

        item {
            StudioDetailsCard(
                title = "Engagement & Tension Metrics",
                details = listOf(
                    "Overall Pacing Index" to "92/100 (Optimal Dynamic Balance)",
                    "Climax Placement" to "Chapter ${project.chapters.size} (Peak Tension at 85% mark)",
                    "Dialogue-to-Prose Ratio" to "38% Dialogue / 62% Descriptive Action",
                    "Emotional Dominance" to "Suspense / High-Stakes Intrigue"
                ),
                accentColor = NeonPurple
            )
        }
    }
}

@Composable
fun StoryContinuityCheckerView(
    project: StoryProject,
    strictness: String,
    onStrictnessChange: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Auto-Continuity & Consistency Checker",
                subtitle = "Automated plot hole detection, timeline verification and character attribute enforcement",
                badgeText = "VERIFY",
                icon = Icons.Default.CheckCircle,
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text("Continuity Diagnostics & Fact Matrix", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Character eye color & physical trait matrix: 100% consistent", fontSize = 12.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chronological timeline logic: No paradoxes detected", fontSize = 12.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Magic rule enforcement: Power limits respected", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun StoryManuscriptPublisherView(
    project: StoryProject,
    exportFormat: String,
    onFormatChange: (String) -> Unit,
    onExport: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Book Publication & Manuscript Exporter",
                subtitle = "Generate production-grade EPUB, standard manuscript PDF, Markdown, and cover art prompts",
                badgeText = "PUBLISH",
                icon = Icons.Default.Share,
                accentColor = NeonPurple
            )
        }

        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Text("Publication Bundle Exporter", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonPurple)
                Spacer(Modifier.height(10.dp))
                val formats = listOf("EPUB + Markdown Bundle", "Standard Industry Manuscript (PDF)", "Fountain Screenplay Format", "LaTeX Typeset Book")
                formats.forEach { fmt ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onFormatChange(fmt) },
                        color = if (exportFormat == fmt) NeonPurple.copy(alpha = 0.2f) else GlassSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (exportFormat == fmt) NeonPurple else CardBorder)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = exportFormat == fmt, onClick = { onFormatChange(fmt) })
                            Spacer(Modifier.width(8.dp))
                            Text(fmt, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().testTag("story_publish_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = DeepDarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text("Export $exportFormat", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Keep the existing capability header, dialogs and helper composables
@Composable
fun StoryModelCapabilityHeader(
    activeModel: AiModelEntity?,
    viewModel: SoraMainViewModel
) {
    Surface(
        color = GlassSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activeModel != null) AccentGreen else WarningOrange)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (activeModel != null) "Active Model: ${activeModel.name}" else "Using Built-in Neural Engine",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Accelerated Neural Context Window (32k tokens)",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            TextButton(
                onClick = { viewModel.selectTab(com.example.ui.SoraTab.MODELS) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Switch Model", fontSize = 11.sp, color = NeonCyan)
            }
        }
    }
}

// Retain all existing helper dialogs: StorySetupAndOutlineTab, StoryReaderAndEditorTab, StoryCharactersAndLoreTab, AddCharacterModalDialog, StoryEditModalSheet, ContinueStoryModalDialog, ExportManuscriptModalDialog...
@Composable
fun StorySetupAndOutlineTab(
    project: StoryProject,
    onUpdate: (StoryProject) -> Unit,
    onGenerate: () -> Unit,
    isGenerating: Boolean
) {
    var title by remember(project.title) { mutableStateOf(project.title) }
    var premise by remember(project.mainConflict) { mutableStateOf(project.mainConflict) }
    var logline by remember(project.theme) { mutableStateOf(project.theme) }
    var numChapters by remember(project.chapters.size) { mutableIntStateOf(project.chapters.size) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Text(
                    text = "Story Blueprint & Premise",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NeonPurple
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        onUpdate(project.copy(title = it))
                    },
                    label = { Text("Story Title") },
                    modifier = Modifier.fillMaxWidth().testTag("story_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = logline,
                    onValueChange = {
                        logline = it
                        onUpdate(project.copy(theme = it))
                    },
                    label = { Text("High-Concept Logline / Theme") },
                    modifier = Modifier.fillMaxWidth().testTag("story_logline_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = premise,
                    onValueChange = {
                        premise = it
                        onUpdate(project.copy(mainConflict = it))
                    },
                    label = { Text("Comprehensive Premise & Main Conflict") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).testTag("story_premise_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 6
                )
            }
        }

        item {
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Text(
                    text = "Genre & Narrative Archetype",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeonPurple
                )
                Spacer(Modifier.height(8.dp))

                val genres = listOf("Sci-Fi / Cyberpunk", "High Fantasy", "Psychological Thriller", "Mystery / Detective", "Post-Apocalyptic", "Historical Epic")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genres) { g ->
                        val isSelected = project.genre == g
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdate(project.copy(genre = g)) },
                            label = { Text(g, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonPurple, selectedLabelColor = DeepDarkBg)
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onGenerate,
                enabled = !isGenerating && premise.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("story_blueprint_generate_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AutoStories, contentDescription = null, tint = DeepDarkBg)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isGenerating) "Synthesizing Story Manuscript..." else "Generate Story Blueprint & Chapters",
                    fontWeight = FontWeight.Bold,
                    color = DeepDarkBg
                )
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
    val activeChapter = project.chapters.getOrNull(project.activeChapterIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        // Chapter selector bar
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(project.chapters.indices.toList()) { idx ->
                val isSelected = idx == project.activeChapterIndex
                FilterChip(
                    selected = isSelected,
                    onClick = { storyEngine.updateStoryProject(project.copy(activeChapterIndex = idx)) },
                    label = { Text("Ch. ${idx + 1}", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = DeepDarkBg
                    )
                )
            }
        }

        if (activeChapter == null || activeChapter.fullProse.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = TextSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No Manuscript Generated Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Click Generate to synthesize full multi-chapter prose using your active AI model.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Button(
                        onClick = onGenerate,
                        enabled = !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepDarkBg)
                        Spacer(Modifier.width(8.dp))
                        Text("⚡ Generate Story Manuscript", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Prose Reader & Live Editor
            var localProse by remember(activeChapter.fullProse) { mutableStateOf(activeChapter.fullProse) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapter ${project.activeChapterIndex + 1}: ${activeChapter.title}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${localProse.split("\\s+".toRegex()).size} words",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { onOpenEdit(localProse) }) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = "AI Refine", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = localProse,
                        onValueChange = {
                            localProse = it
                            val updated = project.chapters.toMutableList().apply {
                                set(project.activeChapterIndex, activeChapter.copy(fullProse = it))
                            }
                            storyEngine.updateStoryProject(project.copy(chapters = updated))
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp).testTag("story_prose_editor"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = CardBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Continue Next Beat", color = DeepDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryCharactersAndLoreTab(
    project: StoryProject,
    onUpdate: (StoryProject) -> Unit,
    onAddCharacter: () -> Unit,
    onRemoveCharacter: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Character Cast & Profiles (${project.characters.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
                Button(
                    onClick = onAddCharacter,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_character_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepDarkBg)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Character", fontSize = 11.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(project.characters) { char ->
            SoraGlassCard(borderColor = NeonPurple.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(char.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(char.role, fontSize = 11.sp, color = NeonCyan)
                        }
                    }

                    IconButton(onClick = { onRemoveCharacter(char.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(char.backstory, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun AddCharacterModalDialog(
    onDismiss: () -> Unit,
    onConfirm: (StoryCharacter) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Protagonist") }
    var backstory by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Character to Lore Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Character Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role (Protagonist, Antagonist, Mentor...)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = backstory,
                    onValueChange = { backstory = it },
                    label = { Text("Backstory & Personality") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(StoryCharacter(name = name, role = role, backstory = backstory))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Add Character", color = DeepDarkBg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun StoryEditModalSheet(
    initialText: String,
    onDismiss: () -> Unit,
    onApply: (StoryEditOperation, String) -> Unit
) {
    var selectedOp by remember { mutableStateOf(StoryEditOperation.IMPROVE_DESCRIPTIONS) }
    var param by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Prose Refinement & Inpainting", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Selected Operation:", fontSize = 12.sp, color = TextSecondary)
                StoryEditOperation.entries.take(5).forEach { op ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOp = op },
                        color = if (selectedOp == op) NeonPurple.copy(alpha = 0.2f) else GlassSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedOp == op) NeonPurple else CardBorder)
                    ) {
                        Text(
                            text = op.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedOp == op) NeonPurple else TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedOp, param) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Apply AI Edit", color = DeepDarkBg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ContinueStoryModalDialog(
    project: StoryProject,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Continue Story from Chapter", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Text("Select the chapter from which the AI should synthesize the next narrative sequence.", fontSize = 12.sp, color = TextSecondary)
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(project.activeChapterIndex) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Continue Chapter ${project.activeChapterIndex + 1}", color = DeepDarkBg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ExportManuscriptModalDialog(
    project: StoryProject,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Story Manuscript", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your manuscript \"${project.title}\" is ready to export across formats:", fontSize = 12.sp, color = TextSecondary)
                Text("• Markdown (.md) Manuscript\n• EPUB E-Book Format\n• Plain Text (.txt) Archive", fontSize = 12.sp, color = TextPrimary)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Download Manuscript", color = DeepDarkBg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}
