package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.editor.MediaClipTrack
import com.example.editor.VideoEditorEngine
import com.example.editor.VideoEditorProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sora AI Studio", appName)
  }

  @Test
  fun `test video timeline engine frame generation and duration adjust`() {
    val engine = VideoEditorEngine()
    val frames = engine.generateDefaultFramesForClip("clip_test", 4000L, "Action Sequence")
    assertTrue(frames.isNotEmpty())
    assertEquals(6, frames.size)
    assertEquals(1, frames.first().frameIndex)
    assertEquals(6, frames.last().frameIndex)
  }

  @Test
  fun `test clip split and reverse functionality`() {
    val engine = VideoEditorEngine()
    val clip = MediaClipTrack(
        id = "test_clip",
        title = "Test Clip",
        filePath = "renders/test.mp4",
        startMs = 0L,
        endMs = 6000L,
        durationMs = 6000L,
        frames = engine.generateDefaultFramesForClip("test_clip", 6000L, "Test")
    )
    val project = VideoEditorProject(
        id = "test_project",
        name = "Test Project",
        videoClips = listOf(clip)
    )
    val (partA, partB) = engine.splitClip(clip, 3000L)
    assertEquals(3000L, partA.durationMs)
    assertEquals(3000L, partB.durationMs)
    assertNotNull(partA.frames)
    assertNotNull(partB.frames)
  }

  @Test
  fun `test batch job creation request parameters`() {
    val req = com.example.ai.queue.BatchJobCreationRequest(
        titlePrefix = "Batch Sequence",
        prompts = listOf("Prompt 1", "Prompt 2", "Prompt 3"),
        generationType = "TEXT_TO_VIDEO",
        mode = "FAST",
        durationSeconds = 10,
        resolution = "1080p",
        fps = 24
    )
    assertEquals(3, req.prompts.size)
    assertEquals(10, req.durationSeconds)
    assertEquals("1080p", req.resolution)
    assertEquals("FAST", req.mode)
  }
}

