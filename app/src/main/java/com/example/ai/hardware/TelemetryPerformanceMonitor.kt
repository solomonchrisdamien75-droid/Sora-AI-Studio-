package com.example.ai.hardware

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.RandomAccessFile
import kotlin.math.roundToInt

data class RealtimeTelemetryState(
    val cpuUsagePercent: Int? = null,
    val ramUsedMb: Int = 0,
    val ramTotalMb: Int = 0,
    val ramUsedPercent: Int = 0,
    val gpuLoadPercent: Int? = null, // null if unavailable
    val activeInferenceFps: Float = 0f,
    val thermalStatus: String = "Normal",
    val isThermalThrottled: Boolean = false,
    val activeCores: Int = Runtime.getRuntime().availableProcessors(),
    val isInferencing: Boolean = false,
    val activeModelCount: Int = 0,
    val totalActiveModelsRamMb: Int = 0
) {
    val usedRamMb: Int get() = ramUsedMb
    val totalRamMb: Int get() = ramTotalMb
    val freeRamMb: Int get() = (ramTotalMb - ramUsedMb).coerceAtLeast(0)
    val inferenceFpsBenchmark: Float get() = activeInferenceFps
}

class TelemetryPerformanceMonitor(private val context: Context) {

    private val _telemetryState = MutableStateFlow(RealtimeTelemetryState())
    val telemetryState: StateFlow<RealtimeTelemetryState> = _telemetryState.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()

            while (isActive) {
                activityManager.getMemoryInfo(memInfo)
                val totalMb = (memInfo.totalMem / (1024 * 1024)).toInt()
                val availMb = (memInfo.availMem / (1024 * 1024)).toInt()
                val usedMb = (totalMb - availMb).coerceAtLeast(0)
                val ramPct = if (totalMb > 0) ((usedMb.toDouble() / totalMb.toDouble()) * 100).toInt() else 0

                val currentCpuPct = readCpuUsagePercent()
                val current = _telemetryState.value

                _telemetryState.value = current.copy(
                    cpuUsagePercent = currentCpuPct, // Actually read CPU or null
                    ramUsedMb = usedMb,
                    ramTotalMb = totalMb,
                    ramUsedPercent = ramPct,
                    gpuLoadPercent = null // Android GPU load requires root or vendor specific APIs, so unavailable
                )

                delay(1000)
            }
        }
    }

    fun setInferenceActivity(isInferencing: Boolean, fps: Float = 0f, activeModelsCount: Int = 1, activeModelsRamMb: Int = 2000) {
        _telemetryState.value = _telemetryState.value.copy(
            isInferencing = isInferencing,
            activeInferenceFps = fps,
            activeModelCount = activeModelsCount,
            totalActiveModelsRamMb = activeModelsRamMb
        )
    }

    private fun readCpuUsagePercent(): Int? {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val toks = load.split(" +".toRegex())
            val idle = toks[4].toLong()
            val cpu = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[5].toLong() + toks[6].toLong() + toks[7].toLong()
            val total = idle + cpu
            if (total > 0) ((cpu.toDouble() / total.toDouble()) * 100).roundToInt().coerceIn(0, 100) else null
        } catch (_: Exception) {
            null
        }
    }
}
