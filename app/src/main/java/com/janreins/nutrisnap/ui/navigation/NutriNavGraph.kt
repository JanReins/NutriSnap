package com.janreins.nutrisnap.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.janreins.nutrisnap.ui.screens.GoalsScreen
import com.janreins.nutrisnap.ui.screens.HomeScreen
import com.janreins.nutrisnap.ui.screens.LockMode
import com.janreins.nutrisnap.ui.screens.LockScreen
import com.janreins.nutrisnap.ui.screens.SettingsScreen
import com.janreins.nutrisnap.ui.screens.SummaryScreen
import com.janreins.nutrisnap.ui.viewmodel.NutriViewModel

object NutriDestinations {
    const val HOME_ROUTE = "home"
    const val SETTINGS_ROUTE = "settings"
    const val GOALS_ROUTE = "goals"
    const val LOCK_ROUTE = "lock"
    const val SUMMARY_ROUTE = "summary"
}

@Composable
fun NutriNavGraph(
    viewModel: NutriViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (uiState.isPinEnabled && !uiState.isUnlocked) {
        NutriDestinations.LOCK_ROUTE
    } else {
        NutriDestinations.HOME_ROUTE
    }

    LaunchedEffect(uiState.isPinEnabled, uiState.isUnlocked) {
        if (uiState.isPinEnabled && !uiState.isUnlocked) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != NutriDestinations.LOCK_ROUTE) {
                navController.navigate(NutriDestinations.LOCK_ROUTE) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NutriDestinations.LOCK_ROUTE) {
            LockScreen(
                viewModel = viewModel,
                mode = LockMode.UNLOCK,
                onSuccess = {
                    navController.navigate(NutriDestinations.HOME_ROUTE) {
                        popUpTo(NutriDestinations.LOCK_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(NutriDestinations.HOME_ROUTE) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGoals = {
                    navController.navigate(NutriDestinations.GOALS_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(NutriDestinations.SETTINGS_ROUTE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSummary = {
                    navController.navigate(NutriDestinations.SUMMARY_ROUTE) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(NutriDestinations.SUMMARY_ROUTE) {
            SummaryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLock = {
                    navController.navigate(NutriDestinations.LOCK_ROUTE) {
                        popUpTo(NutriDestinations.HOME_ROUTE)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
