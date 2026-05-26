package com.socialhub.downloader.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.socialhub.downloader.ui.components.SocialPlatform
import com.socialhub.downloader.ui.screens.download.ActiveDownload
import com.socialhub.downloader.ui.screens.download.CompletedDownload
import com.socialhub.downloader.ui.screens.download.DownloadStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
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

    private val gson = Gson()
    private val preferences = context.getSharedPreferences("downloads", Context.MODE_PRIVATE)

    private val _completedDownloads = MutableStateFlow(loadCompletedDownloads())
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
        persistCompletedDownloads()
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
        persistCompletedDownloads()
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
            val outputTarget = createOutputTarget(title, extension, contentType)
            var downloadedBytes = 0L
            val startedAt = System.currentTimeMillis()

            connection.inputStream.use { input ->
                outputTarget.outputStream.use { output ->
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
            finalizeOutputTarget(outputTarget, contentType)

            completeDownload(
                id = downloadId,
                duration = duration,
                filePath = outputTarget.displayPath
            )
            outputTarget.displayPath
        }.onFailure {
            cancelDownload(downloadId)
        }
    }

    private fun createOutputTarget(title: String, extension: String, contentType: String): OutputTarget {
        val safeTitle = title
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim()
            .ifBlank { "download_${System.currentTimeMillis()}" }
        val cleanExtension = extension.trim().ifBlank { ".mp4" }.let {
            if (it.startsWith(".")) it else ".$it"
        }
        val fileName = "$safeTitle$cleanExtension"
        val mimeType = resolveMimeType(cleanExtension, contentType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/SocialHub")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create media file")
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open media file")

            return OutputTarget(
                displayPath = uri.toString(),
                uri = uri,
                file = null,
                outputStream = outputStream
            )
        }

        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SocialHub")
        directory.mkdirs()
        val file = File(directory, fileName)
        return OutputTarget(
            displayPath = file.absolutePath,
            uri = Uri.fromFile(file),
            file = file,
            outputStream = file.outputStream()
        )
    }

    private fun finalizeOutputTarget(target: OutputTarget, contentType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && target.uri != null) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(target.uri, values, null, null)
        } else {
            target.file?.let { file ->
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(contentType.takeIf { it.contains("/") } ?: "video/mp4"),
                    null
                )
            }
        }
    }

    private fun resolveMimeType(extension: String, contentType: String): String {
        val cleanContentType = contentType.substringBefore(";").trim()
        if (cleanContentType.contains("/") &&
            !cleanContentType.equals("application/octet-stream", ignoreCase = true)
        ) {
            return cleanContentType
        }

        val extensionWithoutDot = extension.removePrefix(".").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extensionWithoutDot)
            ?: if (extensionWithoutDot in audioExtensions) "audio/$extensionWithoutDot" else "video/$extensionWithoutDot"
    }

    private fun Long.formatBytes(): String {
        val kb = this / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
    }

    private fun loadCompletedDownloads(): List<CompletedDownload> {
        val json = preferences.getString(COMPLETED_DOWNLOADS_KEY, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<CompletedDownload>>() {}.type
            gson.fromJson<List<CompletedDownload>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun persistCompletedDownloads() {
        preferences.edit()
            .putString(COMPLETED_DOWNLOADS_KEY, gson.toJson(_completedDownloads.value))
            .apply()
    }

    private data class OutputTarget(
        val displayPath: String,
        val uri: Uri?,
        val file: File?,
        val outputStream: OutputStream
    )

    private companion object {
        const val COMPLETED_DOWNLOADS_KEY = "completed_downloads"
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus")
    }
}
