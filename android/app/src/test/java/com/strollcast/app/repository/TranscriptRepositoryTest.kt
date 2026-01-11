package com.strollcast.app.repository

import com.strollcast.app.data.TranscriptDao
import com.strollcast.app.models.TranscriptCue
import com.strollcast.app.models.TranscriptEntity
import com.strollcast.app.models.TranscriptLineEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TranscriptRepositoryTest {
    private lateinit var repository: TranscriptRepository
    private lateinit var mockDao: TranscriptDao
    private lateinit var mockOkHttpClient: OkHttpClient

    private val sampleVttContent = """
        WEBVTT

        00:00:00.000 --> 00:00:05.000
        <v Eric>Welcome to Strollcast

        00:00:05.000 --> 00:00:10.000
        <v Maya>Today's episode
    """.trimIndent()

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        mockOkHttpClient = mockk(relaxed = true)
        repository = TranscriptRepository(mockDao, mockOkHttpClient)
    }

    @Test
    fun `getTranscript returns from memory cache if available`() = runTest {
        // First call to populate memory cache
        coEvery { mockDao.getTranscript("episode-1") } returns null

        val mockResponse = createMockResponse(sampleVttContent)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        val firstResult = repository.getTranscript("episode-1", "https://example.com/transcript.vtt")
        assertTrue(firstResult.isSuccess)

        // Second call should hit memory cache
        val secondResult = repository.getTranscript("episode-1", null)
        assertTrue(secondResult.isSuccess)
        assertEquals(2, secondResult.getOrNull()?.size)

        // DAO should only be called once (for first call)
        coVerify(exactly = 1) { mockDao.getTranscript("episode-1") }
    }

    @Test
    fun `getTranscript returns from Room cache if not in memory`() = runTest {
        val cachedEntity = TranscriptEntity(
            id = "episode-2",
            episodeId = "episode-2",
            vttContent = sampleVttContent,
            cachedAt = System.currentTimeMillis()
        )

        coEvery { mockDao.getTranscript("episode-2") } returns cachedEntity

        val result = repository.getTranscript("episode-2", null)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)

        // Verify DAO was called
        coVerify { mockDao.getTranscript("episode-2") }

        // Verify no network call was made
        verify(exactly = 0) { mockOkHttpClient.newCall(any()) }
    }

    @Test
    fun `getTranscript downloads from network if not cached`() = runTest {
        coEvery { mockDao.getTranscript("episode-3") } returns null

        val mockResponse = createMockResponse(sampleVttContent)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        val result = repository.getTranscript("episode-3", "https://example.com/transcript.vtt")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)

        // Verify network call was made
        verify { mockOkHttpClient.newCall(any()).execute() }

        // Verify data was cached in Room
        coVerify { mockDao.insertTranscript(any()) }
        coVerify { mockDao.insertTranscriptLines(any()) }
    }

    @Test
    fun `getTranscript caches parsed lines in Room`() = runTest {
        coEvery { mockDao.getTranscript("episode-4") } returns null

        val mockResponse = createMockResponse(sampleVttContent)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        repository.getTranscript("episode-4", "https://example.com/transcript.vtt")

        // Verify transcript entity was inserted
        coVerify {
            mockDao.insertTranscript(
                match {
                    it.id == "episode-4" &&
                    it.vttContent == sampleVttContent
                }
            )
        }

        // Verify transcript lines were inserted
        coVerify {
            mockDao.insertTranscriptLines(
                match { lines ->
                    lines.size == 2 &&
                    lines[0].text == "Welcome to Strollcast" &&
                    lines[1].text == "Today's episode"
                }
            )
        }
    }

    @Test
    fun `getTranscript returns failure if no URL provided and not cached`() = runTest {
        coEvery { mockDao.getTranscript("episode-5") } returns null

        val result = repository.getTranscript("episode-5", null)

        assertTrue(result.isFailure)
        assertEquals("No transcript URL available", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getTranscript returns failure on network error`() = runTest {
        coEvery { mockDao.getTranscript("episode-6") } returns null

        // Mock failed network response
        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body("".toResponseBody())
            .build()

        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        val result = repository.getTranscript("episode-6", "https://example.com/transcript.vtt")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("404") == true)
    }

    @Test
    fun `getTranscript returns failure on malformed VTT`() = runTest {
        coEvery { mockDao.getTranscript("episode-7") } returns null

        val invalidVtt = "This is not valid VTT content"
        val mockResponse = createMockResponse(invalidVtt)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        val result = repository.getTranscript("episode-7", "https://example.com/transcript.vtt")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getTranscriptLines returns lines from DAO`() = runTest {
        val lines = listOf(
            TranscriptLineEntity(
                id = 1,
                transcriptId = "episode-8",
                startMs = 0,
                endMs = 5000,
                speaker = "Eric",
                text = "Test line",
                lineNumber = 0
            )
        )

        coEvery { mockDao.getTranscriptLines("episode-8") } returns lines

        val result = repository.getTranscriptLines("episode-8")

        assertEquals(1, result?.size)
        assertEquals("Test line", result?.get(0)?.text)
    }

    @Test
    fun `getTranscriptLines returns null if empty`() = runTest {
        coEvery { mockDao.getTranscriptLines("episode-9") } returns emptyList()

        val result = repository.getTranscriptLines("episode-9")

        assertEquals(null, result)
    }

    @Test
    fun `getCurrentLine delegates to DAO`() = runTest {
        val line = TranscriptLineEntity(
            id = 1,
            transcriptId = "episode-10",
            startMs = 5000,
            endMs = 10000,
            speaker = "Maya",
            text = "Current line",
            lineNumber = 1
        )

        coEvery { mockDao.getCurrentLine("episode-10", 7000) } returns line

        val result = repository.getCurrentLine("episode-10", 7000)

        assertEquals("Current line", result?.text)
        coVerify { mockDao.getCurrentLine("episode-10", 7000) }
    }

    @Test
    fun `clearOldTranscripts delegates to DAO with correct cutoff time`() = runTest {
        coEvery { mockDao.deleteOldTranscripts(any()) } returns 5

        repository.clearOldTranscripts()

        coVerify {
            mockDao.deleteOldTranscripts(
                match { cutoffTime ->
                    // Verify cutoff time is approximately 30 days ago
                    val now = System.currentTimeMillis()
                    val expectedCutoff = now - (30L * 24 * 60 * 60 * 1000)
                    Math.abs(cutoffTime - expectedCutoff) < 1000 // Within 1 second
                }
            )
        }
    }

    @Test
    fun `clearMemoryCache clears cached transcripts`() = runTest {
        // Populate memory cache
        coEvery { mockDao.getTranscript("episode-11") } returns null
        val mockResponse = createMockResponse(sampleVttContent)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        repository.getTranscript("episode-11", "https://example.com/transcript.vtt")

        // Clear cache
        repository.clearMemoryCache()

        // Next call should hit DAO, not memory
        coEvery { mockDao.getTranscript("episode-11") } returns TranscriptEntity(
            id = "episode-11",
            episodeId = "episode-11",
            vttContent = sampleVttContent,
            cachedAt = System.currentTimeMillis()
        )

        repository.getTranscript("episode-11", null)

        // Should have called DAO twice (once before clear, once after)
        coVerify(exactly = 2) { mockDao.getTranscript("episode-11") }
    }

    @Test
    fun `getTranscript parses VTT with speaker tags correctly`() = runTest {
        coEvery { mockDao.getTranscript("episode-12") } returns null

        val vttWithSpeakers = """
            WEBVTT

            00:00:00.000 --> 00:00:05.000
            <v Eric>First speaker

            00:00:05.000 --> 00:00:10.000
            <v Maya>Second speaker
        """.trimIndent()

        val mockResponse = createMockResponse(vttWithSpeakers)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        val result = repository.getTranscript("episode-12", "https://example.com/transcript.vtt")

        assertTrue(result.isSuccess)
        val cues = result.getOrNull()!!
        assertEquals(2, cues.size)
        assertEquals("Eric", cues[0].speaker)
        assertEquals("First speaker", cues[0].text)
        assertEquals("Maya", cues[1].speaker)
        assertEquals("Second speaker", cues[1].text)
    }

    @Test
    fun `getTranscript with timestamps converts correctly`() = runTest {
        coEvery { mockDao.getTranscript("episode-13") } returns null

        val mockResponse = createMockResponse(sampleVttContent)
        every { mockOkHttpClient.newCall(any()).execute() } returns mockResponse

        val result = repository.getTranscript("episode-13", "https://example.com/transcript.vtt")

        assertTrue(result.isSuccess)
        val cues = result.getOrNull()!!

        // First cue: 00:00:00.000 --> 00:00:05.000
        assertEquals(0L, cues[0].startTime)
        assertEquals(5000L, cues[0].endTime)

        // Second cue: 00:00:05.000 --> 00:00:10.000
        assertEquals(5000L, cues[1].startTime)
        assertEquals(10000L, cues[1].endTime)
    }

    private fun createMockResponse(content: String): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(content.toResponseBody())
            .build()
    }
}
