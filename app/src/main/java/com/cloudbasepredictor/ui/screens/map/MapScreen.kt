package com.cloudbasepredictor.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cloudbasepredictor.model.SavedPlace
import kotlinx.coroutines.flow.collectLatest
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

@Composable
fun MapRoute(
    onOpenForecast: () -> Unit,
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
    )
}

@Composable
fun MapScreen(
    uiState: MapUiState,
    onMapTapped: (Double, Double) -> Unit,
    onOpenForecast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = 13.4050, latitude = 52.5200),
            zoom = 5.5,
        )
    )
    val markerSource = rememberGeoJsonSource(
        data = GeoJsonData.JsonString(emptyFeatureCollection()),
    )

    LaunchedEffect(uiState.selectedPlace) {
        markerSource.setData(
            GeoJsonData.JsonString(
                uiState.selectedPlace?.let(::buildMarkerFeatureCollection) ?: emptyFeatureCollection()
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Map",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Tap anywhere on the OpenFreeMap map to place a marker.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp)),
        ) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                cameraState = cameraState,
                onMapClick = { position, _ ->
                    onMapTapped(position.latitude, position.longitude)
                    ClickResult.Consume
                },
            ) {
                CircleLayer(
                    id = "selected-point",
                    source = markerSource,
                    color = const(Color(0xFFE64A5B)),
                    radius = const(9.dp),
                    strokeColor = const(Color.White),
                    strokeWidth = const(3.dp),
                )
            }

            uiState.selectedPlace?.let { selectedPlace ->
                SelectedPointCard(
                    selectedPlace = selectedPlace,
                    onOpenForecast = onOpenForecast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
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
                "name": "${place.name}"
              }
            }
          ]
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
