package com.example.manhwa.model

import java.util.UUID

/**
 * Core Data Models for Manhwa Studio — Real Manhwa Recap & Animation Engine
 */

data class ManhwaProject(
    val id: String = "manhwa_proj_${System.currentTimeMillis()}",
    val title: String = "My Manhwa Recap",
    val episodeTitle: String = "Episode 01",
    val description: String = "Solo Hunter Awakening — Complete Season 1 Recap",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailUri: String? = null,
    val durationSeconds: Int = 512, // e.g. 8m 32s
    val fps: Int = 24,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val totalPanels: Int = 18,
    val totalScenes: Int = 24,
    val narrationStyle: String = "Cinematic Storyteller",
    val language: String = "English",
    val copyrightConsentAcknowledged: Boolean = true,
    val status: ProjectStatus = ProjectStatus.READY_TO_ANIMATE,
    val exportedVideoPath: String? = null,
    val exportedSubtitlesPath: String? = null,
    val projectDir: String? = null,
    val panels: List<ManhwaPanel> = emptyList(),
    val scenes: List<ManhwaScene> = emptyList(),
    val characters: List<ManhwaCharacter> = emptyList(),
    val audioTrack: AudioTrack? = null,
    val storyState: StoryState = StoryState(),
    val recapConfig: RecapConfig = RecapConfig(),
    val lastSavedStep: String = "SCENE_BUILDER",
    val lastSceneIndex: Int = 0
)

enum class ProjectStatus {
    DRAFT,
    IMPORTING,
    ANALYZING_PANELS,
    OCR_PROCESSING,
    AUDIO_ANALYZING,
    SYNCHRONIZING,
    ANIMATING,
    READY_TO_ANIMATE,
    EXPORTING,
    COMPLETED,
    FAILED
}

data class ManhwaPanel(
    val id: String = "P001",
    val pageIndex: Int = 0,
    val panelIndex: Int = 0,
    val originalImageUri: String? = null,
    val croppedPanelUri: String? = null,
    val boundingBox: PanelBoundingBox = PanelBoundingBox(0.05f, 0.05f, 0.90f, 0.40f),
    val characterIds: List<String> = listOf("CHAR_01"),
    val environmentDescription: String = "City street at dusk with shattered asphalt",
    val actionDescription: String = "Hero activates dark energy blade and slashes forward",
    val cameraFraming: CameraFraming = CameraFraming.MEDIUM_SHOT,
    val panelOrder: Int = 1,
    val expressionSummary: String = "Determined & Fierce",
    val soundEffects: List<String> = listOf("BOOM", "SLASH"),
    val ocrTextBlocks: List<OcrTextBlock> = emptyList(),
    val composition: String = "DYNAMIC_DIAGONAL",
    val confidenceScore: Float = 0.96f
)

data class PanelBoundingBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = (left + width).coerceAtMost(1f)
    val bottom: Float get() = (top + height).coerceAtMost(1f)
}

enum class CameraFraming {
    EXTREME_CLOSE_UP,
    CLOSE_UP,
    MEDIUM_SHOT,
    WIDE_SHOT,
    EXTREME_WIDE,
    DUTCH_ANGLE,
    BIRDS_EYE_VIEW,
    LOW_ANGLE_HERO
}

data class OcrTextBlock(
    val id: String = "OCR_${UUID.randomUUID().toString().take(6)}",
    val text: String,
    val category: OcrCategory = OcrCategory.DIALOGUE,
    val bubbleBoundingBox: PanelBoundingBox = PanelBoundingBox(0.1f, 0.1f, 0.4f, 0.15f),
    val confidence: Float = 0.94f,
    val speakerCharacterId: String? = "CHAR_01",
    val detectedEmotion: String = "ANGRY"
)

enum class OcrCategory {
    DIALOGUE,
    NARRATION,
    SOUND_EFFECT,
    VISUAL_TEXT,
    ACTION_TEXT
}

