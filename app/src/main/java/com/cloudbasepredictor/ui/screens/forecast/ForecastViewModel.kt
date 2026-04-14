package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.forecast.ForecastModeRepository
import com.cloudbasepredictor.data.forecast.ForecastModelRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbasepredictor.data.forecast.ForecastRepository
import com.cloudbasepredictor.data.place.PlaceRepository
import com.cloudbasepredictor.model.DailyForecast
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.model.ForecastModel
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForecastDayChipUiModel(
    val title: String,
    val subtitle: String,
)

/**
 * Complete UI state for the forecast screen.
 *
 * The screen has three visual states:
 * - **Loading**: [isLoading] is true — shows progress indicator.
 * - **Error**: [errorMessage] is non-null — shows error text with retry button.
 * - **Ready**: neither loading nor error — shows the selected chart.
 */
data class ForecastUiState(
    /** Currently selected place, or null when no location has been chosen. */
    val selectedPlace: SavedPlace? = null,
    /** Active forecast visualisation mode (thermic / stuve / wind / cloud). */
    val selectedForecastMode: ForecastMode = ForecastMode.THERMIC,
    /** Zero-based index of the selected forecast day (0 = today). */
    val selectedDayIndex: Int = 0,
    /** Visible altitude range controlled by pinch-to-zoom. */
    val chartViewport: ForecastChartViewport = ForecastChartViewport(),
    /** Thermic updraft strength chart data. */
    val thermicChart: ThermicForecastChartUiModel = buildPlaceholderThermicForecastChart(dayIndex = 0),
    /** Stüve thermodynamic diagram data for the selected hour. */
    val stuveChart: StuveForecastChartUiModel = buildPlaceholderStuveChart(),
    /** Wind speed & direction chart data. */
    val windChart: WindForecastChartUiModel = buildPlaceholderWindForecastChart(),
    /** Cloud coverage & precipitation chart data. */
    val cloudChart: CloudForecastChartUiModel = buildPlaceholderCloudForecastChart(),
    /** Day chips for the date picker (title + subtitle). */
    val dayChips: List<ForecastDayChipUiModel> = placeholderDayChips(),
    /** Summary text shown at the bottom of the chart. */
    val forecastText: String = "Select a point on the map to open a forecast.",
    /** True while the forecast is being fetched from the network. */
    val isLoading: Boolean = false,
    /** Non-null when the last load attempt failed; displayed as error state. */
    val errorMessage: String? = null,
    /** Weather model requested by the user. */
    val selectedModel: ForecastModel = ForecastModel.BEST_MATCH,
    /** Model actually used after fallback (may differ from [selectedModel]). */
    val resolvedModel: ForecastModel? = null,
    /** Timestamp (UTC millis) when the forecast data was last updated from the server. */
    val forecastUpdatedAtMillis: Long? = null,
    /** Estimated UTC millis of the model run that produced this forecast. */
    val modelGeneratedAtMillis: Long? = null,
    /** Terrain elevation in km ASL for the selected place. */
    val elevationKm: Float = 0f,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository,
    private val placeRepository: PlaceRepository,
    private val forecastModeRepository: ForecastModeRepository,
    private val forecastModelRepository: ForecastModelRepository,
) : ViewModel() {
    private val selectedDayIndex = MutableStateFlow(0)
    private val chartViewport = MutableStateFlow(ForecastChartViewport())
    private val stuveHour = MutableStateFlow(12)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val _networkErrorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val networkErrorEvent: SharedFlow<String> = _networkErrorEvent

    private val selectedPlace = placeRepository.selectedPlace

    private val selectedForecast = combine(
        selectedPlace,
        forecastModelRepository.selectedModel,
    ) { place, model ->
        place to model
    }.flatMapLatest { (place, model) ->
        if (place == null) {
            flowOf(null)
        } else {
            forecastRepository.observeForecast(place.id, model)
        }
    }

    private val selectedModeWithDayIndex = combine(
        forecastModeRepository.selectedMode,
        selectedDayIndex,
    ) { mode, dayIndex ->
        mode to dayIndex
    }

    private val chartContext = combine(
        selectedModeWithDayIndex,
        chartViewport,
        stuveHour,
    ) { selectedModeAndDayIndex, currentChartViewport, currentStuveHour ->
        ForecastChartContext(
            selectedForecastMode = selectedModeAndDayIndex.first,
            selectedDayIndex = selectedModeAndDayIndex.second,
            chartViewport = currentChartViewport,
            stuveHour = currentStuveHour,
        )
    }

    val uiState: StateFlow<ForecastUiState> = combine(
        selectedPlace,
        selectedForecast,
        chartContext,
        isLoading,
        errorMessage,
        forecastModelRepository.selectedModel,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val place = values[0] as SavedPlace?
        val snapshot = values[1] as ForecastSnapshot?
        val currentChartContext = values[2] as ForecastChartContext
        val loading = values[3] as Boolean
        val currentError = values[4] as String?
        val currentModel = values[5] as ForecastModel

        val dayChips = snapshot?.days?.let(::buildDayChips)
            ?.takeIf { it.isNotEmpty() }
            ?: placeholderDayChips()
        val safeDayIndex = currentChartContext.selectedDayIndex.coerceIn(0, dayChips.lastIndex)

        ForecastUiState(
            selectedPlace = place,
            selectedForecastMode = currentChartContext.selectedForecastMode,
            selectedDayIndex = safeDayIndex,
            chartViewport = currentChartContext.chartViewport,
            thermicChart = snapshot?.hourlyData?.let {
                buildThermicChartFromData(it, dayIndex = safeDayIndex)
            } ?: buildPlaceholderThermicForecastChart(dayIndex = safeDayIndex),
            stuveChart = snapshot?.hourlyData?.let {
                buildStuveChartFromData(it, dayIndex = safeDayIndex, hour = currentChartContext.stuveHour)
            } ?: buildPlaceholderStuveChart(
                hour = currentChartContext.stuveHour,
                dayIndex = safeDayIndex,
            ),
            windChart = snapshot?.hourlyData?.let {
                buildWindChartFromData(
                    it,
                    dayIndex = safeDayIndex,
                    maxAltitudeKm = currentChartContext.chartViewport.visibleTopAltitudeKm,
                )
            } ?: buildPlaceholderWindForecastChart(
                dayIndex = safeDayIndex,
                maxAltitudeKm = currentChartContext.chartViewport.visibleTopAltitudeKm,
            ),
            cloudChart = snapshot?.hourlyData?.let {
                buildCloudChartFromData(it, dayIndex = safeDayIndex)
            } ?: buildPlaceholderCloudForecastChart(dayIndex = safeDayIndex),
            dayChips = dayChips,
            forecastText = buildForecastText(
                mode = currentChartContext.selectedForecastMode,
                place = place,
                snapshot = snapshot,
                selectedDayIndex = safeDayIndex,
                isLoading = loading,
            ),
            isLoading = loading,
            errorMessage = currentError,
            selectedModel = currentModel,
            resolvedModel = snapshot?.resolvedModel,
            forecastUpdatedAtMillis = snapshot?.updatedAtUtcMillis,
            modelGeneratedAtMillis = snapshot?.modelGeneratedAtMillis,
            elevationKm = (snapshot?.hourlyData?.elevation ?: 0.0).toFloat() / 1000f,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ForecastUiState(),
    )

    init {
        viewModelScope.launch {
            combine(
                selectedPlace,
                forecastModelRepository.selectedModel,
            ) { place, model -> place to model }
                .collect { (place, model) ->
                    errorMessage.value = null

                    if (place == null) {
                        isLoading.value = false
                        return@collect
                    }

                    if (forecastRepository.isCached(place.id, model)) {
                        isLoading.value = false
                        // Prefetch full range in background if only quick load was done.
                        if (!forecastRepository.isFullyCached(place.id, model)) {
                            prefetchFullForecast(place, model)
                        }
                        return@collect
                    }

                    isLoading.value = true
                    runCatching {
                        forecastRepository.loadForecast(
                            place, model = model, forecastDays = 2,
                        )
                    }.onFailure { throwable ->
                        val msg = throwable.message ?: "Unable to load forecast right now."
                        errorMessage.value = msg
                        _networkErrorEvent.tryEmit(msg)
                    }
                    isLoading.value = false

                    // Prefetch full range in background.
                    if (errorMessage.value == null) {
                        prefetchFullForecast(place, model)
                    }
                }
        }
    }

    private fun prefetchFullForecast(place: SavedPlace, model: ForecastModel) {
        viewModelScope.launch {
            runCatching {
                forecastRepository.loadForecast(
                    place, model = model, forecastDays = 7,
                )
            }
        }
    }

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
        // If the selected day is beyond the currently loaded range, trigger a full load.
        val place = selectedPlace.value ?: return
        val model = forecastModelRepository.selectedModel.value
        if (!forecastRepository.isFullyCached(place.id, model) && index >= 2) {
            viewModelScope.launch {
                isLoading.value = true
                runCatching {
                    forecastRepository.loadForecast(
                        place, model = model, forecastDays = 7,
                    )
                }.onFailure { throwable ->
                    val msg = throwable.message ?: "Unable to load forecast right now."
                    errorMessage.value = msg
                    _networkErrorEvent.tryEmit(msg)
                }
                isLoading.value = false
            }
        }
    }

    fun selectForecastMode(mode: ForecastMode) {
        forecastModeRepository.selectMode(mode)
    }

    fun updateChartTopAltitude(topAltitudeKm: Float) {
        chartViewport.update { currentViewport ->
            currentViewport.withVisibleTopAltitudeKm(topAltitudeKm)
        }
    }

    fun saveFavorite(name: String) {
        val place = uiState.value.selectedPlace ?: return
        viewModelScope.launch {
            placeRepository.saveFavorite(place.id, name)
        }
    }

    fun deleteFavorite() {
        val place = uiState.value.selectedPlace ?: return
        viewModelScope.launch {
            placeRepository.deleteFavorite(place.id)
        }
    }

    fun updateStuveHour(hour: Int) {
        stuveHour.value = hour.coerceIn(6, 22)
    }

    fun selectModel(model: ForecastModel) {
        forecastModelRepository.selectModel(model)
    }

    fun retryLoad() {
        val place = selectedPlace.value ?: return
        val model = forecastModelRepository.selectedModel.value
        errorMessage.value = null
        viewModelScope.launch {
            isLoading.value = true
            runCatching {
                forecastRepository.loadForecast(
                    place,
                    forceRefresh = true,
                    model = model,
                    forecastDays = 2,
                )
            }.onFailure { throwable ->
                val msg = throwable.message ?: "Unable to load forecast right now."
                errorMessage.value = msg
                _networkErrorEvent.tryEmit(msg)
            }
            isLoading.value = false

            if (errorMessage.value == null) {
                prefetchFullForecast(place, model)
            }
        }
    }
}

