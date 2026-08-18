package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

/**
 * App Architecture, Complete Features, Screen Catalog & Source Code Blueprint
 * Provides an on-device searchable, copyable, and structured specification file for the entire app.
 */
object AppArchitectureContent {

    const val FULL_SOURCE_BLUEPRINT = """================================================================================
          SORA AI CREATIVE STUDIO & PRIVATE AGENT - FULL ARCHITECTURE SPECIFICATION
================================================================================
Version: 3.5.0-PRO (Production On-Device AI Suite)
Target Runtime: Android 14+ (API Level 34+), Jetpack Compose (M3), Kotlin Coroutines & Flow
Local Persistence: SQLite / Room Database (KSP) with Multi-Table Entity Relations
Inference Framework: On-Device Snapdragon NPU / Adreno Vulkan / CPU Vectorized Inference (LiteRT/Llama.cpp/ONNX) + Remote GPU Node
Audio Pipeline: Low-latency AudioTrack PCM16 Synthesizer + Spatial 3D Binaural DSP
Video Pipeline: RealMediaSynthesisEngine with 1s to 24+ Hours Multi-Segment Chaining (Real-Time Hardware-Accelerated Inference)

================================================================================
1. COMPLETE REPOSITORY & PACKAGE ARCHITECTURE
================================================================================
com.example/
├── MainActivity.kt                      // App Entrypoint, Scaffold, Modal Navigation & Dynamic Theming
├── ai/
│   ├── assistant/                       // Real-time Voice Agent & Tool Function Calling Execution Engine
│   ├── downloader/                      // Resumable Background GGUF/Diffusers Weight Model Downloader
│   ├── engine/
│   │   └── GenerationEngines.kt        // Diffusion Tensor Generator & Progress Stream Flow Emitters
│   ├── fusion/                          // Manhwa OCR, Speech-to-Speech & Multi-Modal Fusion Lab
│   ├── generator/
│   │   └── RealMediaSynthesisEngine.kt // ISO/IEC 14496 MP4/WAV Media Packager & Frame Serializer
│   ├── hardware/                        // Dynamic Hardware Profiler (Snapdragon NPU, Adreno GPU, RAM Monitor)
│   ├── inference/                       // On-Device Inference Orchestrator & Execution Dispatcher
│   ├── jobs/                            // Background Task Engine & WorkManager Scheduling Handlers
│   ├── logging/                         // Real-time System Telemetry & Diagnostics Circular Log Buffer
│   ├── models/                          // GGUF, SafeTensors, LoRA & ONNX Weight Registry
│   ├── quantization/                    // Q4_K_M, Q8_0, FP16 Quantization Calibration Benchmark
│   ├── queue/                           // Priority Task Queue Manager with VRAM Auto-Checkpointing
│   ├── script/                          // Screenplay Formatter, Subtext Analyzer & Storyboard Mapper
│   ├── server/                          // Local OpenAI-Compatible REST Server (0.0.0.0:8080)
│   ├── story/                           // Lorebook, Continuity Matrix & Character Bible Graph Engine
│   ├── voice/                           // Voice Synthesizer, 3D Spatial Audio & Pitch Tuner DSP
│   └── wakeword/                        // Offline Acoustic Neural Hotword Listener ("Hey Sora")
├── cloud/                               // Sora Cloud Remote GPU Cluster & Cloudflare Tunnel Client
├── data/
│   ├── AppDatabase.kt                   // Room Database with Migrations & Type Converters
│   ├── GenerationTaskDao.kt             // DAO for Background Jobs, Render Passes & Progress
│   ├── MediaItemDao.kt                  // DAO for Video, Image, Audio, Script & Story Assets
│   └── SoraRepository.kt                // Unified Reactive Single-Source-of-Truth Data Layer
├── editor/                              // Multi-Track Timeline Engine, Keyframe Interpolator & VFX
├── network/
│   └── huggingface/
│       ├── HuggingFaceApiService.kt     // Retrofit REST API Interface (Hub search, repo metadata & @Streaming downloads)
│       ├── HuggingFaceDataModels.kt     // Moshi JSON Models (HfModelItem, HfModelDetail, HfSibling, HfLfsInfo)
│       ├── HuggingFaceNetworkUtility.kt // Production Retrofit Stream Downloader (Chunk streaming, Range headers, SHA-256)
│       └── HuggingFaceRetrofitClient.kt // OkHttpClient & Retrofit Builder with Token Auth & Logging Interceptors
├── manhwa/
│   ├── data/                            // Manhwa Project Manifests, Page Slices & OCR Cache
│   ├── engine/                          // 2.5D Multi-Plane Parallax Camera & Particle FX Choreographer
│   ├── model/                           // Manhwa Panel, Dialogue Bubble & Layer Geometry Entities
│   └── ui/
│       ├── ManhwaCanvasPlayer.kt        // Hardware-Accelerated 60fps Dynamic Manhwa Canvas Player
│       ├── ManhwaStudioScreen.kt        // 12-Feature Manhwa Production Screen with 3-Line Menu Drawer
│       └── ManhwaViews.kt               // Panel Inspector, OCR Overlay & Speech Bubble Editor Views
└── ui/
    ├── MainActivity.kt                  // Root Scaffold, System Bar Colors & Bottom/Rail Navigation
    ├── SoraMainViewModel.kt             // Central State ViewModel (StateFlows, Forms, Task Dispatches)
    ├── components/
    │   ├── AppArchitectureAndSourceViewer.kt // Interactive On-Device Architecture & Source Code Viewer
    │   ├── CommonUiComponents.kt        // SoraGlassCard, SoraBadge, TopBars, StatCards & Dialogs
    │   ├── GenerationChips.kt           // Hyperparameter Chips, Aspect Ratios & Quality Selectors
    │   ├── StudioFeatureNavigator.kt    // Universal 3-Line Menu Drawer & TopBar for All 6 Studios
    │   ├── TimelineEditorView.kt        // Multi-Track Keyframe Canvas & Splice/Trim Controls
    │   └── generation/
    │       ├── AudioGenerationStudio.kt // Voice AI Studio Workspace View
    │       ├── DurationFormatters.kt    // Timecode Formatters (1s to 24+ Hours, HMS, Bitrate Calc)
    │       ├── ImageGenerationStudio.kt // Image Studio Workspace View
    │       ├── StoryGenerationStudio.kt // Story & Script Studio Workspace View
    │       └── VideoDurationSelector.kt // Comprehensive 1s-to-Hours Duration Controller Component
    ├── screens/
    │   ├── AssistantScreen.kt           // Conversational Voice AI Assistant Screen
    │   ├── DownloadsScreen.kt           // Background Resumable Model Weights Downloader Screen
    │   ├── EditorScreen.kt              // Non-Linear Multi-Track Video/Audio Timeline Editor Screen
    │   ├── GalleryScreen.kt             // Generated Media Vault, Video Player & File Inspector Screen
    │   ├── GenerateScreen.kt            // 12-Feature Video Generation Studio Screen (1s to Hours)
    │   ├── HomeScreen.kt                // System Telemetry Dashboard & Creative Studio Portal
    │   ├── ImageGenScreen.kt            // 12-Feature Image Generation Studio Screen
    │   ├── ModelsScreen.kt              // Local AI Model Manager, Benchmarks & Memory Allocator
    │   ├── ProjectsScreen.kt            // Multi-Project Workspace & Timeline Asset Organizer
    │   ├── ScriptWriterScreen.kt        // 12-Feature Screenplay Studio Screen with Standard Formatting
    │   ├── SettingsScreen.kt            // System Hardware, SAF Storage, REST API & Blueprint Viewer
    │   ├── SoraCloudScreen.kt           // Remote Cloud GPU Cluster & Hybrid Compute Manager
    │   ├── StoryWriterScreen.kt         // 12-Feature Lorebook, Worldbuilding & Novel Studio Screen
    │   ├── TaskQueueScreen.kt           // Active Multi-Segment Render Queue & Resource Inspector
    │   ├── VoiceAIScreen.kt             // 12-Feature Voice AI, Cloning & Spatial Audio Studio Screen
    │   └── WakeWordScreen.kt            // Offline Neural Acoustic Listener Screen
    ├── state/
    │   └── GenerationStates.kt          // UI State Data Classes, Filter Presets & Form Models
    └── theme/
        ├── Color.kt                     // Cyberpunk Dark Aesthetic (NeonCyan, NeonPurple, ElectricPink)
        ├── Shape.kt                     // Material 3 Rounded Corner Geometries
        ├── Theme.kt                     // Dynamic ColorScheme & Glassmorphism Surfaces
        └── Type.kt                      // Typography System (Display, Headline, Title, Body, Monospace)

================================================================================
2. COMPREHENSIVE SCREEN & FEATURE CATALOG (ALL 16 APP PAGES)
================================================================================

[1] HOME SCREEN (HomeScreen.kt)
- Real-time Hardware Telemetry HUD (Snapdragon NPU, Adreno GPU, RAM & Temperature meters)
- Studio Quick-Launch Gateway (Story, Script, Voice, Manhwa, Video, Image, Editor)
- Recent Creations Carousel with direct media playback and export shortcuts
- Active Render Pipeline Status widget and quick benchmark trigger
- Quick action pills for instantaneous task generation

[2] VIDEO STUDIO (GenerateScreen.kt & VideoGenerationStudio.kt)
- Universal 3-Line Menu Drawer with Category Grouping & Real-time Active Indicator
- Full-Range Duration Support: 1 Second to 24+ Hours!
  * Seconds Mode (1s–59s): Quick micro-clip / GIF / shorts presets (1s, 2s, 3s, 5s, 10s, 15s, 30s, 45s, 59s)
  * Minutes Mode (1m–59m): Scene & short-film presets (1m, 2m, 3m, 5m, 10m, 15m, 20m, 30m, 45m, 59m)
  * Hours Mode (1h–24h): Long-form feature presentation presets (1h, 2h, 3h, 4h, 6h, 8h, 12h, 24h)
  * Custom Timecode Editor: Interactive HH:MM:SS dialog with increment shortcuts (+10s, +1m, +10m, +1h)
- 12 Specialized Video Studio Features:
  1. Text-to-Video Synthesis (Prompt -> 4K HDR 60fps Video)
  2. Image-to-Video Animation (First/Last frame keyframing)
  3. Video-to-Video Style Transfer (Style, anime, realistic transformation)
  4. Infinite Video Continuation (Temporal auto-regressive continuation)
  5. Object Removal & Inpainting (Brush mask erase and replacement)
  6. 4K / 60fps Super-Resolution (Real-ESRGAN + Frame Interpolation)
  7. Motion & Pose Transfer (Skeletal tracking animation transfer)
  8. Neural Lip-Sync Studio (Audio-driven facial phoneme deformation)
  9. 3D Camera Trajectory (Orbital, Dolly Zoom, Pan, Crane, Drone paths)
  10. RIFE 8x Slow-Motion (Ultra-smooth optical flow temporal interpolation)
  11. Spatial 3D / VR Stereoscopic (Left/Right eye depth rendering)
  12. Script Storyboard Director (Batch scene multi-shot sequence generation)

[3] IMAGE STUDIO (ImageGenScreen.kt & ImageGenerationStudio.kt)
- Universal 3-Line Menu Drawer with 12 Creative Workspace Modes:
  1. Text-to-Image Diffusion (SDXL, Turbo, Midjourney aesthetic engine)
  2. AI Image Editing & Retouching (Natural language modifications)
  3. AI Super-Resolution Upscaling (2x, 4x, 8x Ultra-HD enhancement)
  4. Precision Brush Inpainting (Smart contextual content replacement)
  5. Infinite Canvas Outpainting (Boundary expansion in 4 directions)
  6. Background Removal & Matting (Alpha hair-level precision cutout)
  7. Motion Pose Warping (OpenPose 2D/3D joint deformation)
  8. Video Frame Enhancement (Denoise & texture sharpening)
  9. 3D Character Turnarounds (Orthographic Front, Side, Back, 3/4 views)
  10. 2D-to-3D Depth Mesh Generator (Displacement maps & OBJ/GLTF export)
  11. Donghua Cultivation Creator (Immortal fantasy styling & magical auras)
  12. Panoramic Matte Painting Synthesizer (360° seamless HDR environments)

[4] STORY WRITER (StoryWriterScreen.kt & StoryGenerationStudio.kt)
- Universal 3-Line Menu Drawer with 12 Novel & Narrative Architectures:
  1. Premise & Hook Synthesizer (Loglines, genre fusion, tropes)
  2. 3-Act & 8-Sequence Beat Sheet (Dan Harmon story circle, hero journey)
  3. Dynamic Lorebook & Worldbuilder (Magic systems, geopolitics, history)
  4. Deep Character Bibles (Backstories, MBTI, flaws, voice quirks)
  5. Chapter & Scene Flow Editor (Focus mode, distraction-free markdown)
  6. Real-time Dialogue Optimizer (Sharp subtext, dialect consistency)
  7. Narrative Continuity Analyzer (Timeline conflict & plot-hole detection)
  8. Foreshadowing & Chekhov's Gun Matrix (Clue placement tracker)
  9. Scene Pacing & Tension Graph (W-curve tension visualization)
  10. Multi-Angle Synopsis & Blurb (Backcover, elevator pitch, loglines)
  11. Author Style Adaptation Engine (Tolkien, King, Sanderson, Asimov)
  12. Multi-Format Novel Exporter (EPUB, PDF, Markdown, DOCX)

[5] SCRIPT WRITER (ScriptWriterScreen.kt)
- Universal 3-Line Menu Drawer with 12 Screenplay & Storyboard Engines:
  1. Industry Standard Screenplay Formatter (Sluglines, Action, Parentheticals)
  2. Scene Slugline & Asset Breakdown (Locations, props, VFX, cast count)
  3. Dialogue Rhythm & Pacing Tuner (Beats, pauses, emotional cadence)
  4. Dynamic Character Arc Tracker (Emotional journey per scene)
  5. Action Sequence Choreographer (Combat, stunts, camera angles)
  6. Subtext & Theme Resonance Analyzer (Hidden character motives)
  7. Real-time Scene Timing & Page Budget (1 min per page calculation)
  8. Sound & Music Cue Choreographer (Diegetic/Non-diegetic audio tags)
  9. Visual Storyboard Generator (Prompt generation for each shot)
  10. Script-to-Video Prompt Mapper (Direct injection to Sora Video Studio)
  11. Cast Voice Tagging & Speech Binder (Binding lines to Voice AI models)
  12. Shooting Script Exporter (Fountain, Final Draft FDX, PDF, Call Sheets)

[6] VOICE AI STUDIO (VoiceAIScreen.kt & AudioGenerationStudio.kt)
- Universal 3-Line Menu Drawer with 12 Audio & Speech Engines:
  1. Multi-Lingual Neural Synthesizer (Natural prosody, 40+ languages)
  2. Instant 3-Second Voice Cloning (Reference audio sample matching)
  3. Phoneme & Pitch Tuner Matrix (Micro-tonal curve modulation)
  4. Multi-Speaker Table Dialogue (Conversational script voiceover)
  5. Audio-to-Audio Style Transfer (Vocal morphing, age/gender transformation)
  6. Dynamic Emotion & Pacing Slider (Whisper, rage, fear, excitement)
  7. Spatial 3D Binaural DSP (Surround sound positioning in 3D sphere)
  8. Dynamic BGM Auto-Ducking (Automatic music ducking under voiceover)
  9. Real-Time Vocal Changer (Mic input real-time morphing)
  10. Speech-to-Speech Instant Translator (Live voice-matched translation)
  11. Vocal Preset & Voiceprint Library (Custom speaker registry)
  12. High-Res Audio Master Exporter (24-bit 96kHz FLAC / WAV / MP3)

[7] MANHWA STUDIO (ManhwaStudioScreen.kt & ManhwaCanvasPlayer.kt)
- Universal 3-Line Menu Drawer with 12 Webtoon & Comic Production Tools:
  1. Production Dashboard & Strip Ingestion (Vertical long-strip slicing)
  2. AI Panel Segmentation & OCR (Text bubble extraction & translation)
  3. Character Layer Binder & Inpainting (Subject separation for animation)
  4. Voiceover & Dialogue Dubbing Sync (Speech alignment to panels)
  5. Action FX & Particle Choreographer (Speedlines, energy auras, glow)
  6. Dynamic 2.5D Camera & Parallax (Multi-layer depth panning & zoom)
  7. Timeline & Chapter Sequencer (Vertical scroll pacing & frame beats)
  8. AI Episode Recap Generator (Short highlight reel for social media)
  9. Live Interactive Canvas Player (Hardware-accelerated 60fps viewer)
  10. Model Fusion Lab (Combining anime LoRAs for consistent styles)
  11. Multi-Format Video Exporter (9:16 Shorts, 16:9 Movie, GIF, WebM)
  12. Asset Vault & Layer Manager (Backgrounds, stickers, SFX library)

[8] EDITOR SCREEN (EditorScreen.kt & TimelineEditorView.kt)
- Professional Multi-Track Non-Linear Video Editor (NLE)
- Track 1: Primary Video Canvas with frame-accurate scrubbing
- Track 2: B-Roll & Overlay Video with blending modes (Screen, Multiply, Overlay)
- Track 3: Voiceover & Dialogue Track with waveform visualization
- Track 4: Ambient BGM & Sound Effects with keyframe volume automation
- Track 5: Subtitles & SRT/VTT Caption Generator with karaoke animations
- Splice, Trim, Cut, Ripple Delete, Speed Ramp (0.1x to 10x), Color Grading LUTs

[9] GALLERY & MEDIA VAULT (GalleryScreen.kt)
- Comprehensive Media Asset Manager (Videos, Images, Audio, Scripts, Stories)
- Full-screen Video Player with seekbar, loop mode, frame-stepping and PIP
- Metadata Inspector (Prompts, seeds, CFG scale, steps, model hash, duration)
- Batch Selection, Direct Gallery Export, System Share Sheet, Delete confirmation
- Asset Filtering by Type (All, Video, Image, Audio, Story/Script) and Search

[10] MODEL MANAGER (ModelsScreen.kt & DownloadsScreen.kt)
- Local Model Weights Registry (GGUF, Diffusers, Safetensors, ONNX, LoRA)
- Snapdragon NPU / GPU / CPU memory allocation sliders
- Resumable multi-threaded model weight downloader with SHA-256 validation
- Benchmark Suite: Tokens/sec, Time-to-first-token, VRAM throughput, FPS

[11] TASK QUEUE SCREEN (TaskQueueScreen.kt)
- Active Multi-Segment Render Queue Inspector
- Real-time Hardware Sensor Monitor (CPU %, GPU %, NPU TOPS, RAM MB, Temp °C)
- Visual Step-by-Step Progress & Time-Remaining Estimation
- Task Priority Re-ordering, Pause, Resume, Cancel & Log Inspection

[12] SORA CLOUD (SoraCloudScreen.kt)
- Hybrid Local/Cloud Compute Bridge
- Remote GPU Node Connection (Auto-scaling, RTX 4090 / A100 / H100 clusters)
- Cloudflare Secure Tunnel / Tailscale integration
- Job Synchronization & Local Cache Offloading

[13] ASSISTANT & TOOL ENGINE (AssistantScreen.kt)
- Multi-turn Conversational AI Assistant with Persona Presets
- Real-time Audio Waveform Speech Interface & Voice Output
- Function Calling Tools: Media generation, system queries, memory recall
- Terminal Execution & Diagnostics Logging console

[14] WAKE WORD ENGINE (WakeWordScreen.kt)
- Offline Neural Acoustic Listener ("Hey Sora", "Activate Studio")
- Continuous Audio Spectrogram Visualizer
- Adjustable Sensitivity Threshold & False-Positive Rejection Filter
- Battery-Optimized Low-Power Ambient Audio Pipeline

[15] PROJECTS SCREEN (ProjectsScreen.kt)
- Multi-Project Workspace Manager (Films, Series, Audiobooks, Webtoons)
- Asset Relationship Graph & Version Snapshots
- Project Backup, Restore & Archive (.soraproject bundle)

[16] SETTINGS SCREEN (SettingsScreen.kt)
- System Hardware Profiler & Acceleration Switch (NPU / Vulkan / OpenCL / CPU)
- Storage Manager (Internal, External SD Card, Custom SAF Tree Picker)
- Local OpenAI REST Server Configuration (Host 0.0.0.0, Port 8080, API Keys)
- Telegram Bot Webhook Integration for Remote AI Generation Triggering
- App Architecture, Features & Source Code Blueprint Viewer

================================================================================
3. CORE CODE ARCHITECTURE & IMPLEMENTATION PATTERNS
================================================================================

--- A. DURATION CONTROLLER (1s TO 24+ HOURS) ---
// Location: /app/src/main/java/com/example/ui/components/generation/VideoDurationSelector.kt
// Provides 3 specialized units:
// 1. Seconds Mode: 1s .. 59s
// 2. Minutes Mode: 1m .. 59m (60s .. 3540s)
// 3. Hours Mode: 1h .. 24h (3600s .. 86400s)
// 4. Custom Timecode: Exact HH:MM:SS with continuous multi-segment calculation.

--- B. MEDIA SYNTHESIS ENGINE ---
// Location: /app/src/main/java/com/example/ai/generator/RealMediaSynthesisEngine.kt
// Serializes genuine ISO/IEC 14496-12 / 14496-14 MP4 containers directly on-device.
// Writes ftyp, moov (mvhd, trak, tkhd, mdia, minf, stbl), and mdat sample buffers.
// Embeds prompt hashes, frame index markers, and PCM audio sync tracks.

--- C. REACTIVE DATA LAYER (ROOM DATABASE + FLOWS) ---
// Location: /app/src/main/java/com/example/data/AppDatabase.kt
// Entities: MediaItemEntity, GenerationTaskEntity, ModelPresetEntity, ScriptProjectEntity
// DAO: Reactive Flow queries with auto-invalidation on inserts, updates, and deletes.

================================================================================
4. DEPENDENCIES & BUILD SPECIFICATIONS
================================================================================
- Kotlin: 2.0.21
- Jetpack Compose: BOM 2024.10.01 (Material 3, Foundation, Animation)
- Android Gradle Plugin: 8.7.2
- Room Database: 2.6.1 (Room KTX, KSP Annotation Processor)
- Coroutines: 1.9.0 (Core, Android)
- Navigation Compose: 2.8.3
- Lifecycle ViewModel Compose: 2.8.7
- Coil Compose: 2.7.0 (Asynchronous Image Loading)
- Serialization: KotlinX Serialization JSON 1.7.3
================================================================================
"""
}

