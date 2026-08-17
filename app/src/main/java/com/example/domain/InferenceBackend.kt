package com.example.domain

import com.example.ai.models.ModelCapabilityProfile
import kotlinx.coroutines.flow.Flow

data class ModelMetadata(
    val modelId: String,
    val name: String,
    val format: String,
    val absolutePath: String,
    val fileSize: Long,
    val capabilityProfile: ModelCapabilityProfile,
    val backend: String = "llama.cpp"
)

data class ModelSession(
    val sessionId: String,
    val modelId: String,
    val backendType: String,
    val loadedAt: Long = System.currentTimeMillis(),
    val memoryUsageMb: Float = 0f
)

data class GenerationRequest(
    val requestId: String,
    val prompt: String,
    val negativePrompt: String? = null,
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val width: Int = 1024,
    val height: Int = 1024,
    val fps: Int = 24,
    val durationSec: Int = 5,
    val extraParameters: Map<String, Any> = emptyMap()
)

sealed class GenerationEvent {
    data class Progress(val currentStep: Int, val totalSteps: Int, val progressPercent: Float, val statusMessage: String) : GenerationEvent()
    data class Token(val text: String) : GenerationEvent()
    data class FrameRendered(val frameIndex: Int, val totalFrames: Int, val frameUri: String) : GenerationEvent()
    data class Completed(val outputUri: String, val metadata: Map<String, Any> = emptyMap()) : GenerationEvent()
    data class Error(val message: String) : GenerationEvent()
}

interface InferenceBackend {
    val backendName: String
    val supportedFormats: List<String>

    fun supports(model: ModelMetadata): Boolean
    suspend fun load(model: ModelMetadata): Result<ModelSession>
    suspend fun unload(session: ModelSession)
    suspend fun generate(request: GenerationRequest): Flow<GenerationEvent>
}

data class TaskRequirements(
    val taskType: String, // chat, text, image, video, audio, vision, embedding
    val requiredFormat: String? = null,
    val minRamMb: Int = 0,
    val preferredBackend: String? = null
)
