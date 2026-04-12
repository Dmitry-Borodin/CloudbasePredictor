package com.cloudbasepredictor.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudbasepredictor.model.SavedPlace
import com.cloudbasepredictor.ui.components.FavoritesListDialog
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MAP_STYLE_HOST = "tiles.openfreemap.org"

@Composable
fun MapRoute(
    onOpenForecast: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            if (event == MapEvent.OpenForecast) {
                onOpenForecast()
            }
        }
    }

    MapScreen(
        uiState = uiState,
        onMapTapped = viewModel::selectPoint,
        onOpenForecast = viewModel::openSelectedForecast,
        onFavoriteClick = viewModel::openForecastForPlace,
        onSaveCameraPosition = viewModel::saveCameraPosition,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun MapScreen(
    uiState: MapUiState,
    onMapTapped: (Double, Double) -> Unit,
    onOpenForecast: () -> Unit,
    onFavoriteClick: (SavedPlace) -> Unit,
    onSaveCameraPosition: (Double, Double, Double) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var availabilityProbeKey by remember { mutableIntStateOf(0) }
    val isStyleHostReachable by produceState<Boolean?>(
        initialValue = null,
        key1 = availabilityProbeKey,
    ) {
        value = isMapStyleHostReachable()
    }

    val initialCamera = uiState.initialCamera
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = if (initialCamera != null) {
                Position(longitude = initialCamera.longitude, latitude = initialCamera.latitude)
            } else {
                Position(longitude = 13.4050, latitude = 52.5200)
            },
            zoom = initialCamera?.zoom ?: 5.5,
        )
    )

    DisposableEffect(Unit) {
        onDispose {
            val pos = cameraState.position
            onSaveCameraPosition(
                pos.target.latitude,
                pos.target.longitude,
                pos.zoom,
            )
        }
    }

    val markerData = uiState.selectedPlace?.let(::buildMarkerFeatureCollection) ?: emptyFeatureCollection()
    val favoritesData = buildFavoritesFeatureCollection(uiState.favoritePlaces)

    var showFavoritesDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when (isStyleHostReachable) {
            null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            false -> {
                MapUnavailableCard(
                    onRetry = {
                        availabilityProbeKey++
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                )
            }
            true -> {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri(MAP_STYLE_URL),
                    cameraState = cameraState,
                    onMapClick = { position, _ ->
                        onMapTapped(position.latitude, position.longitude)
                        ClickResult.Consume
                    },
                ) {
                    val markerSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(markerData),
                    )
                    CircleLayer(
                        id = "selected-point",
                        source = markerSource,
                        color = const(Color(0xFFE64A5B)),
                        radius = const(9.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp),
                    )

                    val favoritesSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(favoritesData),
                    )
                    CircleLayer(
                        id = "favorite-points",
                        source = favoritesSource,
                        color = const(Color(0xFFFFD700)),
                        radius = const(10.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FloatingActionButton(
                onClick = { showFavoritesDialog = true },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = Color(0xFFFFD700),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorites",
                )
            }

            FloatingActionButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                )
            }
        }

        uiState.selectedPlace?.let { selectedPlace ->
            SelectedPointCard(
                selectedPlace = selectedPlace,
                onOpenForecast = onOpenForecast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
            )
        }
    }

    if (showFavoritesDialog) {
        FavoritesListDialog(
            favorites = uiState.favoritePlaces,
            onPlaceClick = onFavoriteClick,
            onDismiss = { showFavoritesDialog = false },
        )
    }
}

@Composable
private fun MapUnavailableCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Map provider is unavailable right now.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Unable to resolve tiles.openfreemap.org. Retry when network is available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun SelectedPointCard(
    selectedPlace: SavedPlace,
    onOpenForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = selectedPlace.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = String.format(
                    java.util.Locale.US,
                    "Lat %.4f, Lon %.4f",
                    selectedPlace.latitude,
                    selectedPlace.longitude,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenForecast) {
                Text(text = "Open")
            }
        }
    }
}

private fun buildMarkerFeatureCollection(place: SavedPlace): String {
    return """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [${place.longitude}, ${place.latitude}]
              },
              "properties": {
                "name": "${place.name.replace("\"", "\\\"")}"
              }
            }
          ]
        }
    """.trimIndent()
}

private fun buildFavoritesFeatureCollection(places: List<SavedPlace>): String {
    if (places.isEmpty()) return emptyFeatureCollection()
    val features = places.joinToString(",") { place ->
        """
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [${place.longitude}, ${place.latitude}]
              },
              "properties": {
                "name": "${place.name.replace("\"", "\\\"")}"
              }
            }
        """
    }
    return """
        {
          "type": "FeatureCollection",
          "features": [$features]
        }
    """.trimIndent()
}

private fun emptyFeatureCollection(): String {
    return """
        {
          "type": "FeatureCollection",
          "features": []
        }
    """.trimIndent()
}

private suspend fun isMapStyleHostReachable(): Boolean {
    val resolution = withTimeoutOrNull(2_000L) {
        withContext(Dispatchers.IO) {
            runCatching {
                InetAddress.getByName(MAP_STYLE_HOST)
            }.isSuccess
        }
    }
    return resolution == true
}

@Preview(showBackground = true)
@Composable
private fun SelectedPointCardPreview() {
    CloudbasePredictorTheme {
        SelectedPointCard(
            selectedPlace = PreviewData.mapUiState.selectedPlace ?: PreviewData.savedPlace,
            onOpenForecast = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    CloudbasePredictorTheme {
        MapScreen(
            uiState = PreviewData.mapUiState,
            onMapTapped = { _, _ -> },
            onOpenForecast = {},
            onFavoriteClick = {},
            onSaveCameraPosition = { _, _, _ -> },
        )
    }
}
