package com.cloudbasepredictor.screenshot

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.testutil.SimulatedTestData
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastScreen
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Screenshot capture tests — render screens with simulated Brauneck data and save PNG screenshots.
 *
 * These tests are NOT part of the regular test suite. Run them explicitly:
 *   ./gradlew :app:connectedDebugAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.class=com.cloudbasepredictor.screenshot.ScreenshotCaptureTest
 *
 * Screenshots are saved to /sdcard/Pictures/CloudbaseScreenshots/ on the device.
 * Pull them with:
 *   adb pull /sdcard/Pictures/CloudbaseScreenshots/ app/screenshots/
 */
class ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun simulatedState(mode: ForecastMode): ForecastUiState {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return SimulatedTestData.forecastUiState(context, mode = mode)
    }

    @Test
    fun captureThermicForecast() {
        captureScreen("forecast_thermic") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.THERMIC),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureStuveForecast() {
        captureScreen("forecast_stuve") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.STUVE),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureWindForecast() {
        captureScreen("forecast_wind") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureCloudForecast() {
        captureScreen("forecast_cloud") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.CLOUD),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureForecastLoading() {
        captureScreen("forecast_loading") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastLoadingUiState.copy(
                        selectedPlace = SimulatedTestData.brauneckPlace,
                    ),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureForecastError() {
        captureScreen("forecast_error") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = PreviewData.forecastErrorUiState.copy(
                        selectedPlace = SimulatedTestData.brauneckPlace,
                    ),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    // ── Raised map variants ───────────────────────────

    @Test
    fun captureThermicForecastWithMap() {
        captureScreen("forecast_thermic_map") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.THERMIC),
                    onDateSelected = {},
                    onOpenMap = {},
                    initiallyExpandedMap = true,
                )
            }
        }
    }

    @Test
    fun captureWindForecastWithMap() {
        captureScreen("forecast_wind_map") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                    initiallyExpandedMap = true,
                )
            }
        }
    }

    @Test
    fun captureCloudForecastWithMap() {
        captureScreen("forecast_cloud_map") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.CLOUD),
                    onDateSelected = {},
                    onOpenMap = {},
                    initiallyExpandedMap = true,
                )
            }
        }
    }

    @Test
    fun captureStuveForecastWithMap() {
        captureScreen("forecast_stuve_map") {
            CloudbasePredictorTheme {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.STUVE),
                    onDateSelected = {},
                    onOpenMap = {},
                    initiallyExpandedMap = true,
                )
            }
        }
    }

    // ── Dark theme variants ─────────────────────────────

    @Test
    fun captureThermicForecastDark() {
        captureScreen("forecast_thermic_dark") {
            CloudbasePredictorTheme(darkTheme = true) {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.THERMIC),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureStuveForecastDark() {
        captureScreen("forecast_stuve_dark") {
            CloudbasePredictorTheme(darkTheme = true) {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.STUVE),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureWindForecastDark() {
        captureScreen("forecast_wind_dark") {
            CloudbasePredictorTheme(darkTheme = true) {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.WIND),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    @Test
    fun captureCloudForecastDark() {
        captureScreen("forecast_cloud_dark") {
            CloudbasePredictorTheme(darkTheme = true) {
                ForecastScreen(
                    uiState = simulatedState(ForecastMode.CLOUD),
                    onDateSelected = {},
                    onOpenMap = {},
                )
            }
        }
    }

    private fun captureScreen(name: String, content: @Composable () -> Unit) {
        composeRule.setContent(content)
        composeRule.waitForIdle()

        val bitmap = composeRule.onRoot().captureToImage()
            .let { imageBitmap ->
                val androidBitmap = Bitmap.createBitmap(
                    imageBitmap.width,
                    imageBitmap.height,
                    Bitmap.Config.ARGB_8888,
                )
                val buffer = IntArray(imageBitmap.width * imageBitmap.height)
                imageBitmap.readPixels(buffer)
                androidBitmap.setPixels(
                    buffer, 0, imageBitmap.width,
                    0, 0, imageBitmap.width, imageBitmap.height,
                )
                androidBitmap
            }

        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "CloudbaseScreenshots",
        )
        dir.mkdirs()
        val file = File(dir, "$name.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
