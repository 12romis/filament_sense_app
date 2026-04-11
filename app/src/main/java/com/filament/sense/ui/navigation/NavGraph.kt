package com.filament.sense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filament.sense.ui.screen.analytics.AnalyticsScreen
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
    object SpoolDetail : Screen("spools/{id}") {
        fun createRoute(id: Int) = "spools/$id"
    }
    object SpoolEdit : Screen("spools/{id}/edit") {
        fun createRoute(id: Int) = "spools/$id/edit"
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
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            SpoolDetailScreen(id = id, navController = navController)
        }
        composable(Screen.SpoolCreate.route) {
            SpoolCreateScreen(navController = navController)
        }
        composable(
            route = Screen.SpoolEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) {
            SpoolEditScreen(navController = navController)
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
