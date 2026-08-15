package com.example.manhwa.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manhwa.engine.AnimationRenderState
import com.example.manhwa.engine.ManhwaAnimationEngine
import com.example.manhwa.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * ManhwaCanvasPlayer renders real-time animated previews of Manhwa scenes including:
 * - Dynamic camera scale, panning offsets, and impact shakes
 * - Parallax depth layers (foreground hero, background environment)
 * - Anime speed lines burst & slash energy arcs
 * - Dark aura / mist particle simulations
 * - Active speaker mouth viseme shape animation
 * - Burned-in styled subtitles & sound effect overlays
 */
@Composable
fun ManhwaCanvasPlayer(
    scenes: List<ManhwaScene>,
    characters: List<ManhwaCharacter>,
    animationEngine: ManhwaAnimationEngine,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f
) {
    if (scenes.isEmpty()) {
        Box(
            modifier = modifier
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0D14))
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No scenes loaded. Import manhwa or build scenes to preview.", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentSceneIndex by remember { mutableIntStateOf(0) }
    var sceneElapsedMs by remember { mutableLongStateOf(0L) }

    val currentScene = scenes.getOrElse(currentSceneIndex) { scenes.first() }
    val totalSceneMs = currentScene.durationMs.coerceAtLeast(1000L)

    // Playback loop
    LaunchedEffect(isPlaying, currentSceneIndex) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            delay(33L) // ~30 FPS preview tick
            sceneElapsedMs += 33L
            if (sceneElapsedMs >= totalSceneMs) {
                sceneElapsedMs = 0L
                currentSceneIndex = (currentSceneIndex + 1) % scenes.size
            }
        }
    }

    val renderState = remember(currentScene, sceneElapsedMs) {
        animationEngine.computeFrameState(currentScene, sceneElapsedMs)
    }

    val activeSpeaker = characters.find { it.id == currentScene.speakerCharacterId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF07090F))
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
    ) {
        // Top Player Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1420))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) AccentGreen else WarningOrange)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scene ${currentScene.sceneNumber}/${scenes.size}: ${currentScene.actionType.name}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${currentScene.cameraMotion.name} • ${currentScene.animationMotion.name}",
                    fontSize = 11.sp,
                    color = NeonCyan
                )
            }
        }

        // Live Animated Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(Color(0xFF05070A))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("manhwa_canvas_viewport")
            ) {
                drawManhwaScene(
                    scene = currentScene,
                    renderState = renderState,
                    activeSpeaker = activeSpeaker
                )
            }

            // Overlay Subtitles & Sound Effect Badges
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Right: SFX Tag
                if (currentScene.sfxName != "NONE" && sceneElapsedMs in 800L..2500L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xE6FF007F))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ SFX: ${currentScene.sfxName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                // Bottom: Subtitle Box
                val subtitleText = currentScene.dialogueText ?: currentScene.narrationText
                if (subtitleText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xD9000000))
                            .border(1.dp, if (currentScene.dialogueText != null) ElectricPink else NeonCyan, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column {
                            if (activeSpeaker != null && currentScene.dialogueText != null) {
                                Text(
                                    text = "🗣️ ${activeSpeaker.name}:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricPink
                                )
                            }
                            Text(
                                text = subtitleText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Timeline Progress Bar
        val progress = (sceneElapsedMs.toFloat() / totalSceneMs).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = NeonCyan,
            trackColor = Color(0xFF1E293B)
        )

        // Player Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        sceneElapsedMs = 0L
                        currentSceneIndex = if (currentSceneIndex > 0) currentSceneIndex - 1 else scenes.size - 1
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Scene", tint = TextPrimary)
                }

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/Pause",
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        sceneElapsedMs = 0L
                        currentSceneIndex = (currentSceneIndex + 1) % scenes.size
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Scene", tint = TextPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${(sceneElapsedMs / 1000f).toInt()}s / ${(totalSceneMs / 1000f).toInt()}s",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentScene.dialogueText != null) {
                    Badge(containerColor = ElectricPink.copy(alpha = 0.2f)) {
                        Text(
                            text = "Active LipSync: ${renderState.activeViseme.name}",
                            fontSize = 10.sp,
                            color = ElectricPink,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing functions for stylized Manhwa artwork, speed lines, auras, and visemes.
 */
private fun DrawScope.drawManhwaScene(
    scene: ManhwaScene,
    renderState: AnimationRenderState,
    activeSpeaker: ManhwaCharacter?
) {
    val canvasW = size.width
    val canvasH = size.height

    // Apply Camera Transform
    scale(
        scaleX = renderState.scale,
        scaleY = renderState.scale,
        pivot = Offset(canvasW * 0.5f, canvasH * 0.5f)
    ) {
        translate(
            left = renderState.offsetX * canvasW,
            top = renderState.offsetY * canvasH
        ) {
            rotate(
                degrees = renderState.rotationDeg,
                pivot = Offset(canvasW * 0.5f, canvasH * 0.5f)
            ) {
                // 1. Background Manhwa Layer (Dramatic dark gradient)
                val isDarkFantasy = true
                val bgBrush = Brush.linearGradient(
                    colors = if (isDarkFantasy) {
                        listOf(Color(0xFF0F0C1B), Color(0xFF1E1435), Color(0xFF080611))
                    } else {
                        listOf(Color(0xFF101827), Color(0xFF1E293B), Color(0xFF0B1120))
                    },
                    start = Offset(0f, 0f),
                    end = Offset(canvasW, canvasH)
                )
                drawRect(brush = bgBrush, size = size)

                // 2. Parallax Midground Layer (Ancient dungeon pillars or city ruins)
                val pillarPaint = Color(0x336B21A8)
                drawRect(
                    color = pillarPaint,
                    topLeft = Offset(canvasW * 0.1f + renderState.hairSwayPx * 0.5f, 0f),
                    size = Size(canvasW * 0.15f, canvasH)
                )
                drawRect(
                    color = pillarPaint,
                    topLeft = Offset(canvasW * 0.75f - renderState.hairSwayPx * 0.5f, 0f),
                    size = Size(canvasW * 0.18f, canvasH)
                )

                // 3. Speed Lines Burst (if active)
                if (renderState.speedLineAlpha > 0.05f) {
                    val centerX = canvasW * 0.5f
                    val centerY = canvasH * 0.55f
                    val lineCount = 36
                    for (i in 0 until lineCount) {
                        val angle = (i.toFloat() / lineCount) * 2f * Math.PI.toFloat()
                        val startR = canvasW * 0.35f
                        val endR = canvasW * 0.8f
                        val sx = centerX + cos(angle) * startR
                        val sy = centerY + sin(angle) * startR
                        val ex = centerX + cos(angle) * endR
                        val ey = centerY + sin(angle) * endR
                        drawLine(
                            color = Color(0xFFFFFFFF).copy(alpha = renderState.speedLineAlpha * 0.35f),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 2.5f
                        )
                    }
                }

                // 4. Character Silhouette / Artwork in Center
                val charCenterX = canvasW * 0.5f
                val charCenterY = canvasH * 0.52f

                // Dark Mist / Aura Particle Glow
                if (scene.animationMotion == AnimationMotionType.DARK_AURA_MIST || scene.actionType == ActionType.TRANSFORMING) {
                    val auraColor = Color(0x7F9333EA)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f * renderState.auraIntensity), Color.Transparent),
                            center = Offset(charCenterX, charCenterY),
                            radius = canvasW * 0.45f
                        ),
                        radius = canvasW * 0.45f,
                        center = Offset(charCenterX, charCenterY)
                    )
                }

                // Character Coat / Body (High contrast ink silhouette)
                val bodyColor = Color(0xFF1E1B4B)
                drawOval(
                    color = bodyColor,
                    topLeft = Offset(charCenterX - canvasW * 0.22f, charCenterY - canvasH * 0.1f + renderState.clothingWavePx),
                    size = Size(canvasW * 0.44f, canvasH * 0.6f)
                )

                // Character Head / Face
                val skinColor = Color(0xFFE2E8F0)
                val headRadius = canvasW * 0.11f
                val headCenterY = charCenterY - canvasH * 0.18f
                drawCircle(
                    color = skinColor,
                    radius = headRadius,
                    center = Offset(charCenterX, headCenterY)
                )

                // Character Hair (Animated sway)
                val hairColor = Color(0xFF09090B)
                val hairPath = Path().apply {
                    moveTo(charCenterX - headRadius * 1.1f + renderState.hairSwayPx, headCenterY - headRadius * 0.4f)
                    lineTo(charCenterX - headRadius * 0.5f, headCenterY - headRadius * 1.3f)
                    lineTo(charCenterX, headCenterY - headRadius * 1.1f)
                    lineTo(charCenterX + headRadius * 0.6f + renderState.hairSwayPx, headCenterY - headRadius * 1.4f)
                    lineTo(charCenterX + headRadius * 1.1f, headCenterY - headRadius * 0.3f)
                    lineTo(charCenterX + headRadius * 0.8f, headCenterY - headRadius * 0.1f)
                    close()
                }
                drawPath(path = hairPath, color = hairColor)

                // Eyes (Glowing Violet / Cyan with Blinking Animation)
                val eyeY = headCenterY - headRadius * 0.15f
                val leftEyeX = charCenterX - headRadius * 0.4f
                val rightEyeX = charCenterX + headRadius * 0.4f

                if (renderState.isEyeBlinking) {
                    // Closed eye slit line
                    drawLine(color = Color(0xFF0F172A), start = Offset(leftEyeX - 10f, eyeY), end = Offset(leftEyeX + 10f, eyeY), strokeWidth = 3f)
                    drawLine(color = Color(0xFF0F172A), start = Offset(rightEyeX - 10f, eyeY), end = Offset(rightEyeX + 10f, eyeY), strokeWidth = 3f)
                } else {
                    // Glowing Awakened Pupil
                    val glowEyeColor = Color(0xFFC084FC)
                    drawCircle(color = glowEyeColor, radius = 7f, center = Offset(leftEyeX, eyeY))
                    drawCircle(color = glowEyeColor, radius = 7f, center = Offset(rightEyeX, eyeY))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(leftEyeX - 1.5f, eyeY - 1.5f))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(rightEyeX - 1.5f, eyeY - 1.5f))
                }

                // 5. Active Speaker Mouth / Lip-Sync Viseme
                val mouthY = headCenterY + headRadius * 0.45f
                val mouthW = 24f + (renderState.mouthOpenRatio * 16f)
                val mouthH = 3f + (renderState.mouthOpenRatio * 18f)

                when (renderState.activeViseme) {
                    VisemeShape.REST -> {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(charCenterX - 12f, mouthY),
                            end = Offset(charCenterX + 12f, mouthY),
                            strokeWidth = 3f
                        )
                    }
                    VisemeShape.O_OH, VisemeShape.U_OO -> {
                        drawOval(
                            color = Color(0xFF450A0A),
                            topLeft = Offset(charCenterX - mouthW * 0.35f, mouthY - mouthH * 0.5f),
                            size = Size(mouthW * 0.7f, mouthH * 1.2f)
                        )
                    }
                    else -> {
                        drawOval(
                            color = Color(0xFF450A0A),
                            topLeft = Offset(charCenterX - mouthW * 0.5f, mouthY - mouthH * 0.5f),
                            size = Size(mouthW, mouthH)
                        )
                    }
                }

                // 6. Double Daggers / Energy Slash (if in combat stance)
                if (scene.actionType == ActionType.ATTACKING || scene.actionType == ActionType.TRANSFORMING) {
                    val bladeColor = Color(0xFF38BDF8)
                    drawLine(
                        color = bladeColor,
                        start = Offset(charCenterX - canvasW * 0.25f, charCenterY + canvasH * 0.05f),
                        end = Offset(charCenterX - canvasW * 0.40f, charCenterY - canvasH * 0.15f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFA855F7),
                        start = Offset(charCenterX + canvasW * 0.25f, charCenterY + canvasH * 0.05f),
                        end = Offset(charCenterX + canvasW * 0.40f, charCenterY - canvasH * 0.15f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
