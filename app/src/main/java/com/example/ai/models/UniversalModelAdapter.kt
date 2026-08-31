package com.example.ai.models

import com.example.ai.hardware.DeviceHardwareProfile

enum class ModelFormat {
    GGUF,
    GGML,
    SAFETENSORS,
    PYTORCH,
    ONNX,
    ONNX_RUNTIME,
    TFLITE,
    LITERT,
    MNN,
    NCNN,
    TORCHSCRIPT,
    DIFFUSERS,
    LORA,
    CONTROLNET,
    VAE,
    TEXT_ENCODER,
    TOKENIZER,
    VISION_ENCODER,
    UNKNOWN
}

enum class ModelFamily {
    TEXT_LLM,
    IMAGE_DIFFUSION,
    VIDEO_DIFFUSION,
    AUDIO,
    VISION,
    UNKNOWN
}

enum class ModelTask {
    TEXT_GENERATION,
    CHAT,
    IMAGE_GENERATION,
    IMAGE_TO_IMAGE,
    INPAINTING,
    OUTPAINTING,
    VIDEO_GENERATION,
    TEXT_TO_VIDEO,
    IMAGE_TO_VIDEO,
    VIDEO_TO_VIDEO,
    VIDEO_EXTENSION,
    AUDIO_TO_TEXT,
    TEXT_TO_SPEECH,
    VOICE_CLONING,
    IMAGE_CAPTIONING,
    OCR,
    VISUAL_QUESTION_ANSWERING,
    OBJECT_DETECTION,
    SEGMENTATION,
    MULTIMODAL,
    EMBEDDING
}

data class UniversalGenerationRequest(
    val modelId: String,
    val task: String,
    val prompt: String = "",
    val negativePrompt: String? = null,
    val inputImage: String? = null,
    val inputVideo: String? = null,
    val audio: String? = null,
    val referenceImages: List<String> = emptyList(),
    val referenceVideo: String? = null,
    val duration: Int = 5,
    val fps: Int = 24,
    val width: Int = 1024,
    val height: Int = 1024,
    val seed: Long? = null,
    val steps: Int = 25,
    val guidance: Float = 7.5f,
    val strength: Float = 0.5f,
    val firstFrame: String? = null,
    val lastFrame: String? = null,
    val keyframes: List<String> = emptyList(),
    val controlInputs: Map<String, Any> = emptyMap(),
    val LoRA: String? = null,
    val outputFormat: String = "mp4"
)

data class ModelDetectionResult(
    val family: ModelFamily,
    val format: ModelFormat,
    val architecture: String,
    val modelSizeMb: Long,
    val quantization: String,
    val backend: String,
    val supportedTasks: Set<ModelTask>,
    val requiredComponents: List<String>,
    val supportedInputs: List<String>,
    val supportedOutputs: List<String>,
    val hardwareRequirements: Map<String, String>,
    val memoryRequirementMb: Int,
    val isRuntimeAvailable: Boolean = true,
    val fileName: String,
    val displayName: String
)

abstract class ModelAdapter(
    open val modelId: String,
    open val modelName: String,
    open val detection: ModelDetectionResult
) {
    abstract fun identify(): String
    abstract fun validate(): Boolean
    abstract fun getCapabilities(): Set<ModelTask>
    abstract fun getRequirements(): Map<String, Any>
    abstract fun load(): Result<Unit>
    abstract fun unload()
    abstract fun generate(request: UniversalGenerationRequest): Result<Any>
    abstract fun cancel()
    abstract fun getProgress(): Float
    abstract fun getMemoryRequirement(): Int
    abstract fun supportsHardware(hardware: DeviceHardwareProfile): Boolean
    abstract fun supportsInput(inputType: String): Boolean
    abstract fun supportsOutput(outputType: String): Boolean
}

