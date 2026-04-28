package com.cloudbasepredictor.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Rule
import org.junit.Test

class MapAttributionOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapAttributionOverlay_showsCompactAttribution() {
        composeRule.setContent {
            CloudbasePredictorTheme {
                MapAttributionOverlay()
            }
        }

        composeRule.onNodeWithText("© OpenMapTiles · © OpenStreetMap")
            .assertIsDisplayed()
    }
}
