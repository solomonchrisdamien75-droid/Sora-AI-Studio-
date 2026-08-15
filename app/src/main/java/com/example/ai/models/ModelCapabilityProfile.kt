package com.example.ai.models

/**
 * Machine-readable capability profile for any AI model in Sora AI Studio.
 * Defines true supported tasks, hardware backends, memory limits, and parameter constraints.
 */
data class ModelCapabilityProfile(
    val modelId: String,
    val modelName: String,
    val format: String, // GGUF, SAFETENSORS, LITERET, ONNX, MNN, NCNN, TFLITE, DIFFUSERS
    val architecture: String, // Transformer, Diffusion, UNet, DiT, Llama, Qwen, Wan, StableDiffusion, Moba
    val backend: String = "LiteRT/Vulkan",

    // Text & Chat Capabilities
    val textGeneration: Boolean = false,
    val chat: Boolean = false,
    val vision: Boolean = false,

    // Image Capabilities
    val imageGeneration: Boolean = false,
    val textToImage: Boolean = false,
    val imageToImage: Boolean = false,
    val imageEditing: Boolean = false,
    val inpainting: Boolean = false,
    val outpainting: Boolean = false,
    val imageUpscaling: Boolean = false,

    // Video Capabilities
    val videoGeneration: Boolean = false,
    val textToVideo: Boolean = false,
    val imageToVideo: Boolean = false,
    val videoToVideo: Boolean = false,
    val videoExtension: Boolean = false,
    val frameInterpolation: Boolean = false,
    val animation: Boolean = false,
    val segmentedLongVideo: Boolean = false,

    // Audio Capabilities
    val speechToText: Boolean = false,
    val textToSpeech: Boolean = false,
    val audioGeneration: Boolean = false,

    // System & Model Extension
    val embedding: Boolean = false,
    val reranking: Boolean = false,
    val quantizationSupported: Boolean = true,
    val adapterSupport: Boolean = true,
    val loraSupport: Boolean = true,
    val controlNetSupport: Boolean = false,

    // Hardware Requirements & Acceleration
    val cpuSupported: Boolean = true,
    val gpuSupported: Boolean = true,
    val vulkanSupported: Boolean = true,
    val nnapiSupported: Boolean = true,
    val minimumRamMb: Int = 2048,
    val recommendedRamMb: Int = 4096,
    val minimumStorageMb: Int = 1024,
    val supportedPrecisions: List<String> = listOf("FP16", "Q8_0", "Q4_K_M", "INT8", "INT4"),
    val supportedResolutions: List<String> = listOf("512x512", "768x768", "1024x1024", "1080p"),
    val supportedFps: List<Int> = listOf(12, 24, 30, 60),
    val maxDurationSec: Int = 300
) {
    fun isImageModel(): Boolean = imageGeneration || textToImage || imageToImage || imageEditing || inpainting
    fun isVideoModel(): Boolean = videoGeneration || textToVideo || imageToVideo || videoToVideo || videoExtension
    fun isChatModel(): Boolean = textGeneration || chat
    fun isMultimodal(): Boolean = (isChatModel() && vision) || (isImageModel() && isVideoModel())
}

/**
 * Registry that resolves real capabilities for any model ID or file format.
 */
object ModelCapabilityRegistry {

