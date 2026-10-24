package com.example.prototyx.ui.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.ConsultResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    var isListening by mutableStateOf(false)
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    var isThinking by mutableStateOf(false)
        private set

    var recognizedText by mutableStateOf("")
        private set

    var responseText by mutableStateOf<String?>(null)
        private set

    var targetAsset by mutableStateOf<String?>(null)
        private set

    var statusMessage by mutableStateOf("FINNY Co-Pilot ready. Tap microphone to speak.")
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initSpeechRecognizer()
        tts = TextToSpeech(context, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceAssistantManager)
            }
        } else {
            statusMessage = "Speech recognition not supported on this device."
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.0f)
        }
    }

    fun startListening() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            statusMessage = "Microphone permission required. Tap mic again after granting permission."
            return
        }

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        stopSpeaking()
        recognizedText = ""
        isListening = true
        statusMessage = "Listening for market query or asset command..."

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            statusMessage = "Unable to start voice capture: ${e.message}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
    }

    fun queryAssistant(query: String, repository: DataRepository) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        recognizedText = cleanQuery
        isThinking = true
        statusMessage = "Consulting Investment Committee AI for: \"$cleanQuery\"..."

        coroutineScope.launch {
            try {
                val result: ConsultResponse = repository.runConsult(cleanQuery)
                withContext(Dispatchers.Main) {
                    isThinking = false
                    targetAsset = result.asset ?: "PORTFOLIO"
                    val rec = result.recommendation ?: "Assessment complete. No anomalies detected."
                    responseText = rec
                    statusMessage = "Verdict delivered by Committee Orchestrator."
                    speak(rec)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isThinking = false
                    val errorMsg = "Orchestrator offline: ${e.message ?: "Failed to connect to /api/agents/consult"}"
                    responseText = errorMsg
                    statusMessage = "Query completed with warning."
                    speak("I was unable to consult the committee. Please check your backend network.")
                }
            }
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || tts == null) return
        stopSpeaking()
        isSpeaking = true

        // Clean speech text for natural listening (remove Markdown tokens like **, ###, etc.)
        val cleanSpeech = text
            .replace(Regex("[#*_`>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "FINNY_TTS_UTTERANCE")
    }

    fun stopSpeaking() {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
        }
    }

    fun clear() {
        stopSpeaking()
        stopListening()
        recognizedText = ""
        responseText = null
        targetAsset = null
        statusMessage = "FINNY Co-Pilot ready. Tap microphone to speak."
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }

    // SpeechRecognizer Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        statusMessage = "Listening... Speak clearly."
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        isListening = false
        statusMessage = "Analyzing audio signal..."
    }

    override fun onError(error: Int) {
        isListening = false
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection required"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic to retry."
            else -> "Recognition error: code $error"
        }
        statusMessage = message
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            recognizedText = text
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            recognizedText = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
