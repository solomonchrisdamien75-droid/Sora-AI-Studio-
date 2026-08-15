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

    // Primary active model for inference engine dispatching
    private val _activeLoadedModel = MutableStateFlow<AiModelEntity?>(null)
    val activeLoadedModel: StateFlow<AiModelEntity?> = _activeLoadedModel.asStateFlow()

    // Multi-Model Pool: Support loading 2 to an infinite number of models simultaneously in memory
    private val _loadedModelsPool = MutableStateFlow<List<AiModelEntity>>(emptyList())
    val loadedModelsPool: StateFlow<List<AiModelEntity>> = _loadedModelsPool.asStateFlow()

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

    /**
     * Load a model into memory.
     * @param model The AI model entity to load.
     * @param keepExisting When true, keeps previously loaded models in RAM (enabling 2 to infinite concurrent models).
     */
    suspend fun loadModel(model: AiModelEntity, keepExisting: Boolean = true): Pair<Boolean, String> = inferenceMutex.withLock {
        if (!keepExisting) {
            unloadCurrentModelInternal()
        }

        val check = hardwareDetector.canRunModel(model.ramRequiredMb)
        val ramWarning = if (check.second.contains("Warning", ignoreCase = true)) " [RAM limits bypassed]" else ""

        val engine = selectEngineForModel(model)
        val loaded = engine.loadModel(model)

        return if (loaded) {
            _activeLoadedModel.value = model
            _activeEngine.value = engine

            // Update Multi-Model Pool
            val pool = _loadedModelsPool.value.toMutableList()
            if (!pool.any { it.id == model.id }) {
                pool.add(model)
            }
            _loadedModelsPool.value = pool

            Pair(
                true,
                "Model '${model.name}' loaded (${pool.size} active model(s) in RAM: ${getTotalLoadedRamMb()}MB combined)$ramWarning"
            )
        } else {
            Pair(false, "Failed to initialize ${engine.engineName} for model '${model.name}'")
        }
    }

    suspend fun unloadSpecificModel(modelId: String): Boolean = inferenceMutex.withLock {
        val pool = _loadedModelsPool.value.toMutableList()
        val removed = pool.removeAll { it.id == modelId }
        _loadedModelsPool.value = pool

        if (_activeLoadedModel.value?.id == modelId) {
            _activeLoadedModel.value = pool.lastOrNull()
            _activeEngine.value = _activeLoadedModel.value?.let { selectEngineForModel(it) }
        }
        return removed
    }

    fun getTotalLoadedRamMb(): Int {
        return _loadedModelsPool.value.sumOf { it.ramRequiredMb }
    }

    fun isModelLoaded(modelId: String): Boolean {
        return _loadedModelsPool.value.any { it.id == modelId }
    }

    suspend fun validateAndPrepareInference(model: AiModelEntity): Pair<Boolean, String> {
        return loadModel(model, keepExisting = true)
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
        _loadedModelsPool.value = emptyList()
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

