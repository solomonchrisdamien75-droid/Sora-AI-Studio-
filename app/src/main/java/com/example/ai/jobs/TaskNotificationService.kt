package com.example.ai.jobs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ai.inference.ModelRouter
import com.example.domain.GenerationEvent
import com.example.domain.GenerationRequest
import com.example.domain.TaskRequirements
import kotlinx.coroutines.flow.collect

/**
 * TaskNotificationService / Worker utilizes WorkManager to maintain background generation tasks
 * even when the app is in the background, ensuring users receive real-time progress updates via notifications.
 */
class GenerationTaskWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: "task_default"
        val prompt = inputData.getString("prompt") ?: "Sora AI Generation"
        val taskType = inputData.getString("task_type") ?: "text"
        val modelId = inputData.getString("model_id")

        createNotificationChannel()

        val notificationId = taskId.hashCode()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Sora AI Generation ($taskType)")
            .setContentText("Processing prompt: $prompt")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .build()

        setForegroundAsync(androidx.work.ForegroundInfo(notificationId, notification))

        try {
            val router = ModelRouter(applicationContext)
            val requirements = TaskRequirements(taskType = taskType)
            val request = GenerationRequest(
                requestId = taskId,
                prompt = prompt
            )

            val flow = if (modelId != null) {
                router.executeGeneration(modelId, request)
            } else {
                val bestModel = router.findBestModelForTask(requirements)
                if (bestModel != null) {
                    router.executeGeneration(bestModel.modelId, request)
                } else {
                    return Result.failure()
                }
            }

            flow.collect { event ->
                when (event) {
                    is GenerationEvent.Progress -> {
                        val updatedNotif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                            .setContentTitle("Generating $taskType (${event.progressPercent.toInt()}%)")
                            .setContentText(event.statusMessage)
                            .setSmallIcon(android.R.drawable.stat_sys_upload)
                            .setProgress(100, event.progressPercent.toInt(), false)
                            .setOngoing(true)
                            .build()
                        notificationManager.notify(notificationId, updatedNotif)
                    }
                    is GenerationEvent.Completed -> {
                        val finishedNotif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                            .setContentTitle("Generation Completed Successfully")
                            .setContentText("Output saved at: ${event.outputUri}")
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setAutoCancel(true)
                            .build()
                        notificationManager.notify(notificationId, finishedNotif)
                    }
                    is GenerationEvent.Error -> {
                        val errorNotif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                            .setContentTitle("Generation Failed")
                            .setContentText(event.message)
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            .setAutoCancel(true)
                            .build()
                        notificationManager.notify(notificationId, errorNotif)
                    }
                    else -> {}
                }
            }

            return Result.success()
        } catch (e: Exception) {
            val errorNotif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Generation Error")
                .setContentText(e.localizedMessage ?: "Unknown error")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(notificationId, errorNotif)
            return Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sora Background Generation Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps track of on-device AI generation progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sora_generation_channel"
    }
}
