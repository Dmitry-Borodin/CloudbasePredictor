package com.cloudbasepredictor.ui.screens.forecast

data class ForecastChartViewport(
    val visibleTopAltitudeKm: Float = DEFAULT_TOP_ALTITUDE_KM,
    val defaultTopAltitudeKm: Float = DEFAULT_TOP_ALTITUDE_KM,
    val maxTopAltitudeKm: Float = MAX_TOP_ALTITUDE_KM,
) {
    fun withVisibleTopAltitudeKm(topAltitudeKm: Float): ForecastChartViewport {
        return copy(
            visibleTopAltitudeKm = sanitizeTopAltitudeKm(
                topAltitudeKm = topAltitudeKm,
                minTopAltitudeKm = defaultTopAltitudeKm,
                maxTopAltitudeKm = maxTopAltitudeKm,
            ),
        )
    }
}

internal fun zoomedTopAltitudeKm(
    currentTopAltitudeKm: Float,
    zoomChange: Float,
    minTopAltitudeKm: Float = DEFAULT_TOP_ALTITUDE_KM,
    maxTopAltitudeKm: Float = MAX_TOP_ALTITUDE_KM,
): Float {
    if (!zoomChange.isFinite() || zoomChange <= 0f) {
        return sanitizeTopAltitudeKm(
            topAltitudeKm = currentTopAltitudeKm,
            minTopAltitudeKm = minTopAltitudeKm,
            maxTopAltitudeKm = maxTopAltitudeKm,
        )
    }

    return sanitizeTopAltitudeKm(
        topAltitudeKm = currentTopAltitudeKm / zoomChange,
        minTopAltitudeKm = minTopAltitudeKm,
        maxTopAltitudeKm = maxTopAltitudeKm,
    )
}

internal fun sanitizeTopAltitudeKm(
    topAltitudeKm: Float,
    minTopAltitudeKm: Float = DEFAULT_TOP_ALTITUDE_KM,
    maxTopAltitudeKm: Float = MAX_TOP_ALTITUDE_KM,
): Float {
    if (!topAltitudeKm.isFinite()) {
        return minTopAltitudeKm
    }

    return topAltitudeKm.coerceIn(minTopAltitudeKm, maxTopAltitudeKm)
}

internal const val DEFAULT_TOP_ALTITUDE_KM = 4.5f
internal const val MAX_TOP_ALTITUDE_KM = 7f