data class ManhwaCharacter(
    val id: String = "CHAR_01",
    val name: String = "Sung Jin-Woo",
    val role: String = "Protagonist",
    val avatarUri: String? = null,
    val appearanceDescription: String = "Black messy hair, glowing purple eyes, dark high-collar coat",
    val hair: String = "Jet Black, Spiky Anime Cut",
    val clothing: String = "Shadow Monarch Trench Coat with glowing runic trims",
    val ageCategory: String = "Young Adult (22)",
    val personality: String = "Calm, strategic, relentless in combat, protective",
    val voiceId: String = "VOICE_DARK_HERO_01",
    val voiceCharacteristics: String = "Deep, resonant, calm baritone with gravel undertones",
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 0.95f,
    val voiceEmotion: String = "DETERMINED",
    val expressions: List<String> = listOf("Neutral Stoic", "Smug Grin", "Glowing Wrath", "Shocked"),
    val typicalActions: List<String> = listOf("Summons shadow army", "Dagger slash", "Teleport strike", "Monologue"),
    val consistencyProfileSummary: String = "Maintain glowing blue/purple eye particle aura, sharp jawline, high contrast ink shadows."
)

data class AudioTrack(
    val id: String = "aud_${System.currentTimeMillis()}",
    val originalAudioUri: String? = null,
    val processedAudioUri: String? = null,
    val durationMs: Long = 512000L,
    val sampleRate: Int = 44100,
    val isOriginalActionAudioMuted: Boolean = true,
    val noiseReductionLevel: Float = 0.8f,
    val vocalIsolationEnabled: Boolean = true,
    val segments: List<AudioSegment> = emptyList(),
    val voiceTrackUri: String? = null,
    val musicTrackUri: String? = null,
    val sfxTrackUri: String? = null,
    val backgroundTrackUri: String? = null
)

data class AudioSegment(
    val id: String = "seg_${UUID.randomUUID().toString().take(6)}",
    val startMs: Long,
    val endMs: Long,
    val classification: AudioClassification = AudioClassification.NARRATION,
    val speakerId: String? = "NARRATOR",
    val transcriptText: String = "",
    val confidence: Float = 0.95f,
    val isUserModified: Boolean = false,
    val peakAmplitude: Float = 0.85f
) {
    val durationMs: Long get() = endMs - startMs
}

enum class AudioClassification {
    NARRATION,
    CHARACTER_DIALOGUE,
    ACTION_SOUND,
    SOUND_EFFECT,
    MUSIC,
    BACKGROUND,
    UNKNOWN
}

data class ManhwaScene(
    val id: String = "S001",
    val sceneNumber: Int = 1,
    val panelId: String = "P001",
    val durationMs: Long = 5400L,
    val narrationText: String = "He had finally arrived at the gate of the final boss.",
    val dialogueText: String? = "You won't escape my shadow realm.",
    val speakerCharacterId: String? = "CHAR_01",
    val actionType: ActionType = ActionType.WALKING,
    val actionDescription: String = "Hero walks forward slowly, drawing double daggers as shadows swirl",
    val actionRequiresReview: Boolean = false,
    val cameraMotion: CameraMotionType = CameraMotionType.SLOW_PUSH_IN,
    val cameraKeyframes: CameraKeyframes = CameraKeyframes(),
    val animationMotion: AnimationMotionType = AnimationMotionType.DARK_AURA_MIST,
    val visemes: List<VisemeKeyframe> = emptyList(),
    val transitionType: TransitionType = TransitionType.MANHWA_SLASH_FADE,
    val sfxName: String = "AURA_HUM",
    val sfxTimestampMs: Long = 1200L,
    val musicTrack: String = "EPIC_ORCHESTRAL_BATTLE",
    val isRedundantActionAudioRemoved: Boolean = true,
    val originalActionAudioText: String? = "BOOM! *punch sound*"
)

enum class ActionType {
    WALKING,
    RUNNING,
    JUMPING,
    PUNCHING,
    KICKING,
    FIGHTING,
    SHOOTING,
    FLYING,
    FALLING,
    TALKING,
    LOOKING,
    TURNING,
    OPENING,
    CLOSING,
    HOLDING,
    THROWING,
    BLOCKING,
    DODGING,
    TRANSFORMING,
    ATTACKING,
    DEFENDING,
    REACTING,
    IDLE
}

enum class CameraMotionType {
    SLOW_PUSH_IN,
    FAST_TRACKING,
    IMPACT_SHAKE_ZOOM,
    SLOW_CLOSEUP,
    PAN_ACROSS,
    WIDE_SWEEP,
    DUTCH_TILT,
    STATIC_DRAMATIC
}

