package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelDao {
    @Query("SELECT * FROM ai_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<AiModelEntity>>

    @Query("SELECT * FROM ai_models WHERE isDownloaded = 1")
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
