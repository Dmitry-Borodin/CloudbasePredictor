package com.cloudbasepredictor.ui.screens.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbasepredictor.data.place.PlaceRepository
import com.cloudbasepredictor.model.SavedPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapCameraData(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

data class MapUiState(
    val selectedPlace: SavedPlace? = null,
    val favoritePlaces: List<SavedPlace> = emptyList(),
    val initialCamera: MapCameraData? = null,
)

sealed interface MapEvent {
    data object OpenForecast : MapEvent
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val selectedPlaceDraft = MutableStateFlow<SavedPlace?>(null)
    private val mutableEvents = MutableSharedFlow<MapEvent>()
    private val prefs = context.getSharedPreferences("map_camera", Context.MODE_PRIVATE)

    val events = mutableEvents.asSharedFlow()

    val uiState: StateFlow<MapUiState> = combine(
        selectedPlaceDraft,
        placeRepository.observeFavoritePlaces(),
    ) { selectedPlace, favorites ->
        MapUiState(
            selectedPlace = selectedPlace,
            favoritePlaces = favorites,
            initialCamera = loadCameraPosition(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(initialCamera = loadCameraPosition()),
    )

    fun selectPoint(
        latitude: Double,
        longitude: Double,
    ) {
        selectedPlaceDraft.value = SavedPlace.fromCoordinates(
            latitude = latitude,
            longitude = longitude,
        )
    }

    fun openSelectedForecast() {
        val place = selectedPlaceDraft.value ?: return

        viewModelScope.launch {
            placeRepository.saveAndSelectPlace(place)
            mutableEvents.emit(MapEvent.OpenForecast)
        }
    }

    fun openForecastForPlace(place: SavedPlace) {
        viewModelScope.launch {
            placeRepository.selectPlace(place)
            mutableEvents.emit(MapEvent.OpenForecast)
        }
    }

    fun saveCameraPosition(latitude: Double, longitude: Double, zoom: Double) {
        prefs.edit()
            .putLong(KEY_LAT, latitude.toBits())
            .putLong(KEY_LNG, longitude.toBits())
            .putLong(KEY_ZOOM, zoom.toBits())
            .putBoolean(KEY_HAS_POSITION, true)
            .apply()
    }

    private fun loadCameraPosition(): MapCameraData? {
        if (!prefs.getBoolean(KEY_HAS_POSITION, false)) return null
        return MapCameraData(
            latitude = Double.fromBits(prefs.getLong(KEY_LAT, 0L)),
            longitude = Double.fromBits(prefs.getLong(KEY_LNG, 0L)),
            zoom = Double.fromBits(prefs.getLong(KEY_ZOOM, 0L)),
        )
    }

    companion object {
        private const val KEY_HAS_POSITION = "has_position"
        private const val KEY_LAT = "camera_lat"
        private const val KEY_LNG = "camera_lng"
        private const val KEY_ZOOM = "camera_zoom"
    }
}
