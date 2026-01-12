package com.strollcast.app.viewmodels

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strollcast.app.models.TranscriptCue
import com.strollcast.app.repository.TranscriptRepository
import com.strollcast.app.utils.EpisodeUrlParser
import com.strollcast.app.utils.MarkdownLinkParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranscriptUiState(
    val transcript: List<TranscriptCue> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLineIndex: Int? = null
)

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptRepository: TranscriptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    /**
     * Load transcript for an episode
     *
     * @param episodeId Episode ID
     * @param vttUrl Optional VTT file URL
     */
    fun loadTranscript(episodeId: String, vttUrl: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            transcriptRepository.getTranscript(episodeId, vttUrl)
                .onSuccess { cues ->
                    _uiState.update {
                        it.copy(
                            transcript = cues,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load transcript"
                        )
                    }
                }
        }
    }

    /**
     * Update current position to highlight the active transcript line
     *
     * @param positionMs Current playback position in milliseconds
     */
    fun updateCurrentPosition(positionMs: Long) {
        val transcript = _uiState.value.transcript
        if (transcript.isEmpty()) return

        val index = transcript.indexOfFirst { cue ->
            positionMs in cue.startTime..cue.endTime
        }

        _uiState.update { it.copy(currentLineIndex = index.takeIf { it >= 0 }) }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Get context for the current playback position including parsed references
     *
     * @param episodeId Current episode ID
     * @param currentPositionMs Current playback position in milliseconds
     * @return TranscriptSegmentContext with current segment and parsed references, or null if no segment found
     */
    fun getCurrentSegmentContext(episodeId: String, currentPositionMs: Long): TranscriptSegmentContext? {
        val transcript = _uiState.value.transcript
        if (transcript.isEmpty()) return null

        // Find current segment by position
        val currentSegment = transcript.firstOrNull { cue ->
            currentPositionMs >= cue.startTime && currentPositionMs < cue.endTime
        } ?: return null

        // Parse references using existing MarkdownLinkParser
        val annotatedText = MarkdownLinkParser.buildAnnotatedString(
            text = currentSegment.text,
            linkColor = Color.Blue  // Color doesn't matter for parsing
        )

        val references = annotatedText.getStringAnnotations(tag = "URL", start = 0, end = currentSegment.text.length)
            .mapNotNull { annotation ->
                EpisodeUrlParser.extractEpisodeId(annotation.item)?.let { extractedEpisodeId ->
                    EpisodeReference(
                        displayText = currentSegment.text.substring(annotation.start, annotation.end),
                        episodeId = extractedEpisodeId,
                        url = annotation.item
                    )
                }
            }

        return TranscriptSegmentContext(
            episodeId = episodeId,
            currentPositionMs = currentPositionMs,
            currentSegment = currentSegment,
            parsedReferences = references
        )
    }
}

/**
 * Context about the current transcript segment during playback
 */
data class TranscriptSegmentContext(
    val episodeId: String,
    val currentPositionMs: Long,
    val currentSegment: TranscriptCue,
    val parsedReferences: List<EpisodeReference>
)

/**
 * Reference extracted from transcript segment
 */
data class EpisodeReference(
    val displayText: String,
    val episodeId: String,
    val url: String
)
