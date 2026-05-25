package com.socialhub.downloader.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val filePath: String
)

@HiltViewModel
class DownloadViewModel @Inject constructor() : ViewModel() {

    private val _activeDownloads = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val activeDownloads: StateFlow<List<ActiveDownload>> = _activeDownloads.asStateFlow()

    private val _completedDownloads = MutableStateFlow<List<CompletedDownload>>(emptyList())
    val completedDownloads: StateFlow<List<CompletedDownload>> = _completedDownloads.asStateFlow()

    private var simulationJob: Job? = null

    init {
        // Initial Mock Items
        _activeDownloads.value = listOf(
            ActiveDownload(
                id = "d1",
                title = "Amazing landscape travel compilation",
                platform = SocialPlatform.INSTAGRAM,
                sizeLabel = "42.1 MB",
                speedLabel = "5.8 MB/s",
                progress = 0.35f,
                remainingTime = "6s",
                status = DownloadStatus.DOWNLOADING
            ),
            ActiveDownload(
                id = "d2",
                title = "Learn Kotlin Coroutines in 10 minutes",
                platform = SocialPlatform.YOUTUBE,
                sizeLabel = "104 MB",
                speedLabel = "0.0 MB/s",
                progress = 0.72f,
                remainingTime = "Paused",
                status = DownloadStatus.PAUSED
            )
        )

        _completedDownloads.value = listOf(
            CompletedDownload("c1", "Best cooking tips and tricks compilation", SocialPlatform.TIKTOK, "12.8 MB", "1:15", "/storage/emulated/0/Download/SocialHub/tiktok_cook.mp4"),
            CompletedDownload("c2", "Space X Launch stream clip", SocialPlatform.X, "35.2 MB", "2:40", "/storage/emulated/0/Download/SocialHub/spacex.mp4")
        )

        startSimulation()
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentActive = _activeDownloads.value.toMutableList()
                val currentCompleted = _completedDownloads.value.toMutableList()
                var listChanged = false

                val updatedList = currentActive.mapIndexed { index, download ->
                    if (download.status == DownloadStatus.DOWNLOADING) {
                        listChanged = true
                        val newProgress = (download.progress + 0.08f).coerceAtMost(1f)
                        
                        if (newProgress >= 1f) {
                            // Move to completed
                            currentCompleted.add(
                                0,
                                CompletedDownload(
                                    id = "c_${System.currentTimeMillis()}",
                                    title = download.title,
                                    platform = download.platform,
                                    sizeLabel = download.sizeLabel,
                                    duration = "1:40",
                                    filePath = "/storage/emulated/0/Download/SocialHub/${download.title.take(8).lowercase()}.mp4"
                                )
                            )
                            null
                        } else {
                            // Randomize speeds
                            val speed = (4..8).random() + ((0..9).random() / 10f)
                            val remainingSec = ((1f - newProgress) * 40).toInt().coerceAtLeast(1)
                            download.copy(
                                progress = newProgress,
                                speedLabel = "${speed} MB/s",
                                remainingTime = "${remainingSec}s"
                            )
                        }
                    } else {
                        download
                    }
                }.filterNotNull()

                if (listChanged) {
                    _activeDownloads.value = updatedList
                    _completedDownloads.value = currentCompleted
                }
            }
        }
    }

    fun pauseDownload(id: String) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) {
                it.copy(status = DownloadStatus.PAUSED, speedLabel = "0.0 MB/s", remainingTime = "Paused")
            } else {
                it
            }
        }
    }

    fun resumeDownload(id: String) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) {
                it.copy(status = DownloadStatus.DOWNLOADING, speedLabel = "Connecting...")
            } else {
                it
            }
        }
    }

    fun cancelDownload(id: String) {
        _activeDownloads.value = _activeDownloads.value.filter { it.id != id }
    }

    fun deleteCompleted(id: String) {
        _completedDownloads.value = _completedDownloads.value.filter { it.id != id }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
