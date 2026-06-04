package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.camera.CameraScreen
import com.example.ui.screens.item.AddItemScreen
import com.example.ui.screens.item.ItemDetailScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.search.VisualSearchScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable(
            "camera?mode={mode}",
            arguments = listOf(navArgument("mode") { defaultValue = "add" })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "add"
            CameraScreen(navController = navController, mode = mode)
        }
        composable(
            "visual_search?imageUri={imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            VisualSearchScreen(navController = navController, imageUri = imageUri)
        }
        composable("add_item?imageUri={imageUri}") { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            AddItemScreen(navController = navController, initialImageUri = imageUri)
        }
        composable("item_detail/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            ItemDetailScreen(navController = navController, itemId = itemId)
        }
    }
}
