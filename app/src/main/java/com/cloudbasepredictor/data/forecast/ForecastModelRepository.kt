package com.cloudbasepredictor.data.forecast

import android.content.SharedPreferences
import com.cloudbasepredictor.model.ForecastModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ForecastModelRepository {
    val selectedModel: StateFlow<ForecastModel>

    fun selectModel(model: ForecastModel)
}

@Singleton
class InMemoryForecastModelRepository @Inject constructor(
    private val prefs: SharedPreferences,
) : ForecastModelRepository {
    private val mutableSelectedModel = MutableStateFlow(loadFromPrefs())

    override val selectedModel: StateFlow<ForecastModel> = mutableSelectedModel.asStateFlow()

    override fun selectModel(model: ForecastModel) {
        mutableSelectedModel.value = model
        prefs.edit().putString(KEY_SELECTED_MODEL, model.apiName).apply()
    }

    private fun loadFromPrefs(): ForecastModel {
        val apiName = prefs.getString(KEY_SELECTED_MODEL, null) ?: return ForecastModel.BEST_MATCH
        return ForecastModel.fromApiName(apiName) ?: ForecastModel.BEST_MATCH
    }

    private companion object {
        const val KEY_SELECTED_MODEL = "selected_forecast_model"
    }
}
