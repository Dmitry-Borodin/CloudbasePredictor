package com.cloudbasepredictor

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
