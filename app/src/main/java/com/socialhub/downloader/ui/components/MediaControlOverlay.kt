package com.socialhub.downloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GestureType {
    VOLUME,
    BRIGHTNESS,
    NONE
}

@Composable
fun MediaControlOverlay(
    gestureType: GestureType,
    value: Float, // 0.0 to 1.0
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && gestureType != GestureType.NONE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val icon: ImageVector = when (gestureType) {
            GestureType.VOLUME -> Icons.Default.VolumeUp
            GestureType.BRIGHTNESS -> Icons.Default.Brightness5
            else -> Icons.Default.VolumeUp
        }

        val label = when (gestureType) {
            GestureType.VOLUME -> "Volume"
            GestureType.BRIGHTNESS -> "Brightness"
            else -> ""
        }

        GlassCard(
            modifier = Modifier
                .width(72.dp)
                .height(180.dp),
            cornerRadius = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White
                )
                
                // Vertical slider representation
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(value)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White)
                    )
                }

                Text(
                    text = "${(value * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}
