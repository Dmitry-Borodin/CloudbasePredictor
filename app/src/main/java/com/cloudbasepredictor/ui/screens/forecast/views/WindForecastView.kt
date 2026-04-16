package com.cloudbasepredictor.ui.screens.forecast.views

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_ALTITUDE_UNIT
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_TIME_AXIS
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.WIND_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.WindForecastChartUiModel
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
            .background(MaterialTheme.colorScheme.surface)
            .testTag(WIND_VIEW),
    ) {
        WindChartCanvas(
            chart = uiState.windChart,
            visibleTopAltitudeKm = uiState.chartViewport.visibleTopAltitudeKm,
            elevationKm = uiState.elevationKm,
            onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WIND_BOTTOM_AXIS_HEIGHT)
                .align(Alignment.BottomCenter)
                .testTag(WIND_TIME_AXIS),
        ) {
            WindBottomAxis(
                hours = uiState.windChart.hours,
                modifier = Modifier.fillMaxSize(),
            )
            // "km" unit label aligned with the Y-axis
            Text(
                text = "km",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp)
                    .testTag(WIND_ALTITUDE_UNIT),
            )
        }

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
private fun WindBottomAxis(
    hours: List<Int>,
    modifier: Modifier = Modifier,
) {
    val firstHour = hours.firstOrNull() ?: return
    val visibleLabels = hours.filter { (it - firstHour) % 3 == 0 }

    Row(
        modifier = modifier.padding(end = 8.dp, bottom = 8.dp),
    ) {
        Spacer(modifier = Modifier.width(WIND_AXIS_WIDTH))
        if (visibleLabels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                visibleLabels.forEach { hour ->
                    Text(
                        text = String.format(Locale.US, "%02d", hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WindChartCanvas(
    chart: WindForecastChartUiModel,
    visibleTopAltitudeKm: Float,
    elevationKm: Float,
    onVisibleTopAltitudeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
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
    val tooltipPaint = remember(density, onSurfaceColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurfaceColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }
    var crosshairPos by remember { mutableStateOf<Offset?>(null) }
    val latestVisibleTopAltitudeKm = rememberUpdatedState(visibleTopAltitudeKm)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        crosshairPos = down.position
                        do {
                            val event = awaitPointerEvent()
                            val primary = event.changes.firstOrNull() ?: break
                            if (primary.pressed && event.changes.size == 1) {
                                crosshairPos = primary.position
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
            }
            .pointerInput(Unit) {
                detectForecastZoomGestures(
                    currentTopAltitudeKm = { latestVisibleTopAltitudeKm.value },
                    onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
                )
            },
    ) {
        drawRect(
            color = surfaceColor,
            size = size,
        )

        val axisWidth = with(density) { 60.dp.toPx() }
        val bottomAxisHeight = with(density) { 38.dp.toPx() }
        val arrowSizePx = with(density) { 48.dp.toPx() }

        val plotLeft = axisWidth
        val plotTop = 0f
        val plotRight = size.width
        val plotBottom = size.height - bottomAxisHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        val minAltitudeKm = elevationKm
        val effectiveTopAltitudeKm = max(
            elevationKm + visibleTopAltitudeKm,
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
        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
        )

        val columnWidth = plotWidth / chart.hours.size

        // Use altitude bands for display
        val visibleBands = chart.altitudeBands.filter {
            it.topKm > minAltitudeKm && it.bottomKm < effectiveTopAltitudeKm
        }
        val visibleAltitudes = visibleBands.map { it.centerKm }
        if (visibleAltitudes.isEmpty()) return@Canvas

        // Average band height for arrow clustering
        val avgBandHeightKm = if (visibleBands.size > 1) {
            (visibleBands.last().topKm - visibleBands.first().bottomKm) / visibleBands.size
        } else 0.25f
        val bandHeightPx = plotHeight * avgBandHeightKm / (effectiveTopAltitudeKm - minAltitudeKm)

        // Clustering: skip arrows if cells are too small
        val altCluster = max(1, ceil(arrowSizePx * 1.1f / bandHeightPx).toInt())
        val hourCluster = max(1, ceil(arrowSizePx * 1.1f / columnWidth).toInt())

        // ── Wind speed background using band boundaries ──────────
        chart.hours.forEachIndexed { hourIndex, hour ->
            val x = plotLeft + hourIndex * columnWidth
            visibleBands.forEach { band ->
                val cell = chart.cells.find { it.hour == hour && it.altitudeKm == band.centerKm }
                    ?: return@forEach
                val topY = altitudeToY(
                    band.topKm.coerceAtMost(effectiveTopAltitudeKm),
                    minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                val bottomY = altitudeToY(
                    band.bottomKm.coerceAtLeast(minAltitudeKm),
                    minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
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

            clusteredAltitudes.forEach clusteredAlt@{ altKm ->
                // Collect cells in the altitude cluster range for averaging
                val altIdx = visibleAltitudes.indexOf(altKm)
                val clusterAlts = visibleAltitudes.subList(
                    altIdx, (altIdx + altCluster).coerceAtMost(visibleAltitudes.size),
                )
                val clusterCells = clusterAlts.mapNotNull { a ->
                    chart.cells.find { it.hour == hour && it.altitudeKm == a }
                }
                if (clusterCells.isEmpty()) return@clusteredAlt

                val avgSpeed = clusterCells.map { it.speedKmh }.average().toFloat()
                val avgDir = averageWindDirection(clusterCells.map { it.directionDeg })
                val clusterBandBottom = visibleBands.find { it.centerKm == clusterAlts.first() }?.bottomKm ?: altKm
                val clusterBandTop = visibleBands.find { it.centerKm == clusterAlts.last() }?.topKm ?: altKm
                val cellCenterY = altitudeToY(
                    (clusterBandBottom + clusterBandTop) / 2f,
                    minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                )
                if (cellCenterY !in plotTop..plotBottom) return@clusteredAlt

                // Draw arrow — black in light theme (onSurface)
                val arrowDrawSize = min(
                    arrowSizePx,
                    min(columnWidth * hourCluster * 0.8f, bandHeightPx * altCluster * 0.8f),
                )
                drawWindArrow(
                    centerX = cellCenterX,
                    centerY = cellCenterY,
                    directionDeg = avgDir,
                    arrowSize = arrowDrawSize,
                    speedKmh = avgSpeed,
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

                clusteredAltitudes.forEach clusteredAltLabel@{ altKm ->
                    val altIdx = visibleAltitudes.indexOf(altKm)
                    val clusterAlts = visibleAltitudes.subList(
                        altIdx, (altIdx + altCluster).coerceAtMost(visibleAltitudes.size),
                    )
                    val clusterCells = clusterAlts.mapNotNull { a ->
                        chart.cells.find { it.hour == hour && it.altitudeKm == a }
                    }
                    if (clusterCells.isEmpty()) return@clusteredAltLabel

                    val avgSpeed = clusterCells.map { it.speedKmh }.average().toFloat()
                    val clusterBandBottom = visibleBands.find { it.centerKm == clusterAlts.first() }?.bottomKm ?: altKm
                    val clusterBandTop = visibleBands.find { it.centerKm == clusterAlts.last() }?.topKm ?: altKm
                    val cellCenterY = altitudeToY(
                        (clusterBandBottom + clusterBandTop) / 2f,
                        minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom,
                    )
                    if (cellCenterY !in plotTop..plotBottom) return@clusteredAltLabel

                    // Speed text below the arrow
                    val arrowDrawSize = min(
                        arrowSizePx,
                        min(columnWidth * hourCluster * 0.8f, bandHeightPx * altCluster * 0.8f),
                    )
                    canvas.nativeCanvas.drawText(
                        "${avgSpeed.toInt()}",
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

        // ── Crosshair overlay ──────────────────────────────────
        crosshairPos?.let { pos ->
            val cx = pos.x.coerceIn(plotLeft, plotRight)
            val cy = pos.y.coerceIn(plotTop, plotBottom)

            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.5f),
                start = Offset(cx, plotTop),
                end = Offset(cx, plotBottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash,
            )
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.5f),
                start = Offset(plotLeft, cy),
                end = Offset(plotRight, cy),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dash,
            )

            val reticleR = with(density) { 18.dp.toPx() }
            drawCircle(
                color = onSurfaceColor.copy(alpha = 0.7f),
                radius = reticleR,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx()),
            )
            val tick = with(density) { 4.dp.toPx() }
            for (angleDeg in listOf(0f, 90f, 180f, 270f)) {
                val rad = angleDeg * PI.toFloat() / 180f
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    start = Offset(
                        cx + cos(rad) * (reticleR - tick),
                        cy + sin(rad) * (reticleR - tick),
                    ),
                    end = Offset(
                        cx + cos(rad) * (reticleR + tick),
                        cy + sin(rad) * (reticleR + tick),
                    ),
                    strokeWidth = 2.dp.toPx(),
                )
            }

            val altKm = yToAltitude(cy, minAltitudeKm, effectiveTopAltitudeKm, plotTop, plotBottom)
            val hourIdx = ((cx - plotLeft) / columnWidth).toInt()
                .coerceIn(0, chart.hours.size - 1)
            val hour = chart.hours[hourIdx]
            val cell = chart.cells
                .filter { it.hour == hour }
                .minByOrNull { kotlin.math.abs(it.altitudeKm - altKm) }

            val tooltipLines = mutableListOf<String>()
            tooltipLines += String.format(Locale.US, "%02dh  %.1f km", hour, altKm)
            if (cell != null) {
                tooltipLines += "${cell.speedKmh.toInt()} km/h  ${cell.directionDeg.toInt()}°"
            }

            val lineH = tooltipPaint.textSize * 1.3f
            val maxTextW = tooltipLines.maxOf { tooltipPaint.measureText(it) }
            val padH = with(density) { 8.dp.toPx() }
            val padV = with(density) { 6.dp.toPx() }
            val ttW = maxTextW + padH * 2
            val ttH = lineH * tooltipLines.size + padV * 2
            val ttX = if (cx + reticleR + ttW + 8.dp.toPx() < plotRight)
                cx + reticleR + 8.dp.toPx()
            else
                cx - reticleR - ttW - 8.dp.toPx()
            val ttY = (cy - ttH / 2f).coerceIn(plotTop, plotBottom - ttH)

            drawRoundRect(
                color = gridBackgroundColor.copy(alpha = 0.92f),
                topLeft = Offset(ttX, ttY),
                size = Size(ttW, ttH),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.3f),
                topLeft = Offset(ttX, ttY),
                size = Size(ttW, ttH),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawIntoCanvas { canvas ->
                tooltipLines.forEachIndexed { idx, line ->
                    canvas.nativeCanvas.drawText(
                        line,
                        ttX + padH,
                        ttY + padV + (idx + 1) * lineH - lineH * 0.15f,
                        tooltipPaint,
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
    val colorStops = listOf(
        0f to Color(0xFF81C784),     // Soft green — calm
        0.15f to Color(0xFF4CAF50),  // Green — light breeze
        0.3f to Color(0xFFCDDC39),   // Lime — moderate
        0.45f to Color(0xFFFFEB3B),  // Yellow — fresh breeze
        0.6f to Color(0xFFFFC107),   // Amber — strong
        0.75f to Color(0xFFFF9800),  // Orange — near gale
        0.9f to Color(0xFFE53935),   // Red — gale
        1f to Color(0xFF9C27B0),     // Purple — storm
    )

    val lowerStop = colorStops.lastOrNull { it.first <= normalized } ?: colorStops.first()
    val upperStop = colorStops.firstOrNull { it.first >= normalized } ?: colorStops.last()

    if (lowerStop.first == upperStop.first) return lowerStop.second

    val fraction = (normalized - lowerStop.first) / (upperStop.first - lowerStop.first)
    return lerp(lowerStop.second, upperStop.second, fraction)
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

private fun yToAltitude(
    y: Float,
    minAltitudeKm: Float,
    maxAltitudeKm: Float,
    plotTop: Float,
    plotBottom: Float,
): Float {
    val normalizedAltitude = (plotBottom - y) / (plotBottom - plotTop)
    return minAltitudeKm + normalizedAltitude * (maxAltitudeKm - minAltitudeKm)
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

private const val WIND_MIN_VISIBLE_ALTITUDE_RANGE_KM = 0.75f
private val WIND_AXIS_WIDTH = 60.dp
private val WIND_BOTTOM_AXIS_HEIGHT = 38.dp

/** Average wind directions by decomposing into unit vectors and recombining. */
private fun averageWindDirection(directions: List<Float>): Float {
    if (directions.isEmpty()) return 0f
    var sinSum = 0f
    var cosSum = 0f
    directions.forEach { deg ->
        val rad = deg * PI.toFloat() / 180f
        sinSum += sin(rad)
        cosSum += cos(rad)
    }
    val avgRad = kotlin.math.atan2(sinSum, cosSum)
    return ((avgRad * 180f / PI.toFloat()) + 360f) % 360f
}

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

@Preview(
    name = "Wind Dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WindForecastViewDarkPreview() {
    CloudbasePredictorTheme(darkTheme = true) {
        WindForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.WIND),
        )
    }
}
