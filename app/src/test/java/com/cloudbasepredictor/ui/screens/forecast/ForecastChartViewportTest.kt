package com.cloudbasepredictor.ui.screens.forecast

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastChartViewportTest {
    @Test
    fun zoomedTopAltitudeKm_clampsZoomOutAtSevenKilometers() {
        val zoomedOutTop = zoomedTopAltitudeKm(
            currentTopAltitudeKm = 6.8f,
            zoomChange = 0.6f,
        )

        assertEquals(7f, zoomedOutTop)
    }

    @Test
    fun zoomedTopAltitudeKm_keepsDefaultUpperBoundWhenZoomingIn() {
        val zoomedInTop = zoomedTopAltitudeKm(
            currentTopAltitudeKm = 4.4f,
            zoomChange = 1.5f,
        )

        assertEquals(4f, zoomedInTop)
    }

    @Test
    fun viewport_withVisibleTopAltitudeKm_ignoresInvalidInput() {
        val viewport = ForecastChartViewport()

        assertEquals(
            4f,
            viewport.withVisibleTopAltitudeKm(Float.NaN).visibleTopAltitudeKm,
        )
    }
}
