package com.cloudbasepredictor.ui.screens.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun ForecastModePicker(
    selectedMode: ForecastMode,
    onModeSelected: (ForecastMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ForecastModePickerItem(
                icon = Icons.Outlined.WbSunny,
                contentDescription = "Stuve",
                selected = selectedMode == ForecastMode.STUVE,
                onClick = { onModeSelected(ForecastMode.STUVE) },
            )
            ForecastModePickerItem(
                icon = Icons.Outlined.Air,
                contentDescription = "Wind",
                selected = selectedMode == ForecastMode.WIND,
                onClick = { onModeSelected(ForecastMode.WIND) },
            )
            ForecastModePickerItem(
                icon = Icons.Outlined.ArrowUpward,
                contentDescription = "Thermic",
                selected = selectedMode == ForecastMode.THERMIC,
                onClick = { onModeSelected(ForecastMode.THERMIC) },
            )
            ForecastModePickerItem(
                icon = Icons.Outlined.Cloud,
                contentDescription = "Cloud",
                selected = selectedMode == ForecastMode.CLOUD,
                onClick = { onModeSelected(ForecastMode.CLOUD) },
            )
        }
    }
}

@Composable
private fun ForecastModePickerItem(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val tintColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastModePickerPreview() {
    CloudbasePredictorTheme {
        ForecastModePicker(
            selectedMode = ForecastMode.CLOUD,
            onModeSelected = {},
        )
    }
}
