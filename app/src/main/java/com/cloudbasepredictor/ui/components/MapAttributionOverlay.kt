package com.cloudbasepredictor.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.R
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun MapAttributionOverlay(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(MapTestTags.ATTRIBUTION_OVERLAY),
        shape = RoundedCornerShape(4.dp),
        color = Color.White.copy(alpha = 0.82f),
        contentColor = Color(0xFF202124),
        tonalElevation = 1.dp,
    ) {
        Text(
            text = stringResource(R.string.map_attribution_compact),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapAttributionOverlayPreview() {
    CloudbasePredictorTheme {
        MapAttributionOverlay()
    }
}
