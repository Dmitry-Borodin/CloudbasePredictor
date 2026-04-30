package com.cloudbasepredictor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudbasepredictor.model.SavedPlace
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

private val FavoriteLabelTextColor = Color(0xFF1B1B1B)
private val FavoriteLabelWidth = 96.dp

@Composable
fun MapFavoriteLabelsOverlay(
    favoritePlaces: List<SavedPlace>,
    cameraState: CameraState,
    markerRadius: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val projection = cameraState.projection ?: return
    val cameraPosition = cameraState.position
    var mapSize by remember { mutableStateOf(IntSize.Zero) }

    val labelPositions = remember(favoritePlaces, projection, cameraPosition, mapSize) {
        favoritePlaces.mapNotNull { place ->
            val screenOffset = runCatching {
                projection.screenLocationFromPosition(
                    Position(longitude = place.longitude, latitude = place.latitude),
                )
            }.getOrNull() ?: return@mapNotNull null

            FavoriteLabelPosition(
                name = place.name,
                screenOffset = screenOffset,
            )
        }
    }

    FavoriteLabelsOverlayContent(
        labels = labelPositions,
        markerRadius = markerRadius,
        fontSize = fontSize,
        mapSize = mapSize,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { mapSize = it },
    )
}

@Composable
private fun FavoriteLabelsOverlayContent(
    labels: List<FavoriteLabelPosition>,
    markerRadius: Dp,
    fontSize: TextUnit,
    mapSize: IntSize,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val mapWidth = with(density) { mapSize.width.toDp() }
    val mapHeight = with(density) { mapSize.height.toDp() }

    Box(
        modifier = modifier,
    ) {
        labels
            .filter { label ->
                label.screenOffset.x >= -FavoriteLabelWidth &&
                    label.screenOffset.x <= mapWidth + FavoriteLabelWidth &&
                    label.screenOffset.y >= -markerRadius &&
                    label.screenOffset.y <= mapHeight + FavoriteLabelWidth
            }
            .forEach { label ->
                Text(
                    text = label.name,
                    modifier = Modifier
                        .width(FavoriteLabelWidth)
                        .offset(
                            x = label.screenOffset.x - FavoriteLabelWidth / 2,
                            y = label.screenOffset.y + markerRadius + 2.dp,
                        )
                        .align(Alignment.TopStart),
                    style = MaterialTheme.typography.labelSmall.merge(
                        TextStyle(
                            color = FavoriteLabelTextColor,
                            fontSize = fontSize,
                            shadow = Shadow(
                                color = Color.White,
                                offset = Offset.Zero,
                                blurRadius = 4f,
                            ),
                        )
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
    }
}

private data class FavoriteLabelPosition(
    val name: String,
    val screenOffset: DpOffset,
)

@Preview(showBackground = true, widthDp = 360, heightDp = 240)
@Composable
private fun MapFavoriteLabelsOverlayPreview() {
    CloudbasePredictorTheme {
        FavoriteLabelsOverlayContent(
            labels = PreviewData.favoritePlaces.mapIndexed { index, place ->
                FavoriteLabelPosition(
                    name = place.name,
                    screenOffset = DpOffset(
                        x = (92 + index * 72).dp,
                        y = (72 + index * 34).dp,
                    ),
                )
            },
            markerRadius = 8.dp,
            fontSize = 10.sp,
            mapSize = IntSize(width = 360, height = 240),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
