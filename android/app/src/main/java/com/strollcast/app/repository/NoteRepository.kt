package com.strollcast.app.repository

import android.util.Log
import com.strollcast.app.data.NoteDao
import com.strollcast.app.models.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    companion object {
        private const val TAG = "NoteRepository"
        private const val MAX_NOTE_LENGTH = 5000
    }

    /**
     * Get all notes as Flow
     *
     * @return Flow of all notes sorted by creation date (newest first)
     */
    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    /**
     * Get all notes for an episode as Flow
     *
     * @param episodeId Episode ID
     * @return Flow of notes sorted by creation date (newest first)
     */
    fun getNotesForEpisode(episodeId: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesForEpisode(episodeId)
    }

    /**
     * Get notes for a specific transcript line
     *
     * @param lineId Transcript line ID
     * @return List of notes sorted by creation date (oldest first)
     */
    suspend fun getNotesForLine(lineId: Int): List<NoteEntity> {
        return withContext(Dispatchers.IO) {
            noteDao.getNotesForLine(lineId)
        }
    }

    /**
     * Create a new note
     *
     * @param transcriptLineId Transcript line ID to attach note to
     * @param episodeId Episode ID (denormalized for fast queries)
     * @param content Note text content
     * @return Result with note ID or error
     */
    suspend fun createNote(
        transcriptLineId: Int,
        episodeId: String,
        content: String
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate content
                val trimmedContent = content.trim()
                if (trimmedContent.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Note content cannot be empty"))
                }
                if (trimmedContent.length > MAX_NOTE_LENGTH) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Note content exceeds maximum length of $MAX_NOTE_LENGTH characters")
                    )
                }

                val now = System.currentTimeMillis()
                val note = NoteEntity(
                    transcriptLineId = transcriptLineId,
                    episodeId = episodeId,
                    content = trimmedContent,
                    createdAt = now,
                    updatedAt = now
                )

                val noteId = noteDao.insertNote(note)
                Log.d(TAG, "Created note with ID: $noteId for line: $transcriptLineId")

                Result.success(noteId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating note", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update an existing note
     *
     * @param note Note entity with updated content
     * @return Result indicating success or error
     */
    suspend fun updateNote(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate content
                val trimmedContent = note.content.trim()
                if (trimmedContent.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Note content cannot be empty"))
                }
                if (trimmedContent.length > MAX_NOTE_LENGTH) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Note content exceeds maximum length of $MAX_NOTE_LENGTH characters")
                    )
                }

                // Validate timestamps
                if (note.createdAt > note.updatedAt) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Updated time cannot be before created time")
                    )
                }

                val updatedNote = note.copy(
                    content = trimmedContent,
                    updatedAt = System.currentTimeMillis()
                )

                noteDao.updateNote(updatedNote)
                Log.d(TAG, "Updated note with ID: ${note.id}")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating note", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a note
     *
     * @param note Note entity to delete
     * @return Result indicating success or error
     */
    suspend fun deleteNote(note: NoteEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                noteDao.deleteNote(note)
                Log.d(TAG, "Deleted note with ID: ${note.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get count of notes for a transcript line
     *
     * @param lineId Transcript line ID
     * @return Number of notes attached to this line
     */
    suspend fun getNoteCount(lineId: Int): Int {
        return withContext(Dispatchers.IO) {
            noteDao.getNoteCount(lineId)
        }
    }
}
