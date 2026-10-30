package com.personal.daymark.core.voice

/**
 * Represents the live state of the speech recognition engine.
 */
sealed class VoiceSpeechState {
    object Idle : VoiceSpeechState()

    /**
     * Active microphone listening state.
     * @param rmsDb Current audio input decibel level (used to drive animated waveforms / pulsing rings)
     * @param partialText Streaming live transcription before speech pause
     */
    data class Listening(
        val rmsDb: Float = 0f,
        val partialText: String = ""
    ) : VoiceSpeechState()

    /**
     * User has finished speaking and audio is being finalized.
     */
    data class Processing(val rawText: String) : VoiceSpeechState()

    /**
     * Transcription successfully completed.
     */
    data class Success(val rawText: String) : VoiceSpeechState()

    /**
     * Recognition error with localized user-friendly message.
     */
    data class Error(val message: String, val canRetry: Boolean = true) : VoiceSpeechState()
}

