package com.cloudbasepredictor.data.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastLoadPolicyTest {
    @Test
    fun requestedForecastDaysForDayIndex_loadsInTwoDayBatches() {
        // First batch covers INITIAL_FORECAST_DAYS (5) rounded up to batch boundary → 6
        assertEquals(6, requestedForecastDaysForDayIndex(dayIndex = 0))
        assertEquals(6, requestedForecastDaysForDayIndex(dayIndex = 1))
        assertEquals(6, requestedForecastDaysForDayIndex(dayIndex = 2))
        assertEquals(6, requestedForecastDaysForDayIndex(dayIndex = 5))
        // Beyond initial window, continues in 2-day batches
        assertEquals(8, requestedForecastDaysForDayIndex(dayIndex = 6))
        assertEquals(8, requestedForecastDaysForDayIndex(dayIndex = 7))
        assertEquals(10, requestedForecastDaysForDayIndex(dayIndex = 8))
    }

    @Test
    fun requestedForecastDaysForDayIndex_clampsAtVisibleForecastHorizon() {
        assertEquals(MAX_FORECAST_DAYS, requestedForecastDaysForDayIndex(dayIndex = 13))
        assertEquals(MAX_FORECAST_DAYS, requestedForecastDaysForDayIndex(dayIndex = 20))
    }

    @Test
    fun exposedForecastDayCount_revealsNextBatchAheadOfCachedData() {
        assertEquals(6, exposedForecastDayCount(loadedForecastDays = 5, selectedDayIndex = 0))
        assertEquals(8, exposedForecastDayCount(loadedForecastDays = 6, selectedDayIndex = 0))
        assertEquals(10, exposedForecastDayCount(loadedForecastDays = 8, selectedDayIndex = 0))
        assertEquals(MAX_FORECAST_DAYS, exposedForecastDayCount(loadedForecastDays = 12, selectedDayIndex = 0))
    }

    @Test
    fun exposedForecastDayCount_expandsToCoverSelectedFutureDay() {
        assertEquals(6, exposedForecastDayCount(loadedForecastDays = 5, selectedDayIndex = 5))
        assertEquals(8, exposedForecastDayCount(loadedForecastDays = 5, selectedDayIndex = 6))
    }

    @Test
    fun exposedForecastDayCount_respectsModelSpecificHorizonCaps() {
        assertEquals(
            2,
            exposedForecastDayCount(
                loadedForecastDays = 2,
                selectedDayIndex = 3,
                maxForecastDays = 2,
            ),
        )
    }

    @Test
    fun dayChipWindow_shows14DaysAndInitiallyExposesAtLeast5() {
        assertEquals(14, MAX_FORECAST_DAYS)
        // With nothing loaded yet, initial window = INITIAL_FORECAST_DAYS (5)
        val initial = exposedForecastDayCount(loadedForecastDays = 0, selectedDayIndex = 0)
        assert(initial >= 5) { "Initially should show at least 5 day chips, got $initial" }
        // Fully loaded shows all 14
        assertEquals(
            14,
            exposedForecastDayCount(loadedForecastDays = 14, selectedDayIndex = 0),
        )
    }
}
