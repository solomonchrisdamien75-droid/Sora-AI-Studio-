package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.LocalModelMetadataDao
import com.example.data.LocalModelMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalModelMetadataDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: LocalModelMetadataDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.localModelMetadataDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadModelMetadata() = runBlocking {
        val model = LocalModelMetadataEntity(
            modelId = "model_test_1",
            modelName = "Sora Test Model",
            version = "v1.2.0",
            architecture = "Transformer-Vulkan",
            quantization = "Q4_K_M",
            localPath = "/data/user/0/com.example/files/models/sora_test.gguf",
            storageLocation = "INTERNAL",
            fileSizeBytes = 1_500_000_000L,
            isDownloaded = true,
            downloadState = "AVAILABLE",
            compatibilityStatus = "COMPATIBLE",
            validationStatus = "VALID",
            ramRequiredMb = 2048,
            checksum = "sha256_abcdef123456"
        )

        dao.insertModelMetadata(model)

        val retrieved = dao.getModelMetadataById("model_test_1")
        assertNotNull(retrieved)
        assertEquals("Sora Test Model", retrieved?.modelName)
        assertEquals("v1.2.0", retrieved?.version)
        assertEquals("COMPATIBLE", retrieved?.compatibilityStatus)
        assertEquals("/data/user/0/com.example/files/models/sora_test.gguf", retrieved?.localPath)

        val allModels = dao.getAllLocalModels().first()
        assertEquals(1, allModels.size)
    }

    @Test
    fun updateCompatibilityAndFilePath() = runBlocking {
        val model = LocalModelMetadataEntity(
            modelId = "model_test_2",
            modelName = "Wan 2.1 Test",
            version = "v1.0",
            architecture = "Diffusion",
            quantization = "FP16",
            localPath = "/old/path/model.bin",
            fileSizeBytes = 500_000L,
            ramRequiredMb = 4000,
            compatibilityStatus = "UNVERIFIED"
        )
        dao.insertModelMetadata(model)

        dao.updateCompatibilityStatus("model_test_2", "INCOMPATIBLE_RAM", "INVALID", 123456L)
        dao.updateModelFilePathAndState("model_test_2", "/new/path/model.bin", 2_000_000L, "AVAILABLE", 789012L)

        val updated = dao.getModelMetadataById("model_test_2")
        assertNotNull(updated)
        assertEquals("INCOMPATIBLE_RAM", updated?.compatibilityStatus)
        assertEquals("INVALID", updated?.validationStatus)
        assertEquals("/new/path/model.bin", updated?.localPath)
        assertEquals(2_000_000L, updated?.fileSizeBytes)
        assertEquals("AVAILABLE", updated?.downloadState)
    }
}
