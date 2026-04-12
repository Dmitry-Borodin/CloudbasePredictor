package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun ForecastDatePicker(
    dayChips: List<ForecastDayChipUiModel>,
    selectedDayIndex: Int,
    onDateSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(dayChips) { index, dayChip ->
                FilterChip(
                    selected = index == selectedDayIndex,
                    onClick = { onDateSelected(index) },
                    label = {
                        Column {
                            Text(text = dayChip.title)
                            Text(
                                text = dayChip.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun ForecastDatePickerPreview() {
    CloudbasePredictorTheme {
        ForecastDatePicker(
            dayChips = PreviewData.forecastDayChips(),
            selectedDayIndex = 2,
            onDateSelected = {},
        )
    }
}
