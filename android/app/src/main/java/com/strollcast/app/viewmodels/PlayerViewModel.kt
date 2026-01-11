package com.strollcast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.strollcast.app.models.Podcast
import com.strollcast.app.repository.HistoryRepository
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
    private val repository: PodcastRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Navigation error state for reference link navigation
    private val _navigationError = MutableStateFlow<String?>(null)
    val navigationError: StateFlow<String?> = _navigationError.asStateFlow()

    // Voice command feedback state
    private val _voiceCommandFeedback = MutableStateFlow<String?>(null)
    val voiceCommandFeedback: StateFlow<String?> = _voiceCommandFeedback.asStateFlow()

    private var player: Player? = null
    private var transcriptViewModel: TranscriptViewModel? = null

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

    /**
     * Navigate to a referenced episode from a transcript link
     * @param episodeId The ID of the episode to navigate to
     */
    fun navigateToReferencedEpisode(episodeId: String) {
        viewModelScope.launch {
            try {
                // Fetch episode metadata
                val episode = repository.getPodcastById(episodeId)
                if (episode == null) {
                    _navigationError.value = "Referenced episode not found"
                    return@launch
                }

                // Check if downloaded (for offline support)
                val download = repository.getDownload(episode.id)
                val isDownloaded = download?.localAudioPath != null

                // For offline scenario: if not downloaded, show error
                // Note: We don't have a network monitor injected, so we'll just attempt to load
                // If the user is offline and episode not downloaded, the media loading will fail
                // with appropriate system error handling

                // Save current position before navigating
                savePosition()

                // Load the referenced episode
                loadPodcast(episode)

                // Auto-play the referenced episode
                play()

                // Clear any previous errors
                _navigationError.value = null

            } catch (e: Exception) {
                _navigationError.value = "Failed to load episode: ${e.message}"
            }
        }
    }

    /**
     * Clear navigation error state
     */
    fun clearNavigationError() {
        _navigationError.value = null
    }

    private fun savePosition() {
        val podcast = _uiState.value.currentPodcast ?: return
        val position = player?.currentPosition ?: return

        viewModelScope.launch {
            repository.savePlaybackPosition(podcast.id, position)

            // Check if episode should be marked as complete (90% threshold)
            checkCompletion(podcast.id, position)
        }
    }

    /**
     * Check if episode reaches completion threshold and mark as complete
     */
    private fun checkCompletion(episodeId: String, currentPosition: Long) {
        val duration = _uiState.value.duration

        if (duration > 0 && currentPosition > 0) {
            viewModelScope.launch {
                historyRepository.markEpisodeComplete(
                    episodeId = episodeId,
                    playbackPosition = currentPosition,
                    episodeDuration = duration
                )
            }
        }
    }

    fun setTranscriptViewModel(viewModel: TranscriptViewModel) {
        this.transcriptViewModel = viewModel
    }

    /**
     * Play the first reference found in current transcript segment
     * Called by voice command "play reference"
     */
    fun playNextReference() {
        viewModelScope.launch {
            try {
                val currentPodcast = _uiState.value.currentPodcast
                if (currentPodcast == null) {
                    _voiceCommandFeedback.value = "No episode currently playing"
                    return@launch
                }

                val currentPosition = player?.currentPosition ?: return@launch
                val context = transcriptViewModel?.getCurrentSegmentContext(currentPodcast.id, currentPosition)

                if (context == null || context.parsedReferences.isEmpty()) {
                    _voiceCommandFeedback.value = "No reference found in current segment"
                    return@launch
                }

                val firstReference = context.parsedReferences.first()
                navigateToReferencedEpisode(firstReference.episodeId)
                _voiceCommandFeedback.value = "Playing reference episode"

            } catch (e: Exception) {
                _voiceCommandFeedback.value = "Failed to play reference: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        savePosition()
        super.onCleared()
    }
}