data class CameraKeyframes(
    val startScale: Float = 1.0f,
    val endScale: Float = 1.25f,
    val startOffsetX: Float = 0.0f,
    val endOffsetX: Float = -0.1f,
    val startOffsetY: Float = 0.0f,
    val endOffsetY: Float = -0.05f,
    val rotationDeg: Float = 0.0f,
    val shakeIntensity: Float = 0.0f
)

enum class AnimationMotionType {
    PARALLAX_DEPTH,
    HAIR_CLOTHING_SWAY,
    EYE_BLINK,
    MOUTH_LIPSYNC,
    SPEED_LINES_BURST,
    DARK_AURA_MIST,
    SLASH_ENERGY,
    IMPACT_FLASH,
    PARTICLE_SPARKS,
    FLOATING_DUST
}

enum class TransitionType {
    MANHWA_SLASH_FADE,
    INK_SPLASH,
    PANEL_SLIDE,
    FLASH_WHITE,
    CROSSFADE,
    HARD_CUT
}

data class VisemeKeyframe(
    val timestampMs: Long,
    val visemeShape: VisemeShape = VisemeShape.REST,
    val mouthOpenRatio: Float = 0.0f,
    val activeSpeakerId: String? = null
)

enum class VisemeShape {
    REST,
    A_AH,
    E_EE,
    I_IH,
    O_OH,
    U_OO,
    M_B_P,
    F_V,
    L_TH,
    W_R
}

data class StoryState(
    val currentChapter: Int = 1,
    val currentEpisode: Int = 1,
    val currentSceneIndex: Int = 24,
    val currentLocation: String = "Demon King's Lair - 100th Floor",
    val characterStates: Map<String, String> = mapOf(
        "Sung Jin-Woo" to "Awakened S-Rank Shadow Lord (Health: 100%, Mana: 85%)",
        "Demon King Baran" to "Enraged, Summoning Storm Wyverns"
    ),
    val characterRelationships: Map<String, String> = mapOf(
        "Sung Jin-Woo -> Baran" to "Mortal Enemy / Dungeon Boss",
        "Sung Jin-Woo -> Cha Hae-In" to "Allied S-Rank Hunter / Mutual Respect"
    ),
    val importantObjects: List<String> = listOf("Demon Monarch's Daggers", "Ruler's Authority Orb", "Elixir of Life Recipe"),
    val knownAbilities: List<String> = listOf("Shadow Extraction", "Stealth", "Ruler's Reach", "Dragon's Fear"),
    val unresolvedConflicts: List<String> = listOf("Defeat Baran to obtain the pure soul of the Demon King", "Save the injured raid squad trapped outside the barrier"),
    val completedEvents: List<String> = listOf("Defeated Floor 90 Guard Knights", "Summoned Shadow Iron and Tank"),
    val currentObjective: String = "Execute final dual-dagger strike on Baran before the lightning storm peaks",
    val previousDialogueHistory: List<String> = listOf(
        "Sung Jin-Woo: 'You ruled over these floors for thousands of years... but today, your domain belongs to the shadows.'",
        "Baran: 'Insolent mortal! You dare challenge the monarch of white flames?!'"
    ),
    val narrativeTone: String = "EPIC_DARK_FANTASY"
)

data class RecapConfig(
    val targetDurationMinutes: Int = 10,
    val narrationStyle: String = "Cinematic Storyteller", // Cinematic Storyteller, Fast Paced Shonen, Webtoon Lore Master, Dramatic Voiceover
    val voiceStyle: String = "Deep Epic Male (Narrator A)",
    val tone: String = "Dark / Dramatic", // Dark / Dramatic, Hype / Energetic, Mysterious, Humorous
    val targetAudience: String = "Anime & Manhwa Recap Viewers",
    val spoilerLevel: String = "Full Spoilers Allowed",
    val language: String = "English",
    val format: RecapFormat = RecapFormat.YOUTUBE_LONG_FORM_16_9,
    val autoSyncWithAudio: Boolean = true,
    val autoLipSync: Boolean = true,
    val autoActionAudioReplacement: Boolean = true,
    val burnedInSubtitles: Boolean = true,
    val generateChapters: Boolean = true,
    val enableStoryContinuationMode: Boolean = false,
    val continuationType: ContinuationType = ContinuationType.CONTINUE_RECAP
)

