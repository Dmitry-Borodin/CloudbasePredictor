package com.cloudbasepredictor.ui.screens.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastUiStateTest {
    @Test
    fun loadingState_isDefaultScreenStateWithoutReadyData() {
        val state: ForecastUiState = ForecastLoadingUiState()

        assertTrue(state is ForecastLoadingUiState)
        assertFalse(state is ForecastReadyUiState)
        assertEquals(0, state.selectedDayIndex)
        assertNull(state.selectedPlace)
    }

    @Test
    fun loadingState_hasEmptyFavoritePlaces() {
        val state = ForecastLoadingUiState()
        assertTrue(state.favoritePlaces.isEmpty())
    }
}
