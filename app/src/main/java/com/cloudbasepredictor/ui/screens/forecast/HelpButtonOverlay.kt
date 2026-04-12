package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.HELP_BUTTON
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun HelpButtonOverlay(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
) {
    val helpContent = rememberForecastHelpContent(uiState)
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { isDialogVisible = true },
        modifier = modifier.testTag(HELP_BUTTON),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = "Open forecast help",
        )
    }

    if (isDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDialogVisible = false },
            title = {
                Text(text = helpContent.title)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = helpContent.summary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = helpContent.statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    helpContent.tips.forEach { tip ->
                        Text(
                            text = "\u2022 $tip",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isDialogVisible = false }) {
                    Text(text = "Close")
                }
            },
        )
    }
}

@Composable
private fun rememberForecastHelpContent(uiState: ForecastUiState): ForecastHelpContent {
    return when (uiState.selectedForecastMode) {
        ForecastMode.THERMIC -> ForecastHelpContent(
            title = "Thermic forecast help",
            summary = "This chart shows estimated thermal strength by hour and altitude. Colored cells mark lift bands, and cloud markers indicate the top of usable convection.",
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                "Read stronger thermals from brighter cool colors and compare them across the day.",
                "Use pinch zoom to inspect lower or higher altitude layers in more detail.",
                "The current thermic chart uses generated UI data and is ready to be replaced by real forecast-derived output.",
            ),
        )
        ForecastMode.STUVE -> ForecastHelpContent(
            title = "Stuve forecast help",
            summary = "The Stuve screen is intended for vertical atmosphere analysis. It will later explain instability, moisture, and other sounding-derived layers.",
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                "Use this view to reason about the vertical structure of the day once the sounding model is connected.",
                "Switch back to thermic or cloud mode for a faster high-level read of soaring conditions.",
                "The current Stuve screen still uses placeholder forecast content.",
            ),
        )
        ForecastMode.WIND -> ForecastHelpContent(
            title = "Wind forecast help",
            summary = "The wind screen is reserved for layered wind direction and strength through the day. It will help compare usable windows at different altitudes.",
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                "Check several altitude bands instead of relying on surface wind alone.",
                "Compare wind mode with thermic mode before judging whether climbs are usable.",
                "The current wind screen still uses placeholder forecast content.",
            ),
        )
        ForecastMode.CLOUD -> ForecastHelpContent(
            title = "Cloud forecast help",
            summary = "The cloud screen is reserved for layered cloud coverage and cloud-base-related information across the day.",
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                "Use cloud mode together with thermic mode to estimate whether lift reaches cloud base or stays blue.",
                "Compare altitude bands to spot when cloud development starts climbing higher.",
                "The current cloud screen still uses placeholder forecast content.",
            ),
        )
    }
}

private fun forecastStatusMessage(uiState: ForecastUiState): String {
    return when {
        uiState.selectedPlace == null -> "No place is selected yet. Open a point from the map to load forecast context for this screen."
        uiState.errorMessage != null -> "Forecast refresh failed: ${uiState.errorMessage}"
        uiState.isLoading -> "Forecast data is currently loading for ${uiState.selectedPlace.name}."
        else -> "Showing the ${uiState.selectedForecastMode.name.lowercase()} forecast for ${uiState.selectedPlace.name} on ${uiState.dayChips.getOrNull(uiState.selectedDayIndex)?.subtitle ?: "the selected day"}."
    }
}

private data class ForecastHelpContent(
    val title: String,
    val summary: String,
    val statusMessage: String,
    val tips: List<String>,
)

@Preview(name = "Forecast Help Overlay", showBackground = true)
@Composable
private fun HelpButtonOverlayPreview() {
    CloudbasePredictorTheme {
        HelpButtonOverlay(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.THERMIC),
        )
    }
}

@Preview(name = "Forecast Help Overlay Error", showBackground = true)
@Composable
private fun HelpButtonOverlayErrorPreview() {
    CloudbasePredictorTheme {
        HelpButtonOverlay(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.CLOUD,
                errorMessage = "Unable to refresh forecast layers right now.",
            ),
        )
    }
}
