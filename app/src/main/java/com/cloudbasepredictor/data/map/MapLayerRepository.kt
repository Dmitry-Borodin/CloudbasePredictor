package com.cloudbasepredictor.data.map

import android.content.SharedPreferences
import com.cloudbasepredictor.data.place.FavoritePlacesBackupStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MapLayerPreference {
    OPENFREEMAP,
    NASA_GIBS,
    ESRI_WORLD_IMAGERY,
}

interface MapLayerRepository {
    val selectedLayer: StateFlow<MapLayerPreference>

    fun selectLayer(layer: MapLayerPreference)
}

@Singleton
class SharedPrefsMapLayerRepository @Inject constructor(
    private val prefs: SharedPreferences,
    private val backupStore: FavoritePlacesBackupStore,
) : MapLayerRepository {
    private val mutableSelectedLayer = MutableStateFlow(loadLayerPreference())

    override val selectedLayer: StateFlow<MapLayerPreference> = mutableSelectedLayer.asStateFlow()

    override fun selectLayer(layer: MapLayerPreference) {
        mutableSelectedLayer.value = layer
        prefs.edit().putString(KEY_MAP_LAYER, layer.name).apply()
        backupStore.saveMapLayer(layer)
    }

    private fun loadLayerPreference(): MapLayerPreference {
        prefs.getString(KEY_MAP_LAYER, null)
            ?.let(::decodeLayerPreference)
            ?.let { return it }

        backupStore.readMapLayer()?.let { restored ->
            prefs.edit().putString(KEY_MAP_LAYER, restored.name).apply()
            return restored
        }

        return DEFAULT_MAP_LAYER
    }

    private fun decodeLayerPreference(value: String): MapLayerPreference? {
        return runCatching { MapLayerPreference.valueOf(value) }.getOrNull()
    }

    private companion object {
        val DEFAULT_MAP_LAYER = MapLayerPreference.OPENFREEMAP
        const val KEY_MAP_LAYER = "map_layer"
    }
}
