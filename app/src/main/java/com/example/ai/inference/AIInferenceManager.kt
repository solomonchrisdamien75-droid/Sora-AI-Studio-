package com.example.ai.inference

import android.content.Context
import com.example.ai.hardware.HardwareDetector
import com.example.ai.inference.model.ModelCapability
import com.example.ai.inference.model.ModelCapabilityDetector
import com.example.ai.inference.model.ModelCompatibilityResult
import com.example.ai.local.LocalAIOrchestrator
import com.example.ai.models.ModelRegistry
import com.example.data.AiModelEntity
import com.example.data.SoraCloudServerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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
    private val localAIOrchestrator = LocalAIOrchestrator(context, inferenceEngineManager)

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

        val model = request.targetModel ?: inferenceEngineManager.activeLoadedModel.value
        val availableModels = inferenceEngineManager.loadedModelsPool.value

        val compatibility = ModelCapabilityDetector.checkCompatibility(model, request.requiredCapability, availableModels)
        if (!compatibility.isCompatible) {
            return@withContext Result.failure(
                IllegalStateException(
                    "NO LOCAL MODEL LOADED. ${compatibility.errorMessage ?: "Import a compatible local model and load it before inference."}"
                )
            )
        }

        val activeModel = model ?: availableModels.firstOrNull { ModelCapabilityDetector.detectCapabilities(it).contains(request.requiredCapability) }
            ?: return@withContext Result.failure(
                IllegalStateException("NO LOCAL MODEL LOADED. No compatible model is loaded for ${request.requiredCapability.label}.")
            )

        val engine = inferenceEngineManager.selectEngineForModel(activeModel)
        if (!engine.isLoaded() || engine.getActiveModel()?.id != activeModel.id) {
            val loadResult = inferenceEngineManager.loadModel(activeModel, keepExisting = true)
            if (!loadResult.first) {
                return@withContext Result.failure(IllegalStateException("MODEL NOT COMPATIBLE: ${loadResult.second}"))
            }
        }

        val fullPrompt = if (!request.systemPrompt.isNullOrBlank()) {
            "<|system|>\n${request.systemPrompt}\n<|user|>\n${request.prompt}\n<|assistant|>\n"
        } else {
            request.prompt
        }

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
            return@withContext Result.failure(
                IllegalStateException("INFERENCE_FAILED: ${e.message ?: "Local inference encountered an error."}")
            )
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
            emit("NO LOCAL MODEL LOADED. ${compatibility.errorMessage ?: "Load a compatible local model first."}")
            return@flow
        }

        val activeModel = model ?: availableModels.firstOrNull { ModelCapabilityDetector.detectCapabilities(it).contains(request.requiredCapability) }
            ?: run {
                emit("NO LOCAL MODEL LOADED. No compatible model is loaded for ${request.requiredCapability.label}.")
                return@flow
            }

        val engine = inferenceEngineManager.selectEngineForModel(activeModel)
        if (!engine.isLoaded() || engine.getActiveModel()?.id != activeModel.id) {
            val loadResult = inferenceEngineManager.loadModel(activeModel, keepExisting = true)
            if (!loadResult.first) {
                emit("MODEL NOT COMPATIBLE: ${loadResult.second}")
                return@flow
            }
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
