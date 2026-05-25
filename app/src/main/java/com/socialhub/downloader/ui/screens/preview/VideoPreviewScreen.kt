package com.socialhub.downloader.ui.screens.preview

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.socialhub.downloader.ui.components.AnimatedDownloadButton
import com.socialhub.downloader.ui.components.GlassCard
import com.socialhub.downloader.ui.components.SkeletonItem
import com.socialhub.downloader.ui.navigation.Screen
import com.socialhub.downloader.ui.theme.CyberCyan
import com.socialhub.downloader.ui.theme.ElectricPurple
import com.socialhub.downloader.ui.theme.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPreviewScreen(
    videoUrl: String,
    navController: NavController,
    viewModel: VideoPreviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val videoDetails by viewModel.videoDetails.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    LaunchedEffect(key1 = videoUrl) {
        viewModel.loadVideoDetails(videoUrl)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resolve Media", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Added to Favorites", Toast.LENGTH_SHORT).show() }) {
                        Icon(imageVector = Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Skeleton Screen for Video Resolution
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        cornerRadius = 20
                    )
                    SkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(24.dp)
                    )
                    SkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonItem(modifier = Modifier.size(40.dp), cornerRadius = 20)
                        Column {
                            SkeletonItem(modifier = Modifier.width(100.dp).height(12.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            SkeletonItem(modifier = Modifier.width(60.dp).height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    repeat(3) {
                        SkeletonItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            cornerRadius = 12
                        )
                    }
                }
            } else {
                videoDetails?.let { details ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFF334155), Color(0xFF0F172A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video Icon
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Preview Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            // Platform Icon indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(details.platform.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = details.platform.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Duration
                            Text(
                                text = details.duration,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Creator & Meta details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = details.creatorName.take(2).uppercase(),
                                    color = ElectricPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "@${details.creatorName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${details.views} • ${details.likes}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title
                        Text(
                            text = details.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quality selector section
                        Text(
                            text = "Select Download Quality",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectedQuality.values().forEach { quality ->
                                val isSelected = selectedQuality == quality
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectQuality(quality) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isSelected) ElectricPurple else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = quality.label,
                                                color = if (isSelected) Color.White else Color.LightGray,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        Text(
                                            text = quality.size,
                                            color = if (isSelected) CyberCyan else Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Premium Promo Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                NeonPink.copy(alpha = 0.15f),
                                                ElectricPurple.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Premium Star",
                                    tint = NeonPink,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Unlimit Download Speeds",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Unlock FHD downloads & remove all popup ads.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Dynamic Action Button
                        AnimatedDownloadButton(
                            buttonState = downloadState,
                            progress = downloadProgress,
                            onClick = {
                                viewModel.startDownload { title, qual ->
                                    Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
                                    // Navigate to Download Manager screen
                                    navController.navigate(Screen.DownloadManager.route) {
                                        popUpTo(Screen.Home.route)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
