package com.example.ai.inference

import com.example.data.AiModelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Common Model Inference Engine interface.
 * Every model backend (llama.cpp/GGUF, LiteRT, ONNX, Safetensors, etc.)
 * implements this unified abstraction.
 */
interface ModelInferenceEngine {
    val engineName: String
    val backendType: String // e.g., "llama.cpp", "LiteRT", "ONNX Runtime"
    val supportedFormats: List<String>

    fun supportsServer(): Boolean = true
    fun supportsStreaming(): Boolean = true
    fun supportsEmbeddings(): Boolean = true

    suspend fun isSupported(): Boolean
    suspend fun loadModel(model: AiModelEntity): Boolean
    suspend fun unloadModel()
    fun isLoaded(): Boolean
    fun getActiveModel(): AiModelEntity?

    suspend fun generateText(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): String

    fun streamText(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): Flow<String>

    suspend fun generateEmbeddings(text: String): List<Float>

    fun generateVideoFrames(
        prompt: String,
        width: Int,
        height: Int,
        fps: Int,
        durationSec: Int,
        onFrameRendered: (currentFrame: Int, totalFrames: Int, previewBmpUri: String) -> Unit
    ): Flow<InferenceProgress>
}
