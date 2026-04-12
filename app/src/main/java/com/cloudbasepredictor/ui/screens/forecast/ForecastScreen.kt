package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.model.ForecastModel
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
        onRetryLoad = viewModel::retryLoad,
        onModelSelected = viewModel::selectModel,
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
    onRetryLoad: () -> Unit = {},
    onModelSelected: (ForecastModel) -> Unit = {},
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
            when {
                uiState.isLoading -> {
                    ForecastLoadingContent(
                        placeName = uiState.selectedPlace?.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.errorMessage != null -> {
                    ForecastErrorContent(
                        errorMessage = uiState.errorMessage,
                        onRetry = onRetryLoad,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    ForecastReadyContent(
                        uiState = uiState,
                        onForecastViewportTopChanged = onForecastViewportTopChanged,
                        onStuveHourChanged = onStuveHourChanged,
                        onModelSelected = onModelSelected,
                    )
                }
            }
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

@Composable
private fun ForecastLoadingContent(
    placeName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (placeName != null) "Loading forecast for $placeName…" else "Loading forecast…",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
    }
}

@Composable
private fun ForecastErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@Composable
private fun ForecastReadyContent(
    uiState: ForecastUiState,
    onForecastViewportTopChanged: (Float) -> Unit,
    onStuveHourChanged: (Int) -> Unit,
    onModelSelected: (ForecastModel) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

        ModelSelectorOverlay(
            selectedModel = uiState.selectedModel,
            resolvedModel = uiState.resolvedModel,
            onModelSelected = onModelSelected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
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
private fun ForecastScreenLoadingPreview() {
    CloudbasePredictorTheme {
        ForecastScreen(
            uiState = PreviewData.forecastLoadingUiState,
            onDateSelected = {},
            onOpenMap = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastScreenErrorPreview() {
    CloudbasePredictorTheme {
        ForecastScreen(
            uiState = PreviewData.forecastErrorUiState,
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
