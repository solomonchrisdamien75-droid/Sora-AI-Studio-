package com.example.ai.hardware

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class StorageVolumeInfo(
    val id: String, // "INTERNAL" or "SD_CARD" or "CUSTOM"
    val label: String, // "Phone Internal Storage", "Removable SD Card"
    val mountPath: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val totalGbFormatted: String,
    val freeGbFormatted: String,
    val usedGbFormatted: String,
    val usedPercent: Int,
    val isRemovable: Boolean,
    val isMounted: Boolean,
    val modelsDirectory: File
)

class DeviceStorageManager(private val context: Context) {

    fun getInternalStorageVolume(): StorageVolumeInfo {
        val internalDataDir = Environment.getDataDirectory()
        val stat = StatFs(internalDataDir.path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 50

        val modelsDir = File(context.filesDir, "ai_models").apply { mkdirs() }

        return StorageVolumeInfo(
            id = "INTERNAL",
            label = "Phone Internal Storage",
            mountPath = internalDataDir.absolutePath,
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            totalGbFormatted = String.format("%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0)),
            freeGbFormatted = String.format("%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0)),
            usedGbFormatted = String.format("%.1f GB", usedBytes / (1024.0 * 1024.0 * 1024.0)),
            usedPercent = usedPct,
            isRemovable = false,
            isMounted = true,
            modelsDirectory = modelsDir
        )
    }

    fun getSdCardStorageVolume(): StorageVolumeInfo {
        val externalDirs = context.getExternalFilesDirs(null)
        val sdCardDir = if (externalDirs.size > 1 && externalDirs[1] != null) {
            externalDirs[1]!!
        } else {
            // Secondary fallback if primary external files directory exists
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }

        val stat = try {
            StatFs(sdCardDir.path)
        } catch (_: Exception) {
            StatFs(Environment.getDataDirectory().path)
        }

        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 35

        val modelsDir = File(sdCardDir, "ai_models").apply { mkdirs() }

        return StorageVolumeInfo(
            id = "SD_CARD",
            label = "SD Card Storage",
            mountPath = sdCardDir.absolutePath,
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            totalGbFormatted = String.format("%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0)),
            freeGbFormatted = String.format("%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0)),
            usedGbFormatted = String.format("%.1f GB", usedBytes / (1024.0 * 1024.0 * 1024.0)),
            usedPercent = usedPct,
            isRemovable = true,
            isMounted = true,
            modelsDirectory = modelsDir
        )
    }

    fun getAllStorageVolumes(): List<StorageVolumeInfo> {
        return listOf(getInternalStorageVolume(), getSdCardStorageVolume())
    }

    fun getTargetDirectoryForStorage(storageType: String, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val custom = File(customPath)
            custom.mkdirs()
            return custom
        }
        return if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageVolume().modelsDirectory
        } else {
            getInternalStorageVolume().modelsDirectory
        }
    }
}
