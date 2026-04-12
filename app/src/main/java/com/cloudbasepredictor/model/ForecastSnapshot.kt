package com.cloudbasepredictor.model

import com.cloudbasepredictor.data.remote.HourlyForecastData

data class ForecastSnapshot(
    val days: List<DailyForecast>,
    val updatedAtUtcMillis: Long,
    /** Hourly + pressure-level data. Null when only the lightweight daily API was used. */
    val hourlyData: HourlyForecastData? = null,
    /** The weather model that was actually used (may differ from requested after fallback). */
    val resolvedModel: ForecastModel? = null,
    /** Number of forecast days loaded (2 for quick load, 7 for full). */
    val forecastDays: Int = 7,
)
