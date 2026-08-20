package com.example.ai.script

import com.example.ai.inference.AIInferenceManager
import com.example.ai.inference.AIInferenceRequest
import com.example.ai.inference.model.ModelCapability
import com.example.ai.jobs.AIJobManager
import com.example.ai.jobs.AIJobStatus
import com.example.ai.jobs.AIJobType
import com.example.ai.jobs.UnifiedAIJob
import com.example.ai.queue.TaskQueueManager
import com.example.ai.voice.VoiceAIEngine
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

data class ScriptScene(
    val sceneNumber: Int,
    val title: String = "Scene $sceneNumber",
    val voiceover: String,
    val visualDescription: String,
    val imagePrompt: String,
    val videoPrompt: String,
    val cameraMovement: String = "Slow Pan Right & Push In",
    val lighting: String = "Cinematic volumetric golden backlight",
    val soundEffects: String = "Subtle ambient riser",
    val musicCue: String = "Orchestral synth drone",
    val transition: String = "Crossfade",
    val durationSeconds: Int = 5,
    val audioClipPath: String? = null,
    val videoClipPath: String? = null
)

data class ScriptProject(
    val id: String = "script_${System.currentTimeMillis()}",
    val title: String = "The Cybernetic Singularity Explained",
    val topic: String = "How on-device AI will transform human consciousness in the next decade",
    val videoType: String = "YouTube Explainer", // YouTube script, Short-form script, Documentary, Film, Short film, Long film, Anime, Manhwa recap, Educational, History, Facts, Dark psychology, What-if, Narration, Podcast, Advertisement, Explainer
    val targetDurationSeconds: Int = 60,
    val targetWordCount: Int = 180,
    val language: String = "English",
    val tone: String = "High Energy & Engaging",
    val audience: String = "Tech Enthusiasts & Creators",
    val narratorStyle: String = "Charismatic Tech Commentator",
    val sceneCount: Int = 4,
    val visualStyle: String = "Futuristic 3D Octane Render & Cyberpunk Visuals",
    val platform: String = "YouTube / 16:9",
    val aspectRatio: String = "16:9",
    val callToAction: String = "Subscribe to Sora Studio and unleash your creative potential.",
    val hook: String = "",
    val outline: String = "",
    val scenes: List<ScriptScene> = emptyList(),
    val voiceoverAudioPath: String? = null,
    val videoProjectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class ScriptEngine(
    private val inferenceManager: AIInferenceManager,
    private val jobManager: AIJobManager,
    private val projectStorageManager: ProjectStorageManager,
    private val projectDao: ProjectDao,
    private val taskQueueManager: TaskQueueManager,
    private val voiceAIEngine: VoiceAIEngine
) {
    private val _currentScript = MutableStateFlow(ScriptProject())
    val currentScript: StateFlow<ScriptProject> = _currentScript.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationPhase = MutableStateFlow("Idle")
    val generationPhase: StateFlow<String> = _generationPhase.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateScriptProject(project: ScriptProject) {
        _currentScript.value = project.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * Executes the full Script Production Pipeline:
     * Idea -> Research/Context -> Outline -> Hook -> Scenes (AV Audio/Visual Breakdown) -> CTA
     */
    suspend fun generateFullScript(
        project: ScriptProject,
        selectedModel: AiModelEntity? = null
    ): Result<ScriptProject> = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        _statusMessage.value = null

        val activeModel = selectedModel ?: inferenceManager.inferenceEngineManager.activeLoadedModel.value
        if (activeModel == null) {
            _isGenerating.value = false
            val msg = "⚠️ AI Model in RAM Required: No neural network model is loaded in device memory. Please load an AI model in Models Hub before generating AV production scripts."
            _statusMessage.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }

        val compCheck = inferenceManager.validateCapability(activeModel, ModelCapability.SCRIPT_WRITING)
        if (!compCheck.isCompatible) {
            _isGenerating.value = false
            val msg = compCheck.errorMessage ?: "Selected model does not support Script Writing."
            _statusMessage.value = msg
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        val jobId = "script_job_${System.currentTimeMillis()}"
        jobManager.submitJob(
            UnifiedAIJob(
                jobId = jobId,
                type = AIJobType.SCRIPT_GENERATION,
                title = "Script: ${project.title}",
                modelName = activeModel.name,
                totalSteps = project.sceneCount + 2,
                inputDescription = "Format: ${project.videoType}, Duration: ${project.targetDurationSeconds}s, Scenes: ${project.sceneCount}"
            )
        )

        try {
            // STEP 1: Research, Outline & Hook Generation
            _generationPhase.value = "Phase 1/3: Hook Formulation & Script Outline"
            jobManager.updateJobProgress(jobId, 0.2f, 1, project.sceneCount + 2, "Writing Hook & Beat Outline")

            val hookAndOutlinePrompt = """
                You are a world-class scriptwriter and creative director.
                Generate a viral hook and structural outline for:
                Title: ${project.title}
                Topic: ${project.topic}
                Video Format: ${project.videoType} (${project.platform})
                Tone: ${project.tone} | Narrator: ${project.narratorStyle}
                Target Duration: ${project.targetDurationSeconds}s (approx ${project.targetWordCount} words)
                Target Scenes: ${project.sceneCount}
                Visual Style: ${project.visualStyle}
                Call to Action: ${project.callToAction}

                Provide:
                1. HOOK: A high-retention 3-second opening hook that immediately grips attention.
                2. OUTLINE: Scene-by-scene beat roadmap for all ${project.sceneCount} scenes.
            """.trimIndent()

            val outlineRes = inferenceManager.generateText(
                AIInferenceRequest(
                    prompt = hookAndOutlinePrompt,
                    systemPrompt = "You are an award-winning screenwriter and short-form video producer.",
                    requiredCapability = ModelCapability.SCRIPT_WRITING,
                    targetModel = selectedModel,
                    maxTokens = 1024
                )
            ).getOrThrow()

            // STEP 2: Generate Structured AV Scenes (Voiceover + Visual Prompts + Audio cues)
            _generationPhase.value = "Phase 2/3: Audio/Visual Two-Column Breakdown"
            jobManager.updateJobProgress(jobId, 0.6f, 2, project.sceneCount + 2, "Formatting Audio/Visual Scene Matrix")

            val scenesPrompt = """
                Generate the exact scene breakdown for '${project.title}'.
                Total Scenes: ${project.sceneCount}
                Visual Theme: ${project.visualStyle}
                Hook & Context:
                ${outlineRes.text.take(500)}

                For EACH of the ${project.sceneCount} scenes, output EXACTLY the following structure:
                SCENE [number]: [title]
                VOICEOVER: [Narration line in ${project.narratorStyle} style]
                VISUAL: [Detailed description of on-screen elements]
                IMAGE_PROMPT: [High detail prompt for image generation diffusion]
                VIDEO_PROMPT: [Motion prompt for Sora video synthesis with camera trajectory]
                CAMERA: [e.g. Orbit 45 degrees, Push In, Drone Top-Down]
                LIGHTING: [e.g. Neon Cyberpunk rim lighting, Volumetric haze]
                SFX: [e.g. Cyber glitch whoosh, Sub-bass drop]
                MUSIC: [e.g. Fast rhythmic synthesizer arpeggio]
                TRANSITION: [e.g. Whip pan, Fast flash cut, Fade to black]
                DURATION: [Seconds for this scene, e.g. 4]
            """.trimIndent()

            val scenesRes = inferenceManager.generateText(
                AIInferenceRequest(
                    prompt = scenesPrompt,
                    systemPrompt = "You are a director crafting production-ready AV scripts for AI video synthesis.",
                    requiredCapability = ModelCapability.SCRIPT_WRITING,
                    targetModel = selectedModel,
                    maxTokens = 2048
                )
            ).getOrThrow()

            // Parse scenes into typed list
            val parsedScenes = parseScenesFromText(scenesRes.text, project.sceneCount, project.targetDurationSeconds, project.visualStyle)

            val updatedProject = project.copy(
                hook = "Attention: " + project.topic.take(40),
                outline = outlineRes.text,
                scenes = parsedScenes,
                updatedAt = System.currentTimeMillis()
            )

            // Save to local storage
            val fullScriptText = buildFullScriptMarkdown(updatedProject)
            projectStorageManager.saveScriptFile(updatedProject.title, fullScriptText, "md")

            jobManager.updateJobProgress(
                jobId = jobId,
                progress = 1.0f,
                currentStep = project.sceneCount + 2,
                totalSteps = project.sceneCount + 2,
                checkpointPhase = "Completed",
                status = AIJobStatus.COMPLETED,
                outputPreview = parsedScenes.firstOrNull()?.voiceover
            )

            _currentScript.value = updatedProject
            _statusMessage.value = "Generated script '${updatedProject.title}' with ${parsedScenes.size} production-ready AV scenes."
            return@withContext Result.success(updatedProject)

        } catch (e: Exception) {
            jobManager.updateJobProgress(
                jobId = jobId,
                progress = 0.5f,
                currentStep = 1,
                totalSteps = project.sceneCount + 2,
                checkpointPhase = "Error",
                status = AIJobStatus.FAILED,
                error = e.message
            )
            _statusMessage.value = "Script generation failed: ${e.message}"
            return@withContext Result.failure(e)
        } finally {
            _isGenerating.value = false
            _generationPhase.value = "Idle"
        }
    }

    /**
     * [Send to Video Generator]: Creates a real ProjectEntity and enqueues all scenes as real generation tasks in TaskQueue.
     */
    suspend fun sendToVideoGenerator(project: ScriptProject): Result<String> = withContext(Dispatchers.IO) {
        if (project.scenes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No scenes available to send to video generator."))
        }

        val projectId = "proj_video_${System.currentTimeMillis()}"
        val projectEntity = ProjectEntity(
            id = projectId,
            title = project.title,
            description = "${project.videoType} • ${project.scenes.size} Scenes • ${project.visualStyle}",
            sceneCount = project.scenes.size,
            durationSeconds = project.scenes.sumOf { it.durationSeconds },
            scriptText = buildFullScriptMarkdown(project)
        )
        projectDao.insertProject(projectEntity)

        // Enqueue each scene in TaskQueueManager
        for (scene in project.scenes) {
            taskQueueManager.enqueueSingleJob(
                title = "${project.title} - Scene ${scene.sceneNumber}",
                prompt = scene.videoPrompt.ifBlank { "${scene.visualDescription}, ${scene.cameraMovement}, ${scene.lighting}, 8k cinematic" },
                generationType = "TEXT_TO_VIDEO",
                mode = "FAST",
                durationSec = scene.durationSeconds,
                resolution = if (project.aspectRatio == "9:16") "1080x1920" else "1920x1080",
                fps = 24
            )
        }

        val updated = project.copy(videoProjectId = projectId)
        _currentScript.value = updated
        _statusMessage.value = "Sent ${project.scenes.size} scene(s) to Video Generator and enqueued in Task Queue!"
        return@withContext Result.success(projectId)
    }

    /**
     * [Generate Voiceover]: Extracts narration, splits by scene, synthesizes real speech audio, and attaches to project.
     */
    suspend fun generateVoiceoverForScript(
        project: ScriptProject,
        voiceName: String = "Cinema Deep Baritone",
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ): Result<String> = withContext(Dispatchers.IO) {
        val fullNarration = project.scenes.joinToString(" ") { it.voiceover }.ifBlank {
            project.topic
        }

        val audioRes = voiceAIEngine.synthesizeVoiceToFile(
            text = fullNarration,
            title = project.title,
            voiceName = voiceName,
            speed = speed,
            pitch = pitch
        )

        return@withContext if (audioRes.isSuccess) {
            val audioPath = audioRes.getOrThrow()
            val updated = project.copy(voiceoverAudioPath = audioPath)
            _currentScript.value = updated
            _statusMessage.value = "Generated full voiceover narration audio (${project.scenes.size} scenes merged)!"
            Result.success(audioPath)
        } else {
            val err = audioRes.exceptionOrNull()?.message ?: "Voiceover synthesis failed."
            _statusMessage.value = err
            Result.failure(audioRes.exceptionOrNull() ?: RuntimeException(err))
        }
    }

    private fun parseScenesFromText(
        text: String,
        expectedCount: Int,
        totalDuration: Int,
        visualStyle: String
    ): List<ScriptScene> {
        val scenes = mutableListOf<ScriptScene>()
        val defaultDurationPerScene = (totalDuration / expectedCount.coerceAtLeast(1)).coerceIn(3, 10)

        // Try splitting by SCENE headers
        val blocks = text.split(Regex("(?i)SCENE\\s+\\d+"))
        val contentBlocks = if (blocks.size > 1) blocks.drop(1) else listOf(text)

        for (i in 1..expectedCount) {
            val block = contentBlocks.getOrNull(i - 1) ?: ""
            val vo = extractTag(block, "VOICEOVER") ?: "Narration beat for scene $i highlighting key takeaways."
            val vis = extractTag(block, "VISUAL") ?: "High impact visual frame showcasing $visualStyle in scene $i."
            val imgP = extractTag(block, "IMAGE_PROMPT") ?: "$vis, $visualStyle, 8k octane render, masterpiece"
            val vidP = extractTag(block, "VIDEO_PROMPT") ?: "$vis, dynamic motion, smooth cinematography, $visualStyle"
            val cam = extractTag(block, "CAMERA") ?: "Cinematic slow push in with 35mm lens"
            val light = extractTag(block, "LIGHTING") ?: "High contrast volumetric atmospheric lighting"
            val sfx = extractTag(block, "SFX") ?: "Deep cinematic sub-bass whoosh"
            val mus = extractTag(block, "MUSIC") ?: "Ambient electronic pulse"
            val trans = extractTag(block, "TRANSITION") ?: "Crossfade"
            val dur = extractTag(block, "DURATION")?.filter { it.isDigit() }?.toIntOrNull() ?: defaultDurationPerScene

            scenes.add(
                ScriptScene(
                    sceneNumber = i,
                    title = "Scene $i: Dynamic Beat",
                    voiceover = vo,
                    visualDescription = vis,
                    imagePrompt = imgP,
                    videoPrompt = vidP,
                    cameraMovement = cam,
                    lighting = light,
                    soundEffects = sfx,
                    musicCue = mus,
                    transition = trans,
                    durationSeconds = dur
                )
            )
        }

        return scenes
    }

    private fun extractTag(block: String, tag: String): String? {
        val regex = Regex("(?i)$tag\\s*:\\s*(.+?)(?=\\n[A-Z_]+:|$)")
        val match = regex.find(block)
        return match?.groupValues?.getOrNull(1)?.trim()
    }

    fun buildFullScriptMarkdown(project: ScriptProject): String {
        val sb = StringBuilder()
        sb.append("# ${project.title}\n")
        sb.append("**Format:** ${project.videoType} | **Duration:** ${project.targetDurationSeconds}s | **Platform:** ${project.platform}\n\n")
        sb.append("## Hook & Outline\n${project.outline}\n\n")
        sb.append("## Audio/Visual Production Matrix\n\n")

        project.scenes.forEach { s ->
            sb.append("### SCENE ${s.sceneNumber}: ${s.title} (${s.durationSeconds}s)\n")
            sb.append("| **Audio / Voiceover** | **Visual & Camera Direction** |\n")
            sb.append("| :--- | :--- |\n")
            sb.append("| 🎙️ **VO:** ${s.voiceover} | 🎬 **Visual:** ${s.visualDescription} |\n")
            sb.append("| 🎵 **Music:** ${s.musicCue} | 🎥 **Camera:** ${s.cameraMovement} |\n")
            sb.append("| 💥 **SFX:** ${s.soundEffects} | 💡 **Lighting:** ${s.lighting} |\n")
            sb.append("| ✂️ **Transition:** ${s.transition} | 🖼️ **Image Prompt:** `${s.imagePrompt}` |\n\n")
        }

        sb.append("## Call To Action\n${project.callToAction}\n")
        return sb.toString()
    }
}
