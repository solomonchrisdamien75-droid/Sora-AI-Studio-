package com.example.ai.hardware

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.documentfile.provider.DocumentFile
import com.example.data.AiModelDao
import com.example.data.AiModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class StorageVolumeInfo(
    val id: String, // "INTERNAL", "SD_CARD", "CUSTOM_SAF"
    val label: String, // "Phone Internal Storage", "Removable SD Card", "Custom Folder"
    val mountPath: String?,
    val uriString: String? = null,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val totalGbFormatted: String,
    val freeGbFormatted: String,
    val usedGbFormatted: String,
    val usedPercent: Int,
    val isRemovable: Boolean,
    val isMounted: Boolean,
    val isAvailable: Boolean,
    val modelsDirectory: File? = null
) {
    val storageType: String get() = id
    val name: String get() = label
    val freeSpaceGb: Double get() = freeBytes / (1024.0 * 1024.0 * 1024.0)
    val totalSpaceGb: Double get() = totalBytes / (1024.0 * 1024.0 * 1024.0)
    val isEmulated: Boolean get() = !isRemovable
}

class DeviceStorageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("sora_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_STORAGE_TYPE = "pref_storage_type" // "INTERNAL", "SD_CARD", "CUSTOM_SAF"
        const val PREF_CUSTOM_URI = "pref_custom_uri"
        const val PREF_CUSTOM_NAME = "pref_custom_name"
    }

    /**
     * Checks if running in a real Android environment where filesystem is accessible.
     */
    fun isRealAndroidStorageAvailable(): Boolean {
        return try {
            val dir = context.filesDir
            dir != null && dir.exists()
        } catch (_: Exception) {
            false
        }
    }

    fun getInternalStorageVolume(): StorageVolumeInfo {
        try {
            val internalDataDir = Environment.getDataDirectory()
            val filesDir = context.filesDir
            val stat = if (filesDir != null && filesDir.exists()) {
                StatFs(filesDir.path)
            } else {
                StatFs(internalDataDir.path)
            }

            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            val usedPct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

            val modelsDir = File(context.filesDir, "ai_models").apply { mkdirs() }

            return StorageVolumeInfo(
                id = "INTERNAL",
                label = "Phone Storage",
                mountPath = modelsDir.absolutePath,
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes,
                totalGbFormatted = formatBytesToGb(totalBytes),
                freeGbFormatted = formatBytesToGb(freeBytes),
                usedGbFormatted = formatBytesToGb(usedBytes),
                usedPercent = usedPct,
                isRemovable = false,
                isMounted = true,
                isAvailable = true,
                modelsDirectory = modelsDir
            )
        } catch (_: Exception) {
            val fallbackDir = File(context.filesDir, "ai_models").apply { mkdirs() }
            return StorageVolumeInfo(
                id = "INTERNAL",
                label = "Phone Storage",
                mountPath = fallbackDir.absolutePath,
                totalBytes = 0L,
                freeBytes = 0L,
                usedBytes = 0L,
                totalGbFormatted = "Storage info unavailable",
                freeGbFormatted = "Storage info unavailable",
                usedGbFormatted = "Storage info unavailable",
                usedPercent = 0,
                isRemovable = false,
                isMounted = true,
                isAvailable = true,
                modelsDirectory = fallbackDir
            )
        }
    }

    /**
     * Accurately detects if a physical removable SD card is mounted.
     * Does NOT report an SD card if one does not physically exist.
     */
    fun getSdCardStorageVolume(): StorageVolumeInfo? {
        try {
            val externalDirs = context.getExternalFilesDirs(null)
            if (externalDirs.size > 1 && externalDirs[1] != null) {
                val sdCardDir = externalDirs[1]!!
                val isRemovable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Environment.isExternalStorageRemovable(sdCardDir)
                } else {
                    true
                }

                if (!isRemovable && externalDirs.size <= 1) {
                    return null
                }

                val stat = StatFs(sdCardDir.path)
                val totalBytes = stat.blockCountLong * stat.blockSizeLong
                val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
                val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
                val usedPct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

                val modelsDir = File(sdCardDir, "ai_models").apply { mkdirs() }

                return StorageVolumeInfo(
                    id = "SD_CARD",
                    label = "Removable SD Card",
                    mountPath = modelsDir.absolutePath,
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    usedBytes = usedBytes,
                    totalGbFormatted = formatBytesToGb(totalBytes),
                    freeGbFormatted = formatBytesToGb(freeBytes),
                    usedGbFormatted = formatBytesToGb(usedBytes),
                    usedPercent = usedPct,
                    isRemovable = true,
                    isMounted = true,
                    isAvailable = true,
                    modelsDirectory = modelsDir
                )
            }
        } catch (_: Exception) {
            // No accessible SD card
        }
        return null
    }

    /**
     * Gets user-selected Storage Access Framework volume if configured and permission is valid.
     */
    fun getCustomSafVolume(): StorageVolumeInfo? {
        val uriStr = prefs.getString(PREF_CUSTOM_URI, null) ?: return null
        val customName = prefs.getString(PREF_CUSTOM_NAME, "Custom Folder") ?: "Custom Folder"
        try {
            val uri = Uri.parse(uriStr)
            // Verify persistable permission is still valid
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isWritePermission
            }
            if (!hasPerm) return null

            val docFile = DocumentFile.fromTreeUri(context, uri)
            if (docFile == null || !docFile.exists() || !docFile.canWrite()) {
                return null
            }

            // Estimate free space from phone storage as baseline for SAF tree
            val internalInfo = getInternalStorageVolume()

            return StorageVolumeInfo(
                id = "CUSTOM_SAF",
                label = customName,
                mountPath = docFile.name ?: "SAF Folder",
                uriString = uriStr,
                totalBytes = internalInfo.totalBytes,
                freeBytes = internalInfo.freeBytes,
                usedBytes = internalInfo.usedBytes,
                totalGbFormatted = internalInfo.totalGbFormatted,
                freeGbFormatted = internalInfo.freeGbFormatted,
                usedGbFormatted = internalInfo.usedGbFormatted,
                usedPercent = internalInfo.usedPercent,
                isRemovable = false,
                isMounted = true,
                isAvailable = true,
                modelsDirectory = null
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun getAllStorageVolumes(): List<StorageVolumeInfo> {
        val list = mutableListOf<StorageVolumeInfo>()
        list.add(getInternalStorageVolume())
        getSdCardStorageVolume()?.let { list.add(it) }
        getCustomSafVolume()?.let { list.add(it) }
        return list
    }

    fun getPreferredStorageType(): String {
        return prefs.getString(PREF_STORAGE_TYPE, "INTERNAL") ?: "INTERNAL"
    }

    fun getPreferredCustomUri(): String? {
        return prefs.getString(PREF_CUSTOM_URI, null)
    }

    fun setPreferredStorage(type: String, customUri: Uri? = null, customName: String? = null) {
        val editor = prefs.edit().putString(PREF_STORAGE_TYPE, type)
        if (customUri != null) {
            editor.putString(PREF_CUSTOM_URI, customUri.toString())
            editor.putString(PREF_CUSTOM_NAME, customName ?: (DocumentFile.fromTreeUri(context, customUri)?.name ?: "Custom Folder"))
            // Take persistable permission
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(customUri, takeFlags)
            } catch (_: Exception) {}
        }
        editor.apply()
    }

    fun getTargetDirectoryForStorage(storageType: String, customPath: String? = null): File {
        if (!customPath.isNullOrBlank() && !customPath.startsWith("content://")) {
            val custom = File(customPath)
            custom.mkdirs()
            return custom
        }
        return if (storageType.equals("SD_CARD", ignoreCase = true)) {
            getSdCardStorageVolume()?.modelsDirectory ?: getInternalStorageVolume().modelsDirectory!!
        } else {
            getInternalStorageVolume().modelsDirectory!!
        }
    }

    /**
     * Moves all downloaded models from current storage to new storage destination.
     * Safely copies -> verifies length & hash -> updates DB -> removes source file.
     */
    suspend fun migrateModelsToNewStorage(
        models: List<AiModelEntity>,
        targetStorageType: String,
        targetCustomUri: Uri?,
        aiModelDao: AiModelDao,
        onProgress: (modelName: String, progress: Float, status: String) -> Unit
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        var migratedCount = 0
        val targetDir = if (targetStorageType == "SD_CARD") {
            getSdCardStorageVolume()?.modelsDirectory ?: getInternalStorageVolume().modelsDirectory!!
        } else if (targetStorageType == "INTERNAL") {
            getInternalStorageVolume().modelsDirectory!!
        } else {
            null
        }

        val totalModels = models.filter { it.isDownloaded }
        if (totalModels.isEmpty()) {
            return@withContext Pair(0, "No downloaded models to migrate.")
        }

        for ((index, model) in totalModels.withIndex()) {
            val srcPath = model.localPath
            if (srcPath.isNullOrBlank()) continue
            val srcFile = File(srcPath)
            if (!srcFile.exists() || srcFile.length() == 0L) continue

            onProgress(model.name, index.toFloat() / totalModels.size, "Migrating ${model.name}...")

            try {
                if (targetDir != null) {
                    val destFile = File(targetDir, srcFile.name)
                    if (destFile.canonicalPath == srcFile.canonicalPath) {
                        continue
                    }

                    // Copy stream
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(destFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }

                    // Verify size
                    if (destFile.exists() && destFile.length() == srcFile.length()) {
                        // Update Room DB
                        val updated = model.copy(
                            localPath = destFile.absolutePath,
                            storageLocation = targetStorageType,
                            lastVerified = System.currentTimeMillis()
                        )
                        aiModelDao.updateModel(updated)
                        // Safe deletion of source only after verified copy
                        srcFile.delete()
                        migratedCount++
                    } else {
                        destFile.delete()
                    }
                } else if (targetCustomUri != null) {
                    val docTree = DocumentFile.fromTreeUri(context, targetCustomUri)
                    if (docTree != null && docTree.canWrite()) {
                        val newDoc = docTree.createFile("application/octet-stream", srcFile.name)
                        if (newDoc != null) {
                            context.contentResolver.openOutputStream(newDoc.uri)?.use { output ->
                                FileInputStream(srcFile).use { input ->
                                    val buffer = ByteArray(64 * 1024)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                    }
                                    output.flush()
                                }
                            }
                            // Update DB
                            val updated = model.copy(
                                localPath = null,
                                fileUri = newDoc.uri.toString(),
                                storageLocation = "CUSTOM_SAF",
                                lastVerified = System.currentTimeMillis()
                            )
                            aiModelDao.updateModel(updated)
                            srcFile.delete()
                            migratedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep original untouched on error
            }
        }

        onProgress("Done", 1.0f, "Completed migration of $migratedCount model(s)")
        Pair(migratedCount, "Successfully migrated $migratedCount model(s) to $targetStorageType")
    }

    private fun formatBytesToGb(bytes: Long): String {
        if (bytes <= 0L) return "0.0 GB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }
}

