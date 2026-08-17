package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_models",
    indices = [Index(value = ["modelId"], unique = true), Index(value = ["format"])]
)
data class ModelEntity(
    @PrimaryKey val modelId: String,
    val name: String,
    val format: String, // GGUF, ONNX, TFLITE, SAFETENSORS
    val backend: String, // llama.cpp, ONNX Runtime, LiteRT
    val localPath: String,
    val fileSize: Long,
    val version: String = "1.0",
    val architecture: String = "Transformer",
    val ramRequiredMb: Int = 1024,
    val downloadStatus: String = "DOWNLOADED",
    val createdAt: Long = System.currentTimeMillis()
)
