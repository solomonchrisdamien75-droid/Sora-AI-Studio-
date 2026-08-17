import sys
import re

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "r") as f:
    content = f.read()

target = """            try {
                // Stream response from unified AIInferenceManager"""

new_logic = """            try {
                var activeModel = repository.inferenceEngineManager.activeLoadedModel.value
                if (activeModel == null) {
                    val downloaded = repository.aiModelDao.getAllModelsList().filter { it.isDownloaded }
                    if (downloaded.isNotEmpty()) {
                        val modelToLoad = downloaded.firstOrNull { it.modelType == "TEXT" } ?: downloaded.first()
                        repository.inferenceEngineManager.loadModel(modelToLoad)
                        activeModel = repository.inferenceEngineManager.activeLoadedModel.value
                    }
                }
                
                if (activeModel == null) {
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = "Error: No downloaded model available. Please download a TEXT model first to pull inference into RAM.") else msg
                    }
                    _isChatStreaming.value = false
                    _isAssistantLoading.value = false
                    return@launch
                }

                // Stream response from unified AIInferenceManager"""

content = content.replace(target, new_logic)

with open("app/src/main/java/com/example/ui/SoraMainViewModel.kt", "w") as f:
    f.write(content)
