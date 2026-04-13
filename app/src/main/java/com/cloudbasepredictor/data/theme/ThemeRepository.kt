package com.cloudbasepredictor.data.theme

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemePreference {
    AUTO,
    LIGHT,
    DARK,
}

interface ThemeRepository {
    val preference: StateFlow<ThemePreference>
    fun setPreference(preference: ThemePreference)
}

@Singleton
class InMemoryThemeRepository @Inject constructor() : ThemeRepository {
    override val preference = MutableStateFlow(ThemePreference.AUTO)

    override fun setPreference(preference: ThemePreference) {
        this.preference.value = preference
    }
}
