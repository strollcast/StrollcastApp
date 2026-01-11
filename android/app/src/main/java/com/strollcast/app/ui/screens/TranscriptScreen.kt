package com.strollcast.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strollcast.app.ui.components.NoteDialog
import com.strollcast.app.ui.components.TranscriptLineItem
import com.strollcast.app.viewmodels.NoteViewModel
import com.strollcast.app.viewmodels.TranscriptViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun TranscriptScreen(
    episodeId: String,
    transcriptUrl: String?,
    currentPosition: Long,
    onSeekTo: (Long) -> Unit,
    viewModel: TranscriptViewModel = hiltViewModel(),
    noteViewModel: NoteViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val noteUiState by noteViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Note dialog state
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedLineIndex by remember { mutableStateOf<Int?>(null) }
    var selectedLineText by remember { mutableStateOf("") }

    // Load transcript when episodeId changes
    LaunchedEffect(episodeId) {
        viewModel.loadTranscript(episodeId, transcriptUrl)
    }

    // Load note counts when transcript is loaded
    LaunchedEffect(uiState.transcript) {
        if (uiState.transcript.isNotEmpty()) {
            // Note: We need to use line entity IDs, not indexes
            // For now, we'll load counts on-demand when creating notes
        }
    }

    // Update current position for highlighting
    LaunchedEffect(currentPosition) {
        viewModel.updateCurrentPosition(currentPosition)
    }

    // Auto-scroll to current line
    LaunchedEffect(uiState.currentLineIndex) {
        uiState.currentLineIndex?.let { index ->
            // Debounce scroll updates to avoid excessive animations
            snapshotFlow { index }
                .debounce(300)
                .collectLatest {
                    if (it in uiState.transcript.indices) {
                        listState.animateScrollToItem(it)
                    }
                }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Transcript Unavailable",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (transcriptUrl != null) {
                        Button(onClick = {
                            viewModel.clearError()
                            viewModel.loadTranscript(episodeId, transcriptUrl)
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.transcript.isEmpty() -> {
                Text(
                    text = "No transcript available for this episode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.transcript,
                        key = { it.startTime }
                    ) { cue ->
                        val index = uiState.transcript.indexOf(cue)
                        val isHighlighted = index == uiState.currentLineIndex

                        TranscriptLineItem(
                            cue = cue,
                            isHighlighted = isHighlighted,
                            onClick = { onSeekTo(cue.startTime) },
                            onLongClick = {
                                selectedLineIndex = index
                                selectedLineText = cue.text
                                showNoteDialog = true
                            }
                        )
                    }

                    // Add bottom padding for better scroll experience
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Note dialog
        NoteDialog(
            isOpen = showNoteDialog,
            transcriptText = selectedLineText,
            onSave = { content ->
                selectedLineIndex?.let { lineIndex ->
                    noteViewModel.createNoteByLineIndex(episodeId, lineIndex, content)
                }
                showNoteDialog = false
                selectedLineIndex = null
                selectedLineText = ""
            },
            onCancel = {
                showNoteDialog = false
                selectedLineIndex = null
                selectedLineText = ""
            }
        )
    }
}
