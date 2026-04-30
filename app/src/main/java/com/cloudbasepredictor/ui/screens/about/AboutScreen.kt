package com.cloudbasepredictor.ui.screens.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.BuildConfig
import com.cloudbasepredictor.R
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

private const val SOURCE_CODE_URL = "https://github.com/CloudbasePredictor/CloudbasePredictor"

@Composable
fun AboutRoute(
    onBack: () -> Unit,
) {
    AboutScreen(onBack = onBack)
}

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = stringResource(R.string.title_about),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                text = stringResource(
                    R.string.about_version_format,
                    BuildConfig.VERSION_NAME,
                    if (BuildConfig.DEBUG) "debug" else "release",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_foss_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LinkText(
                    title = stringResource(R.string.about_source_code),
                    url = SOURCE_CODE_URL,
                    linkColor = linkColor,
                    onOpenUrl = ::openUrl,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.about_data_sources),
                style = MaterialTheme.typography.titleMedium,
            )

            DataSourceLinkRow(
                label = stringResource(R.string.about_forecast_data_label),
                links = listOf(
                    DataSourceLink(
                        title = stringResource(R.string.about_open_meteo),
                        url = "https://open-meteo.com",
                    ),
                ),
                linkColor = linkColor,
                onOpenUrl = ::openUrl,
            )

            DataSourceLinkRow(
                label = stringResource(R.string.about_map_tiles_label),
                links = listOf(
                    DataSourceLink(
                        title = stringResource(R.string.about_openfreemap),
                        url = "https://openfreemap.org",
                    ),
                    DataSourceLink(
                        title = stringResource(R.string.about_openmaptiles),
                        url = "https://openmaptiles.org",
                    ),
                    DataSourceLink(
                        title = stringResource(R.string.about_openstreetmap),
                        url = "https://www.openstreetmap.org/copyright",
                    ),
                ),
                linkColor = linkColor,
                onOpenUrl = ::openUrl,
            )

            DataSourceLinkRow(
                label = stringResource(R.string.about_map_sdk_label),
                links = listOf(
                    DataSourceLink(
                        title = stringResource(R.string.about_maplibre),
                        url = "https://maplibre.org",
                    ),
                ),
                linkColor = linkColor,
                onOpenUrl = ::openUrl,
            )
        }
    }
}

@Composable
private fun DataSourceLinkRow(
    label: String,
    links: List<DataSourceLink>,
    linkColor: Color,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label.trimEnd(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        links.forEach { link ->
            LinkText(
                title = link.title,
                url = link.url,
                linkColor = linkColor,
                onOpenUrl = onOpenUrl,
            )
        }
    }
}

@Composable
private fun LinkText(
    title: String,
    url: String,
    linkColor: Color,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = modifier.clickable { onOpenUrl(url) },
    )
}

private data class DataSourceLink(
    val title: String,
    val url: String,
)

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    CloudbasePredictorTheme {
        AboutScreen(onBack = {})
    }
}
