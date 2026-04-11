package com.cloudbasepredictor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cloudbasepredictor.ui.screens.forecast.ForecastRoute
import com.cloudbasepredictor.ui.screens.map.MapRoute

@Composable
fun CloudbaseNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Map.route,
        modifier = modifier,
    ) {
        composable(route = TopLevelDestination.Map.route) {
            MapRoute(
                onOpenForecast = {
                    navController.navigate(TopLevelDestination.Forecast.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(route = TopLevelDestination.Forecast.route) {
            ForecastRoute()
        }
    }
}
