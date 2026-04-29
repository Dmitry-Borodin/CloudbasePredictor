package com.cloudbasepredictor.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY name ASC")
    fun observeSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavoritePlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE isFavorite = 1 ORDER BY name ASC")
    suspend fun getFavoritePlaces(): List<SavedPlaceEntity>

    @Query("SELECT * FROM saved_places WHERE id = :id")
    suspend fun findById(id: String): SavedPlaceEntity?

    @Upsert
    suspend fun upsert(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteById(id: String)
}
