package com.socialhub.downloader.ui.screens.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.ui.components.DownloadButtonState
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SelectedQuality(val label: String, val size: String, val extension: String) {
    MP4_1080P("MP4 1080p (FHD)", "84.5 MB", ".mp4"),
    MP4_720P("MP4 720p (HD)", "42.1 MB", ".mp4"),
    MP4_360P("MP4 360p (SD)", "18.3 MB", ".mp4"),
    MP3_AUDIO("Audio MP3 (320kbps)", "6.8 MB", ".mp3")
}

data class VideoDetails(
    val title: String,
    val platform: SocialPlatform,
    val duration: String,
    val creatorName: String,
    val creatorAvatarUrl: String,
    val views: String,
    val likes: String
)

@HiltViewModel
class VideoPreviewViewModel @Inject constructor() : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _videoDetails = MutableStateFlow<VideoDetails?>(null)
    val videoDetails: StateFlow<VideoDetails?> = _videoDetails.asStateFlow()

    private val _selectedQuality = MutableStateFlow(SelectedQuality.MP4_1080P)
    val selectedQuality: StateFlow<SelectedQuality> = _selectedQuality.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadButtonState.IDLE)
    val downloadState: StateFlow<DownloadButtonState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    fun loadVideoDetails(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1000) // simulated parse time
            
            val cleanUrl = url.lowercase()
            val platform = when {
                cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") -> SocialPlatform.YOUTUBE
                cleanUrl.contains("tiktok.com") -> SocialPlatform.TIKTOK
                cleanUrl.contains("facebook.com") -> SocialPlatform.FACEBOOK
                cleanUrl.contains("twitter.com") || cleanUrl.contains("x.com") -> SocialPlatform.X
                cleanUrl.contains("pinterest.com") -> SocialPlatform.PINTEREST
                cleanUrl.contains("threads.net") -> SocialPlatform.THREADS
                else -> SocialPlatform.INSTAGRAM
            }

            _videoDetails.value = VideoDetails(
                title = when (platform) {
                    SocialPlatform.YOUTUBE -> "How to build UI with Jetpack Compose (Kotlin Tutorial)"
                    SocialPlatform.INSTAGRAM -> "Beautiful morning views in Norway Fjords 🌲"
                    SocialPlatform.TIKTOK -> "Ultimate dance compilation trend 2026"
                    else -> "Interesting social media post highlight"
                },
                platform = platform,
                duration = when (platform) {
                    SocialPlatform.YOUTUBE -> "12:45"
                    else -> "0:30"
                },
                creatorName = when (platform) {
                    SocialPlatform.YOUTUBE -> "ComposeAcademy"
                    SocialPlatform.INSTAGRAM -> "norway_traveler"
                    else -> "viral_trends"
                },
                creatorAvatarUrl = "",
                views = "245K views",
                likes = "18K likes"
            )
            _isLoading.value = false
        }
    }

    fun selectQuality(quality: SelectedQuality) {
        _selectedQuality.value = quality
    }

    fun startDownload(onDownloadCompleted: (String, SelectedQuality) -> Unit) {
        viewModelScope.launch {
            val details = _videoDetails.value ?: return@launch
            
            // State: Idle -> Preparing
            _downloadState.value = DownloadButtonState.PREPARING
            delay(1500) // simulating resolution checking
            
            // State: Preparing -> Downloading
            _downloadState.value = DownloadButtonState.DOWNLOADING
            var currentProgress = 0f
            while (currentProgress < 1.0f) {
                delay(150)
                currentProgress += 0.05f
                _downloadProgress.value = currentProgress.coerceAtMost(1f)
            }
            
            // State: Downloading -> Completed
            _downloadState.value = DownloadButtonState.COMPLETED
            delay(1000)
            
            onDownloadCompleted(details.title, _selectedQuality.value)
            
            // Reset state
            _downloadState.value = DownloadButtonState.IDLE
            _downloadProgress.value = 0f
        }
    }
}
