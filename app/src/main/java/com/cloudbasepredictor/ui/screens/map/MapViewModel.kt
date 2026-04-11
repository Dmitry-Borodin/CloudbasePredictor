package com.cloudbasepredictor.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbasepredictor.data.place.PlaceRepository
import com.cloudbasepredictor.model.SavedPlace
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapUiState(
    val selectedPlace: SavedPlace? = null,
)

sealed interface MapEvent {
    data object OpenForecast : MapEvent
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
) : ViewModel() {
    private val selectedPlaceDraft = MutableStateFlow<SavedPlace?>(null)
    private val mutableEvents = MutableSharedFlow<MapEvent>()

    val events = mutableEvents.asSharedFlow()

    val uiState: StateFlow<MapUiState> = selectedPlaceDraft
        .map { selectedPlace ->
            MapUiState(selectedPlace = selectedPlace)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(),
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
}
