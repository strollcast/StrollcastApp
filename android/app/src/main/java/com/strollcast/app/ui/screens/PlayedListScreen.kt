package com.strollcast.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strollcast.app.di.RepositoryModule
import com.strollcast.app.models.Podcast
import com.strollcast.app.repository.PodcastRepository
import com.strollcast.app.ui.components.CompletedEpisodeCard
import com.strollcast.app.viewmodels.PlayedViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.launch

/**
 * Entry point for accessing PodcastRepository in Compose
 */
@EntryPoint
@InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PodcastRepositoryEntryPoint {
    fun podcastRepository(): PodcastRepository
}

/**
 * Screen displaying completed/played episodes
 *
 * @param onEpisodeClick Called when user taps an episode
 * @param onReplayClick Called when user wants to replay from start
 * @param viewModel Played view model
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayedListScreen(
    onEpisodeClick: (String) -> Unit,
    onReplayClick: (String) -> Unit,
    viewModel: PlayedViewModel = hiltViewModel()
) {
    // Get PodcastRepository from Hilt
    val context = LocalContext.current
    val podcastRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PodcastRepositoryEntryPoint::class.java
        ).podcastRepository()
    }
    val uiState by viewModel.uiState.collectAsState()

    // Cache for podcast details
    val podcastCache = remember { mutableStateMapOf<String, Podcast?>() }
    val coroutineScope = rememberCoroutineScope()

    // Load podcast details for completed episodes
    LaunchedEffect(uiState.completedEpisodes) {
        uiState.completedEpisodes.forEach { episode ->
            if (!podcastCache.containsKey(episode.episodeId)) {
                coroutineScope.launch {
                    val podcast = podcastRepository.getPodcastById(episode.episodeId)
                    podcastCache[episode.episodeId] = podcast
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Played Episodes") }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.completedEpisodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
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
                        Text(
                            text = "Error Loading Played Episodes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = {
                            viewModel.clearError()
                            viewModel.loadCompletedEpisodes()
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.completedEpisodes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "No played episodes yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Episodes you've completed will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.completedEpisodes,
                        key = { it.id }
                    ) { episode ->
                        CompletedEpisodeCard(
                            episode = episode,
                            podcast = podcastCache[episode.episodeId],
                            onReplayClick = {
                                onReplayClick(episode.episodeId)
                            },
                            onClick = {
                                onEpisodeClick(episode.episodeId)
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
    }
}
