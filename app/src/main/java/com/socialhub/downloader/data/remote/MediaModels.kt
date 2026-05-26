package com.socialhub.downloader.data.remote

data class VideoRequest(
    val url: String,
    val token: String
)

data class VideoResponse(
    val url: String? = null,
    val title: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null,
    val source: String? = null,
    val medias: List<Media>? = null,
    val sid: String? = null
)

data class Media(
    val url: String? = null,
    val quality: String? = null,
    val extension: String? = null,
    val size: Int? = null,
    val formattedSize: String? = null,
    val videoAvailable: Boolean? = null,
    val audioAvailable: Boolean? = null,
    val chunked: Boolean? = null,
    val cached: Boolean? = null,
    val requiresRendering: Boolean? = null
)

data class DownloadOption(
    val label: String,
    val sizeLabel: String,
    val extension: String,
    val downloadUrl: String,
    val hasVideo: Boolean,
    val hasAudio: Boolean
)

data class ResolvedMedia(
    val pageUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: String,
    val source: String,
    val options: List<DownloadOption>
)
