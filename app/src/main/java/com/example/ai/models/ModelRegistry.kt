package com.example.ai.models

import android.content.Context
import com.example.ai.hardware.DeviceHardwareProfile
import com.example.ai.hardware.HardwareDetector
import com.example.ai.inference.model.ModelCapability
import com.example.ai.inference.model.ModelCapabilityDetector
import com.example.ai.inference.model.ModelCompatibilityResult
import com.example.data.AiModelEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ModelRegistry manages locally installed and available models, stores metadata
 * (format, capabilities, requirements), and provides an API for AIInferenceManager
 * to check hardware and capability compatibility.
 */
class ModelRegistry(private val context: Context) {

    private val hardwareDetector = HardwareDetector(context)
    private val _registeredModels = MutableStateFlow<List<AiModelEntity>>(emptyList())
    val registeredModels: StateFlow<List<AiModelEntity>> = _registeredModels.asStateFlow()

    private val _modelProfiles = mutableMapOf<String, ModelCapabilityProfile>()

    init {
        // Register default built-in local engine descriptors
        initializeDefaultProfiles()
    }

    private fun initializeDefaultProfiles() {
        // Register built-in default capability profiles
        val defaultProfiles = listOf(
            ModelCapabilityProfile(
                modelId = "sora-mobile-v1",
                modelName = "Sora Mobile Diffusion v1",
                format = "LITERET",
                architecture = "Diffusion-DiT",
                backend = "LiteRT/Vulkan",
                videoGeneration = true,
                textToVideo = true,
                imageToVideo = true,
                videoToVideo = true,
                minimumRamMb = 3072,
                recommendedRamMb = 6144,
                gpuSupported = true,
                vulkanSupported = true
            ),
            ModelCapabilityProfile(
                modelId = "qwen2.5-1.5b-instruct",
                modelName = "Qwen2.5 1.5B Instruct",
                format = "GGUF",
                architecture = "Qwen2.5",
                backend = "Llama.cpp",
                textGeneration = true,
                chat = true,
                minimumRamMb = 2048,
                recommendedRamMb = 4096,
                cpuSupported = true,
                gpuSupported = true
            ),
            ModelCapabilityProfile(
                modelId = "whisper-tiny-en",
                modelName = "Whisper Tiny English",
                format = "ONNX",
                architecture = "Whisper",
                backend = "ONNX Runtime",
                speechToText = true,
                minimumRamMb = 1024,
                recommendedRamMb = 2048
            ),
            ModelCapabilityProfile(
                modelId = "kokoro-tts-v1",
                modelName = "Kokoro 82M Neural TTS",
                format = "ONNX",
                architecture = "Kokoro",
                backend = "ONNX Runtime",
                textToSpeech = true,
                minimumRamMb = 1024,
                recommendedRamMb = 2048
            )
        )

        defaultProfiles.forEach { profile ->
            _modelProfiles[profile.modelId] = profile
        }
    }

    /**
     * Registers or updates a model in the registry.
     */
    fun registerModel(model: AiModelEntity) {
        val currentList = _registeredModels.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == model.id }
        if (index >= 0) {
            currentList[index] = model
        } else {
            currentList.add(model)
        }
        _registeredModels.value = currentList

        // Resolve and cache capability profile
        val profile = ModelCapabilityRegistry.resolveCapabilities(
            modelId = model.id,
            name = model.name,
            format = model.format,
            modelType = model.modelType,
            fileSizeBytes = model.sizeBytes
        )
        _modelProfiles[model.id] = profile
    }

    /**
     * Syncs a batch of models from storage or database into the registry.
     */
    fun syncModels(models: List<AiModelEntity>) {
        _registeredModels.value = models
        models.forEach { model ->
            val profile = ModelCapabilityRegistry.resolveCapabilities(
                modelId = model.id,
                name = model.name,
                format = model.format,
                modelType = model.modelType,
                fileSizeBytes = model.sizeBytes
            )
            _modelProfiles[model.id] = profile
        }
    }

    /**
     * Retrieves the capability profile for a given model ID.
     */
    fun getModelProfile(modelId: String): ModelCapabilityProfile? {
        return _modelProfiles[modelId]
    }

    /**
     * Returns all models supporting a specific capability.
     */
    fun getModelsByCapability(capability: ModelCapability): List<AiModelEntity> {
        return _registeredModels.value.filter { model ->
            val detected = ModelCapabilityDetector.detectCapabilities(model)
            detected.contains(capability)
        }
    }

    /**
     * Hardware compatibility verification API used by AIInferenceManager.
     * Evaluates RAM, GPU/Vulkan support, and model architecture requirements.
     */
    fun checkHardwareCompatibility(
        model: AiModelEntity,
        hardwareProfile: DeviceHardwareProfile = hardwareDetector.getDeviceProfile()
    ): HardwareCompatibilityReport {
        val requiredRamMb = model.ramRequiredMb
        val availableRamMb = (hardwareProfile.availableRamGb * 1024).toInt()
        val totalRamMb = (hardwareProfile.totalRamGb * 1024).toInt()

        val isRamSufficient = availableRamMb >= (requiredRamMb * 0.7f) || totalRamMb >= requiredRamMb
        val isGpuCompatible = if (model.backend?.contains("Vulkan", ignoreCase = true) == true || model.backend?.contains("GPU", ignoreCase = true) == true) {
            hardwareProfile.gpuVulkanSupported || hardwareProfile.nnapiSupported
        } else {
            true
        }

        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (!isRamSufficient) {
            issues.add("Insufficient RAM: Model requires ~${requiredRamMb}MB, but device has ${availableRamMb}MB free.")
            recommendations.add("Consider quantizing model to Q4_K_M or INT4 to reduce RAM requirement to ~${requiredRamMb / 2}MB.")
        }

        if (!isGpuCompatible) {
            issues.add("GPU/Vulkan acceleration is not supported on this device for ${model.backend ?: "Vulkan"}.")
            recommendations.add("Fallback to CPU multi-threading (${hardwareProfile.cpuCores} cores available).")
        }

        val score = when {
            isRamSufficient && isGpuCompatible -> 100
            isRamSufficient -> 75
            availableRamMb >= (requiredRamMb * 0.5f) -> 50
            else -> 25
        }

        return HardwareCompatibilityReport(
            modelId = model.id,
            modelName = model.name,
            isCompatible = isRamSufficient,
            compatibilityScore = score,
            requiredRamMb = requiredRamMb,
            availableRamMb = availableRamMb,
            recommendedBackend = if (isGpuCompatible && hardwareProfile.gpuVulkanSupported) "Vulkan GPU" else "Multi-Threaded CPU",
            issues = issues,
            recommendations = recommendations
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ModelRegistry? = null

        fun getInstance(context: Context): ModelRegistry {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelRegistry(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

data class HardwareCompatibilityReport(
    val modelId: String,
    val modelName: String,
    val isCompatible: Boolean,
    val compatibilityScore: Int, // 0 - 100
    val requiredRamMb: Int,
    val availableRamMb: Int,
    val recommendedBackend: String,
    val issues: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)
