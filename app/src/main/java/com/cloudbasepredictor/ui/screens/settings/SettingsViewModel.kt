package com.cloudbasepredictor.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.cloudbasepredictor.data.datasource.DataSourcePreference
import com.cloudbasepredictor.data.datasource.DataSourceRepository
import com.cloudbasepredictor.data.theme.ThemePreference
import com.cloudbasepredictor.data.theme.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSourceRepository: DataSourceRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {
    val dataSourcePreference: StateFlow<DataSourcePreference> = dataSourceRepository.preference
    val themePreference: StateFlow<ThemePreference> = themeRepository.preference

    fun setDataSource(preference: DataSourcePreference) {
        dataSourceRepository.setPreference(preference)
    }

    fun setTheme(preference: ThemePreference) {
        themeRepository.setPreference(preference)
    }
}
