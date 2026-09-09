package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY rating DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieById(id: String): Flow<MovieEntity?>

    @Query("SELECT * FROM movies WHERE isWatchlist = 1 ORDER BY title ASC")
    fun getWatchlistMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY rating DESC")
    fun getFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE lastPlaybackPositionMs > 5000 AND lastPlaybackPositionMs < (totalDurationMs - 10000) ORDER BY lastWatchedTimestamp DESC")
    fun getContinueWatchingMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE lastWatchedTimestamp > 0 ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getWatchHistory(): Flow<List<MovieEntity>>

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun getMovieCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(movies: List<MovieEntity>)

    @Update
    suspend fun updateMovie(movie: MovieEntity)

    @Query("UPDATE movies SET isWatchlist = :isWatchlist WHERE id = :id")
    suspend fun setWatchlist(id: String, isWatchlist: Boolean)

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE movies SET lastPlaybackPositionMs = :positionMs, totalDurationMs = :totalDurationMs, lastWatchedTimestamp = :timestamp WHERE id = :id")
    suspend fun updatePlaybackProgress(id: String, positionMs: Long, totalDurationMs: Long, timestamp: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId")
    fun getDownloadById(movieId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE movieId = :movieId")
    suspend fun getDownloadDirect(movieId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("UPDATE downloads SET progressPercent = :progress, downloadedBytes = :bytes, downloadStatus = :status WHERE movieId = :movieId")
    suspend fun updateProgress(movieId: String, progress: Int, bytes: Long, status: String)

    @Query("DELETE FROM downloads WHERE movieId = :movieId")
    suspend fun deleteDownload(movieId: String)
}

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesDirect(): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreferences(preferences: UserPreferenceEntity)
}
