package com.cloudbasepredictor.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.cloudbasepredictor.data.datasource.DataSourcePreference
import com.cloudbasepredictor.data.datasource.DataSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSourceRepository: DataSourceRepository,
) : ViewModel() {
    val dataSourcePreference: StateFlow<DataSourcePreference> = dataSourceRepository.preference

    fun setDataSource(preference: DataSourcePreference) {
        dataSourceRepository.setPreference(preference)
    }
}
