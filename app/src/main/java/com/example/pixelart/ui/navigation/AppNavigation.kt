package com.example.pixelart.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pixelart.ui.coloring.PixelArtScreen
import com.example.pixelart.ui.gallery.GalleryScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "gallery") {
        composable("gallery") {
            GalleryScreen(navController = navController)
        }
        composable(
            route = "coloring/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: -1L
            PixelArtScreen(navController = navController, projectId = projectId)
        }
    }
}