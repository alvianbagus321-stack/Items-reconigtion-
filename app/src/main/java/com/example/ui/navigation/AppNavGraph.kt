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
import com.example.ui.screens.group.GroupsScreen
import com.example.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("groups") {
            GroupsScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("camera") {
            CameraScreen(navController = navController)
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
