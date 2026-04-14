package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.remote.HourlyForecastData
import com.cloudbasepredictor.data.remote.HourlyPoint
import com.cloudbasepredictor.data.remote.PressureLevelPoint
import com.cloudbasepredictor.model.DailyForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermicChartPressureLevelBoundariesTest {

    /**
     * The thermic chart builder should use actual pressure-level geopotential heights
     * as cell boundaries, not fixed-step altitudes.
     */
    @Test
    fun cellBoundaries_matchPressureLevelHeights() {
        val elevationM = 500.0

        // Pressure levels with known geopotential heights
        val pressureLevels = listOf(
            PressureLevelPoint(
                pressureHpa = 950,
                temperatureC = 18.0,
                dewPointC = 8.0,
                windSpeedKmh = 10.0,
                windDirectionDeg = 270.0,
                geopotentialHeightM = 600.0,
            ),
            PressureLevelPoint(
                pressureHpa = 900,
                temperatureC = 14.0,
                dewPointC = 4.0,
                windSpeedKmh = 15.0,
                windDirectionDeg = 280.0,
                geopotentialHeightM = 1000.0,
            ),
            PressureLevelPoint(
                pressureHpa = 850,
                temperatureC = 10.0,
                dewPointC = 1.0,
                windSpeedKmh = 20.0,
                windDirectionDeg = 290.0,
                geopotentialHeightM = 1500.0,
            ),
            PressureLevelPoint(
                pressureHpa = 800,
                temperatureC = 6.0,
                dewPointC = -2.0,
                windSpeedKmh = 25.0,
                windDirectionDeg = 300.0,
                geopotentialHeightM = 2000.0,
            ),
            PressureLevelPoint(
                pressureHpa = 700,
                temperatureC = -1.0,
                dewPointC = -10.0,
                windSpeedKmh = 30.0,
                windDirectionDeg = 310.0,
                geopotentialHeightM = 3000.0,
            ),
            PressureLevelPoint(
                pressureHpa = 600,
                temperatureC = -8.0,
                dewPointC = -18.0,
                windSpeedKmh = 35.0,
                windDirectionDeg = 320.0,
                geopotentialHeightM = 4200.0,
            ),
        )

        val hourlyData = HourlyForecastData(
            latitude = 46.7,
            longitude = 7.8,
            elevation = elevationM,
            hourlyPoints = listOf(
                HourlyPoint(
                    date = "2024-05-15",
                    hour = 12,
                    temperature2mC = 22.0,
                    dewPoint2mC = 10.0,
                    cloudCoverLowPercent = 10.0,
                    cloudCoverMidPercent = 5.0,
                    cloudCoverHighPercent = 0.0,
                    precipitationMm = 0.0,
                    precipitationProbabilityPercent = 10.0,
                    windSpeed10mKmh = 8.0,
                    windDirection10mDeg = 270.0,
                    capeJKg = 800.0,
                    freezingLevelHeightM = 3500.0,
                    pressureLevels = pressureLevels,
                ),
            ),
            dailyForecasts = listOf(
                DailyForecast(
                    date = "2024-05-15",
                    maxTemperatureCelsius = 24.0,
                    minTemperatureCelsius = 12.0,
                    weatherCode = 1,
                ),
            ),
        )

        val chart = buildThermicChartFromData(hourlyData, dayIndex = 0)

        assertTrue("Expected cells to be generated", chart.cells.isNotEmpty())

        // Verify that cell boundaries correspond to pressure-level heights in km
        val expectedHeightsKm = pressureLevels
            .map { (it.geopotentialHeightM!! / 1000f).toFloat() }
            .filter { it >= elevationM.toFloat() / 1000f }
            .sorted()

        val cellBoundaries = chart.cells
            .flatMap { listOf(it.startAltitudeKm, it.endAltitudeKm) }
            .distinct()
            .sorted()

        // Each cell boundary (except possibly elevation and thermal top) should
        // correspond to a pressure-level height
        for (boundary in cellBoundaries) {
            val matchesPressureLevel = expectedHeightsKm.any {
                kotlin.math.abs(it - boundary) < 0.02f
            }
            val isElevation = kotlin.math.abs(boundary - elevationM.toFloat() / 1000f) < 0.02f
            assertTrue(
                "Cell boundary $boundary km should correspond to a pressure level or elevation",
                matchesPressureLevel || isElevation,
            )
        }

        // Verify cells are non-overlapping and contiguous
        val cellsByTime = chart.cells.groupBy { it.startMinuteOfDayLocal }
        cellsByTime.values.forEach { timeCells ->
            val sorted = timeCells.sortedBy { it.startAltitudeKm }
            for (i in 0 until sorted.size - 1) {
                assertEquals(
                    "Cell end should equal next cell start",
                    sorted[i].endAltitudeKm,
                    sorted[i + 1].startAltitudeKm,
                    0.001f,
                )
            }
        }
    }

    @Test
    fun noPressureLevels_producesPlaceholderChart() {
        val hourlyData = HourlyForecastData(
            latitude = 46.7,
            longitude = 7.8,
            elevation = 500.0,
            hourlyPoints = listOf(
                HourlyPoint(
                    date = "2024-05-15",
                    hour = 12,
                    temperature2mC = 22.0,
                    dewPoint2mC = 10.0,
                    cloudCoverLowPercent = 10.0,
                    cloudCoverMidPercent = 5.0,
                    cloudCoverHighPercent = 0.0,
                    precipitationMm = 0.0,
                    precipitationProbabilityPercent = 10.0,
                    windSpeed10mKmh = 8.0,
                    windDirection10mDeg = 270.0,
                    capeJKg = 800.0,
                    freezingLevelHeightM = 3500.0,
                    pressureLevels = emptyList(),
                ),
            ),
            dailyForecasts = listOf(
                DailyForecast(
                    date = "2024-05-15",
                    maxTemperatureCelsius = 24.0,
                    minTemperatureCelsius = 12.0,
                    weatherCode = 1,
                ),
            ),
        )

        val chart = buildThermicChartFromData(hourlyData, dayIndex = 0)
        // With no pressure levels, no thermals are computed, so the chart may be a placeholder
        // or have no cells. Either way, it should not crash.
        assertTrue(
            "Chart should have cells (placeholder) or be empty",
            chart.cells.isNotEmpty() || chart.timeSlots.isNotEmpty(),
        )
    }
}
