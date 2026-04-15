package com.cloudbasepredictor.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbasepredictor.data.datasource.DataSourcePreference
import com.cloudbasepredictor.data.datasource.DataSourceRepository
import com.cloudbasepredictor.data.forecast.ForecastRepository
import com.cloudbasepredictor.data.theme.ThemePreference
import com.cloudbasepredictor.data.theme.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSourceRepository: DataSourceRepository,
    private val themeRepository: ThemeRepository,
    private val forecastRepository: ForecastRepository,
) : ViewModel() {
    val dataSourcePreference: StateFlow<DataSourcePreference> = dataSourceRepository.preference
    val themePreference: StateFlow<ThemePreference> = themeRepository.preference

    fun setDataSource(preference: DataSourcePreference) {
        val previousPreference = dataSourceRepository.preference.value
        dataSourceRepository.setPreference(preference)
        if (previousPreference != preference) {
            viewModelScope.launch {
                forecastRepository.clearAllCaches()
            }
        }
    }

    fun setTheme(preference: ThemePreference) {
        themeRepository.setPreference(preference)
    }
}
