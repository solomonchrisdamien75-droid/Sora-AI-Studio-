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
    val isDownloaded: Boolean = false,
    val downloadState: String = "NOT_DOWNLOADED", // NOT_DOWNLOADED, QUEUED, DOWNLOADING, AVAILABLE, MISSING, INVALID, FAILED
    val storageLocation: String = "INTERNAL", // INTERNAL, SD_CARD, CUSTOM, SAF_URI
    val localPath: String? = null,
    val fileUri: String? = null,
    val checksum: String? = null,
    val lastVerified: Long = 0L,
    val validationStatus: String = "UNVERIFIED", // VALID, MISSING_FILE, ZERO_BYTE_FILE, UNREADABLE, UNVERIFIED
    val architecture: String? = null,
    val backend: String? = null,
    val quantization: String? = null,
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

@Entity(tableName = "quantization_history")
data class QuantizationHistoryEntity(
    @PrimaryKey val id: String,
    val sourceModelId: String,
    val sourceModelName: String,
    val quantizedModelId: String,
    val quantizedModelName: String,
    val precisionFormat: String, // e.g. Q4_K_M, Q3_K_S, Q2_K, INT8, INT4
    val originalSizeBytes: Long,
    val quantizedSizeBytes: Long,
    val originalRamMb: Int,
    val quantizedRamMb: Int,
    val ramSavedPercent: Int,
    val iterationsCount: Int = 1,
    val tradeoffObjective: String = "BALANCED", // REDUCE_DISK_SIZE, REDUCE_RAM, REDUCE_COMPUTE, REDUCE_GEN_COST
    val storageLocation: String = "Phone Storage",
    val destinationPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRequantized: Boolean = false,
    val benchmarkSpeedBefore: String = "2.4 fps",
    val benchmarkSpeedAfter: String = "5.8 fps"
)

