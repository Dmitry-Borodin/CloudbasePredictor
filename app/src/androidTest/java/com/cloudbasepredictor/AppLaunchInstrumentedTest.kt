package com.cloudbasepredictor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLaunchInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_andHomeScreenIsVisible() {
        composeRule.onNodeWithText(
            "Tap anywhere on the OpenFreeMap map to place a marker."
        ).assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_opensForecastScreen() {
        composeRule.onNodeWithContentDescription("Forecast", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("No location selected").assertIsDisplayed()
    }
}
