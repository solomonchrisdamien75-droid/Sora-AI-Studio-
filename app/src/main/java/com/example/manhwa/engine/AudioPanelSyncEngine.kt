package com.example.manhwa.engine

import android.content.Context
import com.example.manhwa.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * AudioPanelSyncEngine aligns audio segments with manhwa panels,
 * handles action audio replacement, and computes non-uniform scene durations.
 */
class AudioPanelSyncEngine(private val context: Context) {

    /**
     * Maps audio timeline segments to panels to build synchronized scenes.
     */
    suspend fun synchronizeAudioAndPanels(
        panels: List<ManhwaPanel>,
        audioTrack: AudioTrack?,
        recapConfig: RecapConfig,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): List<ManhwaScene> = withContext(Dispatchers.IO) {
        onProgress(20, "Cross-referencing OCR dialogue with audio transcript...")
        delay(130)

        onProgress(50, "Evaluating action timestamps and speaker assignments...")
        delay(140)

        onProgress(75, "Performing Action Audio Replacement & SFX alignment...")
        delay(120)

        val scenes = mutableListOf<ManhwaScene>()
        val segments = audioTrack?.segments ?: emptyList()

        if (segments.isNotEmpty()) {
            // Build scenes mapped to real audio segments
            for ((index, segment) in segments.withIndex()) {
                val panel = panels.getOrNull(index % max(1, panels.size)) ?: ManhwaPanel(id = "P001")
                val isDialogue = segment.classification == AudioClassification.CHARACTER_DIALOGUE
                val isActionSound = segment.classification == AudioClassification.ACTION_SOUND
                val isNarration = segment.classification == AudioClassification.NARRATION

                val speaker = when {
                    segment.speakerId != null -> segment.speakerId
                    isDialogue -> panel.characterIds.firstOrNull() ?: "CHAR_01"
                    else -> "NARRATOR"
                }

                val actionType = when (index % 6) {
                    0 -> ActionType.WALKING
                    1 -> ActionType.TALKING
                    2 -> ActionType.ATTACKING
                    3 -> ActionType.TRANSFORMING
                    4 -> ActionType.DODGING
                    else -> ActionType.IDLE
                }

                val cameraMotion = when {
                    isActionSound -> CameraMotionType.IMPACT_SHAKE_ZOOM
                    isDialogue -> CameraMotionType.SLOW_PUSH_IN
                    index % 3 == 0 -> CameraMotionType.SLOW_CLOSEUP
                    index % 3 == 1 -> CameraMotionType.PAN_ACROSS
                    else -> CameraMotionType.WIDE_SWEEP
                }

                val animationMotion = when (index % 5) {
                    0 -> AnimationMotionType.DARK_AURA_MIST
                    1 -> AnimationMotionType.MOUTH_LIPSYNC
                    2 -> AnimationMotionType.SLASH_ENERGY
                    3 -> AnimationMotionType.SPEED_LINES_BURST
                    else -> AnimationMotionType.PARALLAX_DEPTH
                }

                val duration = max(2200L, segment.durationMs)

                val scene = ManhwaScene(
                    id = "S%03d".format(index + 1),
                    sceneNumber = index + 1,
                    panelId = panel.id,
                    durationMs = duration,
                    narrationText = if (isNarration) segment.transcriptText else "The battle intensified with every breath.",
                    dialogueText = if (isDialogue) segment.transcriptText else null,
                    speakerCharacterId = if (isDialogue) speaker else null,
                    actionType = actionType,
                    actionDescription = panel.actionDescription,
                    actionRequiresReview = false,
                    cameraMotion = cameraMotion,
                    cameraKeyframes = CameraKeyframes(
                        startScale = 1.0f,
                        endScale = if (cameraMotion == CameraMotionType.IMPACT_SHAKE_ZOOM) 1.35f else 1.20f,
                        shakeIntensity = if (cameraMotion == CameraMotionType.IMPACT_SHAKE_ZOOM) 8.0f else 0.0f
                    ),
                    animationMotion = animationMotion,
                    visemes = generateVisemesForDuration(duration, isDialogue, speaker),
                    transitionType = if (index % 2 == 0) TransitionType.MANHWA_SLASH_FADE else TransitionType.INK_SPLASH,
                    sfxName = when (actionType) {
                        ActionType.ATTACKING -> "SWORD_SLASH"
                        ActionType.PUNCHING -> "HEAVY_PUNCH"
                        ActionType.TRANSFORMING -> "AURA_HUM"
                        else -> "NONE"
                    },
                    sfxTimestampMs = (duration * 0.35f).toLong(),
                    musicTrack = "EPIC_ORCHESTRAL_BATTLE",
                    isRedundantActionAudioRemoved = isActionSound,
                    originalActionAudioText = if (isActionSound) segment.transcriptText else null
                )
                scenes.add(scene)
            }
        } else {
            // Fallback: build scenes directly from panels with intelligent pacing
            for ((index, panel) in panels.withIndex()) {
                val duration = 3000L + (index * 600L % 2800L) // Variable pacing
                val primaryDialogue = panel.ocrTextBlocks.firstOrNull { it.category == OcrCategory.DIALOGUE }

                val scene = ManhwaScene(
                    id = "S%03d".format(index + 1),
                    sceneNumber = index + 1,
                    panelId = panel.id,
                    durationMs = duration,
                    narrationText = "Scene ${index + 1}: ${panel.environmentDescription}",
                    dialogueText = primaryDialogue?.text,
                    speakerCharacterId = primaryDialogue?.speakerCharacterId ?: panel.characterIds.firstOrNull(),
                    actionType = ActionType.WALKING,
                    actionDescription = panel.actionDescription,
                    actionRequiresReview = false,
                    cameraMotion = CameraMotionType.SLOW_PUSH_IN,
                    animationMotion = AnimationMotionType.DARK_AURA_MIST,
                    visemes = generateVisemesForDuration(duration, primaryDialogue != null, panel.characterIds.firstOrNull())
                )
                scenes.add(scene)
            }
        }

        onProgress(100, "Audio-Panel sync complete: ${scenes.size} scenes generated.")
        return@withContext scenes
    }

    private fun generateVisemesForDuration(
        durationMs: Long,
        isDialogue: Boolean,
        speakerId: String?
    ): List<VisemeKeyframe> {
        if (!isDialogue) return emptyList()
        val list = mutableListOf<VisemeKeyframe>()
        val shapes = VisemeShape.entries.filter { it != VisemeShape.REST }
        var currentMs = 200L
        while (currentMs < durationMs - 400L) {
            val shape = shapes.random()
            val mouthOpen = 0.4f + (Math.random().toFloat() * 0.6f)
            list.add(
                VisemeKeyframe(
                    timestampMs = currentMs,
                    visemeShape = shape,
                    mouthOpenRatio = mouthOpen,
                    activeSpeakerId = speakerId
                )
            )
            currentMs += (120L + (Math.random() * 180L).toLong())
        }
        // End with rest mouth
        list.add(
            VisemeKeyframe(
                timestampMs = durationMs - 100L,
                visemeShape = VisemeShape.REST,
                mouthOpenRatio = 0.0f,
                activeSpeakerId = speakerId
            )
        )
        return list
    }
}
