package com.cloudbasepredictor

import android.app.Application
import android.util.Log
import com.cloudbasepredictor.data.forecast.ForecastRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import javax.inject.Inject

@HiltAndroidApp
class CloudbasePredictorApplication : Application() {

    @Inject
    lateinit var forecastRepository: ForecastRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Enable MapLibre ambient tile cache (200 MB).
        MapLibre.getInstance(this)
        org.maplibre.android.offline.OfflineManager.getInstance(this)
            .setMaximumAmbientCacheSize(
                200L * 1024 * 1024,
                object : org.maplibre.android.offline.OfflineManager.FileSourceCallback {
                    override fun onSuccess() {
                        Log.d("CloudbaseApp", "Ambient tile cache set to 200 MB")
                    }
                    override fun onError(message: String) {
                        Log.e("CloudbaseApp", "Failed to set ambient cache size: $message")
                    }
                },
            )

        // Schedule DB cleanup after 10 seconds.
        appScope.launch {
            delay(10_000L)
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            try {
                forecastRepository.cleanupOldForecasts(oneDayAgo)
            } catch (e: Exception) {
                Log.e("CloudbaseApp", "Forecast cleanup failed", e)
            }
        }
    }
}
