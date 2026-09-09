package com.example.data.downloader

import android.content.Context
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class MovieDownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun startDownload(
        movieId: String,
        movieTitle: String,
        streamUrl: String,
        fileSizeBytes: Long
    ) {
        if (activeJobs.containsKey(movieId)) return

        val downloadsDir = File(context.filesDir, "downloads").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(downloadsDir, "${movieId}.mp4")

        val job = scope.launch {
            try {
                // Initialize in DB
                downloadDao.insertOrUpdate(
                    DownloadEntity(
                        movieId = movieId,
                        movieTitle = movieTitle,
                        localFilePath = targetFile.absolutePath,
                        downloadStatus = "DOWNLOADING",
                        progressPercent = 0,
                        downloadedBytes = 0L,
                        totalBytes = fileSizeBytes,
                        downloadedAt = System.currentTimeMillis()
                    )
                )

                val url = URL(streamUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    requestMethod = "GET"
                }
                connection.connect()

                val contentLength = connection.contentLength.toLong().let {
                    if (it > 0) it else fileSizeBytes
                }

                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastReportedPercent = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            val percent = if (contentLength > 0) {
                                ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                50
                            }

                            if (percent > lastReportedPercent + 2 || percent == 100) {
                                lastReportedPercent = percent
                                downloadDao.updateProgress(
                                    movieId = movieId,
                                    progress = percent,
                                    bytes = totalRead,
                                    status = "DOWNLOADING"
                                )
                            }
                        }
                    }
                }

                // Completed
                downloadDao.insertOrUpdate(
                    DownloadEntity(
                        movieId = movieId,
                        movieTitle = movieTitle,
                        localFilePath = targetFile.absolutePath,
                        downloadStatus = "COMPLETED",
                        progressPercent = 100,
                        downloadedBytes = targetFile.length(),
                        totalBytes = targetFile.length(),
                        downloadedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: CancellationException) {
                targetFile.delete()
                downloadDao.deleteDownload(movieId)
            } catch (e: Exception) {
                targetFile.delete()
                downloadDao.updateProgress(
                    movieId = movieId,
                    progress = 0,
                    bytes = 0L,
                    status = "FAILED"
                )
            } finally {
                activeJobs.remove(movieId)
            }
        }

        activeJobs[movieId] = job
    }

    fun cancelDownload(movieId: String) {
        activeJobs[movieId]?.cancel()
        activeJobs.remove(movieId)
        scope.launch {
            val file = File(context.filesDir, "downloads/${movieId}.mp4")
            if (file.exists()) file.delete()
            downloadDao.deleteDownload(movieId)
        }
    }

    suspend fun deleteDownload(movieId: String) = withContext(Dispatchers.IO) {
        activeJobs[movieId]?.cancel()
        activeJobs.remove(movieId)
        val file = File(context.filesDir, "downloads/${movieId}.mp4")
        if (file.exists()) file.delete()
        downloadDao.deleteDownload(movieId)
    }

    fun getUsedStorageBytes(): Long {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}
