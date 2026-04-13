package com.cloudbasepredictor.data.local

import androidx.room.Entity

@Entity(
    tableName = "forecast_cache",
    primaryKeys = ["placeId", "modelApiName"],
)
data class CachedForecastEntity(
    val placeId: String,
    val modelApiName: String,
    val resolvedModelApiName: String,
    val forecastDays: Int,
    val hourlyDataJson: String,
    val fetchedAtMillis: Long,
    val nextExpectedUpdateMillis: Long,
)
