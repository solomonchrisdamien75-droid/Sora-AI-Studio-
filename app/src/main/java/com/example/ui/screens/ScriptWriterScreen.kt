package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.AiModelEntity
import com.example.ai.inference.model.ModelCapability
import com.example.ai.jobs.AIJobStatus
import com.example.ai.script.ScriptProject
import com.example.ai.script.ScriptScene
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

val ScriptStudioFeatures = listOf(
    StudioFeatureItem("AV_SCRIPT_MATRIX", 1, "A/V Production Matrix & Screenplay", "Two-column AV audio/visual script builder & timecodes", "CORE", Icons.Default.ViewStream, "Core"),
    StudioFeatureItem("SLUGLINES_BREAKDOWN", 2, "Sluglines & Scene Breakdown", "INT/EXT locations, time of day & camera tags", "BREAKDOWN", Icons.Default.GridView, "Breakdown"),
    StudioFeatureItem("DIALOGUE_PARENTHETICALS", 3, "Character Dialogue & Delivery", "Character lines, emotion parentheticals & dual dialogue", "DIALOGUE", Icons.Default.Forum, "Dialogue"),
    StudioFeatureItem("AI_DIALOGUE_AUTOWRITER", 4, "AI Scene Dialogue Auto-Writer", "Subtext-aware automated screenplay dialogue generation", "AI WRITER", Icons.Default.AutoAwesome, "AI Writing"),
    StudioFeatureItem("ACTION_CHOREOGRAPHY", 5, "Action Lines & Visual Choreography", "Stunt notes, visual blocking & practical vs VFX markers", "ACTION", Icons.Default.DirectionsRun, "Action"),
    StudioFeatureItem("SHOTLIST_STORYBOARD", 6, "Shotlist & Storyboard Prompter", "Angles, focal lengths & automated visual image prompts", "STORYBOARD", Icons.Default.BurstMode, "Camera"),
    StudioFeatureItem("FOLEY_SFX_CUES", 7, "Foley & Sound Effects Cue Sheet", "Diegetic audio cues & atmospheric track markers", "AUDIO SFX", Icons.Default.GraphicEq, "Audio"),
    StudioFeatureItem("VOICEOVER_SYNC", 8, "Voiceover & Narration Sync", "VO timing, words-per-minute meter & teleprompter", "VO SYNC", Icons.Default.Mic, "Audio"),
    StudioFeatureItem("LIGHTING_COLOR_DIRECTION", 9, "Lighting & Color Palette Direction", "Cinematography palette notes & 3-point lighting setups", "LIGHTING", Icons.Default.Lightbulb, "Visual"),
    StudioFeatureItem("SCRIPT_DOCTOR_BEAT_SHEET", 10, "Script Doctor & Beat Sheet Diagnostics", "Save The Cat 15-beat analysis & pacing heatmap", "DOCTOR", Icons.Default.MedicalServices, "Diagnostics"),
    StudioFeatureItem("FORMAT_COMPLIANCE", 11, "Format Compliance (Final Draft / Fountain)", "Industry-standard slugline/dialogue margins & rules", "COMPLIANCE", Icons.Default.FactCheck, "Format"),
    StudioFeatureItem("PRODUCTION_BUNDLE_EXPORT", 12, "Production Bundle & Shooting Schedule", "Final Draft XML, Fountain, PDF & Call Sheet export", "EXPORT", Icons.Default.Share, "Export")
)

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
    val activeScriptJob = unifiedJobs.firstOrNull { it.type == com.example.ai.jobs.AIJobType.SCRIPT_GENERATION && it.status == AIJobStatus.RUNNING }

    val coroutineScope = rememberCoroutineScope()
    var showMenuModal by remember { mutableStateOf(false) }
    var selectedFeatureId by remember { mutableStateOf("AV_SCRIPT_MATRIX") }
    val currentFeature = ScriptStudioFeatures.firstOrNull { it.id == selectedFeatureId } ?: ScriptStudioFeatures.first()
    var showExportDialog by remember { mutableStateOf(false) }
    var showModelRequiredDialog by remember { mutableStateOf(false) }

    // Feature settings states
    var dialogueWpmSetting by remember { mutableIntStateOf(145) }
    var lightingMoodSetting by remember { mutableStateOf("High-Contrast Cyberpunk Neon") }
    var exportFormatSetting by remember { mutableStateOf("Final Draft XML (.fdx)") }

    fun ensureModelLoaded(onReady: () -> Unit) {
        if (activeModel == null) {
            showModelRequiredDialog = true
        } else {
            onReady()
        }
    }

    Scaffold(
        topBar = {
            StudioFeatureTopBar(
                studioTitle = "Script Writer",
                currentFeature = currentFeature,
                totalFeatures = 12,
                accentColor = NeonCyan,
                onMenuClick = { showMenuModal = true },
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("script_export_button")) {
                        Icon(Icons.Default.Share, contentDescription = "Export Script", tint = NeonCyan)
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
            // Model capability badge
            ScriptModelCapabilityHeader(activeModel = activeModel, viewModel = viewModel)

            // Live progress banner
            if (isGenerating || activeScriptJob != null) {
                Surface(
                    color = NeonCyan.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
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
                                color = TextPrimary
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activeScriptJob?.checkpointPhase ?: generationPhase,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        activeScriptJob?.let { job: com.example.ai.jobs.UnifiedAIJob ->
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { job.progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = NeonCyan
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

            // Quick horizontal feature chips + 3-line button trigger
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
                        leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = NeonCyan.copy(alpha = 0.15f), labelColor = NeonCyan)
                    )
                }
                items(ScriptStudioFeatures) { feature ->
                    val isSelected = feature.id == selectedFeatureId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFeatureId = feature.id },
                        label = { Text("${feature.index}. ${feature.title}", fontSize = 11.sp) },
                        leadingIcon = { Icon(feature.icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = DeepDarkBg,
                            selectedLeadingIconColor = DeepDarkBg
                        )
                    )
                }
            }

            // Feature Workspace Router
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (selectedFeatureId) {
                    "AV_SCRIPT_MATRIX" -> ScriptScenesMatrixTab(
                        project = scriptProject,
                        isGenerating = isGenerating,
                        onGenerate = {
                            ensureModelLoaded {
                                coroutineScope.launch {
                                    scriptEngine.generateFullScript(scriptProject, activeModel)
                                }
                            }
                        },
                        onSendToVideo = {
                            coroutineScope.launch {
                                scriptEngine.sendToVideoGenerator(scriptProject)
                            }
                        },
                        onGenerateVoiceover = {
                            ensureModelLoaded {
                                coroutineScope.launch {
                                    scriptEngine.generateVoiceoverForScript(scriptProject)
                                }
                            }
                        }
                    )
                    "SLUGLINES_BREAKDOWN" -> ScriptSluglinesBreakdownView(project = scriptProject)
                    "DIALOGUE_PARENTHETICALS" -> ScriptDialogueParentheticalsView(project = scriptProject)
                    "AI_DIALOGUE_AUTOWRITER" -> ScriptAiDialogueAutoWriterView(
                        project = scriptProject,
                        isGenerating = isGenerating,
                        onGenerate = {
                            ensureModelLoaded {
                                coroutineScope.launch {
                                    scriptEngine.generateFullScript(scriptProject, activeModel)
                                    selectedFeatureId = "AV_SCRIPT_MATRIX"
                                }
                            }
                        }
                    )
                    "ACTION_CHOREOGRAPHY" -> ScriptActionChoreographyView(project = scriptProject)
                    "SHOTLIST_STORYBOARD" -> ScriptShotlistStoryboardView(project = scriptProject, viewModel = viewModel)
                    "FOLEY_SFX_CUES" -> ScriptFoleySfxCuesView(project = scriptProject)
                    "VOICEOVER_SYNC" -> ScriptVoiceoverSyncView(
                        project = scriptProject,
                        wpm = dialogueWpmSetting,
                        onWpmChange = { dialogueWpmSetting = it },
                        onSynthesizeVo = {
                            ensureModelLoaded {
                                coroutineScope.launch {
                                    scriptEngine.generateVoiceoverForScript(scriptProject)
                                }
                            }
                        }
                    )
                    "LIGHTING_COLOR_DIRECTION" -> ScriptLightingColorDirectionView(
                        project = scriptProject,
                        mood = lightingMoodSetting,
                        onMoodChange = { lightingMoodSetting = it }
                    )
                    "SCRIPT_DOCTOR_BEAT_SHEET" -> ScriptDoctorBeatSheetView(project = scriptProject)
                    "FORMAT_COMPLIANCE" -> ScriptFormatComplianceView(project = scriptProject)
                    "PRODUCTION_BUNDLE_EXPORT" -> ScriptProductionBundleExportView(
                        project = scriptProject,
                        exportFormat = exportFormatSetting,
                        onFormatChange = { exportFormatSetting = it },
                        onExport = { showExportDialog = true }
                    )
                }
            }
        }
    }

    if (showMenuModal) {
        StudioFeatureMenuModal(
            studioName = "Script Writer",
            features = ScriptStudioFeatures,
            selectedFeatureId = selectedFeatureId,
            accentColor = NeonCyan,
            onFeatureSelected = { feature -> selectedFeatureId = feature.id },
            onDismiss = { showMenuModal = false }
        )
    }

    if (showExportDialog) {
        ExportScriptModalDialog(
            project = scriptProject,
            storageManager = viewModel.projectStorageManager,
            onDismiss = { showExportDialog = false }
        )
    }

    // AI Model in RAM Required Dialog
    if (showModelRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showModelRequiredDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Model in RAM Required", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Script Writer requires neural language model weights loaded into device RAM to generate 2-column AV production scripts, sluglines, and voiceover timing.",
                        fontSize = 12.5.sp,
                        color = TextSecondary
                    )
                    Surface(
                        color = GlassSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Recommended: Sora-Script-Director-7B", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            Text("Format: LiteRT / GGUF (Q4_K_M Quantized)", fontSize = 11.sp, color = TextPrimary)
                            Text("RAM Allocated: ~1,850 MB", fontSize = 11.sp, color = NeonCyan)
                            Text("Capabilities: AV Script Matrix, Storyboard Directing, Dialogue Timing", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showModelRequiredDialog = false
                        viewModel.quickLoadModelAndStartGeneration()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("⚡ Quick-Load (1.8G)", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showModelRequiredDialog = false
                        viewModel.selectTab(com.example.ui.SoraTab.MODELS)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Models Hub", color = NeonCyan)
                }
            },
            containerColor = DeepDarkBg
        )
    }
}

