package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.components.SaveFavoriteDialog
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
        onStuveHourChanged = viewModel::updateStuveHour,
        onSaveFavorite = viewModel::saveFavorite,
        onDeleteFavorite = viewModel::deleteFavorite,
        onOpenMap = onOpenMap,
    )
}

@Composable
fun ForecastScreen(
    uiState: ForecastUiState,
    onDateSelected: (Int) -> Unit,
    onForecastModeSelected: (ForecastMode) -> Unit = {},
    onForecastViewportTopChanged: (Float) -> Unit = {},
    onStuveHourChanged: (Int) -> Unit = {},
    onSaveFavorite: (String) -> Unit = {},
    onDeleteFavorite: () -> Unit = {},
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFavoriteDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ForecastTopBar(
            placeName = uiState.selectedPlace?.name,
            isFavorite = uiState.selectedPlace?.isFavorite == true,
            selectedMode = uiState.selectedForecastMode,
            onModeSelected = onForecastModeSelected,
            onFavoriteClick = { showFavoriteDialog = true },
            onOpenMap = onOpenMap,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (uiState.selectedForecastMode) {
                ForecastMode.THERMIC -> {
                    ThermicForecastView(
                        uiState = uiState,
                        onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ForecastMode.STUVE -> {
                    StuveForecastView(
                        uiState = uiState,
                        onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                        onStuveHourChanged = onStuveHourChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ForecastMode.WIND -> {
                    WindForecastView(
                        uiState = uiState,
                        onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ForecastMode.CLOUD -> {
                    CloudForecastView(
                        uiState = uiState,
                        onVisibleTopAltitudeChange = onForecastViewportTopChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            HelpButtonOverlay(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp),
            )
        }

        ForecastDatePicker(
            dayChips = uiState.dayChips,
            selectedDayIndex = uiState.selectedDayIndex,
            onDateSelected = onDateSelected,
        )
    }

    if (showFavoriteDialog) {
        val place = uiState.selectedPlace
        SaveFavoriteDialog(
            currentName = if (place?.isFavorite == true) place.name else "",
            isFavorite = place?.isFavorite == true,
            onSave = onSaveFavorite,
            onDelete = onDeleteFavorite,
            onDismiss = { showFavoriteDialog = false },
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

@Preview(showBackground = true)
@Composable
private fun ForecastScreenCloudPreview() {
    CloudbasePredictorTheme {
        ForecastScreen(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.CLOUD),
            onDateSelected = {},
            onOpenMap = {},
        )
    }
}
