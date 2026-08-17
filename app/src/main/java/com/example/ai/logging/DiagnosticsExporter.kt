package com.example.ai.logging

import android.content.Context
import android.os.Environment
import com.example.ai.hardware.HardwareMonitor
import com.example.data.AppDatabase
import com.example.data.GenerationLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * DiagnosticsExporter queries the GenerationLogEntity table in Room and
 * allows users to save a real diagnostic report file (txt) to a chosen directory for troubleshooting.
 */
class DiagnosticsExporter(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val logDao = database.generationLogDao()
    private val hardwareMonitor = HardwareMonitor(context)

    suspend fun exportDiagnosticsReport(targetDirectory: File? = null): File = withContext(Dispatchers.IO) {
        val now = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val fileName = "sora_ai_diagnostics_$now.txt"
        
        val outputDir = targetDirectory ?: File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SoraDiagnostics").apply { mkdirs() }
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val reportFile = File(outputDir, fileName)
        val sb = StringBuilder()

        // Gather real hardware metrics
        val metrics = hardwareMonitor.getSystemMetrics()
        
        // Query GenerationLogEntity records from Room
        val roomLogs: List<GenerationLogEntity> = try {
            logDao.getAllLogs().firstOrNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        sb.appendLine("================================================================================")
        sb.appendLine("                   SORA AI STUDIO - SYSTEM DIAGNOSTICS REPORT                   ")
        sb.appendLine("================================================================================")
        sb.appendLine("Generated At        : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("App Package         : ${context.packageName}")
        sb.appendLine("OS Build Version    : Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Device Hardware     : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (${android.os.Build.DEVICE})")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("HARDWARE RESOURCE STATUS:")
        sb.appendLine("  - CPU Usage       : ${String.format(Locale.US, "%.1f%%", metrics.cpuUsagePercent)}")
        sb.appendLine("  - Total RAM       : ${metrics.totalRamMb} MB")
        sb.appendLine("  - Used RAM        : ${metrics.usedRamMb} MB (${String.format(Locale.US, "%.1f%%", metrics.ramUsagePercent)})")
        sb.appendLine("  - Free RAM        : ${metrics.availableRamMb} MB")
        sb.appendLine("  - Battery Level   : ${metrics.batteryPercent}% (${if (metrics.isCharging) "Charging" else "Discharging"})")
        sb.appendLine("  - Battery Temp    : ${metrics.batteryTemperatureCelsius}°C")
        sb.appendLine("  - Internal Storage: ${String.format(Locale.US, "%.2f GB free / %.2f GB total", metrics.internalStorageFreeGb, metrics.internalStorageTotalGb)}")
        sb.appendLine("================================================================================")
        sb.appendLine("ROOM DATABASE GENERATION LOGS (${roomLogs.size} entries):")
        sb.appendLine("================================================================================")

        if (roomLogs.isEmpty()) {
            sb.appendLine("No persistent generation log records found in Room database.")
        } else {
            roomLogs.forEach { log ->
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(log.timestamp))
                sb.appendLine("[$timeStr] [${log.projectType}] Status: ${log.status} | Model: ${log.modelUsed} | Latency: ${log.latencyMs}ms")
                sb.appendLine("  Prompt: ${log.prompt}")
                if (log.logDetails.isNotBlank()) {
                    sb.appendLine("  Details: ${log.logDetails}")
                }
                sb.appendLine("--------------------------------------------------------------------------------")
            }
        }

        sb.appendLine("================================================================================")
        sb.appendLine("IN-MEMORY BUFFER LOGS:")
        sb.appendLine("================================================================================")

        val bufferLogs = AppLogBuffer.logs.value
        if (bufferLogs.isEmpty()) {
            sb.appendLine("No log records found in memory buffer.")
        } else {
            bufferLogs.forEach { logLine ->
                sb.appendLine(logLine)
            }
        }

        sb.appendLine("================================================================================")
        sb.appendLine("                                 END OF REPORT                                  ")
        sb.appendLine("================================================================================")

        reportFile.writeText(sb.toString())
        reportFile
    }
}
