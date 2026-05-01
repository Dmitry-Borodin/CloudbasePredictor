package com.cloudbasepredictor.ui.screens.forecast

import android.util.Log
import com.cloudbasepredictor.data.forecast.ForecastModeRepository
import com.cloudbasepredictor.data.forecast.ForecastModelRepository
import com.cloudbasepredictor.data.forecast.INITIAL_FORECAST_DAYS
import com.cloudbasepredictor.data.forecast.MAX_FORECAST_DAYS
import com.cloudbasepredictor.data.forecast.ForecastViewportRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbasepredictor.data.forecast.ForecastRepository
import com.cloudbasepredictor.data.forecast.exposedForecastDayCount
import com.cloudbasepredictor.data.forecast.requestedForecastDaysForDayIndex
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    val dayChips: List<ForecastDayChipUiModel> = placeholderDayChips(INITIAL_FORECAST_DAYS),
    /** Summary text shown at the bottom of the chart. */
    val forecastText: String = "Select a point on the map to open a forecast.",
    /** True while the forecast is being fetched from the network. */
    val isLoading: Boolean = false,
    /** Non-null when the last load attempt failed; displayed as error state. */
    val errorMessage: String? = null,
    /** Weather model requested by the user. */
    val selectedModel: ForecastModel = ForecastModel.ICON_SEAMLESS,
    /** Model actually used after fallback (may differ from [selectedModel]). */
    val resolvedModel: ForecastModel? = null,
    /** Timestamp (UTC millis) when the forecast data was last updated from the server. */
    val forecastUpdatedAtMillis: Long? = null,
    /** Estimated UTC millis of the model run that produced this forecast. */
    val modelGeneratedAtMillis: Long? = null,
    /** Terrain elevation in km ASL for the selected place. */
    val elevationKm: Float = 0f,
    /** Favorite places to show on the forecast map panel. */
    val favoritePlaces: List<SavedPlace> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val forecastRepository: ForecastRepository,
    private val placeRepository: PlaceRepository,
    private val forecastModeRepository: ForecastModeRepository,
    private val forecastModelRepository: ForecastModelRepository,
    private val forecastViewportRepository: ForecastViewportRepository,
) : ViewModel() {
    private val selectedDayIndex = MutableStateFlow(0)
    private val chartViewport = MutableStateFlow(
        ForecastChartViewport(
            visibleTopAltitudeKm = forecastViewportRepository.visibleTopAltitudeKm.value,
        ),
    )
    private val stuveHour = MutableStateFlow(12)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private var forecastLoadJob: Job? = null
    private var forecastLoadGeneration = 0

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

    private val uiInputs = combine(
        selectedPlace,
        selectedForecast,
        chartContext,
        isLoading,
        errorMessage,
    ) { place, snapshot, currentChartContext, loading, currentError ->
        ForecastUiInputs(
            place = place,
            snapshot = snapshot,
            chartContext = currentChartContext,
            isLoading = loading,
            errorMessage = currentError,
        )
    }

    val uiState: StateFlow<ForecastUiState> = combine(
        uiInputs,
        forecastModelRepository.selectedModel,
        placeRepository.observeFavoritePlaces(),
    ) { inputs, currentModel, favorites ->
        val place = inputs.place
        val snapshot = inputs.snapshot
        val currentChartContext = inputs.chartContext
        val loading = inputs.isLoading
        val currentError = inputs.errorMessage

        val loadedForecastDays = snapshot?.days?.size ?: 0
        val availableForecastDays = (snapshot?.resolvedModel ?: currentModel).visibleForecastDays()
        val displayedForecastDays = exposedForecastDayCount(
            loadedForecastDays = loadedForecastDays,
            selectedDayIndex = currentChartContext.selectedDayIndex,
            maxForecastDays = availableForecastDays,
        )
        val dayChips = buildDisplayedDayChips(
            loadedDays = snapshot?.days.orEmpty(),
            displayedDayCount = displayedForecastDays,
        )
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
            favoritePlaces = favorites,
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
                        cancelForecastLoad()
                        return@collect
                    }

                    val requiredForecastDays = requestedForecastDaysForDayIndex(
                        dayIndex = selectedDayIndex.value,
                        maxForecastDays = model.visibleForecastDays(),
                    )
                    if (forecastRepository.isCached(
                            placeId = place.id,
                            model = model,
                            minimumForecastDays = requiredForecastDays,
                        )
                    ) {
                        cancelForecastLoad()
                        return@collect
                    }

                    startForecastLoad(
                        place = place,
                        model = model,
                        forecastDays = requiredForecastDays,
                    )
                }
        }
    }

    fun selectDay(index: Int) {
        selectedDayIndex.value = index
        val place = selectedPlace.value ?: return
        val model = forecastModelRepository.selectedModel.value
        val requiredForecastDays = requestedForecastDaysForDayIndex(
            dayIndex = index,
            maxForecastDays = (uiState.value.resolvedModel ?: model).visibleForecastDays(),
        )
        if (forecastRepository.isCached(
                placeId = place.id,
                model = model,
                minimumForecastDays = requiredForecastDays,
            )
        ) {
            cancelForecastLoad()
            return
        }
        errorMessage.value = null
        startForecastLoad(
            place = place,
            model = model,
            forecastDays = requiredForecastDays,
        )
    }

    fun selectForecastMode(mode: ForecastMode) {
        forecastModeRepository.selectMode(mode)
    }

    fun updateChartTopAltitude(topAltitudeKm: Float) {
        chartViewport.update { currentViewport ->
            currentViewport.withVisibleTopAltitudeKm(topAltitudeKm)
        }
        forecastViewportRepository.setVisibleTopAltitudeKm(topAltitudeKm)
    }

    fun updateForecastLocation(latitude: Double, longitude: Double) {
        val matchingFavorite = uiState.value.favoritePlaces.find { fav ->
            fav.isNearby(latitude, longitude)
        }
        val newPlace = matchingFavorite ?: SavedPlace.fromCoordinates(latitude, longitude)
        viewModelScope.launch {
            placeRepository.saveAndSelectPlace(newPlace)
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
        val requiredForecastDays = requestedForecastDaysForDayIndex(
            dayIndex = selectedDayIndex.value,
            maxForecastDays = (uiState.value.resolvedModel ?: model).visibleForecastDays(),
        )
        errorMessage.value = null
        startForecastLoad(
            place = place,
            model = model,
            forecastDays = requiredForecastDays,
            forceRefresh = true,
        )
    }

    private fun startForecastLoad(
        place: SavedPlace,
        model: ForecastModel,
        forecastDays: Int,
        forceRefresh: Boolean = false,
    ) {
        val generation = ++forecastLoadGeneration
        forecastLoadJob?.cancel()
        forecastLoadJob = viewModelScope.launch {
            isLoading.value = true
            Log.i(
                FORECAST_LOG_TAG,
                "Loading forecast: place=${place.name} model=${model.apiName} days=$forecastDays forceRefresh=$forceRefresh",
            )
            try {
                loadForecastWindow(
                    place = place,
                    model = model,
                    forecastDays = forecastDays,
                    forceRefresh = forceRefresh,
                )
                Log.i(
                    FORECAST_LOG_TAG,
                    "Forecast load finished: place=${place.name} model=${model.apiName} days=$forecastDays",
                )
            } catch (throwable: CancellationException) {
                Log.i(
                    FORECAST_LOG_TAG,
                    "Forecast load cancelled: place=${place.name} model=${model.apiName}",
                )
                throw throwable
            } finally {
                if (generation == forecastLoadGeneration) {
                    isLoading.value = false
                }
            }
        }
    }

    private fun cancelForecastLoad() {
        forecastLoadGeneration++
        forecastLoadJob?.cancel()
        forecastLoadJob = null
        isLoading.value = false
    }

    private suspend fun loadForecastWindow(
        place: SavedPlace,
        model: ForecastModel,
        forecastDays: Int,
        forceRefresh: Boolean = false,
    ) {
        runCatching {
            forecastRepository.loadForecast(
                place = place,
                forceRefresh = forceRefresh,
                model = model,
                forecastDays = forecastDays,
            )
        }.onFailure { throwable ->
            if (throwable is CancellationException) {
                throw throwable
            }
            val msg = throwable.message ?: "Unable to load forecast right now."
            Log.e(
                FORECAST_LOG_TAG,
                "Forecast load failed; screen error=\"$msg\" place=${place.name} model=${model.apiName} days=$forecastDays forceRefresh=$forceRefresh",
                throwable,
            )
            errorMessage.value = msg
            _networkErrorEvent.tryEmit(msg)
        }
    }
}

