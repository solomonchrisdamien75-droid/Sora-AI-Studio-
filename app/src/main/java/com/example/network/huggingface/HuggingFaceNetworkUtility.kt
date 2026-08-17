package com.example.network.huggingface

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-grade Network Utility using Retrofit & OkHttp to fetch model metadata,
 * list repository binaries, and stream large .bin / .gguf / .safetensors / .onnx files
 * directly into local Android app storage with live progress telemetry, resumable byte ranges,
 * and SHA-256 checksum computation.
 */
class HuggingFaceNetworkUtility(
    private val apiService: HuggingFaceApiService = HuggingFaceRetrofitClient.apiService
) {
    private val activeCancellations = ConcurrentHashMap<String, Boolean>()

    /**
     * Search models on Hugging Face using Retrofit.
     */
    suspend fun searchModels(
        query: String,
        filter: String? = null,
        author: String? = null,
        limit: Int = 20
    ): Result<List<HfModelItem>> = withContext(Dispatchers.IO) {
        try {
            val results = apiService.searchModels(
                search = query.ifBlank { null },
                filter = filter,
                author = author,
                limit = limit,
                full = true
            )
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch complete metadata and repository card information for a model.
     */
    suspend fun fetchModelMetadata(repoId: String): Result<HfModelDetail> = withContext(Dispatchers.IO) {
        try {
            val detail = apiService.getModelDetails(repoId)
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List all files and binaries (.bin, .safetensors, .gguf, .onnx, config.json) in a Hugging Face repo.
     */
    suspend fun listRepositoryFiles(
        repoId: String,
        revision: String = "main"
    ): Result<List<HfSibling>> = withContext(Dispatchers.IO) {
        try {
            // First check if getModelDetails already contains siblings
            val detailResult = runCatching { apiService.getModelDetails(repoId) }
            val siblings = detailResult.getOrNull()?.siblings
            if (!siblings.isNullOrEmpty()) {
                return@withContext Result.success(siblings)
            }

            val treeFiles = apiService.listRepoFiles(repoId, revision)
            Result.success(treeFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Filter and identify all binary model weights in a repository.
     * Looks for .bin (e.g. pytorch_model.bin), .safetensors, .gguf, .onnx, .tflite files.
     */
    suspend fun findBinAndWeightFiles(
        repoId: String,
        revision: String = "main"
    ): Result<List<HfSibling>> = withContext(Dispatchers.IO) {
        listRepositoryFiles(repoId, revision).map { files ->
            files.filter { file ->
                val name = file.rfilename.lowercase()
                name.endsWith(".bin") ||
                name.endsWith(".safetensors") ||
                name.endsWith(".gguf") ||
                name.endsWith(".onnx") ||
                name.endsWith(".tflite") ||
                name.endsWith(".pt") ||
                name.endsWith(".pth") ||
                name.endsWith(".json")
            }
        }
    }

    /**
     * Streams binary/model files directly from Hugging Face via Retrofit to local Android storage.
     *
     * Supports:
     * - Resumable downloads via HTTP Range headers
     * - Direct streaming into internal app storage (context.filesDir) or SD Card
     * - SAF DocumentFile URI stream writing
     * - On-the-fly SHA-256 checksum calculation
     * - Real-time progress, speed (KB/s / MB/s), and ETA calculation
     */
    fun downloadModelBinFile(
        context: Context,
        repoId: String,
        filename: String,
        destinationFile: File? = null,
        destinationSafUri: Uri? = null,
        revision: String = "main",
        authToken: String? = null
    ): Flow<HfDownloadProgress> = flow {
        val downloadKey = "$repoId/$filename"
        activeCancellations[downloadKey] = false

        // Determine destination file if not SAF URI
        val targetFile: File = destinationFile ?: File(
            File(context.filesDir, "ai_models").apply { mkdirs() },
            filename.substringAfterLast("/")
        )

        var existingBytes = 0L
        val isSaf = destinationSafUri != null

        // Check for existing partial file for resumable downloading (only for File destinations)
        if (!isSaf && targetFile.exists()) {
            existingBytes = targetFile.length()
        }

        val rangeHeader = if (existingBytes > 0 && !isSaf) "bytes=$existingBytes-" else null
        val authHeader = authToken?.let { "Bearer $it" }

        val response: Response<ResponseBody> = try {
            apiService.downloadModelFile(
                repoId = repoId,
                revision = revision,
                filename = filename,
                rangeHeader = rangeHeader,
                authHeader = authHeader
            )
        } catch (e: Exception) {
            emit(
                HfDownloadProgress(
                    modelId = repoId,
                    fileName = filename,
                    bytesDownloaded = existingBytes,
                    totalBytes = existingBytes,
                    progressPercent = 0,
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    error = "Network connection failed: ${e.localizedMessage}"
                )
            )
            return@flow
        }

        if (!response.isSuccessful || response.body() == null) {
            // If range is not satisfiable (e.g. 416), file might already be complete or server doesn't support range
            if (response.code() == 416 && existingBytes > 0) {
                emit(
                    HfDownloadProgress(
                        modelId = repoId,
                        fileName = filename,
                        bytesDownloaded = existingBytes,
                        totalBytes = existingBytes,
                        progressPercent = 100,
                        speedBytesPerSec = 0,
                        etaSeconds = 0,
                        isFinished = true,
                        destinationPath = targetFile.absolutePath
                    )
                )
                return@flow
            }

            emit(
                HfDownloadProgress(
                    modelId = repoId,
                    fileName = filename,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    progressPercent = 0,
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    error = "HTTP Error ${response.code()}: ${response.message()}"
                )
            )
            return@flow
        }

        val body = response.body()!!
        val contentLength = body.contentLength()
        val isPartial = response.code() == 206
        val totalBytes = if (isPartial) existingBytes + contentLength else if (contentLength > 0) contentLength else 100_000_000L

        var downloadedBytes = if (isPartial) existingBytes else 0L
        val startTime = System.currentTimeMillis()
        var lastEmitTime = startTime
        var lastEmitBytes = downloadedBytes

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024) // 64 KB buffer

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var randomAccessFile: RandomAccessFile? = null

        try {
            inputStream = body.byteStream()

            if (isSaf && destinationSafUri != null) {
                outputStream = context.contentResolver.openOutputStream(destinationSafUri, "wa")
                    ?: context.contentResolver.openOutputStream(destinationSafUri)
                    ?: throw IllegalStateException("Cannot open output stream for SAF Uri: $destinationSafUri")
            } else {
                if (isPartial && targetFile.exists()) {
                    randomAccessFile = RandomAccessFile(targetFile, "rw")
                    randomAccessFile.seek(existingBytes)
                } else {
                    targetFile.parentFile?.mkdirs()
                    outputStream = FileOutputStream(targetFile, false)
                }
            }

            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (activeCancellations[downloadKey] == true || !currentCoroutineContext().isActive) {
                    throw CancellationException("Download cancelled by user.")
                }

                if (randomAccessFile != null) {
                    randomAccessFile.write(buffer, 0, bytesRead)
                } else {
                    outputStream?.write(buffer, 0, bytesRead)
                }

                digest.update(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastEmitTime >= 150 || downloadedBytes >= totalBytes) {
                    val timeDeltaSec = ((now - lastEmitTime) / 1000.0).coerceAtLeast(0.05)
                    val bytesDelta = downloadedBytes - lastEmitBytes
                    val currentSpeed = (bytesDelta / timeDeltaSec).toLong()

                    val remainingBytes = (totalBytes - downloadedBytes).coerceAtLeast(0L)
                    val etaSec = if (currentSpeed > 0) (remainingBytes / currentSpeed).toInt() else 0
                    val pct = ((downloadedBytes.toDouble() / totalBytes.coerceAtLeast(1L)) * 100).toInt().coerceIn(0, 100)

                    emit(
                        HfDownloadProgress(
                            modelId = repoId,
                            fileName = filename,
                            bytesDownloaded = downloadedBytes,
                            totalBytes = totalBytes,
                            progressPercent = pct,
                            speedBytesPerSec = currentSpeed,
                            etaSeconds = etaSec,
                            isFinished = downloadedBytes >= totalBytes,
                            isResumed = isPartial,
                            destinationPath = if (isSaf) destinationSafUri.toString() else targetFile.absolutePath
                        )
                    )

                    lastEmitTime = now
                    lastEmitBytes = downloadedBytes
                }
            }

            outputStream?.flush()

            val finalChecksum = digest.digest().joinToString("") { "%02x".format(it) }

            emit(
                HfDownloadProgress(
                    modelId = repoId,
                    fileName = filename,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = downloadedBytes,
                    progressPercent = 100,
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    isFinished = true,
                    isResumed = isPartial,
                    destinationPath = if (isSaf) destinationSafUri.toString() else targetFile.absolutePath,
                    sha256Checksum = finalChecksum
                )
            )

        } catch (e: CancellationException) {
            emit(
                HfDownloadProgress(
                    modelId = repoId,
                    fileName = filename,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    progressPercent = ((downloadedBytes.toDouble() / totalBytes.coerceAtLeast(1L)) * 100).toInt(),
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    error = "Download paused / cancelled."
                )
            )
        } catch (e: Exception) {
            emit(
                HfDownloadProgress(
                    modelId = repoId,
                    fileName = filename,
                    bytesDownloaded = downloadedBytes,
                    totalBytes = totalBytes,
                    progressPercent = ((downloadedBytes.toDouble() / totalBytes.coerceAtLeast(1L)) * 100).toInt(),
                    speedBytesPerSec = 0,
                    etaSeconds = 0,
                    error = "Download failed: ${e.localizedMessage}"
                )
            )
        } finally {
            runCatching { inputStream?.close() }
            runCatching { outputStream?.close() }
            runCatching { randomAccessFile?.close() }
            activeCancellations.remove(downloadKey)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Cancels an ongoing download stream.
     */
    fun cancelDownload(repoId: String, filename: String) {
        val downloadKey = "$repoId/$filename"
        activeCancellations[downloadKey] = true
    }
}
