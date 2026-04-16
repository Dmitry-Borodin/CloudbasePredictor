package com.cloudbasepredictor.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun isNearby_returnsTrueForSameCoordinates() {
        val innsbruck = SavedPlace(
            id = "innsbruck",
            name = "Innsbruck",
            latitude = 47.2692,
            longitude = 11.4041,
            defaultModel = "",
            isFavorite = true,
        )
        assertTrue(innsbruck.isNearby(47.2692, 11.4041))
    }

    @Test
    fun isNearby_returnsTrueWithin200m() {
        val innsbruck = SavedPlace(
            id = "innsbruck",
            name = "Innsbruck",
            latitude = 47.2692,
            longitude = 11.4041,
            defaultModel = "",
            isFavorite = true,
        )
        // ~100m offset in latitude (~0.0009°)
        assertTrue(innsbruck.isNearby(47.2701, 11.4041))
    }

    @Test
    fun isNearby_returnsFalseWhenFarAway() {
        val innsbruck = SavedPlace(
            id = "innsbruck",
            name = "Innsbruck",
            latitude = 47.2692,
            longitude = 11.4041,
            defaultModel = "",
            isFavorite = true,
        )
        // ~5km away
        assertFalse(innsbruck.isNearby(47.31, 11.4041))
    }
}