    fun resolveCapabilities(
        modelId: String,
        name: String,
        format: String,
        modelType: String,
        fileSizeBytes: Long = 0L
    ): ModelCapabilityProfile {
        val lowerName = name.lowercase()
        val lowerType = modelType.uppercase()
        val lowerId = modelId.lowercase()

        // 1. Text & LLM Models (Qwen, Llama, Gemma, DeepSeek, Mistral)
        if (lowerType == "TEXT" || lowerName.contains("qwen") || lowerName.contains("llama") || lowerName.contains("gemma") || lowerName.contains("deepseek")) {
            val isVision = lowerName.contains("vl") || lowerName.contains("vision") || lowerName.contains("omni")
            return ModelCapabilityProfile(
                modelId = modelId,
                modelName = name,
                format = format,
                architecture = if (lowerName.contains("qwen")) "Qwen2.5" else "Llama3",
                backend = if (format == "GGUF") "Llama.cpp" else "LiteRT",
                textGeneration = true,
                chat = true,
                vision = isVision,
                imageGeneration = false,
                videoGeneration = false,
                minimumRamMb = if (fileSizeBytes > 3_000_000_000L) 4096 else 2048,
                recommendedRamMb = 6144,
                supportedPrecisions = listOf("FP16", "Q8_0", "Q6_K", "Q4_K_M", "Q3_K_S", "Q2_K")
            )
        }

        // 2. Video Diffusion Models (Sora, Wan, CogVideo, AnimateDiff, SVD, LTX-Video)
        if (lowerType == "VIDEO" || lowerName.contains("sora") || lowerName.contains("wan") || lowerName.contains("cogvideo") || lowerName.contains("svd") || lowerName.contains("ltx")) {
            return ModelCapabilityProfile(
                modelId = modelId,
                modelName = name,
                format = format,
                architecture = "Diffusion Transformer (DiT)",
                backend = "Vulkan / MediaCodec",
                textGeneration = false,
                chat = false,
                imageGeneration = false, // Pure video engine
                videoGeneration = true,
                textToVideo = true,
                imageToVideo = true,
                videoToVideo = true,
                videoExtension = true,
                frameInterpolation = true,
                animation = true,
                segmentedLongVideo = true,
                minimumRamMb = 3072,
                recommendedRamMb = 6144,
                supportedResolutions = listOf("480p", "720p", "1080p", "4K"),
                supportedFps = listOf(12, 24, 30, 60),
                maxDurationSec = 1800
            )
        }

        // 3. Image Synthesis Models (Stable Diffusion, Flux, SDXL, Midjourney-Mobile, DALL-E)
        if (lowerType == "IMAGE" || lowerName.contains("flux") || lowerName.contains("sdxl") || lowerName.contains("diffusion") || lowerName.contains("sd-") || lowerName.contains("art")) {
            return ModelCapabilityProfile(
                modelId = modelId,
                modelName = name,
                format = format,
                architecture = "Latent Diffusion (UNet/DiT)",
                backend = "ONNX / LiteRT",
                textGeneration = false,
                chat = false,
                imageGeneration = true,
                textToImage = true,
                imageToImage = true,
                imageEditing = true,
                inpainting = true,
                outpainting = true,
                imageUpscaling = true,
                videoGeneration = false, // Pure image engine
                minimumRamMb = 2048,
                recommendedRamMb = 4096,
                supportedResolutions = listOf("256x256", "384x384", "512x512", "768x768", "1024x1024", "1536x1024"),
                supportedPrecisions = listOf("FP16", "INT8", "INT4")
            )
        }

        // 4. Audio & Voice Models (Bark, Whisper, Piper, XTTS, VITS)
        if (lowerType == "AUDIO" || lowerName.contains("voice") || lowerName.contains("whisper") || lowerName.contains("audio") || lowerName.contains("tts")) {
            return ModelCapabilityProfile(
                modelId = modelId,
                modelName = name,
                format = format,
                architecture = "Acoustic Neural Vocoder",
                backend = "ONNX Runtime",
                speechToText = lowerName.contains("whisper"),
                textToSpeech = true,
                audioGeneration = true,
                minimumRamMb = 1024,
                recommendedRamMb = 2048
            )
        }

        // Default Fallback
        return ModelCapabilityProfile(
            modelId = modelId,
            modelName = name,
            format = format,
            architecture = "Neural Model",
            backend = "LiteRT",
            textGeneration = true,
            chat = true,
            imageGeneration = true,
            videoGeneration = false
        )
    }
}
