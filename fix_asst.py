import sys

with open("app/src/main/java/com/example/ai/assistant/OfflineAssistantEngine.kt", "r") as f:
    content = f.read()

import re

# Update imports
content = content.replace("import com.example.ai.inference.LlamaCppEngine", "import com.example.ai.inference.InferenceEngineManager")

# Update constructor
content = content.replace("class OfflineAssistantEngine(\n    private val context: Context,\n    private val aiModelDao: AiModelDao\n) {", "class OfflineAssistantEngine(\n    private val context: Context,\n    private val aiModelDao: AiModelDao,\n    private val inferenceEngineManager: InferenceEngineManager\n) {")

# Remove private val llamaCppEngine
content = content.replace("    private val llamaCppEngine = LlamaCppEngine(context)\n", "")

# Replace generation logic
target = """        val rawText = if (downloadedTextModel != null) {
            llamaCppEngine.loadModel(downloadedTextModel)
            llamaCppEngine.generateText("Write a movie script and shot breakdown for: $userConcept")
        } else {
            generateOfflineTemplateScript(userConcept)
        }"""

new_logic = """        val rawText = if (downloadedTextModel != null) {
            inferenceEngineManager.loadModel(downloadedTextModel)
            inferenceEngineManager.runExclusiveInference { engine ->
                engine.generateText("Write a movie script and shot breakdown for: $userConcept")
            }
        } else {
            "Error: No downloaded model available for text inference. Please download a TEXT model to generate."
        }"""

content = content.replace(target, new_logic)

with open("app/src/main/java/com/example/ai/assistant/OfflineAssistantEngine.kt", "w") as f:
    f.write(content)
