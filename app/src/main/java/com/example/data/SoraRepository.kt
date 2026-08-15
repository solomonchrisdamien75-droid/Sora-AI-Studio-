package com.example.data

import android.content.Context
import com.example.ai.assistant.OfflineAssistantEngine
import com.example.ai.downloader.HuggingFaceClient
import com.example.ai.downloader.ModelDownloadManager
import com.example.ai.hardware.DeviceHardwareProfile
import com.example.ai.hardware.HardwareDetector
import com.example.ai.inference.InferenceEngineManager
import com.example.ai.inference.InferenceProgress
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
    val telemetryPerformanceMonitor = com.example.ai.hardware.TelemetryPerformanceMonitor(context)
    val logExportManager = com.example.ai.logging.LogExportManager(context)

    val inferenceEngineManager = InferenceEngineManager(context)
    val localApiServer = LocalApiServer(context, inferenceEngineManager)
    val huggingFaceClient = HuggingFaceClient()
    val modelDownloadManager = ModelDownloadManager(context, aiModelDao)
    val modelQuantizationEngine = ModelQuantizationEngine(context, aiModelDao, quantizationHistoryDao)
    val offlineAssistantEngine = OfflineAssistantEngine(context, aiModelDao)
    val videoEditorEngine = VideoEditorEngine()
    val realMediaSynthesisEngine = com.example.ai.generator.RealMediaSynthesisEngine(context)
    val soraCloudClient = SoraCloudClient(soraCloudDao)
    val taskQueueManager = TaskQueueManager(generationJobDao, galleryDao, inferenceEngineManager, repoScope, onJobFinished, realMediaSynthesisEngine)


    fun getDeviceHardwareProfile(): DeviceHardwareProfile {
        return hardwareDetector.getDeviceProfile()
    }

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        val existingModels = aiModelDao.getAllModels().first()
        if (existingModels.isEmpty()) {
            val defaults = listOf(
                AiModelEntity(
                    id = "model_sora_litert_v1",
                    name = "Sora LiteRT Fast Video (3GB+ RAM)",
                    modelType = "VIDEO",
                    format = "LITERET",
                    sizeBytes = 1_100_000_000L,
                    ramRequiredMb = 2200,
                    isDownloaded = true,
                    description = "Ultra-fast on-device LiteRT Vulkan video generator.",
                    isFavorite = true
                ),
                AiModelEntity(
                    id = "model_wan_13b_gguf",
                    name = "Wan 2.1 Video (1.3B GGUF)",
                    modelType = "VIDEO",
                    format = "GGUF",
                    sizeBytes = 1_400_000_000L,
                    ramRequiredMb = 2800,
                    isDownloaded = true,
                    description = "High detail GGUF model optimized for 6GB RAM phones."
                ),
                AiModelEntity(
                    id = "model_sd15_litert",
                    name = "Stable Diffusion 1.5 Image (LiteRT)",
                    modelType = "IMAGE",
                    format = "LITERET",
                    sizeBytes = 980_000_000L,
                    ramRequiredMb = 1900,
                    isDownloaded = true,
                    description = "Fast text-to-image and inpainting model."
                ),
                AiModelEntity(
                    id = "model_ltx_video_onnx",
                    name = "LTX Video 0.9.1 (ONNX Cinema)",
                    modelType = "VIDEO",
                    format = "ONNX",
                    sizeBytes = 2_100_000_000L,
                    ramRequiredMb = 4200,
                    isDownloaded = true,
                    description = "Cinema quality 1080p rendering for high-end devices."
                ),
                AiModelEntity(
                    id = "model_gemma_2b_gguf",
                    name = "Gemma 2B Offline Scriptwriter",
                    modelType = "TEXT",
                    format = "GGUF",
                    sizeBytes = 1_250_000_000L,
                    ramRequiredMb = 2100,
                    isDownloaded = true,
                    description = "Offline LLM scriptwriter and scene planner."
                )
            )
            aiModelDao.insertModels(defaults)

            // Auto-load Gemma 2B GGUF or Sora LiteRT by default so server is ready out of the box
            inferenceEngineManager.loadModel(defaults[0])
        } else {
            // Load the first downloaded model if available
            val downloaded = existingModels.firstOrNull { it.isDownloaded } ?: existingModels.firstOrNull()
            if (downloaded != null) {
                inferenceEngineManager.loadModel(downloaded)
            }
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
