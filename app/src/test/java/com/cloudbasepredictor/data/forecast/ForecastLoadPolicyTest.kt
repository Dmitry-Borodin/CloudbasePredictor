package com.cloudbasepredictor.data.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastLoadPolicyTest {
    @Test
    fun requestedForecastDaysForDayIndex_loadsInTwoDayBatches() {
        assertEquals(2, requestedForecastDaysForDayIndex(dayIndex = 0))
        assertEquals(2, requestedForecastDaysForDayIndex(dayIndex = 1))
        assertEquals(4, requestedForecastDaysForDayIndex(dayIndex = 2))
        assertEquals(4, requestedForecastDaysForDayIndex(dayIndex = 3))
        assertEquals(6, requestedForecastDaysForDayIndex(dayIndex = 4))
        assertEquals(6, requestedForecastDaysForDayIndex(dayIndex = 5))
    }

    @Test
    fun requestedForecastDaysForDayIndex_clampsAtVisibleForecastHorizon() {
        assertEquals(MAX_FORECAST_DAYS, requestedForecastDaysForDayIndex(dayIndex = 6))
        assertEquals(MAX_FORECAST_DAYS, requestedForecastDaysForDayIndex(dayIndex = 10))
    }

    @Test
    fun exposedForecastDayCount_revealsNextBatchAheadOfCachedData() {
        assertEquals(4, exposedForecastDayCount(loadedForecastDays = 2, selectedDayIndex = 0))
        assertEquals(6, exposedForecastDayCount(loadedForecastDays = 4, selectedDayIndex = 0))
        assertEquals(MAX_FORECAST_DAYS, exposedForecastDayCount(loadedForecastDays = 6, selectedDayIndex = 0))
    }

    @Test
    fun exposedForecastDayCount_expandsToCoverSelectedFutureDay() {
        assertEquals(6, exposedForecastDayCount(loadedForecastDays = 2, selectedDayIndex = 5))
        assertEquals(MAX_FORECAST_DAYS, exposedForecastDayCount(loadedForecastDays = 4, selectedDayIndex = 6))
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
}
