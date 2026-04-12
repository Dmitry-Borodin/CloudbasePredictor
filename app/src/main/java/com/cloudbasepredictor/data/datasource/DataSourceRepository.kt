package com.cloudbasepredictor.data.datasource

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class DataSourcePreference {
    FAKE,
    REAL,
}

interface DataSourceRepository {
    val preference: StateFlow<DataSourcePreference>
    fun setPreference(preference: DataSourcePreference)
}

@Singleton
class InMemoryDataSourceRepository @Inject constructor() : DataSourceRepository {
    override val preference = MutableStateFlow(DataSourcePreference.FAKE)

    override fun setPreference(preference: DataSourcePreference) {
        this.preference.value = preference
    }
}
