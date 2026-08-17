package com.example.ai.inference

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.local.entities.GenerationTaskEntity
import com.example.domain.GenerationEvent
import com.example.domain.GenerationRequest
import com.example.domain.TaskRequirements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * GenerationTaskManager manages background generation tasks using Kotlin Coroutines and Flows,
 * tracking state transitions, persisting task statuses in Room database, and providing
 * real-time updates for model processing without blocking the UI.
 */
class GenerationTaskManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.getDatabase(context)
    private val taskDao = database.generationTaskDao()
    private val modelRouter = ModelRouter(context)

    private val _activeEvents = MutableSharedFlow<Pair<String, GenerationEvent>>()
    val activeEvents: SharedFlow<Pair<String, GenerationEvent>> = _activeEvents.asSharedFlow()

    private val _taskStateFlow = MutableStateFlow<Map<String, GenerationTaskEntity>>(emptyMap())
    val taskStateFlow: StateFlow<Map<String, GenerationTaskEntity>> = _taskStateFlow.asStateFlow()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        scope.launch {
            taskDao.getAllTasks().collect { tasks ->
                _taskStateFlow.value = tasks.associateBy { it.taskId }
            }
        }
    }

    suspend fun startTask(requirements: TaskRequirements, request: GenerationRequest): String {
        val taskId = request.requestId.ifBlank { "task_${UUID.randomUUID()}" }
        
        // Find best model matching requirements
        val modelMeta = modelRouter.findBestModelForTask(requirements)
        val modelId = modelMeta?.modelId

        val taskEntity = GenerationTaskEntity(
            taskId = taskId,
            modelId = modelId,
            taskType = requirements.taskType,
            prompt = request.prompt,
            status = "QUEUED",
            progressPercent = 0f,
            currentStep = 0,
            totalSteps = request.maxTokens,
            statusMessage = "Task queued for execution...",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insertTask(taskEntity)

        // Launch background processing coroutine
        scope.launch {
            executeTaskInternal(taskId, requirements, request, modelId)
        }

        return taskId
    }

    private suspend fun executeTaskInternal(
        taskId: String,
        requirements: TaskRequirements,
        request: GenerationRequest,
        preferredModelId: String?
    ) {
        try {
            taskDao.updateTaskProgress(
                taskId = taskId,
                status = "RUNNING",
                progress = 0.05f,
                step = 0,
                total = request.maxTokens,
                message = "Resolving model and initializing inference backend...",
                output = null,
                error = null
            )

            val modelMeta = if (preferredModelId != null) {
                modelRouter.getModelMetadata(preferredModelId)
            } else {
                modelRouter.findBestModelForTask(requirements)
            }

            if (modelMeta == null) {
                failTask(taskId, "No suitable model found matching task requirements: ${requirements.taskType}")
                return
            }

            val backend = modelRouter.findCompatibleBackend(modelMeta)
            if (backend == null) {
                failTask(taskId, "No compatible inference backend found for model format '${modelMeta.format}'")
                return
            }

            taskDao.updateTaskProgress(
                taskId = taskId,
                status = "RUNNING",
                progress = 0.15f,
                step = 1,
                total = request.maxTokens,
                message = "Loading weights via ${backend.backendName}...",
                output = null,
                error = null
            )

            val loadResult = backend.load(modelMeta)
            if (loadResult.isFailure) {
                failTask(taskId, "Failed to load model session: ${loadResult.exceptionOrNull()?.message}")
                return
            }

            val session = loadResult.getOrThrow()
            try {
                backend.generate(request).collect { event ->
                    _activeEvents.emit(taskId to event)
                    
                    when (event) {
                        is GenerationEvent.Progress -> {
                            taskDao.updateTaskProgress(
                                taskId = taskId,
                                status = "RUNNING",
                                progress = event.progressPercent / 100f,
                                step = event.currentStep,
                                total = event.totalSteps,
                                message = event.statusMessage,
                                output = null,
                                error = null
                            )
                        }
                        is GenerationEvent.Token -> {
                            // Stream token progress
                        }
                        is GenerationEvent.FrameRendered -> {
                            taskDao.updateTaskProgress(
                                taskId = taskId,
                                status = "RUNNING",
                                progress = (event.frameIndex.toFloat() / event.totalFrames),
                                step = event.frameIndex,
                                total = event.totalFrames,
                                message = "Rendered frame ${event.frameIndex}/${event.totalFrames}",
                                output = event.frameUri,
                                error = null
                            )
                        }
                        is GenerationEvent.Completed -> {
                            taskDao.updateTaskProgress(
                                taskId = taskId,
                                status = "COMPLETED",
                                progress = 1.0f,
                                step = request.maxTokens,
                                total = request.maxTokens,
                                message = "Generation completed successfully",
                                output = event.outputUri,
                                error = null
                            )
                        }
                        is GenerationEvent.Error -> {
                            failTask(taskId, event.message)
                        }
                    }
                }
            } finally {
                backend.unload(session)
            }

        } catch (e: Exception) {
            Log.e("GenerationTaskManager", "Task execution failed: ${e.message}", e)
            failTask(taskId, e.localizedMessage ?: "Unknown execution error")
        }
    }

    private suspend fun failTask(taskId: String, errorMessage: String) {
        taskDao.updateTaskProgress(
            taskId = taskId,
            status = "FAILED",
            progress = 0f,
            step = 0,
            total = 100,
            message = "Failed: $errorMessage",
            output = null,
            error = errorMessage
        )
        _activeEvents.emit(taskId to GenerationEvent.Error(errorMessage))
    }

    suspend fun cancelTask(taskId: String) {
        taskDao.updateTaskProgress(
            taskId = taskId,
            status = "CANCELLED",
            progress = 0f,
            step = 0,
            total = 100,
            message = "Task cancelled by user",
            output = null,
            error = "Cancelled"
        )
    }
}
