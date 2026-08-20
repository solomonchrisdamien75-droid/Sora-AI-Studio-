package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.data.local.entities.ModelEntity as LocalModelEntity
import com.example.data.local.entities.ModelCapabilityEntity as LocalModelCapabilityEntity
import com.example.data.local.entities.GenerationTaskEntity
import com.example.data.local.daos.ModelRegistryDao
import com.example.data.local.daos.ModelCapabilityRegistryDao
import com.example.data.local.daos.GenerationTaskDao

@Database(
    entities = [
        AiModelEntity::class,
        GenerationJobEntity::class,
        ProjectEntity::class,
        SoraCloudServerEntity::class,
        GalleryItemEntity::class,
        QuantizationHistoryEntity::class,
        StoryProjectEntity::class,
        ScriptProjectEntity::class,
        GenerationLogEntity::class,
        LocalModelMetadataEntity::class,
        ModelEntity::class,
        ModelCapabilityEntity::class,
        LocalModelEntity::class,
        LocalModelCapabilityEntity::class,
        GenerationTaskEntity::class,
        ManhwaProjectEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiModelDao(): AiModelDao
    abstract fun generationJobDao(): GenerationJobDao
    abstract fun projectDao(): ProjectDao
    abstract fun soraCloudDao(): SoraCloudDao
    abstract fun galleryDao(): GalleryDao
    abstract fun quantizationHistoryDao(): QuantizationHistoryDao
    abstract fun storyProjectDao(): StoryProjectDao
    abstract fun scriptProjectDao(): ScriptProjectDao
    abstract fun manhwaProjectDao(): ManhwaProjectDao
    abstract fun generationLogDao(): GenerationLogDao
    abstract fun localModelMetadataDao(): LocalModelMetadataDao
    abstract fun modelDao(): ModelDao
    abstract fun modelCapabilityDao(): ModelCapabilityDao
    abstract fun modelRegistryDao(): ModelRegistryDao
    abstract fun modelCapabilityRegistryDao(): ModelCapabilityRegistryDao
    abstract fun generationTaskDao(): GenerationTaskDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sora_ai_studio_db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
