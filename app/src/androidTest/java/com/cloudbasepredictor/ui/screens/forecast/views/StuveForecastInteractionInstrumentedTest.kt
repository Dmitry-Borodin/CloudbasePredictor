package com.cloudbasepredictor.ui.screens.forecast.views

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.testutil.SimulatedTestData
import com.cloudbasepredictor.ui.screens.forecast.DEFAULT_TOP_ALTITUDE_KM
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_CHART_CANVAS
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StuveForecastInteractionInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun stuveView_autoFitsTopAltitude_whenOpened() {
        var visibleTopAltitudeKm by mutableFloatStateOf(DEFAULT_TOP_ALTITUDE_KM)

        composeRule.setContent {
            CloudbasePredictorTheme {
                StuveForecastView(
                    uiState = SimulatedTestData.forecastUiState(
                        composeRule.activity,
                        mode = ForecastMode.STUVE,
                        topAltitudeKm = visibleTopAltitudeKm,
                    ),
                    onVisibleTopAltitudeChange = { visibleTopAltitudeKm = it },
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(visibleTopAltitudeKm > DEFAULT_TOP_ALTITUDE_KM + 0.4f)
        }
    }

    @Test
    fun stuveView_tapPinsCursorReadout() {
        var visibleTopAltitudeKm by mutableFloatStateOf(DEFAULT_TOP_ALTITUDE_KM)

        composeRule.setContent {
            CloudbasePredictorTheme {
                StuveForecastView(
                    uiState = SimulatedTestData.forecastUiState(
                        composeRule.activity,
                        mode = ForecastMode.STUVE,
                        topAltitudeKm = visibleTopAltitudeKm,
                    ),
                    onVisibleTopAltitudeChange = { visibleTopAltitudeKm = it },
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(STUVE_CHART_CANVAS)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "idle"))
            .performTouchInput {
                click(center)
            }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(STUVE_CHART_CANVAS)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "pinned"))
    }

    @Test
    fun stuveView_twoFingerPinchZoom_changesTopAltitude() {
        var visibleTopAltitudeKm by mutableFloatStateOf(DEFAULT_TOP_ALTITUDE_KM)

        composeRule.setContent {
            CloudbasePredictorTheme {
                StuveForecastView(
                    uiState = SimulatedTestData.forecastUiState(
                        composeRule.activity,
                        mode = ForecastMode.STUVE,
                        topAltitudeKm = visibleTopAltitudeKm,
                    ),
                    onVisibleTopAltitudeChange = { visibleTopAltitudeKm = it },
                )
            }
        }

        composeRule.waitForIdle()
        val autoFitTopAltitudeKm = composeRule.runOnIdle { visibleTopAltitudeKm }

        composeRule.onNodeWithTag(STUVE_CHART_CANVAS).performTouchInput {
            val centerX = width / 2f
            val centerY = height / 2f
            val startHalfSpan = width * 0.12f
            val endHalfSpan = startHalfSpan * 2.2f

            pinch(
                start0 = Offset(centerX - startHalfSpan, centerY),
                end0 = Offset(centerX - endHalfSpan, centerY),
                start1 = Offset(centerX + startHalfSpan, centerY),
                end1 = Offset(centerX + endHalfSpan, centerY),
                durationMillis = 250,
            )
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(visibleTopAltitudeKm < autoFitTopAltitudeKm - 0.6f)
        }
    }
}
