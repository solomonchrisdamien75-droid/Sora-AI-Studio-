package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_models")
data class AiModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val modelType: String, // TEXT, IMAGE, VIDEO, AUDIO, VISION
    val format: String, // GGUF, SAFETENSORS, LITERET, ONNX, MNN, NCNN
    val sizeBytes: Long,
    val ramRequiredMb: Int,
    val isDownloaded: Boolean,
    val localPath: String? = null,
    val sourceUrl: String? = null,
    val description: String = "",
    val version: String = "1.0",
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val recommendedFps: Int = 24,
    val recommendedResolution: String = "1024x1024"
)

@Entity(tableName = "generation_jobs")
data class GenerationJobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val prompt: String,
    val generationType: String, // TEXT_TO_VIDEO, IMAGE_TO_VIDEO, VIDEO_TO_VIDEO, IMAGE_GEN, UPSCALING
    val mode: String, // FAST, BALANCED, CINEMA
    val progressPercent: Int = 0,
    val currentFrame: Int = 0,
    val totalFrames: Int = 120,
    val fps: Float = 24.0f,
    val status: String, // QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 5,
    val resolution: String = "1080p",
    val previewUri: String? = null,
    val outputVideoUri: String? = null,
    val backendUsed: String = "LiteRT/Vulkan",
    val modelName: String = "Sora-LiteRT-v1",
    val errorMessage: String? = null
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailUri: String? = null,
    val sceneCount: Int = 1,
    val durationSeconds: Int = 10,
    val scriptText: String = "",
    val videoPath: String? = null
)

@Entity(tableName = "sora_cloud_servers")
data class SoraCloudServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 8080,
    val isLocalNetwork: Boolean = true,
    val isConnected: Boolean = false,
    val totalRamGb: Float = 64.0f,
    val availableRamGb: Float = 48.0f,
    val activeUsers: Int = 2,
    val latencyMs: Long = 12,
    val gpuModel: String = "NVIDIA RTX 4090 / Custom Sora NPU"
)

@Entity(tableName = "gallery_items")
data class GalleryItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mediaType: String, // VIDEO, IMAGE
    val filePath: String,
    val durationMs: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val createdAt: Long = System.currentTimeMillis(),
    val prompt: String = "",
    val isFavorite: Boolean = false,
    val resolutionLabel: String = "1080p"
)
