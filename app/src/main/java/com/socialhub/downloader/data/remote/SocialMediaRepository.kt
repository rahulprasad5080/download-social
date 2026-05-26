package com.socialhub.downloader.data.remote

import com.socialhub.downloader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialMediaRepository @Inject constructor(
    private val apiService: SocialMediaApiService
) {
    suspend fun resolve(url: String): Result<ResolvedMedia> = withContext(Dispatchers.IO) {
        runCatching {
            if (BuildConfig.SOCIAL_DOWNLOADER_BASE_URL == "https://example.com/" ||
                BuildConfig.SOCIAL_DOWNLOADER_TOKEN.isBlank()
            ) {
                error("API config missing. Add socialDownloaderBaseUrl, socialDownloaderEndpoint and socialDownloaderToken in gradle.properties.")
            }

            val response = try {
                apiService.resolveMedia(
                    VideoRequest(
                        url = url,
                        token = BuildConfig.SOCIAL_DOWNLOADER_TOKEN
                    )
                )
            } catch (e: java.net.SocketTimeoutException) {
                error("Server is sleeping (Render free tier). Waking it up now, please try again in a few seconds...")
            } catch (e: java.net.ConnectException) {
                error("Server is sleeping (Render free tier). Waking it up now, please try again in a few seconds...")
            } catch (e: java.io.IOException) {
                val msg = e.message.orEmpty().lowercase()
                if (msg.contains("timeout") || msg.contains("connect") || msg.contains("time out")) {
                    error("Server is sleeping (Render free tier). Waking it up now, please try again in a few seconds...")
                } else {
                    throw e
                }
            }

            if (!response.isSuccessful) {
                error(readApiError(response.errorBody()?.string(), response.code(), response.message()))
            }

            val body = response.body() ?: error("API response is empty")
            val options = body.medias.orEmpty()
                .filter { media -> !media.url.isNullOrBlank() }
                .mapIndexed { index, media ->
                    media.toDownloadOption(index)
                }

            if (options.isEmpty()) {
                error("No downloadable media found for this link")
            }

            ResolvedMedia(
                pageUrl = body.url ?: url,
                title = body.title?.takeIf { it.isNotBlank() } ?: "Social media download",
                thumbnailUrl = body.thumbnail.orEmpty(),
                duration = body.duration?.takeIf { it.isNotBlank() } ?: "--:--",
                source = body.source.orEmpty(),
                options = options
            )
        }
    }

    private fun Media.toDownloadOption(index: Int): DownloadOption {
        val cleanExtension = extension
            ?.trim()
            ?.removePrefix(".")
            ?.takeIf { it.isNotBlank() }
            ?: "mp4"
        val qualityLabel = quality?.takeIf { it.isNotBlank() } ?: "Media ${index + 1}"
        val hasVideo = videoAvailable ?: cleanExtension !in audioExtensions
        val hasAudio = audioAvailable ?: true
        val typeLabel = if (hasVideo) "Video" else "Audio"

        return DownloadOption(
            label = "$typeLabel $qualityLabel (${cleanExtension.uppercase()})",
            sizeLabel = formattedSize
                ?: size?.formatBytes()
                ?: "Unknown size",
            extension = ".$cleanExtension",
            downloadUrl = proxyUrl?.takeIf { it.isNotBlank() } ?: requireNotNull(url),
            originalUrl = url,
            requestHeaders = headers.orEmpty()
                .filterKeys { key -> key.isNotBlank() }
                .filterValues { value -> value.isNotBlank() },
            hasVideo = hasVideo,
            hasAudio = hasAudio
        )
    }

    private fun readApiError(errorBody: String?, code: Int, fallbackMessage: String): String {
        val backendMessage = runCatching {
            JSONObject(errorBody.orEmpty()).optString("error")
        }.getOrDefault("")

        return backendMessage
            .takeIf { it.isNotBlank() }
            ?: "API failed: $code $fallbackMessage"
    }

    private fun Long.formatBytes(): String {
        val kb = this / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
    }

    private companion object {
        val audioExtensions = setOf("mp3", "m4a", "aac", "wav", "ogg")
    }
}
