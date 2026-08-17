package com.example.ui.components.generation

import java.util.Locale

/**
 * Utility functions for formatting durations ranging from 1 second to multiple hours.
 */
object DurationFormatters {

    /**
     * Compact display string like "1s", "15s", "1m 30s", "1h", "2h 30m"
     */
    fun formatDisplay(durationSec: Int): String {
        val safe = durationSec.coerceAtLeast(1)
        return when {
            safe < 60 -> "${safe}s"
            safe < 3600 -> {
                val m = safe / 60
                val s = safe % 60
                if (s == 0) "${m}m" else "${m}m ${s}s"
            }
            else -> {
                val h = safe / 3600
                val m = (safe % 3600) / 60
                val s = safe % 60
                when {
                    m == 0 && s == 0 -> "${h}h"
                    s == 0 -> "${h}h ${m}m"
                    else -> "${h}h ${m}m ${s}s"
                }
            }
        }
    }

    /**
     * Human-readable descriptive label for cards and presets.
     */
    fun formatLongLabel(durationSec: Int): String {
        val safe = durationSec.coerceAtLeast(1)
        return when {
            safe == 1 -> "1 Second (Micro Clip / GIF)"
            safe in 2..4 -> "$safe Seconds (Cinematic Cut)"
            safe == 5 -> "5 Seconds (Standard Shot)"
            safe == 10 -> "10 Seconds (B-Roll)"
            safe == 15 -> "15 Seconds (Shorts / Reels)"
            safe == 30 -> "30 Seconds (Commercial / Trailer)"
            safe == 45 -> "45 Seconds (Extended Scene)"
            safe == 60 -> "1 Minute (Music Video / Scene)"
            safe == 120 -> "2 Minutes (Short Narrative)"
            safe == 300 -> "5 Minutes (Short Film)"
            safe == 600 -> "10 Minutes (YouTube Video Essay)"
            safe == 900 -> "15 Minutes (Extended Feature)"
            safe == 1800 -> "30 Minutes (Broadcast Episode / Doc)"
            safe == 3600 -> "1 Hour (Feature Presentation)"
            safe == 7200 -> "2 Hours (Full Movie)"
            safe == 10800 -> "3 Hours (Epic Film / Extended Cut)"
            safe == 14400 -> "4 Hours (Long-Form Ambient Stream)"
            safe == 21600 -> "6 Hours (Atmosphere / Sleep Video)"
            safe == 28800 -> "8 Hours (Generative Looping Stream)"
            safe == 43200 -> "12 Hours (Half-Day Continuous Run)"
            safe >= 86400 -> "${safe / 3600} Hours (24/7 Broadcast Stream)"
            safe > 3600 -> {
                val h = safe / 3600
                val m = (safe % 3600) / 60
                if (m == 0) "$h Hours (Long-Form)" else "$h Hours $m Min (Long-Form)"
            }
            safe > 60 -> {
                val m = safe / 60
                val s = safe % 60
                if (s == 0) "$m Minutes (Extended)" else "$m Min $s Sec (Extended)"
            }
            else -> "$safe Seconds"
        }
    }

    /**
     * Standard HH:MM:SS timecode string (e.g., "00:00:01", "01:30:00", "04:15:30")
     */
    fun formatHms(durationSec: Int): String {
        val safe = durationSec.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /**
     * Timecode formatted from milliseconds for video players.
     */
    fun formatHmsMs(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val fracSec = (ms % 1000) / 100
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d.%d", h, m, s, fracSec)
        } else {
            String.format(Locale.US, "%02d:%02d.%d", m, s, fracSec)
        }
    }

    /**
     * Calculates estimated file size on disk in MB or GB.
     */
    fun formatEstimatedSize(durationSec: Int, resolution: String, codec: String): String {
        val bitrateMbps = when {
            resolution.contains("4K") -> 25.0
            resolution.contains("2K") -> 12.0
            resolution.contains("1080p") -> 6.0
            resolution.contains("720p") -> 3.0
            else -> 4.5
        }
        val codecEfficiency = when {
            codec.contains("AV1") -> 0.65
            codec.contains("H.265") || codec.contains("HEVC") -> 0.75
            else -> 1.0
        }
        val totalBits = durationSec.toDouble() * bitrateMbps * 1_000_000.0 * codecEfficiency
        val totalBytes = totalBits / 8.0
        val totalMb = totalBytes / (1024.0 * 1024.0)

        return when {
            totalMb >= 1024.0 -> String.format(Locale.US, "%.1f GB", totalMb / 1024.0)
            totalMb >= 1.0 -> String.format(Locale.US, "%.0f MB", totalMb)
            else -> String.format(Locale.US, "%.1f MB", totalMb)
        }
    }

    /**
     * Number of temporal continuous passes required.
     */
    fun getSegmentCount(durationSec: Int, segmentSec: Int = 10): Int {
        if (durationSec <= segmentSec) return 1
        return (durationSec + segmentSec - 1) / segmentSec
    }
}
