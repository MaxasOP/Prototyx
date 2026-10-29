package com.example.prototyx.ui.main

import android.content.Intent
import android.speech.SpeechRecognizer
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prototyx.ui.voice.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test

class VoiceAvailabilityTest {
    @Test fun installedRecognitionProviderIsVisibleToTheApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue("Recognition service must be visible from the target app", context.packageManager.queryIntentServices(Intent("android.speech.RecognitionService"), 0).isNotEmpty())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        instrumentation.runOnMainSync {
            assertTrue(SpeechRecognizer.isRecognitionAvailable(context))
            val manager = VoiceAssistantManager(context, scope)
            try {
                assertTrue(manager.isVoiceAvailable)
                assertEquals("Ask a question by voice or text.", manager.statusMessage)
            } finally { manager.destroy(); scope.cancel() }
        }
    }
}
