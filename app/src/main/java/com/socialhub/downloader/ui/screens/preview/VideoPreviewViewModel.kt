package com.socialhub.downloader.ui.screens.preview

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialhub.downloader.data.remote.DownloadOption
import com.socialhub.downloader.data.remote.ResolvedMedia
import com.socialhub.downloader.data.remote.SocialMediaRepository
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

data class VideoDetails(
    val sourceUrl: String,
    val title: String,
    val platform: SocialPlatform,
    val duration: String,
    val creatorName: String,
    val creatorAvatarUrl: String,
    val thumbnailUrl: String,
    val views: String,
    val likes: String,
    val downloadOptions: List<DownloadOption>
)

@HiltViewModel
class VideoPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val socialMediaRepository: SocialMediaRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _videoDetails = MutableStateFlow<VideoDetails?>(null)
    val videoDetails: StateFlow<VideoDetails?> = _videoDetails.asStateFlow()

    private val _selectedOption = MutableStateFlow<DownloadOption?>(null)
    val selectedOption: StateFlow<DownloadOption?> = _selectedOption.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadButtonState.IDLE)
    val downloadState: StateFlow<DownloadButtonState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadMessage = MutableStateFlow<String?>(null)
    val downloadMessage: StateFlow<String?> = _downloadMessage.asStateFlow()

    fun loadVideoDetails(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _downloadMessage.value = null

            val platform = detectPlatform(url)
            val resolved = socialMediaRepository.resolve(url)
            val details = resolved.fold(
                onSuccess = { media -> media.toVideoDetails(url, platform) },
                onFailure = { error ->
                    _downloadMessage.value = error.message ?: "Unable to resolve this link"
                    createFallbackDetails(url, platform)
                }
            )

            _videoDetails.value = details
            _selectedOption.value = details.downloadOptions.firstOrNull()
            _isLoading.value = false
        }
    }

    private fun ResolvedMedia.toVideoDetails(inputUrl: String, fallbackPlatform: SocialPlatform): VideoDetails {
        val platform = detectPlatform(source.ifBlank { pageUrl.ifBlank { inputUrl } })
        return VideoDetails(
            sourceUrl = pageUrl.ifBlank { inputUrl },
            title = title,
            platform = platform,
            duration = duration,
            creatorName = source.ifBlank { platform.displayName }.sanitizeCreatorName(),
            creatorAvatarUrl = "",
            thumbnailUrl = thumbnailUrl,
            views = "${options.size} media option${if (options.size == 1) "" else "s"}",
            likes = if (source.isBlank()) fallbackPlatform.displayName else source,
            downloadOptions = options
        )
    }

    private suspend fun createFallbackDetails(url: String, platform: SocialPlatform): VideoDetails {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        val host = uri?.host.orEmpty()
        val title = inferTitle(url, platform, uri)
        val thumbnailUrl = if (platform == SocialPlatform.YOUTUBE) {
            extractYoutubeVideoId(uri)?.let { videoId ->
                "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }.orEmpty()
        } else {
            ""
        }
        val directOption = if (isDirectDownloadUrl(url)) listOf(createDirectDownloadOption(url)) else emptyList()

        return VideoDetails(
            sourceUrl = url,
            title = title,
            platform = platform,
            duration = if (platform == SocialPlatform.YOUTUBE) "12:45" else "--:--",
            creatorName = host.replace("www.", "").substringBefore(".").sanitizeCreatorName(),
            creatorAvatarUrl = "",
            thumbnailUrl = thumbnailUrl,
            views = if (directOption.isEmpty()) "Needs API resolver" else "Direct media link",
            likes = platform.displayName,
            downloadOptions = directOption
        )
    }

    private suspend fun inferTitle(url: String, platform: SocialPlatform, uri: Uri?): String {
        if (platform == SocialPlatform.YOUTUBE) {
            fetchYoutubeTitle(url).takeIf { it.isNotBlank() }?.let { return it }
        }

        val segments = uri?.pathSegments.orEmpty()
        val rawTitle = when {
            segments.size >= 2 -> "${segments[segments.size - 2]} ${segments.last().take(8)}"
            segments.isNotEmpty() -> segments.last()
            else -> "${platform.displayName} media"
        }

        return rawTitle
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .ifBlank { "${platform.displayName} media" }
    }

    private fun createDirectDownloadOption(url: String): DownloadOption {
        val extension = url.substringBefore("?")
            .substringAfterLast(".", "mp4")
            .lowercase()
            .takeIf { it.length in 2..5 }
            ?: "mp4"
        val hasVideo = extension !in audioExtensions

        return DownloadOption(
            label = if (hasVideo) "Direct Video (${extension.uppercase()})" else "Direct Audio (${extension.uppercase()})",
            sizeLabel = "Unknown size",
            extension = ".$extension",
            downloadUrl = url,
            hasVideo = hasVideo,
            hasAudio = true
        )
    }

    private fun detectPlatform(url: String): SocialPlatform {
        val cleanUrl = url.lowercase()
        return when {
            cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") -> SocialPlatform.YOUTUBE
            cleanUrl.contains("tiktok.com") -> SocialPlatform.TIKTOK
            cleanUrl.contains("facebook.com") || cleanUrl.contains("fb.watch") -> SocialPlatform.FACEBOOK
            cleanUrl.contains("twitter.com") || cleanUrl.contains("x.com") -> SocialPlatform.X
            cleanUrl.contains("pinterest.com") -> SocialPlatform.PINTEREST
            cleanUrl.contains("threads.net") -> SocialPlatform.THREADS
            else -> SocialPlatform.INSTAGRAM
        }
    }

    private fun extractYoutubeVideoId(uri: Uri?): String? {
        if (uri == null) return null
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

    fun selectOption(option: DownloadOption) {
        _selectedOption.value = option
    }

    fun clearDownloadMessage() {
        _downloadMessage.value = null
    }

    fun startDownload(onDownloadCompleted: (String, DownloadOption) -> Unit) {
        viewModelScope.launch {
            val details = _videoDetails.value ?: return@launch
            val option = _selectedOption.value
            _downloadMessage.value = null

            if (option == null) {
                _downloadState.value = DownloadButtonState.ERROR
                _downloadMessage.value = "No downloadable media found. Check API configuration."
                delay(1600)
                _downloadState.value = DownloadButtonState.IDLE
                return@launch
            }

            ContextCompat.startForegroundService(
                context,
                DownloadService.createIntent(
                    context = context,
                    url = option.downloadUrl,
                    title = details.title,
                    platform = details.platform,
                    sizeLabel = option.sizeLabel,
                    duration = details.duration,
                    extension = option.extension
                )
            )
            _downloadState.value = DownloadButtonState.COMPLETED
            _downloadMessage.value = "Download started"
            delay(1000)
            _downloadState.value = DownloadButtonState.IDLE
            onDownloadCompleted(details.title, option)
        }
    }

    private suspend fun isDirectDownloadUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        val lowerUrl = url.substringBefore("?").lowercase()
        if (mediaExtensions.any { lowerUrl.endsWith(".$it") }) return@withContext true

        runCatching {
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

    private fun String.sanitizeCreatorName(): String {
        return trim()
            .replace(" ", "_")
            .replace(Regex("[^A-Za-z0-9._-]"), "")
            .ifBlank { "social_hub" }
    }

    private companion object {
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg")
        val mediaExtensions = audioExtensions + setOf("mp4", "webm", "mov", "mkv")
    }
}
