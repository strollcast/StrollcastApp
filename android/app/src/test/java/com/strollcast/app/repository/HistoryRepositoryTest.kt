package com.strollcast.app.repository

import com.strollcast.app.data.CompletedEpisodeDao
import com.strollcast.app.models.CompletedEpisodeEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HistoryRepositoryTest {
    private lateinit var repository: HistoryRepository
    private lateinit var mockDao: CompletedEpisodeDao

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        repository = HistoryRepository(mockDao)
    }

    @Test
    fun `getAllCompletedEpisodes returns flow from DAO`() = runTest {
        val episodes = listOf(
            CompletedEpisodeEntity(
                id = "episode-1",
                episodeId = "episode-1",
                completionPercent = 95,
                completedAt = System.currentTimeMillis(),
                totalDurationMs = 600000
            )
        )

        coEvery { mockDao.getAllCompletedEpisodes() } returns flowOf(episodes)

        val result = repository.getAllCompletedEpisodes()

        result.collect { episodeList ->
            assertEquals(1, episodeList.size)
            assertEquals("episode-1", episodeList[0].episodeId)
        }

        coVerify { mockDao.getAllCompletedEpisodes() }
    }

    @Test
    fun `isEpisodeCompleted delegates to DAO`() = runTest {
        coEvery { mockDao.isEpisodeCompleted("episode-2") } returns true

        val result = repository.isEpisodeCompleted("episode-2")

        assertTrue(result)
        coVerify { mockDao.isEpisodeCompleted("episode-2") }
    }

    @Test
    fun `markEpisodeComplete with 90% threshold succeeds`() = runTest {
        val episodeId = "episode-3"
        val position = 540000L // 9 minutes
        val duration = 600000L // 10 minutes = 90%

        coEvery { mockDao.insertCompletedEpisode(any()) } returns Unit

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isSuccess)

        coVerify {
            mockDao.insertCompletedEpisode(
                match {
                    it.episodeId == episodeId &&
                    it.completionPercent == 90 &&
                    it.totalDurationMs == duration
                }
            )
        }
    }

    @Test
    fun `markEpisodeComplete with 95% threshold succeeds`() = runTest {
        val episodeId = "episode-4"
        val position = 570000L // 9.5 minutes
        val duration = 600000L // 10 minutes = 95%

        coEvery { mockDao.insertCompletedEpisode(any()) } returns Unit

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isSuccess)

        coVerify {
            mockDao.insertCompletedEpisode(
                match {
                    it.episodeId == episodeId &&
                    it.completionPercent == 95
                }
            )
        }
    }

    @Test
    fun `markEpisodeComplete with 100% threshold succeeds`() = runTest {
        val episodeId = "episode-5"
        val position = 600000L // 10 minutes
        val duration = 600000L // 10 minutes = 100%

        coEvery { mockDao.insertCompletedEpisode(any()) } returns Unit

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isSuccess)

        coVerify {
            mockDao.insertCompletedEpisode(
                match {
                    it.episodeId == episodeId &&
                    it.completionPercent == 100
                }
            )
        }
    }

    @Test
    fun `markEpisodeComplete below 90% threshold does not insert`() = runTest {
        val episodeId = "episode-6"
        val position = 530000L // 8.83 minutes
        val duration = 600000L // 10 minutes = 88%

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isSuccess)

        // Should not call insertCompletedEpisode
        coVerify(exactly = 0) { mockDao.insertCompletedEpisode(any()) }
    }

    @Test
    fun `markEpisodeComplete with exactly 90% threshold succeeds`() = runTest {
        val episodeId = "episode-7"
        val position = 540000L // 9 minutes
        val duration = 600000L // 10 minutes = exactly 90%

        coEvery { mockDao.insertCompletedEpisode(any()) } returns Unit

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) { mockDao.insertCompletedEpisode(any()) }
    }

    @Test
    fun `markEpisodeComplete with zero duration fails`() = runTest {
        val result = repository.markEpisodeComplete("episode-8", 100000L, 0L)

        assertTrue(result.isFailure)
        assertEquals("Episode duration must be positive", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { mockDao.insertCompletedEpisode(any()) }
    }

    @Test
    fun `markEpisodeComplete with negative duration fails`() = runTest {
        val result = repository.markEpisodeComplete("episode-9", 100000L, -1000L)

        assertTrue(result.isFailure)
        assertEquals("Episode duration must be positive", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { mockDao.insertCompletedEpisode(any()) }
    }

    @Test
    fun `markEpisodeComplete handles DAO exceptions`() = runTest {
        val episodeId = "episode-10"
        val position = 540000L
        val duration = 600000L

        coEvery { mockDao.insertCompletedEpisode(any()) } throws RuntimeException("Database error")

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `removeFromCompleted delegates to DAO`() = runTest {
        coEvery { mockDao.removeCompletedEpisode("episode-11") } returns Unit

        val result = repository.removeFromCompleted("episode-11")

        assertTrue(result.isSuccess)
        coVerify { mockDao.removeCompletedEpisode("episode-11") }
    }

    @Test
    fun `removeFromCompleted handles DAO exceptions`() = runTest {
        coEvery { mockDao.removeCompletedEpisode("episode-12") } throws RuntimeException("Delete failed")

        val result = repository.removeFromCompleted("episode-12")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `markEpisodeComplete coerces completion percent to 100`() = runTest {
        val episodeId = "episode-13"
        val position = 700000L // More than duration
        val duration = 600000L // Should cap at 100%

        coEvery { mockDao.insertCompletedEpisode(any()) } returns Unit

        val result = repository.markEpisodeComplete(episodeId, position, duration)

        assertTrue(result.isSuccess)

        coVerify {
            mockDao.insertCompletedEpisode(
                match {
                    it.completionPercent == 100 // Should be capped at 100
                }
            )
        }
    }

    @Test
    fun `markEpisodeComplete sets correct timestamp`() = runTest {
        val episodeId = "episode-14"
        val position = 540000L
        val duration = 600000L
        val beforeTime = System.currentTimeMillis()

        coEvery { mockDao.insertCompletedEpisode(any()) } returns Unit

        repository.markEpisodeComplete(episodeId, position, duration)

        val afterTime = System.currentTimeMillis()

        coVerify {
            mockDao.insertCompletedEpisode(
                match {
                    it.completedAt in beforeTime..afterTime
                }
            )
        }
    }
}
