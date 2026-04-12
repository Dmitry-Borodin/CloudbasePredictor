package com.cloudbasepredictor.model

import java.util.Locale

data class SavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val defaultModel: String,
    val isFavorite: Boolean = false,
) {
    companion object {
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
                defaultModel = "",
            )
        }
    }
}
