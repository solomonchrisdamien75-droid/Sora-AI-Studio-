package com.example.ai.downloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progressPercent: Float,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class ModelLoaderService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getModelsDirectory(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }

    fun downloadModelFromHuggingFace(
        url: String,
        fileName: String
    ): Flow<DownloadProgress> = flow {
        val targetDir = getModelsDirectory()
        val outputFile = File(targetDir, fileName)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SoraStudioAndroid/1.0")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadProgress(0, 0, 0f, error = "HTTP ${response.code}: ${response.message}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadProgress(0, 0, 0f, error = "Response body was null"))
                return@flow
            }

            val contentLength = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { inputStream: InputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val pct = if (contentLength > 0) (downloadedBytes.toFloat() / contentLength) * 100f else 0f
                        emit(DownloadProgress(downloadedBytes, contentLength, pct))
                    }
                    outputStream.flush()
                }
            }

            emit(DownloadProgress(downloadedBytes, contentLength, 100f, isCompleted = true))

        } catch (e: Exception) {
            emit(DownloadProgress(0, 0, 0f, error = e.localizedMessage ?: "Unknown error downloading model"))
        }
    }.flowOn(Dispatchers.IO)
}
