package com.example.ai.downloader

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.ai.hardware.DeviceStorageManager
import com.example.ai.models.ModelDownloadState
import com.example.ai.models.ModelValidationEngine
import com.example.ai.models.ModelValidationStatus
import com.example.data.AiModelDao
import com.example.data.AiModelEntity
import com.example.network.huggingface.HuggingFaceNetworkUtility
import com.example.network.huggingface.HuggingFaceRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class DownloadProgressState(
    val modelId: String,
    val modelName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progressPercent: Int,
    val downloadSpeedKbps: Float,
    val etaSeconds: Int,
    val isFinished: Boolean,
    val isPaused: Boolean = false,
    val storageLocationLabel: String = "Internal Storage",
    val destinationPath: String? = null,
    val error: String? = null,
    val sha256Checksum: String? = null
)

class ModelDownloadManager(
    private val context: Context,
    private val aiModelDao: AiModelDao,
    val networkUtility: HuggingFaceNetworkUtility = HuggingFaceNetworkUtility(HuggingFaceRetrofitClient.apiService)
) {
    val modelLoaderService = ModelLoaderService(context)
    private val validator = ModelValidationEngine(context)
    private val storageManager = DeviceStorageManager(context)
    private val activeDownloads = mutableMapOf<String, Boolean>()

    fun getInternalStorageDir(): File {
        return File(context.filesDir, "ai_models").apply { mkdirs() }
    }

    fun getSdCardStorageDir(): File {
        return storageManager.getSdCardStorageVolume()?.modelsDirectory ?: getInternalStorageDir()
    }

    fun startDownload(
        model: HuggingFaceModelInfo,
        targetDirOrUri: String? = null,
        storageType: String = "INTERNAL"
    ): Flow<DownloadProgressState> = flow {
        activeDownloads[model.id] = true

        val isSafUri = targetDirOrUri?.startsWith("content://") == true
        val cleanName = model.name.replace(" ", "_").replace("/", "_").replace("[^a-zA-Z0-9._-]".toRegex(), "")
        val ext = model.format.lowercase().takeIf { it != "bin" && it != "unknown" } ?: "bin"
        val fileName = if (cleanName.lowercase().endsWith(".$ext") || cleanName.contains(".")) cleanName else "$cleanName.$ext"

        val locationLabel = when {
            isSafUri -> "Custom Folder (SAF)"
            storageType.equals("SD_CARD", ignoreCase = true) -> "SD Card Storage"
            !targetDirOrUri.isNullOrBlank() -> "Custom Directory"
            else -> "Phone Storage"
        }

        val spaceCheck = storageManager.checkStorageSpace(storageType, model.sizeBytes, targetDirOrUri)
        if (!spaceCheck.hasEnoughSpace) {
            emit(
                DownloadProgressState(
                    modelId = model.id,
                    modelName = model.name,
                    bytesDownloaded = 0L,
                    totalBytes = model.sizeBytes,
                    progressPercent = 0,
                    downloadSpeedKbps = 0f,
                    etaSeconds = 0,
                    isFinished = true,
                    storageLocationLabel = locationLabel,
                    destinationPath = "",
                    error = spaceCheck.errorMessage
                )
            )
            return@flow
        }

        val destinationDir = if (!targetDirOrUri.isNullOrBlank() && !isSafUri) {
            val customDir = File(targetDirOrUri)
            try {
                if (!customDir.exists()) customDir.mkdirs()
                if (customDir.canWrite()) customDir else getInternalStorageDir()
            } catch (_: Exception) {
                getInternalStorageDir()
            }
        } else if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageDir()
        } else {
            getInternalStorageDir()
        }

        val targetFile = File(destinationDir, fileName)
        val downloadUrl = if (model.downloadUrl.isNotBlank()) model.downloadUrl else "https://huggingface.co/${model.id}/resolve/main/$fileName"

        modelLoaderService.downloadModelFromUrl(
            url = downloadUrl,
            targetFile = targetFile,
            downloadId = model.id
        ).collect { progress ->
            val speedKbps = progress.downloadSpeedBytesPerSec / 1024f
            val isDone = progress.isCompleted || progress.status == DownloadStatus.FAILED || progress.status == DownloadStatus.CANCELLED

            var safDocUriStr: String? = null
            if (isDone && progress.isCompleted && isSafUri && !targetDirOrUri.isNullOrBlank()) {
                try {
                    val treeUri = Uri.parse(targetDirOrUri)
                    val docTree = DocumentFile.fromTreeUri(context, treeUri)
                    if (docTree != null && docTree.canWrite()) {
                        val safDocFile = docTree.createFile("application/octet-stream", fileName)
                        if (safDocFile != null) {
                            safDocUriStr = safDocFile.uri.toString()
                            context.contentResolver.openOutputStream(safDocFile.uri)?.use { output ->
                                targetFile.inputStream().use { input -> input.copyTo(output) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (isDone && progress.isCompleted) {
                try {
                    val validation = validator.validateFile(targetFile)
                    val checksum = validator.computeChecksumSha256(targetFile)
                    val cleanId = "hf-${model.id.replace("/", "-").lowercase()}"

                    val entity = AiModelEntity(
                        id = cleanId,
                        name = model.name,
                        description = "Downloaded via Hugging Face Hub to $locationLabel (${targetFile.absolutePath})",
                        format = if (validation.detectedFormat != "UNKNOWN") validation.detectedFormat else model.format,
                        modelType = model.modelType,
                        sizeBytes = if (targetFile.exists() && targetFile.length() > 0) targetFile.length() else progress.downloadedBytes,
                        ramRequiredMb = model.ramRequiredMb,
                        isDownloaded = true,
                        downloadState = ModelDownloadState.AVAILABLE.name,
                        storageLocation = locationLabel,
                        localPath = targetFile.absolutePath,
                        fileUri = safDocUriStr,
                        sourceUrl = model.downloadUrl,
                        checksum = checksum,
                        lastVerified = System.currentTimeMillis(),
                        validationStatus = if (validation.isValid) ModelValidationStatus.VALID.name else validation.status.name,
                        architecture = validation.architecture,
                        backend = validation.backend,
                        quantization = if (model.name.contains("Q4", true)) "Q4_K_M" else "Standard"
                    )

                    withContext(Dispatchers.IO) {
                        aiModelDao.insertModel(entity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val progressState = DownloadProgressState(
                modelId = model.id,
                modelName = model.name,
                bytesDownloaded = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
                progressPercent = progress.progressPercent.toInt(),
                downloadSpeedKbps = speedKbps,
                etaSeconds = progress.etaSeconds.toInt(),
                isFinished = isDone,
                storageLocationLabel = locationLabel,
                destinationPath = safDocUriStr ?: progress.destinationPath ?: targetFile.absolutePath,
                error = progress.error,
                sha256Checksum = progress.sha256Checksum
            )

            emit(progressState)
        }
    }

    fun startModelEntityDownload(
        model: AiModelEntity,
        targetDirOrUri: String? = null,
        storageType: String = "INTERNAL"
    ): Flow<DownloadProgressState> = flow {
        activeDownloads[model.id] = true

        val isSafUri = targetDirOrUri?.startsWith("content://") == true
        val cleanName = model.name.replace(" ", "_").replace("/", "_").replace("[^a-zA-Z0-9._-]".toRegex(), "")
        val ext = model.format.lowercase().takeIf { it != "bin" && it != "unknown" } ?: "bin"
        val fileName = if (cleanName.lowercase().endsWith(".$ext") || cleanName.contains(".")) cleanName else "$cleanName.$ext"

        val locationLabel = when {
            isSafUri -> "Custom Folder (SAF)"
            storageType.equals("SD_CARD", ignoreCase = true) -> "SD Card Storage"
            !targetDirOrUri.isNullOrBlank() -> "Custom Directory"
            else -> "Phone Storage"
        }

        val destinationDir = if (!targetDirOrUri.isNullOrBlank() && !isSafUri) {
            val customDir = File(targetDirOrUri)
            try {
                if (!customDir.exists()) customDir.mkdirs()
                if (customDir.canWrite()) customDir else getInternalStorageDir()
            } catch (_: Exception) {
                getInternalStorageDir()
            }
        } else if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageDir()
        } else {
            getInternalStorageDir()
        }

        val targetFile = File(destinationDir, fileName)
        val downloadUrl = model.sourceUrl.takeIf { !it.isNullOrBlank() }
            ?: "https://huggingface.co/${model.id}/resolve/main/$fileName"

        modelLoaderService.downloadModelFromUrl(
            url = downloadUrl,
            targetFile = targetFile,
            expectedSha256 = model.checksum,
            downloadId = model.id
        ).collect { progress ->
            val speedKbps = progress.downloadSpeedBytesPerSec / 1024f
            val isDone = progress.isCompleted || progress.status == DownloadStatus.FAILED || progress.status == DownloadStatus.CANCELLED

            var safDocUriStr: String? = null
            if (isDone && progress.isCompleted && isSafUri && !targetDirOrUri.isNullOrBlank()) {
                try {
                    val treeUri = Uri.parse(targetDirOrUri)
                    val docTree = DocumentFile.fromTreeUri(context, treeUri)
                    if (docTree != null && docTree.canWrite()) {
                        val safDocFile = docTree.createFile("application/octet-stream", fileName)
                        if (safDocFile != null) {
                            safDocUriStr = safDocFile.uri.toString()
                            context.contentResolver.openOutputStream(safDocFile.uri)?.use { output ->
                                targetFile.inputStream().use { input -> input.copyTo(output) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (isDone && progress.isCompleted) {
                try {
                    val validation = validator.validateFile(targetFile)
                    val checksum = validator.computeChecksumSha256(targetFile)

                    val updatedEntity = model.copy(
                        isDownloaded = true,
                        downloadState = ModelDownloadState.AVAILABLE.name,
                        storageLocation = locationLabel,
                        localPath = targetFile.absolutePath,
                        fileUri = safDocUriStr ?: model.fileUri,
                        sizeBytes = if (targetFile.exists() && targetFile.length() > 0) targetFile.length() else progress.downloadedBytes,
                        checksum = checksum,
                        lastVerified = System.currentTimeMillis(),
                        validationStatus = if (validation.isValid) ModelValidationStatus.VALID.name else validation.status.name,
                        architecture = validation.architecture,
                        backend = validation.backend,
                        description = "Downloaded to $locationLabel (${targetFile.absolutePath})"
                    )

                    withContext(Dispatchers.IO) {
                        aiModelDao.updateModel(updatedEntity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val progressState = DownloadProgressState(
                modelId = model.id,
                modelName = model.name,
                bytesDownloaded = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
                progressPercent = progress.progressPercent.toInt(),
                downloadSpeedKbps = speedKbps,
                etaSeconds = progress.etaSeconds.toInt(),
                isFinished = isDone,
                storageLocationLabel = locationLabel,
                destinationPath = safDocUriStr ?: progress.destinationPath ?: targetFile.absolutePath,
                error = progress.error,
                sha256Checksum = progress.sha256Checksum
            )

            emit(progressState)
        }
    }

    suspend fun deleteModelPermanently(model: AiModelEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val path = model.localPath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }

            val uriStr = model.fileUri
            if (!uriStr.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(uriStr)
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    docFile?.delete()
                } catch (_: Exception) {}
            }

            val catalogDefaults = listOf(
                "model_sora_litert_v1",
                "model_wan_13b_gguf",
                "model_sd15_litert",
                "sora-wan-2.1-video-1.3b",
                "sora-ltx-video-0.9.1",
                "sora-sdxl-turbo-image-1.0",
                "sora-flux-1-schnell-image",
                "sora-qwen2.5-coder-7b"
            )

            if (model.id in catalogDefaults) {
                val reset = model.copy(
                    isDownloaded = false,
                    downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                    localPath = null,
                    fileUri = null,
                    checksum = null,
                    lastVerified = 0L,
                    validationStatus = "UNVERIFIED"
                )
                aiModelDao.updateModel(reset)
            } else {
                aiModelDao.deleteModel(model)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeInitialModelHeader(file: File, format: String, modelName: String) {
        FileOutputStream(file).use { fos ->
            val fmt = format.uppercase().trim()
            val headerBytes = ByteArray(128)
            when (fmt) {
                "GGUF" -> {
                    headerBytes[0] = 0x47.toByte()
                    headerBytes[1] = 0x47.toByte()
                    headerBytes[2] = 0x55.toByte()
                    headerBytes[3] = 0x46.toByte()
                    headerBytes[4] = 0x03.toByte()
                }
                "LITERET", "TFLITE" -> {
                    headerBytes[4] = 0x54.toByte()
                    headerBytes[5] = 0x46.toByte()
                    headerBytes[6] = 0x4C.toByte()
                    headerBytes[7] = 0x33.toByte()
                }
                "SAFETENSORS" -> {
                    headerBytes[0] = 0x20.toByte()
                    headerBytes[8] = '{'.code.toByte()
                    headerBytes[9] = '"'.code.toByte()
                }
                else -> {
                    headerBytes[0] = 'P'.code.toByte()
                    headerBytes[1] = 'K'.code.toByte()
                }
            }
            fos.write(headerBytes)
        }
    }

    private fun finalizePhysicalFile(tempFile: File, targetFile: File) {
        if (tempFile.exists()) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
        }
    }

    fun cancelDownload(modelId: String) {
        activeDownloads[modelId] = false
        modelLoaderService.cancelDownload(modelId)
    }
}

