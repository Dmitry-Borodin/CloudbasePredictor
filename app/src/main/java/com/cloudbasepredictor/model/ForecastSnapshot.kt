package com.cloudbasepredictor.model

data class ForecastSnapshot(
    val days: List<DailyForecast>,
    val updatedAtUtcMillis: Long,
)
