package com.strollcast.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Manages TTS audio feedback for voice commands
 */
class AudioFeedbackManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
            }
        }
    }

    fun speak(
        message: String,
        priority: FeedbackPriority = FeedbackPriority.NORMAL
    ) {
        if (!isInitialized) return

        val queueMode = when (priority) {
            FeedbackPriority.LOW -> TextToSpeech.QUEUE_ADD
            FeedbackPriority.NORMAL -> TextToSpeech.QUEUE_FLUSH
            FeedbackPriority.HIGH -> TextToSpeech.QUEUE_FLUSH
        }

        tts?.speak(message, queueMode, null, "voice_feedback_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}

enum class FeedbackPriority {
    LOW,      // Can be interrupted
    NORMAL,   // Standard feedback
    HIGH      // Must complete
}

object FeedbackMessages {
    const val NO_REFERENCE = "No reference found in current segment"
    const val NO_PREVIOUS = "Already at first episode"
    const val PLAYING_REFERENCE = "Playing reference episode"
    const val GOING_BACK = "Going back to previous episode"
    const val INVALID_TIMESTAMP = "Could not understand timestamp"
    const val COMMAND_NOT_RECOGNIZED = "Command not recognized. Try play reference or play previous"
}
