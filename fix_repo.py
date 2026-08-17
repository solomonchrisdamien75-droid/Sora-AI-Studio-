import sys
import re

with open("app/src/main/java/com/example/data/SoraRepository.kt", "r") as f:
    content = f.read()

target = """    fun startLocalGenerationStream(job: GenerationJobEntity): Flow<InferenceProgress> {
        val activeModel = inferenceEngineManager.activeLoadedModel.value ?: AiModelEntity(
            id = "active_model",
            name = "Sora Engine",
            modelType = "VIDEO",
            format = if (job.mode == "BALANCED") "ONNX" else "LITERET",
            sizeBytes = 1_000_000_000L,
            ramRequiredMb = 2000,
            isDownloaded = true
        )
        val engine = inferenceEngineManager.selectEngineForModel(activeModel)
        return engine.generateVideoFrames(
            prompt = job.prompt,
            width = 1080,
            height = 1920,
            fps = job.fps.toInt(),
            durationSec = job.durationSeconds,
            onFrameRendered = { _, _, _ -> }
        )
    }"""

new_stream_logic = """    suspend fun startLocalGenerationStream(job: GenerationJobEntity): Flow<InferenceProgress> {
        var activeModel = inferenceEngineManager.activeLoadedModel.value
        if (activeModel == null) {
            val downloaded = aiModelDao.getAllModelsList().filter { it.isDownloaded }
            if (downloaded.isNotEmpty()) {
                val modelToLoad = downloaded.firstOrNull { it.modelType == "VIDEO" || it.modelType == "IMAGE" } ?: downloaded.first()
                inferenceEngineManager.loadModel(modelToLoad)
                activeModel = inferenceEngineManager.activeLoadedModel.value
            }
        }
        
        if (activeModel == null) {
            throw IllegalStateException("No downloaded model available. Please download a model first to pull inference into RAM.")
        }

        val engine = inferenceEngineManager.selectEngineForModel(activeModel)
        return engine.generateVideoFrames(
            prompt = job.prompt,
            width = 1080,
            height = 1920,
            fps = job.fps.toInt(),
            durationSec = job.durationSeconds,
            onFrameRendered = { _, _, _ -> }
        )
    }"""

content = content.replace(target, new_stream_logic)

with open("app/src/main/java/com/example/data/SoraRepository.kt", "w") as f:
    f.write(content)
