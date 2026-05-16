package com.cloudbasepredictor.ui.screens.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBearingTest {
    @Test
    fun normalizedBearingDegrees_wrapsPositiveBearings() {
        assertEquals(0.0, normalizedBearingDegrees(360.0), 0.0)
        assertEquals(45.0, normalizedBearingDegrees(405.0), 0.0)
        assertEquals(0.5, normalizedBearingDegrees(720.5), 0.0)
    }

    @Test
    fun normalizedBearingDegrees_wrapsNegativeBearings() {
        assertEquals(359.0, normalizedBearingDegrees(-1.0), 0.0)
        assertEquals(315.0, normalizedBearingDegrees(-45.0), 0.0)
    }

    @Test
    fun shouldShowNorthButton_hidesWhenBearingIsNearNorth() {
        assertFalse(shouldShowNorthButton(0.0))
        assertFalse(shouldShowNorthButton(0.99))
        assertFalse(shouldShowNorthButton(359.01))
    }

    @Test
    fun shouldShowNorthButton_showsAtOneDegreeOrMoreFromNorth() {
        assertTrue(shouldShowNorthButton(1.0))
        assertTrue(shouldShowNorthButton(180.0))
        assertTrue(shouldShowNorthButton(359.0))
        assertTrue(shouldShowNorthButton(-1.0))
    }
}
