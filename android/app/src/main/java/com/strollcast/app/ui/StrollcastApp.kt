package com.strollcast.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.strollcast.app.ui.screens.PodcastListScreen
import com.strollcast.app.ui.screens.PlayerScreen
import com.strollcast.app.ui.screens.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Podcasts : Screen("podcasts", "Podcasts", Icons.Filled.Home)
    object Player : Screen("player", "Player", Icons.Filled.PlayCircle) {
        fun createRoute(podcastId: String) = "player/$podcastId"
    }
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrollcastApp() {
    val navController = rememberNavController()
    val items = listOf(Screen.Podcasts, Screen.Player, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
            composable("${Screen.Player.route}/{podcastId}") { backStackEntry ->
                val podcastId = backStackEntry.arguments?.getString("podcastId")
                PlayerScreen(podcastId = podcastId)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
