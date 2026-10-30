package com.personal.daymark.core.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages native on-device and offline-preferred Android speech recognition.
 */
class VoiceSpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<VoiceSpeechState>(VoiceSpeechState.Idle)
    val state: StateFlow<VoiceSpeechState> = _state.asStateFlow()

    /**
     * Checks whether speech recognition services are installed on this device.
     */
    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Checks if strictly on-device offline recognition is available (Android 12+).
     */
    fun isOnDeviceAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            false
        }
    }

    /**
     * Starts listening to user voice input.
     */
    fun startListening() {
        stopListening()

        // Prefer on-device speech recognizer for zero latency & offline privacy
        speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceSpeechState.Listening(rmsDb = 0f, partialText = "")
            }

            override fun onBeginningOfSpeech() {
                _state.value = VoiceSpeechState.Listening(rmsDb = 1f, partialText = "")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val current = _state.value
                if (current is VoiceSpeechState.Listening) {
                    _state.value = current.copy(rmsDb = rmsdB.coerceAtLeast(0f))
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                val current = _state.value
                val partial = if (current is VoiceSpeechState.Listening) current.partialText else ""
                _state.value = VoiceSpeechState.Processing(rawText = partial)
            }

            override fun onError(errorCode: Int) {
                val errorMessage = when (errorCode) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network unavailable for speech service"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that, please tap mic to try again"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone is busy, please try again"
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Could not recognize speech, tap to retry"
                }
                _state.value = VoiceSpeechState.Error(message = errorMessage, canRetry = true)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim()
                if (!recognizedText.isNullOrBlank()) {
                    _state.value = VoiceSpeechState.Success(rawText = recognizedText)
                } else {
                    _state.value = VoiceSpeechState.Error(message = "Didn't hear any words, tap to retry", canRetry = true)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim()
                if (!partial.isNullOrBlank()) {
                    val currentRms = (_state.value as? VoiceSpeechState.Listening)?.rmsDb ?: 0f
                    _state.value = VoiceSpeechState.Listening(rmsDb = currentRms, partialText = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = VoiceSpeechState.Error(message = e.localizedMessage ?: "Failed to start listening")
        }
    }

    /**
     * Stops listening and finalizes transcription.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore stop errors
        }
    }

    /**
     * Resets state back to idle.
     */
    fun reset() {
        cancelListening()
        _state.value = VoiceSpeechState.Idle
    }

    /**
     * Cancels any active recognition immediately.
     */
    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore cancel errors
        } finally {
            speechRecognizer = null
        }
    }

    fun destroy() {
        cancelListening()
        _state.value = VoiceSpeechState.Idle
    }
}

