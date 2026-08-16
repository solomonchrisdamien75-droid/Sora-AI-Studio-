package com.example.ai.inference.model

import com.example.data.AiModelEntity

enum class StudioType(val label: String, val description: String) {
    IMAGE_STUDIO("Image Studio", "AI Image generation, editing, upscaling, inpainting, outpainting, 3D, and Donghua"),
    VIDEO_STUDIO("Video Studio", "Neural video synthesis, image-to-video, continuation, lip sync, and scene building"),
    MANHWA_STUDIO("Manhwa Studio", "Manhwa generation, panel analysis, animation, voice sync, and recap"),
    VOICE_STUDIO("Voice Studio", "Text-to-speech, voice generation, cloning, enhancement, and multi-character dialogue"),
    SHARED("Shared Engine", "Multi-modal model shared across creation pipelines")
}

enum class ModelTaskType(val label: String, val studio: StudioType) {
    // Image Studio Tasks
    IMAGE_GENERATION("Image Generation (Text-to-Image)", StudioType.IMAGE_STUDIO),
    IMAGE_EDITING("AI Image Editing", StudioType.IMAGE_STUDIO),
    IMAGE_UPSCALING("AI Image Upscaling", StudioType.IMAGE_STUDIO),
    IMAGE_INPAINTING("AI Inpainting", StudioType.IMAGE_STUDIO),
    IMAGE_OUTPAINTING("AI Outpainting", StudioType.IMAGE_STUDIO),
    BACKGROUND_REMOVAL("Background Removal", StudioType.IMAGE_STUDIO),
    IMAGE_MOTION_TRANSFER("Motion Transfer", StudioType.IMAGE_STUDIO),
    IMAGE_3D_CHARACTER("3D Character Generation", StudioType.IMAGE_STUDIO),
    IMAGE_3D_SCENE("3D Image/Scene Generation", StudioType.IMAGE_STUDIO),
    DONGHUA_CHARACTER("Donghua Character Creator", StudioType.IMAGE_STUDIO),
    SCENE_GENERATION("Image Scene Generator", StudioType.IMAGE_STUDIO),

    // Video Studio Tasks
    TEXT_TO_VIDEO("Text to Video", StudioType.VIDEO_STUDIO),
    IMAGE_TO_VIDEO("Image to Video", StudioType.VIDEO_STUDIO),
    VIDEO_TO_VIDEO("Video to Video", StudioType.VIDEO_STUDIO),
    VIDEO_CONTINUATION("Video Continuation", StudioType.VIDEO_STUDIO),
    VIDEO_EXTENSION("Video Extension", StudioType.VIDEO_STUDIO),
    VIDEO_ENHANCEMENT("Video Enhancement & Upscaling", StudioType.VIDEO_STUDIO),
    LIP_SYNC("Lip Sync & Speech Animation", StudioType.VIDEO_STUDIO),
    CHARACTER_ANIMATION("Character Animation", StudioType.VIDEO_STUDIO),
    CAMERA_MOTION("Cinematic Camera Motion", StudioType.VIDEO_STUDIO),
    FRAME_INTERPOLATION("Frame Interpolation", StudioType.VIDEO_STUDIO),

    // Manhwa Studio Tasks
    MANHWA_GENERATION("Manhwa Page Generation", StudioType.MANHWA_STUDIO),
    MANHWA_RECAP("Manhwa Video Recap", StudioType.MANHWA_STUDIO),
    MANHWA_ANIMATION("Manhwa Panel Animation", StudioType.MANHWA_STUDIO),
    VOICE_COVER_SYNC("Voice Cover Synchronization", StudioType.MANHWA_STUDIO),
    MANHWA_LIP_SYNC("Manhwa Character Lip Sync", StudioType.MANHWA_STUDIO),
    PANEL_ANALYSIS("Panel OCR & Segmentation", StudioType.MANHWA_STUDIO),
    STORY_CONTINUATION("Story & Panel Continuation", StudioType.MANHWA_STUDIO),

    // Voice Studio Tasks
    TEXT_TO_SPEECH("Text to Speech", StudioType.VOICE_STUDIO),
    VOICE_GENERATION("Voice Generation", StudioType.VOICE_STUDIO),
    VOICE_CLONING("Voice Cloning", StudioType.VOICE_STUDIO),
    VOICE_COVER("Voice Cover", StudioType.VOICE_STUDIO),
    SPEECH_TO_SPEECH("Speech to Speech", StudioType.VOICE_STUDIO),
    VOICE_ENHANCEMENT("Voice Enhancement & De-noise", StudioType.VOICE_STUDIO),
    AUDIO_RESTORATION("Audio Restoration", StudioType.VOICE_STUDIO),
    MULTI_CHARACTER_DIALOGUE("Multi-Character Dialogue", StudioType.VOICE_STUDIO),
    DUBBING("Automatic Dubbing & Translation", StudioType.VOICE_STUDIO),
    LIP_SYNC_AUDIO("Lip-Sync Audio Preparation", StudioType.VOICE_STUDIO),

