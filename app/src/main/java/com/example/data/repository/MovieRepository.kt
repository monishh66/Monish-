package com.example.data.repository

import android.content.Context
import com.example.data.downloader.MovieDownloadManager
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.local.MovieEntity
import com.example.data.local.SampleMovieData
import com.example.data.local.UserPreferenceEntity
import com.example.data.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class MovieRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val movieDao = database.movieDao()
    private val downloadDao = database.downloadDao()
    private val userPreferenceDao = database.userPreferenceDao()

    val downloadManager = MovieDownloadManager(context, downloadDao)

    suspend fun initializeDataIfNeeded() = withContext(Dispatchers.IO) {
        val count = movieDao.getMovieCount()
        if (count == 0) {
            movieDao.insertAll(SampleMovieData.getInitialMovies())
        }
        val pref = userPreferenceDao.getPreferencesDirect()
        if (pref == null) {
            userPreferenceDao.setPreferences(UserPreferenceEntity())
        }
    }

    val userPreferences: Flow<UserPreferenceEntity?> = userPreferenceDao.getPreferences()

    val allMoviesWithDownloads: Flow<List<Movie>> = combine(
        movieDao.getAllMovies(),
        downloadDao.getAllDownloads(),
        userPreferenceDao.getPreferences()
    ) { entities, downloads, prefs ->
        val downloadMap = downloads.associateBy { it.movieId }
        val preferredGenres = prefs?.selectedGenres?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
        val currentMood = prefs?.currentMood ?: "All"

        entities.map { entity ->
            val download = downloadMap[entity.id]
            val localPath = if (download != null && download.downloadStatus == "COMPLETED") {
                val file = File(download.localFilePath)
                if (file.exists()) download.localFilePath else null
            } else null

            val matchScore = calculatePersonalizedMatch(
                movie = entity,
                preferredGenres = preferredGenres,
                currentMood = currentMood
            )

            entity.toDomain(
                download = download,
                validLocalPath = localPath,
                match = matchScore
            )
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    val continueWatchingMovies: Flow<List<Movie>> = combine(
        movieDao.getContinueWatchingMovies(),
        downloadDao.getAllDownloads()
    ) { entities, downloads ->
        val downloadMap = downloads.associateBy { it.movieId }
        entities.map { entity ->
            val download = downloadMap[entity.id]
            entity.toDomain(download, null, 95)
        }
    }.flowOn(Dispatchers.IO)

    val watchlistMovies: Flow<List<Movie>> = combine(
        movieDao.getWatchlistMovies(),
        downloadDao.getAllDownloads()
    ) { entities, downloads ->
        val downloadMap = downloads.associateBy { it.movieId }
        entities.map { it.toDomain(downloadMap[it.id], null, 90) }
    }.flowOn(Dispatchers.IO)

    val downloadedMovies: Flow<List<Movie>> = combine(
        movieDao.getAllMovies(),
        downloadDao.getAllDownloads()
    ) { entities, downloads ->
        val completedDownloads = downloads.filter { it.downloadStatus == "COMPLETED" }.associateBy { it.movieId }
        entities.filter { completedDownloads.containsKey(it.id) }.map { entity ->
            val dl = completedDownloads[entity.id]
            entity.toDomain(dl, dl?.localFilePath, 95)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun toggleWatchlist(movieId: String, isWatchlist: Boolean) = withContext(Dispatchers.IO) {
        movieDao.setWatchlist(movieId, isWatchlist)
    }

    suspend fun toggleFavorite(movieId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        movieDao.setFavorite(movieId, isFavorite)
    }

    suspend fun savePlaybackPosition(movieId: String, positionMs: Long, totalDurationMs: Long) = withContext(Dispatchers.IO) {
        movieDao.updatePlaybackProgress(
            id = movieId,
            positionMs = positionMs,
            totalDurationMs = totalDurationMs,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun updatePreferences(selectedGenres: List<String>, mood: String, offlineMode: Boolean) = withContext(Dispatchers.IO) {
        val entity = UserPreferenceEntity(
            id = 1,
            selectedGenres = selectedGenres.joinToString(","),
            currentMood = mood,
            offlineOnlyMode = offlineMode
        )
        userPreferenceDao.setPreferences(entity)
    }

    fun startDownload(movie: Movie) {
        downloadManager.startDownload(
            movieId = movie.id,
            movieTitle = movie.title,
            streamUrl = movie.streamUrl,
            fileSizeBytes = movie.fileSizeBytes
        )
    }

    suspend fun deleteDownload(movieId: String) {
        downloadManager.deleteDownload(movieId)
    }

    fun cancelDownload(movieId: String) {
        downloadManager.cancelDownload(movieId)
    }

    fun getUsedStorage(): Long = downloadManager.getUsedStorageBytes()

    private fun calculatePersonalizedMatch(
        movie: MovieEntity,
        preferredGenres: List<String>,
        currentMood: String
    ): Int {
        var score = 65 // Base score

        val movieGenres = movie.genres.split(",").map { it.trim().lowercase() }
        val matchingGenres = movieGenres.count { genre ->
            preferredGenres.any { it in genre || genre in it }
        }
        score += (matchingGenres * 8).coerceAtMost(24)

        // Mood match
        if (currentMood != "All") {
            if (movie.moods.contains(currentMood, ignoreCase = true)) {
                score += 12
            }
        }

        // Quality rating boost
        score += ((movie.rating - 4.0f) * 8).toInt().coerceIn(0, 10)

        // Favorite bonus
        if (movie.isFavorite) score += 6

        return score.coerceIn(72, 99)
    }

    private fun MovieEntity.toDomain(
        download: DownloadEntity?,
        validLocalPath: String?,
        match: Int
    ): Movie {
        return Movie(
            id = id,
            title = title,
            year = year,
            durationMinutes = durationMinutes,
            ageRating = ageRating,
            rating = rating,
            genres = genres.split(",").map { it.trim() },
            synopsis = synopsis,
            director = director,
            cast = cast.split(",").map { it.trim() },
            streamUrl = streamUrl,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            fileSizeBytes = fileSizeBytes,
            moods = moods.split(",").map { it.trim() },
            featured = featured,
            trending = trending,
            isWatchlist = isWatchlist,
            isFavorite = isFavorite,
            lastPlaybackPositionMs = lastPlaybackPositionMs,
            totalDurationMs = totalDurationMs,
            lastWatchedTimestamp = lastWatchedTimestamp,
            downloadedLocalPath = validLocalPath,
            downloadProgress = if (download != null && download.downloadStatus == "DOWNLOADING") download.progressPercent else null,
            matchPercentage = match
        )
    }
}