private const val FORECAST_LOG_TAG = "ForecastViewModel"

private data class ForecastChartContext(
    val selectedForecastMode: ForecastMode,
    val selectedDayIndex: Int,
    val chartViewport: ForecastChartViewport,
    val stuveHour: Int = 12,
)

private data class ForecastUiInputs(
    val place: SavedPlace?,
    val snapshot: ForecastSnapshot?,
    val chartContext: ForecastChartContext,
    val isLoading: Boolean,
    val errorMessage: String?,
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
                    "Loading thermic forecast for ${place.name}."
                } else {
                    "Forecast content for ${place.name} will appear here."
                }
            }
            ForecastMode.STUVE -> {
                if (isLoading) {
                    "Loading stuve forecast for ${place.name}."
                } else {
                    "Stuve forecast content for ${place.name} will appear here."
                }
            }
            ForecastMode.WIND -> {
                if (isLoading) {
                    "Loading wind forecast for ${place.name}."
                } else {
                    "Wind forecast content for ${place.name} will appear here."
                }
            }
            ForecastMode.CLOUD -> {
                if (isLoading) {
                    "Loading cloud forecast for ${place.name}."
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
    return days.take(MAX_FORECAST_DAYS).mapIndexed { index, day ->
        ForecastDayChipUiModel(
            title = if (index == 0) "Today" else formatDayTitle(day.date),
            subtitle = formatDaySubtitle(day.date, index),
        )
    }
}

private fun buildDisplayedDayChips(
    loadedDays: List<DailyForecast>,
    displayedDayCount: Int,
): List<ForecastDayChipUiModel> {
    val loadedDayChips = buildDayChips(loadedDays).take(displayedDayCount)
    if (loadedDayChips.size >= displayedDayCount) {
        return loadedDayChips
    }

    return buildList(displayedDayCount) {
        addAll(loadedDayChips)
        for (index in loadedDayChips.size until displayedDayCount) {
            add(placeholderDayChip(index))
        }
    }
}

private fun placeholderDayChips(dayCount: Int = INITIAL_FORECAST_DAYS): List<ForecastDayChipUiModel> {
    return List(dayCount, ::placeholderDayChip)
}

private fun placeholderDayChip(index: Int): ForecastDayChipUiModel {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, index)
    }
    return ForecastDayChipUiModel(
        title = if (index == 0) "Today" else SimpleDateFormat("EEE", Locale.US).format(calendar.time),
        subtitle = SimpleDateFormat("d MMM", Locale.US).format(calendar.time),
    )
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

private fun ForecastModel.visibleForecastDays(): Int {
    return availableForecastDays.coerceAtMost(MAX_FORECAST_DAYS)
}
