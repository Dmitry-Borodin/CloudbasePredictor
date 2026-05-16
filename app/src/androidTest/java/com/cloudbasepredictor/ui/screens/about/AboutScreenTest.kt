package com.cloudbasepredictor.ui.screens.about

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudbasepredictor.R
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Rule
import org.junit.Test

class AboutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aboutScreen_groupsMapProvidersAndEsriSources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            CloudbasePredictorTheme {
                AboutScreen(onBack = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.about_map_services_label))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.about_openfreemap))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.about_opentopomap))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.about_nasa_gibs))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.about_esri_world_imagery))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Powered by Esri", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onAllNodesWithText("Map tiles", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Esri attribution", substring = true).assertCountEquals(0)
    }
}
