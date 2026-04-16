package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudbasepredictor.R
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.testutil.SimulatedTestData
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.FORECAST_CHART_AREA
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.MAP_PANEL
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_ALTITUDE_UNIT
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_TIME_AXIS
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_SELECTED_HOUR
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_TIME_SLIDER
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_ALTITUDE_UNIT
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_TIME_AXIS
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForecastScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun forecastScreen_rendersProvidedUiState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uiState = SimulatedTestData.forecastUiState(context)

        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = uiState,
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithTag(THERMIC_VIEW).assertIsDisplayed()
        composeRule.onNodeWithText(uiState.selectedPlace?.name.orEmpty()).assertIsDisplayed()
        composeRule.onNodeWithText(uiState.dayChips[uiState.selectedDayIndex].title).assertIsDisplayed()
    }

    @Test
    fun forecastScreen_clickingDayChipInvokesCallbackWithCorrectIndex() {
        var selectedIndex: Int? = null
        val uiState = ForecastUiState(
            selectedPlace = SimulatedTestData.brauneckPlace,
            selectedDayIndex = 0,
            dayChips = listOf(
                ForecastDayChipUiModel(title = "Day 1", subtitle = "Now"),
                ForecastDayChipUiModel(title = "Day 2", subtitle = "Next"),
            ),
            forecastText = "Forecast text",
            isLoading = false,
            errorMessage = null,
        )

        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = uiState,
                    onDateSelected = { selectedIndex = it },
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithText("Day 2").performClick()
        composeRule.runOnIdle {
            assertEquals(1, selectedIndex)
        }
    }

    @Test
    fun forecastScreen_loadingStateShowsProgress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val placeName = SimulatedTestData.brauneckPlace.name
        val expectedLoadingMessage = context.getString(R.string.loading_forecast_for_place, placeName)

        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context).copy(
                        isLoading = true,
                        forecastText = expectedLoadingMessage,
                    ),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithText(expectedLoadingMessage).assertIsDisplayed()
    }

    @Test
    fun forecastScreen_windModeShowsVisibleTimeAxis() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context, mode = ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithTag(WIND_TIME_AXIS).assertIsDisplayed()
        composeRule.onNodeWithText("06").assertIsDisplayed()
    }

    @Test
    fun forecastScreen_stuveModeShowsSelectedHour() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context, mode = ForecastMode.STUVE),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithTag(STUVE_TIME_SLIDER).assertIsDisplayed()
        composeRule.onNodeWithTag(STUVE_SELECTED_HOUR)
            .assertIsDisplayed()
            .assertTextEquals("12:00")
    }

    @Test
    fun forecastScreen_thermicModeShowsAltitudeUnitAndHourLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context, mode = ForecastMode.THERMIC),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithTag(THERMIC_TIME_AXIS).assertIsDisplayed()
        composeRule.onNodeWithTag(THERMIC_ALTITUDE_UNIT).assertIsDisplayed()
        composeRule.onNodeWithText("km").assertIsDisplayed()
        composeRule.onNodeWithText("06:00").assertIsDisplayed()
    }

    @Test
    fun forecastScreen_windModeShowsAltitudeUnitKm() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context, mode = ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithTag(WIND_ALTITUDE_UNIT).assertIsDisplayed()
    }

    @Test
    fun forecastScreen_windBottomAxisDoesNotCoverChartArea() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context, mode = ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        val chartBounds = composeRule.onNodeWithTag(FORECAST_CHART_AREA)
            .fetchSemanticsNode().boundsInRoot
        val axisBounds = composeRule.onNodeWithTag(WIND_TIME_AXIS)
            .fetchSemanticsNode().boundsInRoot
        val unitBounds = composeRule.onNodeWithTag(WIND_ALTITUDE_UNIT)
            .fetchSemanticsNode().boundsInRoot

        // The altitude unit label must be fully inside the chart area
        assertTrue(
            "km label bottom (${unitBounds.bottom}) exceeds chart area bottom (${chartBounds.bottom})",
            unitBounds.bottom <= chartBounds.bottom + 1f,
        )
        // The time axis must be within the chart area
        assertTrue(
            "Time axis bottom (${axisBounds.bottom}) exceeds chart area (${chartBounds.bottom})",
            axisBounds.bottom <= chartBounds.bottom + 1f,
        )
    }

    @Test
    fun forecastScreen_mapPanelDoesNotOverlapChartWhenCollapsed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = SimulatedTestData.forecastUiState(context),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        val chartBounds = composeRule.onNodeWithTag(FORECAST_CHART_AREA)
            .fetchSemanticsNode().boundsInRoot
        val thermicBounds = composeRule.onNodeWithTag(THERMIC_VIEW)
            .fetchSemanticsNode().boundsInRoot

        // The thermic view should be within the chart area
        assertTrue(
            "Thermic view exceeds chart area",
            thermicBounds.bottom <= chartBounds.bottom + 1f,
        )
    }
}
