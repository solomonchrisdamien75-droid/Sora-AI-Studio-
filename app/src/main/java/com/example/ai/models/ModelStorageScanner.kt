package com.example.ai.models

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.data.AiModelDao
import com.example.data.AiModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class StorageScanProgress(
    val isScanning: Boolean = false,
    val currentPath: String = "",
    val stageDescription: String = "Idle",
    val progressPercent: Int = 0,
    val discoveredCount: Int = 0,
    val validModelsCount: Int = 0,
    val invalidCount: Int = 0,
    val unsupportedCount: Int = 0,
    val missingCleanedCount: Int = 0,
    val scanSummaryMessage: String = ""
)

/**
 * Scans on-device storage locations (Internal, SD Card, Custom folders)
 * and reconciles physical files with the database.
 */
class ModelStorageScanner(
    private val context: Context,
    private val validator: ModelValidationEngine,
    private val aiModelDao: AiModelDao
) {
    private val _scanProgress = MutableStateFlow(StorageScanProgress())
    val scanProgress: StateFlow<StorageScanProgress> = _scanProgress.asStateFlow()

    /**
     * Reconciles registered database records against physical storage.
     * If a record claims to be downloaded, verifies that the file actually exists and is readable.
     * If the file is missing or corrupted, marks it accordingly.
     * Also discovers any physical files in model directories and registers them.
     */
    suspend fun reconcileDatabaseWithStorage(): StorageScanProgress = withContext(Dispatchers.IO) {
        _scanProgress.value = StorageScanProgress(
            isScanning = true,
            stageDescription = "Checking registered model records...",
            progressPercent = 10
        )

        var missingCount = 0
        var validCount = 0
        var invalidCount = 0
        var unsupportedCount = 0
        var totalDiscovered = 0

        try {
            // 1. Reconcile existing records in database
            val allDbModels = aiModelDao.getAllModelsList()
            for (dbModel in allDbModels) {
                if (dbModel.isDownloaded) {
                    var fileExists = false
                    if (!dbModel.localPath.isNullOrBlank()) {
                        val f = File(dbModel.localPath)
                        fileExists = f.exists() && f.length() > 0
                    } else if (!dbModel.fileUri.isNullOrBlank()) {
                        try {
                            val uri = Uri.parse(dbModel.fileUri)
                            val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                            fileExists = doc != null && doc.exists() && doc.length() > 0
                        } catch (_: Exception) {
                            fileExists = false
                        }
                    }

                    if (!fileExists) {
                        missingCount++
                        val resetModel = dbModel.copy(
                            isDownloaded = false,
                            downloadState = ModelDownloadState.NOT_DOWNLOADED.name,
                            localPath = null,
                            fileUri = null,
                            validationStatus = "MISSING_FROM_STORAGE"
                        )
                        aiModelDao.updateModel(resetModel)
                    }
                }
            }

            val targetDirs = getStandardModelDirectories()

            _scanProgress.value = _scanProgress.value.copy(
                stageDescription = "Scanning storage directories...",
                progressPercent = 30
            )

            val discoveredFiles = mutableListOf<File>()
            targetDirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    _scanProgress.value = _scanProgress.value.copy(currentPath = dir.absolutePath)
                    scanDirectoryRecursively(dir, discoveredFiles)
                }
            }

            totalDiscovered = discoveredFiles.size
            _scanProgress.value = _scanProgress.value.copy(
                discoveredCount = totalDiscovered,
                stageDescription = "Validating discovered model binaries ($totalDiscovered files)...",
                progressPercent = 60
            )

            // Validate each discovered file and register in DB if valid
            discoveredFiles.forEachIndexed { index, file ->
                val validation = validator.validateFile(file)
                val progress = 60 + ((index + 1) * 30 / totalDiscovered.coerceAtLeast(1))
                _scanProgress.value = _scanProgress.value.copy(
                    currentPath = file.name,
                    progressPercent = progress
                )

                if (validation.isValid) {
                    validCount++
                    val modelId = "local-${file.nameWithoutExtension.lowercase().replace("[^a-z0-9_-]".toRegex(), "-")}"
                    val existing = aiModelDao.getModelById(modelId)
                    val modelType = if (file.name.contains("video", true) || file.name.contains("wan", true) || file.name.contains("ltx", true) || file.name.contains("sora", true)) "VIDEO" else "IMAGE"

                    val entity = AiModelEntity(
                        id = existing?.id ?: modelId,
                        name = existing?.name ?: file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').capitalizeWords(),
                        description = existing?.description ?: "Verified physical ${validation.detectedFormat} model on device storage (${validation.architecture})",
                        format = validation.detectedFormat,
                        modelType = existing?.modelType ?: modelType,
                        sizeBytes = validation.actualSizeBytes,
                        ramRequiredMb = validation.estimatedRamMb,
                        isDownloaded = true,
                        downloadState = ModelDownloadState.AVAILABLE.name,
                        storageLocation = if (file.absolutePath.contains("emulated") || file.absolutePath.contains("/data/")) "INTERNAL" else "SD_CARD",
                        localPath = file.absolutePath,
                        fileUri = null,
                        checksum = validation.checksumSha256 ?: validator.computeChecksumSha256(file),
                        lastVerified = System.currentTimeMillis(),
                        validationStatus = ModelValidationStatus.VALID.name,
                        architecture = validation.architecture,
                        backend = validation.backend,
                        quantization = extractQuantizationFromName(file.name)
                    )
                    aiModelDao.insertModel(entity)
                } else if (validation.status == ModelValidationStatus.UNSUPPORTED_EXTENSION) {
                    unsupportedCount++
                } else {
                    invalidCount++
                }
            }

            val summary = if (validCount > 0) {
                "Storage scan complete: $validCount valid model(s) verified on device ($totalDiscovered files examined)."
            } else {
                "Storage scan complete: 0 models installed. Download models from Hugging Face or import from storage."
            }

            val result = StorageScanProgress(
                isScanning = false,
                currentPath = "",
                stageDescription = "Scan finished",
                progressPercent = 100,
                discoveredCount = totalDiscovered,
                validModelsCount = validCount,
                invalidCount = invalidCount,
                unsupportedCount = unsupportedCount,
                missingCleanedCount = missingCount,
                scanSummaryMessage = summary
            )
            _scanProgress.value = result
            result
        } catch (e: Exception) {
            val errorResult = StorageScanProgress(
                isScanning = false,
                stageDescription = "Scan error: ${e.localizedMessage}",
                progressPercent = 100,
                scanSummaryMessage = "Scan error: ${e.localizedMessage}"
            )
            _scanProgress.value = errorResult
            errorResult
        }
    }

    /**
     * Validates and imports a single user-specified file from SAF / ContentResolver / File path.
     */
    suspend fun importAndValidateModel(
        name: String,
        format: String,
        modelType: String,
        ramMb: Int,
        pathOrUri: String,
        storageSource: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val validation = if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                validator.validateUri(uri, name)
            } else {
                val file = File(pathOrUri)
                validator.validateFile(file)
            }

            if (!validation.isValid) {
                return@withContext Pair(false, "Import failed: ${validation.reason} (${validation.status})")
            }

            val cleanId = "custom-${System.currentTimeMillis()}-${name.lowercase().replace("[^a-z0-9]".toRegex(), "-")}"
            val entity = AiModelEntity(
                id = cleanId,
                name = name,
                description = "User-imported verified ${validation.detectedFormat} model ($storageSource)",
                format = if (validation.detectedFormat != "UNKNOWN") validation.detectedFormat else format.uppercase(),
                modelType = modelType.uppercase(),
                sizeBytes = validation.actualSizeBytes,
                ramRequiredMb = if (validation.estimatedRamMb > 0) validation.estimatedRamMb else ramMb,
                isDownloaded = true,
                downloadState = ModelDownloadState.AVAILABLE.name,
                storageLocation = storageSource,
                localPath = if (!pathOrUri.startsWith("content://")) pathOrUri else null,
                fileUri = if (pathOrUri.startsWith("content://")) pathOrUri else null,
                checksum = validation.checksumSha256,
                lastVerified = System.currentTimeMillis(),
                validationStatus = ModelValidationStatus.VALID.name,
                architecture = validation.architecture,
                backend = validation.backend,
                quantization = extractQuantizationFromName(name)
            )

            aiModelDao.insertModel(entity)
            Pair(true, "Successfully verified & imported '${name}' (${validation.actualSizeBytes / (1024 * 1024)}MB, ${validation.detectedFormat})")
        } catch (e: Exception) {
            Pair(false, "Import error: ${e.localizedMessage}")
        }
    }

    private fun getStandardModelDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // Internal App Files
        val internalAiModels = File(context.filesDir, "ai_models")
        dirs.add(internalAiModels)
        val internalModels = File(context.filesDir, "models")
        dirs.add(internalModels)

        // App External Files
        context.getExternalFilesDir("ai_models")?.let { dirs.add(it) }
        context.getExternalFilesDir(null)?.let { dirs.add(it) }

        // External Storage AI folders
        try {
            val extDirs = ContextCompat.getExternalFilesDirs(context, null)
            extDirs.forEach { extDir ->
                extDir?.let {
                    dirs.add(File(it, "ai_models"))
                }
            }

            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads != null && publicDownloads.exists()) {
                val soraDir = File(publicDownloads, "SoraAiStudio")
                if (soraDir.exists()) dirs.add(soraDir)
            }
        } catch (_: Exception) {}

        return dirs
    }

    private fun scanDirectoryRecursively(dir: File, result: MutableList<File>, maxDepth: Int = 3, currentDepth: Int = 0) {
        if (currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory && !f.name.startsWith(".")) {
                scanDirectoryRecursively(f, result, maxDepth, currentDepth + 1)
            } else if (f.isFile && !f.name.startsWith(".") && !f.name.startsWith(".tmp_")) {
                val ext = f.extension.lowercase()
                if (ext in listOf("gguf", "onnx", "tflite", "litert", "safetensors", "pt", "pth", "bin", "mnn", "ncnn")) {
                    result.add(f)
                }
            }
        }
    }

    private fun extractQuantizationFromName(name: String): String {
        val upper = name.uppercase()
        return when {
            upper.contains("Q4_K_M") -> "Q4_K_M"
            upper.contains("Q4_0") -> "Q4_0"
            upper.contains("Q4") -> "Q4"
            upper.contains("Q8_0") || upper.contains("Q8") -> "Q8_0"
            upper.contains("Q5_K_M") || upper.contains("Q5") -> "Q5_K_M"
            upper.contains("Q3_K_M") || upper.contains("Q3") -> "Q3_K_M"
            upper.contains("Q2_K") || upper.contains("Q2") -> "Q2_K"
            upper.contains("INT8") -> "INT8"
            upper.contains("INT4") -> "INT4"
            upper.contains("FP16") -> "FP16"
            else -> "Standard"
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
