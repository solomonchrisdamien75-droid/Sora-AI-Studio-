package com.example.ai.inference

import android.content.Context
import com.example.data.AiModelEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OnnxEngine(private val context: Context) : AIInferenceEngine {
    override val engineName: String = "ONNX Runtime / DirectML"
    override val supportedFormats: List<String> = listOf("ONNX", "SAFETENSORS")

    private var currentModel: AiModelEntity? = null

    override suspend fun isSupported(): Boolean = true

    override suspend fun loadModel(model: AiModelEntity): Boolean {
        currentModel = model
        return true
    }

    override suspend fun generateText(prompt: String, maxTokens: Int): String {
        return "ONNX Engine: Processed '$prompt'"
    }

    override fun generateVideoFrames(
        prompt: String,
        width: Int,
        height: Int,
        fps: Int,
        durationSec: Int,
        onFrameRendered: (currentFrame: Int, totalFrames: Int, previewBmpUri: String) -> Unit
    ): Flow<InferenceProgress> = flow {
        val totalFrames = fps * durationSec
        for (frame in 1..totalFrames) {
            delay(120)
            val currentFps = (10..22).random().toFloat()
            val memoryUsedMb = 450.0f + (frame * 0.15f)

            emit(
                InferenceProgress(
                    currentFrame = frame,
                    totalFrames = totalFrames,
                    fps = currentFps,
                    memoryUsageMb = memoryUsedMb,
                    tempCelsius = 38.2f,
                    isComplete = frame == totalFrames
                )
            )
        }
    }

    override suspend fun unloadModel() {
        currentModel = null
    }
}
