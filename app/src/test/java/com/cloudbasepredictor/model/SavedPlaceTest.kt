package com.cloudbasepredictor.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedPlaceTest {
    @Test
    fun fromCoordinates_formatsIdAndNameWithFourDecimals() {
        val place = SavedPlace.fromCoordinates(
            latitude = 46.55823,
            longitude = 7.83537,
        )

        assertEquals("place:46.5582:7.8354", place.id)
        assertEquals("46.5582, 7.8354", place.name)
        assertEquals("", place.defaultModel)
        assertEquals(46.55823, place.latitude, 0.0)
        assertEquals(7.83537, place.longitude, 0.0)
    }
}