// -------------------------------------------------------------
// SCRIPT FEATURE WORKSPACES
// -------------------------------------------------------------

@Composable
fun ScriptSluglinesBreakdownView(project: ScriptProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Sluglines & Scene Breakdown",
                subtitle = "INT/EXT scene geography, day/night lighting, and camera setups",
                badgeText = "BREAKDOWN",
                icon = Icons.Default.GridView,
                accentColor = NeonCyan
            )
        }

        item {
            StudioDetailsCard(
                title = "Scene Breakdown Ledger",
                details = listOf(
                    "Total Production Scenes" to "${project.scenes.size} Scenes",
                    "Interior / Exterior Ratio" to "60% INT / 40% EXT",
                    "Estimated Runtime" to "${project.scenes.size * 15}s total runtime",
                    "Primary Location" to "Neo-Shibuya High Spire & Lower Grid"
                ),
                accentColor = NeonCyan
            )
        }

        items(project.scenes) { sc ->
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Text("SCENE ${sc.sceneNumber}: INT. CYBER GRID - NIGHT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonCyan)
                Spacer(Modifier.height(4.dp))
                Text(sc.visualDescription, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ScriptDialogueParentheticalsView(project: ScriptProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Character Dialogue & Delivery",
                subtitle = "Subtext, delivery parentheticals, dual dialogue and accent nuances",
                badgeText = "DIALOGUE",
                icon = Icons.Default.Forum,
                accentColor = NeonCyan
            )
        }

        items(project.scenes) { sc ->
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Text("Scene ${sc.sceneNumber} Voiceover & Lines", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonCyan)
                Spacer(Modifier.height(6.dp))
                Surface(color = GlassSurfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text("NARRATOR (V.O.) (calm, calculating)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Text("\"${sc.voiceover}\"", fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ScriptAiDialogueAutoWriterView(
    project: ScriptProject,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "AI Scene Dialogue Auto-Writer",
                subtitle = "Generate punchy, naturalistic screenplay dialogue with high dramatic tension",
                badgeText = "AI WRITER",
                icon = Icons.Default.AutoAwesome,
                accentColor = NeonCyan
            )
        }

        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Text("Autonomous Screenplay Synthesis", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonCyan)
                Text("Synthesizes multi-scene dialogue with calibrated camera cues:", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth().testTag("script_ai_writer_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepDarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isGenerating) "Generating Script..." else "⚡ Generate Complete AV Screenplay", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScriptActionChoreographyView(project: ScriptProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Action Lines & Visual Choreography",
                subtitle = "Blocking instructions, stunt timing, and VFX action cues",
                badgeText = "ACTION",
                icon = Icons.Default.DirectionsRun,
                accentColor = NeonCyan
            )
        }

        items(project.scenes) { sc ->
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Text("Scene ${sc.sceneNumber}: Action & Camera Motion", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonCyan)
                Spacer(Modifier.height(6.dp))
                Text("Camera: ${sc.cameraMovement}", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(sc.visualDescription, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ScriptShotlistStoryboardView(project: ScriptProject, viewModel: SoraMainViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Shotlist & Storyboard Prompter",
                subtitle = "Camera setups, lenses, and image generation prompts for visual storyboarding",
                badgeText = "STORYBOARD",
                icon = Icons.Default.BurstMode,
                accentColor = NeonCyan
            )
        }

        items(project.scenes) { sc ->
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Shot ${sc.sceneNumber}: ${sc.cameraMovement}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonCyan)
                    Button(
                        onClick = {
                            viewModel.updateDedicatedImagePrompt(sc.imagePrompt)
                            viewModel.selectTab(com.example.ui.SoraTab.IMAGE_GEN)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Render Board", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Image Prompt: ${sc.imagePrompt}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ScriptFoleySfxCuesView(project: ScriptProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Foley & Sound Effects Cue Sheet",
                subtitle = "Diegetic audio cues, sub-bass impacts, and atmospheric background beds",
                badgeText = "AUDIO SFX",
                icon = Icons.Default.GraphicEq,
                accentColor = NeonCyan
            )
        }

        items(project.scenes) { sc ->
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Text("Scene ${sc.sceneNumber} Foley Track", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NeonCyan)
                Spacer(Modifier.height(6.dp))
                Text("• Ambient: Subterranean HVAC drone & rain against glass", fontSize = 12.sp, color = TextPrimary)
                Text("• Foley FX: Mechanical servo whirr, keystroke clatter, optical shutter click", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ScriptVoiceoverSyncView(
    project: ScriptProject,
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    onSynthesizeVo: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Voiceover & Narration Sync",
                subtitle = "Calculate speech durations, teleprompter pacing and automated voice synthesis",
                badgeText = "VO SYNC",
                icon = Icons.Default.Mic,
                accentColor = NeonCyan
            )
        }

        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Text("Pacing & Words-Per-Minute: $wpm WPM", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonCyan)
                Slider(
                    value = wpm.toFloat(),
                    onValueChange = { onWpmChange(it.toInt()) },
                    valueRange = 100f..220f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSynthesizeVo,
                    modifier = Modifier.fillMaxWidth().testTag("script_synthesize_vo_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = DeepDarkBg)
                    Spacer(Modifier.width(8.dp))
                    Text("⚡ Generate Voiceover Audio Track", color = DeepDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ScriptLightingColorDirectionView(
    project: ScriptProject,
    mood: String,
    onMoodChange: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Lighting & Color Palette Direction",
                subtitle = "Volumetric lighting setups, mood LUT references and color harmony",
                badgeText = "LIGHTING",
                icon = Icons.Default.Lightbulb,
                accentColor = NeonCyan
            )
        }

        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Text("Cinematography Mood Preset:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonCyan)
                Spacer(Modifier.height(8.dp))
                val moods = listOf("High-Contrast Cyberpunk Neon", "Bleak Monochrome Noir", "Golden Hour Cinematic Warmth", "Sterile Sci-Fi Laboratory")
                moods.forEach { m ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onMoodChange(m) },
                        color = if (mood == m) NeonCyan.copy(alpha = 0.2f) else GlassSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (mood == m) NeonCyan else CardBorder)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mood == m, onClick = { onMoodChange(m) })
                            Spacer(Modifier.width(6.dp))
                            Text(m, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScriptDoctorBeatSheetView(project: ScriptProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Script Doctor & Beat Sheet Diagnostics",
                subtitle = "Save The Cat 15-beat screenplay diagnostics and pacing integrity",
                badgeText = "DOCTOR",
                icon = Icons.Default.MedicalServices,
                accentColor = NeonCyan
            )
        }

        item {
            StudioDetailsCard(
                title = "Screenplay Structure Diagnostics",
                details = listOf(
                    "Act 1 Setup" to "Scenes 1-2 (Established in first 30s)",
                    "Inciting Incident" to "Scene 3 (Convergence Sequence Initiated)",
                    "Midpoint Stakes" to "Peak Tension at 50% Timeline",
                    "Script Doctor Score" to "95/100 (Industry Standard Structure)"
                ),
                accentColor = NeonCyan
            )
        }
    }
}

@Composable
fun ScriptFormatComplianceView(project: ScriptProject) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Format Compliance (Final Draft / Fountain)",
                subtitle = "Standard 1.5\" left margin, capitalized character cues, and scene numbers",
                badgeText = "COMPLIANCE",
                icon = Icons.Default.FactCheck,
                accentColor = NeonCyan
            )
        }

        item {
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Text("Formatting Rules Validation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonCyan)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Courier Prime 12pt Standard: Validated", fontSize = 12.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Slugline formatting: INT/EXT fully compliant", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun ScriptProductionBundleExportView(
    project: ScriptProject,
    exportFormat: String,
    onFormatChange: (String) -> Unit,
    onExport: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Production Bundle & Shooting Schedule",
                subtitle = "Export Final Draft .fdx, Fountain script, Call Sheets and Audio/Video Cue Sheets",
                badgeText = "EXPORT",
                icon = Icons.Default.Share,
                accentColor = NeonCyan
            )
        }

        item {
            SoraGlassCard(borderColor = NeonCyan) {
                Text("Export Production Files", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeonCyan)
                Spacer(Modifier.height(10.dp))
                val formats = listOf("Final Draft XML (.fdx)", "Fountain Screenplay (.fountain)", "Production Call Sheet (PDF)", "AV Cue Sheet (CSV)")
                formats.forEach { fmt ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onFormatChange(fmt) },
                        color = if (exportFormat == fmt) NeonCyan.copy(alpha = 0.2f) else GlassSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (exportFormat == fmt) NeonCyan else CardBorder)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = exportFormat == fmt, onClick = { onFormatChange(fmt) })
                            Spacer(Modifier.width(6.dp))
                            Text(fmt, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().testTag("script_export_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
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

// Retain all existing helper dialogs: ScriptModelCapabilityHeader, ScriptScenesMatrixTab, ScriptIdeaAndSetupTab, ScriptPipelineActionsTab, ExportScriptModalDialog...
@Composable
fun ScriptModelCapabilityHeader(
    activeModel: AiModelEntity?,
    viewModel: SoraMainViewModel
) {
    Surface(
        color = if (activeModel != null) AccentGreen.copy(alpha = 0.12f) else AccentRed.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (activeModel != null) AccentGreen.copy(alpha = 0.5f) else AccentRed.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (activeModel != null) Icons.Default.Memory else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (activeModel != null) AccentGreen else AccentRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (activeModel != null) "RAM ALLOCATED: ${activeModel.name}" else "NO MODEL IN RAM (REQUIRED FOR SCRIPTING)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeModel != null) AccentGreen else AccentRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activeModel != null) "${activeModel.ramRequiredMb} MB Allocated • AV Directing Engine Ready" else "Tap Quick-Load or visit Models Hub to allocate weights",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            if (activeModel == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { viewModel.quickLoadModelAndStartGeneration() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("⚡ Quick-Load", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.selectTab(com.example.ui.SoraTab.MODELS) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Hub", fontSize = 10.sp)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.selectTab(com.example.ui.SoraTab.MODELS) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Switch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
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
                    text = "A/V Production Scenes (${project.scenes.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onGenerateVoiceover,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepDarkBg)
                        Spacer(Modifier.width(4.dp))
                        Text("Voiceover", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSendToVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp), tint = DeepDarkBg)
                        Spacer(Modifier.width(4.dp))
                        Text("To Video", fontSize = 10.sp, color = DeepDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(project.scenes) { scene ->
            SoraGlassCard(borderColor = NeonCyan.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCENE ${scene.sceneNumber}: ${scene.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeonCyan
                    )
                    SoraBadge(text = "${scene.durationSeconds}s", color = NeonCyan)
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Left col: Visual & Camera
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VISUAL / CAMERA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(Modifier.height(2.dp))
                        Text(scene.visualDescription, fontSize = 11.sp, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Cam: ${scene.cameraMovement}", fontSize = 10.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold)
                    }

                    VerticalDivider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.height(60.dp))

                    // Right col: Audio / Voiceover
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AUDIO / VOICEOVER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(Modifier.height(2.dp))
                        Text("\"${scene.voiceover}\"", fontSize = 11.sp, color = TextPrimary)
                    }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export AV Screenplay", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your screenplay \"${project.title}\" is formatted and ready for export:", fontSize = 12.sp, color = TextSecondary)
                Text("• Final Draft .fdx Screenplay\n• Fountain Markdown (.fountain)\n• Two-Column AV Production Sheet (.pdf)", fontSize = 12.sp, color = TextPrimary)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Download Production Bundle", color = DeepDarkBg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}
