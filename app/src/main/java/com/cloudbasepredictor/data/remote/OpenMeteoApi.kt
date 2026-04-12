package com.cloudbasepredictor.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
        @Query("forecast_days") forecastDays: Int = 14,
        @Query("timezone") timezone: String = "auto",
    ): OpenMeteoForecastResponse

    /**
     * Fetch hourly forecast including surface and pressure-level variables.
     *
     * Use [models] to select a specific weather model (e.g. "icon_d2", "icon_eu").
     * Pass "best_match" or omit for automatic model selection.
     */
    @GET("v1/forecast")
    suspend fun getHourlyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = HOURLY_VARIABLES,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weather_code",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "auto",
        @Query("models") models: String? = null,
    ): OpenMeteoHourlyForecastResponse
}

/**
 * Surface + pressure-level variables requested for chart construction.
 *
 * Pressure levels chosen for 0–6 km AGL: 1000, 950, 925, 900, 850, 800, 700, 600, 500 hPa.
 */
private const val HOURLY_VARIABLES =
    "temperature_2m,dew_point_2m," +
    "cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
    "precipitation,precipitation_probability," +
    "wind_speed_10m,wind_direction_10m," +
    "cape," +
    "temperature_1000hPa,temperature_950hPa,temperature_925hPa,temperature_900hPa," +
    "temperature_850hPa,temperature_800hPa,temperature_700hPa,temperature_600hPa,temperature_500hPa," +
    "dew_point_1000hPa,dew_point_950hPa,dew_point_925hPa,dew_point_900hPa," +
    "dew_point_850hPa,dew_point_800hPa,dew_point_700hPa,dew_point_600hPa,dew_point_500hPa," +
    "wind_speed_1000hPa,wind_speed_950hPa,wind_speed_925hPa,wind_speed_900hPa," +
    "wind_speed_850hPa,wind_speed_800hPa,wind_speed_700hPa,wind_speed_600hPa,wind_speed_500hPa," +
    "wind_direction_1000hPa,wind_direction_950hPa,wind_direction_925hPa,wind_direction_900hPa," +
    "wind_direction_850hPa,wind_direction_800hPa,wind_direction_700hPa,wind_direction_600hPa,wind_direction_500hPa," +
    "geopotential_height_1000hPa,geopotential_height_950hPa,geopotential_height_925hPa,geopotential_height_900hPa," +
    "geopotential_height_850hPa,geopotential_height_800hPa,geopotential_height_700hPa,geopotential_height_600hPa,geopotential_height_500hPa"
