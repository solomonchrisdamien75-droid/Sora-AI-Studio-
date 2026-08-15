package com.example.ai.queue

import com.example.ai.inference.InferenceEngineManager
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class BatchJobCreationRequest(
    val titlePrefix: String,
    val prompts: List<String>,
    val generationType: String = "TEXT_TO_VIDEO",
    val mode: String = "FAST",
    val durationSeconds: Int = 5,
    val resolution: String = "1080p",
    val fps: Int = 24
)

data class QueueSummary(
    val totalCount: Int = 0,
    val queuedCount: Int = 0,
    val runningCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val totalEstimatedSeconds: Int = 0
)

class TaskQueueManager(
    private val generationJobDao: GenerationJobDao,
    private val galleryDao: GalleryDao,
    private val inferenceEngineManager: InferenceEngineManager,
    private val scope: CoroutineScope,
    private val onJobFinishedCallback: (GalleryItemEntity) -> Unit = {},
    private val realMediaSynthesisEngine: com.example.ai.generator.RealMediaSynthesisEngine? = null
) {
    private val _isQueueProcessing = MutableStateFlow(false)
    val isQueueProcessing: StateFlow<Boolean> = _isQueueProcessing.asStateFlow()

    private val _isAutoProcessEnabled = MutableStateFlow(true)
    val isAutoProcessEnabled: StateFlow<Boolean> = _isAutoProcessEnabled.asStateFlow()

    private val _currentRunningJobId = MutableStateFlow<String?>(null)
    val currentRunningJobId: StateFlow<String?> = _currentRunningJobId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var activeJobJob: Job? = null
    private var workerLoopJob: Job? = null

    init {
        // Automatically listen to queue changes and start processing if auto-process is true
        scope.launch {
            generationJobDao.getQueuedJobs().collect { queuedList ->
                if (_isAutoProcessEnabled.value && queuedList.isNotEmpty() && !_isQueueProcessing.value && activeJobJob?.isActive != true) {
                    startProcessing()
                }
            }
        }
    }

    fun setAutoProcess(enabled: Boolean) {
        _isAutoProcessEnabled.value = enabled
        if (enabled && !_isQueueProcessing.value) {
            startProcessing()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    suspend fun enqueueSingleJob(
        title: String,
        prompt: String,
        generationType: String,
        mode: String,
        durationSec: Int,
        resolution: String,
        fps: Int
    ): GenerationJobEntity = withContext(Dispatchers.IO) {
        val jobId = "job_queue_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val totalFrames = fps * durationSec
        val job = GenerationJobEntity(
            id = jobId,
            title = title.ifBlank { "Queued Render #${System.currentTimeMillis() % 10000}" },
            prompt = prompt,
            generationType = generationType,
            mode = mode,
            progressPercent = 0,
            currentFrame = 0,
            totalFrames = totalFrames,
            fps = fps.toFloat(),
            status = "QUEUED",
            createdAt = System.currentTimeMillis(),
            durationSeconds = durationSec,
            resolution = resolution,
            backendUsed = when (mode) {
                "FAST" -> "LiteRT / Vulkan"
                "BALANCED" -> "ONNX DirectML / CPU"
                else -> "Cinema Mode / Vulkan"
            }
        )
        generationJobDao.insertJob(job)
        _statusMessage.value = "Task \"${job.title}\" added to offline queue"

        if (_isAutoProcessEnabled.value && !_isQueueProcessing.value) {
            startProcessing()
        }
        return@withContext job
    }

    suspend fun enqueueBatch(request: BatchJobCreationRequest): List<GenerationJobEntity> = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<GenerationJobEntity>()
        var timestamp = System.currentTimeMillis()

        request.prompts.filter { it.isNotBlank() }.forEachIndexed { index, promptText ->
            timestamp += 10 // ensure distinct creation timestamp order
            val title = if (request.prompts.size > 1) {
                "${request.titlePrefix.ifBlank { "Batch Video" }} (Scene ${index + 1}/${request.prompts.size})"
            } else {
                request.titlePrefix.ifBlank { "Batch Video" }
            }

            val totalFrames = request.fps * request.durationSeconds
            val job = GenerationJobEntity(
                id = "job_batch_${timestamp}_${UUID.randomUUID().toString().take(4)}",
                title = title,
                prompt = promptText.trim(),
                generationType = request.generationType,
                mode = request.mode,
                progressPercent = 0,
                currentFrame = 0,
                totalFrames = totalFrames,
                fps = request.fps.toFloat(),
                status = "QUEUED",
                createdAt = timestamp,
                durationSeconds = request.durationSeconds,
                resolution = request.resolution,
                backendUsed = when (request.mode) {
                    "FAST" -> "LiteRT / Vulkan"
                    "BALANCED" -> "ONNX DirectML / CPU"
                    else -> "Cinema Mode / Vulkan"
                }
            )
            jobs.add(job)
        }

        if (jobs.isNotEmpty()) {
            generationJobDao.insertJobs(jobs)
            _statusMessage.value = "Queued ${jobs.size} batch video tasks successfully"

            if (_isAutoProcessEnabled.value && !_isQueueProcessing.value) {
                startProcessing()
            }
        }
        return@withContext jobs
    }

    fun startProcessing() {
        if (_isQueueProcessing.value && workerLoopJob?.isActive == true) return
        _isQueueProcessing.value = true

        workerLoopJob?.cancel()
        workerLoopJob = scope.launch(Dispatchers.IO) {
            runSequentialQueueWorker()
        }
    }

    fun pauseProcessing() {
        _isQueueProcessing.value = false
        activeJobJob?.cancel()
        activeJobJob = null
        _currentRunningJobId.value = null
        workerLoopJob?.cancel()
        workerLoopJob = null

        scope.launch(Dispatchers.IO) {
            // If there was a running job, set it back to QUEUED so it can resume cleanly
            val running = generationJobDao.getRunningJob().firstOrNull()
            if (running != null) {
                generationJobDao.updateJob(running.copy(status = "QUEUED"))
            }
        }
        _statusMessage.value = "Task Queue Paused"
    }

    private suspend fun runSequentialQueueWorker() {
        while (_isQueueProcessing.value) {
            val nextJob = generationJobDao.getNextQueuedJob()
            if (nextJob == null) {
                _currentRunningJobId.value = null
                _isQueueProcessing.value = false
                _statusMessage.value = "All queued AI video tasks completed!"
                break
            }

            _currentRunningJobId.value = nextJob.id
            processSingleJob(nextJob)
            delay(500) // Brief breathing room between batch items
        }
    }

    private suspend fun processSingleJob(job: GenerationJobEntity) {
        val updatedRunningJob = job.copy(status = "RUNNING")
        generationJobDao.updateJob(updatedRunningJob)

        val activeModel = inferenceEngineManager.activeLoadedModel.value ?: AiModelEntity(
            id = "active_model",
            name = "Sora Offline Engine",
            modelType = "VIDEO",
            format = if (job.mode == "BALANCED") "ONNX" else "LITERET",
            sizeBytes = 1_000_000_000L,
            ramRequiredMb = 2000,
            isDownloaded = true
        )
        val engine = inferenceEngineManager.selectEngineForModel(activeModel)

        try {
            coroutineScope {
                activeJobJob = launch {
                    engine.generateVideoFrames(
                        prompt = job.prompt,
                        width = if (job.resolution.contains("4K")) 3840 else if (job.resolution.contains("720p")) 1280 else 1920,
                        height = if (job.resolution.contains("4K")) 2160 else if (job.resolution.contains("720p")) 720 else 1080,
                        fps = job.fps.toInt(),
                        durationSec = job.durationSeconds,
                        onFrameRendered = { _, _, _ -> }
                    ).collect { progress ->
                        val pct = if (progress.totalFrames > 0) {
                            ((progress.currentFrame.toFloat() / progress.totalFrames) * 100).toInt()
                        } else 0

                        val updated = job.copy(
                            currentFrame = progress.currentFrame,
                            totalFrames = progress.totalFrames,
                            progressPercent = pct,
                            fps = progress.fps,
                            status = if (progress.isComplete) "COMPLETED" else "RUNNING"
                        )
                        generationJobDao.updateJob(updated)

                        if (progress.isComplete) {
                            val galleryItem = if (realMediaSynthesisEngine != null) {
                                val isImg = job.generationType in listOf("IMAGE_GEN", "IMAGE_EDIT", "UPSCALING", "INPAINTING", "OUTPAINTING", "BG_REMOVAL")
                                val isAud = job.generationType in listOf("VOICE_CLONE", "VOICE_GEN", "SUBTITLES", "TRANSLATION", "LIP_SYNC")
                                val isStory = job.generationType in listOf("STORY_GEN", "SCRIPT_WRITER", "SCENE_BUILDER", "SHOT_PLANNER", "CHARACTER_CREATOR")

                                when {
                                    isImg -> {
                                        val res = realMediaSynthesisEngine.generateRealImage(
                                            title = job.title,
                                            prompt = job.prompt,
                                            style = "PHOTOREALISTIC",
                                            aspectRatio = "1:1",
                                            resolutionLabel = job.resolution
                                        )
                                        res.second
                                    }
                                    isAud -> {
                                        val res = realMediaSynthesisEngine.generateRealAudio(
                                            title = job.title,
                                            scriptText = job.prompt,
                                            voiceArchetype = "AI_ASSISTANT",
                                            emotion = "NEUTRAL",
                                            durationSec = job.durationSeconds
                                        )
                                        res.second
                                    }
                                    isStory -> {
                                        val res = realMediaSynthesisEngine.generateRealScript(
                                            title = job.title,
                                            prompt = job.prompt,
                                            format = "SCREENPLAY",
                                            tone = "CINEMATIC"
                                        )
                                        res.second
                                    }
                                    else -> {
                                        val res = realMediaSynthesisEngine.generateRealVideo(
                                            title = job.title,
                                            prompt = job.prompt,
                                            durationSec = job.durationSeconds,
                                            resolutionLabel = job.resolution,
                                            fps = job.fps.toInt()
                                        )
                                        res.second
                                    }
                                }
                            } else {
                                GalleryItemEntity(
                                    id = "gal_${System.currentTimeMillis()}",
                                    title = job.title,
                                    mediaType = if (job.generationType.contains("IMAGE")) "IMAGE" else "VIDEO",
                                    filePath = "renders/${job.id}.mp4",
                                    durationMs = (job.durationSeconds * 1000).toLong(),
                                    prompt = job.prompt,
                                    resolutionLabel = job.resolution
                                )
                            }
                            galleryDao.insertItem(galleryItem)
                            onJobFinishedCallback(galleryItem)
                        }
                    }
                }
                activeJobJob?.join()
            }
        } catch (e: CancellationException) {
            generationJobDao.updateJobStatus(job.id, "PAUSED")
        } catch (e: Exception) {
            generationJobDao.updateJob(job.copy(status = "FAILED", errorMessage = e.localizedMessage ?: "Inference error"))
        } finally {
            _currentRunningJobId.value = null
        }
    }

    fun cancelJob(jobId: String) {
        scope.launch(Dispatchers.IO) {
            if (_currentRunningJobId.value == jobId) {
                activeJobJob?.cancel()
                activeJobJob = null
                _currentRunningJobId.value = null
            }
            generationJobDao.updateJobStatus(jobId, "CANCELLED")
            _statusMessage.value = "Job cancelled"

            // Continue to next job if queue is active
            if (_isQueueProcessing.value && workerLoopJob?.isActive != true) {
                startProcessing()
            }
        }
    }

    fun retryJob(jobId: String) {
        scope.launch(Dispatchers.IO) {
            val job = generationJobDao.getJobById(jobId)
            if (job != null) {
                val reset = job.copy(
                    status = "QUEUED",
                    progressPercent = 0,
                    currentFrame = 0,
                    errorMessage = null,
                    createdAt = System.currentTimeMillis()
                )
                generationJobDao.updateJob(reset)
                _statusMessage.value = "Task queued for retry"

                if (_isAutoProcessEnabled.value && !_isQueueProcessing.value) {
                    startProcessing()
                }
            }
        }
    }

    fun deleteJob(jobId: String) {
        scope.launch(Dispatchers.IO) {
            if (_currentRunningJobId.value == jobId) {
                activeJobJob?.cancel()
                activeJobJob = null
                _currentRunningJobId.value = null
            }
            generationJobDao.deleteJobById(jobId)
            _statusMessage.value = "Task removed from queue"
        }
    }

    fun clearCompletedJobs() {
        scope.launch(Dispatchers.IO) {
            generationJobDao.deleteFinishedJobs()
            _statusMessage.value = "Cleared completed & cancelled jobs"
        }
    }

    fun moveJob(jobId: String, moveUp: Boolean) {
        scope.launch(Dispatchers.IO) {
            val allQueued = generationJobDao.getQueuedJobs().first()
            val index = allQueued.indexOfFirst { it.id == jobId }
            if (index == -1) return@launch

            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in allQueued.indices) {
                val currentJob = allQueued[index]
                val otherJob = allQueued[targetIndex]

                val currentTimestamp = currentJob.createdAt
                val otherTimestamp = otherJob.createdAt

                generationJobDao.updateJob(currentJob.copy(createdAt = otherTimestamp))
                generationJobDao.updateJob(otherJob.copy(createdAt = currentTimestamp))
            }
        }
    }

    fun moveJobToTop(jobId: String) {
        scope.launch(Dispatchers.IO) {
            val allQueued = generationJobDao.getQueuedJobs().first()
            val currentJob = allQueued.firstOrNull { it.id == jobId } ?: return@launch
            val earliestTimestamp = allQueued.minOfOrNull { it.createdAt } ?: System.currentTimeMillis()
            val updated = currentJob.copy(createdAt = earliestTimestamp - 1000)
            generationJobDao.updateJob(updated)
            _statusMessage.value = "\"${currentJob.title}\" moved to top priority"
        }
    }
}

