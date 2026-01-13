package com.strollcast.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.strollcast.app.viewmodels.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Invisible activity that handles voice commands from Google Assistant
 * via App Shortcuts, then immediately finishes and delegates to MainActivity
 */
@AndroidEntryPoint
class VoiceCommandActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Process the voice command
        val commandType = intent.getStringExtra("command_type")

        lifecycleScope.launch {
            when (commandType) {
                "PLAY_REFERENCE" -> {
                    playerViewModel.playNextReference()
                }
                "PLAY_PREVIOUS" -> {
                    playerViewModel.playPreviousEpisode()
                }
            }

            // Launch main activity to show the result
            val mainIntent = Intent(this@VoiceCommandActivity, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(mainIntent)

            // Finish this invisible activity
            finish()
        }
    }
}
