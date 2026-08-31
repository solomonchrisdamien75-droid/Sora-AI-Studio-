package com.example.ai.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.inference.AIInferenceManager
import com.example.ai.inference.AIInferenceRequest
import com.example.ai.inference.InferenceEngineManager
import com.example.ai.inference.model.ModelCapability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocalAIOrchestratorTest {
    @Test
    fun `generateText rejects inference when no local model is loaded`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inferenceManager = AIInferenceManager(context, InferenceEngineManager(context))

        val result = inferenceManager.generateText(
            AIInferenceRequest(
                prompt = "hello",
                requiredCapability = ModelCapability.CHAT,
                maxTokens = 64,
                temperature = 0.7f
            )
        )

        assertTrue("Expected a failure when no model is loaded", result.isFailure)
        val message = result.exceptionOrNull()?.message ?: ""
        assertTrue("Expected explicit local-model guard", message.contains("NO LOCAL MODEL LOADED", ignoreCase = true))
        assertFalse("Must not fallback to fake response text", message.contains("I am", ignoreCase = true))
    }
}
