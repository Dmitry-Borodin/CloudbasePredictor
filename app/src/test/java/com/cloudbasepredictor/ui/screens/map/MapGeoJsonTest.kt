package com.cloudbasepredictor.ui.screens.map

import com.cloudbasepredictor.model.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapGeoJsonTest {

    private val testPlace = SavedPlace(
        id = "place:46.6863:7.8632",
        name = "Interlaken",
        latitude = 46.6863,
        longitude = 7.8632,
        isFavorite = false,
    )

    @Test
    fun buildMarkerFeatureCollection_containsCorrectCoordinates() {
        val json = buildMarkerFeatureCollection(testPlace)
        assertTrue(json.contains("[7.8632, 46.6863]"))
        assertTrue(json.contains("\"type\": \"FeatureCollection\""))
        assertTrue(json.contains("\"type\": \"Point\""))
    }

    @Test
    fun buildMarkerFeatureCollection_containsPlaceName() {
        val json = buildMarkerFeatureCollection(testPlace)
        assertTrue(json.contains("\"name\": \"Interlaken\""))
    }

    @Test
    fun buildMarkerFeatureCollection_escapesQuotesInName() {
        val place = testPlace.copy(name = "Say \"hello\"")
        val json = buildMarkerFeatureCollection(place)
        assertTrue(json.contains("Say \\\"hello\\\""))
    }

    @Test
    fun buildFavoritesFeatureCollection_returnsAllFavorites() {
        val places = listOf(
            testPlace,
            testPlace.copy(id = "place:2", name = "Zurich", latitude = 47.3769, longitude = 8.5417),
            testPlace.copy(id = "place:3", name = "Bern", latitude = 46.9480, longitude = 7.4474),
        )

        val json = buildFavoritesFeatureCollection(places)
        assertTrue(json.contains("Interlaken"))
        assertTrue(json.contains("Zurich"))
        assertTrue(json.contains("Bern"))
        // Count features by counting "type": "Feature" occurrences
        assertEquals(3, Regex("\"type\":\\s*\"Feature\"").findAll(json).count())
    }

    @Test
    fun buildFavoritesFeatureCollection_emptyListReturnsEmptyCollection() {
        val json = buildFavoritesFeatureCollection(emptyList())
        assertTrue(json.contains("\"type\": \"FeatureCollection\""))
        assertTrue(json.contains("\"features\": []"))
    }

    @Test
    fun emptyFeatureCollection_isValidGeoJson() {
        val json = emptyFeatureCollection()
        assertTrue(json.contains("\"type\": \"FeatureCollection\""))
        assertTrue(json.contains("\"features\": []"))
    }
}
