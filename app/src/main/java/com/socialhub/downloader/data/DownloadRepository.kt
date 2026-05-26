package com.socialhub.downloader.data

import com.socialhub.downloader.ui.components.SocialPlatform
import com.socialhub.downloader.ui.screens.download.ActiveDownload
import com.socialhub.downloader.ui.screens.download.CompletedDownload
import com.socialhub.downloader.ui.screens.download.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor() {
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
}
