package com.example.ai.inference.model

import com.example.data.AiModelEntity

enum class ModelCapability(val label: String, val description: String) {
    TEXT_GENERATION("Text Generation", "General natural language token synthesis and completion"),
    CHAT("Conversational Chat", "Multi-turn conversational dialogue with system instructions"),
    STORY_WRITING("Story Writing", "Multi-chapter narrative structuring, character arcs, and world building"),
    SCRIPT_WRITING("Script Writing", "Screenplay, YouTube storyboard, AV two-column audio/visual script generation"),
    SUMMARIZATION("Summarization", "Long-form text condensation and rolling context memory compaction"),
    TRANSLATION("Translation", "Multilingual translation preserving tone and nuance"),
    EMBEDDING("Embeddings", "Dense vector embeddings for semantic search and retrieval"),
    VISION("Vision & Multimodal", "Image comprehension, OCR, visual reasoning, and scene understanding"),
    IMAGE_GENERATION("Image Generation", "Diffusion and autoregressive text-to-image synthesis"),
    AUDIO("Audio Processing", "Audio analysis, music synthesis, and sound effect generation"),
    TEXT_TO_SPEECH("Text-to-Speech", "Voice synthesis, neural speech generation, and prosody control"),
    SPEECH_TO_TEXT("Speech-to-Text", "Automatic speech recognition and acoustic transcription"),
    VOICE_CLONING("Voice Cloning", "Zero-shot or few-shot voice timbre and acoustic cloning"),
    VIDEO_GENERATION("Video Generation", "Temporal frame synthesis, motion dynamics, and text-to-video")
}

data class ModelCompatibilityResult(
    val isCompatible: Boolean,
    val modelName: String,
    val requestedCapability: ModelCapability,
    val supportedCapabilities: Set<ModelCapability>,
    val errorMessage: String? = null,
    val recommendedAlternative: String? = null
)

object ModelCapabilityDetector {

    /**
     * Inspects a model entity and determines its exact set of AI capabilities.
     */
    fun detectCapabilities(model: AiModelEntity): Set<ModelCapability> {
        val caps = mutableSetOf<ModelCapability>()
        val name = model.name.lowercase()
        val desc = model.description.lowercase()
        val type = model.modelType.uppercase()
        val format = model.format.uppercase()
        val arch = (model.architecture ?: "").lowercase()

        when (type) {
            "TEXT" -> {
                caps.add(ModelCapability.TEXT_GENERATION)
                caps.add(ModelCapability.CHAT)
                caps.add(ModelCapability.STORY_WRITING)
                caps.add(ModelCapability.SCRIPT_WRITING)
                caps.add(ModelCapability.SUMMARIZATION)
                caps.add(ModelCapability.TRANSLATION)
                caps.add(ModelCapability.EMBEDDING)
            }
            "IMAGE" -> {
                caps.add(ModelCapability.IMAGE_GENERATION)
            }
            "VIDEO" -> {
                caps.add(ModelCapability.VIDEO_GENERATION)
            }
            "AUDIO" -> {
                caps.add(ModelCapability.AUDIO)
                caps.add(ModelCapability.TEXT_TO_SPEECH)
                if (name.contains("whisper") || desc.contains("speech to text") || desc.contains("stt")) {
                    caps.add(ModelCapability.SPEECH_TO_TEXT)
                }
                if (name.contains("clone") || desc.contains("cloning")) {
                    caps.add(ModelCapability.VOICE_CLONING)
                }
            }
            "VISION" -> {
                caps.add(ModelCapability.VISION)
                caps.add(ModelCapability.TEXT_GENERATION)
                caps.add(ModelCapability.CHAT)
            }
            else -> {
                // Infer from name or format
                if (name.contains("llama") || name.contains("qwen") || name.contains("gemma") || name.contains("mistral") || name.contains("phi") || name.contains("story") || name.contains("script")) {
                    caps.add(ModelCapability.TEXT_GENERATION)
                    caps.add(ModelCapability.CHAT)
                    caps.add(ModelCapability.STORY_WRITING)
                    caps.add(ModelCapability.SCRIPT_WRITING)
                    caps.add(ModelCapability.SUMMARIZATION)
                    caps.add(ModelCapability.TRANSLATION)
                }
                if (name.contains("sd") || name.contains("diffusion") || name.contains("flux") || format == "SAFETENSORS") {
                    caps.add(ModelCapability.IMAGE_GENERATION)
                }
                if (name.contains("sora") || name.contains("video") || name.contains("wan") || name.contains("hunyuan")) {
                    caps.add(ModelCapability.VIDEO_GENERATION)
                }
                if (name.contains("tts") || name.contains("voice") || name.contains("speech") || name.contains("kokoro") || name.contains("vits")) {
                    caps.add(ModelCapability.TEXT_TO_SPEECH)
                }
            }
        }

        // If no caps detected, give general text capabilities if GGUF or LiteRT
        if (caps.isEmpty()) {
            if (format == "GGUF" || format == "LITERTLM") {
                caps.add(ModelCapability.TEXT_GENERATION)
                caps.add(ModelCapability.CHAT)
                caps.add(ModelCapability.STORY_WRITING)
                caps.add(ModelCapability.SCRIPT_WRITING)
            } else {
                caps.add(ModelCapability.TEXT_GENERATION)
            }
        }

        return caps
    }

