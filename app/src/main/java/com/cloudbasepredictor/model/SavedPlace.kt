package com.cloudbasepredictor.model

import java.util.Locale

data class SavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false,
) {
    /**
     * Returns true if the given coordinates are within ~200 m of this place.
     * Uses a fast equi-rectangular approximation suitable for short distances.
     */
    fun isNearby(lat: Double, lon: Double, thresholdMeters: Double = 200.0): Boolean {
        val dLat = Math.toRadians(latitude - lat)
        val dLon = Math.toRadians(longitude - lon) * kotlin.math.cos(Math.toRadians((latitude + lat) / 2.0))
        val distMeters = kotlin.math.sqrt(dLat * dLat + dLon * dLon) * EARTH_RADIUS_M
        return distMeters <= thresholdMeters
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0

        fun fromCoordinates(
            latitude: Double,
            longitude: Double,
        ): SavedPlace {
            val normalizedLatitude = String.format(Locale.US, "%.4f", latitude)
            val normalizedLongitude = String.format(Locale.US, "%.4f", longitude)
            val displayName = "$normalizedLatitude, $normalizedLongitude"

            return SavedPlace(
                id = "place:$normalizedLatitude:$normalizedLongitude",
                name = displayName,
                latitude = latitude,
                longitude = longitude,
            )
        }
    }
}
