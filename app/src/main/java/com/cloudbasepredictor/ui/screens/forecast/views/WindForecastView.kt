package com.cloudbasepredictor.ui.screens.forecast.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_VIEW
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun WindForecastView(
    uiState: ForecastUiState,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ForecastGridCard(
        uiState = uiState,
        mode = ForecastMode.WIND,
        minAltitudeKm = 0.4f,
        onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
        modifier = modifier.testTag(WIND_VIEW),
    )
}

@Preview(name = "Wind Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun WindForecastViewPreview() {
    CloudbasePredictorTheme {
        WindForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.WIND),
        )
    }
}

@Preview(name = "Wind Loading", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun WindForecastViewLoadingPreview() {
    CloudbasePredictorTheme {
        WindForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.WIND,
                isLoading = true,
            ),
        )
    }
}
