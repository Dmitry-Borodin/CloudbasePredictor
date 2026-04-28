package com.cloudbasepredictor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppLaunchInstrumentedTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appLaunches_andHomeScreenIsVisible() {
        assertMapChromeVisible()
    }

    private fun assertMapChromeVisible() {
        composeRule.onNodeWithContentDescription("Favorites").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(
                "Map provider is unavailable right now.",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty() || composeRule.onAllNodesWithText(
                "OpenFreeMap",
                substring = true,
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        val mapUnavailableVisible = composeRule.onAllNodesWithText(
            "Map provider is unavailable right now.",
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()
        val mapAttributionVisible = composeRule.onAllNodesWithText(
            "OpenFreeMap",
            substring = true,
            useUnmergedTree = true,
        ).fetchSemanticsNodes().isNotEmpty()
        assertTrue(mapUnavailableVisible || mapAttributionVisible)
    }
}
