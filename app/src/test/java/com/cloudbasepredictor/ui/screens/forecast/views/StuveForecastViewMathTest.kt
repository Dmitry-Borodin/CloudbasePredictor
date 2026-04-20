package com.cloudbasepredictor.ui.screens.forecast.views

import com.cloudbasepredictor.ui.screens.forecast.StuveForecastChartUiModel
import com.cloudbasepredictor.ui.screens.forecast.StuveProfilePoint
import org.junit.Assert.assertTrue
import org.junit.Test

class StuveForecastViewMathTest {

    @Test
    fun buildVisibleTemperatureAxisRange_ignores_upperLevel_cold_outliers_for_initial_view() {
        val chart = sampleChart(
            temperaturePoints = listOf(
                point(850f, 14f),
                point(800f, 12f),
                point(700f, 7f),
                point(600f, 2f),
                point(500f, -2f),
                point(400f, -8f),
                point(300f, -15f),
                point(250f, -20f),
            ),
            dewpointPoints = listOf(
                point(850f, 10f),
                point(800f, 7f),
                point(700f, 1f),
                point(600f, -6f),
                point(500f, -14f),
                point(400f, -25f),
                point(300f, -39f),
                point(250f, -48f),
            ),
        )

        val range = buildVisibleTemperatureAxisRange(
            chart = chart,
            topPressure = 250f,
            bottomPressure = 870f,
        )

        assertTrue(range.minC in -20f..0f)
        assertTrue(range.maxC in 20f..40f)
        assertTrue(range.maxC - range.minC <= 50f)
    }

    @Test
    fun buildVisibleTemperatureAxisRange_zoomed_upper_view_tracks_current_slice() {
        val chart = sampleChart(
            temperaturePoints = listOf(
                point(850f, 14f),
                point(700f, 7f),
                point(600f, 2f),
                point(500f, -2f),
                point(400f, -8f),
                point(300f, -15f),
                point(250f, -20f),
            ),
            dewpointPoints = listOf(
                point(850f, 10f),
                point(700f, 1f),
                point(600f, -6f),
                point(500f, -14f),
                point(400f, -25f),
                point(300f, -30f),
                point(250f, -33f),
            ),
        )

        val range = buildVisibleTemperatureAxisRange(
            chart = chart,
            topPressure = 500f,
            bottomPressure = 700f,
        )

        assertTrue(range.minC in -30f..0f)
        assertTrue(range.maxC in 10f..30f)
    }

    private fun sampleChart(
        temperaturePoints: List<StuveProfilePoint>,
        dewpointPoints: List<StuveProfilePoint>,
    ) = StuveForecastChartUiModel(
        pressureLevels = listOf(850f, 800f, 700f, 600f, 500f, 400f, 300f, 250f),
        temperatureProfile = temperaturePoints,
        dewpointProfile = dewpointPoints,
        parcelAscentPath = temperaturePoints.map { it.copy(temperatureC = it.temperatureC + 1.5f) },
        windBarbs = emptyList(),
        lclPressureHpa = 720f,
        cclPressureHpa = 760f,
        tconC = 18f,
        selectedHour = 12,
        surfacePressureHpa = 850f,
    )

    private fun point(pressureHpa: Float, temperatureC: Float) =
        StuveProfilePoint(
            pressureHpa = pressureHpa,
            temperatureC = temperatureC,
            isRealData = true,
        )
}
