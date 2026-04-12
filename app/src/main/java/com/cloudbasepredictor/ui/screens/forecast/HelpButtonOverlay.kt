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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.R
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
            contentDescription = stringResource(R.string.cd_open_forecast_help),
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
                    Text(text = stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun rememberForecastHelpContent(uiState: ForecastUiState): ForecastHelpContent {
    return when (uiState.selectedForecastMode) {
        ForecastMode.THERMIC -> ForecastHelpContent(
            title = stringResource(R.string.help_thermic_title),
            summary = stringResource(R.string.help_thermic_summary),
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                stringResource(R.string.help_thermic_tip_1),
                stringResource(R.string.help_thermic_tip_2),
                stringResource(R.string.help_thermic_tip_3),
            ),
        )
        ForecastMode.STUVE -> ForecastHelpContent(
            title = stringResource(R.string.help_stuve_title),
            summary = stringResource(R.string.help_stuve_summary),
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                stringResource(R.string.help_stuve_tip_1),
                stringResource(R.string.help_stuve_tip_2),
                stringResource(R.string.help_stuve_tip_3),
            ),
        )
        ForecastMode.WIND -> ForecastHelpContent(
            title = stringResource(R.string.help_wind_title),
            summary = stringResource(R.string.help_wind_summary),
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                stringResource(R.string.help_wind_tip_1),
                stringResource(R.string.help_wind_tip_2),
                stringResource(R.string.help_wind_tip_3),
            ),
        )
        ForecastMode.CLOUD -> ForecastHelpContent(
            title = stringResource(R.string.help_cloud_title),
            summary = stringResource(R.string.help_cloud_summary),
            statusMessage = forecastStatusMessage(uiState),
            tips = listOf(
                stringResource(R.string.help_cloud_tip_1),
                stringResource(R.string.help_cloud_tip_2),
                stringResource(R.string.help_cloud_tip_3),
            ),
        )
    }
}

@Composable
private fun forecastStatusMessage(uiState: ForecastUiState): String {
    return when {
        uiState.selectedPlace == null -> stringResource(R.string.help_status_no_place)
        uiState.errorMessage != null -> stringResource(R.string.help_status_error, uiState.errorMessage!!)
        uiState.isLoading -> stringResource(R.string.help_status_loading, uiState.selectedPlace.name)
        else -> stringResource(
            R.string.help_status_showing,
            uiState.selectedForecastMode.name.lowercase(),
            uiState.selectedPlace.name,
            uiState.dayChips.getOrNull(uiState.selectedDayIndex)?.subtitle ?: stringResource(R.string.help_status_selected_day),
        )
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
