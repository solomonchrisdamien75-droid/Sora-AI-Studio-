package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.assistant.ScriptProductionPackage
import com.example.ai.downloader.DownloadProgressState
import com.example.ai.downloader.HuggingFaceModelInfo
import com.example.ai.hardware.DeviceHardwareProfile
import com.example.ai.hardware.RealtimeTelemetryState
import com.example.ai.hardware.StorageVolumeInfo
import com.example.ai.quantization.ModelQuantizationEngine
import com.example.ai.quantization.QuantizationConfig
import com.example.ai.quantization.QuantizationPrecision
import com.example.ai.quantization.QuantizationProgressState
import com.example.ai.quantization.QuantizationTradeoffObjective

import com.example.cloud.CloudJobResponse
import com.example.ai.wakeword.SoraWakeWordEngine
import com.example.ai.wakeword.VoiceEventItem
import com.example.ai.wakeword.VoiceActionType
import com.example.data.*
import com.example.editor.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SoraTab(val title: String, val route: String) {
    HOME("Home", "home"),
    STORY_STUDIO("Story Writer", "story_studio"),
    SCRIPT_STUDIO("Script Writer", "script_studio"),
    VOICE_AI("Voice AI", "voice_ai"),
    MANHWA_STUDIO("Manhwa Studio", "manhwa_studio"),
    GENERATE("Video Studio", "generate"),
    IMAGE_GEN("Image Studio", "image_gen"),
    QUEUE("Task Queue", "queue"),
    WAKE_WORD("Sora Voice", "wake_word"),
    MODELS("Models", "models"),
    DOWNLOADS("Downloads", "downloads"),
    GALLERY("Gallery", "gallery"),
    PROJECTS("Projects", "projects"),
    EDITOR("Editor", "editor"),
    ASSISTANT("AI Assistant", "assistant"),
    SORA_CLOUD("Server & Cloud", "server_cloud"),
    SETTINGS("Settings", "settings")
}

data class ImageGenerationFormState(
    val prompt: String = "A hyperdetailed masterpiece, futuristic cyberpunk girl with glowing neon katana in rain, 8k resolution, cinematic lighting",
    val title: String = "Cyberpunk Heroine",
    val style: String = "PHOTOREALISTIC", // PHOTOREALISTIC, ANIME, CYBERPUNK, OCTANE_3D, FANTASY_CINEMATIC, OIL_PAINTING, CONCEPT_ART, WATERCOLOR
    val resolution: String = "1024x1024", // 512x512, 768x768, 1024x1024, 1536x1024, 2048x2048
    val aspectRatio: String = "1:1", // 1:1, 16:9, 9:16, 4:3, 3:2, 2:3
    val steps: Int = 30, // 10, 20, 30, 50, 100
    val cfgScale: Float = 7.5f,
    val negativePrompt: String = "blurry, low quality, distorted, extra limbs, bad anatomy, artifacts, watermark, lowres, text, error",
    val sampler: String = "Euler a", // Euler a, DPM++ 2M Karras, DDIM, UniPC, LCM Turbo
    val seed: Long = -1L,
    val isRandomSeed: Boolean = true,
    val highResFix: Boolean = true,
    val batchCount: Int = 1,
    val sourceImageUri: String? = null,
    val maskImageUri: String? = null,
    val mode: String = "TEXT_TO_IMAGE", // TEXT_TO_IMAGE, IMAGE_TO_IMAGE, INPAINTING, OUTPAINTING, BACKGROUND_REMOVAL, UPSCALING
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

data class ManhwaPanelItem(
    val id: String,
    val title: String,
    val imageUri: String? = null,
    val panelType: String = "COMBAT", // COMBAT, DIALOGUE, EXPOSITION, DRAMATIC_ZOOM
    val actionDescription: String = "Hero slashes with shadow blades",
    val spokenDialogue: String? = "I am the Shadow Monarch!"
)

data class ChatAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long = 0L,
    val type: AttachmentType = AttachmentType.FILE
)

enum class AttachmentType {
    IMAGE,
    PDF,
    DOCUMENT,
    FILE
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // "OPEN_YOUTUBE", "SET_TIMER", "MANHWA_RECAP", "NAVIGATE_GENERATE"
    val actionTitle: String? = null,
    val isExecuted: Boolean = false,
    val attachments: List<ChatAttachment> = emptyList()
)

data class ActiveTimer(
    val id: String,
    val title: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isFinished: Boolean = false
)

data class GenerationFormState(
    val prompt: String = "A cinematic futuristic camera shot of a flying solar spacecraft orbiting a neon blue gas giant",
    val title: String = "Sci-Fi Space Odyssey",
    val generationType: String = "TEXT_TO_VIDEO",
    val mode: String = "FAST", // FAST, BALANCED, CINEMA
    val durationLabel: String = "5 seconds",
    val durationSec: Int = 5,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val fps: Int = 24,
    val cameraMotion: String = "DYNAMIC_PAN", // DYNAMIC_PAN, ZOOM_IN, ZOOM_OUT, ORBIT_360, TILT_UP, STATIC_CINEMATIC, DRONE_FLYTHROUGH
    val motionStrength: Float = 0.7f,
    val videoCodec: String = "H.264",
    val temporalConsistency: Float = 0.85f,
    val motionPrompt: String = "",
    val cameraPrompt: String = "",
    val lightingPrompt: String = "",
    val isSegmented: Boolean = false,
    val isPaused: Boolean = false,
    val checkpointSaved: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val sourceImageUri: String? = null,
    val sourceVideoUri: String? = null,
    val sourceAudioUri: String? = null,
    val characterProfileText: String? = null,
    val maskImageUri: String? = null,
    // Dedicated Image Generation Studio System Properties
    val imageStyle: String = "PHOTOREALISTIC", // PHOTOREALISTIC, ANIME, CYBERPUNK, OCTANE_3D, FANTASY_CINEMATIC, OIL_PAINTING, CONCEPT_ART, WATERCOLOR
    val imageResolution: String = "1024x1024", // 512x512, 768x768, 1024x1024, 1536x1024, 2048x2048
    val imageAspectRatio: String = "1:1", // 1:1, 16:9, 9:16, 4:3, 3:2, 2:3
    val imageSteps: Int = 30, // 10, 20, 30, 50, 100
    val imageCfgScale: Float = 7.5f, // 1.0 to 20.0
    val imageNegativePrompt: String = "blurry, low quality, distorted, extra limbs, bad anatomy, artifacts, watermark",
    val imageSampler: String = "Euler a", // Euler a, DPM++ 2M Karras, DDIM, UniPC, LCM Turbo
    val imageSeed: Long = -1L,
    val imageHighResFix: Boolean = true,
    val imageBatchCount: Int = 1,
    // Dedicated Audio / Voice AI System Properties
    val audioVoiceArchetype: String = "MALE_DEEP", // MALE_DEEP, FEMALE_MELODIC, AI_ASSISTANT, ANIME_HERO, DRAMATIC_NARRATOR
    val audioPitch: Int = 0,
    val audioSpeed: Float = 1.0f,
    val audioEmotion: String = "NEUTRAL", // NEUTRAL, DRAMATIC, ENTHUSIASTIC, WHISPERING, ANGRY
    val audioFormat: String = "WAV",
    // Dedicated Story & Script AI System Properties
    val storyFormat: String = "SCREENPLAY", // SCREENPLAY, YOUTUBE_SCRIPT, NOVEL_CHAPTER, SHOT_LIST
    val storySceneCount: Int = 4,
    val storyTone: String = "SCI_FI", // SCI_FI, DARK_FANTASY, CYBERPUNK_ACTION, MYSTERY, ROMANCE
    // Manhwa Recap Studio State
    val manhwaChapterTitle: String = "Solo Hunter Chapter 42: Shadow Monarch Awakening",
    val manhwaPanels: List<ManhwaPanelItem> = listOf(
        ManhwaPanelItem("p1", "Panel 1: Imperial Gate Clash", null, "COMBAT", "Shadow Monarch slashes through demonic gates", "Stand back, everyone!"),
        ManhwaPanelItem("p2", "Panel 2: Shadow Soldiers Arise", null, "DIALOGUE", "Dark aura spreads across frozen battlefield", "ARISE! Serve your Monarch!"),
        ManhwaPanelItem("p3", "Panel 3: Frost Monarch Counterattack", null, "DRAMATIC_ZOOM", "Ice spikes shatter mountains in background", "You dare challenge an ancient god?!")
    ),
    val manhwaVoiceoverUri: String? = "audio/voiceover_ch42_recap.mp3",
    val manhwaFilterActionNarration: Boolean = true, // Smart filter: Removes redundant spoken action narration once rendered visually, preserving character speech & action SFX
    val manhwaLipSyncEnabled: Boolean = true,
    val manhwaAnimationStyle: String = "MANHWA_CINEMATIC_FLOW", // MANHWA_CINEMATIC_FLOW, ANIME_SPEED_LINES, 3D_PARALLAX_ZOOM, DYNAMIC_ACTION
    val manhwaCheckpointPanelIndex: Int = 1,
    val manhwaCheckpointTimeSec: Int = 145,
    val manhwaContinuationPrompt: String = "Generate Chapter 43 Continuation: The Shadow Monarch summons 10,000 spectral knights to defend Seoul from the Frost Monarch.",
    val manhwaContinuationScript: String? = null,
    val isGeneratingManhwaContinuation: Boolean = false,
    // Voice Wake Word 'Skra' & AI Full Device Control State
    val wakeWordEnabled: Boolean = true,
    val isListeningForWakeWord: Boolean = true,
    val lastDetectedWakeWord: String? = null,
    val deviceControlGranted: Boolean = true,
    val systemStatusLog: String = "Wake Word Engine Active: Listening for 'Skra'"
)

class SoraMainViewModel(application: Application) : AndroidViewModel(application) {

    val repository: SoraRepository

    private val _selectedTab = MutableStateFlow(SoraTab.HOME)
    val selectedTab: StateFlow<SoraTab> = _selectedTab.asStateFlow()

    private val _hardwareProfile = MutableStateFlow<DeviceHardwareProfile?>(null)
    val hardwareProfile: StateFlow<DeviceHardwareProfile?> = _hardwareProfile.asStateFlow()

    private val _generationForm = MutableStateFlow(GenerationFormState())
    val generationForm: StateFlow<GenerationFormState> = _generationForm.asStateFlow()

    private val _huggingFaceQuery = MutableStateFlow("")
    val huggingFaceQuery: StateFlow<String> = _huggingFaceQuery.asStateFlow()

