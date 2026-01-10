package com.strollcast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.strollcast.app.models.Podcast
import com.strollcast.app.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentPodcast: Podcast? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PodcastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var player: Player? = null

    fun setPlayer(player: Player) {
        this.player = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.value = _uiState.value.copy(
                        duration = player.duration
                    )
                }
            }
        })
    }

    fun loadPodcastById(podcastId: String) {
        viewModelScope.launch {
            val podcast = repository.getPodcastById(podcastId)
            if (podcast != null) {
                loadPodcast(podcast)
            }
        }
    }

    fun loadPodcast(podcast: Podcast) {
        viewModelScope.launch {
            val download = repository.getDownload(podcast.id)
            val mediaUri = download?.localAudioPath ?: podcast.audioUrl

            if (mediaUri == null) {
                return@launch
            }

            val mediaItem = MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaId(podcast.id)
                .build()

            player?.apply {
                setMediaItem(mediaItem)
                prepare()

                // Restore last position
                val lastPosition = repository.getLastPosition(podcast.id)
                if (lastPosition != null && lastPosition > 0) {
                    seekTo(lastPosition)
                }
            }

            _uiState.value = _uiState.value.copy(currentPodcast = podcast)
        }
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
        savePosition()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun skipForward(seconds: Long = 15) {
        player?.let {
            val newPosition = (it.currentPosition + seconds * 1000).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    fun skipBackward(seconds: Long = 15) {
        player?.let {
            val newPosition = (it.currentPosition - seconds * 1000).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    fun updatePosition() {
        player?.let {
            _uiState.value = _uiState.value.copy(currentPosition = it.currentPosition)
        }
    }

    private fun savePosition() {
        val podcast = _uiState.value.currentPodcast ?: return
        val position = player?.currentPosition ?: return

        viewModelScope.launch {
            repository.savePlaybackPosition(podcast.id, position)
        }
    }

    override fun onCleared() {
        savePosition()
        super.onCleared()
    }
}
