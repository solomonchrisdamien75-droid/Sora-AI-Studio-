package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.assistant.ScriptProductionPackage
import com.example.ai.downloader.DownloadProgressState
import com.example.ai.downloader.HuggingFaceModelInfo
import com.example.ai.hardware.DeviceHardwareProfile
import com.example.cloud.CloudJobResponse
import com.example.data.*
import com.example.editor.MediaClipTrack
import com.example.editor.VideoEditorProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SoraTab(val title: String, val route: String) {
    HOME("Home", "home"),
    GENERATE("Generate", "generate"),
    MODELS("Models", "models"),
    DOWNLOADS("Downloads", "downloads"),
    GALLERY("Gallery", "gallery"),
    PROJECTS("Projects", "projects"),
    EDITOR("Editor", "editor"),
    ASSISTANT("AI Assistant", "assistant"),
    SORA_CLOUD("Server & Cloud", "server_cloud"),
    SETTINGS("Settings", "settings")
}

data class ManhwaPanelItem(
    val id: String,
    val title: String,
    val imageUri: String? = null,
    val panelType: String = "COMBAT", // COMBAT, DIALOGUE, EXPOSITION, DRAMATIC_ZOOM
    val actionDescription: String = "Hero slashes with shadow blades",
    val spokenDialogue: String? = "I am the Shadow Monarch!"
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // "OPEN_YOUTUBE", "SET_TIMER", "MANHWA_RECAP", "NAVIGATE_GENERATE"
    val actionTitle: String? = null,
    val isExecuted: Boolean = false
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

    private val _assistantInput = MutableStateFlow("Sci-Fi action scene with spaceship chase through neon asteroids")
    val assistantInput: StateFlow<String> = _assistantInput.asStateFlow()

    private val _generatedScript = MutableStateFlow<ScriptProductionPackage?>(null)
    val generatedScript: StateFlow<ScriptProductionPackage?> = _generatedScript.asStateFlow()

    private val _isAssistantLoading = MutableStateFlow(false)
    val isAssistantLoading: StateFlow<Boolean> = _isAssistantLoading.asStateFlow()

    // Video Editor state
    private val _editorProject = MutableStateFlow(VideoEditorProject(id = "p1", name = "Sora Film Project 01"))
    val editorProject: StateFlow<VideoEditorProject> = _editorProject.asStateFlow()

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
                text = "👋 Hello! I am your AI Assistant. You can chat with me naturally and tell me to do phone actions like:\n• ▶️ 'Open YouTube'\n• ⏱️ 'Set timer for 5 minutes'\n• 📖 'Create manhwa recap'\n• 🎬 'Write sci-fi movie script'\n• ⚙️ 'Check system status'"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SoraRepository(application, database)

        viewModelScope.launch {
            repository.initializeDefaultData()
            refreshHardwareProfile()
            searchHuggingFaceModels("")
        }
    }

    val allModels: StateFlow<List<AiModelEntity>> = repository.aiModelDao.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedModels: StateFlow<List<AiModelEntity>> = repository.aiModelDao.getDownloadedModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJobs: StateFlow<List<GenerationJobEntity>> = repository.generationJobDao.getAllJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val activeEngine: StateFlow<com.example.ai.inference.ModelInferenceEngine?> = repository.inferenceEngineManager.activeEngine

    private val _serverOperationMessage = MutableStateFlow<String?>(null)
    val serverOperationMessage: StateFlow<String?> = _serverOperationMessage.asStateFlow()

    fun dismissServerOperationMessage() {
        _serverOperationMessage.value = null
    }

    fun loadModelForServer(model: AiModelEntity) {
        viewModelScope.launch {
            val wasServerRunning = serverState.value.status == com.example.ai.server.ServerStatus.RUNNING
            if (wasServerRunning) {
                repository.localApiServer.stopServer()
            }

            val result = repository.inferenceEngineManager.loadModel(model)
            _serverOperationMessage.value = result.second

            if (wasServerRunning && result.first) {
                // Auto-restart server with new model
                repository.localApiServer.startServer()
            }
        }
    }

    fun unloadActiveModel() {
        viewModelScope.launch {
            if (serverState.value.status == com.example.ai.server.ServerStatus.RUNNING) {
                repository.localApiServer.stopServer()
            }
            repository.inferenceEngineManager.unloadCurrentModel()
            _serverOperationMessage.value = "Active model unloaded from memory"
        }
    }

    fun toggleApiServer() {
        if (serverState.value.status == com.example.ai.server.ServerStatus.RUNNING) {
            repository.localApiServer.stopServer()
            _serverOperationMessage.value = "Local API Server stopped"
        } else {
            val result = repository.localApiServer.startServer()
            _serverOperationMessage.value = result.second
        }
    }

    fun startApiServer(): Pair<Boolean, String> {
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
    }

    fun regenerateApiKey() {
        val newKey = "sk-sora-local-" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
        updateApiKey(newKey)
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

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        
        val userMsg = ChatMessage(sender = "USER", text = userText)
        _chatMessages.value = _chatMessages.value + userMsg

        val lower = userText.lowercase()

        viewModelScope.launch {
            kotlinx.coroutines.delay(600) // Realistic AI processing delay

            when {
                // Command: Open YouTube
                lower.contains("youtube") || lower.contains("open youtube") || lower.contains("play youtube") -> {
                    launchYouTubeApp()
                    val aiMsg = ChatMessage(
                        sender = "AI",
                        text = "🚀 Opening YouTube application for you on your device!",
                        actionType = "OPEN_YOUTUBE",
                        actionTitle = "Open YouTube App",
                        isExecuted = true
                    )
                    _chatMessages.value = _chatMessages.value + aiMsg
                }

                // Command: Set Timer
                lower.contains("timer") || lower.contains("alarm") || lower.contains("countdown") || lower.contains("remind") -> {
                    // Extract minutes or default to 5 min
                    val minutes = when {
                        lower.contains("1 minute") || lower.contains("1 min") -> 1
                        lower.contains("2 minute") || lower.contains("2 min") -> 2
                        lower.contains("3 minute") || lower.contains("3 min") -> 3
                        lower.contains("5 minute") || lower.contains("5 min") -> 5
                        lower.contains("10 minute") || lower.contains("10 min") -> 10
                        lower.contains("15 minute") || lower.contains("15 min") -> 15
                        lower.contains("30 minute") || lower.contains("30 min") -> 30
                        lower.contains("30 second") || lower.contains("30 sec") -> 0
                        else -> 5
                    }
                    val seconds = if (lower.contains("30 second") || lower.contains("30 sec")) 30 else minutes * 60
                    val timerTitle = if (userText.length > 25) userText.take(25) + "..." else userText
                    startTimer(timerTitle, seconds)

                    val timeLabel = if (seconds < 60) "$seconds seconds" else "$minutes minute(s)"
                    val aiMsg = ChatMessage(
                        sender = "AI",
                        text = "⏱️ Set a system timer for $timeLabel! A live countdown card is active below.",
                        actionType = "SET_TIMER",
                        actionTitle = "Timer Set ($timeLabel)",
                        isExecuted = true
                    )
                    _chatMessages.value = _chatMessages.value + aiMsg
                }

                // Command: Manhwa Recap
                lower.contains("manhwa") || lower.contains("recap") || lower.contains("comic animation") -> {
                    updateGenerationType("MANHWA_RECAP")
                    val aiMsg = ChatMessage(
                        sender = "AI",
                        text = "📖 Opening Manhwa Recap Studio! I've pre-configured panel auto-animation, lip sync, and smart action voice filter.",
                        actionType = "MANHWA_RECAP",
                        actionTitle = "Launch Manhwa Studio",
                        isExecuted = true
                    )
                    _chatMessages.value = _chatMessages.value + aiMsg
                    selectTab(SoraTab.GENERATE)
                }

                // Command: Write Script / Generate Video
                lower.contains("script") || lower.contains("movie") || lower.contains("scene") -> {
                    updateAssistantInput(userText)
                    generateAssistantScript()
                    val aiMsg = ChatMessage(
                        sender = "AI",
                        text = "🎬 Writing a custom script & shot breakdown for your request: '$userText'. View shot prompts below!",
                        actionType = "NAVIGATE_GENERATE",
                        actionTitle = "Script Generated"
                    )
                    _chatMessages.value = _chatMessages.value + aiMsg
                }

                // Command: Generate Video
                lower.contains("generate") || lower.contains("create video") || lower.contains("render") -> {
                    updatePrompt(userText)
                    selectTab(SoraTab.GENERATE)
                    val aiMsg = ChatMessage(
                        sender = "AI",
                        text = "✨ Loaded your video prompt into AI Workbench and switched to Generate screen!",
                        actionType = "NAVIGATE_GENERATE",
                        actionTitle = "Open AI Workbench",
                        isExecuted = true
                    )
                    _chatMessages.value = _chatMessages.value + aiMsg
                }

                // Default Conversational Answer
                else -> {
                    val aiMsg = ChatMessage(
                        sender = "AI",
                        text = "🤖 I can execute device actions for you! Try saying:\n• 'Open YouTube'\n• 'Set timer for 5 minutes'\n• 'Create manhwa recap for Solo Hunter'\n• 'Generate video of neon futuristic city'"
                    )
                    _chatMessages.value = _chatMessages.value + aiMsg
                }
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
                    // Add item to gallery
                    val galleryItem = GalleryItemEntity(
                        id = "gal_${System.currentTimeMillis()}",
                        title = form.title.ifBlank { "Sora Render" },
                        mediaType = "VIDEO",
                        filePath = "renders/${job.id}.mp4",
                        durationMs = (form.durationSec * 1000).toLong(),
                        prompt = form.prompt,
                        resolutionLabel = form.resolution
                    )
                    repository.galleryDao.insertItem(galleryItem)
                    _latestGeneratedResult.value = galleryItem
                }
            }
        }
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
        viewModelScope.launch {
            repository.modelDownloadManager.startDownload(model).collect { state ->
                _downloadingState.value = state
                if (state.isFinished) {
                    _downloadingState.value = null
                }
            }
        }
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

    fun addClipToEditor(filePath: String, title: String) {
        val current = _editorProject.value
        val newClip = MediaClipTrack(
            id = "clip_${System.currentTimeMillis()}",
            title = title,
            filePath = filePath,
            startMs = 0L,
            endMs = 5000L,
            durationMs = 5000L
        )
        val updatedList = current.videoClips.toMutableList().apply { add(newClip) }
        _editorProject.value = current.copy(videoClips = updatedList)
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
}
