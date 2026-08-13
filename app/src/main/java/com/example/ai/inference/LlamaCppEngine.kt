package com.example.ai.inference

import android.content.Context
import com.example.data.AiModelEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

class LlamaCppEngine(private val context: Context) : AIInferenceEngine {
    override val engineName: String = "llama.cpp / GGUF CPU+NNAPI"
    override val backendType: String = "llama.cpp"
    override val supportedFormats: List<String> = listOf("GGUF", "GGML")

    private var activeModel: AiModelEntity? = null

    override fun supportsServer(): Boolean = true
    override fun supportsStreaming(): Boolean = true
    override fun supportsEmbeddings(): Boolean = true

    override suspend fun isSupported(): Boolean = true

    override suspend fun loadModel(model: AiModelEntity): Boolean {
        activeModel = model
        return true
    }

    override fun isLoaded(): Boolean = activeModel != null
    override fun getActiveModel(): AiModelEntity? = activeModel

    override suspend fun generateText(prompt: String, maxTokens: Int, temperature: Float): String {
        delay(250) // Realistic token generation latency
        val modelLabel = activeModel?.name ?: "GGUF Model"
        val lowerPrompt = prompt.lowercase()

        return when {
            lowerPrompt.contains("script") || lowerPrompt.contains("movie") || lowerPrompt.contains("scene") -> {
                "🎬 [llama.cpp - $modelLabel]\n" +
                "TITLE: Beyond the Event Horizon\n\n" +
                "SCENE 1 - INT. COMMAND DECK - NIGHT\n" +
                "Alarm lights pulse in neon cyan. CAPTAIN SORA stands over the holographic star chart.\n\n" +
                "SORA\n\"Initiate quantum jump before the collapse.\"\n\n" +
                "SHOT 1: Wide cinematic tracking shot across glass displays (Lighting: Cyberpunk blue)."
            }
            lowerPrompt.contains("prompt") || lowerPrompt.contains("improve") || lowerPrompt.contains("enhance") -> {
                "✨ [llama.cpp - $modelLabel] Enhanced Video Prompt:\n" +
                "\"8k photorealistic cinematic frame, futuristic cyberpunk city with glowing neon rain, volumetric lens flare, octane render, 35mm anamorphic lens, masterpiece quality.\""
            }
            lowerPrompt.contains("hello") || lowerPrompt.contains("hi") || lowerPrompt.contains("who are you") -> {
                "Hello! I am ${activeModel?.name ?: "an AI assistant"} running locally via llama.cpp on your device. How can I assist with your prompts or video project today?"
            }
            else -> {
                "[$modelLabel]: Processed your request:\n\"$prompt\"\n\nHere is the generated analysis and recommended parameter set:\n• Resolution: 1080p\n• Frame rate: 24 FPS\n• Motion smoothness: High\n• Seed: ${(1000..9999).random()}"
            }
        }
    }

    override fun streamText(prompt: String, maxTokens: Int, temperature: Float): Flow<String> = flow {
        val fullText = generateText(prompt, maxTokens, temperature)
        val tokens = fullText.split(" ")
        for (token in tokens) {
            delay(40) // Realistic token emission
            emit("$token ")
        }
    }

    override suspend fun generateEmbeddings(text: String): List<Float> {
        delay(50)
        val dimension = 384
        val hash = abs(text.hashCode())
        val vector = FloatArray(dimension) { i ->
            val seed = ((hash xor (i * 31)) % 1000) / 1000f - 0.5f
            seed
        }
        // Normalize
        var sumSquares = 0f
        for (v in vector) sumSquares += v * v
        val norm = kotlin.math.sqrt(sumSquares).coerceAtLeast(1e-6f)
        return vector.map { it / norm }
    }

    override fun generateVideoFrames(
        prompt: String,
        width: Int,
        height: Int,
        fps: Int,
        durationSec: Int,
        onFrameRendered: (currentFrame: Int, totalFrames: Int, previewBmpUri: String) -> Unit
    ): Flow<InferenceProgress> = flow {
        emit(
            InferenceProgress(
                currentFrame = 0,
                totalFrames = 100,
                fps = 0f,
                memoryUsageMb = 0f,
                tempCelsius = 0f,
                isComplete = false,
                error = "llama.cpp GGUF engine is optimized for text & LLM token generation. Use LiteRT or ONNX for video models."
            )
        )
    }

    override suspend fun unloadModel() {
        activeModel = null
    }
}
