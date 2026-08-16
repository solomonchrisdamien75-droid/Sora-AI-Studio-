package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.inference.model.ModelCapability
import com.example.ai.jobs.AIJobStatus
import com.example.ai.voice.VoiceProject
import com.example.ui.SoraMainViewModel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAIScreen(
    viewModel: SoraMainViewModel,
    onBack: () -> Unit = {}
) {
    val voiceEngine = viewModel.voiceAIEngine
    val project by voiceEngine.currentVoiceProject.collectAsState()
    val isGenerating by voiceEngine.isGenerating.collectAsState()
    val generationPhase by voiceEngine.generationPhase.collectAsState()
    val statusMessage by voiceEngine.statusMessage.collectAsState()
    val isPlaying by voiceEngine.isPlaying.collectAsState()
    val activeModel by viewModel.activeLoadedModel.collectAsState()
    val unifiedJobs by viewModel.unifiedJobs.collectAsState()
    val activeVoiceJob = unifiedJobs.values.firstOrNull { it.type == com.example.ai.jobs.AIJobType.VOICE_SYNTHESIS && it.status == AIJobStatus.RUNNING }

    val coroutineScope = rememberCoroutineScope()
    var selectedSubFeature by remember { mutableStateOf(0) } // 0: Text-to-Speech, 1: Speech-to-Text, 2: Voice Conversion, 3: Voice Cloning
    val emotions = listOf("Neutral", "Dramatic", "Cheerful", "Whispering", "Energetic", "Ominous")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Voice AI Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "NEURAL VOCODER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real-time on-device speech synthesis & acoustic modeling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Model Capability Header
            VoiceModelCapabilityHeader(activeModel = activeModel, viewModel = viewModel)

            // Sub-feature Switcher
            PrimaryTabRow(
                selectedTabIndex = selectedSubFeature,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedSubFeature == 0,
                    onClick = { selectedSubFeature = 0 },
                    text = { Text("TTS Studio", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedSubFeature == 1,
                    onClick = { selectedSubFeature = 1 },
                    text = { Text("Speech-to-Text", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedSubFeature == 2,
                    onClick = { selectedSubFeature = 2 },
                    text = { Text("Voice Conversion", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedSubFeature == 3,
                    onClick = { selectedSubFeature = 3 },
                    text = { Text("Voice Cloning", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Live progress banner
            if (isGenerating || activeVoiceJob != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                                text = "🔊 Neural Vocoder Synthesizing Speech",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = activeVoiceJob?.checkpointPhase ?: generationPhase,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        activeVoiceJob?.let { job ->
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { job.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            when (selectedSubFeature) {
                0 -> TtsStudioMainContent(
                    project = project,
                    voiceEngine = voiceEngine,
                    isGenerating = isGenerating,
                    isPlaying = isPlaying,
                    emotions = emotions,
                    onUpdate = { voiceEngine.updateProject(it) },
                    onSynthesize = {
                        coroutineScope.launch {
                            val persona = voiceEngine.availableVoices.firstOrNull { it.id == project.selectedVoiceId }
                            voiceEngine.synthesizeVoiceToFile(
                                text = project.text,
                                title = project.title,
                                voiceName = persona?.name ?: "Cinema Deep Baritone",
                                speed = project.speed,
                                pitch = project.pitch,
                                selectedModel = activeModel
                            )
                        }
                    },
                    onPlayAudio = { path ->
                        voiceEngine.playAudio(path)
                    },
                    onStopAudio = {
                        voiceEngine.stopAudio()
                    }
                )
                1 -> SpeechToTextFeatureContent()
                2 -> VoiceConversionFeatureContent(voiceEngine = voiceEngine)
                3 -> VoiceCloningFeatureContent(activeModel = activeModel)
            }
        }
    }
}

@Composable
fun VoiceModelCapabilityHeader(
    activeModel: com.example.data.AiModelEntity?,
    viewModel: SoraMainViewModel
) {
    val compCheck = remember(activeModel) {
        viewModel.aiInferenceManager.validateCapability(activeModel, ModelCapability.TEXT_TO_SPEECH)
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
                imageVector = if (compCheck.isCompatible) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (compCheck.isCompatible) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Acoustic Vocoder: ${activeModel?.name ?: "LiteRT Neural TTS (16-bit PCM 24kHz)"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hardware: ${if (viewModel.hardwareProfile.value?.hasGpu == true) "GPU Accelerated" else "Multi-Threaded CPU Engine"}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TtsStudioMainContent(
    project: VoiceProject,
    voiceEngine: com.example.ai.voice.VoiceAIEngine,
    isGenerating: Boolean,
    isPlaying: Boolean,
    emotions: List<String>,
    onUpdate: (VoiceProject) -> Unit,
    onSynthesize: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = project.title,
            onValueChange = { onUpdate(project.copy(title = it)) },
            label = { Text("Audio Track Title") },
            modifier = Modifier.fillMaxWidth().testTag("voice_track_title_input")
        )

        OutlinedTextField(
            value = project.text,
            onValueChange = { onUpdate(project.copy(text = it)) },
            label = { Text("Narration / Dialogue Text to Synthesize") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("voice_text_input")
        )

        // Voice Persona Selector
        Text("Select Voice Persona:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(voiceEngine.availableVoices) { persona ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (project.selectedVoiceId == persona.id)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .width(180.dp)
                        .border(
                            width = if (project.selectedVoiceId == persona.id) 2.dp else 1.dp,
                            color = if (project.selectedVoiceId == persona.id) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { onUpdate(project.copy(selectedVoiceId = persona.id)) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(persona.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${persona.gender} • ${persona.style}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }

        // Emotion selector
        Text("Emotion Tone:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(emotions) { emo ->
                FilterChip(
                    selected = project.emotion == emo,
                    onClick = { onUpdate(project.copy(emotion = emo)) },
                    label = { Text(emo, fontSize = 11.sp) }
                )
            }
        }

        // Speed & Pitch Sliders
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Speed: ${String.format("%.2f", project.speed)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = project.speed,
                    onValueChange = { onUpdate(project.copy(speed = it)) },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Pitch: ${String.format("%.2f", project.pitch)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = project.pitch,
                    onValueChange = { onUpdate(project.copy(pitch = it)) },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
            }
        }

        // Synthesize Button
        Button(
            onClick = onSynthesize,
            enabled = !isGenerating && project.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("voice_synthesize_btn")
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isGenerating) "Synthesizing Speech..." else "Synthesize Neural Speech (WAV)")
        }

        // Audio Player & Live Waveform Preview Card
        if (project.outputAudioPath != null && File(project.outputAudioPath!!).exists()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🎧 Synthesized Audio Track", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Duration: ${String.format("%.1f", project.durationSeconds)}s • Format: WAV 16-bit PCM",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isPlaying) onStopAudio() else onPlayAudio(project.outputAudioPath!!)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).testTag("voice_play_btn")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Live Waveform Canvas
                    VoiceWaveformCanvas(isPlaying = isPlaying)

                    Spacer(Modifier.height(8.dp))
                    Text("File Path: ${project.outputAudioPath}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun VoiceWaveformCanvas(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (Math.PI * 2).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f))
    ) {
        val barCount = 36
        val barWidth = size.width / (barCount * 1.5f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val x = i * (barWidth * 1.5f) + barWidth / 2f
            val wave = if (isPlaying) {
                (0.3f + 0.7f * ((sin(phase + i * 0.35f) + 1f) / 2f))
            } else {
                0.2f
            }
            val barHeight = (size.height * 0.8f * wave).coerceAtLeast(4f)
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun SpeechToTextFeatureContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Speech-to-Text Transcriber", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Whisper-compatible acoustic phoneme decoding engine. Transcribes real speech audio from microphone or imported WAV/MP3 files into timestamped text subtitles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun VoiceConversionFeatureContent(voiceEngine: com.example.ai.voice.VoiceAIEngine) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Voice Timbre Conversion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Converts speech audio files from one speaker timbre to another (e.g. Baritone to Anime Heroine) while preserving exact speech prosody and rhythm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun VoiceCloningFeatureContent(activeModel: com.example.data.AiModelEntity?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Zero-Shot Voice Cloning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Extracts speaker acoustic embeddings from 3-second reference audio to clone customized voices. Requires an installed TTS/Voice model supporting speaker embeddings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
