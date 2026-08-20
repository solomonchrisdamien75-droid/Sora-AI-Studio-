package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models")
    suspend fun getAllModelsList(): List<AiModelEntity>

    @Query("SELECT * FROM ai_models WHERE isDownloaded = 1 AND downloadState = 'AVAILABLE'")
    fun getDownloadedModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): AiModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: AiModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<AiModelEntity>)

    @Update
    suspend fun updateModel(model: AiModelEntity)

    @Delete
    suspend fun deleteModel(model: AiModelEntity)

    @Query("DELETE FROM ai_models WHERE id = :id")
    suspend fun deleteModelById(id: String)
}

@Dao
interface GenerationJobDao {
    @Query("SELECT * FROM generation_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<GenerationJobEntity>>

    @Query("SELECT * FROM generation_jobs WHERE status = 'QUEUED' ORDER BY createdAt ASC")
    fun getQueuedJobs(): Flow<List<GenerationJobEntity>>

    @Query("SELECT * FROM generation_jobs WHERE status = 'RUNNING' LIMIT 1")
    fun getRunningJob(): Flow<GenerationJobEntity?>

    @Query("SELECT * FROM generation_jobs WHERE status = 'RUNNING' OR status = 'QUEUED' LIMIT 1")
    fun getActiveJob(): Flow<GenerationJobEntity?>

    @Query("SELECT * FROM generation_jobs WHERE status = 'QUEUED' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextQueuedJob(): GenerationJobEntity?

    @Query("SELECT * FROM generation_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: String): GenerationJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: GenerationJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<GenerationJobEntity>)

    @Update
    suspend fun updateJob(job: GenerationJobEntity)

    @Query("UPDATE generation_jobs SET status = :status WHERE id = :id")
    suspend fun updateJobStatus(id: String, status: String)

    @Query("DELETE FROM generation_jobs WHERE id = :id")
    suspend fun deleteJobById(id: String)

    @Query("DELETE FROM generation_jobs WHERE status IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    suspend fun deleteFinishedJobs()

    @Query("DELETE FROM generation_jobs")
    suspend fun clearAllJobs()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface SoraCloudDao {
    @Query("SELECT * FROM sora_cloud_servers")
    fun getAllServers(): Flow<List<SoraCloudServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: SoraCloudServerEntity)

    @Update
    suspend fun updateServer(server: SoraCloudServerEntity)

    @Delete
    suspend fun deleteServer(server: SoraCloudServerEntity)
}

@Dao
interface GalleryDao {
    @Query("SELECT * FROM gallery_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<GalleryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GalleryItemEntity)

    @Delete
    suspend fun deleteItem(item: GalleryItemEntity)
}

@Dao
interface QuantizationHistoryDao {
    @Query("SELECT * FROM quantization_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<QuantizationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: QuantizationHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: QuantizationHistoryEntity)

    @Query("DELETE FROM quantization_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM quantization_history")
    suspend fun clearAllHistory()
}

@Dao
interface StoryProjectDao {
    @Query("SELECT * FROM story_projects ORDER BY updatedAt DESC")
    fun getAllStoryProjects(): Flow<List<StoryProjectEntity>>

    @Query("SELECT * FROM story_projects WHERE id = :id LIMIT 1")
    suspend fun getStoryProjectById(id: String): StoryProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryProject(project: StoryProjectEntity)

    @Update
    suspend fun updateStoryProject(project: StoryProjectEntity)

    @Delete
    suspend fun deleteStoryProject(project: StoryProjectEntity)

    @Query("DELETE FROM story_projects WHERE id = :id")
    suspend fun deleteStoryProjectById(id: String)
}

@Dao
interface ManhwaProjectDao {
    @Query("SELECT * FROM manhwa_projects ORDER BY updatedAt DESC")
    fun getAllManhwaProjects(): Flow<List<ManhwaProjectEntity>>

    @Query("SELECT * FROM manhwa_projects WHERE id = :id LIMIT 1")
    suspend fun getManhwaProjectById(id: String): ManhwaProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManhwaProject(project: ManhwaProjectEntity)

    @Update
    suspend fun updateManhwaProject(project: ManhwaProjectEntity)

    @Delete
    suspend fun deleteManhwaProject(project: ManhwaProjectEntity)

    @Query("DELETE FROM manhwa_projects WHERE id = :id")
    suspend fun deleteManhwaProjectById(id: String)
}

@Dao
interface ScriptProjectDao {
    @Query("SELECT * FROM script_projects ORDER BY updatedAt DESC")
    fun getAllScriptProjects(): Flow<List<ScriptProjectEntity>>

    @Query("SELECT * FROM script_projects WHERE id = :id LIMIT 1")
    suspend fun getScriptProjectById(id: String): ScriptProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScriptProject(project: ScriptProjectEntity)

    @Update
    suspend fun updateScriptProject(project: ScriptProjectEntity)

    @Delete
    suspend fun deleteScriptProject(project: ScriptProjectEntity)

    @Query("DELETE FROM script_projects WHERE id = :id")
    suspend fun deleteScriptProjectById(id: String)
}

@Dao
interface GenerationLogDao {
    @Query("SELECT * FROM generation_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getLogsForProject(projectId: String): Flow<List<GenerationLogEntity>>

    @Query("SELECT * FROM generation_logs WHERE projectType = :projectType ORDER BY timestamp DESC")
    fun getLogsByType(projectType: String): Flow<List<GenerationLogEntity>>

    @Query("SELECT * FROM generation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<GenerationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GenerationLogEntity)

    @Query("DELETE FROM generation_logs WHERE projectId = :projectId")
    suspend fun deleteLogsForProject(projectId: String)

    @Query("DELETE FROM generation_logs")
    suspend fun clearAllLogs()
}

@Dao
interface LocalModelMetadataDao {
    @Query("SELECT * FROM local_model_metadata ORDER BY modelName ASC")
    fun getAllLocalModels(): Flow<List<LocalModelMetadataEntity>>

    @Query("SELECT * FROM local_model_metadata WHERE compatibilityStatus = :status ORDER BY modelName ASC")
    fun getModelsByCompatibilityStatus(status: String): Flow<List<LocalModelMetadataEntity>>

    @Query("SELECT * FROM local_model_metadata WHERE modelId = :modelId LIMIT 1")
    suspend fun getModelMetadataById(modelId: String): LocalModelMetadataEntity?

    @Query("SELECT * FROM local_model_metadata WHERE localPath = :path LIMIT 1")
    suspend fun getModelByPath(path: String): LocalModelMetadataEntity?

    @Query("SELECT * FROM local_model_metadata WHERE version = :version")
    fun getModelsByVersion(version: String): Flow<List<LocalModelMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelMetadata(metadata: LocalModelMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelMetadataList(metadataList: List<LocalModelMetadataEntity>)

    @Update
    suspend fun updateModelMetadata(metadata: LocalModelMetadataEntity)

    @Query("UPDATE local_model_metadata SET compatibilityStatus = :status, validationStatus = :validationStatus, lastVerifiedTimestamp = :timestamp WHERE modelId = :modelId")
    suspend fun updateCompatibilityStatus(modelId: String, status: String, validationStatus: String, timestamp: Long)

    @Query("UPDATE local_model_metadata SET localPath = :path, fileSizeBytes = :fileSizeBytes, downloadState = :downloadState, lastVerifiedTimestamp = :timestamp WHERE modelId = :modelId")
    suspend fun updateModelFilePathAndState(modelId: String, path: String, fileSizeBytes: Long, downloadState: String, timestamp: Long)

    @Delete
    suspend fun deleteModelMetadata(metadata: LocalModelMetadataEntity)

    @Query("DELETE FROM local_model_metadata WHERE modelId = :modelId")
    suspend fun deleteModelMetadataById(modelId: String)

    @Query("DELETE FROM local_model_metadata")
    suspend fun clearAll()
}

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY name ASC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE modelId = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)

    @Delete
    suspend fun deleteModel(model: ModelEntity)

    @Query("DELETE FROM models WHERE modelId = :modelId")
    suspend fun deleteModelById(modelId: String)
}

@Dao
interface ModelCapabilityDao {
    @Query("SELECT * FROM model_capabilities WHERE modelId = :modelId LIMIT 1")
    suspend fun getCapabilitiesForModel(modelId: String): ModelCapabilityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapabilities(capabilities: ModelCapabilityEntity)

    @Query("DELETE FROM model_capabilities WHERE modelId = :modelId")
    suspend fun deleteCapabilities(modelId: String)
}



