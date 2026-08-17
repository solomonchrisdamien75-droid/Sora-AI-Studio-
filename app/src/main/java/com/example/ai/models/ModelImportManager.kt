package com.example.ai.models

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.AiModelDao
import com.example.data.AiModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ModelImportManager(
    private val context: Context,
    private val aiModelDao: AiModelDao,
    private val validator: ModelValidationEngine = ModelValidationEngine(context)
) {

    /**
     * Import a physical file from storage into the installed models database.
     */
    suspend fun importModelFromFile(
        file: File,
        customName: String? = null,
        forcedType: String? = null
    ): AiModelEntity = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            throw IllegalArgumentException("Target file does not exist or is 0 bytes: ${file.absolutePath}")
        }

        val validation = validator.validateFile(file)
        val checksum = validator.computeChecksumSha256(file)

        val format = if (validation.detectedFormat != "UNKNOWN") validation.detectedFormat else detectFormatFromExtension(file.name)
        val modelType = forcedType ?: determineTaskType(file.name, format)
        val backend = validation.backend ?: determineBackendForFormat(format)
        val architecture = validation.architecture ?: determineArchitecture(file.name)

        val modelId = "model_${UUID.randomUUID().toString().take(8)}"
        val displayName = customName?.ifBlank { null } ?: file.nameWithoutExtension.replace("_", " ").replace("-", " ")

        // Estimate RAM requirement based on file size
        val sizeBytes = file.length()
        val ramRequiredMb = calculateRamRequiredMb(sizeBytes, format)

        val entity = AiModelEntity(
            id = modelId,
            name = displayName,
            modelType = modelType,
            format = format,
            sizeBytes = sizeBytes,
            ramRequiredMb = ramRequiredMb,
            isDownloaded = true,
            downloadState = ModelDownloadState.AVAILABLE.name,
            storageLocation = if (file.absolutePath.contains("models")) "INTERNAL" else "SD_CARD",
            localPath = file.absolutePath,
            checksum = checksum,
            lastVerified = System.currentTimeMillis(),
            validationStatus = if (validation.isValid) ModelValidationStatus.VALID.name else validation.status.name,
            architecture = architecture,
            backend = backend,
            quantization = if (file.name.contains("Q4", true)) "Q4_K_M" else if (file.name.contains("Q8", true)) "Q8_0" else "Standard",
            description = "Imported from physical file: ${file.name} • Format: $format • Size: ${sizeBytes / (1024 * 1024)}MB",
            dateAdded = System.currentTimeMillis()
        )

        aiModelDao.insertModel(entity)
        return@withContext entity
    }

    /**
     * Import a SAF content Uri into installed models database.
     */
    suspend fun importModelFromUri(
        uri: Uri,
        customName: String? = null,
        forcedType: String? = null
    ): AiModelEntity = withContext(Dispatchers.IO) {
        val docFile = DocumentFile.fromSingleUri(context, uri)
            ?: DocumentFile.fromTreeUri(context, uri)
            ?: throw IllegalArgumentException("Could not resolve Uri: $uri")

        val fileName = docFile.name ?: "imported_model.bin"
        val sizeBytes = docFile.length().coerceAtLeast(1024L)
        val validation = validator.validateUri(uri, fileName)

        val format = if (validation.detectedFormat != "UNKNOWN") validation.detectedFormat else detectFormatFromExtension(fileName)
        val modelType = forcedType ?: determineTaskType(fileName, format)
        val backend = validation.backend ?: determineBackendForFormat(format)
        val architecture = validation.architecture ?: determineArchitecture(fileName)

        val modelId = "model_${UUID.randomUUID().toString().take(8)}"
        val displayName = customName?.ifBlank { null } ?: fileName.removeSuffix(".gguf").removeSuffix(".onnx").replace("_", " ")

        val ramRequiredMb = calculateRamRequiredMb(sizeBytes, format)

        val entity = AiModelEntity(
            id = modelId,
            name = displayName,
            modelType = modelType,
            format = format,
            sizeBytes = sizeBytes,
            ramRequiredMb = ramRequiredMb,
            isDownloaded = true,
            downloadState = ModelDownloadState.AVAILABLE.name,
            storageLocation = "CUSTOM_SAF",
            fileUri = uri.toString(),
            checksum = validation.checksumSha256,
            lastVerified = System.currentTimeMillis(),
            validationStatus = ModelValidationStatus.VALID.name,
            architecture = architecture,
            backend = backend,
            quantization = if (fileName.contains("Q4", true)) "Q4_K_M" else "Standard",
            description = "Imported via SAF Uri: $fileName • Format: $format",
            dateAdded = System.currentTimeMillis()
        )

        aiModelDao.insertModel(entity)
        return@withContext entity
    }

    private fun detectFormatFromExtension(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".gguf") -> "GGUF"
            lower.endsWith(".onnx") -> "ONNX"
            lower.endsWith(".safetensors") -> "SAFETENSORS"
            lower.endsWith(".tflite") || lower.endsWith(".litert") -> "LITERET"
            lower.endsWith(".bin") || lower.endsWith(".pt") || lower.endsWith(".pth") -> "PYTORCH"
            lower.endsWith(".mnn") -> "MNN"
            lower.endsWith(".ncnn") || lower.endsWith(".param") -> "NCNN"
            else -> "BIN"
        }
    }

    private fun determineTaskType(fileName: String, format: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.contains("video") || lower.contains("wan") || lower.contains("ltx") || lower.contains("sora") -> "VIDEO"
            lower.contains("sd") || lower.contains("diffusion") || lower.contains("image") -> "IMAGE"
            lower.contains("whisper") || lower.contains("tts") || lower.contains("audio") || lower.contains("voice") -> "AUDIO"
            lower.contains("qwen") || lower.contains("llama") || lower.contains("gemma") || lower.contains("text") || lower.contains("chat") || format == "GGUF" -> "TEXT"
            else -> "TEXT"
        }
    }

    private fun determineBackendForFormat(format: String): String {
        return when (format) {
            "GGUF" -> "Llama.cpp"
            "ONNX" -> "ONNX Runtime"
            "LITERET", "TFLITE" -> "LiteRT / Vulkan"
            "SAFETENSORS" -> "DirectML / Vulkan"
            "MNN" -> "MNN Engine"
            "NCNN" -> "NCNN Engine"
            else -> "Generic Native Engine"
        }
    }

    private fun determineArchitecture(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.contains("qwen") -> "Qwen2.5"
            lower.contains("llama") -> "Llama3"
            lower.contains("gemma") -> "Gemma"
            lower.contains("wan") -> "Wan-DiT"
            lower.contains("ltx") -> "LTX-Video"
            lower.contains("sd") -> "StableDiffusion"
            else -> "Transformer"
        }
    }

    private fun calculateRamRequiredMb(sizeBytes: Long, format: String): Int {
        val sizeMb = (sizeBytes / (1024 * 1024)).toInt()
        return when (format) {
            "GGUF" -> (sizeMb * 1.2f).toInt().coerceAtLeast(1024)
            "ONNX" -> (sizeMb * 1.3f).toInt().coerceAtLeast(1024)
            "LITERET" -> (sizeMb * 1.1f).toInt().coerceAtLeast(512)
            else -> (sizeMb * 1.5f).toInt().coerceAtLeast(1024)
        }
    }
}
