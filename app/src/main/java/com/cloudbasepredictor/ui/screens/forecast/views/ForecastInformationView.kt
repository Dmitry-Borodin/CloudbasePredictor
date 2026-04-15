package com.cloudbasepredictor.ui.screens.forecast.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
internal fun ForecastInformationView(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastInformationViewPreview() {
    CloudbasePredictorTheme {
        ForecastInformationView(
            message = "Unfortunately, no thermals are expected.",
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ForecastInformationViewDarkPreview() {
    CloudbasePredictorTheme(darkTheme = true) {
        ForecastInformationView(
            message = "Unfortunately, no thermals are expected.",
        )
    }
}
