package com.example.manhwa.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manhwa.engine.ManhwaStudioPipeline
import com.example.manhwa.engine.RecapScriptEngine
import com.example.manhwa.engine.StoryContinuationEngine
import com.example.manhwa.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 2. IMPORT MANHWA & AUDIO VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaImportView(
    project: ManhwaProject,
    onImportCompleted: (List<ManhwaPanel>, AudioTrack?) -> Unit
) {
    var importMode by remember { mutableStateOf("SINGLE_MULTI_IMAGE") } // SINGLE_MULTI_IMAGE, FOLDER_SAF, ARCHIVE_CBZ_ZIP, PDF_PAGES
    var storageSource by remember { mutableStateOf("INTERNAL") } // INTERNAL, SD_CARD, USB, CUSTOM_FOLDER
    var isCopyMode by remember { mutableStateOf(true) } // true: copy into project, false: reference original
    var selectedFileNames by remember { mutableStateOf(listOf("chapter_100_p01.png", "chapter_100_p02.png", "narration_voiceover.wav")) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // SAF File picker launcher for images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedFileNames = uris.map { it.lastPathSegment ?: "manhwa_page.jpg" }
        }
    }

    // SAF File picker launcher for audio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileNames = selectedFileNames + (uri.lastPathSegment ?: "voice_narration.mp3")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Import Manhwa & Audio Assets",
                subtitle = "Android Storage Access Framework (SAF) Integration",
                icon = Icons.Default.CloudUpload
            )
        }

        // Storage Source Selector
        item {
            SoraGlassCard {
                Column {
                    Text("Storage Source Location", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("INTERNAL", "Internal", Icons.Default.PhoneAndroid),
                            Triple("SD_CARD", "SD Card", Icons.Default.SdCard),
                            Triple("USB", "USB Storage", Icons.Default.Usb),
                            Triple("CUSTOM_FOLDER", "SAF Folder", Icons.Default.FolderOpen)
                        ).forEach { (id, label, icon) ->
                            val isSel = storageSource == id
                            OutlinedButton(
                                onClick = { storageSource = id },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) ElectricPink.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSel) ElectricPink else GlassBorder),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, contentDescription = null, tint = if (isSel) ElectricPink else TextSecondary, modifier = Modifier.size(16.dp))
                                    Text(label, fontSize = 10.sp, color = if (isSel) ElectricPink else TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Format Support Selector
        item {
            SoraGlassCard {
                Column {
                    Text("Supported Import Formats", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "PNG, JPG, JPEG, WEBP, PDF, CBZ, ZIP, MP3, WAV, M4A, AAC, OGG, FLAC",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch(arrayOf("image/*", "application/pdf", "application/zip", "application/x-cbz")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Pages / ZIP", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Voice Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Storage Mode Toggle (Copy vs Reference)
        item {
            SoraGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Project Storage Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            if (isCopyMode) "Copy files into project directory" else "Reference original files to save storage space",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isCopyMode,
                        onCheckedChange = { isCopyMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.4f))
                    )
                }
            }
        }

        // Selected Files Queue
        item {
            Text("Selected Import Queue (${selectedFileNames.size} files)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedFileNames.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111726))
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (name.endsWith(".wav") || name.endsWith(".mp3")) Icons.Default.Audiotrack else Icons.Default.Image,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontSize = 12.sp, color = TextPrimary)
                        }
                        SoraBadge(text = if (isCopyMode) "COPY" else "REF", color = NeonPurple)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. MANHWA PANEL ANALYSIS VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaPanelAnalysisView(
    panels: List<ManhwaPanel>,
    characters: List<ManhwaCharacter>,
    onPanelUpdated: (ManhwaPanel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Panel Analysis & OCR Extraction",
                subtitle = "Detected ${panels.size} panels with speech bubble classification",
                icon = Icons.Default.GridView
            )
        }

        items(panels) { panel ->
            SoraGlassCard(borderColor = ElectricPink) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(text = panel.id, color = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Panel ${panel.panelOrder} • ${panel.cameraFraming.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        SoraBadge(text = "${(panel.confidenceScore * 100).toInt()}% Conf", color = AccentGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Action: ${panel.actionDescription}", fontSize = 12.sp, color = TextSecondary)
                    Text("Environment: ${panel.environmentDescription}", fontSize = 11.sp, color = TextSecondary)
                    Text("Expression: ${panel.expressionSummary}", fontSize = 11.sp, color = ElectricPink)

                    // Bounding Box Specs
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val bbox = panel.boundingBox
                        SoraBadge(text = "Bounds: (${String.format("%.2f", bbox.left)}, ${String.format("%.2f", bbox.top)}, ${String.format("%.2f", bbox.width)}, ${String.format("%.2f", bbox.height)})", color = NeonPurple)
                        panel.soundEffects.forEach { sfx ->
                            SoraBadge(text = "SFX: $sfx", color = WarningOrange)
                        }
                    }

                    // OCR Blocks
                    if (panel.ocrTextBlocks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Extracted OCR Text Blocks:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        panel.ocrTextBlocks.forEach { block ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF161E30))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = block.text,
                                        fontSize = 12.sp,
                                        color = if (block.category == OcrCategory.DIALOGUE) Color.White else NeonCyan
                                    )
                                    if (block.speakerCharacterId != null) {
                                        Text("Speaker: ${block.speakerCharacterId}", fontSize = 10.sp, color = ElectricPink)
                                    }
                                }
                                SoraBadge(text = block.category.name, color = if (block.category == OcrCategory.DIALOGUE) ElectricPink else NeonCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. CHARACTER MANAGER VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaCharacterManagerView(
    characters: List<ManhwaCharacter>,
    onAddCharacter: (ManhwaCharacter) -> Unit,
    onUpdateCharacter: (ManhwaCharacter) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoraSectionHeader(
                    title = "Character Consistency Database",
                    subtitle = "Maintains visual & vocal consistency across scenes",
                    icon = Icons.Default.People
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(characters) { char ->
            SoraGlassCard(borderColor = if (char.role == "Protagonist") ElectricPink else NeonCyan) {
                Column {
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
                                    .background(if (char.role == "Protagonist") ElectricPink else NeonCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(char.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(char.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${char.role} • ${char.ageCategory}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        SoraBadge(text = char.voiceId, color = NeonPurple)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Appearance: ${char.appearanceDescription}", fontSize = 11.sp, color = TextSecondary)
                    Text("Hair: ${char.hair} | Clothing: ${char.clothing}", fontSize = 11.sp, color = TextSecondary)
                    Text("Voice Profile: ${char.voiceCharacteristics} (Pitch: ${char.voicePitch}x, Speed: ${char.voiceSpeed}x)", fontSize = 11.sp, color = NeonCyan)
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF151C2C))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Consistency Profile: ${char.consistencyProfileSummary}",
                            fontSize = 11.sp,
                            color = ElectricPink
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("Cha Hae-In") }
        var role by remember { mutableStateOf("Supporting / S-Rank Hunter") }
        var hair by remember { mutableStateOf("Short Blonde Bob") }
        var voice by remember { mutableStateOf("VOICE_ENERGETIC_FEMALE") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Character Consistency Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Character Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hair, onValueChange = { hair = it }, label = { Text("Hair & Features") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = voice, onValueChange = { voice = it }, label = { Text("Voice ID") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCharacter(
                            ManhwaCharacter(
                                id = "CHAR_${System.currentTimeMillis().toString().takeLast(4)}",
                                name = name,
                                role = role,
                                hair = hair,
                                voiceId = voice
                            )
                        )
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Save Character", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DeepDarkBg
        )
    }
}

// -------------------------------------------------------------
// 5. AUDIO & VOICE VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaAudioVoiceView(
    audioTrack: AudioTrack?,
    characters: List<ManhwaCharacter>,
    onUpdateAudioTrack: (AudioTrack) -> Unit
) {
    if (audioTrack == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audio track loaded. Import voiceover to analyze waveforms & VAD.", color = TextSecondary)
        }
        return
    }

    var isOriginalTrackSelected by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Audio Pipeline & Voice Activity Detection",
                subtitle = "Compare Original vs Processed Audio Tracks",
                icon = Icons.Default.GraphicEq
            )
        }

        // Track Switcher & Cleanup Controls
        item {
            SoraGlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Audition Track", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = isOriginalTrackSelected,
                                onClick = { isOriginalTrackSelected = true },
                                label = { Text("Original Track") }
                            )
                            FilterChip(
                                selected = !isOriginalTrackSelected,
                                onClick = { isOriginalTrackSelected = false },
                                label = { Text("Processed (Cleaned)") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge("Duration", "${audioTrack.durationMs / 1000}s", Icons.Default.Timer)
                        MetricBadge("VAD Segments", "${audioTrack.segments.size}", Icons.Default.GraphicEq)
                        MetricBadge("Noise Reduct.", "${(audioTrack.noiseReductionLevel * 100).toInt()}%", Icons.Default.VolumeMute)
                        MetricBadge("Vocal Isolation", "Active", Icons.Default.Mic)
                    }
                }
            }
        }

        // Audio Segments List
        item {
            Text("Classified Audio Segments (${audioTrack.segments.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(audioTrack.segments) { seg ->
            SoraGlassCard(borderColor = if (seg.classification == AudioClassification.CHARACTER_DIALOGUE) ElectricPink else NeonCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(text = "${seg.startMs / 1000f}s - ${seg.endMs / 1000f}s", color = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            SoraBadge(text = seg.classification.name, color = if (seg.classification == AudioClassification.ACTION_SOUND) WarningOrange else ElectricPink)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "\"${seg.transcriptText}\"", fontSize = 12.sp, color = TextPrimary)
                        if (seg.speakerId != null) {
                            Text(text = "Speaker: ${seg.speakerId}", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. SYNC & ACTION VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaSyncActionView(
    scenes: List<ManhwaScene>,
    audioTrack: AudioTrack?,
    onUpdateScenes: (List<ManhwaScene>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Audio-to-Panel Synchronization",
                subtitle = "Action Audio Replacement & Speaker Alignment",
                icon = Icons.Default.SyncAlt
            )
        }

        items(scenes) { scene ->
            SoraGlassCard(borderColor = if (scene.actionRequiresReview) WarningOrange else GlassBorder) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoraBadge(text = scene.id, color = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scene ${scene.sceneNumber} ➔ Panel ${scene.panelId}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        SoraBadge(text = "${scene.durationMs / 1000f}s", color = ElectricPink)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Action: ${scene.actionDescription}", fontSize = 12.sp, color = TextSecondary)

                    if (scene.actionRequiresReview) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarningOrange.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Text("⚠️ ACTION REQUIRES REVIEW: Complex multi-character combat detected.", fontSize = 11.sp, color = WarningOrange)
                        }
                    }

                    // Action Audio Replacement Toggle
                    if (scene.originalActionAudioText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Action Audio Replacement", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Replace spoken noise \"${scene.originalActionAudioText}\" with synthesized SFX", fontSize = 10.sp, color = TextSecondary)
                            }
                            Checkbox(
                                checked = scene.isRedundantActionAudioRemoved,
                                onCheckedChange = { checked ->
                                    val list = scenes.toMutableList()
                                    val idx = list.indexOfFirst { it.id == scene.id }
                                    if (idx != -1) {
                                        list[idx] = scene.copy(isRedundantActionAudioRemoved = checked)
                                        onUpdateScenes(list)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. ANIMATION & CAMERA VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaAnimationCameraView(
    scenes: List<ManhwaScene>,
    characters: List<ManhwaCharacter>,
    pipeline: ManhwaStudioPipeline,
    onUpdateScenes: (List<ManhwaScene>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Animation & Cinematic Camera Studio",
                subtitle = "Configure camera trajectories, speed lines & aura effects",
                icon = Icons.Default.AutoAwesome
            )
        }

        // Interactive Canvas Preview
        item {
            ManhwaCanvasPlayer(
                scenes = scenes,
                characters = characters,
                animationEngine = pipeline.animationEngine
            )
        }

        item {
            Text("Scene Motion & Camera Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        items(scenes) { scene ->
            SoraGlassCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scene ${scene.sceneNumber}: ${scene.actionType.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        SoraBadge(text = scene.cameraMotion.name, color = NeonCyan)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Visual Effect: ${scene.animationMotion.name}", fontSize = 11.sp, color = ElectricPink)
                    Text("Sound Effect: ${scene.sfxName} @ ${scene.sfxTimestampMs}ms", fontSize = 11.sp, color = TextSecondary)
                    Text("Transition: ${scene.transitionType.name}", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. 11-TRACK TIMELINE EDITOR VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaTimelineEditorView(
    project: ManhwaProject,
    onUpdateProject: (ManhwaProject) -> Unit
) {
    val trackNames = listOf(
        "VIDEO", "PANELS", "CHARACTERS", "DIALOGUE", "NARRATION",
        "SFX", "MUSIC", "AMBIENCE", "SUBTITLES", "CAMERA", "ANIMATION"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Manhwa Multi-Track Timeline",
                subtitle = "11-Track Professional Audio-Visual Sequence Editor",
                icon = Icons.Default.ViewTimeline
            )
        }

        // Timeline Action Toolbar
        item {
            SoraGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cut", "Trim", "Split", "Merge", "Retiming", "Regenerate").forEach { act ->
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(act, fontSize = 10.sp, color = NeonCyan)
                        }
                    }
                }
            }
        }

        // 11 Track Rows
        items(trackNames) { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0C101A))
                    .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Track Label
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF192236))
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Text(track, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Track Segments Strip
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF131826)),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    project.scenes.forEach { sc ->
                        Box(
                            modifier = Modifier
                                .weight(sc.durationMs.toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when (track) {
                                        "VIDEO", "PANELS" -> ElectricPink.copy(alpha = 0.7f)
                                        "DIALOGUE", "NARRATION" -> NeonCyan.copy(alpha = 0.7f)
                                        "SFX", "MUSIC" -> WarningOrange.copy(alpha = 0.7f)
                                        else -> NeonPurple.copy(alpha = 0.7f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sc.id, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 9. RECAP & STORY CONTINUATION VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaRecapStoryView(
    project: ManhwaProject,
    pipeline: ManhwaStudioPipeline,
    onUpdateProject: (ManhwaProject) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var targetLengthMin by remember { mutableIntStateOf(10) }
    var narrationStyle by remember { mutableStateOf("Cinematic Storyteller") }
    var tone by remember { mutableStateOf("Dark / Dramatic") }
    var generatedPackage by remember { mutableStateOf<RecapScriptEngine.RecapProductionPackage?>(null) }
    var continuationResult by remember { mutableStateOf<StoryContinuationEngine.ContinuationResult?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "AI Recap Script & Story Continuation",
                subtitle = "YouTube Recap Generator & Episode Continuity Memory",
                icon = Icons.Default.MenuBook
            )
        }

        // Recap Generation Card
        item {
            SoraGlassCard(borderColor = ElectricPink) {
                Column {
                    Text("YouTube Manhwa Recap Generator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 10, 20).forEach { mins ->
                            FilterChip(
                                selected = targetLengthMin == mins,
                                onClick = { targetLengthMin = mins },
                                label = { Text("${mins}m Recap") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val pkg = pipeline.recapEngine.generateYouTubeRecap(
                                    project = project,
                                    panels = project.panels,
                                    recapConfig = project.recapConfig.copy(targetDurationMinutes = targetLengthMin)
                                )
                                generatedPackage = pkg
                                onShowMessage("Generated YouTube Production Package: ${pkg.chapters.size} chapters.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Full Recap Script", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Story Continuation Card
        item {
            SoraGlassCard(borderColor = NeonPurple) {
                Column {
                    Text("Story Continuation Engine (Episode Memory)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Continues narrative directly from the final scene without restarting.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = pipeline.continuationEngine.generateNextEpisode(
                                        currentStoryState = project.storyState,
                                        continuationType = ContinuationType.CONTINUE_RECAP
                                    )
                                    continuationResult = res
                                    onUpdateProject(project.copy(storyState = res.updatedStoryState, scenes = project.scenes + res.generatedScenes))
                                    onShowMessage("Created Next Episode: ${res.chapterTitle}")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("A: Continue Recap", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = pipeline.continuationEngine.generateNextEpisode(
                                        currentStoryState = project.storyState,
                                        continuationType = ContinuationType.CREATE_ORIGINAL_CONTINUATION
                                    )
                                    continuationResult = res
                                    onUpdateProject(project.copy(storyState = res.updatedStoryState, scenes = project.scenes + res.generatedScenes))
                                    onShowMessage("Created Original Fictional Continuation.")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("B: Original Arc", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Generated Script Package Display
        generatedPackage?.let { pkg ->
            item {
                SoraGlassCard(borderColor = NeonCyan) {
                    Column {
                        Text(pkg.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pkg.hook, fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Thumbnail Prompt: ${pkg.thumbnailConcept}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 10. PREVIEW & EXPORT VIEW (WITH 10-POINT QC)
// -------------------------------------------------------------
@Composable
fun ManhwaPreviewExportView(
    project: ManhwaProject,
    pipeline: ManhwaStudioPipeline,
    onUpdateProject: (ManhwaProject) -> Unit,
    onExportSuccess: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedFormat by remember { mutableStateOf(RecapFormat.YOUTUBE_LONG_FORM_16_9) }
    var qcReport by remember {
        mutableStateOf(
            pipeline.qcEngine.runQualityCheck(
                project = project,
                scenes = project.scenes,
                panels = project.panels,
                characters = project.characters,
                audioTrack = project.audioTrack
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Video Preview & Quality Control",
                subtitle = "10-Point QA Check & Multi-Format Video Export",
                icon = Icons.Default.Movie
            )
        }

        // Realtime Animated Preview Canvas
        item {
            ManhwaCanvasPlayer(
                scenes = project.scenes,
                characters = project.characters,
                animationEngine = pipeline.animationEngine,
                aspectRatio = if (selectedFormat == RecapFormat.YOUTUBE_SHORTS_9_16 || selectedFormat == RecapFormat.TIKTOK_REELS_9_16) 9f / 16f else 16f / 9f
            )
        }

        // 10-Point Quality Control Status Card
        item {
            SoraGlassCard(borderColor = if (qcReport.isPassed) AccentGreen else WarningOrange) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("10-Point Quality Inspection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        SoraBadge(text = if (qcReport.isPassed) "PASSED" else "REVIEW NEEDED", color = if (qcReport.isPassed) AccentGreen else WarningOrange)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Lip Sync: ${qcReport.lipSyncStatus}", fontSize = 11.sp, color = TextSecondary)
                    Text("• Audio Sync: ${qcReport.audioSyncStatus}", fontSize = 11.sp, color = TextSecondary)
                    Text("• Character Consistency: ${qcReport.characterConsistencyStatus}", fontSize = 11.sp, color = TextSecondary)
                    Text("• Redundancy: ${qcReport.duplicateAudioStatus}", fontSize = 11.sp, color = TextSecondary)

                    if (qcReport.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        qcReport.warnings.forEach { w ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(w.message, fontSize = 11.sp, color = WarningOrange, modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        qcReport = qcReport.copy(warnings = qcReport.warnings.filter { it.id != w.id })
                                    }
                                ) {
                                    Text("[Fix]", fontSize = 11.sp, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export Format Chooser
        item {
            SoraGlassCard {
                Column {
                    Text("Export Resolution & Aspect Ratio", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            RecapFormat.YOUTUBE_LONG_FORM_16_9,
                            RecapFormat.YOUTUBE_SHORTS_9_16,
                            RecapFormat.SQUARE_INSTAGRAM_1_1,
                            RecapFormat.CINEMATIC_ULTRAWIDE_21_9
                        ).forEach { fmt ->
                            val isSel = selectedFormat == fmt
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.aspectRatio) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val result = pipeline.videoAssembler.renderAndExportVideo(
                                    project = project,
                                    scenes = project.scenes,
                                    recapConfig = project.recapConfig.copy(format = selectedFormat)
                                )
                                onUpdateProject(project.copy(exportedVideoPath = result.videoFile.absolutePath, exportedSubtitlesPath = result.srtSubtitleFile.absolutePath))
                                onExportSuccess("Exported ${result.videoFile.name} (${result.fileSizeFormatted}) + Subtitles (.srt, .vtt)")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_export_video"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPink)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export MP4 Video + Subtitles", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 11. MODEL FUSION & COMPATIBILITY VIEW
// -------------------------------------------------------------
@Composable
fun ManhwaModelFusionView(
    modelConfig: ManhwaModelConfig
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoraSectionHeader(
                title = "Manhwa Composite Pipeline & Model Fusion",
                subtitle = "Hardware compatibility matrix & specialized model roles",
                icon = Icons.Default.Hub
            )
        }

        // Hardware Specs Card
        item {
            SoraGlassCard(borderColor = AccentGreen) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pipeline Hardware Allocation", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        SoraBadge(text = modelConfig.backend, color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge("RAM Need", "${modelConfig.ramRequirementGb} GB", Icons.Default.Memory)
                        MetricBadge("VRAM/GPU", "${modelConfig.vramRequirementGb} GB", Icons.Default.VideogameAsset)
                        MetricBadge("Speed", "${modelConfig.expectedSpeedFps} FPS", Icons.Default.Speed)
                        MetricBadge("Status", "COMPATIBLE", Icons.Default.CheckCircle)
                    }
                }
            }
        }

        // Model Role Table
        item {
            Text("Specialized Composite Model Roles", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        val roles = listOf(
            Pair("TEXT MODEL", modelConfig.textModel),
            Pair("VISION MODEL", modelConfig.visionModel),
            Pair("OCR MODEL", modelConfig.ocrModel),
            Pair("IMAGE MODEL", modelConfig.imageModel),
            Pair("VIDEO MODEL", modelConfig.videoModel),
            Pair("TTS MODEL", modelConfig.ttsModel),
            Pair("STT MODEL", modelConfig.sttModel),
            Pair("LIPSYNC MODEL", modelConfig.lipSyncModel),
            Pair("UPSCALE MODEL", modelConfig.upscaleModel),
            Pair("SEPARATION MODEL", modelConfig.separationModel)
        )

        items(roles) { (role, model) ->
            SoraGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(role, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricPink)
                    Text(model, fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}
