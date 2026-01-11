package com.strollcast.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.strollcast.app.models.TranscriptCue
import com.strollcast.app.viewmodels.TranscriptUiState
import com.strollcast.app.viewmodels.TranscriptViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TranscriptScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: TranscriptViewModel
    private lateinit var uiStateFlow: MutableStateFlow<TranscriptUiState>
    private var lastSeekPosition: Long? = null

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow(TranscriptUiState())
        every { mockViewModel.uiState } returns uiStateFlow

        lastSeekPosition = null
    }

    @Test
    fun transcriptScreen_displaysLoadingState() {
        uiStateFlow.value = TranscriptUiState(isLoading = true)

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        // Should show loading indicator (CircularProgressIndicator is present)
        // Note: We can't easily test CircularProgressIndicator directly, but we can verify no error is shown
        composeTestRule.onNodeWithText("Transcript Unavailable").assertDoesNotExist()
        composeTestRule.onNodeWithText("No transcript available for this episode").assertDoesNotExist()
    }

    @Test
    fun transcriptScreen_displaysErrorState() {
        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            error = "Network error"
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("Transcript Unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun transcriptScreen_displaysEmptyState() {
        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            transcript = emptyList()
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = null,
                currentPosition = 0,
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        composeTestRule.onNodeWithText("No transcript available for this episode").assertIsDisplayed()
    }

    @Test
    fun transcriptScreen_displaysTranscriptLines() {
        val transcript = listOf(
            TranscriptCue(
                startTime = 0,
                endTime = 5000,
                speaker = "Eric",
                text = "Welcome to Strollcast"
            ),
            TranscriptCue(
                startTime = 5000,
                endTime = 10000,
                speaker = "Maya",
                text = "Today's episode about transformers"
            ),
            TranscriptCue(
                startTime = 10000,
                endTime = 15000,
                speaker = "Eric",
                text = "Let's dive into the paper"
            )
        )

        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            transcript = transcript
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        // Verify all transcript lines are displayed
        composeTestRule.onNodeWithText("Eric").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome to Strollcast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Maya").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today's episode about transformers").assertIsDisplayed()
    }

    @Test
    fun transcriptScreen_tapLineTriggersSeekWithCorrectTimestamp() {
        val transcript = listOf(
            TranscriptCue(
                startTime = 0,
                endTime = 5000,
                speaker = "Eric",
                text = "First line"
            ),
            TranscriptCue(
                startTime = 5000,
                endTime = 10000,
                speaker = "Maya",
                text = "Second line to tap"
            ),
            TranscriptCue(
                startTime = 10000,
                endTime = 15000,
                speaker = "Eric",
                text = "Third line"
            )
        )

        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            transcript = transcript
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = { position -> lastSeekPosition = position },
                viewModel = mockViewModel
            )
        }

        // Tap on second line
        composeTestRule.onNodeWithText("Second line to tap").performClick()

        // Verify seek was called with correct timestamp
        assert(lastSeekPosition == 5000L) {
            "Expected seek position 5000ms, got $lastSeekPosition"
        }
    }

    @Test
    fun transcriptScreen_multipleTapsSeekToCorrectPositions() {
        val transcript = listOf(
            TranscriptCue(
                startTime = 0,
                endTime = 5000,
                speaker = "Eric",
                text = "First line to tap"
            ),
            TranscriptCue(
                startTime = 5000,
                endTime = 10000,
                speaker = "Maya",
                text = "Second line"
            ),
            TranscriptCue(
                startTime = 10000,
                endTime = 15000,
                speaker = "Eric",
                text = "Third line to tap"
            )
        )

        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            transcript = transcript
        )

        val seekPositions = mutableListOf<Long>()

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = { position -> seekPositions.add(position) },
                viewModel = mockViewModel
            )
        }

        // Tap first line
        composeTestRule.onNodeWithText("First line to tap").performClick()

        // Tap third line
        composeTestRule.onNodeWithText("Third line to tap").performClick()

        // Verify both seeks happened with correct timestamps
        assert(seekPositions.size == 2) {
            "Expected 2 seek operations, got ${seekPositions.size}"
        }
        assert(seekPositions[0] == 0L) {
            "Expected first seek to 0ms, got ${seekPositions[0]}"
        }
        assert(seekPositions[1] == 10000L) {
            "Expected second seek to 10000ms, got ${seekPositions[1]}"
        }
    }

    @Test
    fun transcriptScreen_retryButtonReloadsTranscript() {
        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            error = "Failed to load"
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        // Click retry button
        composeTestRule.onNodeWithText("Retry").performClick()

        // Verify clearError and loadTranscript were called
        verify { mockViewModel.clearError() }
        verify { mockViewModel.loadTranscript("test-episode", "https://example.com/transcript.vtt") }
    }

    @Test
    fun transcriptScreen_highlightsCurrentLine() {
        val transcript = listOf(
            TranscriptCue(
                startTime = 0,
                endTime = 5000,
                speaker = "Eric",
                text = "First line"
            ),
            TranscriptCue(
                startTime = 5000,
                endTime = 10000,
                speaker = "Maya",
                text = "Second line highlighted"
            ),
            TranscriptCue(
                startTime = 10000,
                endTime = 15000,
                speaker = "Eric",
                text = "Third line"
            )
        )

        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            transcript = transcript,
            currentLineIndex = 1  // Second line is highlighted
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 7000,  // Within second line's time range
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        // All lines should be displayed
        composeTestRule.onNodeWithText("First line").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second line highlighted").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third line").assertIsDisplayed()

        // Note: Testing visual highlighting (background color) is difficult with compose test,
        // but we can verify the component renders with the highlighted state
    }

    @Test
    fun transcriptScreen_displaysSpeakerNames() {
        val transcript = listOf(
            TranscriptCue(
                startTime = 0,
                endTime = 5000,
                speaker = "Eric",
                text = "Content from Eric"
            ),
            TranscriptCue(
                startTime = 5000,
                endTime = 10000,
                speaker = "Maya",
                text = "Content from Maya"
            ),
            TranscriptCue(
                startTime = 10000,
                endTime = 15000,
                speaker = null,  // No speaker tag
                text = "Content without speaker"
            )
        )

        uiStateFlow.value = TranscriptUiState(
            isLoading = false,
            transcript = transcript
        )

        composeTestRule.setContent {
            TranscriptScreen(
                episodeId = "test-episode",
                transcriptUrl = "https://example.com/transcript.vtt",
                currentPosition = 0,
                onSeekTo = {},
                viewModel = mockViewModel
            )
        }

        // Verify speaker names are displayed
        composeTestRule.onNodeWithText("Eric").assertIsDisplayed()
        composeTestRule.onNodeText("Maya").assertIsDisplayed()

        // All content should be displayed
        composeTestRule.onNodeWithText("Content from Eric").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content from Maya").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content without speaker").assertIsDisplayed()
    }
}
