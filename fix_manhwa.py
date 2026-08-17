import sys
import re

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "r") as f:
    content = f.read()

target = """    fun generateManhwaStoryContinuation() {
        val form = _generationForm.value
        _generationForm.value = form.copy(isGeneratingManhwaContinuation = true)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val script = \"\"\"
            📜 MANHWA STORY CONTINUATION: CHAPTER 43 (AI GENERATED)
            Title: The Spectral Sovereign's March
            
            [PANEL 1 - ACTION]
            Visual: Dark clouds gather over Namsan Tower. The Shadow Monarch lifts his sword.
            Dialogue (Shadow Monarch): "The eternal eclipse begins."
            
            [PANEL 2 - REACTION]
            Visual: Hunters in the distance tremble. A blue system screen pops up.
            System Prompt: [WARNING! S-Rank Gate Break in progress.]
            
            [PANEL 3 - DYNAMIC]
            Visual: A massive shadow dragon erupts from the ground, shattering the city streets.
            SFX: KRAAAAAK!
            \"\"\".trimIndent()

            _generationForm.value = form.copy(
                prompt = form.prompt + "\n\n" + script,
                isGeneratingManhwaContinuation = false
            )
        }
    }"""

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
    }"""

content = content.replace(target, new_logic)

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "w") as f:
    f.write(content)
