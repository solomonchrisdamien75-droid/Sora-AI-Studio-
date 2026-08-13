package com.example.cloud

import com.example.data.SoraCloudDao
import com.example.data.SoraCloudServerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

data class CloudJobRequest(
    val modelName: String,
    val prompt: String,
    val mode: String,
    val resolution: String,
    val fps: Int,
    val durationSec: Int
)

data class CloudJobResponse(
    val jobId: String,
    val status: String,
    val assignedWorkerIp: String,
    val estimatedQueueTimeSec: Int
)

class SoraCloudClient(private val soraCloudDao: SoraCloudDao) {

    suspend fun discoverLocalNetworkServers(): List<SoraCloudServerEntity> = withContext(Dispatchers.IO) {
        delay(600) // Simulate mDNS & SSDP local discovery scan
        val detected = listOf(
            SoraCloudServerEntity(
                id = "box-01",
                name = "Sora Cloud Box Pro (Local Wi-Fi)",
                ipAddress = "192.168.1.140",
                port = 8080,
                isLocalNetwork = true,
                isConnected = true,
                totalRamGb = 64.0f,
                availableRamGb = 52.4f,
                activeUsers = 3,
                latencyMs = 8,
                gpuModel = "Dual NVIDIA RTX 4090 / Custom Sora NPU"
            ),
            SoraCloudServerEntity(
                id = "cluster-remote-02",
                name = "Sora Private AI Server (SSL Secure)",
                ipAddress = "ai.soracloud.private",
                port = 443,
                isLocalNetwork = false,
                isConnected = false,
                totalRamGb = 128.0f,
                availableRamGb = 96.0f,
                activeUsers = 7,
                latencyMs = 45,
                gpuModel = "NVIDIA H100 SXM5"
            )
        )
        detected.forEach { soraCloudDao.insertServer(it) }
        return@withContext detected
    }

    suspend fun submitJob(server: SoraCloudServerEntity, request: CloudJobRequest): CloudJobResponse = withContext(Dispatchers.IO) {
        delay(300)
        return@withContext CloudJobResponse(
            jobId = "sora_cloud_job_${System.currentTimeMillis()}",
            status = "QUEUED",
            assignedWorkerIp = server.ipAddress,
            estimatedQueueTimeSec = 4
        )
    }

    fun streamJobStatus(serverIp: String, jobId: String): Flow<CloudJobStatusUpdate> = flow {
        for (step in 1..20) {
            delay(250)
            val pct = step * 5
            emit(
                CloudJobStatusUpdate(
                    jobId = jobId,
                    progressPercent = pct,
                    currentFrame = step * 6,
                    totalFrames = 120,
                    status = if (pct == 100) "COMPLETED" else "PROCESSING",
                    outputVideoUrl = if (pct == 100) "https://sora-cloud.local/renders/$jobId.mp4" else null
                )
            )
        }
    }
}

data class CloudJobStatusUpdate(
    val jobId: String,
    val progressPercent: Int,
    val currentFrame: Int,
    val totalFrames: Int,
    val status: String,
    val outputVideoUrl: String? = null
)
