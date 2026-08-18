package com.example.ai.downloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED
}

data class DownloadProgress(
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val downloadSpeedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val sha256Checksum: String? = null,
    val destinationPath: String? = null
)

class ModelLoaderService(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeDownloadCalls = ConcurrentHashMap<String, okhttp3.Call>()

    fun getModelsDirectory(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }

    /**
     * Compute SHA-256 checksum of a file without loading the whole file into RAM.
     */
    suspend fun computeSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext ""
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify SHA-256 checksum against an expected string.
     */
    suspend fun verifyChecksum(file: File, expectedSha256: String): Boolean = withContext(Dispatchers.IO) {
        if (expectedSha256.isBlank()) return@withContext true
        val computed = computeSha256(file)
        computed.equals(expectedSha256.trim(), ignoreCase = true)
    }

    /**
     * Cancel an active download by ID or URL.
     */
    fun cancelDownload(downloadId: String) {
        activeDownloadCalls.remove(downloadId)?.cancel()
    }

    /**
     * Stream download a model binary from HTTPS URL directly to destination file with real-time progress
     * and optional SHA-256 checksum verification.
     */
    fun downloadModelFromUrl(
        url: String,
        targetFile: File,
        expectedSha256: String? = null,
        downloadId: String = targetFile.name
    ): Flow<DownloadProgress> = flow {
        if (targetFile.parentFile?.exists() == false) {
            targetFile.parentFile?.mkdirs()
        }

        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        var existingBytes = 0L

        if (tempFile.exists()) {
            existingBytes = tempFile.length()
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "SoraStudioAndroid/1.0 (Mobile AI Engine)")

        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val request = requestBuilder.build()
        val call = client.newCall(request)
        activeDownloadCalls[downloadId] = call

        try {
            emit(
                DownloadProgress(
                    downloadedBytes = existingBytes,
                    totalBytes = 0L,
                    progressPercent = 0f,
                    status = DownloadStatus.DOWNLOADING,
                    destinationPath = targetFile.absolutePath
                )
            )

            val response = call.execute()
            val isPartial = response.code == 206
            val isSuccess = response.isSuccessful

            if (!isSuccess && !isPartial) {
                if (response.code == 416 && tempFile.exists()) {
                    tempFile.delete()
                    existingBytes = 0L
                    val fallbackRequest = Request.Builder()
                        .url(url)
                        .header("User-Agent", "SoraStudioAndroid/1.0 (Mobile AI Engine)")
                        .build()
                    val fallbackCall = client.newCall(fallbackRequest)
                    activeDownloadCalls[downloadId] = fallbackCall
                    val fallbackResponse = fallbackCall.execute()
                    if (!fallbackResponse.isSuccessful) {
                        emit(
                            DownloadProgress(
                                status = DownloadStatus.FAILED,
                                error = "HTTP ${fallbackResponse.code}: ${fallbackResponse.message}"
                            )
                        )
                        return@flow
                    }
                    processResponseBody(fallbackResponse, tempFile, targetFile, 0L, expectedSha256) { progress ->
                        emit(progress)
                    }
                    return@flow
                }

                emit(
                    DownloadProgress(
                        status = DownloadStatus.FAILED,
                        error = "HTTP ${response.code}: ${response.message}"
                    )
                )
                return@flow
            }

            val append = isPartial && existingBytes > 0
            val startOffset = if (append) existingBytes else 0L

            processResponseBody(response, tempFile, targetFile, startOffset, expectedSha256) { progress ->
                emit(progress)
            }

        } catch (e: Exception) {
            if (call.isCanceled()) {
                emit(
                    DownloadProgress(
                        status = DownloadStatus.CANCELLED,
                        error = "Download cancelled by user"
                    )
                )
            } else {
                emit(
                    DownloadProgress(
                        status = DownloadStatus.FAILED,
                        error = e.localizedMessage ?: "Unknown error downloading model file"
                    )
                )
            }
        } finally {
            activeDownloadCalls.remove(downloadId)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Legacy wrapper method for HuggingFace downloads compatibility.
     */
    fun downloadModelFromHuggingFace(
        url: String,
        fileName: String,
        expectedSha256: String? = null
    ): Flow<DownloadProgress> {
        val targetFile = File(getModelsDirectory(), fileName)
        return downloadModelFromUrl(url, targetFile, expectedSha256, fileName)
    }

    private suspend inline fun processResponseBody(
        response: okhttp3.Response,
        tempFile: File,
        targetFile: File,
        startOffset: Long,
        expectedSha256: String?,
        crossinline onProgress: suspend (DownloadProgress) -> Unit
    ) {
        val body = response.body
        if (body == null) {
            onProgress(
                DownloadProgress(
                    status = DownloadStatus.FAILED,
                    error = "Response body was null"
                )
            )
            return
        }

        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0) contentLength + startOffset else 0L

        var downloadedBytes = startOffset
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        var lastEmitTime = System.currentTimeMillis()
        var bytesSinceLastEmit = 0L

        val digest = MessageDigest.getInstance("SHA-256")

        body.byteStream().use { inputStream: InputStream ->
            FileOutputStream(tempFile, startOffset > 0).use { outputStream ->
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastEmit += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastEmitTime

                    if (timeDelta >= 300) {
                        val speedBytesPerSec = if (timeDelta > 0) (bytesSinceLastEmit * 1000) / timeDelta else 0L
                        val remainingBytes = totalBytes - downloadedBytes
                        val etaSec = if (speedBytesPerSec > 0 && remainingBytes > 0) remainingBytes / speedBytesPerSec else 0L
                        val pct = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) * 100f else 0f

                        onProgress(
                            DownloadProgress(
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                progressPercent = pct,
                                downloadSpeedBytesPerSec = speedBytesPerSec,
                                etaSeconds = etaSec,
                                status = DownloadStatus.DOWNLOADING,
                                destinationPath = targetFile.absolutePath
                            )
                        )

                        lastEmitTime = now
                        bytesSinceLastEmit = 0L
                    }
                }
                outputStream.flush()
            }
        }

        if (tempFile.exists()) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)
        }

        onProgress(
            DownloadProgress(
                downloadedBytes = downloadedBytes,
                totalBytes = downloadedBytes,
                progressPercent = 99f,
                status = DownloadStatus.VERIFYING,
                destinationPath = targetFile.absolutePath
            )
        )

        val computedSha256 = if (expectedSha256 != null || downloadedBytes < 100_000_000L) {
            computeSha256(targetFile)
        } else {
            if (startOffset == 0L) {
                digest.digest().joinToString("") { "%02x".format(it) }
            } else {
                computeSha256(targetFile)
            }
        }

        if (expectedSha256 != null && !computedSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
            onProgress(
                DownloadProgress(
                    downloadedBytes = downloadedBytes,
                    totalBytes = downloadedBytes,
                    progressPercent = 100f,
                    status = DownloadStatus.FAILED,
                    error = "Checksum mismatch: Expected $expectedSha256, got $computedSha256",
                    sha256Checksum = computedSha256,
                    destinationPath = targetFile.absolutePath
                )
            )
            return
        }

        onProgress(
            DownloadProgress(
                downloadedBytes = downloadedBytes,
                totalBytes = downloadedBytes,
                progressPercent = 100f,
                status = DownloadStatus.COMPLETED,
                isCompleted = true,
                sha256Checksum = computedSha256,
                destinationPath = targetFile.absolutePath
            )
        )
    }
}
