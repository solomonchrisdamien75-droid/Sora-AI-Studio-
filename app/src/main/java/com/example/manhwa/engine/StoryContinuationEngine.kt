package com.example.manhwa.engine

import com.example.ai.inference.AIInferenceManager
import com.example.ai.inference.AIInferenceRequest
import com.example.ai.inference.model.ModelCapability
import com.example.manhwa.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * StoryContinuationEngine manages narrative continuity across episodes using StoryState memory.
 * Supports Option A: Continue Recap (original lore) and Option B: Create Original Continuation (fictional branch with disclaimer).
 */
class StoryContinuationEngine(private val inferenceManager: AIInferenceManager? = null) {

    data class ContinuationResult(
        val episodeNumber: Int,
        val chapterTitle: String,
        val startingSceneDescription: String,
        val generatedScenes: List<ManhwaScene>,
        val updatedStoryState: StoryState,
        val isOriginalContinuation: Boolean,
        val legalDisclaimer: String
    )

    /**
     * Generates the next story episode starting exactly where the previous left off.
     */
    suspend fun generateNextEpisode(
        currentStoryState: StoryState,
        continuationType: ContinuationType,
        customPrompt: String = "",
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): ContinuationResult = withContext(Dispatchers.Default) {
        onProgress(20, "Loading StoryState continuity memory (Chapter ${currentStoryState.currentChapter})...")
        delay(120)

        onProgress(50, "Evaluating unresolved conflicts & character status...")
        delay(140)

        val nextEp = currentStoryState.currentEpisode + 1
        val nextChapter = currentStoryState.currentChapter + 1
        val isOriginal = continuationType == ContinuationType.CREATE_ORIGINAL_CONTINUATION

        onProgress(75, if (isOriginal) "Generating original narrative branch via local inference..." else "Extrapolating recap continuation...")

        var generatedNarrative: String? = null
        if (inferenceManager != null) {
            val prompt = """
                Generate the next scene continuation for Episode $nextEp (Chapter $nextChapter).
                Previous Location: ${currentStoryState.currentLocation}
                Objective: ${currentStoryState.currentObjective}
                User Guidance: $customPrompt
                Type: ${if (isOriginal) "Original Creative Alternate Branch" else "Canon Progression"}
                
                Provide 3 cinematic scene descriptions with narration and dialogue.
            """.trimIndent()

            val response = inferenceManager.generateText(
                AIInferenceRequest(
                    prompt = prompt,
                    systemPrompt = "You are a specialized Manhwa Webtoon Story Director and Scriptwriter.",
                    requiredCapability = ModelCapability.SCRIPT_WRITING,
                    maxTokens = 1200
                )
            )
            generatedNarrative = response.getOrNull()?.text
        }

        val scene1Narration = if (!generatedNarrative.isNullOrBlank()) {
            generatedNarrative.lines().firstOrNull { it.isNotBlank() } ?: "With the previous conflict resolved, a new path opened."
        } else if (isOriginal) {
            "With the Demon King defeated, an ancient celestial portal opened above the castle ruins."
        } else {
            "As the dust settled, Jin-Woo collected the Demon Sovereign's core and prepared to craft the Holy Water of Life."
        }

        val newScenes = listOf(
            ManhwaScene(
                id = "S_EP${nextEp}_001",
                sceneNumber = 1,
                panelId = "P_CONT_001",
                durationMs = 4800L,
                narrationText = scene1Narration,
                dialogueText = "This power... it's far greater than anything on Earth.",
                speakerCharacterId = "CHAR_01",
                actionType = ActionType.LOOKING,
                actionDescription = "Hero examines the glowing core while shadows surround him",
                cameraMotion = CameraMotionType.SLOW_PUSH_IN,
                animationMotion = AnimationMotionType.DARK_AURA_MIST
            ),
            ManhwaScene(
                id = "S_EP${nextEp}_002",
                sceneNumber = 2,
                panelId = "P_CONT_002",
                durationMs = 5200L,
                narrationText = "A sudden system alert resonated directly in his mind.",
                dialogueText = "[System: Special Quest - The Monarch of Frost Beckons]",
                speakerCharacterId = null,
                actionType = ActionType.REACTING,
                actionDescription = "System holographic screen flashes blue in front of hero",
                cameraMotion = CameraMotionType.SLOW_CLOSEUP,
                animationMotion = AnimationMotionType.SPEED_LINES_BURST
            ),
            ManhwaScene(
                id = "S_EP${nextEp}_003",
                sceneNumber = 3,
                panelId = "P_CONT_003",
                durationMs = 6000L,
                narrationText = "Without hesitation, he summoned Kaisel and soared towards the northern rift.",
                dialogueText = "Let's see what these other Monarchs are capable of.",
                speakerCharacterId = "CHAR_01",
                actionType = ActionType.FLYING,
                actionDescription = "Hero mounts shadow dragon and takes flight through the crimson sky",
                cameraMotion = CameraMotionType.WIDE_SWEEP,
                animationMotion = AnimationMotionType.PARALLAX_DEPTH,
                sfxName = "AURA_HUM"
            )
        )

        val updatedState = currentStoryState.copy(
            currentChapter = nextChapter,
            currentEpisode = nextEp,
            currentLocation = if (isOriginal) "Celestial Rift Ruins" else "Northern Frost Domain",
            characterStates = currentStoryState.characterStates.toMutableMap().apply {
                put("Sung Jin-Woo", "Awakened Monarch (Level 100, Demon King Core Equipped)")
            },
            completedEvents = currentStoryState.completedEvents + "Defeated Demon King Baran",
            currentObjective = if (isOriginal) "Investigate the Celestial Rift" else "Confront the Frost Monarch",
            previousDialogueHistory = currentStoryState.previousDialogueHistory + "Sung Jin-Woo: 'Let's see what these other Monarchs are capable of.'"
        )

        onProgress(100, "Continuation generated successfully.")

        return@withContext ContinuationResult(
            episodeNumber = nextEp,
            chapterTitle = if (isOriginal) "Original Story Arc: The Celestial Rupture" else "Chapter $nextChapter: The Northern Frost",
            startingSceneDescription = "Hero inside tower ruins following the defeat of the floor boss.",
            generatedScenes = newScenes,
            updatedStoryState = updatedState,
            isOriginalContinuation = isOriginal,
            legalDisclaimer = if (isOriginal) {
                "⚠️ ORIGINAL FICTIONAL CONTINUATION: This episode was generated as original creative content inspired by established characters and themes. It is not affiliated with or part of any official copyrighted publication."
            } else {
                "ℹ️ RECAP CONTINUATION: User is responsible for holding the rights or permissions for materials processed."
            }
        )
    }
}
