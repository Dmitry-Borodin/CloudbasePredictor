package com.cloudbasepredictor.data.place

import com.cloudbasepredictor.data.local.SavedPlaceDao
import com.cloudbasepredictor.data.local.SavedPlaceEntity
import com.cloudbasepredictor.di.IoDispatcher
import com.cloudbasepredictor.model.SavedPlace
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface PlaceRepository {
    val selectedPlace: StateFlow<SavedPlace?>

    fun observeSavedPlaces(): Flow<List<SavedPlace>>

    suspend fun saveAndSelectPlace(place: SavedPlace)
}

@Singleton
class DefaultPlaceRepository @Inject constructor(
    private val savedPlaceDao: SavedPlaceDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlaceRepository {
    private val mutableSelectedPlace = MutableStateFlow<SavedPlace?>(null)

    override val selectedPlace: StateFlow<SavedPlace?> = mutableSelectedPlace.asStateFlow()

    override fun observeSavedPlaces(): Flow<List<SavedPlace>> {
        return savedPlaceDao.observeSavedPlaces().map { entities ->
            entities.map(SavedPlaceEntity::toDomainModel)
        }
    }

    override suspend fun saveAndSelectPlace(place: SavedPlace) = withContext(ioDispatcher) {
        savedPlaceDao.upsert(
            SavedPlaceEntity(
                id = place.id,
                name = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                defaultModel = place.defaultModel,
            )
        )
        mutableSelectedPlace.value = place
    }
}

private fun SavedPlaceEntity.toDomainModel(): SavedPlace {
    return SavedPlace(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        defaultModel = defaultModel,
    )
}
