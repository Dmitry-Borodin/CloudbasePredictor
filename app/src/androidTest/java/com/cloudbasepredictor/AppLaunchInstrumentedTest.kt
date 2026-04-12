package com.cloudbasepredictor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_andHomeScreenIsVisible() {
        assertMapHomeVisible()
    }

    @Test
    fun bottomNavigation_opensForecastScreen() {
        composeRule.onNodeWithContentDescription("Forecast", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()
    }

    @Test
    fun forecastTopBarMapButton_returnsToMapScreen() {
        composeRule.onNodeWithContentDescription("Forecast", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Open map", useUnmergedTree = true).performClick()
        assertMapHomeVisible()
    }

    private fun assertMapHomeVisible() {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(
                "Tap anywhere on the OpenFreeMap map to place a marker.",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty() || composeRule.onAllNodesWithText(
                "Map provider is unavailable right now.",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val mapInstructionVisible = composeRule.onAllNodesWithText(
            "Tap anywhere on the OpenFreeMap map to place a marker.",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()
        val mapUnavailableVisible = composeRule.onAllNodesWithText(
            "Map provider is unavailable right now.",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()
        assertTrue(mapInstructionVisible || mapUnavailableVisible)
    }
}
