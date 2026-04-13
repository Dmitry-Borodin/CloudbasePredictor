package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.R
import com.cloudbasepredictor.model.ForecastModel
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSelectorOverlay(
    selectedModel: ForecastModel,
    resolvedModel: ForecastModel?,
    onModelSelected: (ForecastModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val label = when {
        selectedModel == ForecastModel.BEST_MATCH &&
            resolvedModel != null &&
            resolvedModel != ForecastModel.BEST_MATCH ->
            "${selectedModel.displayName} (${resolvedModel.displayName})"
        else -> selectedModel.displayName
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        FilledTonalButton(
            onClick = { showSheet = true },
            modifier = Modifier.testTag(ForecastTestTags.MODEL_SELECTOR_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = null,
                modifier = Modifier.height(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = stringResource(R.string.model_selector_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            ModelSelectorSheetContent(
                selectedModel = selectedModel,
                resolvedModel = resolvedModel,
                onModelSelected = { model ->
                    onModelSelected(model)
                    showSheet = false
                },
            )
        }
    }
}

@Composable
private fun ModelSelectorSheetContent(
    selectedModel: ForecastModel,
    resolvedModel: ForecastModel?,
    onModelSelected: (ForecastModel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.title_weather_model),
            style = MaterialTheme.typography.titleMedium,
        )
        if (resolvedModel != null && resolvedModel != selectedModel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.model_selector_fell_back, resolvedModel.displayName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        ForecastModel.entries.forEach { model ->
            val isSelected = model == selectedModel
            ModelOptionRow(
                model = model,
                isSelected = isSelected,
                onClick = { onModelSelected(model) },
            )
        }
    }
}

@Composable
private fun ModelOptionRow(
    model: ForecastModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ForecastTestTags.MODEL_OPTION_PREFIX + model.apiName),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelSelectorOverlayPreview() {
    CloudbasePredictorTheme {
        ModelSelectorOverlay(
            selectedModel = ForecastModel.ICON_D2,
            resolvedModel = ForecastModel.ICON_D2,
            onModelSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelSelectorSheetContentPreview() {
    CloudbasePredictorTheme {
        ModelSelectorSheetContent(
            selectedModel = ForecastModel.ICON_D2,
            resolvedModel = ForecastModel.ICON_EU,
            onModelSelected = {},
        )
    }
}