    // General / Text Tasks
    TEXT_GENERATION("Text Generation", StudioType.SHARED),
    CHAT("Conversational Chat", StudioType.SHARED),
    STORY_WRITING("Story Writing", StudioType.SHARED),
    SCRIPT_WRITING("Script Writing", StudioType.SHARED)
}

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
    IMAGE_EDITING("Image Editing", "Inpainting, outpainting, background manipulation, and instruction editing"),
    AUDIO("Audio Processing", "Audio analysis, music synthesis, and sound effect generation"),
    TEXT_TO_SPEECH("Text-to-Speech", "Voice synthesis, neural speech generation, and prosody control"),
    SPEECH_TO_TEXT("Speech-to-Text", "Automatic speech recognition and acoustic transcription"),
    VOICE_CLONING("Voice Cloning", "Zero-shot or few-shot voice timbre and acoustic cloning"),
    VOICE_ENHANCEMENT("Voice Enhancement", "Denoising, de-reverb, audio isolation and acoustic restoration"),
    VIDEO_GENERATION("Video Generation", "Temporal frame synthesis, motion dynamics, and text-to-video"),
    VIDEO_ENHANCEMENT("Video Enhancement", "Frame interpolation, temporal super-resolution and stabilization"),
    LIP_SYNC("Lip Sync Animation", "Phoneme-to-viseme acoustic synchronizer"),
    MANHWA_PROCESSING("Manhwa Production", "Panel segmentation, OCR, and sequential comics animation"),
    D3_SYNTHESIS("3D Mesh & Scene Synthesis", "3D Gaussian splatting, OBJ/GLB mesh synthesis")
}

data class ModelCompatibilityResult(
    val isCompatible: Boolean,
    val modelName: String,
    val requestedCapability: ModelCapability,
    val supportedCapabilities: Set<ModelCapability>,
    val supportedStudios: Set<StudioType>,
    val errorMessage: String? = null,
    val recommendedAlternative: String? = null
)

object ModelCapabilityDetector {

    /**
     * Inspects a model entity and determines its exact set of AI capabilities and supported studios.
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
                caps.add(ModelCapability.IMAGE_EDITING)
                if (name.contains("3d") || desc.contains("3d") || name.contains("tripo") || name.contains("mesh")) {
                    caps.add(ModelCapability.D3_SYNTHESIS)
                }
                if (name.contains("manhwa") || name.contains("anime") || name.contains("comic")) {
                    caps.add(ModelCapability.MANHWA_PROCESSING)
                }
            }
            "VIDEO" -> {
                caps.add(ModelCapability.VIDEO_GENERATION)
                caps.add(ModelCapability.VIDEO_ENHANCEMENT)
                if (name.contains("lipsync") || name.contains("wav2lip") || name.contains("sadtalker") || desc.contains("lip")) {
                    caps.add(ModelCapability.LIP_SYNC)
                }
            }
            "AUDIO" -> {
                caps.add(ModelCapability.AUDIO)
                caps.add(ModelCapability.TEXT_TO_SPEECH)
                if (name.contains("whisper") || desc.contains("speech to text") || desc.contains("stt")) {
                    caps.add(ModelCapability.SPEECH_TO_TEXT)
                }
                if (name.contains("clone") || desc.contains("cloning") || name.contains("xtts") || name.contains("f5")) {
                    caps.add(ModelCapability.VOICE_CLONING)
                }
                if (name.contains("enhance") || desc.contains("denoise") || desc.contains("restoration")) {
                    caps.add(ModelCapability.VOICE_ENHANCEMENT)
                }
                if (name.contains("lipsync") || name.contains("phoneme")) {
                    caps.add(ModelCapability.LIP_SYNC)
                }
            }
            "VISION" -> {
                caps.add(ModelCapability.VISION)
                caps.add(ModelCapability.TEXT_GENERATION)
                caps.add(ModelCapability.CHAT)
                caps.add(ModelCapability.MANHWA_PROCESSING)
            }
            else -> {
                if (name.contains("llama") || name.contains("qwen") || name.contains("gemma") || name.contains("mistral") || name.contains("phi") || name.contains("story") || name.contains("script")) {
                    caps.add(ModelCapability.TEXT_GENERATION)
                    caps.add(ModelCapability.CHAT)
                    caps.add(ModelCapability.STORY_WRITING)
                    caps.add(ModelCapability.SCRIPT_WRITING)
                    caps.add(ModelCapability.SUMMARIZATION)
                    caps.add(ModelCapability.TRANSLATION)
                }
                if (name.contains("sd") || name.contains("diffusion") || name.contains("flux") || format == "SAFETENSORS" || name.contains("image")) {
                    caps.add(ModelCapability.IMAGE_GENERATION)
                    caps.add(ModelCapability.IMAGE_EDITING)
                }
                if (name.contains("sora") || name.contains("video") || name.contains("wan") || name.contains("hunyuan") || name.contains("ltx") || name.contains("cogvideo") || name.contains("mochi") || name.contains("animatediff")) {
                    caps.add(ModelCapability.VIDEO_GENERATION)
                    caps.add(ModelCapability.VIDEO_ENHANCEMENT)
                }
                if (name.contains("tts") || name.contains("voice") || name.contains("speech") || name.contains("kokoro") || name.contains("vits") || name.contains("chattts")) {
                    caps.add(ModelCapability.TEXT_TO_SPEECH)
                    caps.add(ModelCapability.AUDIO)
                }
                if (name.contains("manhwa") || name.contains("manga") || name.contains("ocr")) {
                    caps.add(ModelCapability.MANHWA_PROCESSING)
                }
            }
        }

        if (caps.isEmpty()) {
            if (format == "GGUF" || format == "LITERTLM" || format == "LITERT") {
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
     * Determines which studios can use this model based on its capabilities.
     */
    fun detectSupportedStudios(model: AiModelEntity): Set<StudioType> {
        val caps = detectCapabilities(model)
        val studios = mutableSetOf<StudioType>()

        if (caps.contains(ModelCapability.IMAGE_GENERATION) || caps.contains(ModelCapability.IMAGE_EDITING) || caps.contains(ModelCapability.D3_SYNTHESIS)) {
            studios.add(StudioType.IMAGE_STUDIO)
        }
        if (caps.contains(ModelCapability.VIDEO_GENERATION) || caps.contains(ModelCapability.VIDEO_ENHANCEMENT) || caps.contains(ModelCapability.LIP_SYNC)) {
            studios.add(StudioType.VIDEO_STUDIO)
        }
        if (caps.contains(ModelCapability.MANHWA_PROCESSING) || caps.contains(ModelCapability.IMAGE_GENERATION) || caps.contains(ModelCapability.STORY_WRITING) || caps.contains(ModelCapability.VISION)) {
            studios.add(StudioType.MANHWA_STUDIO)
        }
        if (caps.contains(ModelCapability.TEXT_TO_SPEECH) || caps.contains(ModelCapability.AUDIO) || caps.contains(ModelCapability.VOICE_CLONING) || caps.contains(ModelCapability.VOICE_ENHANCEMENT)) {
            studios.add(StudioType.VOICE_STUDIO)
        }
        if (caps.contains(ModelCapability.TEXT_GENERATION) || caps.contains(ModelCapability.STORY_WRITING) || caps.contains(ModelCapability.SCRIPT_WRITING)) {
            studios.add(StudioType.SHARED)
        }

        return studios
    }

