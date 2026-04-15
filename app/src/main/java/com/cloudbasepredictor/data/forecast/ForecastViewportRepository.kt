package com.cloudbasepredictor.data.forecast

import android.content.SharedPreferences
import com.cloudbasepredictor.ui.screens.forecast.DEFAULT_TOP_ALTITUDE_KM
import com.cloudbasepredictor.ui.screens.forecast.sanitizeTopAltitudeKm
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ForecastViewportRepository {
    val visibleTopAltitudeKm: StateFlow<Float>
    fun setVisibleTopAltitudeKm(value: Float)
}

@Singleton
class SharedPrefsForecastViewportRepository @Inject constructor(
    private val prefs: SharedPreferences,
) : ForecastViewportRepository {
    private val mutableAltitude = MutableStateFlow(loadFromPrefs())

    override val visibleTopAltitudeKm: StateFlow<Float> = mutableAltitude.asStateFlow()

    override fun setVisibleTopAltitudeKm(value: Float) {
        val sanitized = sanitizeTopAltitudeKm(value)
        mutableAltitude.value = sanitized
        prefs.edit().putFloat(KEY_VISIBLE_TOP_ALTITUDE_KM, sanitized).apply()
    }

    private fun loadFromPrefs(): Float {
        val stored = prefs.getFloat(KEY_VISIBLE_TOP_ALTITUDE_KM, DEFAULT_TOP_ALTITUDE_KM)
        return sanitizeTopAltitudeKm(stored)
    }

    private companion object {
        const val KEY_VISIBLE_TOP_ALTITUDE_KM = "forecast_visible_top_altitude_km"
    }
}
