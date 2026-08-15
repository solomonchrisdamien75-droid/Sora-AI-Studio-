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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

enum class ManhwaSubTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    IMPORT("Import", Icons.Default.CloudUpload),
    PANEL_ANALYSIS("Panels & OCR", Icons.Default.GridView),
    CHARACTERS("Characters", Icons.Default.People),
    AUDIO_VOICE("Audio & VAD", Icons.Default.GraphicEq),
    SYNC_ACTION("Sync & Action", Icons.Default.SyncAlt),
    ANIMATION_CAMERA("Animation", Icons.Default.AutoAwesome),
    TIMELINE("Timeline", Icons.Default.ViewTimeline),
    RECAP_STORY("Recap & Story", Icons.Default.MenuBook),
    PREVIEW_EXPORT("Preview & QC", Icons.Default.Movie),
    MODELS_FUSION("Model Fusion", Icons.Default.Hub)
}

@Composable
fun ManhwaStudioScreen(viewModel: SoraMainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pipeline = remember { ManhwaStudioPipeline(context) }
    var currentProject by remember { mutableStateOf(pipeline.projectManager.createDefaultProject()) }
    var selectedSubTab by remember { mutableStateOf(ManhwaSubTab.DASHBOARD) }

    val activeTask by pipeline.currentTask.collectAsStateWithLifecycle()
    val modelConfig by pipeline.modelConfig.collectAsStateWithLifecycle()

    var showLegalDisclaimer by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepDarkBg)
        ) {
            // Header Bar
            ManhwaStudioHeader(
                project = currentProject,
                onNewProjectClick = { showNewProjectDialog = true },
                onLegalInfoClick = { showLegalDisclaimer = true },
                onQuickRecapClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Starting AI Manhwa Recap Production Pipeline...")
                        pipeline.runFullRecapPipeline(
                            project = currentProject,
                            imageUris = emptyList(),
                            audioUri = null,
                            recapConfig = currentProject.recapConfig
                        ) { updated -> currentProject = updated }
                    }
                }
            )

            // Active Background Task Status Banner (if running)
            activeTask?.let { task ->
                ManhwaTaskBanner(
                    task = task,
                    onDismiss = { pipeline.clearTask() }
                )
            }

            // Sub Navigation Tab Bar
            ScrollableTabRow(
                selectedTabIndex = selectedSubTab.ordinal,
                containerColor = GlassSurface,
                contentColor = NeonCyan,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty() && selectedSubTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab.ordinal]),
                            height = 3.dp,
                            color = ElectricPink
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                ManhwaSubTab.entries.forEach { tab ->
                    val isSelected = tab == selectedSubTab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSubTab = tab },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .testTag("manhwa_tab_${tab.name.lowercase()}"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) ElectricPink else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ElectricPink else TextSecondary
                                )
                            }
                        }
                    )
                }
            }

            // Sub Tab Screen Contents
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (selectedSubTab) {
                    ManhwaSubTab.DASHBOARD -> ManhwaDashboardView(
                        project = currentProject,
                        onNavigateTo = { selectedSubTab = it },
                        onStartPipeline = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Assembling Manhwa Recap...")
                                pipeline.runFullRecapPipeline(
                                    project = currentProject,
                                    imageUris = emptyList(),
                                    audioUri = null,
                                    recapConfig = currentProject.recapConfig
                                ) { updated -> currentProject = updated }
                            }
                        }
                    )
                    ManhwaSubTab.IMPORT -> ManhwaImportView(
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
                    ManhwaSubTab.PANEL_ANALYSIS -> ManhwaPanelAnalysisView(
                        panels = currentProject.panels,
                        characters = currentProject.characters,
                        onPanelUpdated = { updatedPanel ->
                            val list = currentProject.panels.toMutableList()
                            val idx = list.indexOfFirst { it.id == updatedPanel.id }
                            if (idx != -1) list[idx] = updatedPanel
                            currentProject = currentProject.copy(panels = list)
                        }
                    )
                    ManhwaSubTab.CHARACTERS -> ManhwaCharacterManagerView(
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
                    ManhwaSubTab.AUDIO_VOICE -> ManhwaAudioVoiceView(
                        audioTrack = currentProject.audioTrack,
                        characters = currentProject.characters,
                        onUpdateAudioTrack = { currentProject = currentProject.copy(audioTrack = it) }
                    )
                    ManhwaSubTab.SYNC_ACTION -> ManhwaSyncActionView(
                        scenes = currentProject.scenes,
                        audioTrack = currentProject.audioTrack,
                        onUpdateScenes = { currentProject = currentProject.copy(scenes = it) }
                    )
                    ManhwaSubTab.ANIMATION_CAMERA -> ManhwaAnimationCameraView(
                        scenes = currentProject.scenes,
                        characters = currentProject.characters,
                        pipeline = pipeline,
                        onUpdateScenes = { currentProject = currentProject.copy(scenes = it) }
                    )
                    ManhwaSubTab.TIMELINE -> ManhwaTimelineEditorView(
                        project = currentProject,
                        onUpdateProject = { currentProject = it }
                    )
                    ManhwaSubTab.RECAP_STORY -> ManhwaRecapStoryView(
                        project = currentProject,
                        pipeline = pipeline,
                        onUpdateProject = { currentProject = it },
                        onShowMessage = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    ManhwaSubTab.PREVIEW_EXPORT -> ManhwaPreviewExportView(
                        project = currentProject,
                        pipeline = pipeline,
                        onUpdateProject = { currentProject = it },
                        onExportSuccess = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                    ManhwaSubTab.MODELS_FUSION -> ManhwaModelFusionView(
                        modelConfig = modelConfig
                    )
                }
            }
        }
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
fun ManhwaStudioHeader(
    project: ManhwaProject,
    onNewProjectClick: () -> Unit,
    onLegalInfoClick: () -> Unit,
    onQuickRecapClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MANHWA STUDIO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ElectricPink,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                SoraBadge(text = "RECAP ENGINE", color = NeonCyan)
            }
            Text(
                text = "${project.title} • ${project.episodeTitle} (${project.scenes.size} scenes)",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLegalInfoClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Gavel, contentDescription = "Legal Notice", tint = WarningOrange)
            }
            IconButton(onClick = onNewProjectClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "New Project", tint = NeonCyan)
            }
            Button(
                onClick = onQuickRecapClick,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPink),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_build_recap")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Build Recap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ManhwaTaskBanner(
    task: ManhwaTask,
    onDismiss: () -> Unit
) {
    SoraGlassCard(
        borderColor = if (task.isCompleted) AccentGreen else ElectricPink,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = if (task.isCompleted) AccentGreen else ElectricPink
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = task.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RAM: ${task.ramUsageMb}MB • GPU: ${task.gpuUsagePercent}%",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = task.currentStep, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = ElectricPink,
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}

// -------------------------------------------------------------
// 1. DASHBOARD VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaDashboardView(
    project: ManhwaProject,
    onNavigateTo: (ManhwaSubTab) -> Unit,
    onStartPipeline: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Project Overview Card
        item {
            SoraGlassCard(borderColor = ElectricPink) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = project.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text(text = "${project.episodeTitle} • ${project.narrationStyle}", fontSize = 12.sp, color = NeonCyan)
                        }
                        SoraBadge(text = project.status.name, color = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = project.description, fontSize = 13.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge("Scenes", "${project.scenes.size} Active", Icons.Default.Movie)
                        MetricBadge("Panels", "${project.panels.size} Detected", Icons.Default.GridView)
                        MetricBadge("Duration", "${project.durationSeconds / 60}m ${project.durationSeconds % 60}s", Icons.Default.Timer)
                        MetricBadge("FPS", "${project.fps} FPS", Icons.Default.Speed)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartPipeline,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Full Pipeline", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onNavigateTo(ManhwaSubTab.PREVIEW_EXPORT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Canvas", color = NeonCyan)
                        }
                    }
                }
            }
        }

        // Quick Navigation Grid
        item {
            SoraSectionHeader(title = "Production Modules", subtitle = "Manhwa Studio 20-Step AI Architecture", icon = Icons.Default.Hub)
            Spacer(modifier = Modifier.height(8.dp))

            val modules = listOf(
                Triple("Import Manhwa", "SAF, Images, PDF, CBZ, ZIP", ManhwaSubTab.IMPORT),
                Triple("Analyze Panels", "OCR, Speech Bubbles, Bounds", ManhwaSubTab.PANEL_ANALYSIS),
                Triple("Characters", "Consistency Profiles & Voices", ManhwaSubTab.CHARACTERS),
                Triple("Audio & VAD", "Speech-to-Text & Diarization", ManhwaSubTab.AUDIO_VOICE),
                Triple("Sync & Action", "Action Audio Replacement", ManhwaSubTab.SYNC_ACTION),
                Triple("Animation Studio", "Parallax, LipSync, Speed Lines", ManhwaSubTab.ANIMATION_CAMERA),
                Triple("11-Track Timeline", "Multi-Track Video & SFX Editor", ManhwaSubTab.TIMELINE),
                Triple("Recap & Story", "YouTube Recap & Continuation", ManhwaSubTab.RECAP_STORY),
                Triple("Export & QC", "10-Point QC, MP4, SRT, VTT", ManhwaSubTab.PREVIEW_EXPORT),
                Triple("Model Fusion", "Composite Pipeline Hub", ManhwaSubTab.MODELS_FUSION)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modules.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (name, sub, tab) ->
                            SoraGlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateTo(tab) },
                                borderColor = GlassBorder
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(tab.icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(text = sub, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF131824))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, fontSize = 9.sp, color = TextSecondary)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
