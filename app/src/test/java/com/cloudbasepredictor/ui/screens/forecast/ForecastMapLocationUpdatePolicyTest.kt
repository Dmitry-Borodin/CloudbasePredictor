package com.cloudbasepredictor.ui.screens.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastMapLocationUpdatePolicyTest {
    @Test
    fun updateDecision_allowsFirstLocationUpdate() {
        val decision = forecastMapLocationUpdateDecision(
            nowMs = 10_000L,
            lastUpdateTimeMs = 0L,
            lastLocation = null,
            candidate = ForecastMapLocation(latitude = 47.2692, longitude = 11.4041),
        )

        assertEquals(ForecastMapLocationUpdateDecision.UPDATE, decision)
    }

    @Test
    fun updateDecision_blocksUpdatesWithinRateLimitWindow() {
        val decision = forecastMapLocationUpdateDecision(
            nowMs = 12_000L,
            lastUpdateTimeMs = 10_000L,
            lastLocation = ForecastMapLocation(latitude = 47.2692, longitude = 11.4041),
            candidate = ForecastMapLocation(latitude = 47.30, longitude = 11.45),
            rateLimitMs = 3_000L,
            minDistanceMeters = 200.0,
        )

        assertEquals(ForecastMapLocationUpdateDecision.TOO_SOON, decision)
    }

    @Test
    fun updateDecision_blocksLocationsTooCloseAfterRateLimitWindow() {
        val decision = forecastMapLocationUpdateDecision(
            nowMs = 14_000L,
            lastUpdateTimeMs = 10_000L,
            lastLocation = ForecastMapLocation(latitude = 47.2692, longitude = 11.4041),
            candidate = ForecastMapLocation(latitude = 47.2700, longitude = 11.4041),
            rateLimitMs = 3_000L,
            minDistanceMeters = 200.0,
        )

        assertEquals(ForecastMapLocationUpdateDecision.TOO_CLOSE, decision)
    }

    @Test
    fun updateDecision_allowsFarLocationAfterRateLimitWindow() {
        val decision = forecastMapLocationUpdateDecision(
            nowMs = 14_000L,
            lastUpdateTimeMs = 10_000L,
            lastLocation = ForecastMapLocation(latitude = 47.2692, longitude = 11.4041),
            candidate = ForecastMapLocation(latitude = 47.30, longitude = 11.45),
            rateLimitMs = 3_000L,
            minDistanceMeters = 200.0,
        )

        assertEquals(ForecastMapLocationUpdateDecision.UPDATE, decision)
    }
}