object ModelDetector {
    fun detectModel(fileName: String, customName: String? = null): ModelDetectionResult {
        val name = (customName ?: fileName).trim()
        val lowerName = name.lowercase()
        val format = detectFormat(fileName)
        val family = detectFamily(lowerName, format)
        val architecture = detectArchitecture(lowerName, family)
        val quantization = detectQuantization(lowerName)
        val backend = detectBackend(format, family, lowerName)
        val tasks = detectTasks(family, lowerName)
        val memoryMb = estimateMemoryMb(format, lowerName)
        val runtimeAvailable = detectRuntimeAvailability(family, format)

        return ModelDetectionResult(
            family = family,
            format = format,
            architecture = architecture,
            modelSizeMb = 0L,
            quantization = quantization,
            backend = backend,
            supportedTasks = tasks,
            requiredComponents = detectRequiredComponents(family, format),
            supportedInputs = detectSupportedInputs(family, lowerName),
            supportedOutputs = detectSupportedOutputs(family),
            hardwareRequirements = detectHardwareRequirements(family, memoryMb),
            memoryRequirementMb = memoryMb,
            isRuntimeAvailable = runtimeAvailable,
            fileName = fileName,
            displayName = name
        )
    }

    private fun detectFormat(fileName: String): ModelFormat = when {
        fileName.lowercase().endsWith(".gguf") -> ModelFormat.GGUF
        fileName.lowercase().endsWith(".ggml") -> ModelFormat.GGML
        fileName.lowercase().endsWith(".safetensors") -> ModelFormat.SAFETENSORS
        fileName.lowercase().endsWith(".pt") || fileName.lowercase().endsWith(".pth") || fileName.lowercase().endsWith(".ckpt") -> ModelFormat.PYTORCH
        fileName.lowercase().endsWith(".onnx") -> ModelFormat.ONNX
        fileName.lowercase().endsWith(".tflite") -> ModelFormat.TFLITE
        fileName.lowercase().endsWith(".litert") -> ModelFormat.LITERT
        fileName.lowercase().endsWith(".mnn") -> ModelFormat.MNN
        fileName.lowercase().endsWith(".param") || fileName.lowercase().endsWith(".bin") -> ModelFormat.NCNN
        fileName.lowercase().endsWith(".ts") -> ModelFormat.TORCHSCRIPT
        fileName.lowercase().endsWith(".lora") -> ModelFormat.LORA
        fileName.lowercase().contains("controlnet") -> ModelFormat.CONTROLNET
        fileName.lowercase().contains("vae") -> ModelFormat.VAE
        else -> ModelFormat.UNKNOWN
    }

    private fun detectFamily(lowerName: String, format: ModelFormat): ModelFamily = when {
        lowerName.contains("llama") || lowerName.contains("qwen") || lowerName.contains("gemma") || lowerName.contains("mistral") || lowerName.contains("phi") || lowerName.contains("deepseek") || lowerName.contains("smol") || (format == ModelFormat.GGUF && (lowerName.contains("instruct") || lowerName.contains("chat"))) -> ModelFamily.TEXT_LLM
        lowerName.contains("ltx") || lowerName.contains("wan") || lowerName.contains("cogvideo") || lowerName.contains("hunyuan") || lowerName.contains("mochi") || lowerName.contains("svd") || lowerName.contains("animatediff") || lowerName.contains("cosmos") || lowerName.contains("framepack") -> ModelFamily.VIDEO_DIFFUSION
        lowerName.contains("whisper") || lowerName.contains("tts") || lowerName.contains("voice") || lowerName.contains("kokoro") || lowerName.contains("vits") || lowerName.contains("xtts") || lowerName.contains("musicgen") || lowerName.contains("audiocraft") -> ModelFamily.AUDIO
        lowerName.contains("llava") || lowerName.contains("qwen-vl") || lowerName.contains("minicpm") || lowerName.contains("florence") || lowerName.contains("sam") || lowerName.contains("ocr") || lowerName.contains("caption") -> ModelFamily.VISION
        lowerName.contains("sdxl") || lowerName.contains("flux") || lowerName.contains("stable") || lowerName.contains("diffusion") || lowerName.contains("pixart") || lowerName.contains("kandinsky") || lowerName.contains("auraflow") -> ModelFamily.IMAGE_DIFFUSION
        else -> ModelFamily.UNKNOWN
    }

