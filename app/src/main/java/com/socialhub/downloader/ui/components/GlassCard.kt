package com.socialhub.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.socialhub.downloader.ui.theme.GlassBorderDark
import com.socialhub.downloader.ui.theme.GlassBorderLight
import com.socialhub.downloader.ui.theme.GlassOverlayDark
import com.socialhub.downloader.ui.theme.GlassOverlayLight

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    val backgroundBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                GlassOverlayDark.copy(alpha = 0.4f),
                GlassOverlayDark.copy(alpha = 0.15f)
            )
        } else {
            listOf(
                GlassOverlayLight.copy(alpha = 0.5f),
                GlassOverlayLight.copy(alpha = 0.2f)
            )
        }
    )

    val borderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                GlassBorderDark.copy(alpha = 0.25f),
                GlassBorderDark.copy(alpha = 0.05f)
            )
        } else {
            listOf(
                GlassBorderLight.copy(alpha = 0.20f),
                GlassBorderLight.copy(alpha = 0.05f)
            )
        }
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush)
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(1.dp)
    ) {
        content()
    }
}
