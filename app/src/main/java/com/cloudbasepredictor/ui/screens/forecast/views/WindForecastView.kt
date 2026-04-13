package com.cloudbasepredictor.ui.screens.forecast.views

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.WindForecastChartUiModel
import com.cloudbasepredictor.ui.screens.forecast.zoomedTopAltitudeKm
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun WindForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(WIND_VIEW),
    ) {
        WindChartCanvas(
            chart = uiState.windChart,
            visibleTopAltitudeKm = uiState.chartViewport.visibleTopAltitudeKm,
            onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
            modifier = Modifier.fillMaxSize(),
        )

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun WindChartCanvas(
    chart: WindForecastChartUiModel,
    visibleTopAltitudeKm: Float,
    onVisibleTopAltitudeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val gridBackgroundColor = lerp(
        start = MaterialTheme.colorScheme.surface,
        stop = MaterialTheme.colorScheme.onSurface,
        fraction = 0.035f,
    )
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val axisLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val hourLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }
    val unitLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val speedLabelPaint = remember(density, onSurfaceColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    Canvas(
        modifier = modifier.pointerInput(visibleTopAltitudeKm) {
            detectTransformGestures { _, _, zoom, _ ->
                onVisibleTopAltitudeChange(
                    zoomedTopAltitudeKm(
                        currentTopAltitudeKm = visibleTopAltitudeKm,
                        zoomChange = zoom,
                    ),
                )
            }
        },
    ) {
        val axisWidth = with(density) { 60.dp.toPx() }
        val bottomAxisHeight = with(density) { 38.dp.toPx() }
        val arrowSizePx = with(density) { 48.dp.toPx() }

        val plotLeft = axisWidth
        val plotTop = 0f
        val plotRight = size.width
        val plotBottom = size.height - bottomAxisHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        val minAltitudeKm = WIND_MIN_ALTITUDE_KM
        val effectiveTopAltitudeKm = max(
            visibleTopAltitudeKm,
            minAltitudeKm + WIND_MIN_VISIBLE_ALTITUDE_RANGE_KM,
        )

        if (plotWidth <= 0f || plotHeight <= 0f || chart.hours.isEmpty()) {
            return@Canvas
        }

        // Background
        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(0f, plotTop),
            size = Size(axisWidth, plotHeight),
        )

        val columnWidth = plotWidth / chart.hours.size

        // Determine which altitudes and hours to show arrows for,
        // based on available cell size vs arrowSizePx
        val visibleAltitudes = chart.altitudeBandsKm.filter {
            it in minAltitudeKm..effectiveTopAltitudeKm
        }
        if (visibleAltitudes.isEmpty()) return@Canvas

        val altStep = if (visibleAltitudes.size > 1) {
            visibleAltitudes[1] - visibleAltitudes[0]
        } else {
            0.25f
        }
        val bandHeightPx = plotHeight * altStep / (effectiveTopAltitudeKm - minAltitudeKm)

        // Clustering: skip arrows if cells are too small
        val altCluster = max(1, ceil(arrowSizePx * 1.1f / bandHeightPx).toInt())
        val hourCluster = max(1, ceil(arrowSizePx * 1.1f / columnWidth).toInt())

        // ── Wind speed background (smooth color per cell) ──────────
        chart.hours.forEachIndexed { hourIndex, hour ->
            val x = plotLeft + hourIndex * columnWidth
            visibleAltitudes.forEachIndexed { altIdx, altKm ->
                val cell = chart.cells.find { it.hour == hour && it.altitudeKm == altKm }
                    ?: return@forEachIndexed
                val topY = altitudeToY(
                    altKm + altStep, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                val bottomY = altitudeToY(
                    altKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                drawRect(
                    color = windSpeedBgColor(cell.speedKmh),
                    topLeft = Offset(x, topY),
                    size = Size(columnWidth, bottomY - topY),
                )
            }
        }

        // Grid lines
        chart.hours.forEachIndexed { index, hour ->
            val x = plotLeft + index * columnWidth
            val alpha = if ((hour - chart.hours.first()) % 3 == 0) 0.5f else 0.22f
            drawLine(
                color = outlineColor.copy(alpha = alpha),
                start = Offset(x, plotTop),
                end = Offset(x, plotBottom),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val altitudeTicks = buildAltitudeTicks(
            minAltitudeKm = minAltitudeKm,
            maxAltitudeKm = effectiveTopAltitudeKm,
            stepKm = if (effectiveTopAltitudeKm <= 3.5f) 0.5f else 1f,
        )
        altitudeTicks.forEach { altKm ->
            val y = altitudeToY(altKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom)
            drawLine(
                color = outlineColor.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Outline
        drawRect(
            color = outlineColor.copy(alpha = 0.4f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            style = Stroke(width = 1.dp.toPx()),
        )

        // ── CCL line (orange, 2.5dp, rounded) ──────────────────────
        if (chart.cclKm.isNotEmpty()) {
            val cclPath = Path()
            var started = false
            chart.cclKm.forEach { marker ->
                val hourIndex = chart.hours.indexOf(marker.hour)
                if (hourIndex < 0) return@forEach
                val x = plotLeft + hourIndex * columnWidth + columnWidth / 2f
                val y = altitudeToY(
                    marker.altitudeKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                if (y !in plotTop..plotBottom) return@forEach
                if (!started) {
                    cclPath.moveTo(x, y)
                    started = true
                } else {
                    cclPath.lineTo(x, y)
                }
            }
            if (started) {
                drawPath(
                    path = cclPath,
                    color = Color(0xFFFF8C00),
                    style = Stroke(
                        width = 2.5f.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
        }

        // ── 0 °C level line (cyan, 2dp, rounded) ───────────────────
        if (chart.freezingLevelKm.isNotEmpty()) {
            val flPath = Path()
            var started = false
            chart.freezingLevelKm.forEach { marker ->
                val hourIndex = chart.hours.indexOf(marker.hour)
                if (hourIndex < 0) return@forEach
                val x = plotLeft + hourIndex * columnWidth + columnWidth / 2f
                val y = altitudeToY(
                    marker.altitudeKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                if (y !in plotTop..plotBottom) return@forEach
                if (!started) {
                    flPath.moveTo(x, y)
                    started = true
                } else {
                    flPath.lineTo(x, y)
                }
            }
            if (started) {
                drawPath(
                    path = flPath,
                    color = Color(0xFF00BCD4),
                    style = Stroke(
                        width = 2f.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
        }

        // Draw wind arrows and speed labels with clustering
        val clusteredHours = chart.hours.filterIndexed { i, _ -> i % hourCluster == 0 }
        val clusteredAltitudes = visibleAltitudes.filterIndexed { i, _ -> i % altCluster == 0 }

        clusteredHours.forEach { hour ->
            val hourIndex = chart.hours.indexOf(hour)
            val cellCenterX = plotLeft + hourIndex * columnWidth + columnWidth * hourCluster / 2f

            clusteredAltitudes.forEach { altKm ->
                val cellCenterY = altitudeToY(
                    altKm + altStep * altCluster / 2f,
                    minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )

                // Find the cell closest to center of cluster
                val cell = chart.cells.find { it.hour == hour && it.altitudeKm == altKm }
                    ?: return@forEach

                if (cellCenterY !in plotTop..plotBottom) return@forEach

                // Draw arrow — black in light theme (onSurface)
                val arrowDrawSize = min(
                    arrowSizePx,
                    min(columnWidth * hourCluster * 0.8f, bandHeightPx * altCluster * 0.8f),
                )
                drawWindArrow(
                    centerX = cellCenterX,
                    centerY = cellCenterY,
                    directionDeg = cell.directionDeg,
                    arrowSize = arrowDrawSize,
                    speedKmh = cell.speedKmh,
                    color = onSurfaceColor,
                )
            }
        }

        // Axis labels
        drawIntoCanvas { canvas ->
            altitudeTicks.forEach { altKm ->
                val y = altitudeToY(
                    altKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.1f", altKm),
                    8.dp.toPx(),
                    y + axisLabelPaint.textSize * 0.35f,
                    axisLabelPaint,
                )
            }

            canvas.nativeCanvas.drawText(
                "km",
                8.dp.toPx(),
                plotBottom + unitLabelPaint.textSize + 12.dp.toPx(),
                unitLabelPaint,
            )

            chart.hours.forEachIndexed { index, hour ->
                if ((hour - chart.hours.first()) % 3 != 0) return@forEachIndexed
                val labelCenterX = plotLeft + index * columnWidth + columnWidth / 2f
                canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%02d", hour),
                    labelCenterX,
                    plotBottom + hourLabelPaint.textSize + 14.dp.toPx(),
                    hourLabelPaint,
                )
            }

            // Speed labels at center of each clustered cell
            clusteredHours.forEach { hour ->
                val hourIndex = chart.hours.indexOf(hour)
                val cellCenterX = plotLeft + hourIndex * columnWidth + columnWidth * hourCluster / 2f

                clusteredAltitudes.forEach { altKm ->
                    val cellCenterY = altitudeToY(
                        altKm + altStep * altCluster / 2f,
                        minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                    )

                    val cell = chart.cells.find { it.hour == hour && it.altitudeKm == altKm }
                        ?: return@forEach

                    if (cellCenterY !in plotTop..plotBottom) return@forEach

                    // Speed text below the arrow
                    val arrowDrawSize = min(
                        arrowSizePx,
                        min(columnWidth * hourCluster * 0.8f, bandHeightPx * altCluster * 0.8f),
                    )
                    canvas.nativeCanvas.drawText(
                        "${cell.speedKmh.toInt()}",
                        cellCenterX,
                        cellCenterY + arrowDrawSize / 2f + speedLabelPaint.textSize + 1.dp.toPx(),
                        speedLabelPaint,
                    )
                }
            }

            // CCL label (left side, 30dp from left edge)
            chart.cclKm.firstOrNull()?.let { firstCcl ->
                val y = altitudeToY(
                    firstCcl.altitudeKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                if (y in plotTop..plotBottom) {
                    val cclLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(0xFF, 0x8C, 0x00)
                        textSize = with(density) { 10.sp.toPx() }
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                    canvas.nativeCanvas.drawText(
                        "CCL",
                        30.dp.toPx(),
                        y - 4.dp.toPx(),
                        cclLabelPaint,
                    )
                }
            }

            // 0°C level label with snowflake (left side, 30dp from left edge)
            chart.freezingLevelKm.firstOrNull()?.let { firstFl ->
                val y = altitudeToY(
                    firstFl.altitudeKm, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                if (y in plotTop..plotBottom) {
                    val flLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(0x00, 0xBC, 0xD4)
                        textSize = with(density) { 10.sp.toPx() }
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                    canvas.nativeCanvas.drawText(
                        "❄ 0°C",
                        30.dp.toPx(),
                        y - 4.dp.toPx(),
                        flLabelPaint,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawWindArrow(
    centerX: Float,
    centerY: Float,
    directionDeg: Float,
    arrowSize: Float,
    speedKmh: Float,
    color: Color,
) {
    // Arrow points in the direction the wind is going TO (opposite of "from")
    val goingToDeg = (directionDeg + 180f) % 360f
    val angleRad = (goingToDeg - 90f) * PI.toFloat() / 180f
    val halfSize = arrowSize / 2f * 0.7f

    val tipX = centerX + cos(angleRad) * halfSize
    val tipY = centerY + sin(angleRad) * halfSize
    val tailX = centerX - cos(angleRad) * halfSize
    val tailY = centerY - sin(angleRad) * halfSize

    val strokeWidth = (1.5f + speedKmh / 40f).coerceAtMost(3.5f)

    drawLine(
        color = color,
        start = Offset(tailX, tailY),
        end = Offset(tipX, tipY),
        strokeWidth = strokeWidth,
    )

    val arrowLen = halfSize * 0.35f
    val arrowAngle = PI.toFloat() / 6f
    drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(
            tipX - cos(angleRad - arrowAngle) * arrowLen,
            tipY - sin(angleRad - arrowAngle) * arrowLen,
        ),
        strokeWidth = strokeWidth,
    )
    drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(
            tipX - cos(angleRad + arrowAngle) * arrowLen,
            tipY - sin(angleRad + arrowAngle) * arrowLen,
        ),
        strokeWidth = strokeWidth,
    )
}

private fun windSpeedColor(speedKmh: Float): Color {
    val normalized = (speedKmh / 60f).coerceIn(0f, 1f)
    val low = Color(0xFF4CAF50)    // Green — calm
    val medium = Color(0xFFFFC107)  // Amber — moderate
    val high = Color(0xFFE53935)    // Red — strong

    return if (normalized <= 0.5f) {
        lerp(low, medium, normalized / 0.5f)
    } else {
        lerp(medium, high, (normalized - 0.5f) / 0.5f)
    }
}

/** Background color for wind cells — same scale as windSpeedColor but with low alpha. */
private fun windSpeedBgColor(speedKmh: Float): Color {
    return windSpeedColor(speedKmh).copy(alpha = 0.25f)
}

private fun altitudeToY(
    altitudeKm: Float,
    minAltitudeKm: Float,
    maxAltitudeKm: Float,
    plotTop: Float,
    plotBottom: Float,
): Float {
    val normalizedAltitude = (altitudeKm - minAltitudeKm) / (maxAltitudeKm - minAltitudeKm)
    return plotBottom - normalizedAltitude * (plotBottom - plotTop)
}

private fun buildAltitudeTicks(
    minAltitudeKm: Float,
    maxAltitudeKm: Float,
    stepKm: Float,
): List<Float> {
    val ticks = mutableListOf(minAltitudeKm)
    var nextTick = ceil(minAltitudeKm / stepKm) * stepKm
    while (nextTick < maxAltitudeKm) {
        if (nextTick > minAltitudeKm + 0.001f) ticks += nextTick
        nextTick += stepKm
    }
    if (maxAltitudeKm - ticks.last() > 0.001f) ticks += maxAltitudeKm
    return ticks.distinctBy { (it * 100f).toInt() }
}

private const val WIND_MIN_ALTITUDE_KM = 0.4f
private const val WIND_MIN_VISIBLE_ALTITUDE_RANGE_KM = 0.75f

@Preview(name = "Wind Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun WindForecastViewPreview() {
    CloudbasePredictorTheme {
        WindForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.WIND),
        )
    }
}

@Preview(name = "Wind Loading", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun WindForecastViewLoadingPreview() {
    CloudbasePredictorTheme {
        WindForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.WIND,
                isLoading = true,
            ),
        )
    }
}

@Preview(name = "Wind Zoomed Out", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun WindForecastViewZoomedOutPreview() {
    CloudbasePredictorTheme {
        WindForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.WIND,
                topAltitudeKm = 6.5f,
            ),
        )
    }
}
