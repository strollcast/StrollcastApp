package com.strollcast.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.strollcast.app.services.PlaybackService
import com.strollcast.app.ui.StrollcastApp
import com.strollcast.app.ui.theme.StrollcastTheme
import com.strollcast.app.viewmodels.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    private val voiceCommandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(PlaybackService.EXTRA_COMMAND_TYPE)) {
                PlaybackService.VoiceCommandType.PLAY_REFERENCE.name -> {
                    playerViewModel.playNextReference()
                }
                PlaybackService.VoiceCommandType.PLAY_PREVIOUS.name -> {
                    // Will be implemented in Phase 4 (T022)
                    // playerViewModel.playPreviousEpisode()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrollcastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StrollcastApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            voiceCommandReceiver,
            IntentFilter(PlaybackService.ACTION_VOICE_COMMAND)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceCommandReceiver)
    }
}
