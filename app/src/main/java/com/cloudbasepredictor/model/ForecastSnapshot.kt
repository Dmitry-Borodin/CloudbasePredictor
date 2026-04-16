package com.cloudbasepredictor.model

import com.cloudbasepredictor.data.forecast.MAX_FORECAST_DAYS
import com.cloudbasepredictor.data.remote.HourlyForecastData

data class ForecastSnapshot(
    val days: List<DailyForecast>,
    val updatedAtUtcMillis: Long,
    /** Hourly + pressure-level data. Null when only the lightweight daily API was used. */
    val hourlyData: HourlyForecastData? = null,
    /** The weather model that was actually used (may differ from requested after fallback). */
    val resolvedModel: ForecastModel? = null,
    /** Number of forecast days currently cached for this place/model. */
    val forecastDays: Int = MAX_FORECAST_DAYS,
    /** Estimated UTC millis when the model run started, based on model update interval. */
    val modelGeneratedAtMillis: Long? = null,
)
