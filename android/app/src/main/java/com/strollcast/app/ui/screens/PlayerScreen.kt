package com.strollcast.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.strollcast.app.viewmodels.PlayerUiState
import com.strollcast.app.viewmodels.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    podcastId: String? = null,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Only show transcript tab if transcript URL is available
    val hasTranscript = uiState.currentPodcast?.transcriptUrl != null
    val tabs = if (hasTranscript) listOf("Player", "Transcript") else listOf("Player")

    // Create and set up ExoPlayer
    DisposableEffect(Unit) {
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        viewModel.setPlayer(player)

        onDispose {
            player.release()
        }
    }

    // Load podcast when screen opens with podcast ID
    LaunchedEffect(podcastId) {
        if (podcastId != null) {
            viewModel.loadPodcastById(podcastId)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            viewModel.updatePosition()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Now Playing") }
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTabIndex) {
            0 -> PlayerContent(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            1 -> {
                when {
                    uiState.currentPodcast == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episode selected",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    uiState.currentPodcast!!.transcriptUrl == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Transcript Not Available",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "This episode does not have a transcript",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        TranscriptScreen(
                            episodeId = uiState.currentPodcast!!.id,
                            transcriptUrl = uiState.currentPodcast!!.transcriptUrl,
                            currentPosition = uiState.currentPosition,
                            onSeekTo = { position -> viewModel.seekTo(position) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Podcast Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.currentPodcast != null) {
                Text(
                    text = uiState.currentPodcast!!.title ?: "Untitled",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.currentPodcast!!.authors ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No podcast selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (uiState.duration > 0) {
                    uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                } else 0f,
                onValueChange = { value ->
                    val position = (value * uiState.duration).toLong()
                    viewModel.seekTo(position)
                },
                enabled = uiState.currentPodcast != null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(uiState.currentPosition),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatTime(uiState.duration),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.skipBackward(15) },
                enabled = uiState.currentPodcast != null
            ) {
                Icon(
                    Icons.Filled.Replay,
                    contentDescription = "Skip back 15s",
                    modifier = Modifier.size(32.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    if (uiState.isPlaying) {
                        viewModel.pause()
                    } else {
                        viewModel.play()
                    }
                },
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = { viewModel.skipForward(15) },
                enabled = uiState.currentPodcast != null
            ) {
                Icon(
                    Icons.Filled.Forward30,
                    contentDescription = "Skip forward 15s",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
