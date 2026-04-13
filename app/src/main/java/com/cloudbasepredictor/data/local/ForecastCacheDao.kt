package com.cloudbasepredictor.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ForecastCacheDao {

    @Query("SELECT * FROM forecast_cache WHERE placeId = :placeId AND modelApiName = :modelApiName LIMIT 1")
    suspend fun getCachedForecast(placeId: String, modelApiName: String): CachedForecastEntity?

    @Upsert
    suspend fun upsertForecast(entity: CachedForecastEntity)

    @Query("DELETE FROM forecast_cache WHERE fetchedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM forecast_cache")
    suspend fun deleteAll()
}
