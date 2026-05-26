package com.socialhub.downloader.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.data.DownloadRepository
import com.socialhub.downloader.ui.components.SocialPlatform
import com.socialhub.downloader.ui.screens.download.ActiveDownload
import com.socialhub.downloader.ui.screens.download.CompletedDownload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

data class TrendingDownload(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val duration: String,
    val size: String,
    val progressLabel: String
)

data class RecentDownload(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val size: String,
    val date: String,
    val filePath: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    downloadRepository: DownloadRepository
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _detectedPlatform = MutableStateFlow<SocialPlatform?>(null)
    val detectedPlatform: StateFlow<SocialPlatform?> = _detectedPlatform.asStateFlow()

    val isLoading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    val trendingList: StateFlow<List<TrendingDownload>> = downloadRepository.activeDownloads
        .map { downloads -> downloads.map { it.toTrendingDownload() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentList: StateFlow<List<RecentDownload>> = downloadRepository.completedDownloads
        .map { downloads -> downloads.map { it.toRecentDownload() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
        detectPlatform(newUrl)
    }

    private fun detectPlatform(url: String) {
        val cleanUrl = url.lowercase().trim()
        _detectedPlatform.value = when {
            cleanUrl.contains("instagram.com") || cleanUrl.contains("instagr.am") -> SocialPlatform.INSTAGRAM
            cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") -> SocialPlatform.YOUTUBE
            cleanUrl.contains("facebook.com") || cleanUrl.contains("fb.watch") -> SocialPlatform.FACEBOOK
            cleanUrl.contains("tiktok.com") -> SocialPlatform.TIKTOK
            cleanUrl.contains("twitter.com") || cleanUrl.contains("x.com") -> SocialPlatform.X
            cleanUrl.contains("pinterest.com") -> SocialPlatform.PINTEREST
            cleanUrl.contains("threads.net") -> SocialPlatform.THREADS
            else -> null
        }
    }

    fun pasteFromClipboard(text: String) {
        onUrlChange(text)
    }

    fun clearInput() {
        _urlInput.value = ""
        _detectedPlatform.value = null
    }

    private fun ActiveDownload.toTrendingDownload() = TrendingDownload(
        id = id,
        title = title,
        platform = platform,
        duration = remainingTime,
        size = sizeLabel,
        progressLabel = "${(progress * 100).toInt()}%"
    )

    private fun CompletedDownload.toRecentDownload() = RecentDownload(
        id = id,
        title = title,
        platform = platform,
        size = sizeLabel,
        date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(createdAtMillis)),
        filePath = filePath
    )
}