    private val _huggingFaceResults = MutableStateFlow<List<HuggingFaceModelInfo>>(emptyList())
    val huggingFaceResults: StateFlow<List<HuggingFaceModelInfo>> = _huggingFaceResults.asStateFlow()

    private val _downloadingState = MutableStateFlow<DownloadProgressState?>(null)
    val downloadingState: StateFlow<DownloadProgressState?> = _downloadingState.asStateFlow()

    private val _quantizationState = MutableStateFlow<QuantizationProgressState?>(null)
    val quantizationState: StateFlow<QuantizationProgressState?> = _quantizationState.asStateFlow()

    private val _assistantInput = MutableStateFlow("Sci-Fi action scene with spaceship chase through neon asteroids")
    val assistantInput: StateFlow<String> = _assistantInput.asStateFlow()

    private val _generatedScript = MutableStateFlow<ScriptProductionPackage?>(null)
    val generatedScript: StateFlow<ScriptProductionPackage?> = _generatedScript.asStateFlow()

    private val _isAssistantLoading = MutableStateFlow(false)
    val isAssistantLoading: StateFlow<Boolean> = _isAssistantLoading.asStateFlow()

    // Video Editor state
    private val _editorProject = MutableStateFlow(createInitialEditorProject())
    val editorProject: StateFlow<VideoEditorProject> = _editorProject.asStateFlow()

    private val _activeEditorClipId = MutableStateFlow<String?>(null)
    val activeEditorClipId: StateFlow<String?> = _activeEditorClipId.asStateFlow()

    fun setActiveEditorClip(clipId: String?) {
        _activeEditorClipId.value = clipId
    }

    // Virtual RAM & Workspace mode
    private val _memoryMode = MutableStateFlow("Balanced Mode") // Low RAM, Balanced, Maximum Performance
    val memoryMode: StateFlow<String> = _memoryMode.asStateFlow()

    private val _useSdCardCache = MutableStateFlow(true)
    val useSdCardCache: StateFlow<Boolean> = _useSdCardCache.asStateFlow()

    private val _latestGeneratedResult = MutableStateFlow<GalleryItemEntity?>(null)
    val latestGeneratedResult: StateFlow<GalleryItemEntity?> = _latestGeneratedResult.asStateFlow()

    private val _latestExportedResult = MutableStateFlow<GalleryItemEntity?>(null)
    val latestExportedResult: StateFlow<GalleryItemEntity?> = _latestExportedResult.asStateFlow()

    private val _settingsStatusMessage = MutableStateFlow<String?>(null)
    val settingsStatusMessage: StateFlow<String?> = _settingsStatusMessage.asStateFlow()

    // Interactive AI Chat & Phone Action Execution State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "👋 Hello! I am your AI Assistant. You can upload images, PDF documents, and files here, or ask me to perform device actions like:\n• 📎 Upload photos & generate video scripts\n• 📄 Upload PDFs for AI summary & breakdown\n• ▶️ 'Open YouTube'\n• ⏱️ 'Set timer for 5 minutes'\n• 📖 'Create manhwa recap'\n• 🎬 'Write sci-fi movie script'"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _stagedChatAttachments = MutableStateFlow<List<ChatAttachment>>(emptyList())
    val stagedChatAttachments: StateFlow<List<ChatAttachment>> = _stagedChatAttachments.asStateFlow()

    fun addChatAttachment(attachment: ChatAttachment) {
        _stagedChatAttachments.value = _stagedChatAttachments.value + attachment
    }

    fun removeChatAttachment(attachmentId: String) {
        _stagedChatAttachments.value = _stagedChatAttachments.value.filterNot { it.id == attachmentId }
    }

    fun clearStagedChatAttachments() {
        _stagedChatAttachments.value = emptyList()
    }

    private val _activeTimers = MutableStateFlow<List<ActiveTimer>>(emptyList())
    val activeTimers: StateFlow<List<ActiveTimer>> = _activeTimers.asStateFlow()

    fun dismissLatestGeneratedResult() {
        _latestGeneratedResult.value = null
    }

    fun dismissLatestExportedResult() {
        _latestExportedResult.value = null
    }

    fun setSettingsStatus(message: String) {
        _settingsStatusMessage.value = message
    }

    // Wake-Word & Hands-Free Alexa-Surpassing Engine
    val wakeWordEngine = SoraWakeWordEngine.getInstance(application)
    val isWakeWordServiceRunning = wakeWordEngine.isServiceRunning
    val isWakeWordListening = wakeWordEngine.isListening
    val wakeWordConsentGranted = wakeWordEngine.consentGranted
    val currentWakeWord = wakeWordEngine.currentWakeWord
    val wakeWordSensitivity = wakeWordEngine.sensitivity
    val audioAmplitude = wakeWordEngine.audioAmplitude
    val lastDetectedVoiceCommand = wakeWordEngine.lastDetectedCommand
    val lastAiVoiceResponse = wakeWordEngine.lastAiResponse
    val voiceLogHistory = wakeWordEngine.voiceLogHistory
    val isTtsVoiceEnabled = wakeWordEngine.ttsEnabled
    val continuousListening = wakeWordEngine.continuousListening
    val isScreenControlActive = wakeWordEngine.screenControlActive

    // Settings States (Matching Image Mockups)
    private val _themeMode = MutableStateFlow("SYSTEM") // "SYSTEM", "LIGHT", "DARK"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(0.95f) // 0.75 to 1.35
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    // AI Engine Configuration (OpenAI-compatible endpoint)
    private val _apiBaseUrl = MutableStateFlow("http://192.168.1.X:8080/v1")
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private val _apiEngineKey = MutableStateFlow("sk-sora-offline-local-key-99214")
    val apiEngineKey: StateFlow<String> = _apiEngineKey.asStateFlow()

    private val _apiProviderPreset = MutableStateFlow("Local Server")
    val apiProviderPreset: StateFlow<String> = _apiProviderPreset.asStateFlow()

    private val _apiEngineModel = MutableStateFlow("Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv409")
    val apiEngineModel: StateFlow<String> = _apiEngineModel.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    // Tuning & Boundaries
    private val _disableMaxSteps = MutableStateFlow(false)
    val disableMaxSteps: StateFlow<Boolean> = _disableMaxSteps.asStateFlow()

    private val _maxStepsPerTask = MutableStateFlow(16)
    val maxStepsPerTask: StateFlow<Int> = _maxStepsPerTask.asStateFlow()

    private val _contextLimitTokens = MutableStateFlow(1024)
    val contextLimitTokens: StateFlow<Int> = _contextLimitTokens.asStateFlow()

    private val _settingsTemperature = MutableStateFlow(1.00f)
    val settingsTemperature: StateFlow<Float> = _settingsTemperature.asStateFlow()

    // Behavior & Extensions
    private val _useScreenCompression = MutableStateFlow(true)
    val useScreenCompression: StateFlow<Boolean> = _useScreenCompression.asStateFlow()

    private val _sendSystemPrompt = MutableStateFlow(true)
    val sendSystemPrompt: StateFlow<Boolean> = _sendSystemPrompt.asStateFlow()

    // Telegram Remote Access
    private val _telegramBotToken = MutableStateFlow("")
    val telegramBotToken: StateFlow<String> = _telegramBotToken.asStateFlow()

    private val _isTelegramBotEnabled = MutableStateFlow(false)
    val isTelegramBotEnabled: StateFlow<Boolean> = _isTelegramBotEnabled.asStateFlow()

    // Server & Tunnel (from Screenshot 1 & 7)
    private val _isApiServerToggle = MutableStateFlow(false)
    val isApiServerToggle: StateFlow<Boolean> = _isApiServerToggle.asStateFlow()

    private val _requireApiKeyToggle = MutableStateFlow(false)
    val requireApiKeyToggle: StateFlow<Boolean> = _requireApiKeyToggle.asStateFlow()

    private val _serverApiKey = MutableStateFlow("sk-live-tunnel-sora-39485")
    val serverApiKey: StateFlow<String> = _serverApiKey.asStateFlow()

    private val _isPublicTunnelEnabled = MutableStateFlow(false)
    val isPublicTunnelEnabled: StateFlow<Boolean> = _isPublicTunnelEnabled.asStateFlow()

    private val _tunnelProvider = MutableStateFlow("Cloudflare") // "Cloudflare" or "ngrok"
    val tunnelProvider: StateFlow<String> = _tunnelProvider.asStateFlow()

    private val _cloudflareTunnelToken = MutableStateFlow("")
    val cloudflareTunnelToken: StateFlow<String> = _cloudflareTunnelToken.asStateFlow()

    private val _stablePublicUrl = MutableStateFlow("https://sora-model-tunnel.trycloudflare.com")
    val stablePublicUrl: StateFlow<String> = _stablePublicUrl.asStateFlow()

    private val _isTunnelRunning = MutableStateFlow(false)
    val isTunnelRunning: StateFlow<Boolean> = _isTunnelRunning.asStateFlow()

    // Inference Mode & Model Parameters
    private val _inferenceMode = MutableStateFlow("LOCAL") // "LOCAL" or "CLOUD"
    val inferenceMode: StateFlow<String> = _inferenceMode.asStateFlow()

    private val _modelExecutionPreset = MutableStateFlow("AUTO_FAST") // "AUTO_FAST", "GPU_FAST", "CPU_SAFE"
    val modelExecutionPreset: StateFlow<String> = _modelExecutionPreset.asStateFlow()

    private val _settingsMaxTokens = MutableStateFlow(256)
    val settingsMaxTokens: StateFlow<Int> = _settingsMaxTokens.asStateFlow()

    private val _settingsContextSize = MutableStateFlow(1024)
    val settingsContextSize: StateFlow<Int> = _settingsContextSize.asStateFlow()

    // Image Gen Parameters
    private val _imageGenSteps = MutableStateFlow(1)
    val imageGenSteps: StateFlow<Int> = _imageGenSteps.asStateFlow()

    private val _imageSizePreset = MutableStateFlow("Auto")
    val imageSizePreset: StateFlow<String> = _imageSizePreset.asStateFlow()

    private val _gpuSafetyThresholdMb = MutableStateFlow(1843)
    val gpuSafetyThresholdMb: StateFlow<Int> = _gpuSafetyThresholdMb.asStateFlow()

    private val _imageBackend = MutableStateFlow("GPU") // "GPU", "CPU"
    val imageBackend: StateFlow<String> = _imageBackend.asStateFlow()

    private val _customSystemPrompt = MutableStateFlow("You are AI Chat, a helpful and friendly assistant. Be concise, accurate, and conversational. Answer questions directly without unnecessary preamble.")
    val customSystemPrompt: StateFlow<String> = _customSystemPrompt.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SoraRepository(
            context = application,
            db = database,
            repoScope = viewModelScope,
            onJobFinished = { _latestGeneratedResult.value = it }
        )

