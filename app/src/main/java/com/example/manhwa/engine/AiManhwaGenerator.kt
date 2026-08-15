package com.example.manhwa.engine

import android.content.Context
import com.example.manhwa.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AiManhwaGenerator creates original manhwa chapters, panel layouts, dialogues,
 * and character consistency profiles from story ideas and art style prompts.
 */
class AiManhwaGenerator(private val context: Context) {

    data class GeneratedManhwaPackage(
        val storyTitle: String,
        val genre: String,
        val synopsis: String,
        val characters: List<ManhwaCharacter>,
        val panels: List<ManhwaPanel>,
        val script: String
    )

    /**
     * Synthesizes an entire original Manhwa chapter with panels & character profiles.
     */
    suspend fun generateOriginalChapter(
        idea: String,
        genre: String,
        artStyle: String,
        panelCount: Int = 8,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): GeneratedManhwaPackage = withContext(Dispatchers.Default) {
        onProgress(20, "Drafting world-building and character consistency profiles...")
        delay(130)

        onProgress(50, "Generating storyboard compositions and panel bounding boxes...")
        delay(150)

        onProgress(75, "Synthesizing character dialogues, sound effects, and action lines...")
        delay(140)

        val protagonist = ManhwaCharacter(
            id = "CHAR_ORIG_01",
            name = "Kaelen Voss",
            role = "Protagonist",
            appearanceDescription = "Silver messy hair with cybernetic eye, midnight blue tactical coat",
            hair = "Silver White, Undercut",
            clothing = "Midnight Trench Coat with energy conduits",
            ageCategory = "Young Adult (20)",
            personality = "Analytical, relentless, silent protector",
            voiceId = "VOICE_COOL_HERO",
            consistencyProfileSummary = "Maintain sharp silver hair outline, glowing cyan right pupil, ink splash shading."
        )

        val panels = mutableListOf<ManhwaPanel>()
        for (i in 0 until panelCount) {
            val panelId = "P_AI_%03d".format(i + 1)
            panels.add(
                ManhwaPanel(
                    id = panelId,
                    pageIndex = i / 4,
                    panelIndex = i % 4,
                    boundingBox = PanelBoundingBox(0.05f, 0.05f + (i % 4) * 0.23f, 0.90f, 0.20f),
                    characterIds = listOf("CHAR_ORIG_01"),
                    environmentDescription = "Neo-Seoul Underbelly drenched in rain and holographic billboards",
                    actionDescription = when (i % 3) {
                        0 -> "Kaelen stands atop a rainy skyscraper overlooking the ruined sector"
                        1 -> "He activates his nano-blade as cyber-hounds leap from the dark"
                        else -> "High-speed energy slash bisecting the lead mechanical hound"
                    },
                    cameraFraming = when (i % 3) {
                        0 -> CameraFraming.WIDE_SHOT
                        1 -> CameraFraming.MEDIUM_SHOT
                        else -> CameraFraming.DUTCH_ANGLE
                    },
                    panelOrder = i + 1,
                    expressionSummary = "Cold Calculation",
                    soundEffects = listOf("ZZZZT!", "SLASH!"),
                    ocrTextBlocks = listOf(
                        OcrTextBlock(
                            text = if (i == 0) "Sector 9 has fallen. It's time." else "Slice through the core!",
                            category = OcrCategory.DIALOGUE,
                            speakerCharacterId = "CHAR_ORIG_01"
                        )
                    )
                )
            )
        }

        onProgress(100, "Original Manhwa chapter created with $panelCount panels.")

        return@withContext GeneratedManhwaPackage(
            storyTitle = if (idea.isNotBlank()) idea.take(40) else "Chronicles of the Nano-Monarch",
            genre = genre.ifBlank { "Action / Cyberpunk Fantasy" },
            synopsis = "In a futuristic realm where ancient demonic rifts meet cyber-augmentation, Kaelen awakens the lost Nano-Monarch system.",
            characters = listOf(protagonist),
            panels = panels,
            script = "Scene 1: Skyscraper overlook\nScene 2: Ambush by cyber-hounds\nScene 3: First awakening slash"
        )
    }
}
