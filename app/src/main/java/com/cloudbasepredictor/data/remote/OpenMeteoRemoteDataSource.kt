package com.cloudbasepredictor.data.remote

import com.cloudbasepredictor.model.DailyForecast
import com.cloudbasepredictor.model.ForecastModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoRemoteDataSource @Inject constructor(
    private val openMeteoApi: OpenMeteoApi,
) {
    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
    ): List<DailyForecast> {
        return openMeteoApi.getForecast(
            latitude = latitude,
            longitude = longitude,
        ).toDomainModels()
    }

    /**
     * Fetch hourly + pressure-level forecast for a specific weather model.
     *
     * If [model] is [ForecastModel.BEST_MATCH], the `models` query parameter is omitted
     * so Open-Meteo auto-selects the best model for the location.
     *
     * @return [HourlyForecastData] ready for chart construction.
     * @throws retrofit2.HttpException on API error (e.g. 400 if model unavailable for region).
     */
    suspend fun getHourlyForecast(
        latitude: Double,
        longitude: Double,
        model: ForecastModel,
        forecastDays: Int = 7,
    ): HourlyForecastData {
        val modelParam = if (model == ForecastModel.BEST_MATCH) null else model.apiName
        return openMeteoApi.getHourlyForecast(
            latitude = latitude,
            longitude = longitude,
            models = modelParam,
            forecastDays = forecastDays,
        ).toHourlyForecastData()
    }

    /**
     * Fetch hourly forecast, falling back through the model's fallback chain
     * if the requested model is not available for that location.
     *
     * Returns a pair of (actualModel, forecastData).
     */
    suspend fun getHourlyForecastWithFallback(
        latitude: Double,
        longitude: Double,
        requestedModel: ForecastModel,
        forecastDays: Int = 7,
    ): Pair<ForecastModel, HourlyForecastData> {
        var currentModel = requestedModel
        while (true) {
            try {
                val data = getHourlyForecast(latitude, longitude, currentModel, forecastDays)
                return currentModel to data
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 400 && currentModel != ForecastModel.BEST_MATCH) {
                    currentModel = ForecastModel.fallbackFor(currentModel)
                } else {
                    throw e
                }
            }
        }
    }
}
