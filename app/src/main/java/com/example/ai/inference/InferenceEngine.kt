package com.example.ai.inference

import com.example.data.AiModelEntity
import kotlinx.coroutines.flow.Flow

interface AIInferenceEngine {
    val engineName: String
    val supportedFormats: List<String>
    
    suspend fun isSupported(): Boolean
    suspend fun loadModel(model: AiModelEntity): Boolean
    suspend fun generateText(prompt: String, maxTokens: Int = 512): String
    fun generateVideoFrames(
        prompt: String,
        width: Int,
        height: Int,
        fps: Int,
        durationSec: Int,
        onFrameRendered: (currentFrame: Int, totalFrames: Int, previewBmpUri: String) -> Unit
    ): Flow<InferenceProgress>
    suspend fun unloadModel()
}

data class InferenceProgress(
    val currentFrame: Int,
    val totalFrames: Int,
    val fps: Float,
    val memoryUsageMb: Float,
    val tempCelsius: Float,
    val isComplete: Boolean,
    val previewBmpPath: String? = null,
    val error: String? = null
)
