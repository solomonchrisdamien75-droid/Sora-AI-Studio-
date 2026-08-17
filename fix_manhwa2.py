import sys

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "r") as f:
    content = f.read()

target_start = "    fun generateManhwaStoryContinuation() {"
target_end = "    fun clearManhwaProject() {"

start_idx = content.find(target_start)
end_idx = content.find(target_end, start_idx)

if start_idx != -1 and end_idx != -1:
    new_logic = """    fun generateManhwaStoryContinuation() {
        val form = _generationForm.value
        _generationForm.value = form.copy(isGeneratingManhwaContinuation = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var activeModel = repository.inferenceEngineManager.activeLoadedModel.value
                if (activeModel == null) {
                    val downloaded = repository.aiModelDao.getAllModelsList().filter { it.isDownloaded }
                    if (downloaded.isNotEmpty()) {
                        val modelToLoad = downloaded.firstOrNull { it.modelType == "TEXT" } ?: downloaded.first()
                        repository.inferenceEngineManager.loadModel(modelToLoad)
                        activeModel = repository.inferenceEngineManager.activeLoadedModel.value
                    }
                }
                
                val script = if (activeModel != null) {
                    repository.inferenceEngineManager.runExclusiveInference { engine ->
                        engine.generateText(prompt = "Continue Manhwa Story: ${form.prompt}", maxTokens = 500)
                    }
                } else {
                    "Error: No downloaded model available for text inference."
                }

                _generationForm.value = form.copy(
                    prompt = form.prompt + "\n\n" + script,
                    isGeneratingManhwaContinuation = false
                )
            } catch (e: Exception) {
                _generationForm.value = form.copy(isGeneratingManhwaContinuation = false)
            }
        }
    }

"""
    content = content[:start_idx] + new_logic + content[end_idx:]
    with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "w") as f:
        f.write(content)