    /**
     * Verifies if the given model supports the required capability for a studio task.
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
                supportedStudios = emptySet(),
                errorMessage = "No model is currently selected for ${requiredCapability.label}.",
                recommendedAlternative = fallback
            )
        }

        val supported = detectCapabilities(model)
        val supportedStudios = detectSupportedStudios(model)

        if (supported.contains(requiredCapability)) {
            return ModelCompatibilityResult(
                isCompatible = true,
                modelName = model.name,
                requestedCapability = requiredCapability,
                supportedCapabilities = supported,
                supportedStudios = supportedStudios
            )
        }

        // Incompatible model selected
        val compatibleAlternative = availableModels.firstOrNull { detectCapabilities(it).contains(requiredCapability) }?.name
            ?: when (requiredCapability) {
                ModelCapability.TEXT_GENERATION, ModelCapability.CHAT, ModelCapability.STORY_WRITING, ModelCapability.SCRIPT_WRITING ->
                    "Qwen-2.5-Coder-7B (GGUF) or Llama-3.2-3B (LiteRT)"
                ModelCapability.TEXT_TO_SPEECH, ModelCapability.VOICE_CLONING, ModelCapability.VOICE_ENHANCEMENT -> "Kokoro-82M-TTS or Neural-Voice-v2 (LiteRT)"
                ModelCapability.SPEECH_TO_TEXT -> "Whisper-Tiny (ONNX)"
                ModelCapability.IMAGE_GENERATION, ModelCapability.IMAGE_EDITING, ModelCapability.D3_SYNTHESIS -> "StableDiffusion-v1.5 (SafeTensors/ONNX) or Flux.1-Schnell (LiteRT)"
                ModelCapability.VIDEO_GENERATION, ModelCapability.VIDEO_ENHANCEMENT, ModelCapability.LIP_SYNC -> "Sora-LiteRT-v1 or Wan2.1-1.3B (LiteRT)"
                ModelCapability.MANHWA_PROCESSING -> "ManhwaDiffusion-v2 or Qwen-2.5-VL-7B (GGUF)"
                else -> "a compatible model"
            }

        return ModelCompatibilityResult(
            isCompatible = false,
            modelName = model.name,
            requestedCapability = requiredCapability,
            supportedCapabilities = supported,
            supportedStudios = supportedStudios,
            errorMessage = "This model '${model.name}' (${model.modelType} format: ${model.format}) does not support ${requiredCapability.label}. Supported: ${supported.joinToString { it.label }}.",
            recommendedAlternative = compatibleAlternative
        )
    }
}

