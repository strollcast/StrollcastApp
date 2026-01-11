package com.strollcast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strollcast.app.models.CompletedEpisodeEntity
import com.strollcast.app.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for played/completed episodes
 */
data class PlayedUiState(
    val completedEpisodes: List<CompletedEpisodeEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing played/completed episodes
 */
@HiltViewModel
class PlayedViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayedUiState())
    val uiState: StateFlow<PlayedUiState> = _uiState.asStateFlow()

    init {
        loadCompletedEpisodes()
    }

    /**
     * Load all completed episodes
     */
    fun loadCompletedEpisodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                historyRepository.getAllCompletedEpisodes().collect { episodes ->
                    _uiState.update {
                        it.copy(
                            completedEpisodes = episodes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load completed episodes"
                    )
                }
            }
        }
    }

    /**
     * Remove episode from completed list
     *
     * @param episodeId Episode ID to remove
     */
    fun removeFromCompleted(episodeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = historyRepository.removeFromCompleted(episodeId)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to remove episode"
                        )
                    }
                }
            )
        }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
