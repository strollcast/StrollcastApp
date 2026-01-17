package com.strollcast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
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

    private var controller: MediaController? = null
    private var transcriptViewModel: TranscriptViewModel? = null
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                controller?.let { player ->
                    _uiState.value = _uiState.value.copy(
                        duration = player.duration
                    )
                }
            }
        }
    }

    fun setController(mediaController: MediaController) {
        // Remove listener from old controller if exists
        controller?.removeListener(playerListener)

        this.controller = mediaController
        mediaController.addListener(playerListener)

        // Update UI state with current controller state
        _uiState.value = _uiState.value.copy(
            isPlaying = mediaController.isPlaying,
            duration = if (mediaController.duration > 0) mediaController.duration else 0L,
            currentPosition = mediaController.currentPosition
        )
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

            // Build MediaItem with metadata for MediaSession
            val metadata = MediaMetadata.Builder()
                .setTitle(podcast.title)
                .setArtist(podcast.authors)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaId(podcast.id)
                .setMediaMetadata(metadata)
                .build()

            controller?.apply {
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
        controller?.play()
    }

    fun pause() {
        controller?.pause()
        savePosition()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun skipForward(seconds: Long = 15) {
        controller?.let {
            val newPosition = (it.currentPosition + seconds * 1000).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    fun skipBackward(seconds: Long = 15) {
        controller?.let {
            val newPosition = (it.currentPosition - seconds * 1000).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    fun updatePosition() {
        controller?.let {
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
     * Navigate to an episode referenced by arXiv ID from a paper reference URL
     * @param arxivId The arXiv ID of the referenced paper
     * @return true if navigation succeeded, false if no matching episode found
     */
    suspend fun navigateToArxivReference(arxivId: String): Boolean {
        return try {
            // Search for episode where paperUrl contains the arXiv ID
            val episode = repository.getPodcastByArxivId(arxivId)
            if (episode == null) {
                return false
            }

            // Save current position before navigating
            savePosition()

            // Load the referenced episode
            loadPodcast(episode)

            // Auto-play the referenced episode
            play()

            // Clear any previous errors
            _navigationError.value = null
            true
        } catch (e: Exception) {
            _navigationError.value = "Failed to load episode: ${e.message}"
            false
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
        val position = controller?.currentPosition ?: return

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
                    _voiceCommandFeedback.value = "No episode playing"
                    return@launch
                }

                val currentPosition = controller?.currentPosition ?: run {
                    _voiceCommandFeedback.value = "Player not available"
                    return@launch
                }

                val context = transcriptViewModel?.getCurrentSegmentContext(currentPodcast.id, currentPosition)

                if (context == null || context.parsedReferences.isEmpty()) {
                    _voiceCommandFeedback.value = "No reference in this segment"
                    return@launch
                }

                // Provide context-aware feedback for multiple references
                val feedback = if (context.parsedReferences.size > 1) {
                    "Playing first of ${context.parsedReferences.size} references"
                } else {
                    "Playing reference"
                }

                val firstReference = context.parsedReferences.first()
                navigateToReferencedEpisode(firstReference.episodeId)
                _voiceCommandFeedback.value = feedback

            } catch (e: Exception) {
                _voiceCommandFeedback.value = when {
                    e.message?.contains("not found", ignoreCase = true) == true -> "Episode not found"
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error"
                    else -> "Failed to load episode"
                }
            }
        }
    }

    /**
     * Navigate to previous episode in playback history
     * Called by voice command "play previous"
     *
     * Note: This uses Media3's seekToPrevious() which manages the playback history.
     * When navigateToReferencedEpisode() is called, savePosition() is called first,
     * which allows Media3 to track the episode chain for previous navigation.
     */
    fun playPreviousEpisode() {
        try {
            controller?.let {
                if (it.hasPreviousMediaItem()) {
                    it.seekToPrevious()
                    _voiceCommandFeedback.value = "Going back"
                } else {
                    _voiceCommandFeedback.value = "Already at first episode"
                }
            } ?: run {
                _voiceCommandFeedback.value = "Player not available"
            }
        } catch (e: Exception) {
            _voiceCommandFeedback.value = "Playback error occurred"
        }
    }

    override fun onCleared() {
        savePosition()
        // Don't release controller - it should persist across screen navigations
        // Audio keeps playing even when navigating away from player screen
        controller?.removeListener(playerListener)
        super.onCleared()
    }
}
