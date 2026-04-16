package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudbasepredictor.R
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_SELECTED_HOUR
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_TIME_SLIDER
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_TIME_AXIS
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ForecastScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun forecastScreen_rendersProvidedUiState() {
        val uiState = PreviewData.forecastReadyUiState

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
            selectedPlace = PreviewData.savedPlace,
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
        val placeName = PreviewData.forecastLoadingUiState.selectedPlace?.name.orEmpty()
        val expectedLoadingMessage = context.getString(R.string.loading_forecast_for_place, placeName)

        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastLoadingUiState,
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithText(expectedLoadingMessage).assertIsDisplayed()
    }

    @Test
    fun forecastScreen_windModeShowsVisibleTimeAxis() {
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastUiStateForMode(ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithTag(WIND_TIME_AXIS).assertIsDisplayed()
    }

    @Test
    fun forecastScreen_stuveModeShowsSelectedHour() {
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastUiStateForMode(ForecastMode.STUVE),
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
}
