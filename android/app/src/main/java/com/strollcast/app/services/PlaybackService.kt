package com.strollcast.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.strollcast.app.MainActivity
import com.strollcast.app.R
import com.strollcast.app.utils.AudioFeedbackManager
import com.strollcast.app.utils.CommandType
import com.strollcast.app.utils.FeedbackMessages
import com.strollcast.app.utils.VoiceCommandParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var audioFeedback: AudioFeedbackManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val CHANNEL_ID = "playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_SKIP_FORWARD = "com.strollcast.app.SKIP_FORWARD_15"
        private const val ACTION_SKIP_BACKWARD = "com.strollcast.app.SKIP_BACKWARD_15"

        // Voice command constants
        const val ACTION_VOICE_COMMAND = "com.strollcast.app.VOICE_COMMAND"
        const val EXTRA_COMMAND_TYPE = "command_type"
    }

    enum class VoiceCommandType {
        PLAY_REFERENCE,
        PLAY_PREVIOUS
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for foreground service
        createNotificationChannel()

        // Initialize AudioFeedbackManager for voice commands
        audioFeedback = AudioFeedbackManager(this)

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Add listener to log playback state changes
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                android.util.Log.d("PlaybackService", "Player isPlaying changed: $isPlaying")
            }
        })

        // Create MediaSession with custom callback
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
    }

    /**
     * Custom MediaSession.Callback to handle playback commands and voice control
     */
    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            android.util.Log.d("PlaybackService", "MediaSession onConnect from: ${controller.packageName}")
            val connectionResult = super.onConnect(session, controller)

            // Add custom commands for skip forward/backward
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_SKIP_FORWARD, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SKIP_BACKWARD, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_SKIP_FORWARD -> {
                    skipForward()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SKIP_BACKWARD -> {
                    skipBackward()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun sendVoiceCommand(type: VoiceCommandType) {
        val intent = Intent(ACTION_VOICE_COMMAND).apply {
            putExtra(EXTRA_COMMAND_TYPE, type.name)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Skip forward 15 seconds
     */
    private fun skipForward() {
        val newPosition = (player.currentPosition + 15000).coerceAtMost(player.duration)
        player.seekTo(newPosition)
    }

    /**
     * Skip backward 15 seconds
     */
    private fun skipBackward() {
        val newPosition = (player.currentPosition - 15000).coerceAtLeast(0)
        player.seekTo(newPosition)
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Strollcast")
            .setContentText("Ready for playback")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Update MediaSession metadata for "what's playing" queries
     * This should be called when a new episode is loaded
     */
    fun updateMetadata(title: String, artist: String? = null, artworkUri: String? = null) {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)

        artist?.let { metadataBuilder.setArtist(it) }
        artworkUri?.let { metadataBuilder.setArtworkUri(android.net.Uri.parse(it)) }

        player.setMediaItem(
            player.currentMediaItem?.buildUpon()
                ?.setMediaMetadata(metadataBuilder.build())
                ?.build() ?: return
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        audioFeedback.shutdown()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
