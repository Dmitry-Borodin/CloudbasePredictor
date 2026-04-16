package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.forecast.INITIAL_FORECAST_DAYS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastUiStateTest {
    @Test
    fun defaultState_containsPlaceholderChipsForInitialLoadWindow() {
        val state = ForecastUiState()

        assertEquals(INITIAL_FORECAST_DAYS, state.dayChips.size)
        assertEquals("Today", state.dayChips.first().title)
        assertEquals(0, state.selectedDayIndex)
        assertNull(state.selectedPlace)
    }

    @Test
    fun defaultState_hasEmptyFavoritePlaces() {
        val state = ForecastUiState()
        assertTrue(state.favoritePlaces.isEmpty())
    }
}
