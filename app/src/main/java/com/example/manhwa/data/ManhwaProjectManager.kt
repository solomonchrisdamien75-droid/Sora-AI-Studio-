package com.example.manhwa.data

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ManhwaProjectEntity
import com.example.manhwa.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * ManhwaProjectManager handles project directory creation, project.json serialization,
 * Storage Access Framework file imports, and checkpoint save/resume.
 */
class ManhwaProjectManager(private val context: Context) {

    private val manhwaProjectDao = AppDatabase.getDatabase(context).manhwaProjectDao()

    private val baseProjectsDir: File by lazy {
        File(context.filesDir, "manhwa_projects").apply { if (!exists()) mkdirs() }
    }

    /**
     * Initializes a structured project folder directory.
     */
    fun createProjectDirectory(projectId: String): File {
        val projDir = File(baseProjectsDir, projectId).apply { if (!exists()) mkdirs() }
        listOf("panels", "characters", "scenes", "audio", "voices", "animation", "generated", "models", "subtitles", "exports", "logs").forEach { sub ->
            File(projDir, sub).apply { if (!exists()) mkdirs() }
        }
        return projDir
    }

    /**
     * Saves project.json state for checkpoint resume, and updates Room database.
     */
    suspend fun saveProjectState(project: ManhwaProject) = withContext(Dispatchers.IO) {
        val projDir = createProjectDirectory(project.id)
        val metaFile = File(projDir, "project.json")

        val json = JSONObject().apply {
            put("id", project.id)
            put("title", project.title)
            put("episodeTitle", project.episodeTitle)
            put("description", project.description)
            put("createdAt", project.createdAt)
            put("updatedAt", System.currentTimeMillis())
            put("durationSeconds", project.durationSeconds)
            put("fps", project.fps)
            put("resolution", project.resolution)
            put("aspectRatio", project.aspectRatio)
            put("totalPanels", project.panels.size)
            put("totalScenes", project.scenes.size)
            put("status", project.status.name)
            put("lastSavedStep", project.lastSavedStep)
            put("lastSceneIndex", project.lastSceneIndex)
            put("exportedVideoPath", project.exportedVideoPath ?: "")
            put("exportedSubtitlesPath", project.exportedSubtitlesPath ?: "")

            // StoryState
            put("storyState", JSONObject().apply {
                put("currentChapter", project.storyState.currentChapter)
                put("currentEpisode", project.storyState.currentEpisode)
                put("currentLocation", project.storyState.currentLocation)
                put("currentObjective", project.storyState.currentObjective)
                put("narrativeTone", project.storyState.narrativeTone)
            })

            // RecapConfig
            put("recapConfig", JSONObject().apply {
                put("targetDurationMinutes", project.recapConfig.targetDurationMinutes)
                put("narrationStyle", project.recapConfig.narrationStyle)
                put("voiceStyle", project.recapConfig.voiceStyle)
                put("tone", project.recapConfig.tone)
                put("format", project.recapConfig.format.name)
            })
        }

        val jsonString = json.toString(2)
        metaFile.writeText(jsonString)

        val entity = ManhwaProjectEntity(
            id = project.id,
            title = project.title,
            episodeTitle = project.episodeTitle,
            description = project.description,
            createdAt = project.createdAt,
            updatedAt = System.currentTimeMillis(),
            thumbnailUri = project.thumbnailUri,
            projectDir = projDir.absolutePath,
            status = project.status.name,
            totalPanels = project.panels.size,
            totalScenes = project.scenes.size,
            manhwaProjectJson = jsonString
        )
        manhwaProjectDao.insertManhwaProject(entity)
    }

