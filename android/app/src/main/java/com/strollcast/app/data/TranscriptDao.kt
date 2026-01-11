package com.strollcast.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.strollcast.app.models.TranscriptEntity
import com.strollcast.app.models.TranscriptLineEntity

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts WHERE id = :episodeId")
    suspend fun getTranscript(episodeId: String): TranscriptEntity?

    @Query("SELECT * FROM transcripts ORDER BY cached_at DESC")
    suspend fun getAllTranscripts(): List<TranscriptEntity>

    @Insert
    suspend fun insertTranscript(transcript: TranscriptEntity)

    @Query("DELETE FROM transcripts WHERE cached_at < :cutoffTime")
    suspend fun deleteOldTranscripts(cutoffTime: Long): Int

    @Query("DELETE FROM transcripts WHERE episodeId = :episodeId")
    suspend fun deleteTranscript(episodeId: String): Int

    @Query("SELECT * FROM transcript_lines WHERE transcript_id = :transcriptId ORDER BY line_number ASC")
    suspend fun getTranscriptLines(transcriptId: String): List<TranscriptLineEntity>

    @Insert
    suspend fun insertTranscriptLines(lines: List<TranscriptLineEntity>)

    @Query("SELECT * FROM transcript_lines WHERE transcript_id = :transcriptId AND start_ms <= :positionMs AND end_ms > :positionMs LIMIT 1")
    suspend fun getCurrentLine(transcriptId: String, positionMs: Long): TranscriptLineEntity?

    @Query("SELECT * FROM transcript_lines WHERE transcript_id = :transcriptId AND line_number = :lineNumber LIMIT 1")
    suspend fun getTranscriptLineByNumber(transcriptId: String, lineNumber: Int): TranscriptLineEntity?
}
