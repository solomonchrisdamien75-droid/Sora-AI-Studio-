package com.example.ai.downloader

import android.content.Context
import android.os.Environment
import com.example.data.AiModelDao
import com.example.data.AiModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

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
    private val activeDownloads = mutableMapOf<String, Boolean>()

    fun getInternalStorageDir(): File {
        return File(context.filesDir, "ai_models").apply { mkdirs() }
    }

    fun getSdCardStorageDir(): File {
        // Try external files dirs or SD card path
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
            "SD Card (${destinationDir.absolutePath})"
        } else if (!targetDir.isNullOrBlank()) {
            "Custom Path (${destinationDir.name})"
        } else {
            "Internal Storage"
        }

        val targetFile = File(destinationDir, "${model.name.replace(" ", "_").replace("/", "_")}.${model.format.lowercase()}")

        val totalBytes = model.sizeBytes
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 35).coerceAtLeast(1024L)
        val startTime = System.currentTimeMillis()

        while (downloadedBytes < totalBytes && activeDownloads[model.id] == true) {
            delay(120) // Simulate downloading chunks over network
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
                // Save model entry to local Room database
                val entity = AiModelEntity(
                    id = model.id,
                    name = model.name,
                    modelType = model.modelType,
                    format = model.format,
                    sizeBytes = model.sizeBytes,
                    ramRequiredMb = model.ramRequiredMb,
                    isDownloaded = true,
                    localPath = targetFile.absolutePath,
                    sourceUrl = model.downloadUrl,
                    description = "Downloaded to $locationLabel • Author: ${model.author}"
                )
                withContext(Dispatchers.IO) {
                    aiModelDao.insertModel(entity)
                }
                break
            }
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
            "Custom Directory"
        } else {
            "Internal Phone Storage"
        }

        val targetFile = File(destinationDir, "${model.name.replace(" ", "_")}.${model.format.lowercase()}")

        val totalBytes = if (model.sizeBytes > 0) model.sizeBytes else 1024L * 1024L * 1024L * 2L
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 35).coerceAtLeast(1024L)
        val startTime = System.currentTimeMillis()

        while (downloadedBytes < totalBytes && activeDownloads[model.id] == true) {
            delay(120)
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
                val updated = model.copy(
                    isDownloaded = true,
                    localPath = targetFile.absolutePath,
                    description = if (model.description.isNotBlank()) "${model.description} (Saved in $locationLabel)" else "Saved in $locationLabel"
                )
                withContext(Dispatchers.IO) {
                    aiModelDao.updateModel(updated)
                }
                break
            }
        }
    }

    suspend fun importManualModel(entity: AiModelEntity) = withContext(Dispatchers.IO) {
        aiModelDao.insertModel(entity)
    }

    fun cancelDownload(modelId: String) {
        activeDownloads[modelId] = false
    }
}
