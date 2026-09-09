package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PlayerView
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MovieDetailSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.CinemaBackground
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.MovieViewModel

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: MovieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MovieViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val homeFeedState by viewModel.homeFeed.collectAsStateWithLifecycle()
    val allMovies by viewModel.allMovies.collectAsStateWithLifecycle()
    val filteredExploreMovies by viewModel.filteredExploreMovies.collectAsStateWithLifecycle()
    val downloadedMovies by viewModel.downloadedMovies.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val activePlayback by viewModel.activePlayback.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsStateWithLifecycle()
    val selectedMood by viewModel.selectedMoodFilter.collectAsStateWithLifecycle()
    val storageUsed by viewModel.storageUsed.collectAsStateWithLifecycle()

    val navTabs = listOf(
        NavTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
        NavTabItem("Explore", Icons.Filled.Search, Icons.Outlined.Search, "tab_explore"),
        NavTabItem("Downloads", Icons.Filled.Download, Icons.Outlined.Download, "tab_downloads"),
        NavTabItem("Taste & List", Icons.Filled.Tune, Icons.Outlined.Tune, "tab_profile")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        Scaffold(
            bottomBar = {
                // Bottom bar hidden during active full player
                if (activePlayback == null) {
                    NavigationBar(
                        containerColor = CinemaSurface,
                        contentColor = TextWhite,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("main_navigation_bar")
                    ) {
                        navTabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                icon = {
                                    if (tab.title == "Downloads" && downloadedMovies.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CinemaCyan) {
                                                    Text(
                                                        text = "${downloadedMovies.size}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextWhite,
                                    selectedTextColor = CinemaCrimson,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = CinemaCrimson
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        state = homeFeedState,
                        onSelectMovie = { viewModel.selectMovie(it) },
                        onPlayMovie = { movie, offline -> viewModel.startPlayback(movie, offline) },
                        onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                    )

                    1 -> ExploreScreen(
                        movies = filteredExploreMovies,
                        searchQuery = searchQuery,
                        selectedGenre = selectedGenre,
                        selectedMood = selectedMood,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSelectGenre = { viewModel.setSelectedGenreFilter(it) },
                        onSelectMood = { viewModel.setSelectedMoodFilter(it) },
                        onSelectMovie = { viewModel.selectMovie(it) }
                    )

                    2 -> DownloadsScreen(
                        downloadedMovies = downloadedMovies,
                        allMovies = allMovies,
                        storageUsedBytes = storageUsed,
                        isOfflineOnlyMode = preferences?.offlineOnlyMode ?: false,
                        onToggleOfflineMode = { viewModel.toggleOfflineMode() },
                        onPlayOffline = { viewModel.startPlayback(it, forceOffline = true) },
                        onDeleteDownload = { viewModel.deleteDownload(it) },
                        onCancelDownload = { viewModel.cancelDownload(it) },
                        onNavigateExplore = { selectedTab = 1 },
                        onSelectMovie = { viewModel.selectMovie(it) }
                    )

                    3 -> ProfileScreen(
                        userPreferences = preferences,
                        watchlist = watchlist,
                        allMovies = allMovies,
                        onUpdatePreferences = { genres, mood, offline ->
                            viewModel.updatePreferences(genres, mood, offline)
                        },
                        onSelectMovie = { viewModel.selectMovie(it) }
                    )
                }
            }
        }

        // Movie Detail Sheet
        if (selectedMovie != null) {
            val movie = selectedMovie!!
            MovieDetailSheet(
                movie = movie,
                allMovies = allMovies,
                onDismiss = { viewModel.selectMovie(null) },
                onPlay = { m, isOffline -> viewModel.startPlayback(m, isOffline) },
                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onStartDownload = { viewModel.startDownload(it) },
                onCancelDownload = { viewModel.cancelDownload(it) },
                onDeleteDownload = { viewModel.deleteDownload(it) },
                onSelectMovie = { viewModel.selectMovie(it) }
            )
        }

        // Fullscreen Active Player Overlay
        AnimatedVisibility(
            visible = activePlayback != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (activePlayback != null) {
                PlayerView(
                    playbackInfo = activePlayback!!,
                    onClose = { viewModel.closePlayback() },
                    onProgressUpdate = { pos, dur ->
                        viewModel.updatePlaybackPosition(activePlayback!!.movieId, pos, dur)
                    }
                )
            }
        }
    }
}