    private fun detectArchitecture(lowerName: String, family: ModelFamily): String = when (family) {
        ModelFamily.TEXT_LLM -> when {
            lowerName.contains("qwen") -> "Qwen2.5"
            lowerName.contains("llama") -> "Llama3"
            lowerName.contains("gemma") -> "Gemma"
            lowerName.contains("mistral") -> "Mistral"
            lowerName.contains("phi") -> "Phi"
            lowerName.contains("deepseek") -> "DeepSeek"
            else -> "Transformer-LLM"
        }
        ModelFamily.VIDEO_DIFFUSION -> when {
            lowerName.contains("ltx") -> "LTX-Video"
            lowerName.contains("wan") -> "Wan-DiT"
            lowerName.contains("cogvideo") -> "CogVideoX"
            lowerName.contains("hunyuan") -> "HunyuanVideo"
            lowerName.contains("svd") -> "StableVideoDiffusion"
            lowerName.contains("mochi") -> "Mochi"
            else -> "Video-Diffusion"
        }
        ModelFamily.IMAGE_DIFFUSION -> when {
            lowerName.contains("sdxl") -> "StableDiffusionXL"
            lowerName.contains("flux") -> "Flux"
            lowerName.contains("pixart") -> "PixArt"
            lowerName.contains("kandinsky") -> "Kandinsky"
            lowerName.contains("auraflow") -> "AuraFlow"
            else -> "LatentDiffusion"
        }
        ModelFamily.AUDIO -> when {
            lowerName.contains("whisper") -> "Whisper"
            lowerName.contains("kokoro") -> "Kokoro"
            lowerName.contains("xtts") -> "XTTS"
            lowerName.contains("vits") -> "VITS"
            lowerName.contains("piper") -> "Piper"
            else -> "AudioTransformer"
        }
        ModelFamily.VISION -> when {
            lowerName.contains("llava") -> "LLaVA"
            lowerName.contains("qwen-vl") -> "Qwen-VL"
            lowerName.contains("sam") -> "SAM"
            lowerName.contains("florence") -> "Florence"
            else -> "VisionTransformer"
        }
        ModelFamily.UNKNOWN -> "Unknown"
    }

    private fun detectQuantization(lowerName: String): String = when {
        lowerName.contains("q4") || lowerName.contains("q4_k") -> "Q4_K_M"
        lowerName.contains("q5") -> "Q5_K_M"
        lowerName.contains("q8") -> "Q8_0"
        lowerName.contains("fp8") -> "FP8"
        lowerName.contains("fp16") -> "FP16"
        lowerName.contains("int4") -> "INT4"
        lowerName.contains("int8") -> "INT8"
        else -> "Unspecified"
    }

    private fun detectBackend(format: ModelFormat, family: ModelFamily, lowerName: String): String = when (format) {
        ModelFormat.GGUF, ModelFormat.GGML -> "llama.cpp / Vulkan / CPU"
        ModelFormat.ONNX, ModelFormat.ONNX_RUNTIME -> "ONNX Runtime / NNAPI"
        ModelFormat.TFLITE, ModelFormat.LITERT -> "LiteRT / NNAPI / Vulkan"
        ModelFormat.SAFETENSORS -> "PyTorch / Diffusers / ONNX"
        ModelFormat.MNN -> "MNN"
        ModelFormat.NCNN -> "NCNN"
        ModelFormat.PYTORCH -> if (family == ModelFamily.VIDEO_DIFFUSION) "PyTorch / Vulkan" else "PyTorch"
        else -> "Runtime-dependent"
    }

