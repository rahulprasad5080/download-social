package com.socialhub.downloader.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.socialhub.downloader.ui.theme.CyberCyan
import com.socialhub.downloader.ui.theme.ElectricPurple

enum class DownloadButtonState {
    IDLE,
    PREPARING,
    DOWNLOADING,
    COMPLETED
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedDownloadButton(
    buttonState: DownloadButtonState,
    progress: Float, // 0.0 to 1.0
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transitionDuration = 400
    
    // Width animation based on state (collapses on completing/downloading if needed, or stays full width)
    val buttonHeight by animateDpAsState(
        targetValue = 56.dp,
        animationSpec = tween(transitionDuration),
        label = "ButtonHeight"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (buttonState) {
            DownloadButtonState.IDLE -> ElectricPurple
            DownloadButtonState.PREPARING -> Color(0xFF6B21A8)
            DownloadButtonState.DOWNLOADING -> CyberCyan
            DownloadButtonState.COMPLETED -> Color(0xFF10B981)
        },
        animationSpec = tween(transitionDuration),
        label = "ButtonColor"
    )

    Box(
        modifier = modifier
            .height(buttonHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = when (buttonState) {
                        DownloadButtonState.IDLE -> listOf(ElectricPurple, Color(0xFF9061F9))
                        DownloadButtonState.PREPARING -> listOf(Color(0xFF6B21A8), Color(0xFF7E22CE))
                        DownloadButtonState.DOWNLOADING -> listOf(CyberCyan, Color(0xFF0891B2))
                        DownloadButtonState.COMPLETED -> listOf(Color(0xFF10B981), Color(0xFF059669))
                    }
                )
            )
            .clickable(enabled = buttonState == DownloadButtonState.IDLE) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = buttonState,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) with fadeOut(animationSpec = tween(200))
            },
            label = "ButtonTextContent"
        ) { targetState ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (targetState) {
                    DownloadButtonState.IDLE -> {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Icon",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download in High Quality",
                            color = Color.White,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    DownloadButtonState.PREPARING -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Resolving link...",
                            color = Color.White,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    DownloadButtonState.DOWNLOADING -> {
                        CircularProgressIndicator(
                            progress = progress,
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Downloading... ${(progress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    DownloadButtonState.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success check",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Added to Downloads",
                            color = Color.White,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
