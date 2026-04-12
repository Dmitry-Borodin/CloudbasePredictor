package com.cloudbasepredictor.ui.screens.forecast.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_VIEW
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun CloudForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
) {
    ForecastGridCard(
        uiState = uiState,
        mode = ForecastMode.CLOUD,
        minAltitudeKm = 1.0f,
        onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
        modifier = modifier.testTag(CLOUD_VIEW),
    )
}

@Preview(name = "Cloud Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CloudForecastViewPreview() {
    CloudbasePredictorTheme {
        CloudForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.CLOUD),
        )
    }
}

@Preview(name = "Cloud Zoomed Out", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CloudForecastViewZoomedOutPreview() {
    CloudbasePredictorTheme {
        CloudForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.CLOUD,
                topAltitudeKm = 6.5f,
            ),
        )
    }
}
