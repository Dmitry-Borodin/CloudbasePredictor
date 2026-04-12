package com.cloudbasepredictor.ui.screens.forecast.views

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.THERMIC_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ThermicForecastChartUiModel
import com.cloudbasepredictor.ui.screens.forecast.aggregatedForDisplay
import com.cloudbasepredictor.ui.screens.forecast.zoomedTopAltitudeKm
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@Composable
internal fun ThermicForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
) {
    ForecastChartCard(
        uiState = uiState,
        modifier = modifier.testTag(THERMIC_VIEW),
    ) { chartModifier ->
        ThermicForecastGrid(
            chart = uiState.thermicChart,
            visibleTopAltitudeKm = uiState.chartViewport.visibleTopAltitudeKm,
            onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
            modifier = chartModifier,
        )
    }
}

@Composable
private fun ThermicForecastGrid(
    chart: ThermicForecastChartUiModel,
    visibleTopAltitudeKm: Float,
    onVisibleTopAltitudeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = Color(0xFF667085)
    val cellTextColor = Color(0xFF111827)
    val cloudOutlineColor = Color(0xFF111111)
    val gridBackgroundColor = Color(0xFFF7F7F6)
    val outlineColor = Color(0xFF98A2B3)

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
    val cellLabelPaint = remember(density, cellTextColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cellTextColor.toArgb()
            textSize = with(density) { 20.sp.toPx() }
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
        if (chart.timeSlots.isEmpty()) {
            return@Canvas
        }

        val axisWidth = with(density) { 60.dp.toPx() }
        val outerHorizontalPadding = 0f
        val axisToPlotSpacing = 0f
        val bottomAxisHeight = with(density) { 38.dp.toPx() }
        val plotCornerRadius = 0f
        val tileInset = with(density) { 1.dp.toPx() }

        val plotLeft = outerHorizontalPadding + axisWidth + axisToPlotSpacing
        val plotTop = outerHorizontalPadding
        val plotRight = size.width - outerHorizontalPadding
        val plotBottom = size.height - bottomAxisHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        val effectiveTopAltitudeKm = max(
            visibleTopAltitudeKm,
            THERMIC_MIN_ALTITUDE_KM + MIN_VISIBLE_ALTITUDE_RANGE_KM,
        )
        val majorAltitudeTicks = buildAltitudeTicks(
            minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
            maxAltitudeKm = effectiveTopAltitudeKm,
            stepKm = thermicMajorAltitudeStepKm(effectiveTopAltitudeKm),
        )
        val minorAltitudeTicks = buildAltitudeTicks(
            minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
            maxAltitudeKm = effectiveTopAltitudeKm,
            stepKm = THERMIC_MINOR_ALTITUDE_STEP_KM,
        )

        if (plotWidth <= 0f || plotHeight <= 0f) {
            return@Canvas
        }

        val displayChart = chart.aggregatedForDisplay(
            timeBucketSlotCount = resolveTimeBucketSlotCount(
                plotWidth = plotWidth,
                rawTimeSlotCount = chart.timeSlots.size,
            ),
            altitudeBucketStepKm = resolveAltitudeBucketStepKm(
                plotHeight = plotHeight,
                visibleTopAltitudeKm = effectiveTopAltitudeKm,
                rawAltitudeStepKm = THERMIC_DATA_ALTITUDE_STEP_KM,
            ),
        )

        drawRoundRect(
            color = gridBackgroundColor,
            topLeft = Offset(outerHorizontalPadding, plotTop),
            size = Size(axisWidth, plotHeight),
            cornerRadius = CornerRadius(plotCornerRadius, plotCornerRadius),
        )
        drawRoundRect(
            color = gridBackgroundColor,
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            cornerRadius = CornerRadius(plotCornerRadius, plotCornerRadius),
        )

        val columnWidth = plotWidth / displayChart.timeSlots.size
        val timeIndexLookup = displayChart.timeSlots.withIndex().associate { (index, minute) ->
            minute to index
        }

        minorAltitudeTicks.forEach { altitudeKm ->
            val y = altitudeToY(
                altitudeKm = altitudeKm,
                minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                maxAltitudeKm = effectiveTopAltitudeKm,
                plotTop = plotTop,
                plotBottom = plotBottom,
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.15f),
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        displayChart.timeSlots.forEachIndexed { index, startMinute ->
            val x = plotLeft + (index * columnWidth)
            val boundaryAlpha = if (startMinute % THERMIC_MAJOR_TIME_STEP_MINUTES == 0) 0.42f else 0.18f

            drawLine(
                color = outlineColor.copy(alpha = boundaryAlpha),
                start = Offset(x, plotTop),
                end = Offset(x, plotBottom),
                strokeWidth = 1.dp.toPx(),
            )

            drawLine(
                color = outlineColor.copy(alpha = 0.12f),
                start = Offset(x + (columnWidth / 2f), plotTop),
                end = Offset(x + (columnWidth / 2f), plotBottom),
                strokeWidth = 1.dp.toPx(),
            )

            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(x + (columnWidth / 2f), plotBottom + 4.dp.toPx()),
                end = Offset(x + (columnWidth / 2f), plotBottom + 10.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }

        displayChart.cells.forEach { cell ->
            val timeIndex = timeIndexLookup[cell.startMinuteOfDayLocal] ?: return@forEach
            val visibleStartAltitudeKm = cell.startAltitudeKm.coerceAtLeast(THERMIC_MIN_ALTITUDE_KM)
            val visibleEndAltitudeKm = cell.endAltitudeKm.coerceAtMost(effectiveTopAltitudeKm)

            if (visibleEndAltitudeKm <= visibleStartAltitudeKm) {
                return@forEach
            }

            val topY = altitudeToY(
                altitudeKm = visibleEndAltitudeKm,
                minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                maxAltitudeKm = effectiveTopAltitudeKm,
                plotTop = plotTop,
                plotBottom = plotBottom,
            )
            val bottomY = altitudeToY(
                altitudeKm = visibleStartAltitudeKm,
                minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                maxAltitudeKm = effectiveTopAltitudeKm,
                plotTop = plotTop,
                plotBottom = plotBottom,
            )
            val cellHeight = bottomY - topY

            if (cellHeight <= tileInset * 2f) {
                return@forEach
            }

            drawRoundRect(
                color = thermicStrengthColor(cell.strengthMps),
                topLeft = Offset(
                    x = plotLeft + (timeIndex * columnWidth) + tileInset,
                    y = topY + tileInset,
                ),
                size = Size(
                    width = columnWidth - (tileInset * 2f),
                    height = cellHeight - (tileInset * 2f),
                ),
                cornerRadius = CornerRadius(tileInset * 2f, tileInset * 2f),
            )
        }

        majorAltitudeTicks.forEach { altitudeKm ->
            val y = altitudeToY(
                altitudeKm = altitudeKm,
                minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                maxAltitudeKm = effectiveTopAltitudeKm,
                plotTop = plotTop,
                plotBottom = plotBottom,
            )

            drawLine(
                color = outlineColor.copy(alpha = 0.34f),
                start = Offset(outerHorizontalPadding, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        drawLine(
            color = outlineColor.copy(alpha = 0.35f),
            start = Offset(plotRight, plotTop),
            end = Offset(plotRight, plotBottom),
            strokeWidth = 1.dp.toPx(),
        )

        drawRoundRect(
            color = outlineColor.copy(alpha = 0.4f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            cornerRadius = CornerRadius(plotCornerRadius, plotCornerRadius),
            style = Stroke(width = 1.dp.toPx()),
        )

        displayChart.cloudMarkers.forEach { marker ->
            val timeIndex = timeIndexLookup[marker.startMinuteOfDayLocal] ?: return@forEach
            if (marker.altitudeKm !in THERMIC_MIN_ALTITUDE_KM..effectiveTopAltitudeKm) {
                return@forEach
            }

            drawCloudMarker(
                center = Offset(
                    x = plotLeft + (timeIndex * columnWidth) + (columnWidth / 2f),
                    y = altitudeToY(
                        altitudeKm = marker.altitudeKm,
                        minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                        maxAltitudeKm = effectiveTopAltitudeKm,
                        plotTop = plotTop,
                        plotBottom = plotBottom,
                    ),
                ),
                width = minOf(columnWidth * 0.5f, 24.dp.toPx()),
                height = 13.dp.toPx(),
                fillColor = Color.White.copy(alpha = 0.94f),
                outlineColor = cloudOutlineColor,
            )
        }

        drawIntoCanvas { canvas ->
            // Radius-based dedup: skip labels whose center is within 30sp of an
            // already-drawn label, so numbers never overlap.
            val dedupRadiusPx = with(density) { 30.sp.toPx() }
            val dedupRadiusSq = dedupRadiusPx * dedupRadiusPx
            val drawnCenters = mutableListOf<Offset>()

            // Sort cells so that strongest thermals get labels first
            val sortedCells = displayChart.cells.sortedByDescending { it.strengthMps }

            sortedCells.forEach cellLoop@{ cell ->
                val timeIndex = timeIndexLookup[cell.startMinuteOfDayLocal] ?: return@cellLoop

                val visibleStartAltitudeKm =
                    cell.startAltitudeKm.coerceAtLeast(THERMIC_MIN_ALTITUDE_KM)
                val visibleEndAltitudeKm =
                    cell.endAltitudeKm.coerceAtMost(effectiveTopAltitudeKm)

                if (visibleEndAltitudeKm <= visibleStartAltitudeKm) return@cellLoop

                val topY = altitudeToY(
                    altitudeKm = visibleEndAltitudeKm,
                    minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                    maxAltitudeKm = effectiveTopAltitudeKm,
                    plotTop = plotTop,
                    plotBottom = plotBottom,
                )
                val bottomY = altitudeToY(
                    altitudeKm = visibleStartAltitudeKm,
                    minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                    maxAltitudeKm = effectiveTopAltitudeKm,
                    plotTop = plotTop,
                    plotBottom = plotBottom,
                )

                if ((bottomY - topY) < cellLabelPaint.textSize + 2.dp.toPx()) {
                    return@cellLoop
                }

                val centerX = plotLeft + (timeIndex * columnWidth) + (columnWidth / 2f)
                val centerY = topY + ((bottomY - topY) / 2f)
                val center = Offset(centerX, centerY)

                // Skip if too close to an already-drawn label
                val tooClose = drawnCenters.any { existing ->
                    val dx = existing.x - center.x
                    val dy = existing.y - center.y
                    dx * dx + dy * dy < dedupRadiusSq
                }
                if (tooClose) return@cellLoop

                drawnCenters += center

                canvas.nativeCanvas.drawText(
                    formatThermicStrengthLabel(cell.strengthMps),
                    centerX,
                    centerY + (cellLabelPaint.textSize * 0.35f),
                    cellLabelPaint,
                )
            }

            majorAltitudeTicks.forEach { altitudeKm ->
                val y = altitudeToY(
                    altitudeKm = altitudeKm,
                    minAltitudeKm = THERMIC_MIN_ALTITUDE_KM,
                    maxAltitudeKm = effectiveTopAltitudeKm,
                    plotTop = plotTop,
                    plotBottom = plotBottom,
                )
                canvas.nativeCanvas.drawText(
                    formatAltitudeLabel(altitudeKm),
                    outerHorizontalPadding + 8.dp.toPx(),
                    y + (axisLabelPaint.textSize * 0.35f),
                    axisLabelPaint,
                )
            }

            canvas.nativeCanvas.drawText(
                "km",
                outerHorizontalPadding + 8.dp.toPx(),
                plotBottom + unitLabelPaint.textSize + 12.dp.toPx(),
                unitLabelPaint,
            )

            displayChart.timeSlots.forEachIndexed { index, startMinute ->
                if (!shouldDrawTimeLabel(
                        startMinuteOfDayLocal = startMinute,
                        displayedSlotCount = displayChart.timeSlots.size,
                    )
                ) {
                    return@forEachIndexed
                }

                canvas.nativeCanvas.drawText(
                    formatTimeLabel(startMinute),
                    plotLeft + (index * columnWidth) + (columnWidth / 2f),
                    plotBottom + hourLabelPaint.textSize + 14.dp.toPx(),
                    hourLabelPaint,
                )
            }
        }
    }
}

private fun resolveTimeBucketSlotCount(
    plotWidth: Float,
    rawTimeSlotCount: Int,
): Int {
    val rawColumnWidth = plotWidth / rawTimeSlotCount.coerceAtLeast(1)
    return max(1, ceil(MIN_TIME_BUCKET_WIDTH_PX / rawColumnWidth).toInt())
}

private fun resolveAltitudeBucketStepKm(
    plotHeight: Float,
    visibleTopAltitudeKm: Float,
    rawAltitudeStepKm: Float,
): Float {
    val rawRowHeight = plotHeight * (rawAltitudeStepKm / visibleTopAltitudeKm.coerceAtLeast(rawAltitudeStepKm))
    val bucketRowCount = max(1, ceil(MIN_ALTITUDE_BUCKET_HEIGHT_PX / rawRowHeight).toInt())
    return rawAltitudeStepKm * bucketRowCount
}

private fun shouldDrawTimeLabel(
    startMinuteOfDayLocal: Int,
    displayedSlotCount: Int,
): Boolean {
    return if (displayedSlotCount <= 8) {
        true
    } else {
        startMinuteOfDayLocal % THERMIC_MAJOR_TIME_STEP_MINUTES == 0
    }
}

private fun thermicMajorAltitudeStepKm(maxAltitudeKm: Float): Float {
    return if (maxAltitudeKm <= 3.5f) 0.5f else 1f
}

private fun buildAltitudeTicks(
    minAltitudeKm: Float,
    maxAltitudeKm: Float,
    stepKm: Float,
): List<Float> {
    val ticks = mutableListOf(minAltitudeKm)
    var nextTick = ceil(minAltitudeKm / stepKm) * stepKm

    while (nextTick < maxAltitudeKm) {
        if (nextTick > minAltitudeKm + ALTITUDE_EPSILON) {
            ticks += nextTick
        }
        nextTick += stepKm
    }

    if (maxAltitudeKm - ticks.last() > ALTITUDE_EPSILON) {
        ticks += maxAltitudeKm
    }

    return ticks.distinctBy { tick ->
        (tick * 100f).toInt()
    }
}

private fun altitudeToY(
    altitudeKm: Float,
    minAltitudeKm: Float,
    maxAltitudeKm: Float,
    plotTop: Float,
    plotBottom: Float,
): Float {
    val normalizedAltitude = (altitudeKm - minAltitudeKm) / (maxAltitudeKm - minAltitudeKm)
    return plotBottom - (normalizedAltitude * (plotBottom - plotTop))
}

private fun thermicStrengthColor(strengthMps: Float): Color {
    val clampedStrength = strengthMps.coerceIn(0f, MAX_THERMIC_STRENGTH_MPS)
    val normalized = clampedStrength / MAX_THERMIC_STRENGTH_MPS
    val colorStops = listOf(
        0f to Color(0xFFFCE0AE),
        0.17f to Color(0xFFFFFF00),
        0.33f to Color(0xFF00F6B2),
        0.5f to Color(0xFF7BD0BC),
        0.67f to Color(0xFF19C8E0),
        0.83f to Color(0xFF6A95E6),
        1f to Color(0xFF2015F3),
    )

    val lowerStop = colorStops.lastOrNull { it.first <= normalized } ?: colorStops.first()
    val upperStop = colorStops.firstOrNull { it.first >= normalized } ?: colorStops.last()

    if (lowerStop.first == upperStop.first) {
        return lowerStop.second
    }

    val fraction = (normalized - lowerStop.first) / (upperStop.first - lowerStop.first)
    return lerp(lowerStop.second, upperStop.second, fraction)
}

private fun DrawScope.drawCloudMarker(
    center: Offset,
    width: Float,
    height: Float,
    fillColor: Color,
    outlineColor: Color,
) {
    val strokeWidth = max(width * 0.08f, 1.5f)
    val baseHeight = height * 0.36f
    val baseWidth = width * 0.66f
    val baseTop = center.y + (height * 0.05f)
    val baseLeft = center.x - (baseWidth / 2f)
    val radiusSmall = height * 0.23f
    val radiusMedium = height * 0.28f
    val radiusLarge = height * 0.33f

    drawRoundRect(
        color = fillColor,
        topLeft = Offset(baseLeft, baseTop),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(baseHeight / 2f, baseHeight / 2f),
    )
    drawCircle(
        color = fillColor,
        radius = radiusMedium,
        center = Offset(center.x - (width * 0.18f), center.y),
    )
    drawCircle(
        color = fillColor,
        radius = radiusLarge,
        center = Offset(center.x, center.y - (height * 0.1f)),
    )
    drawCircle(
        color = fillColor,
        radius = radiusSmall,
        center = Offset(center.x + (width * 0.2f), center.y + (height * 0.02f)),
    )

    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(baseLeft, baseTop),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(baseHeight / 2f, baseHeight / 2f),
        style = Stroke(width = strokeWidth),
    )
    drawCircle(
        color = outlineColor,
        radius = radiusMedium,
        center = Offset(center.x - (width * 0.18f), center.y),
        style = Stroke(width = strokeWidth),
    )
    drawCircle(
        color = outlineColor,
        radius = radiusLarge,
        center = Offset(center.x, center.y - (height * 0.1f)),
        style = Stroke(width = strokeWidth),
    )
    drawCircle(
        color = outlineColor,
        radius = radiusSmall,
        center = Offset(center.x + (width * 0.2f), center.y + (height * 0.02f)),
        style = Stroke(width = strokeWidth),
    )
}

private fun formatThermicStrengthLabel(value: Float): String {
    return String.format(Locale.US, "%.1f", value)
}

private fun formatAltitudeLabel(altitudeKm: Float): String {
    return String.format(Locale.US, "%.1f", altitudeKm)
}

private fun formatTimeLabel(startMinuteOfDayLocal: Int): String {
    val hour = startMinuteOfDayLocal / 60
    val minute = startMinuteOfDayLocal % 60
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

private const val THERMIC_MIN_ALTITUDE_KM = 0f
private const val THERMIC_MINOR_ALTITUDE_STEP_KM = 0.25f
private const val MIN_VISIBLE_ALTITUDE_RANGE_KM = 0.75f
private const val ALTITUDE_EPSILON = 0.001f
private const val THERMIC_DATA_ALTITUDE_STEP_KM = 0.05f
private const val THERMIC_MAJOR_TIME_STEP_MINUTES = 180
private const val MAX_THERMIC_STRENGTH_MPS = 3f
private const val MIN_TIME_BUCKET_WIDTH_PX = 28f
private const val MIN_ALTITUDE_BUCKET_HEIGHT_PX = 20f

@Preview(name = "Thermic Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun ThermicForecastViewPreview() {
    CloudbasePredictorTheme {
        ThermicForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.THERMIC),
        )
    }
}

@Preview(name = "Thermic Zoomed Out", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun ThermicForecastViewZoomedOutPreview() {
    CloudbasePredictorTheme {
        ThermicForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.THERMIC,
                topAltitudeKm = 6.5f,
            ),
        )
    }
}
