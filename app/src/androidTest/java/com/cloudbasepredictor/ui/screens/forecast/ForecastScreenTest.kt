package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ForecastScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun forecastScreen_rendersProvidedUiState() {
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastReadyUiState,
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithText("Sat in Interlaken. Partly cloudy. High 20.0°C, low 10.2°C.")
            .assertIsDisplayed()
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
        composeRule.setContent {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastLoadingUiState,
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }

        composeRule.onNodeWithText("Loading a 14-day forecast for Interlaken.").assertIsDisplayed()
    }
}
