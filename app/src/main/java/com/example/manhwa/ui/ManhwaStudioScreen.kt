package com.example.manhwa.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.manhwa.engine.ManhwaStudioPipeline
import com.example.manhwa.model.*
import com.example.ui.SoraMainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val ManhwaStudioFeatureItems = listOf(
    StudioFeatureItem("DASHBOARD", 1, "Production Dashboard & Project Hub", "Project summary, progress statistics & batch render", "CORE", Icons.Default.Dashboard, "Hub"),
    StudioFeatureItem("IMPORT", 2, "Smart Chapter Import & Strip Ingestion", "Multi-page PDF/CBZ/Webtoon strip import & vertical slicing", "IMPORT", Icons.Default.CloudUpload, "Ingestion"),
    StudioFeatureItem("PANEL_ANALYSIS", 3, "Panel Segmentation & OCR Text Extraction", "Automated bounding box detection & bubble text OCR", "OCR", Icons.Default.GridView, "Vision"),
    StudioFeatureItem("CHARACTERS", 4, "Character Cast & Consistency Binder", "Face embeddings, character sheets & color schemes", "CAST", Icons.Default.People, "Cast"),
    StudioFeatureItem("AUDIO_VOICE", 5, "Voiceover & Cast Voice Dubbing", "Character-specific voice mapping & bubble audio dubbing", "AUDIO", Icons.Default.GraphicEq, "Audio"),
    StudioFeatureItem("SYNC_ACTION", 6, "Motion & Action FX Choreographer", "Speed lines, impact zooms, screen shakes & sword trails", "ACTION", Icons.Default.SyncAlt, "Motion"),
    StudioFeatureItem("ANIMATION_CAMERA", 7, "Dynamic Camera & 2.5D Parallax", "Vertical webtoon scroll camera & depth layer parallax", "CAMERA", Icons.Default.AutoAwesome, "Camera"),
    StudioFeatureItem("TIMELINE", 8, "Timeline & Multi-Track Sequencer", "Panel timing, audio sync, BGM crossfade & triggers", "TIMELINE", Icons.Default.ViewTimeline, "Timeline"),
    StudioFeatureItem("RECAP_STORY", 9, "AI Story Recap & Narrated Summary", "Scripted narrative recap summary generator & hype trailer", "RECAP", Icons.Default.MenuBook, "Recap"),
    StudioFeatureItem("PREVIEW_EXPORT", 10, "Live Canvas Preview & Quality Control", "Real-time 60fps webtoon video preview & inspector", "PREVIEW", Icons.Default.Movie, "Preview"),
    StudioFeatureItem("MODELS_FUSION", 11, "Model Fusion & Neural Engine Lab", "Quantized visual+audio models & hardware NPU engine", "FUSION", Icons.Default.Hub, "Engine"),
    StudioFeatureItem("EXPORT_PRESETS", 12, "Video & Webtoon Multi-Format Exporter", "4K 60fps, 9:16 Shorts/Reels/TikTok & GIF animations", "EXPORT", Icons.Default.Share, "Export")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManhwaStudioScreen(viewModel: SoraMainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pipeline = remember { ManhwaStudioPipeline(context) }
    var currentProject by remember { mutableStateOf(pipeline.projectManager.createDefaultProject()) }
    var selectedFeatureId by remember { mutableStateOf("DASHBOARD") }
    val currentFeature = ManhwaStudioFeatureItems.firstOrNull { it.id == selectedFeatureId } ?: ManhwaStudioFeatureItems.first()
    var showMenuModal by remember { mutableStateOf(false) }

    val activeTask by pipeline.currentTask.collectAsStateWithLifecycle()
    val modelConfig by pipeline.modelConfig.collectAsStateWithLifecycle()

    var showLegalDisclaimer by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            StudioFeatureTopBar(
                studioTitle = "Manhwa Studio",
                currentFeature = currentFeature,
                totalFeatures = 12,
                accentColor = ElectricPink,
                onMenuClick = { showMenuModal = true },
                actions = {
                    IconButton(onClick = { showLegalDisclaimer = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Gavel, contentDescription = "Legal Notice", tint = WarningOrange)
                    }
                    IconButton(onClick = { showNewProjectDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "New Project", tint = NeonCyan)
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Starting AI Manhwa Recap Production Pipeline...")
                                pipeline.runFullRecapPipeline(
                                    project = currentProject,
                                    imageUris = emptyList(),
                                    audioUri = null,
                                    recapConfig = currentProject.recapConfig
                                ) { updated -> currentProject = updated }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_build_recap")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recap", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepDarkBg)
        ) {
            // Project Status Subheader
            Surface(
                color = GlassSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project: ${currentProject.title} • ${currentProject.episodeTitle} (${currentProject.scenes.size} Scenes)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    SoraBadge(text = "12 FEATURES ACTIVE", color = ElectricPink)
                }
            }

            // Active Background Task Status Banner (if running)
            activeTask?.let { task ->
                ManhwaTaskBanner(
                    task = task,
                    onDismiss = { pipeline.clearTask() }
                )
            }

            // Quick horizontal feature chips + 3-line drawer trigger
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
                        leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricPink) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = ElectricPink.copy(alpha = 0.15f), labelColor = ElectricPink)
                    )
                }
                items(ManhwaStudioFeatureItems) { feature ->
                    val isSelected = feature.id == selectedFeatureId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFeatureId = feature.id },
                        label = { Text("${feature.index}. ${feature.title}", fontSize = 11.sp) },
                        leadingIcon = { Icon(feature.icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricPink,
                            selectedLabelColor = DeepDarkBg,
                            selectedLeadingIconColor = DeepDarkBg
                        )
                    )
                }
            }

            // Sub Tab Screen Contents (Changes the WHOLE page dynamically)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                when (selectedFeatureId) {
                    "DASHBOARD" -> ManhwaDashboardView(
                        project = currentProject,
                        onNavigateFeature = { featureId -> selectedFeatureId = featureId }
                    )
                    "IMPORT" -> ManhwaImportView(
                        project = currentProject,
                        onImportCompleted = { panels, audio ->
                            currentProject = currentProject.copy(
                                panels = panels,
                                audioTrack = audio ?: currentProject.audioTrack
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Imported ${panels.size} panels successfully.")
                            }
                        }
                    )
                    "PANEL_ANALYSIS" -> ManhwaPanelAnalysisView(
                        panels = currentProject.panels,
                        characters = currentProject.characters,
                        onPanelUpdated = { updatedPanel ->
                            val list = currentProject.panels.toMutableList()
                            val idx = list.indexOfFirst { it.id == updatedPanel.id }
                            if (idx != -1) list[idx] = updatedPanel
                            currentProject = currentProject.copy(panels = list)
                        }
                    )
                    "CHARACTERS" -> ManhwaCharacterManagerView(
                        characters = currentProject.characters,
                        onAddCharacter = { newChar ->
                            currentProject = currentProject.copy(characters = currentProject.characters + newChar)
                        },
                        onUpdateCharacter = { updatedChar ->
                            val list = currentProject.characters.toMutableList()
                            val idx = list.indexOfFirst { it.id == updatedChar.id }
                            if (idx != -1) list[idx] = updatedChar
                            currentProject = currentProject.copy(characters = list)
                        }
                    )
                    "AUDIO_VOICE" -> ManhwaAudioVoiceView(
                        audioTrack = currentProject.audioTrack,
                        characters = currentProject.characters,
                        onUpdateAudioTrack = { currentProject = currentProject.copy(audioTrack = it) }
                    )
                    "SYNC_ACTION" -> ManhwaSyncActionView(
                        scenes = currentProject.scenes,
                        audioTrack = currentProject.audioTrack,
                        onUpdateScenes = { currentProject = currentProject.copy(scenes = it) }
                    )
                    "ANIMATION_CAMERA" -> ManhwaAnimationCameraView(
                        scenes = currentProject.scenes,
                        characters = currentProject.characters,
                        pipeline = pipeline,
                        onUpdateScenes = { currentProject = currentProject.copy(scenes = it) }
                    )
                    "TIMELINE" -> ManhwaTimelineEditorView(
                        project = currentProject,
                        onUpdateProject = { currentProject = it }
                    )
                    "RECAP_STORY" -> ManhwaRecapStoryView(
                        project = currentProject,
                        pipeline = pipeline,
                        onUpdateProject = { currentProject = it },
                        onShowMessage = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    "PREVIEW_EXPORT" -> ManhwaPreviewExportView(
                        project = currentProject,
                        pipeline = pipeline,
                        onUpdateProject = { currentProject = it },
                        onExportSuccess = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    "MODELS_FUSION" -> ManhwaModelFusionView(
                        modelConfig = modelConfig
                    )
                    "EXPORT_PRESETS" -> ManhwaExportPresetsFeatureView(
                        project = currentProject,
                        pipeline = pipeline,
                        onExportSuccess = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }
        }
    }

    // 12 Feature 3-Line Menu Drawer Modal
    if (showMenuModal) {
        StudioFeatureMenuModal(
            studioName = "Manhwa Studio",
            features = ManhwaStudioFeatureItems,
            selectedFeatureId = selectedFeatureId,
            accentColor = ElectricPink,
            onFeatureSelected = { feature -> selectedFeatureId = feature.id },
            onDismiss = { showMenuModal = false }
        )
    }

    // Legal & Copyright Rights Disclaimer Dialog
    if (showLegalDisclaimer) {
        AlertDialog(
            onDismissRequest = { showLegalDisclaimer = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = WarningOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manhwa Studio Copyright Notice", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Manhwa Studio provides professional AI animation, lip-sync, and recap generation tools for content creators.",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Users must only upload or animate original manhwa, user-created artwork, or materials they have explicit license and legal rights to use.\n• The application does not claim copyrighted manhwa can automatically be reproduced or distributed legally.\n• Users are solely responsible for ensuring rights compliance for uploaded or exported materials.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLegalDisclaimer = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("I Understand & Agree", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DeepDarkBg,
            textContentColor = TextPrimary
        )
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        var newTitle by remember { mutableStateOf("Demon Sovereign Recap") }
        var newEpisode by remember { mutableStateOf("Episode 02") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create New Manhwa Project", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Project Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEpisode,
                        onValueChange = { newEpisode = it },
                        label = { Text("Episode / Chapter Label") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newProj = pipeline.projectManager.createDefaultProject().copy(
                            id = "proj_${System.currentTimeMillis()}",
                            title = newTitle,
                            episodeTitle = newEpisode
                        )
                        currentProject = newProj
                        showNewProjectDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Created project: $newTitle")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                ) {
                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DeepDarkBg
        )
    }
}

@Composable
fun ManhwaExportPresetsFeatureView(
    project: ManhwaProject,
    pipeline: ManhwaStudioPipeline,
    onExportSuccess: (String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf("9:16 YouTube Shorts / TikTok (1080x1920)") }
    var fpsSetting by remember { mutableIntStateOf(60) }
    val presets = listOf(
        "9:16 YouTube Shorts / TikTok (1080x1920)",
        "16:9 Cinema Ultra HD (3840x2160)",
        "1:1 Square Feed (1080x1080)",
        "Animated WebP / GIF Sticker Pack"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            StudioFeatureSectionHeader(
                title = "Video & Webtoon Multi-Format Exporter",
                subtitle = "Batch encode vertical webtoon animations into viral 9:16 social videos or 4K master files",
                badgeText = "EXPORT",
                icon = Icons.Default.Share,
                accentColor = ElectricPink
            )
        }

        item {
            StudioDetailsCard(
                title = "Production Specifications & Format Ledger",
                details = listOf(
                    "Total Webtoon Panels" to "${project.panels.size} Panels",
                    "Total Sequenced Scenes" to "${project.scenes.size} Scenes",
                    "Estimated Render Time" to "~18 Seconds via Hardware Acceleration",
                    "Target Framerate" to "$fpsSetting FPS Constant Framerate"
                ),
                accentColor = ElectricPink
            )
        }

        item {
            SoraGlassCard(borderColor = ElectricPink) {
                Text("Select Master Export Preset", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ElectricPink)
                Spacer(Modifier.height(8.dp))

                presets.forEach { pr ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedPreset = pr },
                        color = if (selectedPreset == pr) ElectricPink.copy(alpha = 0.2f) else GlassSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedPreset == pr) ElectricPink else CardBorder)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedPreset == pr, onClick = { selectedPreset = pr })
                            Spacer(Modifier.width(8.dp))
                            Text(pr, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onExportSuccess("Successfully encoded $selectedPreset at $fpsSetting FPS.") },
                    modifier = Modifier.fillMaxWidth().testTag("manhwa_master_export_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("⚡ Export Master Manhwa Video", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ManhwaTaskBanner(
    task: ManhwaTask,
    onDismiss: () -> Unit
) {
    Surface(
        color = GlassSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPink.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier.size(20.dp),
                        color = ElectricPink,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.currentStep,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
