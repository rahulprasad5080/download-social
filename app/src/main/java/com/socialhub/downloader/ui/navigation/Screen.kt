package com.socialhub.downloader.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object VideoPreview : Screen("preview/{videoUrl}") {
        fun createRoute(url: String) = "preview/${android.net.Uri.encode(url)}"
    }
    object DownloadManager : Screen("download", "Downloads", Icons.Default.Download)
    object MediaPlayer : Screen("player/{mediaPath}") {
        fun createRoute(path: String) = "player/${android.net.Uri.encode(path)}"
    }
    object Library : Screen("library", "Library", Icons.Default.VideoLibrary)
    object Profile : Screen("profile", "Settings", Icons.Default.Person)

    companion object {
        val bottomNavItems = listOf(Home, DownloadManager, Library, Profile)
    }
}
