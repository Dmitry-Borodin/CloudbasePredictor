package com.cloudbasepredictor.data.remote

import com.cloudbasepredictor.model.DailyForecast
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
}
