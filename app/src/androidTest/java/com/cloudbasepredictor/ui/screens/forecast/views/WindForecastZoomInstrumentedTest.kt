package com.cloudbasepredictor.ui.screens.forecast.views

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.DEFAULT_TOP_ALTITUDE_KM
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_VIEW
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uses preview-backed fake data only, so the test never depends on the real forecast backend.
 */
@RunWith(AndroidJUnit4::class)
class WindForecastZoomInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun windView_twoFingerPinchZoom_scalesWithGestureStrength() {
        var visibleTopAltitudeKm by mutableFloatStateOf(DEFAULT_TOP_ALTITUDE_KM)

        composeRule.setContent {
            CloudbasePredictorTheme {
                WindForecastView(
                    uiState = PreviewData.forecastUiStateForMode(
                        mode = ForecastMode.WIND,
                        topAltitudeKm = visibleTopAltitudeKm,
                    ),
                    onVisibleTopAltitudeChange = { visibleTopAltitudeKm = it },
                )
            }
        }

        fun performPinchOut(zoomRatio: Float): Float {
            composeRule.runOnIdle {
                visibleTopAltitudeKm = DEFAULT_TOP_ALTITUDE_KM
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag(WIND_VIEW).performTouchInput {
                val centerX = width / 2f
                val centerY = height / 2f
                val startHalfSpan = width * 0.12f
                val endHalfSpan = startHalfSpan * zoomRatio

                pinch(
                    start0 = Offset(centerX - startHalfSpan, centerY),
                    end0 = Offset(centerX - endHalfSpan, centerY),
                    start1 = Offset(centerX + startHalfSpan, centerY),
                    end1 = Offset(centerX + endHalfSpan, centerY),
                    durationMillis = 250,
                )
            }

            composeRule.waitForIdle()
            return composeRule.runOnIdle { visibleTopAltitudeKm }
        }

        val gentleZoomResult = performPinchOut(zoomRatio = 1.2f)
        val strongZoomResult = performPinchOut(zoomRatio = 2.4f)
        val gentleZoomDelta = DEFAULT_TOP_ALTITUDE_KM - gentleZoomResult
        val strongZoomDelta = DEFAULT_TOP_ALTITUDE_KM - strongZoomResult

        assertTrue(gentleZoomResult < DEFAULT_TOP_ALTITUDE_KM - 0.4f)
        assertTrue(strongZoomResult < gentleZoomResult - 1.0f)
        assertTrue(strongZoomDelta > gentleZoomDelta * 2f)
    }
}
