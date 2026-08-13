package com.example.editor

data class VideoFrameItem(
    val id: String,
    val frameIndex: Int,
    val timestampMs: Long,
    val label: String = "Frame",
    val visualHue: Float = 180f,
    val isKeyframe: Boolean = false,
    val captionSnippet: String? = null
)

data class MediaClipTrack(
    val id: String,
    val title: String,
    val filePath: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val playbackSpeed: Float = 1.0f,
    val velocityCurve: String = "NONE", // NONE, AUTO_VELOCITY, MONTAGE_RAMP, HERO_PULSE, BULLET_TIME, FLASH_FREEZE
    val filterName: String = "Normal", // Normal, Cyberpunk Cyan, Vintage Film, Noir Monochrome, Neon Vivid, CapCut Teal/Orange
    val aiStyleEffect: String = "NONE", // NONE, ZOOM_3D_PARALLAX, ANIME_CONVERSION, CYBERPUNK_GLOW, MANGA_SKETCH, RETRO_VHS, LIGHT_LEAKS, BODY_EDGE_GLOW
    val transitionType: String = "NONE", // NONE, WHIP_PAN, FLASH_WHITE, BLUR_SLIDE, GLITCH_TEAR, ZOOM_IN_OUT, PAGE_FLIP
    val isMuted: Boolean = false,
    val volumeLevel: Float = 1.0f,
    val textOverlay: String? = null,
    val subtitleStyle: String = "KINETIC_BOUNCE", // NONE, KINETIC_BOUNCE, NEON_GLOW, KARAOKE_HIGHLIGHT, COMIC_BUBBLE
    val voiceChangerPreset: String = "NONE", // NONE, DEEP_TRAILER, ANIME_HERO, CYBER_ROBOT, CHIPMUNK
    val sfxPreset: String = "NONE", // NONE, BASS_DROP, WHOOSH_SWIPE, GLITCH_STATIC, LASER_FIRE, SWORD_SLASH
    val bgRemovalCutout: Boolean = false,
    val canvasBackground: String = "BLUR", // BLUR, CYBER_NEON, OBSIDIAN_BLACK, SUNSET_GLOW
    val removeWatermark: Boolean = false,
    val watermarkMethod: String = "AI_INPAINT_ERASER", // AI_INPAINT_ERASER, EDGE_CROP, SMART_BLUR_MASK
    val frames: List<VideoFrameItem> = emptyList()
)

enum class AspectRatioPreset(val label: String, val widthRatio: Int, val heightRatio: Int) {
    RATIO_16_9("16:9 Landscape", 16, 9),
    RATIO_9_16("9:16 Vertical Shorts", 9, 16),
    RATIO_1_1("1:1 Square", 1, 1),
    RATIO_21_9("21:9 Ultra Cinema", 21, 9)
}

enum class ExportResolution(val label: String, val heightPx: Int) {
    RES_720P("720p HD", 720),
    RES_1080P("1080p Full HD", 1080),
    RES_4K("4K Ultra HD (High-End GPU)", 2160)
}

data class VideoEditorProject(
    val id: String,
    val name: String,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_16_9,
    val exportResolution: ExportResolution = ExportResolution.RES_1080P,
    val exportFps: Int = 30,
    val videoClips: List<MediaClipTrack> = emptyList(),
    val audioClips: List<MediaClipTrack> = emptyList(),
    val gpuHardwareAcceleration: Boolean = true,
    val cpuMultiThreadCount: Int = 8,
    val npuTensorAcceleration: Boolean = true,
    val globalWatermarkEraser: Boolean = true
)

class VideoEditorEngine {

    fun generateDefaultFramesForClip(clipId: String, durationMs: Long, title: String): List<VideoFrameItem> {
        val frameCount = maxOf(3, minOf(8, (durationMs / 800).toInt() + 1))
        val stepMs = if (frameCount > 1) durationMs / (frameCount - 1) else durationMs
        val baseHue = (clipId.hashCode() % 360).let { if (it < 0) it + 360 else it }.toFloat()
        return (0 until frameCount).map { i ->
            val frameTs = i * stepMs
            VideoFrameItem(
                id = "${clipId}_f$i",
                frameIndex = i + 1,
                timestampMs = frameTs,
                label = "F${i + 1} (${frameTs / 1000}.${(frameTs % 1000) / 100}s)",
                visualHue = (baseHue + i * 32f) % 360f,
                isKeyframe = i == 0 || i == frameCount - 1 || i == frameCount / 2
            )
        }
    }

    fun splitClip(clip: MediaClipTrack, splitAtMs: Long): Pair<MediaClipTrack, MediaClipTrack> {
        val part1Duration = splitAtMs - clip.startMs
        val part2Duration = clip.endMs - splitAtMs
        val clip1 = clip.copy(
            id = "${clip.id}_part1",
            endMs = splitAtMs,
            durationMs = part1Duration,
            frames = generateDefaultFramesForClip("${clip.id}_part1", part1Duration, clip.title)
        )
        val clip2 = clip.copy(
            id = "${clip.id}_part2",
            startMs = splitAtMs,
            durationMs = part2Duration,
            frames = generateDefaultFramesForClip("${clip.id}_part2", part2Duration, clip.title)
        )
        return Pair(clip1, clip2)
    }

    fun applyFilter(clip: MediaClipTrack, filterName: String): MediaClipTrack {
        return clip.copy(filterName = filterName)
    }

    fun adjustSpeed(clip: MediaClipTrack, speedMultiplier: Float): MediaClipTrack {
        return clip.copy(playbackSpeed = speedMultiplier)
    }

    fun getTotalDurationMs(project: VideoEditorProject): Long {
        return project.videoClips.sumOf { (it.durationMs / it.playbackSpeed).toLong() }
    }
}
