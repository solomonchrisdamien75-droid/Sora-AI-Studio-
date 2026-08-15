package com.example.ai.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

data class DeviceHardwareProfile(
    val totalRamGb: Float,
    val availableRamGb: Float,
    val cpuCores: Int,
    val cpuAbi: String,
    val gpuVulkanSupported: Boolean,
    val nnapiSupported: Boolean,
    val internalStorageFreeGb: Float,
    val externalSdFreeGb: Float,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val androidVersion: String,
    val thermalStatus: String,
    val maxRecommendedModelRamMb: Int,
    val performanceTier: PerformanceTier
)

enum class PerformanceTier {
    LOW_END_3GB,     // 3 - 4 GB RAM: Focus on LiteRT, GGUF Q4, small models & Sora Cloud delegation
    MID_RANGE_6GB,   // 6 - 8 GB RAM: Supports Wan 1.3B, Stable Diffusion 1.5, LTX Video 0.9
    HIGH_END_12GB_PLUS // 12+ GB RAM: Supports Flux, Hunyuan Video, CogVideoX, GGUF 8B
}

class HardwareDetector(private val context: Context) {

    fun getDeviceProfile(): DeviceHardwareProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)
        val availRamGb = memoryInfo.availMem / (1024f * 1024f * 1024f)

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        val pm = context.packageManager
        val vulkanSupported = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
        val nnapiSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        // Storage
        val internalStat = StatFs(Environment.getDataDirectory().path)
        val internalFreeGb = (internalStat.availableBlocksLong * internalStat.blockSizeLong) / (1024f * 1024f * 1024f)

        var externalFreeGb = 0f
        val externalDirs = context.getExternalFilesDirs(null)
        if (externalDirs.size > 1 && externalDirs[1] != null) {
            val extFile = externalDirs[1]
            try {
                val extStat = StatFs(extFile.path)
                externalFreeGb = (extStat.availableBlocksLong * extStat.blockSizeLong) / (1024f * 1024f * 1024f)
            } catch (_: Exception) {}
        }

        // Battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(null, filter)
            }
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 80
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            when (powerManager?.currentThermalStatus) {
                android.os.PowerManager.THERMAL_STATUS_NONE -> "Nominal (Cool)"
                android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth"
                android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Thermal"
                android.os.PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttle"
                else -> "Normal"
            }
        } else {
            "Normal"
        }

        val tier = when {
            totalRamGb >= 11f -> PerformanceTier.HIGH_END_12GB_PLUS
            totalRamGb >= 5.5f -> PerformanceTier.MID_RANGE_6GB
            else -> PerformanceTier.LOW_END_3GB
        }

        val maxModelRamMb = (totalRamGb * 1024 * 0.55f).toInt()

        return DeviceHardwareProfile(
            totalRamGb = totalRamGb,
            availableRamGb = availRamGb,
            cpuCores = cpuCores,
            cpuAbi = cpuAbi,
            gpuVulkanSupported = vulkanSupported,
            nnapiSupported = nnapiSupported,
            internalStorageFreeGb = internalFreeGb,
            externalSdFreeGb = externalFreeGb,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            thermalStatus = thermal,
            maxRecommendedModelRamMb = maxModelRamMb,
            performanceTier = tier
        )
    }

    fun canRunModel(requiredRamMb: Int): Pair<Boolean, String> {
        val profile = getDeviceProfile()
        val availRamMb = (profile.availableRamGb * 1024).toInt()

        return if (availRamMb < requiredRamMb) {
            Pair(
                true,
                "Warning: Model requires ${requiredRamMb}MB RAM, but device has ${availRamMb}MB RAM. Force-loading model directly into memory (RAM limits bypassed)."
            )
        } else {
            Pair(true, "Hardware compatible (${availRamMb}MB RAM available).")
        }
    }
}
