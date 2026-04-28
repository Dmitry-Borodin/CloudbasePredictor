package com.cloudbasepredictor.ui.screens.forecast.views

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.testutil.SimulatedTestData
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_VIEW
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Uses simulated forecast data only, so the test never depends on the real forecast backend.
 */
@RunWith(AndroidJUnit4::class)
class WindForecastVisualInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun windView_rendersThermicLevelMarkersOverWindSpeedBackground() {
        composeRule.setContent {
            CloudbasePredictorTheme {
                WindForecastView(
                    uiState = SimulatedTestData.forecastUiState(
                        context = composeRule.activity,
                        mode = ForecastMode.WIND,
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        val image = composeRule.onNodeWithTag(WIND_VIEW).captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)

        assertTrue(
            "Wind forecast should draw the CCL overlay on top of the wind-speed background.",
            pixels.countNearColor(target = Color.rgb(0xFF, 0x8C, 0x00), tolerance = 2) > 100,
        )
        assertTrue(
            "Wind forecast should draw the freezing-level overlay on top of the wind-speed background.",
            pixels.countNearColor(target = Color.rgb(0x00, 0xBC, 0xD4), tolerance = 2) > 100,
        )
        assertTrue(
            "Wind forecast should keep the wind-speed cell colors visible behind overlays.",
            pixels.countWindSpeedBackgroundColors() > pixels.size / 20,
        )
    }
}

private fun IntArray.countNearColor(target: Int, tolerance: Int): Int {
    val targetRed = Color.red(target)
    val targetGreen = Color.green(target)
    val targetBlue = Color.blue(target)
    return count { color ->
        abs(Color.red(color) - targetRed) <= tolerance &&
            abs(Color.green(color) - targetGreen) <= tolerance &&
            abs(Color.blue(color) - targetBlue) <= tolerance
    }
}

private fun IntArray.countWindSpeedBackgroundColors(): Int {
    return count { color ->
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        green > 70 && green > blue + 20 && red > 35
    }
}
