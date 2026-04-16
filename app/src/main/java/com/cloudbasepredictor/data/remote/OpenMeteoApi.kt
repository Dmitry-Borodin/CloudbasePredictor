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
 * Pressure levels: 1000, 975, 950, 925, 900, 875, 850, 800, 750, 700, 650, 600, 550, 500 hPa.
 * Extra levels (975, 875, 750, 650, 550) added for granularity below 4 km.
 */
private const val HOURLY_VARIABLES =
    "temperature_2m,dew_point_2m," +
    "cloud_cover_low,cloud_cover_mid,cloud_cover_high," +
    "precipitation,precipitation_probability," +
    "wind_speed_10m,wind_direction_10m," +
    "cape,freezing_level_height," +
    "surface_pressure,shortwave_radiation,is_day," +
    "temperature_1000hPa,temperature_975hPa,temperature_950hPa,temperature_925hPa,temperature_900hPa," +
    "temperature_875hPa,temperature_850hPa,temperature_800hPa,temperature_750hPa," +
    "temperature_700hPa,temperature_650hPa,temperature_600hPa,temperature_550hPa,temperature_500hPa," +
    "dew_point_1000hPa,dew_point_975hPa,dew_point_950hPa,dew_point_925hPa,dew_point_900hPa," +
    "dew_point_875hPa,dew_point_850hPa,dew_point_800hPa,dew_point_750hPa," +
    "dew_point_700hPa,dew_point_650hPa,dew_point_600hPa,dew_point_550hPa,dew_point_500hPa," +
    "wind_speed_1000hPa,wind_speed_975hPa,wind_speed_950hPa,wind_speed_925hPa,wind_speed_900hPa," +
    "wind_speed_875hPa,wind_speed_850hPa,wind_speed_800hPa,wind_speed_750hPa," +
    "wind_speed_700hPa,wind_speed_650hPa,wind_speed_600hPa,wind_speed_550hPa,wind_speed_500hPa," +
    "wind_direction_1000hPa,wind_direction_975hPa,wind_direction_950hPa,wind_direction_925hPa,wind_direction_900hPa," +
    "wind_direction_875hPa,wind_direction_850hPa,wind_direction_800hPa,wind_direction_750hPa," +
    "wind_direction_700hPa,wind_direction_650hPa,wind_direction_600hPa,wind_direction_550hPa,wind_direction_500hPa," +
    "geopotential_height_1000hPa,geopotential_height_975hPa,geopotential_height_950hPa,geopotential_height_925hPa,geopotential_height_900hPa," +
    "geopotential_height_875hPa,geopotential_height_850hPa,geopotential_height_800hPa,geopotential_height_750hPa," +
    "geopotential_height_700hPa,geopotential_height_650hPa,geopotential_height_600hPa,geopotential_height_550hPa,geopotential_height_500hPa"
