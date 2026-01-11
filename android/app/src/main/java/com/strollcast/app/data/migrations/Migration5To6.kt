package com.strollcast.app.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create transcripts table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transcripts (
                id TEXT PRIMARY KEY NOT NULL,
                episodeId TEXT NOT NULL,
                vtt_content TEXT NOT NULL,
                cached_at INTEGER NOT NULL,
                FOREIGN KEY(episodeId) REFERENCES podcasts(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transcripts_episodeId ON transcripts(episodeId)")

        // 2. Create transcript_lines table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transcript_lines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transcript_id TEXT NOT NULL,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                speaker TEXT,
                text TEXT NOT NULL,
                line_number INTEGER NOT NULL,
                FOREIGN KEY(transcript_id) REFERENCES transcripts(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transcript_lines_transcriptId ON transcript_lines(transcript_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transcript_lines_transcriptId_lineNumber ON transcript_lines(transcript_id, line_number)")

        // 3. Create notes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transcript_line_id INTEGER NOT NULL,
                episode_id TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(transcript_line_id) REFERENCES transcript_lines(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_transcriptLineId ON notes(transcript_line_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_episodeId_createdAt ON notes(episode_id, created_at)")

        // 4. Create completed_episodes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS completed_episodes (
                id TEXT PRIMARY KEY NOT NULL,
                episode_id TEXT NOT NULL,
                completion_percent INTEGER NOT NULL,
                completed_at INTEGER NOT NULL,
                total_duration_ms INTEGER NOT NULL,
                FOREIGN KEY(episode_id) REFERENCES podcasts(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_completed_episodes_completedAt ON completed_episodes(completed_at DESC)")
    }
}
