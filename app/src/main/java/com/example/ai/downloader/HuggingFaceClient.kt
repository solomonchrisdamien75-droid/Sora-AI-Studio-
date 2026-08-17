package com.example.ai.downloader

import com.example.network.huggingface.HfModelDetail
import com.example.network.huggingface.HfModelItem
import com.example.network.huggingface.HfSibling
import com.example.network.huggingface.HuggingFaceNetworkUtility
import com.example.network.huggingface.HuggingFaceRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HuggingFaceModelInfo(
    val id: String,
    val name: String,
    val author: String,
    val downloads: Int,
    val likes: Int,
    val format: String,
    val modelType: String,
    val sizeBytes: Long,
    val ramRequiredMb: Int,
    val downloadUrl: String,
    val tags: List<String>,
    val availableFiles: List<String> = emptyList()
)

class HuggingFaceClient(
    val networkUtility: HuggingFaceNetworkUtility = HuggingFaceNetworkUtility(HuggingFaceRetrofitClient.apiService)
) {
    suspend fun searchModels(query: String = "video"): List<HuggingFaceModelInfo> = withContext(Dispatchers.IO) {
        val curated = getCuratedModels()
        if (query.isBlank()) return@withContext curated

        // 1. Try Retrofit API search first
        val apiResult = networkUtility.searchModels(query = query, limit = 20)
        if (apiResult.isSuccess) {
            val hfItems: List<HfModelItem> = apiResult.getOrDefault(emptyList())
            if (hfItems.isNotEmpty()) {
                val converted = hfItems.map { item ->
                    val modelId = item.id
                    val authorName = item.author ?: modelId.substringBefore("/", "Community")
                    val modelName = modelId.substringAfterLast("/")

                    // Identify format from tags, library, or id
                    val detectedFormat = when {
                        item.tags.any { it.contains("gguf", true) } || modelId.contains("gguf", true) -> "GGUF"
                        item.tags.any { it.contains("safetensors", true) } || modelId.contains("safetensors", true) -> "SAFETENSORS"
                        item.tags.any { it.contains("onnx", true) } || modelId.contains("onnx", true) -> "ONNX"
                        item.tags.any { it.contains("tflite", true) || it.contains("litert", true) } -> "LITERET"
                        else -> "BIN"
                    }

                    // Identify model type
                    val detectedType = when {
                        item.pipelineTag?.contains("video", true) == true || item.tags.any { it.contains("video", true) } -> "VIDEO"
                        item.pipelineTag?.contains("image", true) == true || item.tags.any { it.contains("image", true) } -> "IMAGE"
                        item.pipelineTag?.contains("audio", true) == true || item.pipelineTag?.contains("speech", true) == true -> "AUDIO"
                        item.pipelineTag?.contains("text", true) == true -> "TEXT"
                        else -> if (modelId.contains("video", true)) "VIDEO" else "IMAGE"
                    }

                    val siblingFilenames = item.siblings?.map { it.rfilename } ?: emptyList()

                    // Pick suitable file for downloadUrl
                    val targetFile = siblingFilenames.firstOrNull { f ->
                        f.endsWith(".bin") || f.endsWith(".safetensors") || f.endsWith(".gguf") || f.endsWith(".onnx")
                    } ?: when (detectedFormat) {
                        "GGUF" -> "model.gguf"
                        "SAFETENSORS" -> "model.safetensors"
                        "ONNX" -> "model.onnx"
                        "LITERET" -> "model.tflite"
                        else -> "pytorch_model.bin"
                    }

                    HuggingFaceModelInfo(
                        id = modelId,
                        name = modelName,
                        author = authorName,
                        downloads = item.downloads,
                        likes = item.likes,
                        format = detectedFormat,
                        modelType = detectedType,
                        sizeBytes = 1_650_000_000L,
                        ramRequiredMb = 3400,
                        downloadUrl = "https://huggingface.co/$modelId/resolve/main/$targetFile",
                        tags = if (item.tags.isNotEmpty()) item.tags else listOf("huggingface", "retrofit"),
                        availableFiles = siblingFilenames
                    )
                }
                return@withContext converted
            }
        }

        // 2. Fallback to curated filter
        val filtered = curated.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.id.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }

        if (filtered.isNotEmpty()) return@withContext filtered

        return@withContext curated
    }

    suspend fun getModelDetails(repoId: String): Result<HfModelDetail> = withContext(Dispatchers.IO) {
        networkUtility.fetchModelMetadata(repoId)
    }

    suspend fun getModelFiles(repoId: String): Result<List<HfSibling>> = withContext(Dispatchers.IO) {
        networkUtility.listRepositoryFiles(repoId)
    }

    private fun getCuratedModels(): List<HuggingFaceModelInfo> {
        return listOf(
            HuggingFaceModelInfo(
                id = "sora-studio/wan-2.1-video-1.3b-gguf",
                name = "Wan 2.1 Video (1.3B GGUF)",
                author = "SoraAIStudio",
                downloads = 45200,
                likes = 3100,
                format = "GGUF",
                modelType = "VIDEO",
                sizeBytes = 1_400_000_000L,
                ramRequiredMb = 2800,
                downloadUrl = "https://huggingface.co/SoraAIStudio/wan-2.1-video/resolve/main/wan-1.3b-q4.gguf",
                tags = listOf("video-gen", "low-ram", "gguf", "fast"),
                availableFiles = listOf("wan-1.3b-q4.gguf", "config.json", "tokenizer.json")
            ),
            HuggingFaceModelInfo(
                id = "sora-studio/ltx-video-0.9.1-onnx",
                name = "LTX Video 0.9.1 (ONNX Vulkan)",
                author = "Lightricks",
                downloads = 89000,
                likes = 6400,
                format = "ONNX",
                modelType = "VIDEO",
                sizeBytes = 2_100_000_000L,
                ramRequiredMb = 4200,
                downloadUrl = "https://huggingface.co/Lightricks/LTX-Video/resolve/main/ltx-video.onnx",
                tags = listOf("video", "vulkan", "cinema-mode", "onnx"),
                availableFiles = listOf("ltx-video.onnx", "vae.onnx", "config.json")
            ),
            HuggingFaceModelInfo(
                id = "sora-studio/stable-diffusion-v1-5-bin",
                name = "Stable Diffusion 1.5 (PyTorch BIN)",
                author = "StabilityAI",
                downloads = 142000,
                likes = 12500,
                format = "BIN",
                modelType = "IMAGE",
                sizeBytes = 1_700_000_000L,
                ramRequiredMb = 2900,
                downloadUrl = "https://huggingface.co/runwayml/stable-diffusion-v1-5/resolve/main/unet/diffusion_pytorch_model.bin",
                tags = listOf("image-gen", "bin-weights", "pytorch", "retro"),
                availableFiles = listOf("unet/diffusion_pytorch_model.bin", "text_encoder/pytorch_model.bin", "vae/diffusion_pytorch_model.bin")
            ),
            HuggingFaceModelInfo(
                id = "sora-studio/stable-diffusion-v1-5-litert",
                name = "Stable Diffusion 1.5 (LiteRT)",
                author = "StabilityAI",
                downloads = 142000,
                likes = 12500,
                format = "LITERET",
                modelType = "IMAGE",
                sizeBytes = 980_000_000L,
                ramRequiredMb = 1900,
                downloadUrl = "https://huggingface.co/SoraAIStudio/sd-1.5-litert/resolve/main/sd15.tflite",
                tags = listOf("image-gen", "3gb-ram-compatible", "litert"),
                availableFiles = listOf("sd15.tflite", "config.json")
            ),
            HuggingFaceModelInfo(
                id = "sora-studio/gemma-2b-it-assistant-gguf",
                name = "Gemma 2B Scriptwriter (GGUF)",
                author = "Google",
                downloads = 98000,
                likes = 8900,
                format = "GGUF",
                modelType = "TEXT",
                sizeBytes = 1_250_000_000L,
                ramRequiredMb = 2200,
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-GGUF/resolve/main/gemma-2b-it-q4_k_m.gguf",
                tags = listOf("scriptwriter", "text", "assistant", "offline"),
                availableFiles = listOf("gemma-2b-it-q4_k_m.gguf", "tokenizer.model", "config.json")
            )
        )
    }
}

