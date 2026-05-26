package com.socialhub.downloader.ui.screens.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
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

    private val _totalDuration = MutableStateFlow(0L)
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

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position.coerceIn(0L, _totalDuration.value)
    }

    fun updatePlaybackPosition(position: Long) {
        _currentPosition.value = position.coerceAtLeast(0L)
    }

    fun updateDuration(duration: Long) {
        if (duration > 0) {
            _totalDuration.value = duration
        }
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

    fun setNowPlayingFromPath(path: String) {
        if (path.isBlank()) return

        val currentPlaylist = _playlist.value.toMutableList()
        val existingIndex = currentPlaylist.indexOfFirst { it.source == path }
        val selectedIndex = if (existingIndex >= 0) {
            existingIndex
        } else {
            currentPlaylist.add(0, PlaylistItem(titleFromPath(path), path, "--:--"))
            0
        }

        _playlist.value = currentPlaylist
        selectTrack(selectedIndex)
    }

    fun selectTrack(index: Int) {
        if (index !in _playlist.value.indices) return

        _currentTrackIndex.value = index
        _currentPosition.value = 0L
    }

    fun playNextTrack() {
        if (_playlist.value.isEmpty()) return
        val nextIndex = (_currentTrackIndex.value + 1) % _playlist.value.size
        selectTrack(nextIndex)
    }

    fun playPreviousTrack() {
        if (_playlist.value.isEmpty()) return
        var prevIndex = _currentTrackIndex.value - 1
        if (prevIndex < 0) prevIndex = _playlist.value.size - 1
        selectTrack(prevIndex)
    }

    private fun titleFromPath(path: String): String {
        val filename = File(path).nameWithoutExtension.ifBlank { path.substringAfterLast('/') }
        return filename
            .replace('_', ' ')
            .replace('-', ' ')
            .ifBlank { "Media File" }
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

}
