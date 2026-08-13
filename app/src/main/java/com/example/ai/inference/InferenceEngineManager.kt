package com.example.ai.inference

import android.content.Context
import com.example.ai.hardware.HardwareDetector
import com.example.data.AiModelEntity

class InferenceEngineManager(private val context: Context) {
    private val liteRTEngine = LiteRTEngine(context)
    private val llamaCppEngine = LlamaCppEngine(context)
    private val onnxEngine = OnnxEngine(context)
    private val hardwareDetector = HardwareDetector(context)

    fun selectEngineForModel(model: AiModelEntity): AIInferenceEngine {
        return when (model.format.uppercase()) {
            "GGUF" -> llamaCppEngine
            "ONNX", "SAFETENSORS" -> onnxEngine
            else -> liteRTEngine
        }
    }

    suspend fun validateAndPrepareInference(model: AiModelEntity): Pair<Boolean, String> {
        val check = hardwareDetector.canRunModel(model.ramRequiredMb)
        if (!check.first) {
            return check
        }
        val engine = selectEngineForModel(model)
        val loaded = engine.loadModel(model)
        return if (loaded) {
            Pair(true, "Model ${model.name} successfully loaded into ${engine.engineName}")
        } else {
            Pair(false, "Failed to initialize engine for model ${model.name}")
        }
    }

    suspend fun trimMemory() {
        liteRTEngine.unloadModel()
        llamaCppEngine.unloadModel()
        onnxEngine.unloadModel()
    }
}