    /**
     * Loads default initial project with sample panels, characters, and scenes.
     */
    fun createDefaultProject(): ManhwaProject {
        val p1 = ManhwaPanel(
            id = "P001",
            pageIndex = 0,
            panelIndex = 0,
            characterIds = listOf("CHAR_01"),
            actionDescription = "Sung Jin-Woo unsheathes glowing double daggers as shadow aura engulfs the ground",
            cameraFraming = CameraFraming.MEDIUM_SHOT,
            panelOrder = 1,
            expressionSummary = "Determined & Fierce",
            soundEffects = listOf("SHINNGG!", "VRRRMM!"),
            ocrTextBlocks = listOf(
                OcrTextBlock(
                    text = "From this moment on... you answer to the Shadow Monarch.",
                    category = OcrCategory.DIALOGUE,
                    speakerCharacterId = "CHAR_01"
                )
            )
        )

        val p2 = ManhwaPanel(
            id = "P002",
            pageIndex = 0,
            panelIndex = 1,
            characterIds = listOf("CHAR_02"),
            actionDescription = "Demon King Baran roars in fury, lightning crackling between his horns",
            cameraFraming = CameraFraming.CLOSE_UP,
            panelOrder = 2,
            expressionSummary = "Wrathful & Menacing",
            soundEffects = listOf("KRAAA-BOOM!"),
            ocrTextBlocks = listOf(
                OcrTextBlock(
                    text = "Insolent mortal! You will burn to ash in my white flames!",
                    category = OcrCategory.DIALOGUE,
                    speakerCharacterId = "CHAR_02"
                )
            )
        )

        val p3 = ManhwaPanel(
            id = "P003",
            pageIndex = 0,
            panelIndex = 2,
            characterIds = listOf("CHAR_01", "CHAR_02"),
            actionDescription = "Hero leaps through the storm, dodging a massive lightning strike",
            cameraFraming = CameraFraming.WIDE_SHOT,
            panelOrder = 3,
            expressionSummary = "Focused Combat",
            soundEffects = listOf("WHOOSH", "CRASH"),
            ocrTextBlocks = listOf(
                OcrTextBlock(
                    text = "The dungeon floor shattered under the sheer weight of their clash.",
                    category = OcrCategory.NARRATION,
                    speakerCharacterId = "NARRATOR"
                )
            )
        )

        val p4 = ManhwaPanel(
            id = "P004",
            pageIndex = 0,
            panelIndex = 3,
            characterIds = listOf("CHAR_01"),
            actionDescription = "Hero raises his hand as violet runes ignite across the battlefield",
            cameraFraming = CameraFraming.EXTREME_CLOSE_UP,
            panelOrder = 4,
            expressionSummary = "Commanding Monarch",
            soundEffects = listOf("FLASH", "RUMBLE"),
            ocrTextBlocks = listOf(
                OcrTextBlock(
                    text = "Arise.",
                    category = OcrCategory.DIALOGUE,
                    speakerCharacterId = "CHAR_01"
                )
            )
        )

        val char1 = ManhwaCharacter(
            id = "CHAR_01",
            name = "Sung Jin-Woo",
            role = "Protagonist",
            appearanceDescription = "Messy black hair, glowing purple eyes, dark high-collar monarch trench coat",
            hair = "Jet Black, Spiky",
            clothing = "Shadow Monarch Trench Coat",
            ageCategory = "Young Adult (22)",
            personality = "Calm, strategic, protective",
            voiceId = "VOICE_DARK_HERO",
            voiceCharacteristics = "Deep, resonant baritone with gravel undertones",
            voiceSpeed = 1.0f,
            voicePitch = 0.95f,
            voiceEmotion = "DETERMINED",
            consistencyProfileSummary = "Glowing purple eye particle aura, sharp jawline, high contrast ink shadows."
        )

        val char2 = ManhwaCharacter(
            id = "CHAR_02",
            name = "Demon King Baran",
            role = "Antagonist",
            appearanceDescription = "Giant demon lord with white flame aura, curved horns, and storm halberd",
            hair = "White Flame Crest",
            clothing = "Heavy Obsidian Demon Plate Armor",
            ageCategory = "Ancient Sovereign",
            personality = "Arrogant, wrathful, destructive",
            voiceId = "VOICE_DEEP_EPIC",
            voiceCharacteristics = "Booming guttural demon roar with echo reverb",
            voiceSpeed = 0.9f,
            voicePitch = 0.75f,
            voiceEmotion = "WRATHFUL",
            consistencyProfileSummary = "White flame horn accents, fiery red eyes, cracked lightning armor texture."
        )

        val narrator = ManhwaCharacter(
            id = "NARRATOR",
            name = "Epic Storyteller",
            role = "Narrator",
            appearanceDescription = "Omniscient Manhwa Recap Narrator Voice",
            hair = "N/A",
            clothing = "N/A",
            ageCategory = "Adult",
            personality = "Hype, cinematic, thrilling",
            voiceId = "VOICE_NARRATOR_EPIC",
            voiceCharacteristics = "Charismatic storyteller baritone with dynamic dramatic cadence",
            voiceSpeed = 1.05f,
            voicePitch = 1.0f,
            voiceEmotion = "DRAMATIC",
            consistencyProfileSummary = "Standard recap narrator audio profile."
        )

        val scenes = listOf(
            ManhwaScene(
                id = "S001",
                sceneNumber = 1,
                panelId = "P001",
                durationMs = 5400L,
                narrationText = "He had finally reached the hundredth floor of the demon castle.",
                dialogueText = "From this moment on... you answer to the Shadow Monarch.",
                speakerCharacterId = "CHAR_01",
                actionType = ActionType.WALKING,
                actionDescription = "Hero unsheathes glowing double daggers as shadow aura engulfs the ground",
                cameraMotion = CameraMotionType.SLOW_PUSH_IN,
                animationMotion = AnimationMotionType.DARK_AURA_MIST,
                sfxName = "AURA_HUM",
                transitionType = TransitionType.MANHWA_SLASH_FADE
            ),
            ManhwaScene(
                id = "S002",
                sceneNumber = 2,
                panelId = "P002",
                durationMs = 4800L,
                narrationText = "The demon king unleashed a tempest of white lightning.",
                dialogueText = "Insolent mortal! You will burn to ash in my white flames!",
                speakerCharacterId = "CHAR_02",
                actionType = ActionType.ATTACKING,
                actionDescription = "Demon King Baran roars in fury, lightning crackling between his horns",
                cameraMotion = CameraMotionType.IMPACT_SHAKE_ZOOM,
                animationMotion = AnimationMotionType.SPEED_LINES_BURST,
                sfxName = "HEAVY_PUNCH",
                transitionType = TransitionType.INK_SPLASH
            ),
            ManhwaScene(
                id = "S003",
                sceneNumber = 3,
                panelId = "P003",
                durationMs = 5200L,
                narrationText = "With impossible speed, Jin-Woo danced through the lightning strikes.",
                dialogueText = null,
                speakerCharacterId = null,
                actionType = ActionType.DODGING,
                actionDescription = "Hero leaps through the storm, dodging a massive lightning strike",
                cameraMotion = CameraMotionType.FAST_TRACKING,
                animationMotion = AnimationMotionType.PARALLAX_DEPTH,
                sfxName = "SWORD_SLASH",
                transitionType = TransitionType.MANHWA_SLASH_FADE
            ),
            ManhwaScene(
                id = "S004",
                sceneNumber = 4,
                panelId = "P004",
                durationMs = 6200L,
                narrationText = "Extending his hand, the single word echoed through the dimensional barrier.",
                dialogueText = "Arise.",
                speakerCharacterId = "CHAR_01",
                actionType = ActionType.TRANSFORMING,
                actionDescription = "Hero raises his hand as violet runes ignite across the battlefield",
                cameraMotion = CameraMotionType.SLOW_CLOSEUP,
                animationMotion = AnimationMotionType.DARK_AURA_MIST,
                sfxName = "AURA_HUM",
                transitionType = TransitionType.FLASH_WHITE
            )
        )

        return ManhwaProject(
            id = "proj_manhwa_awakening",
            title = "Solo Hunter Awakening",
            episodeTitle = "Episode 01 — The 100th Floor",
            description = "The decisive battle against the Sovereign of White Flames and the birth of the Shadow Army.",
            durationSeconds = 512,
            fps = 24,
            resolution = "1080p",
            aspectRatio = "16:9",
            totalPanels = 18,
            totalScenes = 24,
            panels = listOf(p1, p2, p3, p4),
            characters = listOf(char1, char2, narrator),
            scenes = scenes,
            storyState = StoryState(),
            recapConfig = RecapConfig()
        )
    }
}
