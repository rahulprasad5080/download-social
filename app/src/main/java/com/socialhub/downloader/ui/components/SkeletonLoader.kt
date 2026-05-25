package com.socialhub.downloader.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200)
        ),
        label = "ShimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B), // slate-800
                Color(0xFF334155), // slate-700
                Color(0xFF1E293B)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun SkeletonItem(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .shimmerEffect()
    )
}

@Composable
fun TrendingSkeleton() {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .padding(end = 12.dp)
            ) {
                SkeletonItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    cornerRadius = 16
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonItem(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonItem(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                )
            }
        }
    }
}

@Composable
fun RecentDownloadsSkeleton() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonItem(
                    modifier = Modifier.size(56.dp),
                    cornerRadius = 12
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                SkeletonItem(
                    modifier = Modifier.size(24.dp),
                    cornerRadius = 6
                )
            }
        }
    }
}
