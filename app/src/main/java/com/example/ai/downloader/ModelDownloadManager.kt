package com.example.ai.downloader

import android.content.Context
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
    val error: String? = null
)

class ModelDownloadManager(
    private val context: Context,
    private val aiModelDao: AiModelDao
) {
    private val activeDownloads = mutableMapOf<String, Boolean>()

    fun startDownload(model: HuggingFaceModelInfo): Flow<DownloadProgressState> = flow {
        activeDownloads[model.id] = true

        val modelsDir = File(context.filesDir, "ai_models").apply { mkdirs() }
        val targetFile = File(modelsDir, "${model.name.replace(" ", "_")}.${model.format.lowercase()}")

        val totalBytes = model.sizeBytes
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 40).coerceAtLeast(1024L)
        val startTime = System.currentTimeMillis()

        while (downloadedBytes < totalBytes && activeDownloads[model.id] == true) {
            delay(150) // Simulate downloading chunks over network
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
                    isFinished = isDone
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
                    description = "Downloaded from Hugging Face: ${model.author}"
                )
                withContext(Dispatchers.IO) {
                    aiModelDao.insertModel(entity)
                }
                break
            }
        }
    }

    fun cancelDownload(modelId: String) {
        activeDownloads[modelId] = false
    }
}