enum class RecapFormat(val label: String, val aspectRatio: String, val width: Int, val height: Int) {
    YOUTUBE_LONG_FORM_16_9("YouTube Long-Form (16:9)", "16:9", 1920, 1080),
    YOUTUBE_SHORTS_9_16("YouTube Shorts (9:16)", "9:16", 1080, 1920),
    TIKTOK_REELS_9_16("TikTok / Reels (9:16)", "9:16", 1080, 1920),
    SQUARE_INSTAGRAM_1_1("Square Feed (1:1)", "1:1", 1080, 1080),
    COMIC_STANDARD_4_3("Comic Frame (4:3)", "4:3", 1440, 1080),
    CINEMATIC_ULTRAWIDE_21_9("Cinematic Ultrawide (21:9)", "21:9", 2560, 1080)
}

enum class ContinuationType {
    CONTINUE_RECAP,
    CREATE_ORIGINAL_CONTINUATION
}

data class QualityCheckReport(
    val isPassed: Boolean = true,
    val continuityStatus: String = "PASSED",
    val lipSyncStatus: String = "PASSED",
    val audioSyncStatus: String = "PASSED",
    val characterConsistencyStatus: String = "PASSED",
    val sceneTimingStatus: String = "PASSED",
    val missingAssetsStatus: String = "PASSED",
    val missingAudioStatus: String = "PASSED",
    val duplicateAudioStatus: String = "PASSED",
    val resolutionStatus: String = "PASSED",
    val frameRateStatus: String = "PASSED",
    val warnings: List<QcWarning> = emptyList(),
    val fixActions: List<QcAction> = emptyList()
)

data class QcWarning(
    val id: String = UUID.randomUUID().toString().take(6),
    val sceneId: String,
    val message: String,
    val severity: QcSeverity = QcSeverity.WARNING,
    val suggestedFix: String = "Auto-assign active character voice profile"
)

enum class QcSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class QcAction(
    val id: String,
    val title: String,
    val actionType: String,
    val sceneId: String
)

data class ManhwaTask(
    val taskId: String = "task_${UUID.randomUUID().toString().take(8)}",
    val taskType: ManhwaTaskType,
    val title: String,
    val progressPercent: Int = 0,
    val currentStep: String = "Initializing pipeline...",
    val estimatedRemainingSeconds: Int? = null,
    val ramUsageMb: Int? = null,
    val cpuUsagePercent: Int? = null,
    val gpuUsagePercent: Int? = null,
    val currentModel: String? = null,
    val currentSceneIndex: Int = 1,
    val totalScenes: Int = 24,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

enum class ManhwaTaskType {
    PANEL_ANALYSIS,
    OCR_EXTRACTION,
    CHARACTER_DETECTION,
    AUDIO_ANALYSIS,
    AUDIO_PANEL_SYNC,
    ACTION_ANIMATION,
    LIP_SYNC,
    VOICE_SYNTHESIS,
    SCENE_ASSEMBLY,
    RECAP_GENERATION,
    STORY_CONTINUATION,
    QUALITY_CHECK,
    VIDEO_EXPORT
}

data class ManhwaModelConfig(
    val textModel: String = "Gemini-Flash / Sora-Script-7B",
    val visionModel: String = "Manhwa-Vision-OCR-v2",
    val ocrModel: String = "Tesseract-Manga-OCR-v4",
    val imageModel: String = "Sora-Manhwa-Diffusion-XL",
    val videoModel: String = "Sora-Motion-LiteRT-v1",
    val ttsModel: String = "Piper-Neural-Voice-TTS",
    val sttModel: String = "Whisper-Tiny-VAD",
    val lipSyncModel: String = "Wav2Lip-Manhwa-Lite",
    val upscaleModel: String = "RealESRGAN-Anime-4x",
    val separationModel: String = "Demucs-Lite-4Stem",
    val ramRequirementGb: Float = 3.8f,
    val vramRequirementGb: Float = 2.2f,
    val backend: String = "LiteRT + Vulkan NPU",
    val expectedSpeedFps: Float = 28.5f,
    val isHardwareCompatible: Boolean = true
)
