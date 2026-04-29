package com.cloudbasepredictor.data.place

import android.content.SharedPreferences
import com.cloudbasepredictor.di.FavoritePlacesBackupPreferences
import com.cloudbasepredictor.model.SavedPlace
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object FavoritePlacesBackupContract {
    const val PREFS_NAME = "favorite_places_backup"
    const val PREFS_FILE_NAME = "$PREFS_NAME.xml"
}

@Singleton
class FavoritePlacesBackupStore @Inject constructor(
    @param:FavoritePlacesBackupPreferences private val prefs: SharedPreferences,
    private val json: Json,
) {
    fun readFavoritePlaces(): List<SavedPlace> {
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return emptyList()
        return try {
            json.decodeFromString<FavoritePlacesBackupPayload>(payload)
                .places
                .map { place ->
                    SavedPlace.fromCoordinates(
                        latitude = place.latitude,
                        longitude = place.longitude,
                    ).copy(
                        name = place.name,
                        isFavorite = true,
                    )
                }
                .distinctBy(SavedPlace::id)
        } catch (_: IllegalArgumentException) {
            emptyList()
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    fun saveFavoritePlaces(places: List<SavedPlace>) {
        val payload = FavoritePlacesBackupPayload(
            places = places
                .filter(SavedPlace::isFavorite)
                .distinctBy(SavedPlace::id)
                .map { place ->
                    FavoritePlaceBackupEntry(
                        name = place.name,
                        latitude = place.latitude,
                        longitude = place.longitude,
                    )
                },
        )
        prefs.edit()
            .putString(KEY_PAYLOAD, json.encodeToString(payload))
            .apply()
    }

    private companion object {
        const val KEY_PAYLOAD = "payload"
    }
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
private data class FavoritePlacesBackupPayload(
    @EncodeDefault
    val schemaVersion: Int = 1,
    val places: List<FavoritePlaceBackupEntry> = emptyList(),
)

@Serializable
private data class FavoritePlaceBackupEntry(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
