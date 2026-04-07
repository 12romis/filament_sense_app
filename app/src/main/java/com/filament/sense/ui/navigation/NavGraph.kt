package com.filament.sense.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filament.sense.ui.screen.home.HomeScreen
import com.filament.sense.ui.screen.scan.ScanScreen
import com.filament.sense.ui.screen.settings.SettingsScreen
import com.filament.sense.ui.screen.spool.SpoolDetailScreen
import com.filament.sense.ui.screen.spools.SpoolCreateScreen
import com.filament.sense.ui.screen.spools.SpoolEditScreen
import com.filament.sense.ui.screen.spools.SpoolListScreen

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Scan       : Screen("scan")
    object SpoolList  : Screen("spools")
    object SpoolDetail : Screen("spools/{index}") {
        fun createRoute(index: Int) = "spools/$index"
    }
    object SpoolEdit : Screen("spools/{index}/edit") {
        fun createRoute(index: Int) = "spools/$index/edit"
    }
    object SpoolCreate : Screen("spools/create")
    object Analytics   : Screen("analytics")
    object Settings    : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Scan.route) {
            ScanScreen(navController = navController)
        }
        composable(Screen.SpoolList.route) {
            SpoolListScreen(navController = navController)
        }
        composable(
            route = Screen.SpoolDetail.route,
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            SpoolDetailScreen(index = index, navController = navController)
        }
        composable(Screen.SpoolCreate.route) {
            SpoolCreateScreen(navController = navController)
        }
        composable(
            route = Screen.SpoolEdit.route,
            arguments = listOf(navArgument("index") { type = NavType.IntType }),
        ) {
            SpoolEditScreen(navController = navController)
        }
        composable(Screen.Analytics.route) {
            // Аналітика — Coming Soon (Phase 5)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Аналітика — скоро",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
