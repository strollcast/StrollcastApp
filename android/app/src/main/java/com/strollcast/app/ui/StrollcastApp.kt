package com.strollcast.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.strollcast.app.R
import com.strollcast.app.ui.screens.NotesScreen
import com.strollcast.app.ui.screens.PlayedListScreen
import com.strollcast.app.ui.screens.PodcastListScreen
import com.strollcast.app.ui.screens.PlayerScreen
import com.strollcast.app.ui.screens.SettingsScreen

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Podcasts : Screen("podcasts", R.string.nav_podcasts, Icons.Filled.Home)
    object Played : Screen("played", R.string.nav_played, Icons.Filled.CheckCircle)
    object Player : Screen("player", R.string.nav_player, Icons.Filled.PlayCircle) {
        fun createRoute(podcastId: String) = "player/$podcastId"
    }
    object Notes : Screen("notes", R.string.nav_notes, Icons.Filled.StickyNote2) {
        fun createRoute(episodeId: String? = null) = if (episodeId != null) "notes/$episodeId" else "notes"
    }
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrollcastApp() {
    val navController = rememberNavController()
    val items = listOf(Screen.Podcasts, Screen.Played, Screen.Notes, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val title = stringResource(screen.titleRes)
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            // Check if already on this screen
                            if (currentDestination?.hierarchy?.any { it.route == screen.route } != true) {
                                navController.navigate(screen.route) {
                                    // Pop everything up to the start destination
                                    popUpTo(Screen.Podcasts.route) {
                                        inclusive = false
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Podcasts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Podcasts.route) {
                PodcastListScreen(
                    onPodcastClick = { podcast ->
                        // Navigate to player with podcast ID
                        navController.navigate(Screen.Player.createRoute(podcast.id))
                    }
                )
            }
            composable(Screen.Played.route) {
                PlayedListScreen(
                    onEpisodeClick = { episodeId ->
                        navController.navigate(Screen.Player.createRoute(episodeId))
                    },
                    onReplayClick = { episodeId ->
                        // Navigate to player and it will reset position to 0
                        navController.navigate(Screen.Player.createRoute(episodeId))
                    }
                )
            }
            composable("${Screen.Player.route}/{podcastId}") { backStackEntry ->
                val podcastId = backStackEntry.arguments?.getString("podcastId")
                PlayerScreen(podcastId = podcastId)
            }
            composable(Screen.Notes.route) {
                NotesScreen()
            }
            composable("${Screen.Notes.route}/{episodeId}") { backStackEntry ->
                val episodeId = backStackEntry.arguments?.getString("episodeId")
                NotesScreen(episodeId = episodeId)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
