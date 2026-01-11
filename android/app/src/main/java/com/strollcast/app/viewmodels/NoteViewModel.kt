package com.strollcast.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strollcast.app.models.NoteEntity
import com.strollcast.app.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for note management
 */
data class NoteUiState(
    val notes: List<NoteEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedNote: NoteEntity? = null,
    val noteCounts: Map<Int, Int> = emptyMap() // lineId -> count
)

/**
 * ViewModel for managing notes on transcript lines
 */
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val transcriptRepository: com.strollcast.app.repository.TranscriptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    /**
     * Load all notes for an episode
     *
     * @param episodeId Episode ID to load notes for
     */
    fun loadNotesForEpisode(episodeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                noteRepository.getNotesForEpisode(episodeId).collect { notes ->
                    _uiState.update {
                        it.copy(
                            notes = notes,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load notes"
                    )
                }
            }
        }
    }

    /**
     * Create a new note
     *
     * @param transcriptLineId Line ID to attach note to
     * @param episodeId Episode ID (denormalized)
     * @param content Note text content
     */
    fun createNote(
        transcriptLineId: Int,
        episodeId: String,
        content: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = noteRepository.createNote(transcriptLineId, episodeId, content)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to create note"
                        )
                    }
                }
            )
        }
    }

    /**
     * Create a new note by line index
     *
     * @param episodeId Episode ID
     * @param lineIndex Line index (0-based) in the transcript
     * @param content Note text content
     */
    fun createNoteByLineIndex(
        episodeId: String,
        lineIndex: Int,
        content: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Get the transcript line entity
                val lineEntity = transcriptRepository.getTranscriptLineByNumber(episodeId, lineIndex)

                if (lineEntity == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Transcript line not found"
                        )
                    }
                    return@launch
                }

                // Create the note
                val result = noteRepository.createNote(lineEntity.id, episodeId, content)

                result.fold(
                    onSuccess = {
                        // Reload note count for this line
                        loadNoteCount(lineEntity.id)
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to create note"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create note"
                    )
                }
            }
        }
    }

    /**
     * Update an existing note
     *
     * @param note Note entity with updated content
     */
    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = noteRepository.updateNote(note)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            selectedNote = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to update note"
                        )
                    }
                }
            )
        }
    }

    /**
     * Delete a note
     *
     * @param note Note entity to delete
     */
    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = noteRepository.deleteNote(note)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            selectedNote = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to delete note"
                        )
                    }
                }
            )
        }
    }

    /**
     * Select a note for editing
     *
     * @param note Note to select, or null to deselect
     */
    fun selectNote(note: NoteEntity?) {
        _uiState.update { it.copy(selectedNote = note) }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Load note count for a specific line
     *
     * @param lineId Transcript line ID
     */
    fun loadNoteCount(lineId: Int) {
        viewModelScope.launch {
            try {
                val count = noteRepository.getNoteCount(lineId)
                _uiState.update { state ->
                    state.copy(
                        noteCounts = state.noteCounts + (lineId to count)
                    )
                }
            } catch (e: Exception) {
                // Silent failure for note counts
            }
        }
    }

    /**
     * Load note counts for multiple lines
     *
     * @param lineIds List of transcript line IDs
     */
    fun loadNoteCounts(lineIds: List<Int>) {
        lineIds.forEach { loadNoteCount(it) }
    }
}