    private fun detectTasks(family: ModelFamily, lowerName: String): Set<ModelTask> = when (family) {
        ModelFamily.TEXT_LLM -> setOf(ModelTask.TEXT_GENERATION, ModelTask.CHAT, ModelTask.EMBEDDING)
        ModelFamily.IMAGE_DIFFUSION -> setOf(ModelTask.IMAGE_GENERATION, ModelTask.IMAGE_TO_IMAGE, ModelTask.INPAINTING, ModelTask.OUTPAINTING)
        ModelFamily.VIDEO_DIFFUSION -> {
            val tasks = mutableSetOf(ModelTask.VIDEO_GENERATION)
            if (lowerName.contains("ltx") || lowerName.contains("wan") || lowerName.contains("sora") || lowerName.contains("video")) {
                tasks += ModelTask.TEXT_TO_VIDEO
                tasks += ModelTask.IMAGE_TO_VIDEO
                tasks += ModelTask.VIDEO_TO_VIDEO
                tasks += ModelTask.VIDEO_EXTENSION
            }
            tasks
        }
        ModelFamily.AUDIO -> {
            val tasks = mutableSetOf(ModelTask.TEXT_TO_SPEECH)
            if (lowerName.contains("whisper")) tasks += ModelTask.AUDIO_TO_TEXT
            if (lowerName.contains("voice") || lowerName.contains("clone") || lowerName.contains("xtts")) tasks += ModelTask.VOICE_CLONING
            tasks
        }
        ModelFamily.VISION -> setOf(ModelTask.IMAGE_CAPTIONING, ModelTask.OCR, ModelTask.VISUAL_QUESTION_ANSWERING, ModelTask.OBJECT_DETECTION, ModelTask.SEGMENTATION)
        ModelFamily.UNKNOWN -> emptySet()
    }

    private fun detectRequiredComponents(family: ModelFamily, format: ModelFormat): List<String> = when (family) {
        ModelFamily.TEXT_LLM -> listOf("tokenizer", "transformer", "backend")
        ModelFamily.IMAGE_DIFFUSION -> listOf("text encoder", "vae", "unet", "scheduler")
        ModelFamily.VIDEO_DIFFUSION -> listOf("text encoder", "vae", "transformer", "scheduler", "conditioning")
        ModelFamily.AUDIO -> listOf("audio tokenizer", "decoder", "vocoder")
        ModelFamily.VISION -> listOf("vision encoder", "classifier", "tokenizer")
        ModelFamily.UNKNOWN -> listOf("validation")
    }

    private fun detectSupportedInputs(family: ModelFamily, lowerName: String): List<String> = when (family) {
        ModelFamily.TEXT_LLM -> listOf("text")
        ModelFamily.IMAGE_DIFFUSION -> listOf("text", "image")
        ModelFamily.VIDEO_DIFFUSION -> listOf("text", "image", "video", "keyframe")
        ModelFamily.AUDIO -> listOf("text", "audio")
        ModelFamily.VISION -> listOf("image", "text")
        ModelFamily.UNKNOWN -> listOf("unknown")
    }

    private fun detectSupportedOutputs(family: ModelFamily): List<String> = when (family) {
        ModelFamily.TEXT_LLM -> listOf("text")
        ModelFamily.IMAGE_DIFFUSION -> listOf("image")
        ModelFamily.VIDEO_DIFFUSION -> listOf("video")
        ModelFamily.AUDIO -> listOf("audio", "text")
        ModelFamily.VISION -> listOf("text", "bounding boxes", "mask")
        ModelFamily.UNKNOWN -> listOf("unknown")
    }

    private fun detectHardwareRequirements(family: ModelFamily, memoryMb: Int): Map<String, String> = mapOf(
        "family" to family.name,
        "minimumRamMb" to memoryMb.toString(),
        "gpu" to if (family == ModelFamily.VIDEO_DIFFUSION || family == ModelFamily.IMAGE_DIFFUSION) "Vulkan preferred" else "CPU ok",
        "npu" to "Device-dependent",
        "precision" to "FP16 or quantized"
    )

