package com.cloudbasepredictor.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cloudbasepredictor.ui.screens.forecast.ForecastRoute
import com.cloudbasepredictor.ui.screens.map.MapRoute
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun CloudbaseNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mapDestination: @Composable (onOpenForecast: () -> Unit) -> Unit = { onOpenForecast ->
        MapRoute(onOpenForecast = onOpenForecast)
    },
    forecastDestination: @Composable (onOpenMap: () -> Unit) -> Unit = { onOpenMap ->
        ForecastRoute(onOpenMap = onOpenMap)
    },
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.Map.route,
        modifier = modifier,
    ) {
        composable(route = TopLevelDestination.Map.route) {
            mapDestination {
                navController.navigate(TopLevelDestination.Forecast.route) {
                    launchSingleTop = true
                }
            }
        }
        composable(route = TopLevelDestination.Forecast.route) {
            forecastDestination {
                val popped = navController.popBackStack(
                    route = TopLevelDestination.Map.route,
                    inclusive = false,
                )
                if (!popped) {
                    navController.navigate(TopLevelDestination.Map.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CloudbaseNavGraphPreview() {
    CloudbasePredictorTheme {
        val navController = rememberNavController()
        CloudbaseNavGraph(
            navController = navController,
            mapDestination = { _ ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Map destination preview")
                }
            },
            forecastDestination = { _ ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Forecast destination preview")
                }
            },
        )
    }
}
