package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForecastTopBar(
    placeName: String?,
    selectedMode: ForecastMode,
    onModeSelected: (ForecastMode) -> Unit,
    onOpenMap: () -> Unit,
    onFavoriteClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(text = placeName ?: "Forecast")
        },
        navigationIcon = {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                )
            }
        },
        actions = {
            ForecastModePicker(
                selectedMode = selectedMode,
                onModeSelected = onModeSelected,
            )
            IconButton(onClick = onOpenMap) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = "Open map",
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ForecastTopBarPreview() {
    CloudbasePredictorTheme {
        ForecastTopBar(
            placeName = "Interlaken",
            selectedMode = ForecastMode.THERMIC,
            onModeSelected = {},
            onOpenMap = {},
        )
    }
}