    private fun detectRuntimeAvailability(family: ModelFamily, format: ModelFormat): Boolean = when (family) {
        ModelFamily.TEXT_LLM -> format == ModelFormat.GGUF || format == ModelFormat.GGML || format == ModelFormat.ONNX || format == ModelFormat.ONNX_RUNTIME || format == ModelFormat.TFLITE || format == ModelFormat.LITERT
        ModelFamily.IMAGE_DIFFUSION -> format == ModelFormat.ONNX || format == ModelFormat.SAFETENSORS || format == ModelFormat.TFLITE || format == ModelFormat.LITERT || format == ModelFormat.PYTORCH
        ModelFamily.VIDEO_DIFFUSION -> format == ModelFormat.PYTORCH || format == ModelFormat.SAFETENSORS || format == ModelFormat.ONNX || format == ModelFormat.MNN || format == ModelFormat.NCNN
        ModelFamily.AUDIO -> format == ModelFormat.ONNX || format == ModelFormat.TFLITE || format == ModelFormat.LITERT || format == ModelFormat.PYTORCH
        ModelFamily.VISION -> format == ModelFormat.ONNX || format == ModelFormat.TFLITE || format == ModelFormat.LITERT || format == ModelFormat.PYTORCH
        ModelFamily.UNKNOWN -> false
    }

    private fun estimateMemoryMb(format: ModelFormat, lowerName: String): Int {
        val multiplier = when (format) {
            ModelFormat.GGUF, ModelFormat.GGML -> 1.2f
            ModelFormat.ONNX, ModelFormat.ONNX_RUNTIME -> 1.4f
            ModelFormat.SAFETENSORS -> 1.5f
            ModelFormat.PYTORCH -> 2.0f
            ModelFormat.MNN, ModelFormat.NCNN -> 1.3f
            ModelFormat.TFLITE, ModelFormat.LITERT -> 1.1f
            else -> 1.0f
        }

        val baseMb = when {
            lowerName.contains("13b") -> 25000
            lowerName.contains("7b") -> 14000
            lowerName.contains("3b") -> 7000
            lowerName.contains("1.5b") || lowerName.contains("1b") -> 3500
            lowerName.contains("xl") -> 6000
            lowerName.contains("ltx") -> 9000
            lowerName.contains("wan") -> 12000
            else -> 3000
        }
        return (baseMb * multiplier).toInt().coerceAtLeast(1024)
    }
}

object ModelAdapterRegistry {
    private val adapters = mutableListOf<ModelAdapter>()

    fun register(adapter: ModelAdapter) {
        adapters += adapter
    }

    fun resolve(modelId: String, name: String, fileName: String): ModelAdapter? {
        val detection = ModelDetector.detectModel(fileName, name)
        return when {
            detection.family == ModelFamily.VIDEO_DIFFUSION && detection.architecture.contains("LTX", ignoreCase = true) -> LTXVideoModelAdapter(modelId, name, detection)
            detection.family == ModelFamily.TEXT_LLM -> GenericTextModelAdapter(modelId, name, detection)
            detection.family == ModelFamily.IMAGE_DIFFUSION -> GenericImageDiffusionAdapter(modelId, name, detection)
            detection.family == ModelFamily.AUDIO -> GenericAudioModelAdapter(modelId, name, detection)
            detection.family == ModelFamily.VISION -> GenericVisionModelAdapter(modelId, name, detection)
            else -> adapters.firstOrNull { it.modelId == modelId }
        }
    }

    fun registeredCount(): Int = adapters.size
}

class ModelManager {
    private val loadedModels = linkedMapOf<String, ModelAdapter>()

    fun detect(fileName: String, customName: String? = null): ModelDetectionResult = ModelDetector.detectModel(fileName, customName)

    fun createAdapter(modelId: String, name: String, fileName: String): ModelAdapter? = ModelAdapterRegistry.resolve(modelId, name, fileName)

