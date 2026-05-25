package com.socialhub.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.socialhub.downloader.ui.theme.PlatformColors

enum class SocialPlatform(
    val displayName: String,
    val primaryColor: Color,
    val icon: ImageVector,
    val gradientColors: List<Color>
) {
    INSTAGRAM(
        "Instagram",
        PlatformColors.Instagram,
        Icons.Default.CameraAlt,
        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCAF45))
    ),
    YOUTUBE(
        "YouTube",
        PlatformColors.YouTube,
        Icons.Default.PlayArrow,
        listOf(Color(0xFFFF0000), Color(0xFFB30000))
    ),
    FACEBOOK(
        "Facebook",
        PlatformColors.Facebook,
        Icons.Default.Public,
        listOf(Color(0xFF1877F2), Color(0xFF0F51A8))
    ),
    TIKTOK(
        "TikTok",
        PlatformColors.TikTok,
        Icons.Default.MusicNote,
        listOf(Color(0xFF000000), Color(0xFF25F4EE), Color(0xFFFE2C55))
    ),
    X(
        "X",
        PlatformColors.X,
        Icons.Default.Link,
        listOf(Color(0xFF14171A), Color(0xFF000000))
    ),
    PINTEREST(
        "Pinterest",
        PlatformColors.Pinterest,
        Icons.Default.CardGiftcard,
        listOf(Color(0xFFBD081C), Color(0xFF8B0715))
    ),
    THREADS(
        "Threads",
        PlatformColors.Threads,
        Icons.Default.ViewHeadline,
        listOf(Color(0xFF101010), Color(0xFF303030))
    )
}

@Composable
fun PlatformIcon(
    platform: SocialPlatform,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = platform.gradientColors
                )
            )
            .then(clickModifier)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = platform.icon,
            contentDescription = platform.displayName,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
