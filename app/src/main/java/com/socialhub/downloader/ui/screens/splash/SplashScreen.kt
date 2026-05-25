package com.socialhub.downloader.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.socialhub.downloader.ui.navigation.Screen
import com.socialhub.downloader.ui.theme.CyberCyan
import com.socialhub.downloader.ui.theme.ElectricPurple
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1500
            )
        )
        delay(300)
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Dynamic Premium Splash Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw outer glowing circle
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(ElectricPurple, CyberCyan, ElectricPurple)
                        ),
                        radius = width / 2.2f,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw internal Arrow / Hub representation
                    val arrowPath = Path().apply {
                        // Arrow Shaft
                        moveTo(width / 2f, height / 3f)
                        lineTo(width / 2f, height / 1.6f)
                        // Arrow Head
                        moveTo(width / 2.7f, height / 1.9f)
                        lineTo(width / 2f, height / 1.5f)
                        lineTo(width / 1.58f, height / 1.9f)
                        
                        // Lower bar
                        moveTo(width / 3f, height / 1.3f)
                        lineTo(width / 1.5f, height / 1.3f)
                    }

                    drawPath(
                        path = arrowPath,
                        color = Color.White,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SocialHub",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            )
            
            Text(
                text = "DOWNLOADER & PLAYER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    letterSpacing = 3.sp
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Smooth linear loader
            LinearProgressIndicator(
                progress = progress.value,
                color = ElectricPurple,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
            )
        }
    }
}
