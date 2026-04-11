package com.cloudbasepredictor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val iconVector: ImageVector,
) {
    Map(
        route = "map",
        label = "Map",
        iconVector = Icons.Outlined.Map,
    ),
    Forecast(
        route = "forecast",
        label = "Forecast",
        iconVector = Icons.Outlined.WbCloudy,
    ),
    ;

    @Composable
    fun icon() {
        Icon(
            imageVector = iconVector,
            contentDescription = label,
        )
    }
}
