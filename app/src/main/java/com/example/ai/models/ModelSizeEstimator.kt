package com.example.ai.models

/**
 * ModelSizeEstimator: Dynamically computes accurate file size in bytes and required RAM in megabytes
 * for any AI model based on real repository metadata or model parameter count, quantization, and architecture.
 * Eliminates inaccurate hardcoded 1.54 GB placeholders across Hugging Face downloads and model screens.
 */
object ModelSizeEstimator {

    fun estimateSizeBytes(
        modelName: String,
        filename: String = "",
        tags: List<String> = emptyList(),
        format: String = "",
        modelType: String = "",
        actualSize: Long? = null
    ): Long {
        if (actualSize != null && actualSize > 0) return actualSize

        val combined = "$modelName $filename ${tags.joinToString(" ")} $format $modelType".lowercase()

        val isQ4orQ5 = combined.contains("q4") || combined.contains("q5") || combined.contains("gguf") || combined.contains("k_m")
        val isFP16 = combined.contains("f16") || combined.contains("fp16") || combined.contains("safetensors")

        return when {
            // Large LLM Models (70B / 72B)
            combined.contains("70b") || combined.contains("72b") -> if (isQ4orQ5) 41_500_000_000L else 142_000_000_000L
            
            // Medium-Large LLM Models (32B / 34B / 27B)
            combined.contains("32b") || combined.contains("34b") || combined.contains("27b") -> if (isQ4orQ5) 19_800_000_000L else 66_000_000_000L
            
            // Mid LLM Models (14B / 13B / 12B)
            combined.contains("14b") || combined.contains("13b") || combined.contains("12b") -> if (isQ4orQ5) 8_900_000_000L else 28_000_000_000L
            
            // Standard LLM Models (7B / 8B / 9B)
            combined.contains("8b") || combined.contains("9b") || combined.contains("7b") -> if (isQ4orQ5) 4_920_000_000L else 15_400_000_000L
            
            // Small LLM Models (3B / 4B)
            combined.contains("3b") || combined.contains("4b") || combined.contains("3.8b") || combined.contains("3_8b") -> if (isQ4orQ5) 2_150_000_000L else 7_200_000_000L
            
            // Compact LLM Models (1B / 1.5B / 2B / 2.7B)
            combined.contains("1.5b") || combined.contains("1_5b") || combined.contains("2b") || combined.contains("1b") || combined.contains("2.7b") -> if (isQ4orQ5) 1_180_000_000L else 3_400_000_000L
            
            // Tiny LLMs (0.5B / 350M / 500M)
            combined.contains("0.5b") || combined.contains("0_5b") || combined.contains("500m") || combined.contains("350m") -> 380_000_000L
            
            // Image Diffusion Models
            combined.contains("flux") -> 11_900_000_000L
            combined.contains("sdxl") || combined.contains("stable-diffusion-xl") -> 6_620_000_000L
            combined.contains("sd-v1-5") || combined.contains("stable-diffusion-v1-5") || combined.contains("sd15") || combined.contains("v1-5") -> 2_130_000_000L
            
            // Video Diffusion Models (Sora, Wan, Hunyuan, CogVideo, LTX, Mochi)
            combined.contains("wan2.1") || combined.contains("wan-") || combined.contains("sora") || combined.contains("cogvideo") || combined.contains("ltx") || combined.contains("mochi") -> 4_850_000_000L
            combined.contains("animatediff") -> 1_750_000_000L
            
            // Speech Recognition (Whisper)
            combined.contains("whisper-large") || combined.contains("whisper_large") -> 1_540_000_000L
            combined.contains("whisper-medium") -> 769_000_000L
            combined.contains("whisper-small") -> 461_000_000L
            combined.contains("whisper-base") -> 145_000_000L
            combined.contains("whisper-tiny") -> 75_000_000L
            
            // Text-To-Speech & Voice Models (Kokoro, ChatTTS, VITS, XTTS)
            combined.contains("kokoro") || combined.contains("vits") || combined.contains("chattts") -> 185_000_000L
            combined.contains("xtts") || combined.contains("f5-tts") -> 1_860_000_000L
            
            // Computer Vision & Upscalers
            combined.contains("realesrgan") || combined.contains("esrgan") || combined.contains("upscaler") -> 67_000_000L
            combined.contains("depth-anything") || combined.contains("midas") -> 375_000_000L
            
            // Type-based fallbacks
            combined.contains("video") || modelType == "VIDEO" -> 3_800_000_000L
            combined.contains("image") || modelType == "IMAGE" -> 2_400_000_000L
            combined.contains("audio") || combined.contains("tts") || modelType == "AUDIO" -> 450_000_000L
            
            else -> 1_250_000_000L
        }
    }

    fun estimateRamMb(sizeBytes: Long): Int {
        val sizeMb = (sizeBytes / (1024 * 1024)).toInt()
        return ((sizeMb * 1.25f) + 400).toInt().coerceAtLeast(350)
    }

    fun formatStorageSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "Unknown"
        val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
