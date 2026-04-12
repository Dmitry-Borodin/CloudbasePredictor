package com.cloudbasepredictor.data.forecast

import com.cloudbasepredictor.data.remote.OpenMeteoRemoteDataSource
import com.cloudbasepredictor.di.IoDispatcher
import com.cloudbasepredictor.model.ForecastModel
import com.cloudbasepredictor.model.ForecastSnapshot
import com.cloudbasepredictor.model.SavedPlace
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

    suspend fun loadForecast(
        place: SavedPlace,
        forceRefresh: Boolean = false,
        model: ForecastModel = ForecastModel.BEST_MATCH,
    )
}

@Singleton
class InMemoryForecastRepository @Inject constructor(
    private val openMeteoRemoteDataSource: OpenMeteoRemoteDataSource,
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

    override suspend fun loadForecast(
        place: SavedPlace,
        forceRefresh: Boolean,
        model: ForecastModel,
    ) = withContext(ioDispatcher) {
        val key = cacheKey(place.id, model)
        if (!forceRefresh && cachedForecasts.value.containsKey(key)) {
            return@withContext
        }

        val (resolvedModel, hourlyData) = openMeteoRemoteDataSource.getHourlyForecastWithFallback(
            latitude = place.latitude,
            longitude = place.longitude,
            requestedModel = model,
        )

        val snapshot = ForecastSnapshot(
            days = hourlyData.dailyForecasts,
            updatedAtUtcMillis = System.currentTimeMillis(),
            hourlyData = hourlyData,
            resolvedModel = resolvedModel,
        )

        cachedForecasts.value = cachedForecasts.value + (key to snapshot)
    }

    private fun cacheKey(placeId: String, model: ForecastModel): String {
        return "$placeId:${model.apiName}"
    }
}
