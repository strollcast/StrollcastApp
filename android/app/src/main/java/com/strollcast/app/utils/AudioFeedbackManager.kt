package com.strollcast.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manages TTS audio feedback for voice commands with enhanced edge case handling
 *
 * Features:
 * - Concise feedback (<3 seconds per message)
 * - Rate limiting to prevent feedback spam
 * - Graceful degradation when TTS unavailable
 * - Progress tracking for debugging
 */
class AudioFeedbackManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var lastFeedbackTime = 0L
    private val minFeedbackInterval = 500L // Prevent spam

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                // Set speech rate slightly faster for concise feedback
                tts?.setSpeechRate(1.1f)
                tts?.setPitch(1.0f)

                // Add progress listener for debugging and timeout handling
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "Feedback started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "Feedback completed: $utteranceId")
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "Feedback error: $utteranceId")
                    }
                })

                isInitialized = true
                Log.d(TAG, "AudioFeedbackManager initialized")
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }

    /**
     * Speak a feedback message with priority and rate limiting
     *
     * @param message The message to speak (should be concise, <20 words)
     * @param priority Priority level affecting queue behavior
     */
    fun speak(
        message: String,
        priority: FeedbackPriority = FeedbackPriority.NORMAL
    ) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak: $message")
            return
        }

        // Rate limiting: prevent feedback spam
        val now = System.currentTimeMillis()
        if (priority != FeedbackPriority.HIGH && now - lastFeedbackTime < minFeedbackInterval) {
            Log.d(TAG, "Rate limited feedback: $message")
            return
        }
        lastFeedbackTime = now

        val queueMode = when (priority) {
            FeedbackPriority.LOW -> TextToSpeech.QUEUE_ADD
            FeedbackPriority.NORMAL -> TextToSpeech.QUEUE_FLUSH
            FeedbackPriority.HIGH -> TextToSpeech.QUEUE_FLUSH
        }

        val utteranceId = "voice_feedback_${System.currentTimeMillis()}"
        tts?.speak(message, queueMode, null, utteranceId)
        Log.d(TAG, "Speaking: $message (priority: $priority)")
    }

    /**
     * Stop current speech immediately
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Check if TTS is ready for use
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Shutdown TTS engine and release resources
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down AudioFeedbackManager")
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "AudioFeedbackManager"
    }
}

enum class FeedbackPriority {
    LOW,      // Can be interrupted
    NORMAL,   // Standard feedback
    HIGH      // Must complete
}

/**
 * Standardized feedback messages for voice commands
 * All messages are concise (<20 words) to keep feedback under 3 seconds
 */
object FeedbackMessages {
    // Success messages
    const val PLAYING_REFERENCE = "Playing reference"
    const val PLAYING_FIRST_REFERENCE = "Playing first reference"
    const val GOING_BACK = "Going back"
    const val SEEKING_TO = "Seeking"

    // Error messages - No content
    const val NO_REFERENCE = "No reference in this segment"
    const val NO_PREVIOUS = "Already at first episode"
    const val NO_EPISODE_PLAYING = "No episode playing"

    // Error messages - Network/Loading
    const val EPISODE_NOT_FOUND = "Episode not found"
    const val LOADING_FAILED = "Failed to load episode"
    const val NETWORK_ERROR = "Network error, check connection"

    // Error messages - Player state
    const val NO_PLAYER = "Player not available"
    const val PLAYER_ERROR = "Playback error occurred"

    // Error messages - Invalid input
    const val INVALID_TIMESTAMP = "Invalid timestamp"
    const val COMMAND_NOT_RECOGNIZED = "Command not recognized"

    // Help and discovery
    const val HELP_MESSAGE = "Try: play reference, or play previous"
    const val AVAILABLE_COMMANDS = "Available commands: play reference, play previous"

    /**
     * Get contextual message for multiple references
     */
    fun multipleReferences(count: Int): String = "Playing first of $count references"

    /**
     * Get formatted timestamp message
     */
    fun seekingTo(minutes: Long, seconds: Long): String {
        return when {
            minutes == 0L -> "Seeking to $seconds seconds"
            seconds == 0L -> "Seeking to $minutes minutes"
            else -> "Seeking to $minutes minutes $seconds seconds"
        }
    }

    /**
     * Get error message with context
     */
    fun errorWithContext(operation: String, error: String): String {
        return "$operation failed: $error"
    }
}
