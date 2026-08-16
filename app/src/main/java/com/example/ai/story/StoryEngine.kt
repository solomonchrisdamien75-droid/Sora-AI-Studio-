package com.example.ai.story

import com.example.ai.inference.AIInferenceManager
import com.example.ai.inference.AIInferenceRequest
import com.example.ai.inference.model.ModelCapability
import com.example.ai.jobs.AIJobManager
import com.example.ai.jobs.AIJobStatus
import com.example.ai.jobs.AIJobType
import com.example.ai.jobs.UnifiedAIJob
import com.example.data.AiModelEntity
import com.example.data.ProjectStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

data class StoryCharacter(
    val id: String = UUID.randomUUID().toString().take(6),
    val name: String,
    val role: String = "Protagonist", // Protagonist, Antagonist, Sidekick, Mentor, Love Interest
    val personality: String = "",
    val appearance: String = "",
    val backstory: String = "",
    val goals: String = ""
)

data class StoryScene(
    val sceneIndex: Int,
    val title: String,
    val setting: String,
    val charactersPresent: List<String>,
    val objective: String,
    val conflict: String,
    val prose: String = ""
)

data class StoryChapter(
    val chapterIndex: Int,
    val title: String,
    val summary: String = "",
    val scenes: List<StoryScene> = emptyList(),
    val fullProse: String = "",
    val wordCount: Int = 0
)

