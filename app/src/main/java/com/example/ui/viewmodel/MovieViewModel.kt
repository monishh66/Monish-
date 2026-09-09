package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Movie
import com.example.data.model.PlaybackInfo
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeFeedState(
    val featuredMovie: Movie? = null,
    val continueWatching: List<Movie> = emptyList(),
    val recommendedForYou: List<Movie> = emptyList(),
    val trendingNow: List<Movie> = emptyList(),
    val sciFiMovies: List<Movie> = emptyList(),
    val actionMovies: List<Movie> = emptyList(),
    val classicMovies: List<Movie> = emptyList(),
    val becauseYouWatched: Pair<String, List<Movie>>? = null,
    val isOfflineMode: Boolean = false
)

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = MovieRepository(application, database)

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _activePlayback = MutableStateFlow<PlaybackInfo?>(null)
    val activePlayback: StateFlow<PlaybackInfo?> = _activePlayback.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenreFilter = MutableStateFlow("All")
    val selectedGenreFilter: StateFlow<String> = _selectedGenreFilter.asStateFlow()

    private val _selectedMoodFilter = MutableStateFlow("All")
    val selectedMoodFilter: StateFlow<String> = _selectedMoodFilter.asStateFlow()

    private val _storageUsed = MutableStateFlow(0L)
    val storageUsed: StateFlow<Long> = _storageUsed.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDataIfNeeded()
            refreshStorage()
        }
    }

    fun refreshStorage() {
        _storageUsed.value = repository.getUsedStorage()
    }

    val preferences = repository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allMovies = repository.allMoviesWithDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val continueWatching = repository.continueWatchingMovies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val watchlist = repository.watchlistMovies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedMovies = repository.downloadedMovies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val homeFeed: StateFlow<HomeFeedState> = combine(
        allMovies,
        continueWatching,
        preferences
    ) { movies, continueList, prefs ->
        val offlineOnly = prefs?.offlineOnlyMode ?: false
        val displayList = if (offlineOnly) {
            movies.filter { it.downloadedLocalPath != null }
        } else {
            movies
        }

        val featured = displayList.firstOrNull { it.featured } ?: displayList.firstOrNull()
        val recommended = displayList.sortedByDescending { it.matchPercentage }
        val trending = displayList.filter { it.trending }
        val sciFi = displayList.filter { m -> m.genres.any { it.contains("Sci-Fi", ignoreCase = true) } }
        val action = displayList.filter { m -> m.genres.any { it.contains("Action", ignoreCase = true) || it.contains("Adventure", ignoreCase = true) } }
        val classic = displayList.filter { m -> m.genres.any { it.contains("Classic", ignoreCase = true) } || m.year < 1990 }

        // "Because you watched" shelf from most recent in continue watching or first watched
        val lastWatched = continueList.firstOrNull()
        val becauseWatched = if (lastWatched != null) {
            val primaryGenre = lastWatched.genres.firstOrNull() ?: "Cinema"
            val similar = displayList.filter { it.id != lastWatched.id && it.genres.any { g -> g.equals(primaryGenre, ignoreCase = true) } }
            if (similar.isNotEmpty()) {
                "Because you watched ${lastWatched.title}" to similar
            } else null
        } else null

        HomeFeedState(
            featuredMovie = featured,
            continueWatching = continueList,
            recommendedForYou = recommended,
            trendingNow = trending,
            sciFiMovies = sciFi,
            actionMovies = action,
            classicMovies = classic,
            becauseYouWatched = becauseWatched,
            isOfflineMode = offlineOnly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeFeedState()
    )

    val filteredExploreMovies: StateFlow<List<Movie>> = combine(
        allMovies,
        searchQuery,
        selectedGenreFilter,
        selectedMoodFilter
    ) { movies, query, genre, mood ->
        movies.filter { movie ->
            val matchesQuery = query.isBlank() ||
                    movie.title.contains(query, ignoreCase = true) ||
                    movie.genres.any { it.contains(query, ignoreCase = true) } ||
                    movie.director.contains(query, ignoreCase = true) ||
                    movie.cast.any { it.contains(query, ignoreCase = true) }

            val matchesGenre = genre == "All" || movie.genres.any { it.equals(genre, ignoreCase = true) }
            val matchesMood = mood == "All" || movie.moods.any { it.contains(mood, ignoreCase = true) }

            matchesQuery && matchesGenre && matchesMood
        }.sortedByDescending { it.matchPercentage }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectMovie(movie: Movie?) {
        _selectedMovie.value = movie
    }

    fun startPlayback(movie: Movie, forceOffline: Boolean = false) {
        val uri = if (forceOffline || movie.downloadedLocalPath != null) {
            movie.downloadedLocalPath ?: movie.streamUrl
        } else {
            movie.streamUrl
        }
        val isOffline = forceOffline || movie.downloadedLocalPath != null
        _activePlayback.value = PlaybackInfo(
            movieId = movie.id,
            title = movie.title,
            streamUri = uri,
            isOffline = isOffline,
            initialPositionMs = movie.lastPlaybackPositionMs,
            durationMs = movie.totalDurationMs
        )
    }

    fun closePlayback() {
        _activePlayback.value = null
    }

    fun updatePlaybackPosition(movieId: String, positionMs: Long, totalDurationMs: Long) {
        viewModelScope.launch {
            repository.savePlaybackPosition(movieId, positionMs, totalDurationMs)
        }
    }

    fun toggleWatchlist(movie: Movie) {
        viewModelScope.launch {
            repository.toggleWatchlist(movie.id, !movie.isWatchlist)
            _selectedMovie.value = _selectedMovie.value?.copy(isWatchlist = !movie.isWatchlist)
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavorite(movie.id, !movie.isFavorite)
            _selectedMovie.value = _selectedMovie.value?.copy(isFavorite = !movie.isFavorite)
        }
    }

    fun startDownload(movie: Movie) {
        repository.startDownload(movie)
        refreshStorage()
    }

    fun cancelDownload(movieId: String) {
        repository.cancelDownload(movieId)
        refreshStorage()
    }

    fun deleteDownload(movieId: String) {
        viewModelScope.launch {
            repository.deleteDownload(movieId)
            refreshStorage()
        }
    }

    fun updatePreferences(selectedGenres: List<String>, mood: String, offlineMode: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(selectedGenres, mood, offlineMode)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGenreFilter(genre: String) {
        _selectedGenreFilter.value = genre
    }

    fun setSelectedMoodFilter(mood: String) {
        _selectedMoodFilter.value = mood
    }

    fun toggleOfflineMode() {
        val currentPref = preferences.value
        val newOfflineMode = !(currentPref?.offlineOnlyMode ?: false)
        viewModelScope.launch {
            repository.updatePreferences(
                selectedGenres = currentPref?.selectedGenres?.split(",") ?: listOf("Sci-Fi", "Action", "Animation"),
                mood = currentPref?.currentMood ?: "All",
                offlineMode = newOfflineMode
            )
        }
    }
}