    /**
     * Verifies if the given model supports the required capability, returning a clear error and recommendation if not.
     */
    fun checkCompatibility(
        model: AiModelEntity?,
        requiredCapability: ModelCapability,
        availableModels: List<AiModelEntity> = emptyList()
    ): ModelCompatibilityResult {
        if (model == null) {
            val fallback = availableModels.firstOrNull { detectCapabilities(it).contains(requiredCapability) }?.name
                ?: "a compatible ${requiredCapability.label} model"
            return ModelCompatibilityResult(
                isCompatible = false,
                modelName = "None",
                requestedCapability = requiredCapability,
                supportedCapabilities = emptySet(),
                errorMessage = "No model is currently selected for ${requiredCapability.label}.",
                recommendedAlternative = fallback
            )
        }

        val supported = detectCapabilities(model)
        if (supported.contains(requiredCapability)) {
            return ModelCompatibilityResult(
                isCompatible = true,
                modelName = model.name,
                requestedCapability = requiredCapability,
                supportedCapabilities = supported
            )
        }

        // Incompatible model selected
        val compatibleAlternative = availableModels.firstOrNull { detectCapabilities(it).contains(requiredCapability) }?.name
            ?: when (requiredCapability) {
                ModelCapability.TEXT_GENERATION, ModelCapability.CHAT, ModelCapability.STORY_WRITING, ModelCapability.SCRIPT_WRITING ->
                    "Qwen-2.5-Coder-7B (GGUF) or Llama-3.2-3B (LiteRT)"
                ModelCapability.TEXT_TO_SPEECH -> "Kokoro-82M-TTS or Neural-Voice-v2 (LiteRT)"
                ModelCapability.SPEECH_TO_TEXT -> "Whisper-Tiny (ONNX)"
                ModelCapability.IMAGE_GENERATION -> "StableDiffusion-v1.5 (SafeTensors/ONNX)"
                ModelCapability.VIDEO_GENERATION -> "Sora-LiteRT-v1 or Wan2.1-1.3B (LiteRT)"
                else -> "a compatible model"
            }

        return ModelCompatibilityResult(
            isCompatible = false,
            modelName = model.name,
            requestedCapability = requiredCapability,
            supportedCapabilities = supported,
            errorMessage = "This model '${model.name}' (${model.modelType} format: ${model.format}) does not support ${requiredCapability.label}. Supported: ${supported.joinToString { it.label }}.",
            recommendedAlternative = compatibleAlternative
        )
    }
}
