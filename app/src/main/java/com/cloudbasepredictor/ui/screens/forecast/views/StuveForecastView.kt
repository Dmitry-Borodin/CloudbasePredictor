package com.cloudbasepredictor.ui.screens.forecast.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_VIEW
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun StuveForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
) {
    ForecastGridCard(
        uiState = uiState,
        mode = ForecastMode.STUVE,
        minAltitudeKm = 0.6f,
        onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
        modifier = modifier.testTag(STUVE_VIEW),
    )
}

@Preview(name = "Stuve Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun StuveForecastViewPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.STUVE),
        )
    }
}

@Preview(name = "Stuve Error", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun StuveForecastViewErrorPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.STUVE,
                errorMessage = "Unable to refresh forecast layers right now.",
            ),
        )
    }
}
