package com.cloudbasepredictor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cloudbasepredictor.ui.navigation.CloudbaseNavGraph
import com.cloudbasepredictor.ui.navigation.TopLevelDestination
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun CloudbasePredictorApp(
    navGraph: @Composable (Modifier, NavHostController) -> Unit = { modifier, navController ->
        CloudbaseNavGraph(
            navController = navController,
            modifier = modifier,
        )
    },
) {
    CloudbasePredictorTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val isSelected = currentDestination
                            ?.hierarchy
                            ?.any { navDestination -> navDestination.route == destination.route } == true

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(TopLevelDestination.Map.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { destination.icon() },
                            label = { Text(text = destination.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            navGraph(Modifier.padding(innerPadding), navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CloudbasePredictorAppPreview() {
    CloudbasePredictorApp(
        navGraph = { modifier, _ ->
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Cloudbase app preview")
            }
        },
    )
}
