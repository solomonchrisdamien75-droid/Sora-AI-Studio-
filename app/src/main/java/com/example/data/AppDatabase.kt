package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AiModelEntity::class,
        GenerationJobEntity::class,
        ProjectEntity::class,
        SoraCloudServerEntity::class,
        GalleryItemEntity::class,
        QuantizationHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiModelDao(): AiModelDao
    abstract fun generationJobDao(): GenerationJobDao
    abstract fun projectDao(): ProjectDao
    abstract fun soraCloudDao(): SoraCloudDao
    abstract fun galleryDao(): GalleryDao
    abstract fun quantizationHistoryDao(): QuantizationHistoryDao


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
