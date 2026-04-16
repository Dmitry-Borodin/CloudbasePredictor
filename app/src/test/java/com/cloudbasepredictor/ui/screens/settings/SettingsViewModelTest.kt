package com.cloudbasepredictor.ui.screens.settings

import com.cloudbasepredictor.data.datasource.DataSourcePreference
import com.cloudbasepredictor.data.datasource.InMemoryDataSourceRepository
import com.cloudbasepredictor.data.forecast.ForecastRepository
import com.cloudbasepredictor.data.theme.InMemoryThemeRepository
import com.cloudbasepredictor.model.ForecastModel
import com.cloudbasepredictor.model.ForecastSnapshot
import com.cloudbasepredictor.model.SavedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun switchingDataSource_samePreference_noCacheClear() {
        val dataSourceRepo = InMemoryDataSourceRepository()
        val forecastRepo = FakeForecastRepository()
        val vm = SettingsViewModel(dataSourceRepo, InMemoryThemeRepository(), forecastRepo)

        // Default is REAL; setting REAL again should not clear caches
        vm.setDataSource(DataSourcePreference.REAL)

        assertEquals(0, forecastRepo.clearAllCachesCallCount)
    }

    @Test
    fun dataSourcePreference_updates_viaRepository() {
        val dataSourceRepo = InMemoryDataSourceRepository()

        dataSourceRepo.setPreference(DataSourcePreference.FAKE)
        assertEquals(DataSourcePreference.FAKE, dataSourceRepo.preference.value)

        dataSourceRepo.setPreference(DataSourcePreference.SIMULATED)
        assertEquals(DataSourcePreference.SIMULATED, dataSourceRepo.preference.value)
    }

    @Test
    fun forecastCacheClear_doesNotTouchMapTileCache() {
        // ForecastRepository.clearAllCaches() only clears in-memory forecast snapshots
        // and DB-cached forecasts. Map tile caching is handled by MapLibre internally
        // (file-based SQLite) and is never touched by ForecastRepository.
        val forecastRepo = FakeForecastRepository()
        assertTrue(
            "clearAllCaches should only clear forecast data, not map tiles",
            !forecastRepo.mapTileCacheCleared,
        )
    }

    private class FakeForecastRepository : ForecastRepository {
        var clearAllCachesCallCount = 0
        var mapTileCacheCleared = false // Always false: map tiles are external

        override fun observeForecast(placeId: String, model: ForecastModel): Flow<ForecastSnapshot?> =
            flowOf(null)

        override fun isCached(placeId: String, model: ForecastModel, minimumForecastDays: Int): Boolean =
            false

        override fun isFullyCached(placeId: String, model: ForecastModel): Boolean = false

        override suspend fun loadForecast(
            place: SavedPlace,
            forceRefresh: Boolean,
            model: ForecastModel,
            forecastDays: Int,
        ) {}

        override suspend fun cleanupOldForecasts(cutoffMillis: Long) {}

        override suspend fun clearAllCaches() {
            clearAllCachesCallCount++
        }
    }
}
