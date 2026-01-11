package com.strollcast.app.viewmodels

import com.strollcast.app.models.CompletedEpisodeEntity
import com.strollcast.app.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayedViewModelTest {
    private lateinit var viewModel: PlayedViewModel
    private lateinit var mockRepository: HistoryRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads completed episodes`() = runTest(testDispatcher) {
        val episodes = listOf(
            CompletedEpisodeEntity(
                id = "episode-1",
                episodeId = "episode-1",
                completionPercent = 95,
                completedAt = System.currentTimeMillis(),
                totalDurationMs = 600000
            )
        )

        coEvery { mockRepository.getAllCompletedEpisodes() } returns flowOf(episodes)

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.completedEpisodes.size)
        assertEquals("episode-1", state.completedEpisodes[0].episodeId)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadCompletedEpisodes sets loading state`() = runTest(testDispatcher) {
        coEvery { mockRepository.getAllCompletedEpisodes() } returns flowOf(emptyList())

        viewModel = PlayedViewModel(mockRepository)

        // Check loading state before flow emits
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
    }

    @Test
    fun `loadCompletedEpisodes handles empty list`() = runTest(testDispatcher) {
        coEvery { mockRepository.getAllCompletedEpisodes() } returns flowOf(emptyList())

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.completedEpisodes.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadCompletedEpisodes handles errors`() = runTest(testDispatcher) {
        coEvery { mockRepository.getAllCompletedEpisodes() } throws RuntimeException("Database error")

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Database error", state.error)
    }

    @Test
    fun `removeFromCompleted calls repository`() = runTest(testDispatcher) {
        coEvery { mockRepository.getAllCompletedEpisodes() } returns flowOf(emptyList())
        coEvery { mockRepository.removeFromCompleted("episode-2") } returns Result.success(Unit)

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeFromCompleted("episode-2")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.removeFromCompleted("episode-2") }
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `removeFromCompleted handles errors`() = runTest(testDispatcher) {
        coEvery { mockRepository.getAllCompletedEpisodes() } returns flowOf(emptyList())
        coEvery { mockRepository.removeFromCompleted("episode-3") } returns Result.failure(
            RuntimeException("Delete failed")
        )

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeFromCompleted("episode-3")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Delete failed", state.error)
    }

    @Test
    fun `clearError clears error state`() = runTest(testDispatcher) {
        coEvery { mockRepository.getAllCompletedEpisodes() } throws RuntimeException("Error")

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        assertNotNull(viewModel.uiState.value.error)

        // Clear error
        viewModel.clearError()

        // Verify error is cleared
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `multiple episodes are sorted correctly`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val episodes = listOf(
            CompletedEpisodeEntity(
                id = "episode-old",
                episodeId = "episode-old",
                completionPercent = 95,
                completedAt = now - 86400000, // 1 day ago
                totalDurationMs = 600000
            ),
            CompletedEpisodeEntity(
                id = "episode-new",
                episodeId = "episode-new",
                completionPercent = 100,
                completedAt = now, // Now
                totalDurationMs = 600000
            )
        )

        coEvery { mockRepository.getAllCompletedEpisodes() } returns flowOf(episodes)

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.completedEpisodes.size)
        // Episodes should be in the order returned by repository (newest first expected from DAO)
        assertEquals("episode-old", state.completedEpisodes[0].episodeId)
        assertEquals("episode-new", state.completedEpisodes[1].episodeId)
    }

    @Test
    fun `loadCompletedEpisodes can be called multiple times`() = runTest(testDispatcher) {
        val episodes1 = listOf(
            CompletedEpisodeEntity(
                id = "episode-1",
                episodeId = "episode-1",
                completionPercent = 95,
                completedAt = System.currentTimeMillis(),
                totalDurationMs = 600000
            )
        )

        val episodes2 = listOf(
            CompletedEpisodeEntity(
                id = "episode-2",
                episodeId = "episode-2",
                completionPercent = 100,
                completedAt = System.currentTimeMillis(),
                totalDurationMs = 600000
            )
        )

        coEvery { mockRepository.getAllCompletedEpisodes() } returnsMany listOf(
            flowOf(episodes1),
            flowOf(episodes2)
        )

        viewModel = PlayedViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.completedEpisodes.size)

        viewModel.loadCompletedEpisodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should have updated data
        val state = viewModel.uiState.value
        assertEquals(1, state.completedEpisodes.size)
    }
}
