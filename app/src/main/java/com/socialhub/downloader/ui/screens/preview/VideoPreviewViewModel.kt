package com.socialhub.downloader.ui.screens.preview

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.download.DownloadService
import com.socialhub.downloader.ui.components.DownloadButtonState
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

enum class SelectedQuality(val label: String, val size: String, val extension: String) {
    MP4_1080P("MP4 1080p (FHD)", "84.5 MB", ".mp4"),
    MP4_720P("MP4 720p (HD)", "42.1 MB", ".mp4"),
    MP4_360P("MP4 360p (SD)", "18.3 MB", ".mp4"),
    MP3_AUDIO("Audio MP3 (320kbps)", "6.8 MB", ".mp3")
}

data class VideoDetails(
    val sourceUrl: String,
    val title: String,
    val platform: SocialPlatform,
    val duration: String,
    val creatorName: String,
    val creatorAvatarUrl: String,
    val thumbnailUrl: String,
    val views: String,
    val likes: String
)

@HiltViewModel
class VideoPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    private val _downloadMessage = MutableStateFlow<String?>(null)
    val downloadMessage: StateFlow<String?> = _downloadMessage.asStateFlow()

    fun loadVideoDetails(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(300)
            
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

            _videoDetails.value = extractDetailsFromUrl(url, platform)
            _isLoading.value = false
        }
    }

    private suspend fun extractDetailsFromUrl(url: String, platform: SocialPlatform): VideoDetails {
        var title = ""
        var creator = ""
        var thumbnailUrl = ""
        
        try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host ?: ""
            
            if (platform == SocialPlatform.YOUTUBE) {
                val videoId = extractYoutubeVideoId(uri)
                if (!videoId.isNullOrBlank()) {
                    thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    title = fetchYoutubeTitle(url).ifBlank {
                        "YouTube video $videoId"
                    }
                } else {
                    val segments = uri.pathSegments
                    if (segments.isNotEmpty()) {
                        title = segments.last().replace("-", " ").replace("_", " ")
                    }
                }
                creator = "YouTube Creator"
            } else {
                val segments = uri.pathSegments
                if (segments.size >= 2) {
                    title = segments[segments.size - 2].replace("-", " ").replace("_", " ") + 
                            " (" + segments.last().take(6) + ")"
                } else if (segments.isNotEmpty()) {
                    title = segments.last().replace("-", " ").replace("_", " ")
                }
                
                creator = host.replace("www.", "").substringBefore(".")
            }
        } catch (e: Exception) {
            // fallback handled below
        }

        // Validate and apply default fallback if parsed name is too short/generic
        if (title.trim().length < 4 || title.all { it.isDigit() || !it.isLetter() }) {
            title = "${platform.displayName} media from ${creator.ifBlank { "shared link" }}"
        } else {
            // Clean double spaces and titlecase
            title = title.split(" ")
                .filter { it.isNotEmpty() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
        }

        if (creator.isEmpty() || creator.length < 3) {
            creator = "social_hub_creator"
        }

        return VideoDetails(
            sourceUrl = url,
            title = title,
            platform = platform,
            duration = when (platform) {
                SocialPlatform.YOUTUBE -> "12:45"
                else -> "0:45"
            },
            creatorName = creator,
            creatorAvatarUrl = "",
            thumbnailUrl = thumbnailUrl,
            views = "${url.length.coerceAtLeast(1)} chars",
            likes = platform.displayName
        )
    }

    private fun extractYoutubeVideoId(uri: android.net.Uri): String? {
        uri.getQueryParameter("v")?.takeIf { it.isNotBlank() }?.let { return it }

        val host = uri.host.orEmpty()
        val segments = uri.pathSegments
        return when {
            host.contains("youtu.be") && segments.isNotEmpty() -> segments.first()
            segments.size >= 2 && segments.first() in listOf("shorts", "embed", "live") -> segments[1]
            else -> null
        }
    }

    private suspend fun fetchYoutubeTitle(url: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val connection = URL("https://www.youtube.com/oembed?url=$encodedUrl&format=json").openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            JSONObject(json).optString("title")
        }.getOrDefault("")
    }

    fun selectQuality(quality: SelectedQuality) {
        _selectedQuality.value = quality
    }

    fun clearDownloadMessage() {
        _downloadMessage.value = null
    }

    fun startDownload(onDownloadCompleted: (String, SelectedQuality) -> Unit) {
        viewModelScope.launch {
            // 1. User ne jo URL paste kiya tha, woh details.sourceUrl mein available hai.
            val details = _videoDetails.value ?: return@launch
            _downloadMessage.value = null

            // 2. App pehle HEAD/content-type se check karta hai ki link direct video/audio file hai ya nahi.
            if (!isDirectDownloadUrl(details.sourceUrl)) {
                _downloadState.value = DownloadButtonState.ERROR
                _downloadMessage.value = "This link is a web page. Use a direct video/audio file link."
                delay(1600)
                _downloadState.value = DownloadButtonState.IDLE
                return@launch
            }

            // 3. Direct media URL milne par foreground DownloadService start hoti hai.
            ContextCompat.startForegroundService(
                context,
                DownloadService.createIntent(
                    context = context,
                    url = details.sourceUrl,
                    title = details.title,
                    platform = details.platform,
                    sizeLabel = _selectedQuality.value.size,
                    duration = details.duration,
                    extension = _selectedQuality.value.extension
                )
            )
            _downloadState.value = DownloadButtonState.COMPLETED
            _downloadMessage.value = "Download started"
            delay(1000)
            _downloadState.value = DownloadButtonState.IDLE
            onDownloadCompleted(details.title, _selectedQuality.value)
        }
    }

    private suspend fun isDirectDownloadUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        val lowerUrl = url.substringBefore('?').lowercase()
        if (lowerUrl.endsWith(".mp4") ||
            lowerUrl.endsWith(".mp3") ||
            lowerUrl.endsWith(".m4a") ||
            lowerUrl.endsWith(".webm") ||
            lowerUrl.endsWith(".mov")
        ) {
            return@withContext true
        }

        runCatching {
            // Same idea as: val connection = URL(inputUrl).openConnection() as HttpURLConnection
            // Then requestMethod = "HEAD" and contentType startsWith("video/") or "audio/".
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                instanceFollowRedirects = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "SocialHubDownloader/1.0")
            }
            connection.connect()
            val contentType = connection.contentType.orEmpty().lowercase()
            connection.disconnect()
            contentType.startsWith("video/") ||
                contentType.startsWith("audio/") ||
                contentType == "application/octet-stream"
        }.getOrDefault(false)
    }
}
