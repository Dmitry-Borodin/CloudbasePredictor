package com.cloudbasepredictor.ui.screens.map

import com.cloudbasepredictor.model.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When a user taps on the map at coordinates near a favorite place,
 * the selected place should be the favorite (with its name), not a
 * generic coordinate-based place.
 */
class MapViewModelFavoriteMatchTest {

    private val innsbruck = SavedPlace(
        id = "fav_innsbruck",
        name = "Innsbruck",
        latitude = 47.2692,
        longitude = 11.4041,
        defaultModel = "icon_seamless",
        isFavorite = true,
    )

    @Test
    fun isNearby_matchesFavoriteWhenTappingNearby() {
        val favorites = listOf(innsbruck)

        // Simulate tapping very close to Innsbruck (~50m offset)
        val tapLat = 47.2696
        val tapLon = 11.4041

        val match = favorites.find { it.isNearby(tapLat, tapLon) }
        assertNotNull("Should match Innsbruck favorite", match)
        assertEquals("Innsbruck", match!!.name)
        assertTrue(match.isFavorite)
    }

    @Test
    fun isNearby_doesNotMatchFavoriteWhenTappingFarAway() {
        val favorites = listOf(innsbruck)

        // Simulate tapping 1km away from Innsbruck
        val tapLat = 47.28
        val tapLon = 11.4041

        val match = favorites.find { it.isNearby(tapLat, tapLon) }
        assertEquals("Should not match any favorite", null, match)
    }

    @Test
    fun favoriteMatchResult_hasCorrectNameAndId() {
        val favorites = listOf(innsbruck)

        // Tap at exact favorite coordinates
        val tapLat = 47.2692
        val tapLon = 11.4041

        val match = favorites.find { it.isNearby(tapLat, tapLon) }
        val place = match ?: SavedPlace.fromCoordinates(tapLat, tapLon)

        assertEquals("Innsbruck", place.name)
        assertEquals("fav_innsbruck", place.id)
    }

    @Test
    fun noFavoriteMatch_fallsBackToCoordinates() {
        val favorites = listOf(innsbruck)

        // Tap far from any favorite
        val tapLat = 48.0
        val tapLon = 12.0

        val match = favorites.find { it.isNearby(tapLat, tapLon) }
        val place = match ?: SavedPlace.fromCoordinates(tapLat, tapLon)

        assertEquals("48.0000, 12.0000", place.name)
    }
}
