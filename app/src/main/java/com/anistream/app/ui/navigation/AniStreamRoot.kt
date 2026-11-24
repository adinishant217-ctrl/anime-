package com.anistream.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anistream.app.di.AppContainer
import com.anistream.app.ui.screen.details.DetailsScreen
import com.anistream.app.ui.screen.details.DetailsViewModel
import com.anistream.app.ui.screen.episodes.EpisodeListScreen
import com.anistream.app.ui.screen.episodes.EpisodeListViewModel
import com.anistream.app.ui.screen.explore.ExploreMode
import com.anistream.app.ui.screen.explore.ExploreScreen
import com.anistream.app.ui.screen.explore.ExploreViewModel
import com.anistream.app.ui.screen.home.HomeScreen
import com.anistream.app.ui.screen.home.HomeViewModel
import com.anistream.app.ui.screen.library.LibraryScreen
import com.anistream.app.ui.screen.library.LibraryViewModel
import com.anistream.app.ui.screen.player.PlayerScreen
import com.anistream.app.ui.screen.player.PlayerViewModel
import com.anistream.app.ui.screen.profile.ProfileScreen
import com.anistream.app.ui.screen.profile.ProfileViewModel
import com.anistream.app.ui.screen.search.SearchScreen
import com.anistream.app.ui.screen.search.SearchViewModel
import com.anistream.app.ui.screen.settings.SettingsScreen
import com.anistream.app.ui.screen.settings.SettingsViewModel

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.EXPLORE_BASE, "Explore", Icons.Filled.Explore),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search),
    BottomNavItem(Routes.LIBRARY, "Library", Icons.Filled.VideoLibrary),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

private val topLevelRoutes = setOf(
    Routes.HOME,
    Routes.EXPLORE_BASE,
    Routes.SEARCH,
    Routes.LIBRARY,
    Routes.PROFILE
)

@Composable
fun AniStreamRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentBaseRoute = currentRoute?.substringBefore('?')
    val isPlayer = currentBaseRoute == "player/{animeSlug}/{episodeSlug}"
    val showBottomBar = currentBaseRoute != null && currentBaseRoute in topLevelRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                AniStreamBottomBar(
                    selectedRoute = currentBaseRoute,
                    onSelect = { item ->
                        navController.navigate(item.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = if (isPlayer) {
                Modifier.fillMaxSize()
            } else {
                Modifier.padding(innerPadding)
            }
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            HomeViewModel(
                                animeRepository = container.animeRepository,
                                libraryRepository = container.libraryRepository
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onAnimeClick = { anime -> navController.navigate(Routes.detail(anime.slug)) },
                    onContinueClick = { entry ->
                        navController.navigate(Routes.player(entry.animeSlug, entry.episodeSlug))
                    },
                    onBrowseOngoing = { navController.navigate(Routes.explore("ongoing")) },
                    onBrowseCompleted = { navController.navigate(Routes.explore("completed")) },
                    onRetry = viewModel::load
                )
            }

            composable(
                route = Routes.EXPLORE,
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "ongoing"
                    }
                )
            ) { entry ->
                val initialMode = entry.arguments?.getString("mode").orEmpty()
                val viewModel: ExploreViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ExploreViewModel(container.animeRepository) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(initialMode) {
                    when (initialMode) {
                        "completed" -> viewModel.setMode(ExploreMode.COMPLETED)
                        "schedule" -> viewModel.setMode(ExploreMode.SCHEDULE)
                        else -> viewModel.setMode(ExploreMode.ONGOING)
                    }
                }
                ExploreScreen(
                    state = state,
                    onModeSelected = viewModel::setMode,
                    onGenreSelected = viewModel::selectGenre,
                    onClearGenre = viewModel::clearGenre,
                    onAnimeClick = { anime -> navController.navigate(Routes.detail(anime.slug)) },
                    onRetry = viewModel::retry,
                    onLoadMore = viewModel::loadMore
                )
            }

            composable(Routes.SEARCH) {
                val viewModel: SearchViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            SearchViewModel(
                                animeRepository = container.animeRepository,
                                settingsRepository = container.settingsRepository
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                SearchScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = viewModel::onSubmit,
                    onHistorySelected = viewModel::onHistorySelected,
                    onClearHistory = viewModel::clearHistory,
                    onRetry = viewModel::retry,
                    onAnimeClick = { anime -> navController.navigate(Routes.detail(anime.slug)) }
                )
            }

            composable(Routes.LIBRARY) {
                val viewModel: LibraryViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { LibraryViewModel(container.libraryRepository) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                LibraryScreen(
                    state = state,
                    onAnimeClick = { slug -> navController.navigate(Routes.detail(slug)) },
                    onHistoryClick = { entry ->
                        navController.navigate(Routes.player(entry.animeSlug, entry.episodeSlug))
                    },
                    onRemoveWatchlist = viewModel::removeFromWatchlist,
                    onRemoveHistory = viewModel::removeHistoryEntry,
                    onClearHistory = viewModel::clearHistory
                )
            }

            composable(Routes.PROFILE) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ProfileViewModel(container.libraryRepository) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                ProfileScreen(
                    state = state,
                    onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.SETTINGS) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { SettingsViewModel(container.settingsRepository) }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onThemeMode = viewModel::setThemeMode,
                    onAutoplayNext = viewModel::setAutoplayNext,
                    onPreferHighestQuality = viewModel::setPreferHighestQuality,
                    onClearSearchHistory = viewModel::clearSearchHistory
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { entry ->
                val slug = entry.arguments?.getString("slug").orEmpty()
                val viewModel: DetailsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            DetailsViewModel(
                                animeSlug = slug,
                                animeRepository = container.animeRepository,
                                libraryRepository = container.libraryRepository
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                DetailsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRetry = viewModel::load,
                    onToggleWatchlist = viewModel::toggleWatchlist,
                    onPlayEpisode = { episode ->
                        navController.navigate(Routes.player(slug, episode.slug))
                    },
                    onViewAllEpisodes = { navController.navigate(Routes.episodes(slug)) }
                )
            }

            composable(
                route = Routes.EPISODES,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { entry ->
                val slug = entry.arguments?.getString("slug").orEmpty()
                val viewModel: EpisodeListViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            EpisodeListViewModel(
                                animeSlug = slug,
                                animeRepository = container.animeRepository,
                                libraryRepository = container.libraryRepository
                            )
                        }
                    }
                )
                val state by viewModel.state.collectAsStateWithLifecycle()
                EpisodeListScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRetry = viewModel::load,
                    onEpisodeClick = { episode ->
                        navController.navigate(Routes.player(slug, episode.slug))
                    }
                )
            }

            composable(
                route = Routes.PLAYER,
                arguments = listOf(
                    navArgument("animeSlug") { type = NavType.StringType },
                    navArgument("episodeSlug") { type = NavType.StringType }
                )
            ) { entry ->
                val animeSlug = entry.arguments?.getString("animeSlug").orEmpty()
                val episodeSlug = entry.arguments?.getString("episodeSlug").orEmpty()
                val viewModel: PlayerViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            PlayerViewModel(
                                animeSlug = animeSlug,
                                initialEpisodeSlug = episodeSlug,
                                animeRepository = container.animeRepository,
                                libraryRepository = container.libraryRepository,
                                settingsRepository = container.settingsRepository,
                                sourceResolver = container.createPlaybackResolver(),
                                engine = container.createPlaybackEngine(),
                                applicationScope = container.applicationScope
                            )
                        }
                    }
                )
                PlayerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AniStreamBottomBar(
    selectedRoute: String?,
    onSelect: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = selectedRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
