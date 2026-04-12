package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.views.CloudForecastView
import com.cloudbasepredictor.ui.screens.forecast.views.StuveForecastView
import com.cloudbasepredictor.ui.screens.forecast.views.ThermicForecastView
import com.cloudbasepredictor.ui.screens.forecast.views.WindForecastView
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun ForecastRoute(
    onOpenMap: () -> Unit,
    viewModel: ForecastViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForecastScreen(
        uiState = uiState,
        onDateSelected = viewModel::selectDay,
        onForecastModeSelected = viewModel::selectForecastMode,
        onForecastViewportTopChanged = viewModel::updateChartTopAltitude,
        onOpenMap = onOpenMap,
    )
}

@Composable
fun ForecastScreen(
    uiState: ForecastUiState,
    onDateSelected: (Int) -> Unit,
    onForecastModeSelected: (ForecastMode) -> Unit = {},
    onForecastViewportTopChanged: (Float) -> Unit = {},
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ForecastTopBar(
            placeName = uiState.selectedPlace?.name,
            selectedMode = uiState.selectedForecastMode,
            onModeSelected = onForecastModeSelected,
            onOpenMap = onOpenMap,
        )

        when (uiState.selectedForecastMode) {
            ForecastMode.THERMIC -> {
                ThermicForecastView(
                    uiState = uiState,
                    onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                    modifier = Modifier.weight(1f),
                )
            }
            ForecastMode.STUVE -> {
                StuveForecastView(
                    uiState = uiState,
                    onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                    modifier = Modifier.weight(1f),
                )
            }
            ForecastMode.WIND -> {
                WindForecastView(
                    uiState = uiState,
                    onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                    modifier = Modifier.weight(1f),
                )
            }
            ForecastMode.CLOUD -> {
                CloudForecastView(
                    uiState = uiState,
                    onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ForecastDatePicker(
            dayChips = uiState.dayChips,
            selectedDayIndex = uiState.selectedDayIndex,
            onDateSelected = onDateSelected,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastScreenPreview() {
    CloudbasePredictorTheme {
        ForecastScreen(
            uiState = PreviewData.forecastReadyUiState,
            onDateSelected = {},
            onOpenMap = {},
        )
    }
}
