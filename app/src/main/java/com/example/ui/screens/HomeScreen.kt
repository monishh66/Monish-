package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Movie
import com.example.ui.components.HeroBanner
import com.example.ui.components.MovieCard
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaBackground
import com.example.ui.theme.CinemaCrimson
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.HomeFeedState

@Composable
fun HomeScreen(
    state: HomeFeedState,
    onSelectMovie: (Movie) -> Unit,
    onPlayMovie: (Movie, Boolean) -> Unit,
    onToggleWatchlist: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBackground)
            .testTag("home_screen_feed"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Offline mode alert banner if active
        if (state.isOfflineMode) {
            item {
                Surface(
                    color = CinemaCyan.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = null,
                            tint = CinemaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Offline Mode Active",
                                color = CinemaCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Showing downloaded movies available without an internet connection.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Hero Spotlight Banner
        if (state.featuredMovie != null) {
            item {
                HeroBanner(
                    movie = state.featuredMovie,
                    onPlayClick = {
                        onPlayMovie(state.featuredMovie, state.featuredMovie.downloadedLocalPath != null)
                    },
                    onDetailsClick = { onSelectMovie(state.featuredMovie) },
                    onToggleWatchlist = { onToggleWatchlist(state.featuredMovie) }
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Continue Watching Shelf
        if (state.continueWatching.isNotEmpty()) {
            item {
                MovieSection(
                    title = "Continue Watching",
                    icon = Icons.Default.PlayCircleOutline,
                    iconTint = CinemaCrimson,
                    movies = state.continueWatching,
                    onSelectMovie = onSelectMovie,
                    showProgress = true
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Personalized "Recommended For You" Shelf
        if (state.recommendedForYou.isNotEmpty()) {
            item {
                MovieSection(
                    title = "Personalized For You",
                    subtitle = "Curated based on your taste & watch history",
                    icon = Icons.Default.Recommend,
                    iconTint = CinemaAmber,
                    movies = state.recommendedForYou,
                    onSelectMovie = onSelectMovie
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Dynamic "Because you watched [Title]" Shelf
        if (state.becauseYouWatched != null) {
            val (title, list) = state.becauseYouWatched
            item {
                MovieSection(
                    title = title,
                    movies = list,
                    onSelectMovie = onSelectMovie
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Trending Now Carousel
        if (state.trendingNow.isNotEmpty()) {
            item {
                MovieSection(
                    title = "Trending This Week",
                    icon = Icons.Default.TrendingUp,
                    iconTint = CinemaCrimson,
                    movies = state.trendingNow,
                    onSelectMovie = onSelectMovie
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Sci-Fi & Cyberpunk
        if (state.sciFiMovies.isNotEmpty()) {
            item {
                MovieSection(
                    title = "Sci-Fi & Cyberpunk",
                    movies = state.sciFiMovies,
                    onSelectMovie = onSelectMovie
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Action & Adventure
        if (state.actionMovies.isNotEmpty()) {
            item {
                MovieSection(
                    title = "Action & Adventure",
                    movies = state.actionMovies,
                    onSelectMovie = onSelectMovie
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Classic Cinema
        if (state.classicMovies.isNotEmpty()) {
            item {
                MovieSection(
                    title = "Classic Cinema Vault",
                    subtitle = "Remastered public domain masterpieces",
                    movies = state.classicMovies,
                    onSelectMovie = onSelectMovie
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MovieSection(
    title: String,
    movies: List<Movie>,
    onSelectMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = CinemaCrimson,
    showProgress: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onSelectMovie(movie) },
                    showProgress = showProgress
                )
            }
        }
    }
}
