package com.example.data.local.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.GenerationTaskEntity
import com.example.data.local.entities.ModelCapabilityEntity
import com.example.data.local.entities.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelRegistryDao {
    @Query("SELECT * FROM local_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM local_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)

    @Delete
    suspend fun deleteModel(model: ModelEntity)

    @Query("DELETE FROM local_models WHERE modelId = :modelId")
    suspend fun deleteModelById(modelId: String)
}

@Dao
interface ModelCapabilityRegistryDao {
    @Query("SELECT * FROM local_model_capabilities WHERE modelId = :modelId LIMIT 1")
    suspend fun getCapabilitiesForModel(modelId: String): ModelCapabilityEntity?

    @Query("SELECT * FROM local_model_capabilities")
    suspend fun getAllCapabilities(): List<ModelCapabilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapabilities(capabilities: ModelCapabilityEntity)

    @Query("DELETE FROM local_model_capabilities WHERE modelId = :modelId")
    suspend fun deleteCapabilities(modelId: String)
}

@Dao
interface GenerationTaskDao {
    @Query("SELECT * FROM generation_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<GenerationTaskEntity>>

    @Query("SELECT * FROM generation_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): GenerationTaskEntity?

    @Query("SELECT * FROM generation_tasks WHERE taskId = :taskId LIMIT 1")
    fun observeTaskById(taskId: String): Flow<GenerationTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: GenerationTaskEntity)

    @Query("UPDATE generation_tasks SET status = :status, progressPercent = :progress, currentStep = :step, totalSteps = :total, statusMessage = :message, outputUri = :output, errorMessage = :error, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun updateTaskProgress(
        taskId: String,
        status: String,
        progress: Float,
        step: Int,
        total: Int,
        message: String,
        output: String?,
        error: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun deleteTask(task: GenerationTaskEntity)

    @Query("DELETE FROM generation_tasks WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: String)
}
