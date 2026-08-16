package com.example.ai.jobs

import android.content.Context
import com.example.ai.hardware.HardwareDetector
import com.example.ai.inference.AIInferenceManager
import com.example.data.GenerationJobDao
import com.example.data.GenerationJobEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

enum class AIJobType(val label: String) {
    STORY_GENERATION("Story Generation"),
    SCRIPT_GENERATION("Script Generation"),
    VOICE_SYNTHESIS("Voice Synthesis / TTS"),
    IMAGE_SYNTHESIS("Image Generation"),
    VIDEO_SYNTHESIS("Video Generation"),
    MODEL_QUANTIZATION("Model Quantization")
}

enum class AIJobStatus(val label: String) {
    QUEUED("Queued"),
    LOADING_MODEL("Loading Model"),
    RUNNING("Running"),
    PAUSED("Paused"),
    CANCELLING("Cancelling"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class UnifiedAIJob(
    val jobId: String = "job_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
    val type: AIJobType,
    val title: String,
    val status: AIJobStatus = AIJobStatus.QUEUED,
    val priority: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val modelId: String? = null,
    val modelName: String = "Auto Selected",
    val progress: Float = 0.0f, // 0.0f to 1.0f
    val currentStep: Int = 0,
    val totalSteps: Int = 100,
    val estimatedRemainingSeconds: Long = 0L,
    val inputDescription: String = "",
    val outputPreview: String? = null,
    val errorMessage: String? = null,
    val checkpointPhase: String = "Initialized",
    val tokensGenerated: Int = 0,
    val ramPeakMb: Int = 0,
    val cpuPeakPercent: Int = 0,
    val gpuUsagePercent: Int = 0
)

class AIJobManager(
    private val context: Context,
    private val inferenceManager: AIInferenceManager,
    private val generationJobDao: GenerationJobDao,
    private val scope: CoroutineScope
) {
    private val hardwareDetector = HardwareDetector(context)

    private val _jobs = MutableStateFlow<Map<String, UnifiedAIJob>>(emptyMap())
    val jobs: StateFlow<List<UnifiedAIJob>> = _jobs.map { it.values.toList().sortedByDescending { j -> j.createdAt } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _activeRunningJob = MutableStateFlow<UnifiedAIJob?>(null)
    val activeRunningJob: StateFlow<UnifiedAIJob?> = _activeRunningJob.asStateFlow()

    private val runningCoroutines = mutableMapOf<String, Job>()

    /**
     * Checks hardware resources before launching an intensive job.
     */
    fun checkHardwareReadiness(requiredRamMb: Int = 1024): Pair<Boolean, String> {
        val profile = hardwareDetector.getDeviceProfile()
        val availMb = (profile.availableRamGb * 1024).toInt()

        if (availMb < (requiredRamMb * 0.4f)) {
            return Pair(false, "Insufficient RAM ($availMb MB free, model requires approx $requiredRamMb MB). Job will be queued or require Low RAM mode.")
        }
        return Pair(true, "Hardware resources nominal ($availMb MB free RAM).")
    }

    /**
     * Submits a new AI job to the system.
     */
    fun submitJob(job: UnifiedAIJob): String {
        val updated = _jobs.value.toMutableMap()
        updated[job.jobId] = job
        _jobs.value = updated

        // Sync with Room GenerationJob table for persistent TaskQueue visibility
        scope.launch(Dispatchers.IO) {
            generationJobDao.insertJob(
                GenerationJobEntity(
                    id = job.jobId,
                    title = job.title,
                    prompt = job.inputDescription,
                    generationType = job.type.name,
                    mode = "STANDARD",
                    progressPercent = (job.progress * 100).toInt(),
                    currentFrame = job.currentStep,
                    totalFrames = job.totalSteps,
                    fps = 24f,
                    status = job.status.name,
                    createdAt = job.createdAt,
                    durationSeconds = job.totalSteps / 24,
                    resolution = "1080p",
                    modelName = job.modelName,
                    errorMessage = job.errorMessage
                )
            )
        }

        // Process queue if idle
        processNextInQueue()
        return job.jobId
    }

    /**
     * Updates an existing job's status, progress, and dynamically calculates estimated remaining time.
     */
    fun updateJobProgress(
        jobId: String,
        progress: Float,
        currentStep: Int,
        totalSteps: Int,
        checkpointPhase: String,
        status: AIJobStatus = AIJobStatus.RUNNING,
        outputPreview: String? = null,
        error: String? = null
    ) {
        val current = _jobs.value[jobId] ?: return
        val now = System.currentTimeMillis()
        val startTime = current.startedAt ?: now

        // Calculate dynamic remaining time
        val elapsedSec = ((now - startTime) / 1000L).coerceAtLeast(1)
        val estimatedRemainingSec = if (progress > 0.05f) {
            val totalEstSec = (elapsedSec.toFloat() / progress.coerceIn(0.01f, 1.0f)).toLong()
            (totalEstSec - elapsedSec).coerceAtLeast(0L)
        } else {
            (totalSteps - currentStep) * 2L
        }

        val updatedJob = current.copy(
            status = status,
            progress = progress.coerceIn(0.0f, 1.0f),
            currentStep = currentStep,
            totalSteps = totalSteps,
            checkpointPhase = checkpointPhase,
            estimatedRemainingSeconds = estimatedRemainingSec,
            outputPreview = outputPreview ?: current.outputPreview,
            errorMessage = error ?: current.errorMessage,
            completedAt = if (status == AIJobStatus.COMPLETED || status == AIJobStatus.FAILED || status == AIJobStatus.CANCELLED) now else null
        )

        val updatedMap = _jobs.value.toMutableMap()
        updatedMap[jobId] = updatedJob
        _jobs.value = updatedMap

        if (_activeRunningJob.value?.jobId == jobId) {
            _activeRunningJob.value = if (status == AIJobStatus.RUNNING || status == AIJobStatus.LOADING_MODEL) updatedJob else null
        }

        // Update DB
        scope.launch(Dispatchers.IO) {
            val existing = generationJobDao.getJobById(jobId)
            if (existing != null) {
                generationJobDao.updateJob(
                    existing.copy(
                        progressPercent = (progress * 100).toInt(),
                        currentFrame = currentStep,
                        status = status.name,
                        errorMessage = error ?: existing.errorMessage
                    )
                )
            } else {
                generationJobDao.updateJobStatus(jobId, status.name)
            }
        }
    }

    /**
     * Pauses a running job.
     */
    fun pauseJob(jobId: String) {
        runningCoroutines[jobId]?.cancel()
        runningCoroutines.remove(jobId)
        updateJobProgress(
            jobId = jobId,
            progress = _jobs.value[jobId]?.progress ?: 0f,
            currentStep = _jobs.value[jobId]?.currentStep ?: 0,
            totalSteps = _jobs.value[jobId]?.totalSteps ?: 100,
            checkpointPhase = "Paused by user",
            status = AIJobStatus.PAUSED
        )
        processNextInQueue()
    }

    /**
     * Cancels a job.
     */
    fun cancelJob(jobId: String) {
        runningCoroutines[jobId]?.cancel()
        runningCoroutines.remove(jobId)
        updateJobProgress(
            jobId = jobId,
            progress = _jobs.value[jobId]?.progress ?: 0f,
            currentStep = _jobs.value[jobId]?.currentStep ?: 0,
            totalSteps = _jobs.value[jobId]?.totalSteps ?: 100,
            checkpointPhase = "Cancelled",
            status = AIJobStatus.CANCELLED
        )
        processNextInQueue()
    }

    /**
     * Processes next queued job if no high-priority task is currently blocking.
     */
    private fun processNextInQueue() {
        if (_activeRunningJob.value != null) return
        val nextJob = _jobs.value.values.firstOrNull { it.status == AIJobStatus.QUEUED } ?: return

        val readiness = checkHardwareReadiness()
        if (!readiness.first) {
            // Defer execution
            return
        }

        _activeRunningJob.value = nextJob.copy(status = AIJobStatus.RUNNING, startedAt = System.currentTimeMillis())
        val updatedMap = _jobs.value.toMutableMap()
        updatedMap[nextJob.jobId] = _activeRunningJob.value!!
        _jobs.value = updatedMap
    }
}
