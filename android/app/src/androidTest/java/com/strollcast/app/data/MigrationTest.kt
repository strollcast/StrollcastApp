package com.strollcast.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.strollcast.app.data.migrations.MIGRATION_5_6
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StrollcastDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // Create database with version 5 schema
        val db = helper.createDatabase(TEST_DB, 5).apply {
            // Insert test data in version 5 schema (existing tables)
            execSQL(
                """
                INSERT INTO podcasts (id, title, authors, year, duration, durationSeconds, description,
                                      audioUrl, transcriptUrl, paperUrl, published, createdAt)
                VALUES ('test-ep-1', 'Test Episode', 'Test Author', 2024, '10:00', 600,
                        'Test description', 'https://example.com/audio.m4a',
                        'https://example.com/transcript.vtt', 'https://example.com/paper.pdf',
                        1, 1704067200000)
                """.trimIndent()
            )

            close()
        }

        // Run migration from v5 to v6
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Verify new tables exist
        val cursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
        val tables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0))
        }
        cursor.close()

        // Check that all new tables were created
        assert(tables.contains("transcripts")) { "transcripts table not found" }
        assert(tables.contains("transcript_lines")) { "transcript_lines table not found" }
        assert(tables.contains("notes")) { "notes table not found" }
        assert(tables.contains("completed_episodes")) { "completed_episodes table not found" }

        // Verify existing data is preserved
        val podcastCursor = migratedDb.query("SELECT id, title FROM podcasts WHERE id = 'test-ep-1'")
        assert(podcastCursor.moveToFirst()) { "Test podcast not found after migration" }
        assertEquals("test-ep-1", podcastCursor.getString(0))
        assertEquals("Test Episode", podcastCursor.getString(1))
        podcastCursor.close()

        migratedDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6_verifiesIndexes() {
        // Create database with version 5 schema
        helper.createDatabase(TEST_DB, 5).close()

        // Run migration
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Verify indexes were created
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' ORDER BY name")
        val indexes = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val indexName = cursor.getString(0)
            if (!indexName.startsWith("sqlite_")) {  // Filter out internal SQLite indexes
                indexes.add(indexName)
            }
        }
        cursor.close()

        // Check critical indexes
        assert(indexes.contains("index_transcripts_episodeId")) { "Transcript episodeId index missing" }
        assert(indexes.contains("index_transcript_lines_transcriptId")) { "Transcript lines transcriptId index missing" }
        assert(indexes.contains("index_notes_transcriptLineId")) { "Notes transcriptLineId index missing" }
        assert(indexes.contains("index_completed_episodes_completedAt")) { "Completed episodes completedAt index missing" }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6_canInsertIntoNewTables() {
        // Create and migrate database
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO podcasts (id, title, authors, year, duration, durationSeconds, description,
                                      audioUrl, transcriptUrl, paperUrl, published, createdAt)
                VALUES ('test-ep-2', 'Test Episode 2', 'Test Author', 2024, '10:00', 600,
                        'Test description', 'https://example.com/audio.m4a',
                        'https://example.com/transcript.vtt', 'https://example.com/paper.pdf',
                        1, 1704067200000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Insert into new transcripts table
        db.execSQL(
            """
            INSERT INTO transcripts (id, episodeId, vtt_content, cached_at)
            VALUES ('test-ep-2', 'test-ep-2', 'WEBVTT\n\n00:00:00.000 --> 00:00:05.000\nTest', ${System.currentTimeMillis()})
            """.trimIndent()
        )

        // Insert into transcript_lines table
        db.execSQL(
            """
            INSERT INTO transcript_lines (transcript_id, start_ms, end_ms, speaker, text, line_number)
            VALUES ('test-ep-2', 0, 5000, 'Eric', 'Test line', 0)
            """.trimIndent()
        )

        // Insert into notes table
        db.execSQL(
            """
            INSERT INTO notes (transcript_line_id, episode_id, content, created_at, updated_at)
            VALUES (1, 'test-ep-2', 'Test note', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent()
        )

        // Insert into completed_episodes table
        db.execSQL(
            """
            INSERT INTO completed_episodes (id, episode_id, completion_percent, completed_at, total_duration_ms)
            VALUES ('test-ep-2', 'test-ep-2', 100, ${System.currentTimeMillis()}, 600000)
            """.trimIndent()
        )

        // Verify inserts worked
        val transcriptCursor = db.query("SELECT COUNT(*) FROM transcripts")
        transcriptCursor.moveToFirst()
        assertEquals(1, transcriptCursor.getInt(0))
        transcriptCursor.close()

        val linesCursor = db.query("SELECT COUNT(*) FROM transcript_lines")
        linesCursor.moveToFirst()
        assertEquals(1, linesCursor.getInt(0))
        linesCursor.close()

        val notesCursor = db.query("SELECT COUNT(*) FROM notes")
        notesCursor.moveToFirst()
        assertEquals(1, notesCursor.getInt(0))
        notesCursor.close()

        val completedCursor = db.query("SELECT COUNT(*) FROM completed_episodes")
        completedCursor.moveToFirst()
        assertEquals(1, completedCursor.getInt(0))
        completedCursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6_foreignKeyConstraints() {
        // Create and migrate database
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO podcasts (id, title, authors, year, duration, durationSeconds, description,
                                      audioUrl, transcriptUrl, paperUrl, published, createdAt)
                VALUES ('test-ep-3', 'Test Episode 3', 'Test Author', 2024, '10:00', 600,
                        'Test description', 'https://example.com/audio.m4a',
                        'https://example.com/transcript.vtt', 'https://example.com/paper.pdf',
                        1, 1704067200000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Enable foreign keys
        db.execSQL("PRAGMA foreign_keys=ON")

        // Insert transcript
        db.execSQL(
            """
            INSERT INTO transcripts (id, episodeId, vtt_content, cached_at)
            VALUES ('test-ep-3', 'test-ep-3', 'WEBVTT\n\nTest', ${System.currentTimeMillis()})
            """.trimIndent()
        )

        // Verify CASCADE delete: deleting podcast should delete transcript
        db.execSQL("DELETE FROM podcasts WHERE id = 'test-ep-3'")

        val cursor = db.query("SELECT COUNT(*) FROM transcripts WHERE id = 'test-ep-3'")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()

        db.close()
    }

    @Test
    fun testDatabaseCreation_afterMigration() {
        // Run migration
        helper.createDatabase(TEST_DB, 5).close()
        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6).close()

        // Open database using Room
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            StrollcastDatabase::class.java,
            TEST_DB
        )
            .addMigrations(MIGRATION_5_6)
            .build()

        // Verify DAOs are accessible
        val transcriptDao = db.transcriptDao()
        val noteDao = db.noteDao()
        val completedEpisodeDao = db.completedEpisodeDao()

        // Close database
        db.close()
    }
}
