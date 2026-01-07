package com.strollcast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strollcast.app.models.Podcast
import com.strollcast.app.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastUiState(
    val podcasts: List<Podcast> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val repository: PodcastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PodcastUiState(isLoading = true))
    val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

    init {
        loadPodcasts()
        refreshPodcasts()
    }

    private fun loadPodcasts() {
        viewModelScope.launch {
            repository.podcasts
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message,
                        isLoading = false
                    )
                }
                .collect { podcasts ->
                    _uiState.value = _uiState.value.copy(
                        podcasts = podcasts,
                        isLoading = false
                    )
                }
        }
    }

    fun refreshPodcasts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.refreshPodcasts()
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
