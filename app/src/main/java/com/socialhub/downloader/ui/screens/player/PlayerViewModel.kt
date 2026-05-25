package com.socialhub.downloader.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistItem(
    val title: String,
    val source: String,
    val durationLabel: String
)

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(160000L) // 2m 40s in ms
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _brightness = MutableStateFlow(0.6f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isMiniPlayer = MutableStateFlow(false)
    val isMiniPlayer: StateFlow<Boolean> = _isMiniPlayer.asStateFlow()

    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist: StateFlow<List<PlaylistItem>> = _playlist.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private var progressJob: Job? = null

    init {
        _playlist.value = listOf(
            PlaylistItem("Space X Launch stream clip", "/storage/emulated/0/Download/SocialHub/spacex.mp4", "2:40"),
            PlaylistItem("TikTok trending compilation", "/storage/emulated/0/Download/SocialHub/tiktok_cook.mp4", "1:15"),
            CompletedDownloadDetails("Kotlin Coroutine Deep Dive", "10:24")
        )
        startProgressTracker()
    }

    private fun CompletedDownloadDetails(title: String, duration: String) =
        PlaylistItem(title, "/storage/emulated/0/Download/SocialHub/kotlin.mp4", duration)

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isPlaying.value) {
                    val nextPos = _currentPosition.value + (1000 * _playbackSpeed.value).toLong()
                    if (nextPos >= _totalDuration.value) {
                        _currentPosition.value = 0L
                        playNextTrack()
                    } else {
                        _currentPosition.value = nextPos
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position.coerceIn(0L, _totalDuration.value)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun updateVolume(delta: Float) {
        _volume.value = (_volume.value + delta).coerceIn(0f, 1f)
    }

    fun updateBrightness(delta: Float) {
        _brightness.value = (_brightness.value + delta).coerceIn(0f, 1f)
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun toggleMiniPlayer() {
        _isMiniPlayer.value = !_isMiniPlayer.value
    }

    fun playNextTrack() {
        val nextIndex = (_currentTrackIndex.value + 1) % _playlist.value.size
        _currentTrackIndex.value = nextIndex
        _currentPosition.value = 0L
        // update duration mock
        _totalDuration.value = if (nextIndex == 1) 75000L else if (nextIndex == 2) 624000L else 160000L
    }

    fun playPreviousTrack() {
        var prevIndex = _currentTrackIndex.value - 1
        if (prevIndex < 0) prevIndex = _playlist.value.size - 1
        _currentTrackIndex.value = prevIndex
        _currentPosition.value = 0L
        _totalDuration.value = if (prevIndex == 1) 75000L else if (prevIndex == 2) 624000L else 160000L
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
