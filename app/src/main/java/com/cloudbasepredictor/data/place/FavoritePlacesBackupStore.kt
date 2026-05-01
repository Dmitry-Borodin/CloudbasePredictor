package com.cloudbasepredictor.data.place

import android.content.SharedPreferences
import com.cloudbasepredictor.data.units.UnitPreset
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
        return readPayload()
            ?.places
            ?.mapNotNull { place ->
                runCatching {
                    SavedPlace.fromCoordinates(
                        latitude = place.latitude,
                        longitude = place.longitude,
                    ).copy(
                        name = place.name,
                        isFavorite = true,
                    )
                }.getOrNull()
            }
            ?.distinctBy(SavedPlace::id)
            .orEmpty()
    }

    fun saveFavoritePlaces(places: List<SavedPlace>) {
        val existingPayload = readPayload()
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
            unitPreset = existingPayload?.unitPreset,
        )
        savePayload(payload)
    }

    fun readUnitPreset(): UnitPreset? {
        val unitPresetName = (readPayload()?.unitPreset as? JsonPrimitive)?.contentOrNull ?: return null
        return runCatching { UnitPreset.valueOf(unitPresetName) }.getOrNull()
    }

    fun saveUnitPreset(unitPreset: UnitPreset) {
        val existingPayload = readPayload()
        savePayload(
            FavoritePlacesBackupPayload(
                places = existingPayload?.places.orEmpty(),
                unitPreset = JsonPrimitive(unitPreset.name),
            ),
        )
    }

    private fun readPayload(): FavoritePlacesBackupPayload? {
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return null
        return try {
            json.decodeFromString<FavoritePlacesBackupPayload>(payload)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SerializationException) {
            null
        }
    }

    private fun savePayload(payload: FavoritePlacesBackupPayload) {
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
    val schemaVersion: Int = 2,
    val places: List<FavoritePlaceBackupEntry> = emptyList(),
    val unitPreset: JsonElement? = null,
)

@Serializable
private data class FavoritePlaceBackupEntry(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
