package com.cloudbasepredictor.data.forecast

import com.cloudbasepredictor.data.datasource.DataSourcePreference
import com.cloudbasepredictor.data.datasource.DataSourceRepository
import com.cloudbasepredictor.data.remote.OpenMeteoRemoteDataSource
import com.cloudbasepredictor.di.IoDispatcher
import com.cloudbasepredictor.model.DailyForecast
import com.cloudbasepredictor.model.ForecastModel
import com.cloudbasepredictor.model.ForecastSnapshot
import com.cloudbasepredictor.model.SavedPlace
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ForecastRepository {
    fun observeForecast(placeId: String, model: ForecastModel): Flow<ForecastSnapshot?>

    fun isCached(placeId: String, model: ForecastModel): Boolean

    /** Returns true if the cache already has the full 7-day forecast. */
    fun isFullyCached(placeId: String, model: ForecastModel): Boolean

    suspend fun loadForecast(
        place: SavedPlace,
        forceRefresh: Boolean = false,
        model: ForecastModel = ForecastModel.BEST_MATCH,
        forecastDays: Int = 7,
    )
}

@Singleton
class InMemoryForecastRepository @Inject constructor(
    private val openMeteoRemoteDataSource: OpenMeteoRemoteDataSource,
    private val dataSourceRepository: DataSourceRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ForecastRepository {
    private val cachedForecasts = MutableStateFlow<Map<String, ForecastSnapshot>>(emptyMap())

    override fun observeForecast(placeId: String, model: ForecastModel): Flow<ForecastSnapshot?> {
        return cachedForecasts
            .map { forecasts -> forecasts[cacheKey(placeId, model)] }
            .distinctUntilChanged()
    }

    override fun isCached(placeId: String, model: ForecastModel): Boolean {
        return cachedForecasts.value.containsKey(cacheKey(placeId, model))
    }

    override fun isFullyCached(placeId: String, model: ForecastModel): Boolean {
        val snapshot = cachedForecasts.value[cacheKey(placeId, model)] ?: return false
        return snapshot.forecastDays >= 7
    }

    override suspend fun loadForecast(
        place: SavedPlace,
        forceRefresh: Boolean,
        model: ForecastModel,
        forecastDays: Int,
    ) = withContext(ioDispatcher) {
        val key = cacheKey(place.id, model)
        if (!forceRefresh && cachedForecasts.value.containsKey(key)) {
            val existing = cachedForecasts.value[key]
            if (existing != null && existing.forecastDays >= forecastDays) {
                return@withContext
            }
        }

        val snapshot = if (dataSourceRepository.preference.value == DataSourcePreference.FAKE) {
            ForecastSnapshot(
                days = generateFakeDays(forecastDays),
                updatedAtUtcMillis = System.currentTimeMillis(),
                resolvedModel = model,
                forecastDays = forecastDays,
            )
        } else {
            val (resolvedModel, hourlyData) = openMeteoRemoteDataSource.getHourlyForecastWithFallback(
                latitude = place.latitude,
                longitude = place.longitude,
                requestedModel = model,
                forecastDays = forecastDays,
            )
            ForecastSnapshot(
                days = hourlyData.dailyForecasts,
                updatedAtUtcMillis = System.currentTimeMillis(),
                hourlyData = hourlyData,
                resolvedModel = resolvedModel,
                forecastDays = forecastDays,
            )
        }

        cachedForecasts.value = cachedForecasts.value + (key to snapshot)
    }

    private fun cacheKey(placeId: String, model: ForecastModel): String {
        return "$placeId:${model.apiName}"
    }
}

private fun generateFakeDays(count: Int): List<DailyForecast> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance(Locale.US)
    return List(count) { index ->
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, index)
        DailyForecast(
            date = dateFormat.format(calendar.time),
            maxTemperatureCelsius = 18.0 + index,
            minTemperatureCelsius = 9.0 + (index * 0.6),
            weatherCode = if (index % 2 == 0) 1 else 3,
        )
    }
}
