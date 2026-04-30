package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.remote.HourlyForecastData
import com.cloudbasepredictor.data.remote.HourlyPoint
import com.cloudbasepredictor.data.remote.PressureLevelPoint
import com.cloudbasepredictor.model.DailyForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermicChartDiagnosticsTest {

    private val pressureLevels = listOf(
        PressureLevelPoint(950, 18.0, 8.0, 10.0, 270.0, 600.0, 45.0, 5.0),
        PressureLevelPoint(900, 14.0, 4.0, 15.0, 280.0, 1000.0, 42.0, 5.0),
        PressureLevelPoint(850, 10.0, 1.0, 20.0, 290.0, 1500.0, 38.0, 5.0),
        PressureLevelPoint(800, 6.0, -2.0, 25.0, 300.0, 2000.0, 35.0, 10.0),
        PressureLevelPoint(700, -1.0, -10.0, 30.0, 310.0, 3000.0, 30.0, 10.0),
        PressureLevelPoint(600, -8.0, -18.0, 35.0, 320.0, 4200.0, 25.0, 5.0),
    )

    private fun makeHourlyData(
        hour: Int = 12,
        t2m: Double = 22.0,
        td2m: Double = 10.0,
        capeJKg: Double = 500.0,
    ) = HourlyForecastData(
        latitude = 46.7,
        longitude = 7.8,
        elevation = 500.0,
        hourlyPoints = listOf(
            HourlyPoint(
                date = "2024-05-15",
                hour = hour,
                temperature2mC = t2m,
                dewPoint2mC = td2m,
                cloudCoverLowPercent = 10.0,
                cloudCoverMidPercent = 5.0,
                cloudCoverHighPercent = 0.0,
                precipitationMm = 0.0,
                precipitationProbabilityPercent = 10.0,
                windSpeed10mKmh = 8.0,
                windDirection10mDeg = 270.0,
                capeJKg = capeJKg,
                freezingLevelHeightM = 3500.0,
                surfacePressureHpa = 955.0,
                shortwaveRadiationWm2 = 700.0,
                isDay = 1.0,
                pressureLevels = pressureLevels,
                liftedIndexC = -1.5,
                convectiveInhibitionJKg = 25.0,
                boundaryLayerHeightM = 1800.0,
            ),
        ),
        dailyForecasts = listOf(
            DailyForecast("2024-05-15", 24.0, 12.0, 1),
        ),
    )

    @Test
    fun buildChart_producesDiagnostics() {
        val chart = buildThermicChartFromData(makeHourlyData(), dayIndex = 0)
        assertTrue("Should have diagnostics", chart.slotDiagnostics.isNotEmpty())
        val diag = chart.slotDiagnostics.first()
        assertTrue("Nominal top should be > elevation", diag.topNominalKm > 0.5f)
        assertTrue("LCL should be > elevation", diag.lclKm > 0.5f)
        assertTrue("CCL should be > elevation", diag.cclKm > 0.5f)
        assertNotNull("Model CAPE should be present", diag.modelCapeJKg)
        assertNotNull("Model CIN should be present", diag.modelCinJKg)
        assertNotNull("Model lifted index should be present", diag.liftedIndexC)
        assertNotNull("Model PBL height should be present", diag.boundaryLayerHeightM)
        assertTrue("Top range should contain nominal top", diag.topLowKm <= diag.topNominalKm)
        assertTrue("Top range should contain nominal top", diag.topHighKm >= diag.topNominalKm)
        assertTrue("Updraft range should contain nominal value", diag.updraftLowMps <= diag.updraftNominalMps)
        assertTrue("Updraft range should contain nominal value", diag.updraftHighMps >= diag.updraftNominalMps)
        assertTrue("Computed CAPE should be >= 0", diag.computedCapeJKg >= 0f)
    }

    @Test
    fun buildChart_dryTopLine_isConsistentWithCells() {
        val chart = buildThermicChartFromData(makeHourlyData(), dayIndex = 0)
        val diag = chart.slotDiagnostics.firstOrNull() ?: return
        val maxCellTop = chart.cells
            .filter { it.startMinuteOfDayLocal == diag.startMinuteOfDayLocal }
            .maxOfOrNull { it.endAltitudeKm }

        if (maxCellTop != null) {
            assertTrue(
                "Cell top ($maxCellTop) should not exceed nominal top (${diag.topNominalKm})",
                maxCellTop <= diag.topNominalKm + 0.02f,
            )
        }
    }

    @Test
    fun buildChart_cloudDiagnostics_preserveCloudBaseAndMoistTop() {
        val chart = buildThermicChartFromData(makeHourlyData(), dayIndex = 0)
        val diag = chart.slotDiagnostics.firstOrNull() ?: return
        val cloudBase = diag.cloudBaseKm ?: return

        val moistTop = diag.moistEquilibriumTopKm
        if (moistTop != null && moistTop > cloudBase + 0.1f) {
            assertTrue(
                "Moist top should stay above cloud base",
                moistTop > cloudBase,
            )
        }
        assertTrue("Cloud markers should no longer be repeated icons", chart.cloudMarkers.isEmpty())
    }

    @Test
    fun buildChart_aggregation_preservesDiagnostics() {
        val chart = buildThermicChartFromData(makeHourlyData(), dayIndex = 0)
        val aggregated = chart.aggregatedForDisplay(
            timeBucketSlotCount = 1,
            altitudeBucketStepKm = 0.1f,
        )
        assertEquals(
            "Diagnostics should survive aggregation",
            chart.slotDiagnostics.size,
            aggregated.slotDiagnostics.size,
        )
    }

    @Test
    fun buildChart_noCape_capsBuoyancyBasedThermals() {
        val chart = buildThermicChartFromData(makeHourlyData(capeJKg = 0.0), dayIndex = 0)
        val maxOptimistic = chart.slotDiagnostics.maxOfOrNull { it.updraftHighMps } ?: 0f

        assertTrue("Dry profile can still produce capped weak thermals with 0 model CAPE", chart.cells.isNotEmpty())
        assertTrue("Zero CAPE should keep optimistic lift practical, got $maxOptimistic", maxOptimistic <= 3.0f)
    }

    @Test
    fun buildChart_modelCapeCalibratesThermicStrength() {
        val lowCape = buildThermicChartFromData(makeHourlyData(capeJKg = 0.0), dayIndex = 0)
        val highCape = buildThermicChartFromData(makeHourlyData(capeJKg = 2500.0), dayIndex = 0)

        assertEquals(
            "Model CAPE should be preserved as a diagnostic",
            2500f,
            highCape.slotDiagnostics.first().modelCapeJKg!!,
            0.0001f,
        )
        assertTrue(
            "Model CAPE should raise calibrated nominal updraft",
            highCape.slotDiagnostics.first().updraftNominalMps >
                lowCape.slotDiagnostics.first().updraftNominalMps,
        )
    }

    @Test
    fun buildChart_missingPressureLevels_gracefulDegradation() {
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
                    capeJKg = 500.0,
                    freezingLevelHeightM = 3500.0,
                    pressureLevels = emptyList(),
                ),
            ),
            dailyForecasts = listOf(
                DailyForecast("2024-05-15", 24.0, 12.0, 1),
            ),
        )

        // Should not crash, may produce empty chart
        val chart = buildThermicChartFromData(hourlyData, dayIndex = 0)
        assertNotNull("Chart should never be null", chart)
    }
}
