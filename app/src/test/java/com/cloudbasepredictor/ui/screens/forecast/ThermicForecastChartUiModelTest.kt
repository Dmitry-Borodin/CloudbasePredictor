package com.cloudbasepredictor.ui.screens.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ThermicForecastChartUiModelTest {

    @Test
    fun aggregatedForDisplay_averagesStrengthValuesInsideDisplayBucket() {
        val chart = ThermicForecastChartUiModel(
            timeSlots = listOf(360, 375),
            cells = listOf(
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 360,
                    startAltitudeKm = 0f,
                    endAltitudeKm = 0.05f,
                    strengthMps = 1f,
                ),
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 360,
                    startAltitudeKm = 0.05f,
                    endAltitudeKm = 0.1f,
                    strengthMps = 3f,
                ),
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 375,
                    startAltitudeKm = 0f,
                    endAltitudeKm = 0.05f,
                    strengthMps = 1f,
                ),
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 375,
                    startAltitudeKm = 0.05f,
                    endAltitudeKm = 0.1f,
                    strengthMps = 3f,
                ),
            ),
            cloudMarkers = emptyList(),
        )

        val aggregated = chart.aggregatedForDisplay(
            timeBucketSlotCount = 2,
            altitudeBucketStepKm = 0.1f,
        )

        assertEquals(listOf(360), aggregated.timeSlots)
        assertEquals(1, aggregated.cells.size)
        assertEquals(2f, aggregated.cells.single().strengthMps, 0.0001f)
    }

    @Test
    fun aggregatedForDisplay_preservesThermalTopAndCloudHeights() {
        val chart = ThermicForecastChartUiModel(
            timeSlots = listOf(360, 375),
            cells = listOf(
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 360,
                    startAltitudeKm = 0f,
                    endAltitudeKm = 0.05f,
                    strengthMps = 1f,
                ),
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 360,
                    startAltitudeKm = 0.05f,
                    endAltitudeKm = 0.15f,
                    strengthMps = 2f,
                ),
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 375,
                    startAltitudeKm = 0f,
                    endAltitudeKm = 0.05f,
                    strengthMps = 1.5f,
                ),
                ThermicForecastCellUiModel(
                    startMinuteOfDayLocal = 375,
                    startAltitudeKm = 0.05f,
                    endAltitudeKm = 0.15f,
                    strengthMps = 2.5f,
                ),
            ),
            cloudMarkers = listOf(
                ThermicForecastCloudMarkerUiModel(
                    startMinuteOfDayLocal = 360,
                    altitudeKm = 1.2f,
                ),
                ThermicForecastCloudMarkerUiModel(
                    startMinuteOfDayLocal = 375,
                    altitudeKm = 1.6f,
                ),
            ),
        )

        val aggregated = chart.aggregatedForDisplay(
            timeBucketSlotCount = 2,
            altitudeBucketStepKm = 0.1f,
        )

        assertEquals(2, aggregated.cells.size)
        assertEquals(0.15f, aggregated.cells.last().endAltitudeKm, 0.0001f)
        assertEquals(listOf(1.2f, 1.6f), aggregated.cloudMarkers.map { it.altitudeKm })
    }

    @Test
    fun visibleSegment_clipsCellAtCloudBaseClearance() {
        val cell = ThermicForecastCellUiModel(
            startMinuteOfDayLocal = 900,
            startAltitudeKm = 2.0f,
            endAltitudeKm = 3.1f,
            strengthMps = 4.0f,
        )

        val visibleSegment = cell.visibleSegment(
            minAltitudeKm = 1.1f,
            maxAltitudeKm = 5.5f,
            cloudBaseKm = 2.7f,
        )

        assertNotNull(visibleSegment)
        visibleSegment!!
        assertEquals(2.0f, visibleSegment.startAltitudeKm, 0.0001f)
        assertEquals(2.65f, visibleSegment.endAltitudeKm, 0.0001f)
    }

    @Test
    fun visibleSegment_hidesCellEntirelyAboveCloudBaseClearance() {
        val cell = ThermicForecastCellUiModel(
            startMinuteOfDayLocal = 900,
            startAltitudeKm = 2.7f,
            endAltitudeKm = 3.1f,
            strengthMps = 4.0f,
        )

        val visibleSegment = cell.visibleSegment(
            minAltitudeKm = 1.1f,
            maxAltitudeKm = 5.5f,
            cloudBaseKm = 2.7f,
        )

        assertNull(visibleSegment)
    }
}
