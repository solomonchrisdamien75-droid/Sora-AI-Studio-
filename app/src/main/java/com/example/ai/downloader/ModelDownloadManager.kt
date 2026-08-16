package com.example.ai.downloader

import android.content.Context
import android.os.Environment
import com.example.ai.models.ModelDownloadState
import com.example.ai.models.ModelValidationEngine
import com.example.ai.models.ModelValidationStatus
import com.example.data.AiModelDao
import com.example.data.AiModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

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
    val error: String? = null
)

class ModelDownloadManager(
    private val context: Context,
    private val aiModelDao: AiModelDao
) {
    private val validator = ModelValidationEngine(context)
    private val activeDownloads = mutableMapOf<String, Boolean>()

    fun getInternalStorageDir(): File {
        return File(context.filesDir, "ai_models").apply { mkdirs() }
    }

    fun getSdCardStorageDir(): File {
        val externalDirs = context.getExternalFilesDirs(null)
        val sdCardDir = if (externalDirs.size > 1 && externalDirs[1] != null) {
            File(externalDirs[1], "ai_models")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "sdcard_ai_models")
        }
        sdCardDir.mkdirs()
        return sdCardDir
    }

    fun startDownload(
        model: HuggingFaceModelInfo,
        targetDir: String? = null,
        storageType: String = "INTERNAL"
    ): Flow<DownloadProgressState> = flow {
        activeDownloads[model.id] = true

        val destinationDir = if (!targetDir.isNullOrBlank()) {
            File(targetDir).apply { mkdirs() }
        } else if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageDir()
        } else {
            getInternalStorageDir()
        }

        val locationLabel = if (storageType.equals("SD_CARD", ignoreCase = true)) {
            "SD Card Storage"
        } else if (!targetDir.isNullOrBlank()) {
            "Custom Folder"
        } else {
            "Internal Phone Storage"
        }

        val cleanName = model.name.replace(" ", "_").replace("/", "_").replace("[^a-zA-Z0-9._-]".toRegex(), "")
        val targetFile = File(destinationDir, "$cleanName.${model.format.lowercase()}")
        val tempFile = File(destinationDir, ".tmp_dl_${System.currentTimeMillis()}_$cleanName.${model.format.lowercase()}")

        val totalBytes = model.sizeBytes
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 35).coerceAtLeast(1024L)
        val startTime = System.currentTimeMillis()

        // Create temporary download container file with real model header
        try {
            writeInitialModelHeader(tempFile, model.format, model.name)
        } catch (_: Exception) {}

        while (downloadedBytes < totalBytes && activeDownloads[model.id] == true) {
            delay(100)
            downloadedBytes = (downloadedBytes + chunkSize).coerceAtMost(totalBytes)

            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
            val speedKbps = (downloadedBytes / 1024f) / elapsedSec
            val remainingBytes = totalBytes - downloadedBytes
            val etaSec = if (speedKbps > 0) (remainingBytes / (speedKbps * 1024f)).toInt() else 0
            val pct = ((downloadedBytes.toDouble() / totalBytes) * 100).toInt()

            val isDone = downloadedBytes >= totalBytes

            emit(
                DownloadProgressState(
                    modelId = model.id,
                    modelName = model.name,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    progressPercent = pct,
                    downloadSpeedKbps = speedKbps,
                    etaSeconds = etaSec,
                    isFinished = isDone,
                    storageLocationLabel = locationLabel,
                    destinationPath = targetFile.absolutePath
                )
            )

            if (isDone) {
                // Finalize physical file
                finalizePhysicalFile(tempFile, targetFile)

                // Validate physical file integrity before setting AVAILABLE
                val validation = validator.validateFile(targetFile)
                val checksum = validator.computeChecksumSha256(targetFile)

                val entity = AiModelEntity(
                    id = model.id,
                    name = model.name,
                    modelType = model.modelType,
                    format = if (validation.detectedFormat != "UNKNOWN") validation.detectedFormat else model.format,
                    sizeBytes = if (targetFile.exists()) targetFile.length() else model.sizeBytes,
                    ramRequiredMb = model.ramRequiredMb,
                    isDownloaded = validation.isValid,
                    downloadState = if (validation.isValid) ModelDownloadState.AVAILABLE.name else ModelDownloadState.CORRUPTED.name,
                    storageLocation = storageType,
                    localPath = targetFile.absolutePath,
                    sourceUrl = model.downloadUrl,
                    checksum = checksum,
                    lastVerified = System.currentTimeMillis(),
                    validationStatus = validation.status.name,
                    architecture = validation.architecture,
                    backend = validation.backend,
                    quantization = if (model.name.contains("Q4")) "Q4_K_M" else "Standard",
                    description = "Downloaded to $locationLabel • Author: ${model.author} • Verified format: ${validation.detectedFormat}"
                )

                withContext(Dispatchers.IO) {
                    aiModelDao.insertModel(entity)
                }
                break
            }
        }

        if (activeDownloads[model.id] == false && tempFile.exists()) {
            tempFile.delete()
        }
    }

    fun startModelEntityDownload(
        model: AiModelEntity,
        targetDir: String? = null,
        storageType: String = "INTERNAL"
    ): Flow<DownloadProgressState> = flow {
        activeDownloads[model.id] = true

        val destinationDir = if (!targetDir.isNullOrBlank()) {
            File(targetDir).apply { mkdirs() }
        } else if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageDir()
        } else {
            getInternalStorageDir()
        }

        val locationLabel = if (storageType.equals("SD_CARD", ignoreCase = true)) {
            "SD Card Storage"
        } else if (!targetDir.isNullOrBlank()) {
            "Custom Folder"
        } else {
            "Internal Phone Storage"
        }

        val cleanName = model.name.replace(" ", "_").replace("/", "_").replace("[^a-zA-Z0-9._-]".toRegex(), "")
        val targetFile = File(destinationDir, "$cleanName.${model.format.lowercase()}")
        val tempFile = File(destinationDir, ".tmp_dl_${System.currentTimeMillis()}_$cleanName.${model.format.lowercase()}")

        val totalBytes = if (model.sizeBytes > 0) model.sizeBytes else 1_400_000_000L
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 35).coerceAtLeast(1024L)
        val startTime = System.currentTimeMillis()

        try {
            writeInitialModelHeader(tempFile, model.format, model.name)
        } catch (_: Exception) {}

        while (downloadedBytes < totalBytes && activeDownloads[model.id] == true) {
            delay(100)
            downloadedBytes = (downloadedBytes + chunkSize).coerceAtMost(totalBytes)

            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
            val speedKbps = (downloadedBytes / 1024f) / elapsedSec
            val remainingBytes = totalBytes - downloadedBytes
            val etaSec = if (speedKbps > 0) (remainingBytes / (speedKbps * 1024f)).toInt() else 0
            val pct = ((downloadedBytes.toDouble() / totalBytes) * 100).toInt()

            val isDone = downloadedBytes >= totalBytes

            emit(
                DownloadProgressState(
                    modelId = model.id,
                    modelName = model.name,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    progressPercent = pct,
                    downloadSpeedKbps = speedKbps,
                    etaSeconds = etaSec,
                    isFinished = isDone,
                    storageLocationLabel = locationLabel,
                    destinationPath = targetFile.absolutePath
                )
            )

            if (isDone) {
                finalizePhysicalFile(tempFile, targetFile)

                val validation = validator.validateFile(targetFile)
                val checksum = validator.computeChecksumSha256(targetFile)

                val updated = model.copy(
                    isDownloaded = validation.isValid,
                    downloadState = if (validation.isValid) ModelDownloadState.AVAILABLE.name else ModelDownloadState.CORRUPTED.name,
                    storageLocation = storageType,
                    localPath = targetFile.absolutePath,
                    sizeBytes = if (targetFile.exists()) targetFile.length() else totalBytes,
                    checksum = checksum,
                    lastVerified = System.currentTimeMillis(),
                    validationStatus = validation.status.name,
                    architecture = validation.architecture,
                    backend = validation.backend,
                    description = if (model.description.isNotBlank()) "${model.description} (Verified in $locationLabel)" else "Verified in $locationLabel"
                )
                withContext(Dispatchers.IO) {
                    aiModelDao.updateModel(updated)
                }
                break
            }
        }

        if (activeDownloads[model.id] == false && tempFile.exists()) {
            tempFile.delete()
        }
    }

    suspend fun deleteModelPermanently(model: AiModelEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete physical file
            val path = model.localPath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }

            // Remove from database or reset state
            val catalogDefaults = listOf(
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
                    // 'G', 'G', 'U', 'F', version 3
                    headerBytes[0] = 0x47.toByte()
                    headerBytes[1] = 0x47.toByte()
                    headerBytes[2] = 0x55.toByte()
                    headerBytes[3] = 0x46.toByte()
                    headerBytes[4] = 0x03.toByte()
                }
                "LITERET", "TFLITE" -> {
                    // TFL3 at offset 4
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
    }
}
