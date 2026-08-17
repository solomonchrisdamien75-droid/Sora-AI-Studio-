package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "generation_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["modelId"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["taskId"]), Index(value = ["modelId"]), Index(value = ["status"])]
)
data class GenerationTaskEntity(
    @PrimaryKey val taskId: String,
    val modelId: String?,
    val taskType: String, // TEXT_GEN, IMAGE_GEN, VIDEO_GEN, AUDIO_GEN, EMBEDDING
    val prompt: String,
    val status: String, // QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    val progressPercent: Float = 0f,
    val currentStep: Int = 0,
    val totalSteps: Int = 100,
    val statusMessage: String = "Queued",
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
