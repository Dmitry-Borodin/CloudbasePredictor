package com.cloudbasepredictor.domain.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastDayViewDataTest {

    @Test
    fun validForecastDayViewData_acceptsQuarterHourTimelineAndSurfaceLayer() {
        val timeSlots = listOf(
            ForecastTimeSlot(startMinuteOfDayLocal = 0),
            ForecastTimeSlot(startMinuteOfDayLocal = 15),
            ForecastTimeSlot(startMinuteOfDayLocal = 30),
        )

        val dayViewData = ForecastDayViewData(
            localDate = "2026-04-12",
            timeSlots = timeSlots,
            surfaceLayer = ForecastSurfaceLayerViewData(
                samples = List(timeSlots.size) {
                    ForecastSlotValuesViewData(
                        values = listOf(
                            ForecastMetricValueViewData(
                                metricId = ForecastMetricId("surface_temperature_c"),
                                value = 12.5 + it,
                                unitLabel = "°C",
                            ),
                        ),
                    )
                },
            ),
            altitudeLayers = listOf(
                ForecastAltitudeLayerViewData(
                    altitudeMetersAgl = 50,
                    samples = List(timeSlots.size) {
                        ForecastSlotValuesViewData(
                            values = listOf(
                                ForecastMetricValueViewData(
                                    metricId = ForecastMetricId("thermal_strength_mps"),
                                    value = 0.5 + it,
                                    unitLabel = "m/s",
                                ),
                            ),
                        )
                    },
                ),
                ForecastAltitudeLayerViewData(
                    altitudeMetersAgl = 100,
                    samples = List(timeSlots.size) {
                        ForecastSlotValuesViewData(
                            values = listOf(
                                ForecastMetricValueViewData(
                                    metricId = ForecastMetricId("thermal_strength_mps"),
                                    value = 0.25 + it,
                                    unitLabel = "m/s",
                                ),
                            ),
                        )
                    },
                ),
            ),
        )

        assertEquals(3, dayViewData.timeSlots.size)
        assertEquals(2, dayViewData.altitudeLayers.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun forecastTimeSlot_rejectsNonQuarterHourValues() {
        ForecastTimeSlot(startMinuteOfDayLocal = 7)
    }

    @Test(expected = IllegalArgumentException::class)
    fun forecastDayViewData_rejectsAltitudeLayersWithWrongStep() {
        val timeSlots = listOf(
            ForecastTimeSlot(startMinuteOfDayLocal = 0),
            ForecastTimeSlot(startMinuteOfDayLocal = 15),
        )

        ForecastDayViewData(
            localDate = "2026-04-12",
            timeSlots = timeSlots,
            surfaceLayer = ForecastSurfaceLayerViewData(
                samples = List(timeSlots.size) { emptySlotValues() },
            ),
            altitudeLayers = listOf(
                ForecastAltitudeLayerViewData(
                    altitudeMetersAgl = 50,
                    samples = List(timeSlots.size) { emptySlotValues() },
                ),
                ForecastAltitudeLayerViewData(
                    altitudeMetersAgl = 150,
                    samples = List(timeSlots.size) { emptySlotValues() },
                ),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun forecastDayViewData_rejectsSurfaceSampleCountMismatch() {
        ForecastDayViewData(
            localDate = "2026-04-12",
            timeSlots = listOf(
                ForecastTimeSlot(startMinuteOfDayLocal = 0),
                ForecastTimeSlot(startMinuteOfDayLocal = 15),
            ),
            surfaceLayer = ForecastSurfaceLayerViewData(
                samples = listOf(emptySlotValues()),
            ),
            altitudeLayers = emptyList(),
        )
    }

    private fun emptySlotValues(): ForecastSlotValuesViewData {
        return ForecastSlotValuesViewData(values = emptyList())
    }
}