    fun loadModel(modelId: String, name: String, fileName: String, hardware: DeviceHardwareProfile): Result<ModelAdapter> {
        val adapter = createAdapter(modelId, name, fileName)
            ?: return Result.failure(IllegalArgumentException("No adapter registered for $modelId ($fileName)"))

        if (!adapter.validate()) {
            return Result.failure(IllegalStateException("MODEL LOAD FAILED: validation failed for ${adapter.modelName}"))
        }

        if (!adapter.detection.isRuntimeAvailable) {
            return Result.failure(IllegalStateException("SUPPORTED ARCHITECTURE but RUNTIME NOT AVAILABLE ON THIS DEVICE for ${adapter.modelName}"))
        }

        val freeRamMb = (hardware.availableRamGb * 1024f).toInt()
        val memoryNeeded = adapter.getMemoryRequirement()
        if (freeRamMb < memoryNeeded * 0.7f) {
            return Result.failure(IllegalStateException("MODEL LOAD FAILED: insufficient RAM. Need ${memoryNeeded}MB, available ${freeRamMb}MB."))
        }

        if (!adapter.supportsHardware(hardware)) {
            return Result.failure(IllegalStateException("MODEL LOAD FAILED: ${adapter.modelName} cannot run on this hardware profile."))
        }

        val backend = HardwareDispatcher().selectBackend(adapter.detection, hardware)
        if (backend == "UNSUPPORTED") {
            return Result.failure(IllegalStateException("MODEL LOAD FAILED: no compatible backend available for ${adapter.modelName}"))
        }

        val loadResult = adapter.load()
        if (loadResult.isFailure) {
            return Result.failure(loadResult.exceptionOrNull() ?: IllegalStateException("MODEL LOAD FAILED: unknown adapter error"))
        }

        loadedModels[modelId] = adapter
        return Result.success(adapter)
    }

    fun unloadModel(modelId: String) {
        val adapter = loadedModels.remove(modelId)
        adapter?.unload()
    }

    fun findCompatibleFallback(modelId: String, hardware: DeviceHardwareProfile): ModelAdapter? {
        return loadedModels.values.firstOrNull { it.modelId != modelId && it.supportsHardware(hardware) }
    }

    fun isLoaded(modelId: String): Boolean = loadedModels.containsKey(modelId)
}

class HardwareDispatcher {
    fun selectBackend(model: ModelDetectionResult, hardware: DeviceHardwareProfile): String {
        val ramMb = (hardware.availableRamGb * 1024f).toInt()
        val memoryReady = ramMb >= model.memoryRequirementMb * 0.6f

        return when {
            model.family == ModelFamily.TEXT_LLM && hardware.gpuVulkanSupported && memoryReady -> "llama.cpp / Vulkan"
            model.family == ModelFamily.TEXT_LLM && hardware.nnapiSupported -> "NNAPI"
            model.family == ModelFamily.VIDEO_DIFFUSION && hardware.gpuVulkanSupported && memoryReady -> "Vulkan GPU"
            model.family == ModelFamily.IMAGE_DIFFUSION && hardware.gpuVulkanSupported && memoryReady -> "Vulkan GPU"
            model.family == ModelFamily.AUDIO && hardware.nnapiSupported -> "ONNX Runtime / NNAPI"
            model.family == ModelFamily.VISION && hardware.nnapiSupported -> "LiteRT / NNAPI"
            memoryReady -> "CPU"
            else -> "UNSUPPORTED"
        }
    }
}

class LocalInferenceEngine {
    private val dispatcher = HardwareDispatcher()

    fun run(request: UniversalGenerationRequest, model: ModelDetectionResult, hardware: DeviceHardwareProfile): Result<String> {
        val backend = dispatcher.selectBackend(model, hardware)
        if (backend == "UNSUPPORTED") {
            return Result.failure(IllegalStateException("MODEL LOAD FAILED: hardware dispatcher rejected ${model.architecture} on this device"))
        }
        return Result.success("Running ${model.architecture} via $backend for ${request.task}")
    }
}

