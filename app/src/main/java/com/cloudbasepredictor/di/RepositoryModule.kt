package com.cloudbasepredictor.di

import com.cloudbasepredictor.data.forecast.ForecastRepository
import com.cloudbasepredictor.data.forecast.ForecastModeRepository
import com.cloudbasepredictor.data.forecast.InMemoryForecastRepository
import com.cloudbasepredictor.data.forecast.InMemoryForecastModeRepository
import com.cloudbasepredictor.data.place.DefaultPlaceRepository
import com.cloudbasepredictor.data.place.PlaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPlaceRepository(
        repository: DefaultPlaceRepository,
    ): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindForecastRepository(
        repository: InMemoryForecastRepository,
    ): ForecastRepository

    @Binds
    @Singleton
    abstract fun bindForecastModeRepository(
        repository: InMemoryForecastModeRepository,
    ): ForecastModeRepository
}
