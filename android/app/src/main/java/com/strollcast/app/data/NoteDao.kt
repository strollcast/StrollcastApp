package com.strollcast.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.strollcast.app.models.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE episode_id = :episodeId ORDER BY created_at DESC")
    fun getNotesForEpisode(episodeId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE transcript_line_id = :lineId ORDER BY created_at ASC")
    suspend fun getNotesForLine(lineId: Int): List<NoteEntity>

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT COUNT(*) FROM notes WHERE transcript_line_id = :lineId")
    suspend fun getNoteCount(lineId: Int): Int
}