/**
 * Interactive full-screen Dialog / Modal Viewer for the App Architecture & Source Code.
 */
@Composable
fun AppArchitectureAndSourceViewerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("ALL") }

    val rawContent = AppArchitectureContent.FULL_SOURCE_BLUEPRINT

    // Filtered Content based on section and search
    val displayedContent = remember(searchQuery, selectedSection, rawContent) {
        var text = rawContent
        if (selectedSection != "ALL") {
            val sections = text.split("================================================================================")
            val matched = sections.filter { it.contains(selectedSection, ignoreCase = true) }
            if (matched.isNotEmpty()) {
                text = matched.joinToString("\n================================================================================\n")
            }
        }
        if (searchQuery.isNotBlank()) {
            val lines = text.lines()
            val filteredLines = lines.filter { it.contains(searchQuery, ignoreCase = true) }
            if (filteredLines.isNotEmpty()) {
                text = "--- SEARCH RESULTS FOR: '$searchQuery' (${filteredLines.size} matches) ---\n\n" +
                        filteredLines.joinToString("\n")
            }
        }
        text
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            color = DeepDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
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
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "App Architecture & Source Manifest",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "All 16 Screens • 6 Studios • 72 Features • Full Source Spec",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlassSurface)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Search Bar & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search architecture, code, features...", fontSize = 12.sp, color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("blueprint_search_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface
                        ),
                        singleLine = true
                    )

                    // Copy All Blueprint Button
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(rawContent))
                            Toast.makeText(context, "Full Architecture & Source Code copied to clipboard!", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("copy_blueprint_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DeepDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Full Spec", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepDarkBg)
                    }
                }

                // Section Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sections = listOf(
                        "ALL" to "All Specifications",
                        "REPOSITORY" to "1. Package Tree",
                        "SCREEN" to "2. All 16 Pages",
                        "VIDEO STUDIO" to "• Video Studio (1s-Hours)",
                        "IMAGE STUDIO" to "• Image Studio",
                        "STORY WRITER" to "• Story Studio",
                        "SCRIPT WRITER" to "• Script Studio",
                        "VOICE AI" to "• Voice Studio",
                        "MANHWA STUDIO" to "• Manhwa Studio",
                        "CODE ARCHITECTURE" to "3. Core Code",
                        "DEPENDENCIES" to "4. Dependencies"
                    )
                    items(sections) { (key, label) ->
                        val isSelected = selectedSection == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSection = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Main Monospace Text Viewer Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF070B14))
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    val horizontalScroll = rememberScrollState()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScroll)
                    ) {
                        item {
                            Text(
                                text = displayedContent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF8AE8FF),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Bottom Footer Info & Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Length: ${displayedContent.lines().size} lines • UTF-8 • Production-Ready",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
