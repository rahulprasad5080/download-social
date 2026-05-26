package com.socialhub.downloader.ui.screens.download

import androidx.lifecycle.ViewModel
import com.socialhub.downloader.data.DownloadRepository
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class DownloadStatus {
    DOWNLOADING,
    PAUSED,
    CANCELED
}

data class ActiveDownload(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val sizeLabel: String,
    val speedLabel: String,
    val progress: Float,
    val remainingTime: String,
    val status: DownloadStatus
)

data class CompletedDownload(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val sizeLabel: String,
    val duration: String,
    val filePath: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository
) : ViewModel() {

    val activeDownloads: StateFlow<List<ActiveDownload>> = repository.activeDownloads

    val completedDownloads: StateFlow<List<CompletedDownload>> = repository.completedDownloads

    fun pauseDownload(id: String) {
        repository.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        repository.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        repository.cancelDownload(id)
    }

    fun deleteCompleted(id: String) {
        repository.deleteCompleted(id)
    }
}
