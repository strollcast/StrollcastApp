package com.strollcast.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strollcast.app.models.NoteEntity
import com.strollcast.app.models.Podcast
import com.strollcast.app.models.TranscriptEntity
import com.strollcast.app.models.TranscriptLineEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var database: StrollcastDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var podcastDao: PodcastDao
    private lateinit var transcriptDao: TranscriptDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StrollcastDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        noteDao = database.noteDao()
        podcastDao = database.podcastDao()
        transcriptDao = database.transcriptDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveNote() = runTest {
        // Setup parent entities
        val podcast = createTestPodcast("episode-1")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-1")
        transcriptDao.insertTranscript(transcript)

        val lineEntity = createTestTranscriptLine("episode-1", 1, 0)
        transcriptDao.insertTranscriptLines(listOf(lineEntity))

        // Insert note
        val note = NoteEntity(
            transcriptLineId = 1,
            episodeId = "episode-1",
            content = "This is a test note",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val noteId = noteDao.insertNote(note)

        // Retrieve notes
        val notes = noteDao.getNotesForEpisode("episode-1").first()

        assertEquals(1, notes.size)
        assertEquals(noteId, notes[0].id)
        assertEquals("This is a test note", notes[0].content)
    }

    @Test
    fun getNotesForEpisode_orderedByCreatedDate() = runTest {
        // Setup parent entities
        val podcast = createTestPodcast("episode-2")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-2")
        transcriptDao.insertTranscript(transcript)

        val line1 = createTestTranscriptLine("episode-2", 1, 0)
        val line2 = createTestTranscriptLine("episode-2", 2, 1)
        transcriptDao.insertTranscriptLines(listOf(line1, line2))

        // Insert notes with different timestamps
        val now = System.currentTimeMillis()
        val note1 = NoteEntity(
            transcriptLineId = 1,
            episodeId = "episode-2",
            content = "First note",
            createdAt = now - 1000,
            updatedAt = now - 1000
        )
        val note2 = NoteEntity(
            transcriptLineId = 2,
            episodeId = "episode-2",
            content = "Second note",
            createdAt = now,
            updatedAt = now
        )

        noteDao.insertNote(note1)
        noteDao.insertNote(note2)

        // Retrieve notes - should be ordered by created_at DESC
        val notes = noteDao.getNotesForEpisode("episode-2").first()

        assertEquals(2, notes.size)
        assertEquals("Second note", notes[0].content)
        assertEquals("First note", notes[1].content)
    }

    @Test
    fun getNotesForLine() = runTest {
        // Setup parent entities
        val podcast = createTestPodcast("episode-3")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-3")
        transcriptDao.insertTranscript(transcript)

        val line = createTestTranscriptLine("episode-3", 1, 0)
        transcriptDao.insertTranscriptLines(listOf(line))

        // Insert multiple notes for same line
        val now = System.currentTimeMillis()
        val note1 = NoteEntity(
            transcriptLineId = 1,
            episodeId = "episode-3",
            content = "Note 1",
            createdAt = now - 1000,
            updatedAt = now - 1000
        )
        val note2 = NoteEntity(
            transcriptLineId = 1,
            episodeId = "episode-3",
            content = "Note 2",
            createdAt = now,
            updatedAt = now
        )

        noteDao.insertNote(note1)
        noteDao.insertNote(note2)

        // Retrieve notes for line - should be ordered by created_at ASC
        val notes = noteDao.getNotesForLine(1)

        assertEquals(2, notes.size)
        assertEquals("Note 1", notes[0].content)
        assertEquals("Note 2", notes[1].content)
    }

    @Test
    fun updateNote() = runTest {
        // Setup
        val podcast = createTestPodcast("episode-4")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-4")
        transcriptDao.insertTranscript(transcript)

        val line = createTestTranscriptLine("episode-4", 1, 0)
        transcriptDao.insertTranscriptLines(listOf(line))

        // Insert note
        val note = NoteEntity(
            transcriptLineId = 1,
            episodeId = "episode-4",
            content = "Original content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val noteId = noteDao.insertNote(note)

        // Update note
        val updatedNote = note.copy(
            id = noteId.toInt(),
            content = "Updated content",
            updatedAt = System.currentTimeMillis() + 1000
        )
        noteDao.updateNote(updatedNote)

        // Verify update
        val notes = noteDao.getNotesForEpisode("episode-4").first()
        assertEquals(1, notes.size)
        assertEquals("Updated content", notes[0].content)
        assertTrue(notes[0].updatedAt > notes[0].createdAt)
    }

    @Test
    fun deleteNote() = runTest {
        // Setup
        val podcast = createTestPodcast("episode-5")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-5")
        transcriptDao.insertTranscript(transcript)

        val line = createTestTranscriptLine("episode-5", 1, 0)
        transcriptDao.insertTranscriptLines(listOf(line))

        // Insert note
        val note = NoteEntity(
            transcriptLineId = 1,
            episodeId = "episode-5",
            content = "To be deleted",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val noteId = noteDao.insertNote(note)

        // Delete note
        noteDao.deleteNote(note.copy(id = noteId.toInt()))

        // Verify deletion
        val notes = noteDao.getNotesForEpisode("episode-5").first()
        assertEquals(0, notes.size)
    }

    @Test
    fun getNoteCount() = runTest {
        // Setup
        val podcast = createTestPodcast("episode-6")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-6")
        transcriptDao.insertTranscript(transcript)

        val line = createTestTranscriptLine("episode-6", 1, 0)
        transcriptDao.insertTranscriptLines(listOf(line))

        // Initially zero notes
        assertEquals(0, noteDao.getNoteCount(1))

        // Insert notes
        noteDao.insertNote(
            NoteEntity(
                transcriptLineId = 1,
                episodeId = "episode-6",
                content = "Note 1",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        noteDao.insertNote(
            NoteEntity(
                transcriptLineId = 1,
                episodeId = "episode-6",
                content = "Note 2",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Verify count
        assertEquals(2, noteDao.getNoteCount(1))
    }

    @Test
    fun cascadeDelete_deletingTranscriptLine_deletesNotes() = runTest {
        // Setup
        val podcast = createTestPodcast("episode-7")
        podcastDao.insert(podcast)

        val transcript = createTestTranscript("episode-7")
        transcriptDao.insertTranscript(transcript)

        val line = createTestTranscriptLine("episode-7", 1, 0)
        transcriptDao.insertTranscriptLines(listOf(line))

        // Insert note
        noteDao.insertNote(
            NoteEntity(
                transcriptLineId = 1,
                episodeId = "episode-7",
                content = "Will be cascade deleted",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Verify note exists
        assertEquals(1, noteDao.getNotesForEpisode("episode-7").first().size)

        // Delete podcast (should cascade to transcript, then to transcript lines, then to notes)
        podcastDao.delete(podcast)

        // Verify notes were cascade deleted
        assertEquals(0, noteDao.getNotesForEpisode("episode-7").first().size)
    }

    private fun createTestPodcast(id: String): Podcast {
        return Podcast(
            id = id,
            title = "Test Episode",
            authors = "Test Author",
            year = 2024,
            duration = "10:00",
            durationSeconds = 600,
            description = "Test description",
            audioUrl = "https://example.com/audio.m4a",
            transcriptUrl = "https://example.com/transcript.vtt",
            paperUrl = "https://example.com/paper.pdf",
            published = true,
            createdAt = null
        )
    }

    private fun createTestTranscript(episodeId: String): TranscriptEntity {
        return TranscriptEntity(
            id = episodeId,
            episodeId = episodeId,
            vttContent = "WEBVTT\n\n00:00:00.000 --> 00:00:05.000\nTest content",
            cachedAt = System.currentTimeMillis()
        )
    }

    private fun createTestTranscriptLine(transcriptId: String, id: Int, lineNumber: Int): TranscriptLineEntity {
        return TranscriptLineEntity(
            id = id,
            transcriptId = transcriptId,
            startMs = lineNumber.toLong() * 5000,
            endMs = (lineNumber + 1).toLong() * 5000,
            speaker = "Test Speaker",
            text = "Test line $lineNumber",
            lineNumber = lineNumber
        )
    }
}
