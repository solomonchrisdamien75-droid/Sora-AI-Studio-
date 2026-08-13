package com.example.ai.inference

import android.content.Context
import com.example.ai.hardware.HardwareDetector
import com.example.ai.server.ServerModelBackendInfo
import com.example.data.AiModelEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InferenceEngineManager(private val context: Context) {
    val liteRTEngine = LiteRTEngine(context)
    val llamaCppEngine = LlamaCppEngine(context)
    val onnxEngine = OnnxEngine(context)
    val hardwareDetector = HardwareDetector(context)

    private val inferenceMutex = Mutex()

    private val _activeLoadedModel = MutableStateFlow<AiModelEntity?>(null)
    val activeLoadedModel: StateFlow<AiModelEntity?> = _activeLoadedModel.asStateFlow()

    private val _activeEngine = MutableStateFlow<ModelInferenceEngine?>(null)
    val activeEngine: StateFlow<ModelInferenceEngine?> = _activeEngine.asStateFlow()

    fun selectEngineForModel(model: AiModelEntity): ModelInferenceEngine {
        val fmt = model.format.uppercase().trim()
        return when {
            fmt == "GGUF" || fmt == "GGML" -> llamaCppEngine
            fmt == "ONNX" || fmt.contains("SAFE") || fmt == "MNN" || fmt == "NCNN" -> onnxEngine
            fmt == "LITERET" || fmt == "LITERTLM" || fmt == "TFLITE" -> liteRTEngine
            else -> liteRTEngine // Default universal fallback
        }
    }

    fun getBackendInfoForModel(model: AiModelEntity?): ServerModelBackendInfo {
        if (model == null) {
            return ServerModelBackendInfo(
                modelName = "No model loaded",
                format = "None",
                backend = "None",
                isServerCompatible = false,
                supportsStreaming = false,
                supportsEmbeddings = false,
                statusMessage = "No model currently loaded. Load a GGUF, LiteRT, ONNX, or Safetensors model."
            )
        }

        val engine = selectEngineForModel(model)
        val compatible = engine.supportsServer()

        return ServerModelBackendInfo(
            modelName = model.name,
            format = model.format,
            backend = engine.backendType,
            isServerCompatible = compatible,
            supportsStreaming = engine.supportsStreaming(),
            supportsEmbeddings = engine.supportsEmbeddings(),
            statusMessage = if (compatible) "Ready to serve" else "Backend incompatible with API server"
        )
    }

    suspend fun loadModel(model: AiModelEntity): Pair<Boolean, String> = inferenceMutex.withLock {
        // Safe Model Switching: Unload any currently active model first
        unloadCurrentModelInternal()

        val check = hardwareDetector.canRunModel(model.ramRequiredMb)
        if (!check.first) {
            return check
        }

        val engine = selectEngineForModel(model)
        val loaded = engine.loadModel(model)

        return if (loaded) {
            _activeLoadedModel.value = model
            _activeEngine.value = engine
            Pair(true, "Model '${model.name}' successfully loaded into ${engine.engineName}")
        } else {
            _activeLoadedModel.value = null
            _activeEngine.value = null
            Pair(false, "Failed to initialize ${engine.engineName} for model '${model.name}'")
        }
    }

    suspend fun validateAndPrepareInference(model: AiModelEntity): Pair<Boolean, String> {
        return loadModel(model)
    }

    suspend fun unloadCurrentModel() = inferenceMutex.withLock {
        unloadCurrentModelInternal()
    }

    private suspend fun unloadCurrentModelInternal() {
        _activeEngine.value?.unloadModel()
        liteRTEngine.unloadModel()
        llamaCppEngine.unloadModel()
        onnxEngine.unloadModel()
        _activeLoadedModel.value = null
        _activeEngine.value = null
    }

    suspend fun trimMemory() = inferenceMutex.withLock {
        unloadCurrentModelInternal()
    }

    suspend fun <T> runExclusiveInference(block: suspend (ModelInferenceEngine) -> T): T {
        return inferenceMutex.withLock {
            val engine = _activeEngine.value
                ?: throw IllegalStateException("No model loaded on device. Load a model before executing inference.")
            block(engine)
        }
    }
}
