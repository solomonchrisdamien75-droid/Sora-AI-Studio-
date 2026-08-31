package com.example.ai.local

import android.content.Context
import com.example.ai.inference.InferenceEngineManager
import com.example.ai.inference.model.ModelCapability
import com.example.ai.inference.model.ModelCapabilityDetector
import com.example.data.AiModelEntity
import kotlinx.coroutines.flow.StateFlow

enum class LocalModelStatus {
    LOADED,
    NOT_LOADED,
    INCOMPATIBLE,
    UNSUPPORTED
}

data class LocalModelLoadCheck(
    val status: LocalModelStatus,
    val model: AiModelEntity? = null,
    val message: String
)

class LocalAIOrchestrator(
    private val context: Context,
    private val inferenceEngineManager: InferenceEngineManager
) {
    val activeModel: StateFlow<AiModelEntity?> = inferenceEngineManager.activeLoadedModel

    fun requireLocalModel(requiredCapability: ModelCapability = ModelCapability.CHAT): LocalModelLoadCheck {
        val model = inferenceEngineManager.activeLoadedModel.value
        if (model == null) {
            return LocalModelLoadCheck(
                status = LocalModelStatus.NOT_LOADED,
                message = "NO LOCAL MODEL LOADED. Import a compatible GGUF, LiteRT, ONNX, or SafeTensors model in the Models screen and load it before inference."
            )
        }

        val supported = ModelCapabilityDetector.detectCapabilities(model)
        if (!supported.contains(requiredCapability)) {
            return LocalModelLoadCheck(
                status = LocalModelStatus.INCOMPATIBLE,
                model = model,
                message = "MODEL NOT COMPATIBLE: '${model.name}' (${model.format}) does not support ${requiredCapability.label}."
            )
        }

        return LocalModelLoadCheck(
            status = LocalModelStatus.LOADED,
            model = model,
            message = "Local model '${model.name}' is loaded and compatible with ${requiredCapability.label}."
        )
    }

    suspend fun ensureReady(requiredCapability: ModelCapability = ModelCapability.CHAT): AiModelEntity {
        val check = requireLocalModel(requiredCapability)
        if (check.status != LocalModelStatus.LOADED) {
            throw IllegalStateException(check.message)
        }

        val model = check.model ?: throw IllegalStateException("NO LOCAL MODEL LOADED")
        val engine = inferenceEngineManager.selectEngineForModel(model)
        if (!engine.isLoaded() || engine.getActiveModel()?.id != model.id) {
            val loadResult = inferenceEngineManager.loadModel(model, keepExisting = true)
            if (!loadResult.first) {
                throw IllegalStateException("MODEL NOT COMPATIBLE: ${loadResult.second}")
            }
        }
        return model
    }

    fun selectPreferredBackend(model: AiModelEntity): String {
        val format = model.format.uppercase()
        return when {
            format == "GGUF" || format == "GGML" -> "Llama.cpp / CPU or Vulkan"
            format == "ONNX" || format == "SAFE" || format.contains("SAFETENSOR") -> "ONNX Runtime / NNAPI or Vulkan"
            format == "LITERET" || format == "TFLITE" || format == "LITERTLM" -> "LiteRT / GPU or CPU"
            model.backend?.contains("Vulkan", ignoreCase = true) == true -> "Vulkan GPU"
            model.backend?.contains("NNAPI", ignoreCase = true) == true -> "NNAPI"
            else -> "CPU"
        }
    }
}