class GenericTextModelAdapter(
    modelId: String,
    modelName: String,
    detection: ModelDetectionResult
) : ModelAdapter(modelId, modelName, detection) {
    override fun identify(): String = detection.architecture
    override fun validate(): Boolean = detection.family == ModelFamily.TEXT_LLM
    override fun getCapabilities(): Set<ModelTask> = detection.supportedTasks
    override fun getRequirements(): Map<String, Any> = mapOf(
        "family" to detection.family.name,
        "backend" to detection.backend,
        "format" to detection.format.name,
        "requiresTokenizer" to true,
        "memoryMB" to detection.memoryRequirementMb
    )
    override fun load(): Result<Unit> = Result.success(Unit)
    override fun unload() = Unit
    override fun generate(request: UniversalGenerationRequest): Result<Any> = Result.success(mapOf("modelId" to modelId, "task" to request.task, "status" to "queued"))
    override fun cancel() = Unit
    override fun getProgress(): Float = 0f
    override fun getMemoryRequirement(): Int = detection.memoryRequirementMb
    override fun supportsHardware(hardware: DeviceHardwareProfile): Boolean = when {
        detection.format == ModelFormat.GGUF || detection.format == ModelFormat.GGML -> hardware.gpuVulkanSupported || hardware.nnapiSupported || hardware.availableRamGb >= 2f
        else -> hardware.availableRamGb >= 1.5f
    }
    override fun supportsInput(inputType: String): Boolean = inputType.lowercase() in setOf("text")
    override fun supportsOutput(outputType: String): Boolean = outputType.lowercase() in setOf("text")
}

class GenericImageDiffusionAdapter(
    modelId: String,
    modelName: String,
    detection: ModelDetectionResult
) : ModelAdapter(modelId, modelName, detection) {
    override fun identify(): String = detection.architecture
    override fun validate(): Boolean = detection.family == ModelFamily.IMAGE_DIFFUSION
    override fun getCapabilities(): Set<ModelTask> = detection.supportedTasks
    override fun getRequirements(): Map<String, Any> = mapOf(
        "family" to detection.family.name,
        "backend" to detection.backend,
        "format" to detection.format.name,
        "requiresVAE" to true,
        "memoryMB" to detection.memoryRequirementMb
    )
    override fun load(): Result<Unit> = Result.success(Unit)
    override fun unload() = Unit
    override fun generate(request: UniversalGenerationRequest): Result<Any> = Result.success(mapOf("modelId" to modelId, "task" to request.task, "status" to "queued"))
    override fun cancel() = Unit
    override fun getProgress(): Float = 0f
    override fun getMemoryRequirement(): Int = detection.memoryRequirementMb
    override fun supportsHardware(hardware: DeviceHardwareProfile): Boolean = hardware.availableRamGb >= 3f
    override fun supportsInput(inputType: String): Boolean = inputType.lowercase() in setOf("text", "image")
    override fun supportsOutput(outputType: String): Boolean = outputType.lowercase() in setOf("image")
}

class GenericAudioModelAdapter(
    modelId: String,
    modelName: String,
    detection: ModelDetectionResult
) : ModelAdapter(modelId, modelName, detection) {
    override fun identify(): String = detection.architecture
    override fun validate(): Boolean = detection.family == ModelFamily.AUDIO
    override fun getCapabilities(): Set<ModelTask> = detection.supportedTasks
    override fun getRequirements(): Map<String, Any> = mapOf(
        "family" to detection.family.name,
        "backend" to detection.backend,
        "format" to detection.format.name,
        "requiresVocoder" to true,
        "memoryMB" to detection.memoryRequirementMb
    )
    override fun load(): Result<Unit> = Result.success(Unit)
    override fun unload() = Unit
    override fun generate(request: UniversalGenerationRequest): Result<Any> = Result.success(mapOf("modelId" to modelId, "task" to request.task, "status" to "queued"))
    override fun cancel() = Unit
    override fun getProgress(): Float = 0f
    override fun getMemoryRequirement(): Int = detection.memoryRequirementMb
    override fun supportsHardware(hardware: DeviceHardwareProfile): Boolean = hardware.availableRamGb >= 1.5f
    override fun supportsInput(inputType: String): Boolean = inputType.lowercase() in setOf("text", "audio")
    override fun supportsOutput(outputType: String): Boolean = outputType.lowercase() in setOf("audio", "text")
}

