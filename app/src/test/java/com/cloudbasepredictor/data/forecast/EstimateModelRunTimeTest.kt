package com.cloudbasepredictor.data.forecast

import com.cloudbasepredictor.model.ForecastModel
import org.junit.Assert.assertEquals
import org.junit.Test

class EstimateModelRunTimeTest {

    @Test
    fun exactMultipleOfInterval_returnsUnchanged() {
        val threeHoursMs = 3 * 3_600_000L
        val fetchTime = threeHoursMs * 5 // exactly 15 hours
        val result = estimateModelRunTimeInternal(fetchTime, ForecastModel.ICON_D2)
        assertEquals(fetchTime, result)
    }

    @Test
    fun midwayBetweenIntervals_roundsDown() {
        val threeHoursMs = 3 * 3_600_000L
        val fetchTime = threeHoursMs * 5 + threeHoursMs / 2 // 16.5 hours
        val result = estimateModelRunTimeInternal(fetchTime, ForecastModel.ICON_D2)
        assertEquals(threeHoursMs * 5, result)
    }

    @Test
    fun justBeforeNextInterval_roundsDown() {
        val threeHoursMs = 3 * 3_600_000L
        val fetchTime = threeHoursMs * 6 - 1 // 1 ms before 18 hours
        val result = estimateModelRunTimeInternal(fetchTime, ForecastModel.ICON_D2)
        assertEquals(threeHoursMs * 5, result)
    }

    @Test
    fun sixHourIntervalModel_roundsCorrectly() {
        val sixHoursMs = 6 * 3_600_000L
        // 2024-05-15 14:30 UTC → should round to 12:00 UTC
        val fetchTime = sixHoursMs * 2 + 2 * 3_600_000L + 30 * 60_000L
        val result = estimateModelRunTimeInternal(fetchTime, ForecastModel.ECMWF_IFS)
        assertEquals(sixHoursMs * 2, result)
    }

    @Test
    fun realisticTimestamp_roundsToModelRun() {
        // 2024-05-15 08:45 UTC as millis from epoch
        val fetchTime = 1_715_758_800_000L + 45 * 60_000L // approx 08:45 UTC
        val threeHoursMs = 3 * 3_600_000L
        val expected = (fetchTime / threeHoursMs) * threeHoursMs
        val result = estimateModelRunTimeInternal(fetchTime, ForecastModel.ICON_D2)
        assertEquals(expected, result)
    }

    @Test
    fun zeroFetchTime_returnsZero() {
        val result = estimateModelRunTimeInternal(0L, ForecastModel.BEST_MATCH)
        assertEquals(0L, result)
    }
}
