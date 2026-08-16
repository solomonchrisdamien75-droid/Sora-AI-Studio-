package com.example.data

import android.content.Context
import com.example.ai.assistant.OfflineAssistantEngine
import com.example.ai.downloader.HuggingFaceClient
import com.example.ai.downloader.ModelDownloadManager
import com.example.ai.hardware.DeviceHardwareProfile
import com.example.ai.hardware.HardwareDetector
import com.example.ai.inference.InferenceEngineManager
import com.example.ai.inference.InferenceProgress
import com.example.ai.models.ModelDownloadState
import com.example.ai.models.ModelStorageScanner
import com.example.ai.models.ModelValidationEngine
import com.example.ai.models.ModelValidationStatus
import com.example.ai.quantization.ModelQuantizationEngine
import com.example.ai.queue.TaskQueueManager
import com.example.ai.server.LocalApiServer
import com.example.cloud.SoraCloudClient
import com.example.editor.VideoEditorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class SoraRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val repoScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    onJobFinished: (GalleryItemEntity) -> Unit = {}
) {
    val aiModelDao = db.aiModelDao()
    val generationJobDao = db.generationJobDao()
    val projectDao = db.projectDao()
    val soraCloudDao = db.soraCloudDao()
    val galleryDao = db.galleryDao()
    val quantizationHistoryDao = db.quantizationHistoryDao()

    val hardwareDetector = HardwareDetector(context)
    val deviceStorageManager = com.example.ai.hardware.DeviceStorageManager(context)
    val projectStorageManager = ProjectStorageManager(context)
    val telemetryPerformanceMonitor = com.example.ai.hardware.TelemetryPerformanceMonitor(context)
    val logExportManager = com.example.ai.logging.LogExportManager(context)

    val inferenceEngineManager = InferenceEngineManager(context)
    val aiInferenceManager = com.example.ai.inference.AIInferenceManager(context, inferenceEngineManager)
    val aiJobManager = com.example.ai.jobs.AIJobManager(context, aiInferenceManager, generationJobDao, repoScope)
    val localApiServer = LocalApiServer(context, inferenceEngineManager)
    val huggingFaceClient = HuggingFaceClient()
    val modelDownloadManager = ModelDownloadManager(context, aiModelDao)
    val modelValidator = ModelValidationEngine(context)
    val modelStorageScanner = ModelStorageScanner(context, modelValidator, aiModelDao)
    val modelQuantizationEngine = ModelQuantizationEngine(context, aiModelDao, quantizationHistoryDao)
    val offlineAssistantEngine = OfflineAssistantEngine(context, aiModelDao)
    val videoEditorEngine = VideoEditorEngine()
    val realMediaSynthesisEngine = com.example.ai.generator.RealMediaSynthesisEngine(context)
    val soraCloudClient = SoraCloudClient(soraCloudDao)
    val taskQueueManager = TaskQueueManager(generationJobDao, galleryDao, inferenceEngineManager, repoScope, onJobFinished, realMediaSynthesisEngine)

    // Unified AI Engines for Story, Script, and Voice
    val voiceAIEngine = com.example.ai.voice.VoiceAIEngine(context, aiInferenceManager, aiJobManager, projectStorageManager)
    val storyEngine = com.example.ai.story.StoryEngine(aiInferenceManager, aiJobManager, projectStorageManager)
    val scriptEngine = com.example.ai.script.ScriptEngine(aiInferenceManager, aiJobManager, projectStorageManager, projectDao, taskQueueManager, voiceAIEngine)

    fun getDeviceHardwareProfile(): DeviceHardwareProfile {
        return hardwareDetector.getDeviceProfile()
    }

    /**
     * Initializes default catalog models and executes on-device physical file reconciliation.
     * Never falsely marks a model as downloaded unless validated on disk.
     */
    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        val existingModels = aiModelDao.getAllModels().first()
        if (existingModels.isEmpty()) {
            val catalogAvailableForDownload = listOf(
                AiModelEntity(
                    id = "model_sora_litert_v1",
                    name = "Sora LiteRT Fast Video (3GB+ RAM)",
                    modelType = "VIDEO",
                    format = "LITERET",
                    sizeBytes = 1_100_000_000L,
                    ramRequiredMb = 2200,
                    isDownloaded = false,
                    downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                    storageLocation = "INTERNAL",
                    localPath = null,
                    description = "Ultra-fast on-device LiteRT Vulkan video generator.",
                    isFavorite = true,
                    validationStatus = "UNVERIFIED"
                ),
                AiModelEntity(
                    id = "model_wan_13b_gguf",
                    name = "Wan 2.1 Video (1.3B GGUF)",
                    modelType = "VIDEO",
                    format = "GGUF",
                    sizeBytes = 1_400_000_000L,
                    ramRequiredMb = 2800,
                    isDownloaded = false,
                    downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                    storageLocation = "INTERNAL",
                    localPath = null,
                    description = "High detail GGUF model optimized for 6GB RAM phones.",
                    validationStatus = "UNVERIFIED"
                ),
                AiModelEntity(
                    id = "model_sd15_litert",
                    name = "Stable Diffusion 1.5 Image (LiteRT)",
                    modelType = "IMAGE",
                    format = "LITERET",
                    sizeBytes = 980_000_000L,
                    ramRequiredMb = 1900,
                    isDownloaded = false,
                    downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                    storageLocation = "INTERNAL",
                    localPath = null,
                    description = "Fast text-to-image and inpainting model.",
                    validationStatus = "UNVERIFIED"
                ),
                AiModelEntity(
                    id = "model_ltx_video_onnx",
                    name = "LTX Video 0.9.1 (ONNX Cinema)",
                    modelType = "VIDEO",
                    format = "ONNX",
                    sizeBytes = 2_100_000_000L,
                    ramRequiredMb = 4200,
                    isDownloaded = false,
                    downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                    storageLocation = "INTERNAL",
                    localPath = null,
                    description = "Cinema quality 1080p rendering for high-end devices.",
                    validationStatus = "UNVERIFIED"
                ),
                AiModelEntity(
                    id = "model_gemma_2b_gguf",
                    name = "Gemma 2B Offline Scriptwriter",
                    modelType = "TEXT",
                    format = "GGUF",
                    sizeBytes = 1_250_000_000L,
                    ramRequiredMb = 2100,
                    isDownloaded = false,
                    downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                    storageLocation = "INTERNAL",
                    localPath = null,
                    description = "Offline LLM scriptwriter and scene planner.",
                    validationStatus = "UNVERIFIED"
                )
            )
            aiModelDao.insertModels(catalogAvailableForDownload)
        }

        // Run physical storage scan and reconciliation
        // This validates real files on disk and marks ONLY truly existing physical files as AVAILABLE
        modelStorageScanner.reconcileDatabaseWithStorage()

        // Clean any existing records that had isDownloaded = true without valid physical file
        val allModels = aiModelDao.getAllModelsList()
        for (model in allModels) {
            val path = model.localPath
            if (model.isDownloaded) {
                if (path.isNullOrBlank()) {
                    aiModelDao.updateModel(
                        model.copy(
                            isDownloaded = false,
                            downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                            validationStatus = ModelValidationStatus.MISSING_FILE.name
                        )
                    )
                } else {
                    val file = File(path)
                    if (!file.exists() || file.length() == 0L) {
                        aiModelDao.updateModel(
                            model.copy(
                                isDownloaded = false,
                                downloadState = ModelDownloadState.MISSING.name,
                                validationStatus = ModelValidationStatus.MISSING_FILE.name
                            )
                        )
                    }
                }
            }
        }

        // Only load a model if it is genuinely downloaded and physically exists
        val trulyDownloadedModels = aiModelDao.getAllModelsList().filter { it.isDownloaded && it.downloadState == ModelDownloadState.AVAILABLE.name }
        if (trulyDownloadedModels.isNotEmpty()) {
            inferenceEngineManager.loadModel(trulyDownloadedModels.first())
        }

        val existingGallery = galleryDao.getAllItems().first()
        if (existingGallery.isEmpty()) {
            val sampleItems = listOf(
                GalleryItemEntity(
                    id = "gal_1",
                    title = "Cyberpunk City Dusk - 1080p",
                    mediaType = "VIDEO",
                    filePath = "sample_video_1.mp4",
                    durationMs = 10000L,
                    width = 1920,
                    height = 1080,
                    prompt = "Cinematic shot of flying cars over glowing neon skyscrapers at dusk, 4k 60fps octane render",
                    isFavorite = true,
                    resolutionLabel = "1080p"
                ),
                GalleryItemEntity(
                    id = "gal_2",
                    title = "Futuristic Space Station Reveal",
                    mediaType = "VIDEO",
                    filePath = "sample_video_2.mp4",
                    durationMs = 5000L,
                    width = 1080,
                    height = 1920,
                    prompt = "Vertical shot of solar panel wings opening on deep space shuttle orbiting Earth",
                    isFavorite = false,
                    resolutionLabel = "9:16 Vertical"
                )
            )
            sampleItems.forEach { galleryDao.insertItem(it) }
        }
    }

    suspend fun createNewGenerationJob(
        title: String,
        prompt: String,
        generationType: String,
        mode: String,
        durationSec: Int,
        resolution: String,
        fps: Int
    ): GenerationJobEntity = withContext(Dispatchers.IO) {
        val jobId = "job_${System.currentTimeMillis()}"
        val totalFrames = fps * durationSec
        val job = GenerationJobEntity(
            id = jobId,
            title = title.ifBlank { "Untitled Sora Render" },
            prompt = prompt,
            generationType = generationType,
            mode = mode,
            progressPercent = 0,
            currentFrame = 0,
            totalFrames = totalFrames,
            fps = fps.toFloat(),
            status = "RUNNING",
            durationSeconds = durationSec,
            resolution = resolution,
            backendUsed = when (mode) {
                "FAST" -> "LiteRT / Vulkan"
                "BALANCED" -> "ONNX DirectML / CPU+NNAPI"
                else -> "Cinema Mode / High-Res Vulkan"
            }
        )
        generationJobDao.insertJob(job)
        return@withContext job
    }

    fun startLocalGenerationStream(job: GenerationJobEntity): Flow<InferenceProgress> {
        val activeModel = inferenceEngineManager.activeLoadedModel.value ?: AiModelEntity(
            id = "active_model",
            name = "Sora Engine",
            modelType = "VIDEO",
            format = if (job.mode == "BALANCED") "ONNX" else "LITERET",
            sizeBytes = 1_000_000_000L,
            ramRequiredMb = 2000,
            isDownloaded = true
        )
        val engine = inferenceEngineManager.selectEngineForModel(activeModel)
        return engine.generateVideoFrames(
            prompt = job.prompt,
            width = 1080,
            height = 1920,
            fps = job.fps.toInt(),
            durationSec = job.durationSeconds,
            onFrameRendered = { _, _, _ -> }
        )
    }
}
