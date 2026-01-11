package com.strollcast.app.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.strollcast.app.models.CompletedEpisodeEntity
import com.strollcast.app.models.Podcast
import com.strollcast.app.repository.HistoryRepository
import com.strollcast.app.repository.PodcastRepository
import com.strollcast.app.viewmodels.PlayedViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlayedListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockHistoryRepository: HistoryRepository
    private lateinit var mockPodcastRepository: PodcastRepository
    private lateinit var mockViewModel: PlayedViewModel
    private lateinit var completedEpisodesFlow: MutableStateFlow<List<CompletedEpisodeEntity>>

    @Before
    fun setup() {
        mockHistoryRepository = mockk(relaxed = true)
        mockPodcastRepository = mockk(relaxed = true)
        mockViewModel = mockk(relaxed = true)

        completedEpisodesFlow = MutableStateFlow(emptyList())
        every { mockHistoryRepository.getAllCompletedEpisodes() } returns completedEpisodesFlow
    }

    @Test
    fun playedListScreen_displaysEmptyState() {
        coEvery { mockHistoryRepository.getAllCompletedEpisodes() } returns flowOf(emptyList())
        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = emptyList(),
                isLoading = false,
                error = null
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Should show empty state
        composeTestRule.onNodeWithText("No played episodes yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episodes you've completed will appear here").assertIsDisplayed()
    }

    @Test
    fun playedListScreen_displaysLoadingState() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = emptyList(),
                isLoading = true,
                error = null
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Should show loading indicator
        // Note: CircularProgressIndicator doesn't have text, so we can't easily assert it
        // but we can verify empty state is not shown
        composeTestRule.onNodeWithText("No played episodes yet").assertDoesNotExist()
    }

    @Test
    fun playedListScreen_displaysErrorState() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = emptyList(),
                isLoading = false,
                error = "Database error"
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Should show error message
        composeTestRule.onNodeWithText("Error Loading Played Episodes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Database error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun playedListScreen_displaysCompletedEpisodes() {
        val episode = CompletedEpisodeEntity(
            id = "episode-1",
            episodeId = "episode-1",
            completionPercent = 95,
            completedAt = System.currentTimeMillis(),
            totalDurationMs = 600000
        )

        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = listOf(episode),
                isLoading = false,
                error = null
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Episode should be displayed (without podcast details, will show episode ID)
        composeTestRule.onNodeWithText("Episode episode-1").assertIsDisplayed()
        composeTestRule.onNodeWithText("95% complete").assertIsDisplayed()
    }

    @Test
    fun playedListScreen_clickEpisode_triggersCallback() {
        val episode = CompletedEpisodeEntity(
            id = "episode-2",
            episodeId = "episode-2",
            completionPercent = 100,
            completedAt = System.currentTimeMillis(),
            totalDurationMs = 600000
        )

        var clickedEpisodeId: String? = null

        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = listOf(episode),
                isLoading = false,
                error = null
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = { episodeId -> clickedEpisodeId = episodeId },
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Click episode card
        composeTestRule.onNodeWithText("Episode episode-2").performClick()

        // Verify callback was called
        assert(clickedEpisodeId == "episode-2") {
            "Expected episode-2, got $clickedEpisodeId"
        }
    }

    @Test
    fun playedListScreen_clickReplay_triggersCallback() {
        val episode = CompletedEpisodeEntity(
            id = "episode-3",
            episodeId = "episode-3",
            completionPercent = 100,
            completedAt = System.currentTimeMillis(),
            totalDurationMs = 600000
        )

        var replayedEpisodeId: String? = null

        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = listOf(episode),
                isLoading = false,
                error = null
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = { episodeId -> replayedEpisodeId = episodeId },
                viewModel = mockViewModel
            )
        }

        // Find and click replay button (IconButton with PlayArrow icon)
        composeTestRule.onNodeWithContentDescription("Replay from start").performClick()

        // Verify callback was called
        assert(replayedEpisodeId == "episode-3") {
            "Expected episode-3, got $replayedEpisodeId"
        }
    }

    @Test
    fun playedListScreen_multipleEpisodes_displayedCorrectly() {
        val now = System.currentTimeMillis()
        val episodes = listOf(
            CompletedEpisodeEntity(
                id = "episode-1",
                episodeId = "episode-1",
                completionPercent = 90,
                completedAt = now - 86400000, // Yesterday
                totalDurationMs = 600000
            ),
            CompletedEpisodeEntity(
                id = "episode-2",
                episodeId = "episode-2",
                completionPercent = 95,
                completedAt = now, // Today
                totalDurationMs = 600000
            )
        )

        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = episodes,
                isLoading = false,
                error = null
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Both episodes should be displayed
        composeTestRule.onNodeWithText("Episode episode-1").assertIsDisplayed()
        composeTestRule.onNodeWithText("90% complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Episode episode-2").assertIsDisplayed()
        composeTestRule.onNodeWithText("95% complete").assertIsDisplayed()

        // Check date formatting
        composeTestRule.onNodeWithText("Yesterday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
    }

    @Test
    fun playedListScreen_retryButton_clearsErrorAndReloads() {
        every { mockViewModel.uiState } returns MutableStateFlow(
            com.strollcast.app.viewmodels.PlayedUiState(
                completedEpisodes = emptyList(),
                isLoading = false,
                error = "Network error"
            )
        )

        composeTestRule.setContent {
            PlayedListScreen(
                onEpisodeClick = {},
                onReplayClick = {},
                viewModel = mockViewModel
            )
        }

        // Click retry button
        composeTestRule.onNodeWithText("Retry").performClick()

        // Verify viewModel methods were called
        io.mockk.verify { mockViewModel.clearError() }
        io.mockk.verify { mockViewModel.loadCompletedEpisodes() }
    }
}
