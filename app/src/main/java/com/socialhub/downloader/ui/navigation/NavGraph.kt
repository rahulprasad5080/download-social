package com.socialhub.downloader.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.socialhub.downloader.ui.screens.download.DownloadManagerScreen
import com.socialhub.downloader.ui.screens.home.HomeScreen
import com.socialhub.downloader.ui.screens.library.LibraryScreen
import com.socialhub.downloader.ui.screens.player.MediaPlayerScreen
import com.socialhub.downloader.ui.screens.preview.VideoPreviewScreen
import com.socialhub.downloader.ui.screens.profile.ProfileScreen
import com.socialhub.downloader.ui.screens.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(
            route = Screen.VideoPreview.route,
            arguments = listOf(navArgument("videoUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
            VideoPreviewScreen(videoUrl = videoUrl, navController = navController)
        }
        
        composable(Screen.DownloadManager.route) {
            DownloadManagerScreen(navController = navController)
        }
        
        composable(
            route = Screen.MediaPlayer.route,
            arguments = listOf(navArgument("mediaPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val mediaPath = backStackEntry.arguments?.getString("mediaPath") ?: ""
            MediaPlayerScreen(mediaPath = mediaPath, navController = navController)
        }
        
        composable(Screen.Library.route) {
            LibraryScreen(navController = navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController
            )
        }
    }
}
