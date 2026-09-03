package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.NutriViewModel

object NutriDestinations {
    const val HOME_ROUTE = "home"
    const val SETTINGS_ROUTE = "settings"
    const val GOALS_ROUTE = "goals"
}

@Composable
fun NutriNavGraph(
    viewModel: NutriViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NutriDestinations.HOME_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NutriDestinations.HOME_ROUTE) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGoals = { navController.navigate(NutriDestinations.GOALS_ROUTE) },
                onNavigateToSettings = { navController.navigate(NutriDestinations.SETTINGS_ROUTE) }
            )
        }
        composable(NutriDestinations.GOALS_ROUTE) {
            GoalsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NutriDestinations.SETTINGS_ROUTE) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