class GenericVisionModelAdapter(
    modelId: String,
    modelName: String,
    detection: ModelDetectionResult
) : ModelAdapter(modelId, modelName, detection) {
    override fun identify(): String = detection.architecture
    override fun validate(): Boolean = detection.family == ModelFamily.VISION
    override fun getCapabilities(): Set<ModelTask> = detection.supportedTasks
    override fun getRequirements(): Map<String, Any> = mapOf(
        "family" to detection.family.name,
        "backend" to detection.backend,
        "format" to detection.format.name,
        "requiresVisionEncoder" to true,
        "memoryMB" to detection.memoryRequirementMb
    )
    override fun load(): Result<Unit> = Result.success(Unit)
    override fun unload() = Unit
    override fun generate(request: UniversalGenerationRequest): Result<Any> = Result.success(mapOf("modelId" to modelId, "task" to request.task, "status" to "queued"))
    override fun cancel() = Unit
    override fun getProgress(): Float = 0f
    override fun getMemoryRequirement(): Int = detection.memoryRequirementMb
    override fun supportsHardware(hardware: DeviceHardwareProfile): Boolean = hardware.availableRamGb >= 2f
    override fun supportsInput(inputType: String): Boolean = inputType.lowercase() in setOf("image", "text")
    override fun supportsOutput(outputType: String): Boolean = outputType.lowercase() in setOf("text", "bounding boxes", "mask")
}

class LTXVideoModelAdapter(
    override val modelId: String,
    override val modelName: String,
    override val detection: ModelDetectionResult
) : ModelAdapter(modelId, modelName, detection) {
    override fun identify(): String = "LTX-Video"
    override fun validate(): Boolean = detection.family == ModelFamily.VIDEO_DIFFUSION && detection.architecture.contains("LTX", ignoreCase = true)
    override fun getCapabilities(): Set<ModelTask> = setOf(
        ModelTask.TEXT_TO_VIDEO,
        ModelTask.IMAGE_TO_VIDEO,
        ModelTask.VIDEO_TO_VIDEO,
        ModelTask.VIDEO_EXTENSION,
        ModelTask.VIDEO_GENERATION
    )
    override fun getRequirements(): Map<String, Any> = mapOf(
        "architecture" to detection.architecture,
        "backend" to detection.backend,
        "format" to detection.format.name,
        "memoryMB" to detection.memoryRequirementMb,
        "supportsControl" to true,
        "supportsLoRA" to true,
        "supportsKeyframeConditioning" to true,
        "supportsVideoExtension" to true
    )
    override fun load(): Result<Unit> = Result.success(Unit)
    override fun unload() = Unit
    override fun generate(request: UniversalGenerationRequest): Result<Any> = Result.success(mapOf("modelId" to modelId, "status" to "queued", "task" to request.task))
    override fun cancel() = Unit
    override fun getProgress(): Float = 0f
    override fun getMemoryRequirement(): Int = detection.memoryRequirementMb
    override fun supportsHardware(hardware: DeviceHardwareProfile): Boolean {
        val availableRamMb = (hardware.availableRamGb * 1024f).toInt()
        return hardware.gpuVulkanSupported && availableRamMb >= detection.memoryRequirementMb * 0.6f && detection.isRuntimeAvailable
    }
    override fun supportsInput(inputType: String): Boolean = inputType.lowercase() in setOf("text", "image", "video", "keyframe")
    override fun supportsOutput(outputType: String): Boolean = outputType.lowercase() in setOf("video", "mp4")
}
