package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.R
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.model.ForecastModel
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.HELP_BUTTON
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.text.DateFormat
import java.util.Date

@Composable
internal fun HelpButtonOverlay(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
) {
    val helpContent = rememberForecastHelpContent(uiState)
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { isDialogVisible = true },
        modifier = modifier
            .size(48.dp)
            .testTag(HELP_BUTTON),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
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
                    // Model info section
                    if (uiState.resolvedModel != null) {
                        Text(
                            text = stringResource(R.string.help_model_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        val modelText = if (uiState.selectedModel != uiState.resolvedModel) {
                            "${uiState.selectedModel.displayName} → ${uiState.resolvedModel.displayName}"
                        } else {
                            uiState.resolvedModel.displayName
                        }
                        Text(
                            text = modelText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Update time
                    if (uiState.forecastUpdatedAtMillis != null) {
                        val localDateTimeFormat = DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT,
                        )
                        val formattedTime = localDateTimeFormat.format(
                            Date(uiState.forecastUpdatedAtMillis),
                        )
                        Text(
                            text = stringResource(R.string.help_data_updated_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Estimated model generation time
                    if (uiState.modelGeneratedAtMillis != null) {
                        val localDateTimeFormat = DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT,
                        )
                        val formattedModelTime = localDateTimeFormat.format(
                            Date(uiState.modelGeneratedAtMillis),
                        )
                        Text(
                            text = stringResource(R.string.help_model_generated_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = formattedModelTime,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    HorizontalDivider()

                    // Mode-specific content with legends
                    when (uiState.selectedForecastMode) {
                        ForecastMode.THERMIC -> {
                            Text(
                                text = helpContent.summary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ThermicStrengthLegend()
                        }
                        ForecastMode.STUVE -> {
                            Text(
                                text = helpContent.summary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            helpContent.tips.forEach { tip ->
                                Text(
                                    text = "\u2022 $tip",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                        ForecastMode.WIND -> {
                            Text(
                                text = helpContent.summary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            WindSpeedLegend()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.help_wind_ccl_info),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ForecastMode.CLOUD -> {
                            CloudForecastLegend()
                        }
                    }

                    if (helpContent.statusMessage.isNotBlank()) {
                        Text(
                            text = helpContent.statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            tips = emptyList(),
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
            tips = emptyList(),
        )
        ForecastMode.CLOUD -> ForecastHelpContent(
            title = stringResource(R.string.help_cloud_title),
            summary = stringResource(R.string.help_cloud_summary),
            statusMessage = forecastStatusMessage(uiState, includeSelectedForecast = false),
            tips = emptyList(),
        )
    }
}

@Composable
private fun forecastStatusMessage(
    uiState: ForecastUiState,
    includeSelectedForecast: Boolean = true,
): String {
    val errorMessage = uiState.errorMessage
    return when {
        uiState.selectedPlace == null -> stringResource(R.string.help_status_no_place)
        errorMessage != null -> stringResource(R.string.help_status_error, errorMessage)
        uiState.isLoading -> stringResource(R.string.help_status_loading, uiState.selectedPlace.name)
        !includeSelectedForecast -> ""
        else -> stringResource(
            R.string.help_status_showing,
            uiState.selectedForecastMode.name.lowercase(),
            uiState.selectedPlace.name,
            uiState.dayChips.getOrNull(uiState.selectedDayIndex)?.subtitle ?: stringResource(R.string.help_status_selected_day),
        )
    }
}

@Composable
private fun CloudForecastLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.help_cloud_legend_sun),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.help_cloud_legend_radiation),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.help_cloud_legend_layers),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.help_cloud_legend_rain),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class ForecastHelpContent(
    val title: String,
    val summary: String,
    val statusMessage: String,
    val tips: List<String>,
)

// ── Thermic strength legend (0 → 10 m/s) ────────────────────────────────

@Composable
private fun ThermicStrengthLegend() {
    val steps = listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { value ->
                Text(
                    text = "${value.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            // Draw gradient bar using thermic color scale
            val colorCount = 40
            repeat(colorCount) { i ->
                val strength = i.toFloat() / (colorCount - 1) * 10f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(thermicLegendColor(strength)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "weak", style = MaterialTheme.typography.labelSmall)
            Text(text = "strong", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Wind speed legend (0 → 60 km/h) ────────────────────────────────

@Composable
private fun WindSpeedLegend() {
    val steps = listOf(0, 10, 20, 30, 40, 50, 60)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { value ->
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            val colorCount = 40
            repeat(colorCount) { i ->
                val speedKmh = i.toFloat() / (colorCount - 1) * 60f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(windLegendColor(speedKmh)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "calm", style = MaterialTheme.typography.labelSmall)
            Text(text = "strong", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Thermic strength color scale matching ThermicForecastView. */
private fun thermicLegendColor(strengthMps: Float): Color {
    val normalized = (strengthMps / 10f).coerceIn(0f, 1f)
    val colorStops = listOf(
        0f to Color(0xFFFCE0AE),
        0.17f to Color(0xFFFFFF00),
        0.33f to Color(0xFF00F6B2),
        0.5f to Color(0xFF7BD0BC),
        0.67f to Color(0xFF19C8E0),
        0.83f to Color(0xFF6A95E6),
        1f to Color(0xFF2015F3),
    )
    val lower = colorStops.lastOrNull { it.first <= normalized } ?: colorStops.first()
    val upper = colorStops.firstOrNull { it.first >= normalized } ?: colorStops.last()
    if (lower.first == upper.first) return lower.second
    val fraction = (normalized - lower.first) / (upper.first - lower.first)
    return lerp(lower.second, upper.second, fraction)
}

/** Wind speed color scale matching WindForecastView. */
private fun windLegendColor(speedKmh: Float): Color {
    val normalized = (speedKmh / 60f).coerceIn(0f, 1f)
    val low = Color(0xFF4CAF50)
    val medium = Color(0xFFFFC107)
    val high = Color(0xFFE53935)
    return if (normalized <= 0.5f) {
        lerp(low, medium, normalized / 0.5f)
    } else {
        lerp(medium, high, (normalized - 0.5f) / 0.5f)
    }
}

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
