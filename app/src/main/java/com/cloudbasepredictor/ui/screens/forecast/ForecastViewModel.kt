package com.cloudbasepredictor.ui.screens.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbasepredictor.data.forecast.ForecastRepository
import com.cloudbasepredictor.data.place.PlaceRepository
import com.cloudbasepredictor.model.DailyForecast
import com.cloudbasepredictor.model.ForecastSnapshot
import com.cloudbasepredictor.model.SavedPlace
import com.cloudbasepredictor.model.WeatherCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ForecastDayChipUiModel(
    val title: String,
    val subtitle: String,
)

data class ForecastUiState(
    val selectedPlace: SavedPlace? = null,
    val selectedDayIndex: Int = 0,
    val dayChips: List<ForecastDayChipUiModel> = placeholderDayChips(),
    val forecastText: String = "Select a point on the map to open a forecast.",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository,
    private val placeRepository: PlaceRepository,
) : ViewModel() {
    private val selectedDayIndex = MutableStateFlow(0)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val selectedPlace = placeRepository.selectedPlace

    private val selectedForecast = selectedPlace.flatMapLatest { place ->
        if (place == null) {
            flowOf(null)
        } else {
            forecastRepository.observeForecast(place.id)
        }
    }

    val uiState: StateFlow<ForecastUiState> = combine(
        selectedPlace,
        selectedForecast,
        selectedDayIndex,
        isLoading,
        errorMessage,
    ) { place, snapshot, dayIndex, loading, currentError ->
        val dayChips = snapshot?.days?.let(::buildDayChips)
            ?.takeIf { it.isNotEmpty() }
            ?: placeholderDayChips()
        val safeDayIndex = dayIndex.coerceIn(0, dayChips.lastIndex)

        ForecastUiState(
            selectedPlace = place,
            selectedDayIndex = safeDayIndex,
            dayChips = dayChips,
            forecastText = buildForecastText(
                place = place,
                snapshot = snapshot,
                selectedDayIndex = safeDayIndex,
                isLoading = loading,
            ),
            isLoading = loading,
            errorMessage = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ForecastUiState(),
    )

    init {
        viewModelScope.launch {
            selectedPlace.collect { place ->
                selectedDayIndex.value = 0
                errorMessage.value = null

                if (place == null) {
                    isLoading.value = false
                    return@collect
                }

                isLoading.value = true
                runCatching {
                    forecastRepository.loadForecast(place)
                }.onFailure { throwable ->
                    errorMessage.value = throwable.message ?: "Unable to load forecast right now."
                }
                isLoading.value = false
            }
        }
    }

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
    }
}

private fun buildForecastText(
    place: SavedPlace?,
    snapshot: ForecastSnapshot?,
    selectedDayIndex: Int,
    isLoading: Boolean,
): String {
    if (place == null) {
        return "Select a point on the map and open it to see the forecast here."
    }

    val days = snapshot?.days.orEmpty()
    val selectedDay = days.getOrNull(selectedDayIndex)

    if (selectedDay == null) {
        return if (isLoading) {
            "Loading a 14-day forecast for ${place.name}."
        } else {
            "Forecast content for ${place.name} will appear here."
        }
    }

    val weather = WeatherCode.present(selectedDay.weatherCode)
    val dayTitle = if (selectedDayIndex == 0) {
        "Today"
    } else {
        selectedDay.date
    }

    return buildString {
        append(dayTitle)
        append(" in ")
        append(place.name)
        append(". ")
        append(weather.label)
        append(". High ")
        append(formatTemperature(selectedDay.maxTemperatureCelsius))
        append(", low ")
        append(formatTemperature(selectedDay.minTemperatureCelsius))
        append(". This area can later host the full forecast layout.")
    }
}

private fun buildDayChips(days: List<DailyForecast>): List<ForecastDayChipUiModel> {
    return days.take(14).mapIndexed { index, day ->
        ForecastDayChipUiModel(
            title = if (index == 0) "Today" else formatDayTitle(day.date),
            subtitle = formatDaySubtitle(day.date, index),
        )
    }
}

private fun placeholderDayChips(): List<ForecastDayChipUiModel> {
    return List(14) { index ->
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, index)
        }
        ForecastDayChipUiModel(
            title = if (index == 0) "Today" else SimpleDateFormat("EEE", Locale.US).format(calendar.time),
            subtitle = SimpleDateFormat("d MMM", Locale.US).format(calendar.time),
        )
    }
}

private fun formatDayTitle(date: String): String {
    return parseForecastDate(date)?.let { parsedDate ->
        SimpleDateFormat("EEE", Locale.US).format(parsedDate)
    } ?: date
}

private fun formatDaySubtitle(
    date: String,
    selectedDayIndex: Int,
): String {
    if (selectedDayIndex == 0) {
        return "Today"
    }

    return parseForecastDate(date)?.let { parsedDate ->
        SimpleDateFormat("d MMM", Locale.US).format(parsedDate)
    } ?: date
}

private fun parseForecastDate(date: String): Date? {
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
    }.getOrNull()
}

private fun formatTemperature(value: Double): String {
    return String.format(Locale.US, "%.1f°C", value)
}
