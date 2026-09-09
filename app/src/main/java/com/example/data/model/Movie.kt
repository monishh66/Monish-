package com.example.data.model

data class Movie(
    val id: String,
    val title: String,
    val year: Int,
    val durationMinutes: Int,
    val ageRating: String,
    val rating: Float,
    val genres: List<String>,
    val synopsis: String,
    val director: String,
    val cast: List<String>,
    val streamUrl: String,
    val posterUrl: String,
    val backdropUrl: String,
    val fileSizeBytes: Long,
    val moods: List<String>,
    val featured: Boolean = false,
    val trending: Boolean = false,
    val isWatchlist: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlaybackPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val lastWatchedTimestamp: Long = 0L,
    val downloadedLocalPath: String? = null,
    val downloadProgress: Int? = null,
    val matchPercentage: Int = 90
)

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

enum class StreamQuality(val label: String, val resolution: String) {
    QUALITY_1080P("1080p FHD", "1920x1080"),
    QUALITY_720P("720p HD", "1280x720"),
    QUALITY_480P("480p SD", "854x480"),
    QUALITY_SAVER("Data Saver", "640x360")
}

data class PlaybackInfo(
    val movieId: String,
    val title: String,
    val streamUri: String,
    val isOffline: Boolean,
    val initialPositionMs: Long,
    val durationMs: Long
)
