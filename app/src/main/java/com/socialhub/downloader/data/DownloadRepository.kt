package com.socialhub.downloader.data

import android.content.Context
import android.os.Environment
import com.socialhub.downloader.ui.components.SocialPlatform
import com.socialhub.downloader.ui.screens.download.ActiveDownload
import com.socialhub.downloader.ui.screens.download.CompletedDownload
import com.socialhub.downloader.ui.screens.download.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _activeDownloads = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val activeDownloads: StateFlow<List<ActiveDownload>> = _activeDownloads.asStateFlow()

    private val _completedDownloads = MutableStateFlow<List<CompletedDownload>>(emptyList())
    val completedDownloads: StateFlow<List<CompletedDownload>> = _completedDownloads.asStateFlow()

    fun startDownload(title: String, platform: SocialPlatform, sizeLabel: String): String {
        val id = "download_${System.currentTimeMillis()}"
        val download = ActiveDownload(
            id = id,
            title = title,
            platform = platform,
            sizeLabel = sizeLabel,
            speedLabel = "Preparing",
            progress = 0f,
            remainingTime = "Starting",
            status = DownloadStatus.DOWNLOADING
        )
        _activeDownloads.value = listOf(download) + _activeDownloads.value
        return id
    }

    fun updateProgress(id: String, progress: Float, speedLabel: String, remainingTime: String) {
        _activeDownloads.value = _activeDownloads.value.map { download ->
            if (download.id == id) {
                download.copy(
                    progress = progress.coerceIn(0f, 1f),
                    speedLabel = speedLabel,
                    remainingTime = remainingTime
                )
            } else {
                download
            }
        }
    }

    fun completeDownload(id: String, duration: String, filePath: String) {
        val completed = _activeDownloads.value.firstOrNull { it.id == id } ?: return
        _activeDownloads.value = _activeDownloads.value.filterNot { it.id == id }
        _completedDownloads.value = listOf(
            CompletedDownload(
                id = "completed_$id",
                title = completed.title,
                platform = completed.platform,
                sizeLabel = completed.sizeLabel,
                duration = duration,
                filePath = filePath,
                createdAtMillis = System.currentTimeMillis()
            )
        ) + _completedDownloads.value
    }

    fun pauseDownload(id: String) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speedLabel = "0.0 MB/s", remainingTime = "Paused") else it
        }
    }

    fun resumeDownload(id: String) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING, speedLabel = "Connecting") else it
        }
    }

    fun cancelDownload(id: String) {
        _activeDownloads.value = _activeDownloads.value.filterNot { it.id == id }
    }

    fun deleteCompleted(id: String) {
        _completedDownloads.value = _completedDownloads.value.filterNot { it.id == id }
    }

    suspend fun downloadDirectMedia(
        sourceUrl: String,
        title: String,
        platform: SocialPlatform,
        requestedSizeLabel: String,
        duration: String,
        extension: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val downloadId = startDownload(title, platform, requestedSizeLabel)
        runCatching {
            val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "SocialHubDownloader/1.0")
            }

            connection.connect()
            val contentType = connection.contentType.orEmpty()
            if (connection.responseCode !in 200..299) {
                error("Server returned ${connection.responseCode}")
            }
            if (contentType.contains("text/html", ignoreCase = true)) {
                error("This link is a web page, not a direct media file")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: -1L
            val outputFile = createOutputFile(title, extension)
            var downloadedBytes = 0L
            val startedAt = System.currentTimeMillis()

            // 4. Service stream ko file mein write karti hai:
            // connection.inputStream.use { input -> outputFile.outputStream().use { output -> input.copyTo(output) } }
            // Yahan manual loop use kiya hai taaki progress/speed Downloads tab mein update hoti rahe.
            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val elapsedSeconds = ((System.currentTimeMillis() - startedAt).coerceAtLeast(1L)) / 1000.0
                        val bytesPerSecond = (downloadedBytes / elapsedSeconds).toLong()
                        val progress = if (totalBytes > 0L) downloadedBytes.toFloat() / totalBytes else 0f
                        val remainingTime = if (totalBytes > 0L && bytesPerSecond > 0L) {
                            "${((totalBytes - downloadedBytes) / bytesPerSecond).coerceAtLeast(1L)}s"
                        } else {
                            "Calculating"
                        }

                        updateProgress(
                            id = downloadId,
                            progress = progress,
                            speedLabel = "${bytesPerSecond.formatBytes()}/s",
                            remainingTime = remainingTime
                        )
                    }
                }
            }

            completeDownload(
                id = downloadId,
                duration = duration,
                filePath = outputFile.absolutePath
            )
            outputFile.absolutePath
        }.onFailure {
            cancelDownload(downloadId)
        }
    }

    private fun createOutputFile(title: String, extension: String): File {
        val safeTitle = title
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim()
            .ifBlank { "download_${System.currentTimeMillis()}" }
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SocialHub")
        directory.mkdirs()
        return File(directory, "$safeTitle$extension")
    }

    private fun Long.formatBytes(): String {
        val kb = this / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
    }
}
