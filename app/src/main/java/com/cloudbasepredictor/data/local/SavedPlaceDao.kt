package com.cloudbasepredictor.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY name ASC")
    fun observeSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Upsert
    suspend fun upsert(place: SavedPlaceEntity)
}
