package com.cloudbasepredictor.data.forecast

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
class InMemoryForecastModelRepository @Inject constructor() : ForecastModelRepository {
    private val mutableSelectedModel = MutableStateFlow(ForecastModel.BEST_MATCH)

    override val selectedModel: StateFlow<ForecastModel> = mutableSelectedModel.asStateFlow()

    override fun selectModel(model: ForecastModel) {
        mutableSelectedModel.value = model
    }
}