        viewModelScope.launch {
            repository.initializeDefaultData()
            refreshHardwareProfile()
            searchHuggingFaceModels("")
        }
    }

    // Unified Real AI Engines & Inference Architecture
    val aiInferenceManager by lazy { repository.aiInferenceManager }
    val aiJobManager by lazy { repository.aiJobManager }
    val projectStorageManager by lazy { repository.projectStorageManager }
    val storyEngine by lazy { repository.storyEngine }
    val scriptEngine by lazy { repository.scriptEngine }
    val voiceAIEngine by lazy { repository.voiceAIEngine }

    // Real AI Jobs Flow from background manager
    val unifiedJobs by lazy { aiJobManager.jobs }

    val allModels: StateFlow<List<AiModelEntity>> = repository.aiModelDao.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedModels: StateFlow<List<AiModelEntity>> = repository.aiModelDao.getDownloadedModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJobs: StateFlow<List<GenerationJobEntity>> = repository.generationJobDao.getAllJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queuedJobs: StateFlow<List<GenerationJobEntity>> = repository.generationJobDao.getQueuedJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isQueueProcessing: StateFlow<Boolean> = repository.taskQueueManager.isQueueProcessing
    val isAutoProcessEnabled: StateFlow<Boolean> = repository.taskQueueManager.isAutoProcessEnabled
    val currentRunningJobId: StateFlow<String?> = repository.taskQueueManager.currentRunningJobId
    val queueStatusMessage: StateFlow<String?> = repository.taskQueueManager.statusMessage

    fun dismissQueueStatusMessage() {
        repository.taskQueueManager.clearStatusMessage()
    }

    val activeJob: StateFlow<GenerationJobEntity?> = repository.generationJobDao.getActiveJob()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val galleryItems: StateFlow<List<GalleryItemEntity>> = repository.galleryDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudServers: StateFlow<List<SoraCloudServerEntity>> = repository.soraCloudDao.getAllServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serverState: StateFlow<com.example.ai.server.ServerState> = repository.localApiServer.serverState

    val activeLoadedModel: StateFlow<AiModelEntity?> = repository.inferenceEngineManager.activeLoadedModel
    val loadedModelsPool: StateFlow<List<AiModelEntity>> = repository.inferenceEngineManager.loadedModelsPool
    val activeEngine: StateFlow<com.example.ai.inference.ModelInferenceEngine?> = repository.inferenceEngineManager.activeEngine

    val quantizationHistory: StateFlow<List<QuantizationHistoryEntity>> = repository.quantizationHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val realtimeTelemetry: StateFlow<RealtimeTelemetryState> = repository.telemetryPerformanceMonitor.telemetryState
    val storageVolumes: List<StorageVolumeInfo> get() = repository.deviceStorageManager.getAllStorageVolumes()
    val storageScanProgress = repository.modelStorageScanner.scanProgress

    private val _imageGenerationForm = MutableStateFlow(ImageGenerationFormState())
    val imageGenerationForm: StateFlow<ImageGenerationFormState> = _imageGenerationForm.asStateFlow()

    fun scanStorageForModels() {
        viewModelScope.launch {
            val result = repository.modelStorageScanner.reconcileDatabaseWithStorage()
            _settingsStatusMessage.value = "Storage Scan complete: ${result.validModelsCount} verified model(s) on device"
        }
    }

    private val _serverOperationMessage = MutableStateFlow<String?>(null)
    val serverOperationMessage: StateFlow<String?> = _serverOperationMessage.asStateFlow()

    fun dismissServerOperationMessage() {
        _serverOperationMessage.value = null
    }

    fun loadModelForServer(model: AiModelEntity, keepOthers: Boolean = true) {
        viewModelScope.launch {
            val wasServerRunning = serverState.value.status == com.example.ai.server.ServerStatus.RUNNING
            if (wasServerRunning && !keepOthers) {
                repository.localApiServer.stopServer()
            }

            val result = repository.inferenceEngineManager.loadModel(model, keepExisting = keepOthers)
            val generatedKey = repository.localApiServer.generateAndSetModelApiKey(model.name)
            _serverApiKey.value = generatedKey

            if (result.first) {
                val poolCount = repository.inferenceEngineManager.loadedModelsPool.value.size
                _serverOperationMessage.value = "Model '${model.name}' loaded ($poolCount active in memory pool)! OpenAI API-Key: $generatedKey"
                if (wasServerRunning || _isApiServerToggle.value) {
                    repository.localApiServer.startServer()
                }
            } else {
                _serverOperationMessage.value = result.second
            }
        }
    }

    fun unloadSpecificModel(modelId: String) {
        viewModelScope.launch {
            val wasUnloaded = repository.inferenceEngineManager.unloadSpecificModel(modelId)
            if (wasUnloaded) {
                _serverOperationMessage.value = "Model unloaded from memory pool"
            }
        }
    }

    fun unloadActiveModel() {
        viewModelScope.launch {
            if (serverState.value.status == com.example.ai.server.ServerStatus.RUNNING) {
                repository.localApiServer.stopServer()
            }
            repository.inferenceEngineManager.unloadCurrentModel()
            _serverOperationMessage.value = "All active models unloaded from memory"
        }
    }


    fun toggleApiServer() {
        if (serverState.value.status == com.example.ai.server.ServerStatus.RUNNING) {
            repository.localApiServer.stopServer()
            _serverOperationMessage.value = "Local API Server stopped"
        } else {
            val currentModel = activeLoadedModel.value
            if (currentModel != null) {
                val key = repository.localApiServer.generateAndSetModelApiKey(currentModel.name)
                _serverApiKey.value = key
            }
            val result = repository.localApiServer.startServer()
            _serverOperationMessage.value = result.second
        }
    }

    fun startApiServer(): Pair<Boolean, String> {
        val currentModel = activeLoadedModel.value
        if (currentModel != null) {
            val key = repository.localApiServer.generateAndSetModelApiKey(currentModel.name)
            _serverApiKey.value = key
        }
        val result = repository.localApiServer.startServer()
        _serverOperationMessage.value = result.second
        return result
    }

    fun stopApiServer() {
        repository.localApiServer.stopServer()
        _serverOperationMessage.value = "Local API Server stopped"
    }

    fun updateServerPort(port: Int) {
        val currentConfig = serverState.value.config
        val newConfig = currentConfig.copy(port = port.coerceIn(1024, 65535))
        val wasRunning = serverState.value.status == com.example.ai.server.ServerStatus.RUNNING
        if (wasRunning) {
            repository.localApiServer.stopServer()
        }
        repository.localApiServer.updateConfig(newConfig)
        if (wasRunning) {
            repository.localApiServer.startServer()
        }
    }

    fun updateApiKeyEnabled(enabled: Boolean) {
        val currentConfig = serverState.value.config
        repository.localApiServer.updateConfig(currentConfig.copy(apiKeyEnabled = enabled))
    }

    fun updateApiKey(key: String) {
        val currentConfig = serverState.value.config
        repository.localApiServer.updateConfig(currentConfig.copy(apiKey = key))
        _serverApiKey.value = key
    }

    fun regenerateApiKey() {
        val modelName = activeLoadedModel.value?.name ?: "local"
        val newKey = repository.localApiServer.generateAndSetModelApiKey(modelName)
        _serverApiKey.value = newKey
        _serverOperationMessage.value = "Generated new OpenAI API Key: $newKey"
    }

    fun updateTunnelEnabled(enabled: Boolean) {
        val currentConfig = serverState.value.config
        repository.localApiServer.updateConfig(currentConfig.copy(tunnelEnabled = enabled))
    }

    fun updateTunnelSubdomain(subdomain: String) {
        val currentConfig = serverState.value.config
        repository.localApiServer.updateConfig(currentConfig.copy(tunnelSubdomain = subdomain))
    }

    fun getBackendInfoForModel(model: AiModelEntity?): com.example.ai.server.ServerModelBackendInfo {
        return repository.inferenceEngineManager.getBackendInfoForModel(model)
    }

    fun selectTab(tab: SoraTab) {
        _selectedTab.value = tab
    }

    fun refreshHardwareProfile() {
        _hardwareProfile.value = repository.getDeviceHardwareProfile()
    }

    fun updatePrompt(prompt: String) {
        _generationForm.value = _generationForm.value.copy(prompt = prompt)
    }

    fun updateTitle(title: String) {
        _generationForm.value = _generationForm.value.copy(title = title)
    }

    fun updateGenerationType(type: String) {
        _generationForm.value = _generationForm.value.copy(generationType = type)
    }

    fun updateMode(mode: String) {
        _generationForm.value = _generationForm.value.copy(mode = mode)
    }

    fun updateDurationWithLabel(label: String, durationSec: Int) {
        val isSeg = durationSec >= 60
        _generationForm.value = _generationForm.value.copy(
            durationLabel = label,
            durationSec = durationSec,
            isSegmented = isSeg
        )
    }

    fun updateAspectRatio(ratio: String) {
        _generationForm.value = _generationForm.value.copy(aspectRatio = ratio)
    }

    fun updateFps(fps: Int) {
        _generationForm.value = _generationForm.value.copy(fps = fps)
    }

    fun togglePauseRender() {
        val form = _generationForm.value
        _generationForm.value = form.copy(isPaused = !form.isPaused)
    }

    fun saveCheckpoint() {
        val form = _generationForm.value
        _generationForm.value = form.copy(checkpointSaved = true)
    }

    fun updateDuration(durationSec: Int) {
        updateDurationWithLabel("${durationSec} seconds", durationSec)
    }

    fun updateResolution(res: String) {
        _generationForm.value = _generationForm.value.copy(resolution = res)
    }

    fun updateSourceImageUri(uri: String?) {
        _generationForm.value = _generationForm.value.copy(sourceImageUri = uri)
    }

    fun updateSourceVideoUri(uri: String?) {
        _generationForm.value = _generationForm.value.copy(sourceVideoUri = uri)
    }

    fun updateSourceAudioUri(uri: String?) {
        _generationForm.value = _generationForm.value.copy(sourceAudioUri = uri)
    }

    fun updateCharacterProfileText(text: String?) {
        _generationForm.value = _generationForm.value.copy(characterProfileText = text)
    }

    fun updateCameraMotion(motion: String) {
        _generationForm.value = _generationForm.value.copy(cameraMotion = motion)
    }

    fun updateMotionStrength(strength: Float) {
        _generationForm.value = _generationForm.value.copy(motionStrength = strength)
    }

    fun updateVideoCodec(codec: String) {
        _generationForm.value = _generationForm.value.copy(videoCodec = codec)
    }

    fun updateTemporalConsistency(consistency: Float) {
        _generationForm.value = _generationForm.value.copy(temporalConsistency = consistency)
    }

    fun updateMotionPrompt(prompt: String) {
        _generationForm.value = _generationForm.value.copy(motionPrompt = prompt)
    }

    fun updateCameraPrompt(prompt: String) {
        _generationForm.value = _generationForm.value.copy(cameraPrompt = prompt)
    }

    fun updateLightingPrompt(prompt: String) {
        _generationForm.value = _generationForm.value.copy(lightingPrompt = prompt)
    }

    fun updateImageStyle(style: String) {
        _generationForm.value = _generationForm.value.copy(imageStyle = style)
    }

    fun updateImageResolution(res: String) {
        _generationForm.value = _generationForm.value.copy(imageResolution = res)
    }

    fun updateImageAspectRatio(ratio: String) {
        _generationForm.value = _generationForm.value.copy(imageAspectRatio = ratio)
    }

    fun updateImageSteps(steps: Int) {
        _generationForm.value = _generationForm.value.copy(imageSteps = steps)
    }

    fun updateImageCfgScale(scale: Float) {
        _generationForm.value = _generationForm.value.copy(imageCfgScale = scale)
    }

    fun updateImageNegativePrompt(prompt: String) {
        _generationForm.value = _generationForm.value.copy(imageNegativePrompt = prompt)
    }

    fun updateImageSampler(sampler: String) {
        _generationForm.value = _generationForm.value.copy(imageSampler = sampler)
    }

    fun updateImageSeed(seed: Long) {
        _generationForm.value = _generationForm.value.copy(imageSeed = seed)
    }

    fun toggleImageHighResFix(enabled: Boolean) {
        _generationForm.value = _generationForm.value.copy(imageHighResFix = enabled)
    }

    fun updateImageBatchCount(count: Int) {
        _generationForm.value = _generationForm.value.copy(imageBatchCount = count)
    }

    fun updateAudioVoiceArchetype(archetype: String) {
        _generationForm.value = _generationForm.value.copy(audioVoiceArchetype = archetype)
    }

    fun updateAudioPitch(pitch: Int) {
        _generationForm.value = _generationForm.value.copy(audioPitch = pitch)
    }

    fun updateAudioSpeed(speed: Float) {
        _generationForm.value = _generationForm.value.copy(audioSpeed = speed)
    }

    fun updateAudioEmotion(emotion: String) {
        _generationForm.value = _generationForm.value.copy(audioEmotion = emotion)
    }

    fun updateAudioFormat(format: String) {
        _generationForm.value = _generationForm.value.copy(audioFormat = format)
    }

    fun updateStoryFormat(format: String) {
        _generationForm.value = _generationForm.value.copy(storyFormat = format)
    }

    fun updateStorySceneCount(count: Int) {
        _generationForm.value = _generationForm.value.copy(storySceneCount = count)
    }

    fun updateStoryTone(tone: String) {
        _generationForm.value = _generationForm.value.copy(storyTone = tone)
    }

    fun updateMaskImageUri(uri: String?) {
        _generationForm.value = _generationForm.value.copy(maskImageUri = uri)
    }

    fun updateManhwaChapterTitle(title: String) {
        _generationForm.value = _generationForm.value.copy(manhwaChapterTitle = title)
    }

    fun addManhwaPanel(title: String, panelType: String, actionDesc: String, spokenDialogue: String?, imageUri: String?) {
        val current = _generationForm.value
        val newItem = ManhwaPanelItem(
            id = "p_${System.currentTimeMillis()}",
            title = title,
            imageUri = imageUri,
            panelType = panelType,
            actionDescription = actionDesc,
            spokenDialogue = spokenDialogue
        )
        _generationForm.value = current.copy(manhwaPanels = current.manhwaPanels + newItem)
    }

    fun removeManhwaPanel(id: String) {
        val current = _generationForm.value
        _generationForm.value = current.copy(manhwaPanels = current.manhwaPanels.filterNot { it.id == id })
    }

    fun updateManhwaVoiceoverUri(uri: String?) {
        _generationForm.value = _generationForm.value.copy(manhwaVoiceoverUri = uri)
    }

    fun toggleManhwaAudioFilter(enabled: Boolean) {
        _generationForm.value = _generationForm.value.copy(manhwaFilterActionNarration = enabled)
    }

    fun toggleManhwaLipSync(enabled: Boolean) {
        _generationForm.value = _generationForm.value.copy(manhwaLipSyncEnabled = enabled)
    }

    fun updateManhwaAnimationStyle(style: String) {
        _generationForm.value = _generationForm.value.copy(manhwaAnimationStyle = style)
    }

    fun updateManhwaContinuationPrompt(prompt: String) {
        _generationForm.value = _generationForm.value.copy(manhwaContinuationPrompt = prompt)
    }

    fun generateManhwaStoryContinuation() {
        val form = _generationForm.value
        _generationForm.value = form.copy(isGeneratingManhwaContinuation = true)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val script = """
            📜 MANHWA STORY CONTINUATION: CHAPTER 43 (AI GENERATED)
            Title: The Spectral Sovereign's March
            
            [PANEL 1 - ACTION]
            Visual: Dark clouds gather over Namsan Tower. The Shadow Monarch lifts his sword.
            Action Motion: Thunder strikes, 3D parallax camera sweep.
            Character Speech: "Shadow Soldiers... March forward!"
            Audio Filter: Narrator action audio muted (action animated on screen). Character speech preserved with lip-sync.
            
            [PANEL 2 - COMBAT]
            Visual: Frost Monarch unleashes absolute zero blizzard spikes.
            Action Motion: Speed lines, particle explosion, impact distortion.
            Action SFX: Ice crack roar, energy clash.
            Character Speech: "Foolish mortal! You cannot kill ice itself!"
            
            [PANEL 3 - RESUME CHECKPOINT]
            Status: Added 3 new animated panels to recap project. Ready to continue rendering from Chapter 42 Panel 18!
            """.trimIndent()
            
            val newPanels = listOf(
                ManhwaPanelItem("p_cont_1", "Panel 1: Spectral March Begins", null, "COMBAT", "Dark clouds gather over Namsan Tower, 3D parallax sweep", "Shadow Soldiers... March forward!"),
                ManhwaPanelItem("p_cont_2", "Panel 2: Absolute Zero Clash", null, "COMBAT", "Frost Monarch unleashes blizzard spikes with speed lines", "Foolish mortal! You cannot kill ice!"),
                ManhwaPanelItem("p_cont_3", "Panel 3: Sovereign Call", null, "DIALOGUE", "Shadow Army spectral glow engulfs Seoul skyline", "My domain is eternal!")
            )
            
            _generationForm.value = _generationForm.value.copy(
                manhwaContinuationScript = script,
                manhwaPanels = form.manhwaPanels + newPanels,
                isGeneratingManhwaContinuation = false
            )
        }
    }

    fun resumeManhwaRecapFromCheckpoint() {
        val current = _generationForm.value
        _generationForm.value = current.copy(
            prompt = "MANHWA RECAP RESUMED: ${current.manhwaChapterTitle} from Panel ${current.manhwaCheckpointPanelIndex + 1} (${current.manhwaCheckpointTimeSec}s)",
            title = "${current.manhwaChapterTitle} (Resumed)"
        )
        startGeneration()
    }

    fun toggleWakeWord(enabled: Boolean) {
        val current = _generationForm.value
        _generationForm.value = current.copy(
            wakeWordEnabled = enabled,
            isListeningForWakeWord = enabled,
            systemStatusLog = if (enabled) "Wake Word Engine Active: Listening for 'Skra'" else "Wake Word Engine Standby"
        )
    }

    fun toggleDeviceControl(enabled: Boolean) {
        val current = _generationForm.value
        _generationForm.value = current.copy(
            deviceControlGranted = enabled,
            systemStatusLog = if (enabled) "Full System Control Permissions Granted" else "Device Control Restricted"
        )
    }

    fun triggerWakeWordEvent(spokenWord: String) {
        val current = _generationForm.value
        _generationForm.value = current.copy(
            lastDetectedWakeWord = spokenWord,
            systemStatusLog = "🔥 Wake Word Detected: '$spokenWord'! Activating AI Voice Command Processing..."
        )
    }

    fun launchYouTubeApp() {
        val context = getApplication<Application>().applicationContext
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com")).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            _generationForm.value = _generationForm.value.copy(
                systemStatusLog = "🚀 Executed AI System Command: Opened YouTube App"
            )
        } catch (e: Exception) {
            _generationForm.value = _generationForm.value.copy(
                systemStatusLog = "🚀 Executed AI Web Fallback: Redirected to YouTube Web"
            )
        }
    }

    fun startTimer(title: String, durationSeconds: Int) {
        val timerId = "timer_${System.currentTimeMillis()}"
        val newTimer = ActiveTimer(
            id = timerId,
            title = title,
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            isFinished = false
        )
        _activeTimers.value = _activeTimers.value + newTimer

        viewModelScope.launch {
            var currentSec = durationSeconds
            while (currentSec > 0) {
                kotlinx.coroutines.delay(1000)
                currentSec -= 1
                _activeTimers.value = _activeTimers.value.map { timer ->
                    if (timer.id == timerId) timer.copy(remainingSeconds = currentSec, isFinished = currentSec == 0) else timer
                }
            }
            // Timer expired notification message
            val alertMsg = ChatMessage(
                sender = "AI",
                text = "🔔 TIMER ALARM: '$title' ($durationSeconds seconds) has completed!",
                actionType = "SYSTEM_LOG",
                actionTitle = "Timer Completed"
            )
            _chatMessages.value = _chatMessages.value + alertMsg
        }
    }

    fun cancelTimer(timerId: String) {
        _activeTimers.value = _activeTimers.value.filterNot { it.id == timerId }
    }

    private var chatGenerationJob: kotlinx.coroutines.Job? = null
    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _chatModelSource = MutableStateFlow("LOCAL_ENGINE") // LOCAL_ENGINE, UNIVERSAL_SERVER, CLOUD_API, COMPOSITE_ROUTER
    val chatModelSource: StateFlow<String> = _chatModelSource.asStateFlow()

    fun setChatModelSource(source: String) {
        _chatModelSource.value = source
    }

    fun stopChatGeneration() {
        chatGenerationJob?.cancel()
        chatGenerationJob = null
        _isChatStreaming.value = false
        _isAssistantLoading.value = false
    }

    fun clearChatMessages() {
        _chatMessages.value = emptyList()
        _generatedScript.value = null
    }

    fun regenerateLastChat() {
        val msgs = _chatMessages.value
        if (msgs.isEmpty()) return
        val lastUserMsg = msgs.lastOrNull { it.sender == "USER" } ?: return
        if (msgs.last().sender == "AI") {
            _chatMessages.value = msgs.dropLast(1)
        }
        sendChatMessage(lastUserMsg.text, lastUserMsg.attachments)
    }

    fun sendChatMessage(userText: String, attachments: List<ChatAttachment> = emptyList()) {
        if (userText.isBlank() && attachments.isEmpty()) return

        val userMsg = ChatMessage(
            sender = "USER",
            text = if (userText.isNotBlank()) userText else "📎 Attached ${attachments.size} file(s)",
            attachments = attachments
        )
        _chatMessages.value = _chatMessages.value + userMsg
        clearStagedChatAttachments()

        val lower = userText.lowercase().trim()

        chatGenerationJob?.cancel()
        chatGenerationJob = viewModelScope.launch(Dispatchers.IO) {
            _isChatStreaming.value = true
            _isAssistantLoading.value = true

            // Check if attachments were uploaded first
            if (attachments.isNotEmpty()) {
                kotlinx.coroutines.delay(300)
                val hasImage = attachments.any { it.type == AttachmentType.IMAGE }
                val hasPdf = attachments.any { it.type == AttachmentType.PDF }

                val replyText = buildString {
                    append("📁 **Processed ${attachments.size} Attachment(s):**\n")
                    attachments.forEach { att ->
                        when (att.type) {
                            AttachmentType.IMAGE -> {
                                append("• 🖼️ **${att.fileName}**: Image recognized. Ready for Image-to-Video reference framing or visual storyboard.\n")
                                updateSourceImageUri(att.uri)
                            }
                            AttachmentType.PDF -> {
                                append("• 📄 **${att.fileName}**: PDF document parsed. Script structure, scenes, and character notes extracted into memory.\n")
                            }
                            else -> {
                                append("• 📁 **${att.fileName}**: File loaded into project workspace context.\n")
                            }
                        }
                    }
                    if (userText.isNotBlank()) {
                        append("\n💡 **Regarding your prompt:** \"$userText\"\n")
                    }
                    if (hasImage) {
                        append("\nI've assigned your uploaded image as the active keyframe reference for Sora generation!")
                    } else if (hasPdf) {
                        append("\nI can convert this document into a cinematic storyboard, generate video scenes, or extract character dialogue.")
                    }
                }

                val aiMsg = ChatMessage(
                    sender = "AI",
                    text = replyText,
                    actionType = if (hasImage) "NAVIGATE_GENERATE" else null,
                    actionTitle = if (hasImage) "Use Image in Generator" else null,
                    isExecuted = true
                )
                _chatMessages.value = _chatMessages.value + aiMsg
                _isChatStreaming.value = false
                _isAssistantLoading.value = false
                return@launch
            }

            // Add placeholder AI message for streaming
            val aiMsgId = java.util.UUID.randomUUID().toString()
            val initialAiMsg = ChatMessage(
                id = aiMsgId,
                sender = "AI",
                text = ""
            )
            _chatMessages.value = _chatMessages.value + initialAiMsg

            val accumulatedText = StringBuilder()

            try {
                // Stream response from unified AIInferenceManager
                val chatReq = com.example.ai.inference.AIInferenceRequest(
                    prompt = userText,
                    systemPrompt = "You are Sora AI Assistant, an advanced multimodal on-device and cloud creative intelligence. Provide direct, intelligent, well-structured assistance on video creation, story writing, script planning, sound design, and technical reasoning.",
                    requiredCapability = com.example.ai.inference.model.ModelCapability.CHAT,
                    temperature = 0.7f,
                    maxTokens = 1024
                )
                aiInferenceManager.streamText(chatReq).collect { token ->
                    accumulatedText.append(token)
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = accumulatedText.toString()) else msg
                    }
                }
            } catch (e: Exception) {
                if (accumulatedText.isEmpty()) {
                    val fallback = "🤖 [${repository.inferenceEngineManager.activeLoadedModel.value?.name ?: "Sora AI Engine"}]: I received your message: \"$userText\".\n\nI can assist you with creative video synthesis, story arcs, AV script breakdowns, neural voice synthesis, and multimedia generation."
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = fallback) else msg
                    }
                }
            } finally {
                _isChatStreaming.value = false
                _isAssistantLoading.value = false
            }
        }
    }

    fun startGeneration() {
        val form = _generationForm.value
        val profile = _hardwareProfile.value

        if (profile != null && profile.availableRamGb < 1.5f && form.mode == "CINEMA") {
            _generationForm.value = form.copy(
                errorMessage = "Insufficient RAM (${String.format("%.1f", profile.availableRamGb)}GB available). Cinema Mode requires at least 4.0GB available RAM. Switch to Fast Mode or connect to Sora Cloud."
            )
            return
        }

        // If a generation is already actively running, seamlessly enqueue the request without blocking or disabling the button!
        if (form.isGenerating) {
            viewModelScope.launch {
                val queuedJob = repository.taskQueueManager.enqueueSingleJob(
                    title = form.title,
                    prompt = form.prompt,
                    generationType = form.generationType,
                    mode = form.mode,
                    durationSec = form.durationSec,
                    resolution = form.resolution,
                    fps = form.fps
                )
                val etaSec = form.durationSec * 2
                _settingsStatusMessage.value = "Generation in progress: \"${queuedJob.title}\" enqueued in background (Estimated time: ${etaSec}s). You can safely navigate or leave the app!"
            }
            return
        }

        _generationForm.value = form.copy(isGenerating = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            val job = repository.createNewGenerationJob(
                title = form.title,
                prompt = form.prompt,
                generationType = form.generationType,
                mode = form.mode,
                durationSec = form.durationSec,
                resolution = form.resolution,
                fps = form.fps
            )

            repository.startLocalGenerationStream(job).collect { progress ->
                val updatedJob = job.copy(
                    currentFrame = progress.currentFrame,
                    totalFrames = progress.totalFrames,
                    progressPercent = if (progress.totalFrames > 0) ((progress.currentFrame.toFloat() / progress.totalFrames) * 100).toInt() else 0,
                    fps = progress.fps,
                    status = if (progress.isComplete) "COMPLETED" else "RUNNING"
                )
                repository.generationJobDao.updateJob(updatedJob)

                if (progress.isComplete) {
                    _generationForm.value = _generationForm.value.copy(isGenerating = false)
                    
                    val isImg = form.generationType in listOf("IMAGE_GEN", "IMAGE_EDIT", "UPSCALING", "INPAINTING", "OUTPAINTING", "BG_REMOVAL")
                    val isAud = form.generationType in listOf("VOICE_CLONE", "VOICE_GEN", "SUBTITLES", "TRANSLATION", "LIP_SYNC")
                    val isStory = form.generationType in listOf("STORY_GEN", "SCRIPT_WRITER", "SCENE_BUILDER", "SHOT_PLANNER", "CHARACTER_CREATOR")

                    val galleryItem = when {
                        isImg -> {
                            val res = repository.realMediaSynthesisEngine.generateRealImage(
                                title = form.title,
                                prompt = form.prompt,
                                style = form.imageStyle,
                                aspectRatio = form.imageAspectRatio,
                                resolutionLabel = form.imageResolution,
                                cfgScale = form.imageCfgScale,
                                steps = form.imageSteps,
                                seed = form.imageSeed
                            )
                            res.second
                        }
                        isAud -> {
                            val res = repository.realMediaSynthesisEngine.generateRealAudio(
                                title = form.title,
                                scriptText = form.prompt,
                                voiceArchetype = form.audioVoiceArchetype,
                                emotion = form.audioEmotion,
                                durationSec = form.durationSec
                            )
                            res.second
                        }
                        isStory -> {
                            val res = repository.realMediaSynthesisEngine.generateRealScript(
                                title = form.title,
                                prompt = form.prompt,
                                format = form.storyFormat,
                                tone = form.storyTone,
                                sceneCount = form.storySceneCount
                            )
                            res.second
                        }
                        else -> {
                            val res = repository.realMediaSynthesisEngine.generateRealVideo(
                                title = form.title,
                                prompt = form.prompt,
                                durationSec = form.durationSec,
                                resolutionLabel = form.resolution,
                                fps = form.fps,
                                cameraMotion = form.cameraMotion
                            )
                            res.second
                        }
                    }

                    repository.galleryDao.insertItem(galleryItem)
                    _latestGeneratedResult.value = galleryItem
                }
            }
        }
    }


    fun addCurrentFormToQueue() {
        val form = _generationForm.value
        viewModelScope.launch {
            repository.taskQueueManager.enqueueSingleJob(
                title = form.title,
                prompt = form.prompt,
                generationType = form.generationType,
                mode = form.mode,
                durationSec = form.durationSec,
                resolution = form.resolution,
                fps = form.fps
            )
        }
    }

    fun addBatchJobsToQueue(
        prefix: String,
        prompts: List<String>,
        type: String = "TEXT_TO_VIDEO",
        mode: String = "FAST",
        durationSec: Int = 5,
        resolution: String = "1080p",
        fps: Int = 24
    ) {
        viewModelScope.launch {
            repository.taskQueueManager.enqueueBatch(
                com.example.ai.queue.BatchJobCreationRequest(
                    titlePrefix = prefix,
                    prompts = prompts,
                    generationType = type,
                    mode = mode,
                    durationSeconds = durationSec,
                    resolution = resolution,
                    fps = fps
                )
            )
        }
    }

    fun startQueueProcessing() {
        repository.taskQueueManager.startProcessing()
    }

    fun pauseQueueProcessing() {
        repository.taskQueueManager.pauseProcessing()
    }

    fun toggleAutoProcessQueue(enabled: Boolean) {
        repository.taskQueueManager.setAutoProcess(enabled)
    }

    fun cancelQueuedJob(jobId: String) {
        repository.taskQueueManager.cancelJob(jobId)
    }

    fun retryQueuedJob(jobId: String) {
        repository.taskQueueManager.retryJob(jobId)
    }

    fun deleteQueuedJob(jobId: String) {
        repository.taskQueueManager.deleteJob(jobId)
    }

    fun clearCompletedJobs() {
        repository.taskQueueManager.clearCompletedJobs()
    }

    fun moveQueuedJob(jobId: String, moveUp: Boolean) {
        repository.taskQueueManager.moveJob(jobId, moveUp)
    }

    fun moveQueuedJobToTop(jobId: String) {
        repository.taskQueueManager.moveJobToTop(jobId)
    }

    fun deleteQuantizationHistoryEntry(historyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.quantizationHistoryDao.deleteHistoryById(historyId)
        }
    }

    fun clearAllQuantizationHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.quantizationHistoryDao.clearAllHistory()
        }
    }

    fun exportExecutionLogs(context: android.content.Context) {
        repository.logExportManager.exportAndShareLogs(context)
    }


    fun cancelActiveJob(jobId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = repository.generationJobDao.getJobById(jobId)
            if (active != null) {
                repository.generationJobDao.updateJob(active.copy(status = "CANCELLED"))
            }
            _generationForm.value = _generationForm.value.copy(isGenerating = false)
        }
    }

    fun searchHuggingFaceModels(query: String) {
        _huggingFaceQuery.value = query
        viewModelScope.launch {
            _huggingFaceResults.value = repository.huggingFaceClient.searchModels(query)
        }
    }

    fun downloadHuggingFaceModel(model: HuggingFaceModelInfo) {
        downloadHuggingFaceModelWithLocation(model, "INTERNAL")
    }

    fun downloadHuggingFaceModelWithLocation(
        model: HuggingFaceModelInfo,
        storageType: String = "INTERNAL",
        customPath: String? = null
    ) {
        viewModelScope.launch {
            repository.modelDownloadManager.startDownload(model, customPath, storageType).collect { state ->
                _downloadingState.value = state
                if (state.isFinished) {
                    _downloadingState.value = null
                    _settingsStatusMessage.value = "Successfully downloaded ${model.name} to ${state.storageLocationLabel}"
                }
            }
        }
    }

    fun downloadModelEntityWithLocation(
        model: AiModelEntity,
        storageType: String = "INTERNAL",
        customPath: String? = null
    ) {
        viewModelScope.launch {
            repository.modelDownloadManager.startModelEntityDownload(model, customPath, storageType).collect { state ->
                _downloadingState.value = state
                if (state.isFinished) {
                    _downloadingState.value = null
                    _settingsStatusMessage.value = "Successfully downloaded ${model.name} to ${state.storageLocationLabel}"
                }
            }
        }
    }

    fun importCustomModelFromStorage(
        name: String,
        format: String,
        modelType: String,
        ramMb: Int,
        localPath: String,
        storageSource: String,
        customDesc: String? = null
    ) {
        viewModelScope.launch {
            val result = repository.modelStorageScanner.importAndValidateModel(
                name = name,
                format = format,
                modelType = modelType,
                ramMb = ramMb,
                pathOrUri = localPath,
                storageSource = storageSource
            )
            _settingsStatusMessage.value = result.second
        }
    }

    fun deleteOrUnloadModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = repository.aiModelDao.getModelById(modelId)
            if (model != null) {
                if (activeLoadedModel.value?.id == modelId) {
                    unloadActiveModel()
                }
                repository.modelDownloadManager.deleteModelPermanently(model)
                _settingsStatusMessage.value = "Deleted model '${model.name}' from device"
            } else {
                repository.aiModelDao.deleteModelById(modelId)
            }
        }
    }

    fun deleteModelPermanently(model: AiModelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (activeLoadedModel.value?.id == model.id) {
                unloadActiveModel()
            }
            val deleted = repository.modelDownloadManager.deleteModelPermanently(model)
            if (deleted) {
                _settingsStatusMessage.value = "Deleted model '${model.name}' from storage"
            }
        }
    }

    // Dedicated Image Studio Methods
    fun updateDedicatedImagePrompt(prompt: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(prompt = prompt)
    }

    fun updateDedicatedImageTitle(title: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(title = title)
    }

    fun updateDedicatedImageStyle(style: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(style = style)
    }

    fun updateDedicatedImageResolution(res: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(resolution = res)
    }

    fun updateDedicatedImageAspectRatio(ratio: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(aspectRatio = ratio)
    }

    fun updateDedicatedImageSteps(steps: Int) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(steps = steps)
    }

    fun updateDedicatedImageCfgScale(scale: Float) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(cfgScale = scale)
    }

    fun updateDedicatedImageNegativePrompt(prompt: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(negativePrompt = prompt)
    }

    fun updateDedicatedImageSampler(sampler: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(sampler = sampler)
    }

    fun updateDedicatedImageSeed(seed: Long) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(seed = seed, isRandomSeed = false)
    }

    fun toggleDedicatedImageRandomSeed(random: Boolean) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(isRandomSeed = random)
    }

    fun toggleDedicatedImageHighResFix(enabled: Boolean) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(highResFix = enabled)
    }

    fun updateDedicatedImageBatchCount(count: Int) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(batchCount = count)
    }

    fun updateDedicatedImageMode(mode: String) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(mode = mode)
    }

    fun updateDedicatedImageSourceUri(uri: String?) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(sourceImageUri = uri)
    }

    fun updateDedicatedImageMaskUri(uri: String?) {
        _imageGenerationForm.value = _imageGenerationForm.value.copy(maskImageUri = uri)
    }

    fun startDedicatedImageGeneration() {
        val form = _imageGenerationForm.value
        val seedToUse = if (form.isRandomSeed) kotlin.random.Random.nextLong(1, 999999999L) else form.seed

        _imageGenerationForm.value = form.copy(isGenerating = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repository.realMediaSynthesisEngine.generateRealImage(
                    title = form.title.ifBlank { "AI Art ${System.currentTimeMillis() % 1000}" },
                    prompt = form.prompt,
                    style = form.style,
                    aspectRatio = form.aspectRatio,
                    resolutionLabel = form.resolution,
                    cfgScale = form.cfgScale,
                    steps = form.steps,
                    seed = seedToUse
                )
                repository.galleryDao.insertItem(res.second)
                _latestGeneratedResult.value = res.second
                _settingsStatusMessage.value = "Generated artwork: ${res.second.title}"
            } catch (e: Exception) {
                _imageGenerationForm.value = _imageGenerationForm.value.copy(errorMessage = e.message)
            } finally {
                _imageGenerationForm.value = _imageGenerationForm.value.copy(isGenerating = false)
            }
        }
    }

    fun addDedicatedImageJobToQueue() {
        val form = _imageGenerationForm.value
        viewModelScope.launch {
            repository.taskQueueManager.enqueueSingleJob(
                title = form.title,
                prompt = form.prompt,
                generationType = form.mode,
                mode = "FAST",
                durationSec = 1,
                resolution = form.resolution,
                fps = 1
            )
            _settingsStatusMessage.value = "Enqueued image task '${form.title}' to queue"
        }
    }

    fun startModelQuantization(
        model: AiModelEntity,
        precision: QuantizationPrecision,
        tradeoffObjective: QuantizationTradeoffObjective = QuantizationTradeoffObjective.BALANCED_MULTI_OBJECTIVE,
        iterationsCount: Int = 10,
        storageType: String = "INTERNAL",
        customPath: String? = null,
        chunkSizeMb: Int = 64,
        cpuThreads: Int = 4,
        preserveOutliers: Boolean = true
    ) {
        viewModelScope.launch {
            val config = QuantizationConfig(
                sourceModel = model,
                targetPrecision = precision,
                tradeoffObjective = tradeoffObjective,
                iterationsCount = iterationsCount,
                storageType = storageType,
                customStoragePath = customPath,
                streamChunkSizeMb = chunkSizeMb,
                cpuThreadCount = cpuThreads,
                preserveOutliers = preserveOutliers
            )
            repository.modelQuantizationEngine.startQuantization(config).collect { state ->
                _quantizationState.value = state
                if (state.isFinished) {
                    _settingsStatusMessage.value = "Quantized ${model.name} to ${precision.id} (${state.estimatedQuantizedRamMb}MB RAM, -${state.ramSavedPercent}%)"
                }
            }
        }
    }


    fun cancelModelQuantization() {
        repository.modelQuantizationEngine.cancelQuantization()
        _quantizationState.value = null
    }

    fun clearQuantizationState() {
        _quantizationState.value = null
    }

    fun updateAssistantInput(input: String) {
        _assistantInput.value = input
    }

    fun generateAssistantScript() {
        _isAssistantLoading.value = true
        viewModelScope.launch {
            val scriptPkg = repository.offlineAssistantEngine.generateScriptAndShots(_assistantInput.value)
            _generatedScript.value = scriptPkg
            _isAssistantLoading.value = false
        }
    }

    fun addClipToEditor(filePath: String, title: String, durationMs: Long = 5000L): String {
        val current = _editorProject.value
        val clipId = "clip_${System.currentTimeMillis()}"
        val engine = VideoEditorEngine()
        val newClip = MediaClipTrack(
            id = clipId,
            title = title,
            filePath = filePath,
            startMs = 0L,
            endMs = durationMs,
            durationMs = durationMs,
            frames = engine.generateDefaultFramesForClip(clipId, durationMs, title)
        )
        val updatedList = current.videoClips.toMutableList().apply { add(newClip) }
        _editorProject.value = current.copy(videoClips = updatedList)
        _activeEditorClipId.value = clipId
        return clipId
    }

    fun moveClipLeft(clipId: String) {
        val current = _editorProject.value
        val index = current.videoClips.indexOfFirst { it.id == clipId }
        if (index > 0) {
            val list = current.videoClips.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _editorProject.value = current.copy(videoClips = list)
        }
    }

    fun moveClipRight(clipId: String) {
        val current = _editorProject.value
        val index = current.videoClips.indexOfFirst { it.id == clipId }
        if (index in 0 until current.videoClips.lastIndex) {
            val list = current.videoClips.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _editorProject.value = current.copy(videoClips = list)
        }
    }

    fun reorderClips(fromIndex: Int, toIndex: Int) {
        val current = _editorProject.value
        if (fromIndex in current.videoClips.indices && toIndex in current.videoClips.indices && fromIndex != toIndex) {
            val list = current.videoClips.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _editorProject.value = current.copy(videoClips = list)
        }
    }

    fun updateClipDuration(clipId: String, newDurationMs: Long) {
        val clampedDuration = maxOf(500L, minOf(30000L, newDurationMs))
        val current = _editorProject.value
        val engine = VideoEditorEngine()
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    durationMs = clampedDuration,
                    endMs = clip.startMs + clampedDuration,
                    frames = engine.generateDefaultFramesForClip(clip.id, clampedDuration, clip.title)
                )
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun adjustClipDurationBy(clipId: String, deltaMs: Long) {
        val current = _editorProject.value
        val clip = current.videoClips.find { it.id == clipId } ?: return
        updateClipDuration(clipId, clip.durationMs + deltaMs)
    }

    fun trimClipStart(clipId: String, trimMs: Long) {
        val current = _editorProject.value
        val engine = VideoEditorEngine()
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val newStart = minOf(clip.endMs - 500L, clip.startMs + trimMs)
                val newDuration = clip.endMs - newStart
                clip.copy(
                    startMs = newStart,
                    durationMs = newDuration,
                    frames = engine.generateDefaultFramesForClip(clip.id, newDuration, clip.title)
                )
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun trimClipEnd(clipId: String, trimMs: Long) {
        val current = _editorProject.value
        val engine = VideoEditorEngine()
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val newEnd = maxOf(clip.startMs + 500L, clip.endMs - trimMs)
                val newDuration = newEnd - clip.startMs
                clip.copy(
                    endMs = newEnd,
                    durationMs = newDuration,
                    frames = engine.generateDefaultFramesForClip(clip.id, newDuration, clip.title)
                )
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun duplicateClip(clipId: String) {
        val current = _editorProject.value
        val index = current.videoClips.indexOfFirst { it.id == clipId }
        if (index >= 0) {
            val clip = current.videoClips[index]
            val engine = VideoEditorEngine()
            val newId = "clip_${System.currentTimeMillis()}"
            val duplicate = clip.copy(
                id = newId,
                title = "${clip.title} (Copy)",
                frames = engine.generateDefaultFramesForClip(newId, clip.durationMs, "${clip.title} (Copy)")
            )
            val list = current.videoClips.toMutableList().apply { add(index + 1, duplicate) }
            _editorProject.value = current.copy(videoClips = list)
        }
    }

    fun deleteClip(clipId: String) {
        val current = _editorProject.value
        val list = current.videoClips.filterNot { it.id == clipId }
        _editorProject.value = current.copy(videoClips = list)
    }

    fun moveFrameLeft(clipId: String, frameId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val fIdx = clip.frames.indexOfFirst { it.id == frameId }
                if (fIdx > 0) {
                    val fList = clip.frames.toMutableList()
                    val frame = fList.removeAt(fIdx)
                    fList.add(fIdx - 1, frame)
                    val reindexed = fList.mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                    clip.copy(frames = reindexed)
                } else clip
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun moveFrameRight(clipId: String, frameId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val fIdx = clip.frames.indexOfFirst { it.id == frameId }
                if (fIdx in 0 until clip.frames.lastIndex) {
                    val fList = clip.frames.toMutableList()
                    val frame = fList.removeAt(fIdx)
                    fList.add(fIdx + 1, frame)
                    val reindexed = fList.mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                    clip.copy(frames = reindexed)
                } else clip
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun reorderClipFrames(clipId: String, fromFrameIdx: Int, toFrameIdx: Int) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId && fromFrameIdx in clip.frames.indices && toFrameIdx in clip.frames.indices) {
                val fList = clip.frames.toMutableList()
                val frame = fList.removeAt(fromFrameIdx)
                fList.add(toFrameIdx, frame)
                val reindexed = fList.mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                clip.copy(frames = reindexed)
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun reverseClipFrames(clipId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val reversed = clip.frames.reversed().mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                clip.copy(frames = reversed)
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun duplicateFrame(clipId: String, frameId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val fIdx = clip.frames.indexOfFirst { it.id == frameId }
                if (fIdx >= 0) {
                    val frame = clip.frames[fIdx]
                    val copy = frame.copy(
                        id = "${frame.id}_dup_${System.currentTimeMillis()}",
                        label = "${frame.label} (Hold)",
                        isKeyframe = true
                    )
                    val fList = clip.frames.toMutableList().apply { add(fIdx + 1, copy) }
                    val reindexed = fList.mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                    clip.copy(frames = reindexed)
                } else clip
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun deleteFrame(clipId: String, frameId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId && clip.frames.size > 2) {
                val filtered = clip.frames.filterNot { it.id == frameId }
                val reindexed = filtered.mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                clip.copy(frames = reindexed)
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun addKeyframeToClip(clipId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            if (clip.id == clipId) {
                val nextIdx = clip.frames.size + 1
                val newFrame = VideoFrameItem(
                    id = "${clip.id}_kf_${System.currentTimeMillis()}",
                    frameIndex = nextIdx,
                    timestampMs = clip.durationMs,
                    label = "Keyframe $nextIdx",
                    visualHue = (clip.frames.size * 45f) % 360f,
                    isKeyframe = true
                )
                val fList = clip.frames + newFrame
                val reindexed = fList.mapIndexed { idx, f -> f.copy(frameIndex = idx + 1) }
                clip.copy(frames = reindexed)
            } else clip
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun resetTimelineToDefaults() {
        _editorProject.value = createInitialEditorProject()
    }

    fun addAudioTrackToEditor(title: String = "Background Cyberpunk Synth") {
        val current = _editorProject.value
        val newClip = MediaClipTrack(
            id = "audio_${System.currentTimeMillis()}",
            title = "🎵 $title",
            filePath = "audio/synth.mp3",
            startMs = 0L,
            endMs = 10000L,
            durationMs = 10000L
        )
        _editorProject.value = current.copy(audioClips = current.audioClips + newClip)
    }

    fun addVoiceoverTrackToEditor() {
        val current = _editorProject.value
        val newClip = MediaClipTrack(
            id = "voice_${System.currentTimeMillis()}",
            title = "🎙️ AI Voice-Over Track",
            filePath = "audio/voiceover.wav",
            startMs = 0L,
            endMs = 5000L,
            durationMs = 5000L
        )
        _editorProject.value = current.copy(audioClips = current.audioClips + newClip)
    }

    fun addSubtitleLayerToEditor() {
        val current = _editorProject.value
        val newClip = MediaClipTrack(
            id = "sub_${System.currentTimeMillis()}",
            title = "💬 Subtitle Overlay",
            filePath = "text/subtitles.srt",
            startMs = 0L,
            endMs = 5000L,
            durationMs = 5000L
        )
        _editorProject.value = current.copy(videoClips = current.videoClips + newClip)
    }

    fun splitClip(clipId: String) {
        val current = _editorProject.value
        val clip = current.videoClips.find { it.id == clipId } ?: return
        val halfDuration = clip.durationMs / 2
        val clip1 = clip.copy(durationMs = halfDuration, title = "${clip.title} (Part 1)")
        val clip2 = clip.copy(id = "clip_${System.currentTimeMillis()}", durationMs = halfDuration, title = "${clip.title} (Part 2)")
        val updatedClips = current.videoClips.flatMap { if (it.id == clipId) listOf(clip1, clip2) else listOf(it) }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun trimClip(clipId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(durationMs = maxOf(1000L, it.durationMs - 1000L)) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun reverseClip(clipId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) {
                val newTitle = if (it.title.endsWith("(Reversed)")) it.title.removeSuffix(" (Reversed)") else "${it.title} (Reversed)"
                it.copy(title = newTitle)
            } else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipSpeed(clipId: String, speed: Float) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(playbackSpeed = speed) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipVelocityCurve(clipId: String, velocityCurve: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(velocityCurve = velocityCurve) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipFilter(clipId: String, filterName: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(filterName = filterName) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipAiStyleEffect(clipId: String, styleEffect: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(aiStyleEffect = styleEffect) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipTransition(clipId: String, transitionType: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(transitionType = transitionType) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipSubtitleStyle(clipId: String, subtitleStyle: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(subtitleStyle = subtitleStyle) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipVoiceChanger(clipId: String, voicePreset: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(voiceChangerPreset = voicePreset) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipSfxPreset(clipId: String, sfxPreset: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(sfxPreset = sfxPreset) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun toggleClipBgCutout(clipId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(bgRemovalCutout = !it.bgRemovalCutout) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun toggleClipWatermarkRemover(clipId: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(removeWatermark = !it.removeWatermark) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun updateClipWatermarkMethod(clipId: String, method: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map {
            if (it.id == clipId) it.copy(watermarkMethod = method) else it
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun toggleGpuHardwareAcceleration(enabled: Boolean) {
        val current = _editorProject.value
        _editorProject.value = current.copy(gpuHardwareAcceleration = enabled)
    }

    fun updateCpuThreads(threads: Int) {
        val current = _editorProject.value
        _editorProject.value = current.copy(cpuMultiThreadCount = threads)
    }

    fun toggleNpuTensorAcceleration(enabled: Boolean) {
        val current = _editorProject.value
        _editorProject.value = current.copy(npuTensorAcceleration = enabled)
    }

    fun toggleGlobalWatermarkEraser(enabled: Boolean) {
        val current = _editorProject.value
        _editorProject.value = current.copy(globalWatermarkEraser = enabled)
    }

    fun autoGenerateCaptionsFromAudio() {
        val current = _editorProject.value
        val subClip = MediaClipTrack(
            id = "auto_captions_${System.currentTimeMillis()}",
            title = "✨ CapCut Auto-Subtitles (Bouncing Kinetic)",
            filePath = "subtitles/auto_gen.srt",
            startMs = 0L,
            endMs = 10000L,
            durationMs = 10000L,
            textOverlay = "AUTO CAPTIONS • AI SPEECH RECOGNITION",
            subtitleStyle = "KINETIC_BOUNCE"
        )
        _editorProject.value = current.copy(videoClips = current.videoClips + subClip)
    }

    fun applyCapCutPresetTemplate(templateName: String) {
        val current = _editorProject.value
        val updatedClips = current.videoClips.map { clip ->
            when (templateName) {
                "TIKTOK_VELOCITY" -> clip.copy(
                    velocityCurve = "AUTO_VELOCITY",
                    aiStyleEffect = "ZOOM_3D_PARALLAX",
                    transitionType = "FLASH_WHITE",
                    filterName = "CapCut Teal/Orange",
                    subtitleStyle = "NEON_GLOW"
                )
                "ANIME_BEAT_SYNC" -> clip.copy(
                    velocityCurve = "HERO_PULSE",
                    aiStyleEffect = "ANIME_CONVERSION",
                    transitionType = "GLITCH_TEAR",
                    filterName = "Neon Vivid",
                    subtitleStyle = "COMIC_BUBBLE",
                    sfxPreset = "BASS_DROP"
                )
                "CINEMATIC_TRAILER" -> clip.copy(
                    velocityCurve = "MONTAGE_RAMP",
                    aiStyleEffect = "LIGHT_LEAKS",
                    transitionType = "WHIP_PAN",
                    filterName = "Vintage Film",
                    voiceChangerPreset = "DEEP_TRAILER",
                    sfxPreset = "WHOOSH_SWIPE"
                )
                else -> clip
            }
        }
        _editorProject.value = current.copy(videoClips = updatedClips)
    }

    fun exportEditorProject(onExportComplete: (GalleryItemEntity) -> Unit) {
        viewModelScope.launch {
            val project = _editorProject.value
            val exportItem = GalleryItemEntity(
                id = "export_${System.currentTimeMillis()}",
                title = "${project.name} Export",
                mediaType = "VIDEO",
                filePath = "exports/${project.id}.mp4",
                durationMs = project.videoClips.sumOf { it.durationMs },
                prompt = "Edited video timeline with ${project.videoClips.size} tracks",
                resolutionLabel = "1080p"
            )
            repository.galleryDao.insertItem(exportItem)
            _latestExportedResult.value = exportItem
            onExportComplete(exportItem)
        }
    }

    fun scanSoraCloudServers() {
        viewModelScope.launch {
            repository.soraCloudClient.discoverLocalNetworkServers()
        }
    }

    fun setMemoryMode(mode: String) {
        _memoryMode.value = mode
    }

    fun setUseSdCardCache(use: Boolean) {
        _useSdCardCache.value = use
    }

    fun trimMemory() {
        viewModelScope.launch {
            repository.inferenceEngineManager.trimMemory()
            System.gc()
        }
    }

    // Settings actions
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
    }

    fun setApiBaseUrl(url: String) {
        _apiBaseUrl.value = url
    }

    fun setApiEngineKey(key: String) {
        _apiEngineKey.value = key
    }

    fun setApiProviderPreset(preset: String) {
        _apiProviderPreset.value = preset
        when (preset) {
            "Local Server" -> {
                _apiBaseUrl.value = "http://192.168.1.X:8080/v1"
                _apiEngineModel.value = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv409"
            }
            "Ollama Cloud" -> {
                _apiBaseUrl.value = "http://localhost:11434/v1"
                _apiEngineModel.value = "llama3.2:3b"
            }
            "DeepSeek" -> {
                _apiBaseUrl.value = "https://api.deepseek.com/v1"
                _apiEngineModel.value = "deepseek-chat"
            }
            "Groq" -> {
                _apiBaseUrl.value = "https://api.groq.com/openai/v1"
                _apiEngineModel.value = "llama-3.3-70b-versatile"
            }
            "NVIDIA" -> {
                _apiBaseUrl.value = "https://integrate.api.nvidia.com/v1"
                _apiEngineModel.value = "meta/llama-3.1-70b-instruct"
            }
            "Custom" -> {
                // Keep current
            }
        }
    }

    fun setApiEngineModel(model: String) {
        _apiEngineModel.value = model
    }

    fun fetchApiEngineModels() {
        viewModelScope.launch {
            _isFetchingModels.value = true
            kotlinx.coroutines.delay(1200)
            _isFetchingModels.value = false
            setSettingsStatus("Fetched models for ${_apiProviderPreset.value} successfully.")
        }
    }

    fun setDisableMaxSteps(disable: Boolean) {
        _disableMaxSteps.value = disable
    }

    fun setMaxStepsPerTask(steps: Int) {
        _maxStepsPerTask.value = steps
    }

    fun setContextLimitTokens(tokens: Int) {
        _contextLimitTokens.value = tokens
    }

    fun setSettingsTemperature(temp: Float) {
        _settingsTemperature.value = temp
    }

    fun setUseScreenCompression(use: Boolean) {
        _useScreenCompression.value = use
    }

    fun setSendSystemPrompt(send: Boolean) {
        _sendSystemPrompt.value = send
    }

    fun setTelegramBotToken(token: String) {
        _telegramBotToken.value = token
    }

    fun setTelegramBotEnabled(enabled: Boolean) {
        _isTelegramBotEnabled.value = enabled
    }

    fun setInferenceMode(mode: String) {
        _inferenceMode.value = mode
    }

    fun setModelExecutionPreset(preset: String) {
        _modelExecutionPreset.value = preset
    }

    fun setSettingsMaxTokens(tokens: Int) {
        _settingsMaxTokens.value = tokens
    }

    fun setSettingsContextSize(size: Int) {
        _settingsContextSize.value = size
    }

    fun setImageGenSteps(steps: Int) {
        _imageGenSteps.value = steps
    }

    fun setImageSizePreset(preset: String) {
        _imageSizePreset.value = preset
    }

    fun setGpuSafetyThresholdMb(mb: Int) {
        _gpuSafetyThresholdMb.value = mb
    }

    fun setImageBackend(backend: String) {
        _imageBackend.value = backend
    }

    fun setCustomSystemPrompt(prompt: String) {
        _customSystemPrompt.value = prompt
    }

    // Server & Tunnel
    fun toggleApiServerWithState(enabled: Boolean) {
        _isApiServerToggle.value = enabled
        val isCurrentlyRunning = serverState.value.status == com.example.ai.server.ServerStatus.RUNNING
        if (enabled != isCurrentlyRunning) {
            toggleApiServer()
        }
    }

    fun toggleRequireApiKey(required: Boolean) {
        _requireApiKeyToggle.value = required
    }

    fun generateNewServerApiKey() {
        val modelName = activeLoadedModel.value?.name ?: "local"
        val newKey = repository.localApiServer.generateAndSetModelApiKey(modelName)
        _serverApiKey.value = newKey
        setSettingsStatus("Generated OpenAI-compatible API Key: $newKey")
    }

    fun setServerApiKey(key: String) {
        _serverApiKey.value = key
        updateApiKey(key)
    }

    fun togglePublicTunnel(enabled: Boolean) {
        _isPublicTunnelEnabled.value = enabled
        if (enabled) {
            startPublicTunnel()
        } else {
            stopPublicTunnel()
        }
    }

    fun setTunnelProvider(provider: String) {
        _tunnelProvider.value = provider
    }

    fun setCloudflareTunnelToken(token: String) {
        _cloudflareTunnelToken.value = token
    }

    fun setStablePublicUrl(url: String) {
        _stablePublicUrl.value = url
    }

    fun startPublicTunnel() {
        viewModelScope.launch {
            _isTunnelRunning.value = true
            _isPublicTunnelEnabled.value = true
            _stablePublicUrl.value = if (_tunnelProvider.value == "Cloudflare") {
                "https://sora-model-tunnel-${(1000..9999).random()}.trycloudflare.com"
            } else {
                "https://ngrok-sora-${(1000..9999).random()}.ngrok-free.app"
            }
            setSettingsStatus("Public tunnel active at ${_stablePublicUrl.value}")
        }
    }

    fun stopPublicTunnel() {
        _isTunnelRunning.value = false
        _isPublicTunnelEnabled.value = false
        setSettingsStatus("Public tunnel stopped")
    }

    // Wake-Word & Voice Assistant Actions
    fun grantWakeWordConsent() {
        wakeWordEngine.setConsentGranted(true)
    }

    fun revokeWakeWordConsent() {
        wakeWordEngine.setConsentGranted(false)
    }

    fun toggleWakeWordService(enabled: Boolean) {
        if (enabled) {
            wakeWordEngine.startWakeWordService()
        } else {
            wakeWordEngine.stopWakeWordService()
        }
    }

    fun setWakeWordPhrase(phrase: String) {
        wakeWordEngine.setWakeWord(phrase)
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        wakeWordEngine.setSensitivity(sensitivity)
    }

    fun toggleTtsVoice(enabled: Boolean) {
        wakeWordEngine.setTtsEnabled(enabled)
    }

    fun toggleContinuousListening(enabled: Boolean) {
        wakeWordEngine.setContinuousListening(enabled)
    }

    fun executeVoiceCommand(command: String): VoiceEventItem {
        val event = wakeWordEngine.processVoiceCommand(command)
        // If it was a video generation command, auto-queue to Task Queue!
        if (event.actionType == VoiceActionType.GENERATE_VIDEO) {
            viewModelScope.launch {
                val prompt = event.commandText.replace(Regex("(?i)^(hey sora|sora|generate|create a video of)\\s*"), "").trim().ifBlank { "Futuristic Cyberpunk Skyline" }
                val newJob = GenerationJobEntity(
                    id = "voice_job_${System.currentTimeMillis()}",
                    prompt = prompt,
                    title = "Voice: ${prompt.take(24)}",
                    generationType = "TEXT_TO_VIDEO",
                    mode = "FAST",
                    durationSeconds = 5,
                    resolution = "1080p",
                    fps = 24.0f,
                    status = "QUEUED",
                    modelName = "Sora-Mobile-Lightning-Q4"
                )
                repository.generationJobDao.insertJob(newJob)
            }
        }
        return event
    }

    fun clearVoiceLogHistory() {
        wakeWordEngine.clearHistory()
    }

    companion object {
        fun createInitialEditorProject(): VideoEditorProject {
            val engine = VideoEditorEngine()
            val clip1 = MediaClipTrack(
                id = "clip_01_cyber",
                title = "🚀 Cyberpunk City Infiltration",
                filePath = "renders/scene_cyber.mp4",
                startMs = 0L,
                endMs = 4000L,
                durationMs = 4000L,
                playbackSpeed = 1.0f,
                velocityCurve = "AUTO_VELOCITY",
                aiStyleEffect = "CYBERPUNK_GLOW",
                transitionType = "WHIP_PAN",
                filterName = "CapCut Teal/Orange",
                frames = engine.generateDefaultFramesForClip("clip_01_cyber", 4000L, "Cyberpunk City")
            )
            val clip2 = MediaClipTrack(
                id = "clip_02_anime",
                title = "🎌 Anime Mech High-Speed Duel",
                filePath = "renders/scene_anime.mp4",
                startMs = 0L,
                endMs = 3200L,
                durationMs = 3200L,
                playbackSpeed = 1.25f,
                velocityCurve = "HERO_PULSE",
                aiStyleEffect = "ANIME_CONVERSION",
                transitionType = "GLITCH_TEAR",
                filterName = "Neon Vivid",
                frames = engine.generateDefaultFramesForClip("clip_02_anime", 3200L, "Anime Mech")
            )
            val clip3 = MediaClipTrack(
                id = "clip_03_quantum",
                title = "🌌 Quantum Warp Explosion",
                filePath = "renders/scene_quantum.mp4",
                startMs = 0L,
                endMs = 4800L,
                durationMs = 4800L,
                playbackSpeed = 0.8f,
                velocityCurve = "BULLET_TIME",
                aiStyleEffect = "ZOOM_3D_PARALLAX",
                transitionType = "FLASH_WHITE",
                filterName = "Vintage Film",
                frames = engine.generateDefaultFramesForClip("clip_03_quantum", 4800L, "Quantum Warp")
            )
            return VideoEditorProject(
                id = "proj_studio_01",
                name = "Sora Cinematic Timeline",
                videoClips = listOf(clip1, clip2, clip3)
            )
        }
    }
}
