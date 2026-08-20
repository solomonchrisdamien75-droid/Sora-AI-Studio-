package com.example.ai.inference

import android.content.Context
import com.example.ai.hardware.HardwareDetector
import com.example.ai.inference.model.ModelCapability
import com.example.ai.inference.model.ModelCapabilityDetector
import com.example.ai.inference.model.ModelCompatibilityResult
import com.example.ai.models.ModelRegistry
import com.example.data.AiModelEntity
import com.example.data.SoraCloudServerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/**
 * AIInferenceRequest encapsulating user prompts, parameters, system instructions, and target capabilities.
 */
data class AIInferenceRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val requiredCapability: ModelCapability = ModelCapability.TEXT_GENERATION,
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopSequences: List<String> = emptyList(),
    val targetModel: AiModelEntity? = null,
    val remoteServer: SoraCloudServerEntity? = null,
    val useCloudFallback: Boolean = false
)

data class AIInferenceResponse(
    val text: String,
    val modelUsed: String,
    val backendUsed: String,
    val tokensGenerated: Int,
    val latencyMs: Long,
    val tokensPerSecond: Float,
    val ramPeakMb: Int,
    val finishReason: String = "stop"
)

/**
 * Unified AIInferenceManager: The central engine powering Story Writer, Script Writer,
 * Chat/Assistant, Summaries, and Language AI tasks across Local models, Sora Cloud, and Remote runtimes.
 */
class AIInferenceManager(
    private val context: Context,
    val inferenceEngineManager: InferenceEngineManager
) {
    val hardwareDetector = HardwareDetector(context)
    val modelRegistry = ModelRegistry.getInstance(context)

    /**
     * Checks hardware compatibility for a specific model before loading or executing.
     */
    fun checkHardwareCompatibility(model: AiModelEntity) = modelRegistry.checkHardwareCompatibility(model)

    /**
     * Inspects active or target model for capability compatibility before executing inference.
     */
    fun validateCapability(
        model: AiModelEntity?,
        requiredCapability: ModelCapability,
        availableModels: List<AiModelEntity> = emptyList()
    ): ModelCompatibilityResult {
        return ModelCapabilityDetector.checkCompatibility(model, requiredCapability, availableModels)
    }


    /**
     * Executes real text generation with model capability validation.
     */
    suspend fun generateText(request: AIInferenceRequest): Result<AIInferenceResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Resolve Target Model
        val model = request.targetModel ?: inferenceEngineManager.activeLoadedModel.value
        val availableModels = inferenceEngineManager.loadedModelsPool.value

        // 2. Validate Capability and RAM Presence
        val compatibility = ModelCapabilityDetector.checkCompatibility(model, request.requiredCapability, availableModels)
        if (!compatibility.isCompatible) {
            return@withContext Result.failure(
                IllegalStateException(
                    compatibility.errorMessage ?: "Incompatible model selected for ${request.requiredCapability.label}. Recommended: ${compatibility.recommendedAlternative}"
                )
            )
        }

        val activeModel = model ?: availableModels.firstOrNull { ModelCapabilityDetector.detectCapabilities(it).contains(request.requiredCapability) }
            ?: return@withContext Result.failure(
                IllegalStateException("⚠️ AI Model in RAM Required: No active model loaded in device memory supports ${request.requiredCapability.label}. Please load a model into RAM.")
            )
        val engine = inferenceEngineManager.selectEngineForModel(activeModel)

        // Ensure model is loaded in engine
        if (!engine.isLoaded() || engine.getActiveModel()?.id != activeModel.id) {
            engine.loadModel(activeModel)
        }

        // Build augmented prompt with system instructions if present
        val fullPrompt = if (!request.systemPrompt.isNullOrBlank()) {
            "<|system|>\n${request.systemPrompt}\n<|user|>\n${request.prompt}\n<|assistant|>\n"
        } else {
            request.prompt
        }

        // Execute inference through engine
        try {
            val generatedText = engine.generateText(
                prompt = fullPrompt,
                maxTokens = request.maxTokens,
                temperature = request.temperature
            )

            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
            val estimatedTokens = generatedText.split(Regex("\\s+")).size.coerceAtLeast(1)
            val tokensPerSec = (estimatedTokens.toFloat() / (latency.toFloat() / 1000f)).coerceAtLeast(0.1f)
            val ramUsage = activeModel.ramRequiredMb

            return@withContext Result.success(
                AIInferenceResponse(
                    text = generatedText,
                    modelUsed = activeModel.name,
                    backendUsed = engine.backendType,
                    tokensGenerated = estimatedTokens,
                    latencyMs = latency,
                    tokensPerSecond = String.format("%.1f", tokensPerSec).toFloatOrNull() ?: tokensPerSec,
                    ramPeakMb = ramUsage,
                    finishReason = "stop"
                )
            )
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Executes real token streaming for interactive Chat, Story, or Script editing.
     */
    fun streamText(request: AIInferenceRequest): Flow<String> = flow {
        val model = request.targetModel ?: inferenceEngineManager.activeLoadedModel.value
        val availableModels = inferenceEngineManager.loadedModelsPool.value

        val compatibility = ModelCapabilityDetector.checkCompatibility(model, request.requiredCapability, availableModels)
        if (!compatibility.isCompatible) {
            emit("⚠️ [Model Incompatible] ${compatibility.errorMessage}\n\nPlease select ${compatibility.recommendedAlternative} to proceed.")
            return@flow
        }

        val activeModel = model ?: com.example.data.AiModelEntity(
            id = "builtin_neural_engine",
            name = "On-Device Neural Engine",
            description = "Built-in hardware accelerated neural synthesis engine",
            format = "LITERET",
            modelType = "TEXT",
            sizeBytes = 256_000_000L,
            ramRequiredMb = 384,
            isDownloaded = true,
            downloadState = "AVAILABLE"
        )
        val engine = inferenceEngineManager.selectEngineForModel(activeModel)
        if (!engine.isLoaded()) {
            engine.loadModel(activeModel)
        }

        val fullPrompt = if (!request.systemPrompt.isNullOrBlank()) {
            "<|system|>\n${request.systemPrompt}\n<|user|>\n${request.prompt}\n<|assistant|>\n"
        } else {
            request.prompt
        }

        engine.streamText(
            prompt = fullPrompt,
            maxTokens = request.maxTokens,
            temperature = request.temperature
        ).collect { token ->
            emit(token)
        }
    }.flowOn(Dispatchers.IO)
}
