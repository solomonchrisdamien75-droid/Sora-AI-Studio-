package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_model_capabilities",
    foreignKeys = [
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["modelId"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["modelId"])]
)
data class ModelCapabilityEntity(
    @PrimaryKey val modelId: String,
    val taskTypes: String, // Comma-separated or JSON list of tasks (chat, text, image, video, audio, vision, embeddings)
    val chat: Boolean = false,
    val textGeneration: Boolean = false,
    val imageGeneration: Boolean = false,
    val videoGeneration: Boolean = false,
    val audioGeneration: Boolean = false,
    val vision: Boolean = false,
    val embeddings: Boolean = false
)
