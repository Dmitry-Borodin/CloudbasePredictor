package com.cloudbasepredictor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_MODE_TAB
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.HELP_BUTTON
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_MODE_TAB
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_MODE_TAB
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_MODE_TAB
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_VIEW
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
        openForecastScreen()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()
    }

    @Test
    fun forecastTopBarMapButton_returnsToMapScreen() {
        openForecastScreen()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Open map", useUnmergedTree = true).performClick()
        assertMapHomeVisible()
    }

    @Test
    fun realAppForecastTabs_switchDisplayedViews() {
        openForecastScreen()

        composeRule.onNodeWithTag(STUVE_MODE_TAB).performClick()
        composeRule.onNodeWithTag(STUVE_VIEW).assertIsDisplayed()

        composeRule.onNodeWithTag(WIND_MODE_TAB).performClick()
        composeRule.onNodeWithTag(WIND_VIEW).assertIsDisplayed()

        composeRule.onNodeWithTag(CLOUD_MODE_TAB).performClick()
        composeRule.onNodeWithTag(CLOUD_VIEW).assertIsDisplayed()

        composeRule.onNodeWithTag(THERMIC_MODE_TAB).performClick()
        composeRule.onNodeWithTag(THERMIC_VIEW).assertIsDisplayed()
    }

    @Test
    fun realAppForecastHelpDialog_opensAndUpdatesWithMode() {
        openForecastScreen()

        composeRule.onNodeWithTag(HELP_BUTTON).performClick()
        composeRule.onNodeWithText("Thermic forecast help").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()

        composeRule.onNodeWithTag(CLOUD_MODE_TAB).performClick()
        composeRule.onNodeWithTag(HELP_BUTTON).performClick()
        composeRule.onNodeWithText("Cloud forecast help").assertIsDisplayed()
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

    private fun openForecastScreen() {
        composeRule.onNodeWithContentDescription("Forecast", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag(THERMIC_VIEW).assertIsDisplayed()
    }
}
