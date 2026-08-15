package com.example.ui.state

import com.example.ai.models.ModelCapabilityProfile

/**
 * Dedicated state for Image Generation.
 * Contains ONLY parameters valid for static visual diffusion synthesis.
 * NEVER contains video duration, FPS, codecs, or temporal parameters.
 */
data class ImageGenerationState(
    val prompt: String = "Cyberpunk samurai meditating under neon cherry blossom, cinematic volumetric lighting, 8k octane render",
    val negativePrompt: String = "blurry, low quality, distorted, extra limbs, bad anatomy, artifacts, oversaturated",
    val selectedModelId: String = "sdxl-turbo",
    val selectedModelName: String = "SDXL Turbo (LiteRT)",
    val modelProfile: ModelCapabilityProfile? = null,
    val seed: Long = -1L,
    val batchCount: Int = 1,
    val steps: Int = 30,
    val cfgScale: Float = 7.5f,
    val sampler: String = "Euler a",
    val scheduler: String = "Karras",
    val strength: Float = 0.8f,
    val denoising: Float = 0.7f,
    val width: Int = 1024,
    val height: Int = 1024,
    val aspectRatio: String = "1:1",
    val resolutionLabel: String = "1024x1024",
    val outputFormat: String = "PNG", // PNG, JPEG, WEBP
    val imageStyle: String = "PHOTOREALISTIC",
    val highResFix: Boolean = true,
    val sourceImageUri: String? = null,
    val maskImageUri: String? = null,
    val inputMode: String = "TEXT_TO_IMAGE", // TEXT_TO_IMAGE, IMAGE_TO_IMAGE, INPAINTING, OUTPAINTING, SKETCH
    val isGenerating: Boolean = false,
    val currentProgress: Int = 0,
    val statusMessage: String = "Ready for image synthesis",
    val lastGeneratedImagePath: String? = null,
    val errorMessage: String? = null
)

/**
 * Dedicated state for Video Generation.
 * Contains full Target Video manual configuration, temporal motion controls,
 * camera paths, audio sync, and segmented rendering.
 */
data class VideoGenerationState(
    val prompt: String = "Cinematic drone flythrough across futuristic glowing neon city with flying vehicles and rain reflections, 35mm lens, photorealistic 8k",
    val negativePrompt: String = "jittery, flickering, morphing artifacts, static camera, watermark, low frame rate",
    val motionPrompt: String = "High speed forward dolly rush with soft pan tilt",
    val cameraPrompt: String = "35mm anamorphic wide angle",
    val lightingPrompt: String = "Volumetric cyan and magenta neon backlight with golden hour rim",
    val environmentPrompt: String = "Rainy cyber-metropolis with reflective asphalt",
    val characterMotion: String = "Walking with dynamic cape physics",
    val selectedModelId: String = "sora-mobile-v1",
    val selectedModelName: String = "Sora Mobile V1 (LiteRT)",
    val modelProfile: ModelCapabilityProfile? = null,
    val videoMode: String = "TEXT_TO_VIDEO", // TEXT_TO_VIDEO, IMAGE_TO_VIDEO, VIDEO_TO_VIDEO, VIDEO_EXTENSION, ANIMATION, MOTION_TRANSFER, KEYFRAME_INTERPOLATION
    val durationSec: Int = 5,
    val durationLabel: String = "5 seconds",
    val fps: Int = 24,
    val resolution: String = "1080p",
    val aspectRatio: String = "16:9",
    val seed: Long = -1L,
    val cameraMotion: String = "DYNAMIC_PAN",
    val motionStrength: Float = 0.7f,
    val temporalConsistency: Float = 0.85f,
    val steps: Int = 25,
    val cfgScale: Float = 7.0f,
    val sampler: String = "UniPC",
    val outputFormat: String = "MP4", // MP4, WEBM, MOV, IMAGE_SEQUENCE
    val codec: String = "H.264", // H.264, H.265/HEVC, AV1, VP9
    val bitrate: String = "High", // Auto, Low, Medium, High, Custom
    val audioMode: String = "GENERATED_AUDIO", // NONE, ORIGINAL, GENERATED_AUDIO, IMPORTED, VOICE_OVER, MUSIC, SFX
    val sourceImageUri: String? = null,
    val sourceVideoUri: String? = null,
    val isGenerating: Boolean = false,
    val currentProgress: Int = 0,
    val currentFrame: Int = 0,
    val totalFrames: Int = 120,
    val statusMessage: String = "Ready for video generation",
    val lastGeneratedVideoPath: String? = null,
    val errorMessage: String? = null,
    val currentSegmentIndex: Int = 1,
    val totalSegments: Int = 1
)

/**
 * Dedicated state for Chat Workspace.
 */
data class ChatWorkspaceState(
    val activeBackendType: String = "LOCAL_MODEL", // LOCAL_MODEL, CLOUD_API, LOCAL_SERVER, COMPOSITE_ROUTER, CUSTOM_ENDPOINT
    val activeModelId: String = "qwen-2.5-1.5b",
    val activeModelName: String = "Qwen 2.5 1.5B (GGUF)",
    val customEndpointUrl: String = "http://127.0.0.1:8080/v1",
    val isStreaming: Boolean = false,
    val currentStreamingResponse: String = "",
    val systemPrompt: String = "You are Sora AI Assistant, an on-device neural reasoning and creative copilot for Sora AI Studio."
)
