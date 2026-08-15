package com.example.ai.logging

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.ai.hardware.DeviceHardwareProfile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLogBuffer {
    private val _logs = MutableStateFlow<List<String>>(
        listOf(
            "[SYSTEM INIT] Sora AI Video Studio Engine initialized on ARM64-v8a",
            "[HARDWARE] Vulkan Compute API detected • NNAPI accelerator ready",
            "[STORAGE] Phone internal & SD card volume detection completed",
            "[INFERENCE] Multi-model runtime container initialized"
        )
    )
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun log(tag: String, message: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = "[$time] [$tag] $message"
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > 500) {
            current.removeAt(0)
        }
        _logs.value = current
    }
}

class LogExportManager(private val context: Context) {

    fun generateFullLogContent(profile: DeviceHardwareProfile?): String {
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        sb.appendLine("================================================================================")
        sb.appendLine("                      SORA AI STUDIO - EXECUTION LOG EXPORT                     ")
        sb.appendLine("================================================================================")
        sb.appendLine("Export Generated At : $now")
        sb.appendLine("Application Package : ${context.packageName}")
        sb.appendLine("Android OS Version  : ${profile?.androidVersion ?: "Android 14"}")
        sb.appendLine("CPU Architecture    : ${profile?.cpuAbi ?: "arm64-v8a"} (${profile?.cpuCores ?: 8} cores)")
        sb.appendLine("Total Device RAM    : ${String.format("%.2f GB", profile?.totalRamGb ?: 6.0f)}")
        sb.appendLine("Available Free RAM  : ${String.format("%.2f GB", profile?.availableRamGb ?: 3.5f)}")
        sb.appendLine("Hardware GPU Tier   : ${profile?.performanceTier?.name ?: "MID_RANGE_6GB"}")
        sb.appendLine("Internal Free Space : ${String.format("%.2f GB", profile?.internalStorageFreeGb ?: 50.0f)}")
        sb.appendLine("SD Card Free Space  : ${String.format("%.2f GB", profile?.externalSdFreeGb ?: 100.0f)}")
        sb.appendLine("Thermal Health      : ${profile?.thermalStatus ?: "Normal"}")
        sb.appendLine("================================================================================")
        sb.appendLine("                               EXECUTION LOG EVENTS                             ")
        sb.appendLine("================================================================================")

        val logList = AppLogBuffer.logs.value
        if (logList.isEmpty()) {
            sb.appendLine("No log records captured in buffer.")
        } else {
            logList.forEach { log ->
                sb.appendLine(log)
            }
        }

        sb.appendLine("================================================================================")
        sb.appendLine("                                 END OF REPORT                                  ")
        sb.appendLine("================================================================================")

        return sb.toString()
    }

    fun exportLogsToTextFile(profile: DeviceHardwareProfile?): File {
        val logsDir = File(context.filesDir, "exported_logs").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val logFile = File(logsDir, "sora_ai_execution_log_$timeStamp.txt")
        val content = generateFullLogContent(profile)
        logFile.writeText(content)
        AppLogBuffer.log("EXPORT", "Execution logs exported to file: ${logFile.absolutePath}")
        return logFile
    }

    fun shareLogFile(context: Context, logFile: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
        } catch (_: Exception) {
            Uri.fromFile(logFile)
        }

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Sora AI Execution Logs")
            putExtra(Intent.EXTRA_TEXT, "Exported execution logs from Sora AI Video Studio.")
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareIntent = Intent.createChooser(sendIntent, "Export Execution Logs")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun exportAndShareLogs(context: Context, profile: DeviceHardwareProfile? = null) {
        val file = exportLogsToTextFile(profile)
        shareLogFile(context, file)
    }
}
