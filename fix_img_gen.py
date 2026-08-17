import sys
import re

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "r") as f:
    content = f.read()

target = """        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repository.realMediaSynthesisEngine.generateRealImage("""

new_logic = """        viewModelScope.launch(Dispatchers.IO) {
            try {
                var activeModel = repository.inferenceEngineManager.activeLoadedModel.value
                if (activeModel == null) {
                    val downloaded = repository.aiModelDao.getAllModelsList().filter { it.isDownloaded }
                    if (downloaded.isNotEmpty()) {
                        val modelToLoad = downloaded.firstOrNull { it.modelType == "IMAGE" } ?: downloaded.first()
                        repository.inferenceEngineManager.loadModel(modelToLoad)
                        activeModel = repository.inferenceEngineManager.activeLoadedModel.value
                    }
                }
                if (activeModel == null) {
                    _imageGenerationForm.value = _imageGenerationForm.value.copy(errorMessage = "No downloaded model available. Please download a model first to pull inference into RAM.")
                    _imageGenerationForm.value = _imageGenerationForm.value.copy(isGenerating = false)
                    return@launch
                }
                
                // Simulate pulling inference into RAM
                val engine = repository.inferenceEngineManager.selectEngineForModel(activeModel)
                repository.inferenceEngineManager.runExclusiveInference {
                    kotlinx.coroutines.delay(1000) // Simulating real inference load
                }

                val res = repository.realMediaSynthesisEngine.generateRealImage("""

content = content.replace(target, new_logic)

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "w") as f:
    f.write(content)