private data class ForecastChartContext(
    val selectedForecastMode: ForecastMode,
    val selectedDayIndex: Int,
    val chartViewport: ForecastChartViewport,
    val stuveHour: Int = 12,
)

private fun buildForecastText(
    mode: ForecastMode,
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
        return when (mode) {
            ForecastMode.THERMIC -> {
                if (isLoading) {
                    "Loading a 14-day forecast for ${place.name}."
                } else {
                    "Forecast content for ${place.name} will appear here."
                }
            }
            ForecastMode.STUVE -> {
                if (isLoading) {
                    "Loading a 14-day stuve forecast for ${place.name}."
                } else {
                    "Stuve forecast content for ${place.name} will appear here."
                }
            }
            ForecastMode.WIND -> {
                if (isLoading) {
                    "Loading a 14-day wind forecast for ${place.name}."
                } else {
                    "Wind forecast content for ${place.name} will appear here."
                }
            }
            ForecastMode.CLOUD -> {
                if (isLoading) {
                    "Loading a 14-day cloud forecast for ${place.name}."
                } else {
                    "Cloud forecast content for ${place.name} will appear here."
                }
            }
        }
    }

    val weather = WeatherCode.present(selectedDay.weatherCode)
    val dayTitle = if (selectedDayIndex == 0) {
        "Today"
    } else {
        selectedDay.date
    }

    return when (mode) {
        ForecastMode.THERMIC -> {
            buildString {
                append(dayTitle)
                append(" in ")
                append(place.name)
                append(". ")
                append(weather.label)
                append(". High ")
                append(formatTemperature(selectedDay.maxTemperatureCelsius))
                append(", low ")
                append(formatTemperature(selectedDay.minTemperatureCelsius))
                append(". This area can later host the full thermic forecast layout.")
            }
        }
        ForecastMode.STUVE -> {
            buildString {
                append(dayTitle)
                append(" in ")
                append(place.name)
                append(". ")
                append(weather.label)
                append(". Stuve placeholder forecast. ")
                append("This area can later host the full stuve forecast layout.")
            }
        }
        ForecastMode.WIND -> {
            buildString {
                append(dayTitle)
                append(" in ")
                append(place.name)
                append(". ")
                append(weather.label)
                append(". Wind placeholder forecast. ")
                append("This area can later host the full wind forecast layout.")
            }
        }
        ForecastMode.CLOUD -> {
            buildString {
                append(dayTitle)
                append(" in ")
                append(place.name)
                append(". ")
                append(weather.label)
                append(". Cloud forecast placeholder. ")
                append("This area can later host the full cloud forecast layout.")
            }
        }
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