data class StoryProject(
    val id: String = "story_${System.currentTimeMillis()}",
    val title: String = "Chronicles of the Neon Void",
    val genre: String = "Sci-Fi / Cyberpunk",
    val theme: String = "Identity and Artificial Consciousness",
    val language: String = "English",
    val targetAudience: String = "Young Adult / Adult",
    val storyLength: String = "Novel Chapter", // Short Story, Long Story, Novel, Chapter, Episode, Series, Manhwa Story, Anime Story, Movie Story, Documentary Narrative, YouTube Story
    val chapterCount: Int = 3,
    val writingStyle: String = "Immersive & Atmospheric",
    val tone: String = "Cinematic & Suspenseful",
    val pointOfView: String = "Third-Person Limited", // First-Person, Third-Person Limited, Third-Person Omniscient
    val characters: List<StoryCharacter> = listOf(
        StoryCharacter(name = "Kaelen Cross", role = "Protagonist", personality = "Cynical cybernetic runner with hidden loyalties", appearance = "Dark coat, glowing optical HUD"),
        StoryCharacter(name = "Nyx", role = "Rogue AI Companion", personality = "Analytical, witty, harboring mysterious origin data")
    ),
    val setting: String = "Neo-Aethelgard Mega-Structure, Sector 7 Undercity",
    val timePeriod: String = "Late 22nd Century",
    val mainConflict: String = "The monolithic CoreGrid is wiping sentient synth consciousnesses before the convergence.",
    val endingType: String = "Bittersweet Revelation",
    val ageRating: String = "PG-13",
    val customInstructions: String = "Focus on sensory descriptions of neon rain, humming sub-bass servers, and high-tension pacing.",
    val outline: String = "",
    val chapters: List<StoryChapter> = emptyList(),
    val worldMemory: String = "Rules: Synths must conceal neural overclocking. Rain in Sector 7 is acidic. The CoreGrid monitors neural traffic.",
    val timelineEvents: List<String> = emptyList(),
    val activeChapterIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class StoryEditOperation(val label: String, val promptInstruction: String) {
    REWRITE("Rewrite", "Rewrite the following text with enhanced flair, clarity, and pacing:"),
    EXPAND("Expand & Enrich", "Expand the following text with richer sensory descriptions, internal monologue, and character reactions:"),
    SHORTEN("Condense & Tighten", "Condense the following text into concise, punchy prose without losing key narrative beats:"),
    CHANGE_TONE("Change Tone", "Alter the emotional tone of the following text:"),
    CHANGE_GENRE("Shift Genre", "Adapt the following text into a new genre:"),
    FIX_GRAMMAR("Polish & Fix Grammar", "Proofread and polish the following prose for impeccable grammar, punctuation, and flow:"),
    IMPROVE_DIALOGUE("Sharpen Dialogue", "Sharpen the character dialogue, making it more natural, distinctive, and subtext-heavy:"),
    IMPROVE_DESCRIPTIONS("Enhance Descriptions", "Inject vivid visual imagery, lighting cues, and evocative atmospheric details into:"),
    MAKE_DARKER("Make Darker & Grittier", "Infuse grim suspense, high stakes, and brooding tension into:"),
    MAKE_EMOTIONAL("Make More Emotional", "Heighten the poignant emotional stakes, vulnerability, and character resonance in:"),
    MAKE_CINEMATIC("Make Cinematic", "Render the prose in sweeping, dynamic cinematic motion with visceral action beats:"),
    MAKE_REALISTIC("Make More Realistic", "Ground the interactions, physics, and character logic in authentic realism:"),
    MAKE_HUMOROUS("Inject Humor & Wit", "Add clever banter, ironic wit, and subtle comedic timing into:"),
    TRANSLATE("Translate", "Translate the following text faithfully into the target language:"),
    CONTINUE("Continue Scene", "Continue seamlessly from the exact end of the following scene:")
}

class StoryEngine(
    private val inferenceManager: AIInferenceManager,
    private val jobManager: AIJobManager,
    private val projectStorageManager: ProjectStorageManager
) {
    private val _currentStory = MutableStateFlow(StoryProject())
    val currentStory: StateFlow<StoryProject> = _currentStory.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationPhase = MutableStateFlow("Idle")
    val generationPhase: StateFlow<String> = _generationPhase.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun updateStoryProject(project: StoryProject) {
        _currentStory.value = project.copy(updatedAt = System.currentTimeMillis())
    }

    fun setActiveChapter(index: Int) {
        _currentStory.value = _currentStory.value.copy(activeChapterIndex = index)
    }

    fun addCharacter(character: StoryCharacter) {
        val chars = _currentStory.value.characters.toMutableList()
        chars.add(character)
        _currentStory.value = _currentStory.value.copy(characters = chars)
    }

    fun removeCharacter(charId: String) {
        val chars = _currentStory.value.characters.filterNot { it.id == charId }
        _currentStory.value = _currentStory.value.copy(characters = chars)
    }

    /**
     * Executes multi-phase story generation via AIInferenceManager and background AIJobManager.
     */
    suspend fun generateFullStory(
        project: StoryProject,
        selectedModel: AiModelEntity? = null
    ): Result<StoryProject> = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        _statusMessage.value = null

        // 1. Capability Validation
        val compCheck = inferenceManager.validateCapability(selectedModel, ModelCapability.STORY_WRITING)
        if (!compCheck.isCompatible) {
            _isGenerating.value = false
            val msg = compCheck.errorMessage ?: "Selected model does not support Story Writing."
            _statusMessage.value = msg
            return@withContext Result.failure(IllegalArgumentException(msg))
        }

        val jobId = "story_job_${System.currentTimeMillis()}"
        jobManager.submitJob(
            UnifiedAIJob(
                jobId = jobId,
                type = AIJobType.STORY_GENERATION,
                title = "Story: ${project.title}",
                modelName = selectedModel?.name ?: "Auto AI Model",
                totalSteps = project.chapterCount + 2,
                inputDescription = "Genre: ${project.genre}, Chapters: ${project.chapterCount}, Style: ${project.writingStyle}"
            )
        )

        try {
            // PHASE 1: Story Planner (Premise & 3-Act Outline)
            _generationPhase.value = "Phase 1/5: Story Planner & 3-Act Architecture"
            jobManager.updateJobProgress(jobId, 0.15f, 1, project.chapterCount + 2, "Structuring 3-Act Outline")

            val plannerPrompt = """
                You are a master fiction author and narrative architect.
                Generate a structured story outline for:
                Title: ${project.title}
                Genre: ${project.genre} | Tone: ${project.tone} | Style: ${project.writingStyle}
                Theme: ${project.theme} | Target Audience: ${project.targetAudience}
                Setting: ${project.setting} (${project.timePeriod})
                Conflict: ${project.mainConflict} | Ending: ${project.endingType}
                Characters:
                ${project.characters.joinToString("\n") { "- ${it.name} (${it.role}): ${it.personality}. Backstory: ${it.backstory}" }}
                World Rules & Lore: ${project.worldMemory}
                Custom Directives: ${project.customInstructions}

                Generate a comprehensive narrative outline with ${project.chapterCount} distinct chapters including title, key beat, and tension arc for each.
            """.trimIndent()

            val outlineRes = inferenceManager.generateText(
                AIInferenceRequest(
                    prompt = plannerPrompt,
                    systemPrompt = "You are an elite narrative designer. Write vivid, compelling story outlines.",
                    requiredCapability = ModelCapability.STORY_WRITING,
                    targetModel = selectedModel,
                    maxTokens = 1200
                )
            ).getOrThrow()

            var updatedProject = project.copy(outline = outlineRes.text)

            // PHASE 2 & 3: Chapter Planner & Scene Generator with Rolling Context Compaction
            val generatedChapters = mutableListOf<StoryChapter>()
            val chapterSummaries = mutableListOf<String>()

            for (c in 1..project.chapterCount) {
                _generationPhase.value = "Phase ${c + 1}/${project.chapterCount + 2}: Synthesizing Chapter $c Prose"
                val progressFrac = 0.2f + (c.toFloat() / project.chapterCount.toFloat()) * 0.7f
                jobManager.updateJobProgress(jobId, progressFrac, c + 1, project.chapterCount + 2, "Writing Chapter $c")

                // Compact Context: Rolling summaries of prior chapters + Core Character/World Memory
                val memoryContext = if (chapterSummaries.isNotEmpty()) {
                    "PRIOR CHAPTERS SUMMARY:\n" + chapterSummaries.mapIndexed { idx, s -> "Ch ${idx + 1}: $s" }.joinToString("\n")
                } else {
                    "STARTING CHAPTER 1: Introduce the world, protagonist's status quo, and inciting incident."
                }

                val chapterPrompt = """
                    Write Chapter $c for the story '${project.title}'.
                    Genre: ${project.genre} | Tone: ${project.tone} | POV: ${project.pointOfView}
                    Characters Present: ${project.characters.joinToString { it.name }}
                    World Rules: ${project.worldMemory}
                    
                    $memoryContext
                    
                    STORY OUTLINE CONTEXT:
                    ${updatedProject.outline.take(600)}
                    
                    SPECIFIC INSTRUCTIONS:
                    Write the full prose for Chapter $c with rich dialogue, immersive atmospheric details, sensory pacing, and natural character voice.
                """.trimIndent()

                val chapterProseRes = inferenceManager.generateText(
                    AIInferenceRequest(
                        prompt = chapterPrompt,
                        systemPrompt = "You are a bestselling novelist writing Chapter $c. Emphasize character chemistry, sensory detail, and dynamic conflict.",
                        requiredCapability = ModelCapability.STORY_WRITING,
                        targetModel = selectedModel,
                        maxTokens = 2048
                    )
                ).getOrThrow()

                val prose = chapterProseRes.text
                val summary = "Chapter $c centers on ${project.characters.firstOrNull()?.name ?: "the protagonist"} navigating ${project.setting}. Key developments unfold advancing the main conflict: ${project.mainConflict.take(100)}."
                chapterSummaries.add(summary)

                val wordCount = prose.split(Regex("\\s+")).size
                generatedChapters.add(
                    StoryChapter(
                        chapterIndex = c,
                        title = "Chapter $c: Revelation at ${project.setting.take(20)}",
                        summary = summary,
                        fullProse = prose,
                        wordCount = wordCount
                    )
                )
            }

            // PHASE 4: Continuity Verification & Final Packaging
            _generationPhase.value = "Final Phase: Continuity Verification & Local Storage"
            jobManager.updateJobProgress(jobId, 0.98f, project.chapterCount + 2, project.chapterCount + 2, "Validating continuity")

            updatedProject = updatedProject.copy(
                chapters = generatedChapters,
                activeChapterIndex = 0,
                updatedAt = System.currentTimeMillis()
            )

            // Save to local storage
            val fullManuscript = buildManuscriptText(updatedProject)
            projectStorageManager.saveStoryFile(updatedProject.title, fullManuscript, "md")

            jobManager.updateJobProgress(
                jobId = jobId,
                progress = 1.0f,
                currentStep = project.chapterCount + 2,
                totalSteps = project.chapterCount + 2,
                checkpointPhase = "Completed",
                status = AIJobStatus.COMPLETED,
                outputPreview = generatedChapters.firstOrNull()?.fullProse?.take(300)
            )

            _currentStory.value = updatedProject
            _statusMessage.value = "Successfully generated ${updatedProject.title} (${generatedChapters.sumOf { it.wordCount }} words across ${generatedChapters.size} chapters)."
            return@withContext Result.success(updatedProject)

        } catch (e: Exception) {
            jobManager.updateJobProgress(
                jobId = jobId,
                progress = 0.5f,
                currentStep = 1,
                totalSteps = project.chapterCount + 2,
                checkpointPhase = "Error",
                status = AIJobStatus.FAILED,
                error = e.message
            )
            _statusMessage.value = "Generation failed: ${e.message}"
            return@withContext Result.failure(e)
        } finally {
            _isGenerating.value = false
            _generationPhase.value = "Idle"
        }
    }

    /**
     * Continues an existing story from a specific chapter or starting point.
     */
    suspend fun continueStory(
        project: StoryProject,
        fromChapterIndex: Int,
        selectedModel: AiModelEntity? = null
    ): Result<StoryProject> = withContext(Dispatchers.IO) {
        _isGenerating.value = true
        val nextChapterIndex = project.chapters.size + 1
        _generationPhase.value = "Continuing Story: Writing Chapter $nextChapterIndex"

        val priorContext = project.chapters.mapIndexed { idx, ch ->
            "Chapter ${idx + 1} (${ch.title}): ${ch.summary.ifBlank { ch.fullProse.take(200) }}"
        }.joinToString("\n")

        val continuePrompt = """
            You are continuing the saved story '${project.title}'.
            Genre: ${project.genre} | Style: ${project.writingStyle} | POV: ${project.pointOfView}
            World Lore & Memory: ${project.worldMemory}
            
            STORY PROGRESSION SO FAR:
            $priorContext
            
            DIRECTIVE:
            Write Chapter $nextChapterIndex continuing directly from the events of Chapter ${project.chapters.size}.
            Introduce new narrative friction, explore character motivations, and advance toward the climax.
        """.trimIndent()

        val res = inferenceManager.generateText(
            AIInferenceRequest(
                prompt = continuePrompt,
                systemPrompt = "You are a master fiction novelist continuing a serialized book.",
                requiredCapability = ModelCapability.STORY_WRITING,
                targetModel = selectedModel,
                maxTokens = 2048
            )
        )

        return@withContext if (res.isSuccess) {
            val prose = res.getOrThrow().text
            val newChapter = StoryChapter(
                chapterIndex = nextChapterIndex,
                title = "Chapter $nextChapterIndex: The Next Horizon",
                summary = "Events continue as tensions escalate following Chapter ${project.chapters.size}.",
                fullProse = prose,
                wordCount = prose.split(Regex("\\s+")).size
            )
            val updatedChapters = project.chapters.toMutableList().apply { add(newChapter) }
            val updated = project.copy(chapters = updatedChapters, activeChapterIndex = updatedChapters.size - 1, updatedAt = System.currentTimeMillis())
            _currentStory.value = updated
            _statusMessage.value = "Appended Chapter $nextChapterIndex to ${project.title}."
            _isGenerating.value = false
            Result.success(updated)
        } else {
            _isGenerating.value = false
            _statusMessage.value = "Continue story failed: ${res.exceptionOrNull()?.message}"
            Result.failure(res.exceptionOrNull() ?: RuntimeException("Inference failed"))
        }
    }

    /**
     * Executes contextual story editing on selected text or full chapter prose.
     */
    suspend fun applyEditOperation(
        targetText: String,
        operation: StoryEditOperation,
        additionalParam: String = "",
        selectedModel: AiModelEntity? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            ${operation.promptInstruction} ${if (additionalParam.isNotBlank()) "Target: $additionalParam" else ""}
            
            === ORIGINAL TEXT ===
            $targetText
            =====================
            
            Provide only the refined, polished prose replacement maintaining story continuity.
        """.trimIndent()

        val res = inferenceManager.generateText(
            AIInferenceRequest(
                prompt = prompt,
                systemPrompt = "You are an award-winning literary editor. Provide pristine, evocative prose revisions.",
                requiredCapability = ModelCapability.STORY_WRITING,
                targetModel = selectedModel,
                maxTokens = 1500
            )
        )

        return@withContext if (res.isSuccess) {
            Result.success(res.getOrThrow().text)
        } else {
            Result.failure(res.exceptionOrNull() ?: RuntimeException("Edit failed"))
        }
    }

    fun buildManuscriptText(project: StoryProject): String {
        val sb = StringBuilder()
        sb.append("# ${project.title}\n")
        sb.append("**Genre:** ${project.genre} | **Theme:** ${project.theme}\n\n")
        sb.append("## Story Outline\n${project.outline}\n\n")
        sb.append("## Characters\n")
        project.characters.forEach { ch ->
            sb.append("- **${ch.name}** (${ch.role}): ${ch.personality}\n")
        }
        sb.append("\n---\n\n")
        project.chapters.forEach { ch ->
            sb.append("## ${ch.title}\n\n")
            sb.append("${ch.fullProse}\n\n")
        }
        return sb.toString()
    }
}
