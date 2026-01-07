package com.strollcast.app.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strollcast.app.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val zoteroApiKey: String = "",
    val zoteroUserId: String = "",
    val zoteroCollectionKey: String = "",
    val downloadedCount: Int = 0,
    val totalDownloadedSize: Long = 0L,
    val totalTranscriptSize: Long = 0L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PodcastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("strollcast_prefs", Context.MODE_PRIVATE)

    init {
        loadSettings()
        loadStorageInfo()
    }

    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            zoteroApiKey = prefs.getString("zotero_api_key", "") ?: "",
            zoteroUserId = prefs.getString("zotero_user_id", "") ?: "",
            zoteroCollectionKey = prefs.getString("zotero_collection_key", "") ?: ""
        )
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            repository.getAllDownloads().collect { downloads ->
                val totalSize = downloads.sumOf { it.fileSize }
                _uiState.value = _uiState.value.copy(
                    downloadedCount = downloads.size,
                    totalDownloadedSize = totalSize
                )
            }
        }

        viewModelScope.launch {
            val transcriptDir = context.cacheDir.resolve("transcripts")
            val transcriptSize = transcriptDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }

            _uiState.value = _uiState.value.copy(
                totalTranscriptSize = transcriptSize
            )
        }
    }

    fun updateZoteroApiKey(key: String) {
        _uiState.value = _uiState.value.copy(zoteroApiKey = key)
        prefs.edit().putString("zotero_api_key", key).apply()
    }

    fun updateZoteroUserId(userId: String) {
        _uiState.value = _uiState.value.copy(zoteroUserId = userId)
        prefs.edit().putString("zotero_user_id", userId).apply()
    }

    fun updateZoteroCollectionKey(key: String) {
        _uiState.value = _uiState.value.copy(zoteroCollectionKey = key)
        prefs.edit().putString("zotero_collection_key", key).apply()
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            // Delete all downloaded files
            repository.getAllDownloads().collect { downloads ->
                downloads.forEach { download ->
                    repository.deleteDownload(download.episodeId)
                }
            }

            // Delete all cached transcripts
            val transcriptDir = context.cacheDir.resolve("transcripts")
            transcriptDir.deleteRecursively()
            transcriptDir.mkdirs()

            // Clear playback history
            repository.clearPlaybackHistory()

            // Reload storage info
            loadStorageInfo()
        }
    }
}
