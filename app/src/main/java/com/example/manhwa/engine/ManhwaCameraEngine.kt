package com.example.manhwa.engine

import com.example.manhwa.model.CameraFraming
import com.example.manhwa.model.CameraKeyframes
import com.example.manhwa.model.CameraMotionType

/**
 * ManhwaCameraEngine calculates dynamic cinematic trajectories, panning, zooms,
 * Dutch angles, and impact shake impulses tailored for manhwa panel storytelling.
 */
class ManhwaCameraEngine {

    /**
     * Generates camera keyframe transforms based on scene classification and importance.
     */
    fun createKeyframesForScene(
        motionType: CameraMotionType,
        framing: CameraFraming,
        isClashOrImpact: Boolean
    ): CameraKeyframes {
        return when (motionType) {
            CameraMotionType.SLOW_PUSH_IN -> CameraKeyframes(
                startScale = 1.0f,
                endScale = 1.25f,
                startOffsetX = 0f,
                endOffsetX = 0f,
                startOffsetY = 0f,
                endOffsetY = -0.05f,
                rotationDeg = 0f,
                shakeIntensity = if (isClashOrImpact) 6.0f else 0f
            )
            CameraMotionType.FAST_TRACKING -> CameraKeyframes(
                startScale = 1.1f,
                endScale = 1.3f,
                startOffsetX = -0.15f,
                endOffsetX = 0.15f,
                startOffsetY = 0f,
                endOffsetY = 0f,
                rotationDeg = 1.5f,
                shakeIntensity = 4.0f
            )
            CameraMotionType.IMPACT_SHAKE_ZOOM -> CameraKeyframes(
                startScale = 1.0f,
                endScale = 1.4f,
                startOffsetX = 0f,
                endOffsetX = 0f,
                startOffsetY = 0.05f,
                endOffsetY = -0.05f,
                rotationDeg = 0f,
                shakeIntensity = 12.0f
            )
            CameraMotionType.SLOW_CLOSEUP -> CameraKeyframes(
                startScale = 1.2f,
                endScale = 1.38f,
                startOffsetX = 0f,
                endOffsetX = 0f,
                startOffsetY = 0f,
                endOffsetY = -0.08f,
                rotationDeg = 0f,
                shakeIntensity = 0f
            )
            CameraMotionType.PAN_ACROSS -> CameraKeyframes(
                startScale = 1.15f,
                endScale = 1.15f,
                startOffsetX = -0.2f,
                endOffsetX = 0.2f,
                startOffsetY = 0f,
                endOffsetY = 0f,
                rotationDeg = 0f,
                shakeIntensity = 0f
            )
            CameraMotionType.WIDE_SWEEP -> CameraKeyframes(
                startScale = 1.0f,
                endScale = 1.15f,
                startOffsetX = -0.1f,
                endOffsetX = 0.1f,
                startOffsetY = 0.1f,
                endOffsetY = -0.1f,
                rotationDeg = -1.0f,
                shakeIntensity = 0f
            )
            CameraMotionType.DUTCH_TILT -> CameraKeyframes(
                startScale = 1.1f,
                endScale = 1.25f,
                startOffsetX = 0f,
                endOffsetX = 0f,
                startOffsetY = 0f,
                endOffsetY = 0f,
                rotationDeg = 5.0f,
                shakeIntensity = 2.0f
            )
            CameraMotionType.STATIC_DRAMATIC -> CameraKeyframes(
                startScale = 1.05f,
                endScale = 1.05f,
                startOffsetX = 0f,
                endOffsetX = 0f,
                startOffsetY = 0f,
                endOffsetY = 0f,
                rotationDeg = 0f,
                shakeIntensity = 0f
            )
        }
    }
}
