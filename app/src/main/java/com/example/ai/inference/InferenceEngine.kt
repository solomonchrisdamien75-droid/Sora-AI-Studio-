package com.example.ai.inference

import com.example.data.AiModelEntity
import kotlinx.coroutines.flow.Flow

/**
 * AIInferenceEngine extends ModelInferenceEngine to provide common inference
 * capabilities for text, video, embeddings, and token streaming.
 */
interface AIInferenceEngine : ModelInferenceEngine

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

