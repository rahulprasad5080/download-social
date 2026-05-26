package com.socialhub.downloader.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.socialhub.downloader.data.DownloadRepository
import com.socialhub.downloader.ui.components.SocialPlatform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {
    @Inject lateinit var downloadRepository: DownloadRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()
        val fallbackUrl = intent?.getStringExtra(EXTRA_FALLBACK_URL).orEmpty()
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Media download" }
        val platform = runCatching {
            SocialPlatform.valueOf(intent?.getStringExtra(EXTRA_PLATFORM).orEmpty())
        }.getOrDefault(SocialPlatform.INSTAGRAM)
        val sizeLabel = intent?.getStringExtra(EXTRA_SIZE_LABEL).orEmpty().ifBlank { "Unknown size" }
        val duration = intent?.getStringExtra(EXTRA_DURATION).orEmpty().ifBlank { "--:--" }
        val extension = intent?.getStringExtra(EXTRA_EXTENSION).orEmpty().ifBlank { ".mp4" }
        val requestHeaders = parseHeaders(intent?.getStringExtra(EXTRA_HEADERS).orEmpty())

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading media")
                .setContentText(title)
                .setOngoing(true)
                .build()
        )

        scope.launch {
            if (url.isNotBlank()) {
                downloadRepository.downloadDirectMedia(
                    sourceUrl = url,
                    fallbackUrl = fallbackUrl,
                    title = title,
                    platform = platform,
                    requestedSizeLabel = sizeLabel,
                    duration = duration,
                    extension = extension,
                    requestHeaders = requestHeaders
                )
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "download_service"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_URL = "url"
        private const val EXTRA_FALLBACK_URL = "fallbackUrl"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_PLATFORM = "platform"
        private const val EXTRA_SIZE_LABEL = "sizeLabel"
        private const val EXTRA_DURATION = "duration"
        private const val EXTRA_EXTENSION = "extension"
        private const val EXTRA_HEADERS = "headers"

        fun createIntent(
            context: Context,
            url: String,
            fallbackUrl: String?,
            title: String,
            platform: SocialPlatform,
            sizeLabel: String,
            duration: String,
            extension: String,
            requestHeaders: Map<String, String> = emptyMap()
        ): Intent = Intent(context, DownloadService::class.java).apply {
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_FALLBACK_URL, fallbackUrl.orEmpty())
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PLATFORM, platform.name)
            putExtra(EXTRA_SIZE_LABEL, sizeLabel)
            putExtra(EXTRA_DURATION, duration)
            putExtra(EXTRA_EXTENSION, extension)
            putExtra(EXTRA_HEADERS, JSONObject(requestHeaders).toString())
        }

        private fun parseHeaders(value: String): Map<String, String> {
            if (value.isBlank()) return emptyMap()

            return runCatching {
                val json = JSONObject(value)
                json.keys().asSequence().associateWith { key -> json.optString(key) }
                    .filterKeys { key -> key.isNotBlank() }
                    .filterValues { headerValue -> headerValue.isNotBlank() }
            }.getOrDefault(emptyMap())
        }
    }
}
