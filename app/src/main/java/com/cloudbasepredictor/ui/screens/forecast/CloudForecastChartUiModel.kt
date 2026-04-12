package com.cloudbasepredictor.ui.screens.forecast

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * UI model for the cloud forecast chart.
 *
 * Shows three cloud layers (low, mid, high) as percentage coverage per hour,
 * plus precipitation probability and amount below the chart.
 */
data class CloudForecastChartUiModel(
    /** Local hours of day (e.g. 6..22). */
    val hours: List<Int>,
    /** Cloud coverage per hour, split into low / mid / high layers. */
    val layers: List<CloudLayerUiModel>,
    /** Precipitation forecast per hour. */
    val precipitation: List<CloudPrecipitationUiModel>,
)

data class CloudLayerUiModel(
    /** Local hour of day (0–23). */
    val hour: Int,
    /** Low cloud coverage (0–3 km AGL), percent (0–100). */
    val lowCloudPercent: Float,
    /** Mid cloud coverage (3–8 km AGL), percent (0–100). */
    val midCloudPercent: Float,
    /** High cloud coverage (above 8 km AGL), percent (0–100). */
    val highCloudPercent: Float,
)

data class CloudPrecipitationUiModel(
    /** Local hour of day (0–23). */
    val hour: Int,
    /** Probability of precipitation (>0.1 mm) in preceding hour, percent (0–100). */
    val probabilityPercent: Float,
    /** Precipitation amount in preceding hour, mm (millimetres). */
    val amountMm: Float,
)

internal fun buildPlaceholderCloudForecastChart(
    dayIndex: Int = 0,
): CloudForecastChartUiModel {
    val hours = (6..22).toList()
    val dayPhase = dayIndex * 0.6f

    val layers = hours.map { hour ->
        val timeNorm = (hour - 6f) / 16f
        val solarPeak = (1f - abs(hour - 14f) / 8f).coerceIn(0f, 1f)
        val wave = (sin(timeNorm * PI.toFloat() * 2f + dayPhase) + 1f) / 2f

        CloudLayerUiModel(
            hour = hour,
            lowCloudPercent = ((0.2f + wave * 0.4f + (1f - solarPeak) * 0.3f) * 100f)
                .coerceIn(0f, 100f),
            midCloudPercent = ((0.15f + sin(timeNorm * PI.toFloat() * 1.5f + dayPhase + 1f)
                .coerceIn(0f, 1f) * 0.5f) * 100f).coerceIn(0f, 100f),
            highCloudPercent = ((0.1f + sin(timeNorm * PI.toFloat() + dayPhase + 2f)
                .coerceIn(0f, 1f) * 0.6f) * 100f).coerceIn(0f, 100f),
        )
    }

    val precipitation = hours.map { hour ->
        val timeNorm = (hour - 6f) / 16f
        val wave = (sin(timeNorm * PI.toFloat() * 2.5f + dayPhase + 1f) + 1f) / 2f
        val probability = (wave * 60f + (dayIndex % 3) * 10f).coerceIn(0f, 100f)
        val amount = if (probability > 30f) {
            ((probability - 30f) / 70f * 5f).coerceIn(0f, 8f)
        } else {
            0f
        }

        CloudPrecipitationUiModel(
            hour = hour,
            probabilityPercent = probability,
            amountMm = amount,
        )
    }

    return CloudForecastChartUiModel(
        hours = hours,
        layers = layers,
        precipitation = precipitation,
    )
}
