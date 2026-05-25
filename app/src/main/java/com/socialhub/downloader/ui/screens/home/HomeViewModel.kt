package com.socialhub.downloader.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendingDownload(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val duration: String,
    val size: String,
    val views: String,
    val thumbnailUrl: String
)

data class RecentDownload(
    val id: String,
    val title: String,
    val platform: SocialPlatform,
    val size: String,
    val date: String
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _detectedPlatform = MutableStateFlow<SocialPlatform?>(null)
    val detectedPlatform: StateFlow<SocialPlatform?> = _detectedPlatform.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _trendingList = MutableStateFlow<List<TrendingDownload>>(emptyList())
    val trendingList: StateFlow<List<TrendingDownload>> = _trendingList.asStateFlow()

    private val _recentList = MutableStateFlow<List<RecentDownload>>(emptyList())
    val recentList: StateFlow<List<RecentDownload>> = _recentList.asStateFlow()

    init {
        loadHomeData()
    }

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

    private fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1200) // Simulating standard network delay to showcase skeleton loaders
            
            _trendingList.value = listOf(
                TrendingDownload(
                    "1", "Awesome Kotlin Tricks you need to know!", SocialPlatform.YOUTUBE,
                    "10:24", "45.2 MB", "2.1M views", ""
                ),
                TrendingDownload(
                    "2", "Cinematic drone view of Swiss Alps 4K", SocialPlatform.INSTAGRAM,
                    "0:45", "12.8 MB", "850K views", ""
                ),
                TrendingDownload(
                    "3", "Funny cats compilation 2026", SocialPlatform.TIKTOK,
                    "1:15", "18.5 MB", "5.4M views", ""
                )
            )

            _recentList.value = listOf(
                RecentDownload("r1", "Tech Keynote Review", SocialPlatform.YOUTUBE, "92.4 MB", "Today"),
                RecentDownload("r2", "Cooking Pasta Recipe Reel", SocialPlatform.INSTAGRAM, "8.7 MB", "Yesterday"),
                RecentDownload("r3", "Lo-Fi Beats 1 Hour", SocialPlatform.X, "182 MB", "3 days ago")
            )
            _isLoading.value = false
        }
    }
}
