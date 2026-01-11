package com.strollcast.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strollcast.app.models.Podcast
import com.strollcast.app.models.TranscriptEntity
import com.strollcast.app.models.TranscriptLineEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranscriptDaoTest {
    private lateinit var database: StrollcastDatabase
    private lateinit var transcriptDao: TranscriptDao
    private lateinit var podcastDao: PodcastDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StrollcastDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        transcriptDao = database.transcriptDao()
        podcastDao = database.podcastDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTranscript() = runTest {
        // Insert parent podcast first
        val podcast = createTestPodcast("episode-1")
        podcastDao.insert(podcast)

        // Insert transcript
        val transcript = TranscriptEntity(
            id = "episode-1",
            episodeId = "episode-1",
            vttContent = "WEBVTT\n\n00:00:00.000 --> 00:00:05.000\nTest content",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        // Retrieve transcript
        val retrieved = transcriptDao.getTranscript("episode-1")

        assertNotNull(retrieved)
        assertEquals(transcript.id, retrieved?.id)
        assertEquals(transcript.vttContent, retrieved?.vttContent)
    }

    @Test
    fun getTranscript_nonExistent_returnsNull() = runTest {
        val result = transcriptDao.getTranscript("non-existent")
        assertNull(result)
    }

    @Test
    fun insertTranscriptLines() = runTest {
        // Insert parent entities
        val podcast = createTestPodcast("episode-2")
        podcastDao.insert(podcast)

        val transcript = TranscriptEntity(
            id = "episode-2",
            episodeId = "episode-2",
            vttContent = "WEBVTT\n\nTest",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        // Insert transcript lines
        val lines = listOf(
            TranscriptLineEntity(
                transcriptId = "episode-2",
                startMs = 0,
                endMs = 5000,
                speaker = "Eric",
                text = "First line",
                lineNumber = 0
            ),
            TranscriptLineEntity(
                transcriptId = "episode-2",
                startMs = 5000,
                endMs = 10000,
                speaker = "Maya",
                text = "Second line",
                lineNumber = 1
            ),
            TranscriptLineEntity(
                transcriptId = "episode-2",
                startMs = 10000,
                endMs = 15000,
                speaker = "Eric",
                text = "Third line",
                lineNumber = 2
            )
        )
        transcriptDao.insertTranscriptLines(lines)

        // Retrieve lines
        val retrieved = transcriptDao.getTranscriptLines("episode-2")

        assertEquals(3, retrieved.size)
        assertEquals("First line", retrieved[0].text)
        assertEquals("Second line", retrieved[1].text)
        assertEquals("Third line", retrieved[2].text)
    }

    @Test
    fun getTranscriptLines_orderedByLineNumber() = runTest {
        // Insert parent entities
        val podcast = createTestPodcast("episode-3")
        podcastDao.insert(podcast)

        val transcript = TranscriptEntity(
            id = "episode-3",
            episodeId = "episode-3",
            vttContent = "WEBVTT\n\nTest",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        // Insert lines in random order
        val lines = listOf(
            TranscriptLineEntity(
                transcriptId = "episode-3",
                startMs = 10000,
                endMs = 15000,
                speaker = "Eric",
                text = "Third line",
                lineNumber = 2
            ),
            TranscriptLineEntity(
                transcriptId = "episode-3",
                startMs = 0,
                endMs = 5000,
                speaker = "Eric",
                text = "First line",
                lineNumber = 0
            ),
            TranscriptLineEntity(
                transcriptId = "episode-3",
                startMs = 5000,
                endMs = 10000,
                speaker = "Maya",
                text = "Second line",
                lineNumber = 1
            )
        )
        transcriptDao.insertTranscriptLines(lines)

        // Retrieve should be ordered by lineNumber
        val retrieved = transcriptDao.getTranscriptLines("episode-3")

        assertEquals(3, retrieved.size)
        assertEquals(0, retrieved[0].lineNumber)
        assertEquals(1, retrieved[1].lineNumber)
        assertEquals(2, retrieved[2].lineNumber)
        assertEquals("First line", retrieved[0].text)
        assertEquals("Second line", retrieved[1].text)
        assertEquals("Third line", retrieved[2].text)
    }

    @Test
    fun getCurrentLine_returnsCorrectLine() = runTest {
        // Insert parent entities
        val podcast = createTestPodcast("episode-4")
        podcastDao.insert(podcast)

        val transcript = TranscriptEntity(
            id = "episode-4",
            episodeId = "episode-4",
            vttContent = "WEBVTT\n\nTest",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        // Insert lines
        val lines = listOf(
            TranscriptLineEntity(
                transcriptId = "episode-4",
                startMs = 0,
                endMs = 5000,
                speaker = "Eric",
                text = "First line",
                lineNumber = 0
            ),
            TranscriptLineEntity(
                transcriptId = "episode-4",
                startMs = 5000,
                endMs = 10000,
                speaker = "Maya",
                text = "Second line",
                lineNumber = 1
            ),
            TranscriptLineEntity(
                transcriptId = "episode-4",
                startMs = 10000,
                endMs = 15000,
                speaker = "Eric",
                text = "Third line",
                lineNumber = 2
            )
        )
        transcriptDao.insertTranscriptLines(lines)

        // Get line at position 7000ms (should be second line)
        val currentLine = transcriptDao.getCurrentLine("episode-4", 7000)

        assertNotNull(currentLine)
        assertEquals("Second line", currentLine?.text)
        assertEquals("Maya", currentLine?.speaker)
        assertEquals(5000L, currentLine?.startMs)
        assertEquals(10000L, currentLine?.endMs)
    }

    @Test
    fun getCurrentLine_atStartBoundary() = runTest {
        // Insert parent entities
        val podcast = createTestPodcast("episode-5")
        podcastDao.insert(podcast)

        val transcript = TranscriptEntity(
            id = "episode-5",
            episodeId = "episode-5",
            vttContent = "WEBVTT\n\nTest",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        val lines = listOf(
            TranscriptLineEntity(
                transcriptId = "episode-5",
                startMs = 5000,
                endMs = 10000,
                speaker = "Eric",
                text = "Test line",
                lineNumber = 0
            )
        )
        transcriptDao.insertTranscriptLines(lines)

        // Position exactly at start
        val currentLine = transcriptDao.getCurrentLine("episode-5", 5000)

        assertNotNull(currentLine)
        assertEquals("Test line", currentLine?.text)
    }

    @Test
    fun getCurrentLine_beforeFirstLine_returnsNull() = runTest {
        // Insert parent entities
        val podcast = createTestPodcast("episode-6")
        podcastDao.insert(podcast)

        val transcript = TranscriptEntity(
            id = "episode-6",
            episodeId = "episode-6",
            vttContent = "WEBVTT\n\nTest",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        val lines = listOf(
            TranscriptLineEntity(
                transcriptId = "episode-6",
                startMs = 5000,
                endMs = 10000,
                speaker = "Eric",
                text = "Test line",
                lineNumber = 0
            )
        )
        transcriptDao.insertTranscriptLines(lines)

        // Position before first line
        val currentLine = transcriptDao.getCurrentLine("episode-6", 1000)

        assertNull(currentLine)
    }

    @Test
    fun deleteOldTranscripts() = runTest {
        val now = System.currentTimeMillis()
        val oldTime = now - (40L * 24 * 60 * 60 * 1000) // 40 days ago
        val recentTime = now - (10L * 24 * 60 * 60 * 1000) // 10 days ago

        // Insert podcasts
        podcastDao.insert(createTestPodcast("old-episode"))
        podcastDao.insert(createTestPodcast("recent-episode"))

        // Insert old transcript
        transcriptDao.insertTranscript(
            TranscriptEntity(
                id = "old-episode",
                episodeId = "old-episode",
                vttContent = "WEBVTT\n\nOld",
                cachedAt = oldTime
            )
        )

        // Insert recent transcript
        transcriptDao.insertTranscript(
            TranscriptEntity(
                id = "recent-episode",
                episodeId = "recent-episode",
                vttContent = "WEBVTT\n\nRecent",
                cachedAt = recentTime
            )
        )

        // Delete transcripts older than 30 days
        val cutoffTime = now - (30L * 24 * 60 * 60 * 1000)
        val deletedCount = transcriptDao.deleteOldTranscripts(cutoffTime)

        assertEquals(1, deletedCount)

        // Verify old transcript deleted
        assertNull(transcriptDao.getTranscript("old-episode"))

        // Verify recent transcript still exists
        assertNotNull(transcriptDao.getTranscript("recent-episode"))
    }

    @Test
    fun cascadeDelete_deletingPodcast_deletesTranscript() = runTest {
        // Insert podcast
        val podcast = createTestPodcast("cascade-test")
        podcastDao.insert(podcast)

        // Insert transcript
        val transcript = TranscriptEntity(
            id = "cascade-test",
            episodeId = "cascade-test",
            vttContent = "WEBVTT\n\nTest",
            cachedAt = System.currentTimeMillis()
        )
        transcriptDao.insertTranscript(transcript)

        // Verify transcript exists
        assertNotNull(transcriptDao.getTranscript("cascade-test"))

        // Delete podcast
        podcastDao.delete(podcast)

        // Verify transcript was cascade deleted
        assertNull(transcriptDao.getTranscript("cascade-test"))
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
}
