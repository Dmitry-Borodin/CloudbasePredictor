package com.cloudbasepredictor.ui.screens.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForecastUiStateTest {
    @Test
    fun defaultState_containsPlaceholderChipsForFourteenDays() {
        val state = ForecastUiState()

        assertEquals(14, state.dayChips.size)
        assertEquals("Today", state.dayChips.first().title)
        assertEquals(0, state.selectedDayIndex)
        assertNull(state.selectedPlace)
    }
}
