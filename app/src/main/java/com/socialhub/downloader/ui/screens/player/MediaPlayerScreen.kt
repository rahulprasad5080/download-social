package com.socialhub.downloader.ui.screens.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.socialhub.downloader.ui.components.GestureType
import com.socialhub.downloader.ui.components.GlassCard
import com.socialhub.downloader.ui.components.MediaControlOverlay
import com.socialhub.downloader.ui.theme.CyberCyan
import com.socialhub.downloader.ui.theme.ElectricPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
fun MediaPlayerScreen(
    mediaPath: String,
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.totalDuration.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val trackIndex by viewModel.currentTrackIndex.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var gestureType by remember { mutableStateOf(GestureType.NONE) }
    var gestureValue by remember { mutableStateOf(0f) }
    var showGestureOverlay by remember { mutableStateOf(false) }
    var showPlaylistDrawer by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    var screenWidth by remember { mutableStateOf(0) }
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(mediaPath) {
        viewModel.setNowPlayingFromPath(mediaPath)
    }

    LaunchedEffect(trackIndex, playlist) {
        val source = playlist.getOrNull(trackIndex)?.source.orEmpty()
        if (source.isNotBlank()) {
            val uri = source.toMediaUri()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        }
    }

    LaunchedEffect(player, isPlaying, trackIndex) {
        while (true) {
            val playerDuration = player.duration
            if (playerDuration != C.TIME_UNSET) {
                viewModel.updateDuration(playerDuration)
            }
            viewModel.updatePlaybackPosition(player.currentPosition)
            delay(500)
        }
    }

    LaunchedEffect(isPlaying) {
        player.playWhenReady = isPlaying
    }

    LaunchedEffect(speed) {
        player.setPlaybackSpeed(speed)
    }

    // Auto hide controls after 4s
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Helper: format milliseconds to 00:00
    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { screenWidth = it.size.width }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        showControls = false
                        val isRightSide = offset.x > (screenWidth / 2)
                        gestureType = if (isRightSide) GestureType.VOLUME else GestureType.BRIGHTNESS
                        gestureValue = if (isRightSide) volume else brightness
                        showGestureOverlay = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Drag vertical: up is negative dragAmount.y
                        val sensitivity = 0.003f
                        val delta = -dragAmount.y * sensitivity
                        gestureValue = (gestureValue + delta).coerceIn(0f, 1f)
                        
                        if (gestureType == GestureType.VOLUME) {
                            viewModel.updateVolume(delta)
                        } else {
                            viewModel.updateBrightness(delta)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(800)
                            showGestureOverlay = false
                            gestureType = GestureType.NONE
                        }
                    }
                )
            }
            .clickable { showControls = !showControls }
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        // Gesture Overlay Feedback HUD
        MediaControlOverlay(
            gestureType = gestureType,
            value = gestureValue,
            visible = showGestureOverlay,
            modifier = Modifier.align(Alignment.Center)
        )

        // Interactive Media Controls HUD
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Dark gradient overlay for controls legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                // Top controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    
                    Text(
                        text = playlist.getOrNull(trackIndex)?.title ?: "Video File",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )

                    Row {
                        IconButton(
                            onClick = { showPlaylistDrawer = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = "Playlist", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                        }
                    }
                }

                // Speed Selector Dropdown Menu
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 70.dp, end = 16.dp)
                ) {
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speedOption ->
                            DropdownMenuItem(
                                text = { Text("${speedOption}x") },
                                onClick = {
                                    viewModel.setPlaybackSpeed(speedOption)
                                    showSpeedMenu = false
                                    Toast.makeText(context, "Playback speed: ${speedOption}x", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Bottom control panel (Timeline slider, Play/Pause controller, Fullscreen toggle)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    // Timeline Scrubber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = formatTime(position), color = Color.White, fontSize = 11.sp)
                        Slider(
                            value = position.toFloat(),
                            onValueChange = {
                                val newPosition = it.toLong()
                                player.seekTo(newPosition)
                                viewModel.seekTo(newPosition)
                            },
                            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricPurple,
                                activeTrackColor = ElectricPurple,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Text(text = formatTime(duration), color = Color.White, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons controller row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleFullscreen() }) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Playback controller group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { viewModel.playPreviousTrack() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            IconButton(onClick = { viewModel.playNextTrack() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Options badge placeholder
                        IconButton(onClick = { Toast.makeText(context, "Settings Details Toggle", Toast.LENGTH_SHORT).show() }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sliding Playlist Drawer Panel
        AnimatedVisibility(
            visible = showPlaylistDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(280.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showPlaylistDrawer = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Drawer", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(playlist) { index, item ->
                            val isCurrent = index == trackIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) ElectricPurple.copy(alpha = 0.25f) else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectTrack(index)
                                        showPlaylistDrawer = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCurrent) CyberCyan else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.title,
                                        color = if (isCurrent) Color.White else Color.LightGray,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.durationLabel,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.toMediaUri(): Uri =
    when {
        startsWith("content://") || startsWith("http://") || startsWith("https://") -> Uri.parse(this)
        else -> Uri.fromFile(File(this))
    }
