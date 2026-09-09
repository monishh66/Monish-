package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val year: Int,
    val durationMinutes: Int,
    val ageRating: String,
    val rating: Float,
    val genres: String, // Comma-separated
    val synopsis: String,
    val director: String,
    val cast: String, // Comma-separated
    val streamUrl: String,
    val posterUrl: String,
    val backdropUrl: String,
    val fileSizeBytes: Long,
    val moods: String, // Comma-separated
    val featured: Boolean = false,
    val trending: Boolean = false,
    val isWatchlist: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlaybackPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val lastWatchedTimestamp: Long = 0L
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val movieId: String,
    val movieTitle: String,
    val localFilePath: String,
    val downloadStatus: String, // "DOWNLOADING", "COMPLETED", "FAILED"
    val progressPercent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadedAt: Long
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val selectedGenres: String = "Sci-Fi,Action,Animation,Mystery,Comedy",
    val currentMood: String = "All",
    val offlineOnlyMode: Boolean = false,
    val preferredQuality: String = "1080p FHD"
)
