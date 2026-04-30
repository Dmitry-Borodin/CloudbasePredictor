package com.cloudbasepredictor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudbasepredictor.ui.components.MapTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
        composeRule.onAllNodesWithContentDescription("Favorites").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag(MapTestTags.ATTRIBUTION_OVERLAY).assertIsDisplayed()
    }
}
