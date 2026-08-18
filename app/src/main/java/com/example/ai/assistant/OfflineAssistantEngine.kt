package com.example.ai.assistant

import android.content.Context
import com.example.ai.inference.InferenceEngineManager
import com.example.data.AiModelDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class StoryboardShot(
    val shotNumber: Int,
    val title: String,
    val cameraMovement: String,
    val lighting: String,
    val promptText: String,
    val estimatedDurationSec: Int
)

data class ScriptProductionPackage(
    val title: String,
    val logline: String,
    val genre: String,
    val scriptText: String,
    val shots: List<StoryboardShot>,
    val musicStyle: String,
    val soundEffects: List<String>,
    val youtubeTags: List<String>
)

class OfflineAssistantEngine(
    private val context: Context,
    private val aiModelDao: AiModelDao,
    private val inferenceEngineManager: InferenceEngineManager
) {

    suspend fun generateScriptAndShots(userConcept: String): ScriptProductionPackage = withContext(Dispatchers.IO) {
        var activeModel = inferenceEngineManager.activeLoadedModel.value
        if (activeModel == null) {
            val downloaded = aiModelDao.getAllModelsList().filter { it.isDownloaded }
            if (downloaded.isNotEmpty()) {
                val modelToLoad = downloaded.firstOrNull { it.modelType == "TEXT" } ?: downloaded.first()
                inferenceEngineManager.loadModel(modelToLoad)
                activeModel = inferenceEngineManager.activeLoadedModel.value
            }
        }

        val rawText = if (activeModel != null) {
            val engine = inferenceEngineManager.selectEngineForModel(activeModel)
            if (!engine.isLoaded() || engine.getActiveModel()?.id != activeModel.id) {
                inferenceEngineManager.loadModel(activeModel)
            }
            inferenceEngineManager.runExclusiveInference {
                it.generateText("Write a movie script and shot breakdown for: $userConcept")
            }
        } else {
            "Error: No downloaded model in device RAM. Please download a model first to run local inference."
        }

        val title = userConcept.take(24).ifBlank { "Neo Sora Vision" }.uppercase()
        val shots = listOf(
            StoryboardShot(
                shotNumber = 1,
                title = "Opening Wide Establishing Shot",
                cameraMovement = "Slow forward drone push in, 35mm lens",
                lighting = "Volumetric cyan atmospheric haze, golden backlight",
                promptText = "Cinematic wide establishing shot of $userConcept, volumetric cyan haze, hyperrealistic 8k octane render, 35mm lens",
                estimatedDurationSec = 4
            ),
            StoryboardShot(
                shotNumber = 2,
                title = "Character Focus & Detail",
                cameraMovement = "Medium eye-level track with dynamic depth of field",
                lighting = "Neon purple key light with soft rim fill",
                promptText = "Detailed medium portrait shot related to $userConcept, dramatic neon purple key lighting, sharp bokeh focus",
                estimatedDurationSec = 5
            ),
            StoryboardShot(
                shotNumber = 3,
                title = "Action Climax & Transition",
                cameraMovement = "Low angle whip pan reveal",
                lighting = "High contrast chiaroscuro flash",
                promptText = "Dynamic action sequence of $userConcept, high contrast lightning, speed blur particle effects, ultra-detailed",
                estimatedDurationSec = 3
            )
        )

        return@withContext ScriptProductionPackage(
            title = "Sora Studio - $title",
            logline = "An epic cinematic journey exploring $userConcept rendered with on-device Sora AI.",
            genre = "Sci-Fi / Cinematic",
            scriptText = rawText,
            shots = shots,
            musicStyle = "Synthesizer Cyberpunk / Orchestral Hybrid",
            soundEffects = listOf("Deep bass drone", "Futuristic riser", "Camera shutter click", "Sub-bass pulse"),
            youtubeTags = listOf("#SoraAI", "#AIVideo", "#OfflineAI", "#ShortFilm", "#Cyberpunk", "#3DCinematic")
        )
    }

    private fun generateOfflineTemplateScript(concept: String): String {
        return """
            SCENE 1 - EXT. SORA REALM - NIGHT
            
            Neon reflections ripple across metallic glass. A mysterious figure stands amidst glowing atmospheric haze.
            
            SORA OPERATOR (V.O.)
            "In a world powered by local neural synthesis, imagination has no latency."
            
            Concept Focus: $concept
            
            [SHOT 1: Wide push-in across glowing metropolis]
            [SHOT 2: Medium character focus with high contrast lighting]
            [SHOT 3: Fast orbit turn into bright horizon transition]
        """.trimIndent()
    }
}
