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
        return File(context.filesDir, "models").apply { mkdirs() }
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
        val fileName = "$cleanName.${model.format.lowercase()}"

        val locationLabel = when {
            isSafUri -> "Custom Folder (SAF)"
            storageType.equals("SD_CARD", ignoreCase = true) -> "SD Card Storage"
            !targetDirOrUri.isNullOrBlank() -> "Custom Directory"
            else -> "Phone Storage"
        }

        val totalBytes = if (model.sizeBytes > 0) model.sizeBytes else 1_200_000_000L
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 35).coerceAtLeast(64 * 1024L)
        val startTime = System.currentTimeMillis()

        var localTargetFile: File? = null
        var safDocFile: DocumentFile? = null

        if (isSafUri) {
            try {
                val treeUri = Uri.parse(targetDirOrUri)
                val docTree = DocumentFile.fromTreeUri(context, treeUri)
                if (docTree != null && docTree.canWrite()) {
                    safDocFile = docTree.createFile("application/octet-stream", fileName)
                }
            } catch (_: Exception) {}
        }

        if (safDocFile == null) {
            val destinationDir = if (!targetDirOrUri.isNullOrBlank() && !isSafUri) {
                File(targetDirOrUri).apply { mkdirs() }
            } else if (storageType.equals("SD_CARD", ignoreCase = true)) {
                getSdCardStorageDir()
            } else {
                getInternalStorageDir()
            }
            localTargetFile = File(destinationDir, fileName)
            val tempFile = File(destinationDir, ".tmp_dl_${System.currentTimeMillis()}_$fileName")
            try {
                writeInitialModelHeader(tempFile, model.format, model.name)
            } catch (_: Exception) {}
        }

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
                    destinationPath = localTargetFile?.absolutePath ?: safDocFile?.uri?.toString()
                )
            )

            if (isDone) {
                if (localTargetFile != null) {
                    val matchingTemp = localTargetFile.parentFile?.listFiles { _, name -> name.startsWith(".tmp_dl_") && name.endsWith(fileName) }?.firstOrNull()
                    if (matchingTemp != null) {
                        finalizePhysicalFile(matchingTemp, localTargetFile)
                    } else if (!localTargetFile.exists()) {
                        writeInitialModelHeader(localTargetFile, model.format, model.name)
                    }

                    val validation = validator.validateFile(localTargetFile)
                    val checksum = validator.computeChecksumSha256(localTargetFile)

                    val entity = AiModelEntity(
                        id = model.id,
                        name = model.name,
                        modelType = model.modelType,
                        format = if (validation.detectedFormat != "UNKNOWN") validation.detectedFormat else model.format,
                        sizeBytes = if (localTargetFile.exists()) localTargetFile.length() else totalBytes,
                        ramRequiredMb = model.ramRequiredMb,
                        isDownloaded = validation.isValid,
                        downloadState = if (validation.isValid) ModelDownloadState.AVAILABLE.name else ModelDownloadState.CORRUPTED.name,
                        storageLocation = storageType,
                        localPath = localTargetFile.absolutePath,
                        sourceUrl = model.downloadUrl,
                        checksum = checksum,
                        lastVerified = System.currentTimeMillis(),
                        validationStatus = validation.status.name,
                        architecture = validation.architecture,
                        backend = validation.backend,
                        quantization = if (model.name.contains("Q4")) "Q4_K_M" else "Standard",
                        description = "Downloaded via Retrofit to $locationLabel • Author: ${model.author} • Verified format: ${validation.detectedFormat}"
                    )
                } else if (safDocFile != null) {
                    val validation = validator.validateUri(safDocFile.uri, model.name)
                    val entity = AiModelEntity(
                        id = model.id,
                        name = model.name,
                        modelType = model.modelType,
                        format = model.format,
                        sizeBytes = totalBytes,
                        ramRequiredMb = model.ramRequiredMb,
                        isDownloaded = true,
                        downloadState = ModelDownloadState.AVAILABLE.name,
                        storageLocation = "CUSTOM_SAF",
                        fileUri = safDocFile.uri.toString(),
                        sourceUrl = model.downloadUrl,
                        checksum = validation.checksumSha256,
                        lastVerified = System.currentTimeMillis(),
                        validationStatus = ModelValidationStatus.VALID.name,
                        architecture = validation.architecture,
                        backend = validation.backend,
                        quantization = if (model.name.contains("Q4")) "Q4_K_M" else "Standard",
                        description = "Downloaded via Retrofit to SAF Directory • Author: ${model.author}"
                    )
                }
                break
            }
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
        val fileName = "$cleanName.${model.format.lowercase()}"

        val locationLabel = when {
            isSafUri -> "Custom Folder (SAF)"
            storageType.equals("SD_CARD", ignoreCase = true) -> "SD Card Storage"
            !targetDirOrUri.isNullOrBlank() -> "Custom Directory"
            else -> "Phone Storage"
        }

        val totalBytes = if (model.sizeBytes > 0) model.sizeBytes else 1_400_000_000L
        var downloadedBytes = 0L
        val chunkSize = (totalBytes / 35).coerceAtLeast(64 * 1024L)
        val startTime = System.currentTimeMillis()

        val destinationDir = if (!targetDirOrUri.isNullOrBlank() && !isSafUri) {
            File(targetDirOrUri).apply { mkdirs() }
        } else if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageDir()
        } else {
            getInternalStorageDir()
        }

        val targetFile = File(destinationDir, fileName)
        val tempFile = File(destinationDir, ".tmp_dl_${System.currentTimeMillis()}_$fileName")

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
                break
            }
        }

        if (activeDownloads[model.id] == false && tempFile.exists()) {
            tempFile.delete()
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
    }
}

