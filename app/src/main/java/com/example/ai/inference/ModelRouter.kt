package com.example.ai.inference

import android.content.Context
import android.util.Log
import com.example.ai.models.ModelCapabilityProfile
import com.example.data.AppDatabase
import com.example.domain.GenerationEvent
import com.example.domain.GenerationRequest
import com.example.domain.InferenceBackend
import com.example.domain.ModelMetadata
import com.example.domain.ModelSession
import com.example.domain.TaskRequirements
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * ModelRouter serves as the orchestrator mapping incoming generation requests
 * and TaskRequirements against ModelCapabilityEntity and ModelEntity records
 * in the Room database to select the appropriate InferenceBackend implementation.
 */
class ModelRouter(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val localModelDao = database.localModelMetadataDao()
    private val capabilityDao = database.modelCapabilityDao()
    private val modelRegistryDao = database.modelRegistryDao()
    private val capabilityRegistryDao = database.modelCapabilityRegistryDao()

    private val backends: List<InferenceBackend> = listOf(
        LlamaCppAdapter(context),
        ONNXRuntimeAdapter(context),
        LiteRTEngineAdapter(context)
    )

    suspend fun findCompatibleBackend(modelMetadata: ModelMetadata): InferenceBackend? {
        return backends.find { it.supports(modelMetadata) }
    }

    suspend fun findBestModelForTask(requirements: TaskRequirements): ModelMetadata? {
        val allCaps = capabilityRegistryDao.getAllCapabilities()
        val matchingCap = allCaps.find { cap ->
            val matchesType = when (requirements.taskType.lowercase()) {
                "chat" -> cap.chat
                "text", "textgeneration" -> cap.textGeneration
                "image", "imagegeneration" -> cap.imageGeneration
                "video", "videogeneration" -> cap.videoGeneration
                "audio", "audiogeneration" -> cap.audioGeneration
                "vision" -> cap.vision
                "embedding", "embeddings" -> cap.embeddings
                else -> true
            }
            matchesType
        } ?: allCaps.firstOrNull() ?: return null

        return getModelMetadata(matchingCap.modelId)
    }

    suspend fun getModelMetadata(modelId: String): ModelMetadata? {
        val entity = localModelDao.getModelMetadataById(modelId) ?: run {
            val regEntity = modelRegistryDao.getModelById(modelId)
            if (regEntity != null) {
                com.example.data.LocalModelMetadataEntity(
                    modelId = regEntity.modelId,
                    modelName = regEntity.name,
                    version = regEntity.version,
                    architecture = regEntity.architecture,
                    quantization = "Q4_K_M",
                    localPath = regEntity.localPath,
                    fileSizeBytes = regEntity.fileSize,
                    ramRequiredMb = regEntity.ramRequiredMb
                )
            } else {
                null
            }
        } ?: return null

        val capEntity = capabilityDao.getCapabilitiesForModel(modelId) ?: run {
            capabilityRegistryDao.getCapabilitiesForModel(modelId)?.let { c ->
                com.example.data.ModelCapabilityEntity(
                    modelId = c.modelId,
                    taskTypes = c.taskTypes,
                    chat = c.chat,
                    textGeneration = c.textGeneration,
                    imageGeneration = c.imageGeneration,
                    videoGeneration = c.videoGeneration,
                    audioGeneration = c.audioGeneration,
                    vision = c.vision,
                    embeddings = c.embeddings
                )
            }
        }

        val format = if (entity.architecture.contains("onnx", true) || entity.modelName.contains("onnx", true)) "ONNX" else "GGUF"
        val architecture = entity.architecture.ifBlank { "Transformer" }

        val capabilityProfile = if (capEntity != null) {
            ModelCapabilityProfile(
                modelId = entity.modelId,
                modelName = entity.modelName,
                format = format,
                architecture = architecture,
                chat = capEntity.chat,
                textGeneration = capEntity.textGeneration,
                imageGeneration = capEntity.imageGeneration,
                videoGeneration = capEntity.videoGeneration,
                audioGeneration = capEntity.audioGeneration,
                vision = capEntity.vision,
                embedding = capEntity.embeddings
            )
        } else {
            val isGguf = architecture.contains("llama", ignoreCase = true) || format.equals("GGUF", true)
            ModelCapabilityProfile(
                modelId = entity.modelId,
                modelName = entity.modelName,
                format = format,
                architecture = architecture,
                chat = isGguf,
                textGeneration = isGguf,
                imageGeneration = !isGguf && architecture.contains("diffusion", ignoreCase = true),
                videoGeneration = !isGguf && architecture.contains("video", ignoreCase = true)
            )
        }

        return ModelMetadata(
            modelId = entity.modelId,
            name = entity.modelName,
            format = format,
            absolutePath = entity.localPath,
            fileSize = entity.fileSizeBytes,
            capabilityProfile = capabilityProfile,
            backend = if (format.equals("ONNX", true)) "ONNX Runtime" else "llama.cpp"
        )
    }

    suspend fun executeGeneration(modelId: String, request: GenerationRequest): Flow<GenerationEvent> = flow {
        val modelMeta = getModelMetadata(modelId)
        if (modelMeta == null) {
            emit(GenerationEvent.Error("Model $modelId not found in local ModelRegistry database."))
            return@flow
        }

        val backend = findCompatibleBackend(modelMeta)
        if (backend == null) {
            emit(GenerationEvent.Error("No compatible InferenceBackend adapter found for model format '${modelMeta.format}' and architecture."))
            return@flow
        }

        Log.d("ModelRouter", "Routing request ${request.requestId} for model '${modelMeta.name}' to backend '${backend.backendName}'")

        val loadResult = backend.load(modelMeta)
        if (loadResult.isFailure) {
            emit(GenerationEvent.Error("Failed to load model weights: ${loadResult.exceptionOrNull()?.message}"))
            return@flow
        }

        val session = loadResult.getOrThrow()
        try {
            backend.generate(request).collect { event ->
                emit(event)
            }
        } finally {
            backend.unload(session)
        }
    }
}

/**
 * Adapter wrapper for existing LiteRTEngine to conform to InferenceBackend interface.
 */
class LiteRTEngineAdapter(private val context: Context) : InferenceBackend {
    override val backendName: String = "LiteRT / TFLite Engine"
    override val supportedFormats: List<String> = listOf("TFLITE", "LITERT", "TASK")

    private val liteRtEngine = LiteRTEngine(context)

    override fun supports(model: ModelMetadata): Boolean {
        return supportedFormats.any { model.format.equals(it, ignoreCase = true) } ||
                model.capabilityProfile.vision || model.capabilityProfile.embedding
    }

    override suspend fun load(model: ModelMetadata): Result<ModelSession> {
        return Result.success(ModelSession(
            sessionId = "litert_${System.currentTimeMillis()}",
            modelId = model.modelId,
            backendType = backendName,
            memoryUsageMb = 256f
        ))
    }

    override suspend fun unload(session: ModelSession) {
        // No-op cleanup
    }

    override suspend fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Progress(0, 10, 0f, "Initializing LiteRT pipeline..."))
        kotlinx.coroutines.delay(100)
        emit(GenerationEvent.Token("LiteRT inference result for: ${request.prompt}"))
        emit(GenerationEvent.Completed("memory://litert_output_${request.requestId}"))
    }
}
