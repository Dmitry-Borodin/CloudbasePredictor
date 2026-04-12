package com.cloudbasepredictor.ui.screens.forecast.views

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.STUVE_DRY_ADIABAT_THETAS_K
import com.cloudbasepredictor.ui.screens.forecast.STUVE_MIXING_RATIO_VALUES_GKG
import com.cloudbasepredictor.ui.screens.forecast.STUVE_MOIST_ADIABAT_THETAS_K
import com.cloudbasepredictor.ui.screens.forecast.STUVE_PRESSURE_LEVELS
import com.cloudbasepredictor.ui.screens.forecast.StuveForecastChartUiModel
import com.cloudbasepredictor.ui.screens.forecast.StuveProfilePoint
import com.cloudbasepredictor.ui.screens.forecast.dryAdiabatTemperatureC
import com.cloudbasepredictor.ui.screens.forecast.mixingRatioTemperatureC
import com.cloudbasepredictor.ui.screens.forecast.moistAdiabatTemperatureC
import com.cloudbasepredictor.ui.screens.forecast.pressureToApproxHeightMeters
import com.cloudbasepredictor.ui.screens.forecast.zoomedTopAltitudeKm
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Composable
internal fun StuveForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
    onStuveHourChanged: (Int) -> Unit = {},
) {
    val stuveChart = uiState.stuveChart

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(STUVE_VIEW),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            StuveDiagramCanvas(
                chart = stuveChart,
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

        StuveTimeSlider(
            selectedHour = stuveChart.selectedHour,
            onHourChanged = onStuveHourChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun StuveTimeSlider(
    selectedHour: Int,
    onHourChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember(selectedHour) {
        mutableFloatStateOf(selectedHour.toFloat())
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "06",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onHourChanged(sliderValue.toInt()) },
                valueRange = 6f..22f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "22",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = String.format(Locale.US, "%02d:00", sliderValue.toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun StuveDiagramCanvas(
    chart: StuveForecastChartUiModel,
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
        fraction = 0.02f,
    )
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val axisLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
    }
    val altitudeLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
    }
    val tempLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val mixingRatioLabelPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF5588CC).toArgb()
            textSize = with(density) { 8.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
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
        // Convert visibleTopAltitudeKm to a top pressure for the diagram
        val stuveTopPressure = altitudeKmToApproxPressureHpa(visibleTopAltitudeKm)
            .coerceIn(STUVE_MIN_TOP_PRESSURE, STUVE_BOTTOM_PRESSURE - 50f)
        val leftAxisWidth = with(density) { 40.dp.toPx() }
        val rightAltitudeWidth = with(density) { 40.dp.toPx() }
        val rightWindWidth = with(density) { 56.dp.toPx() }
        val bottomAxisHeight = with(density) { 28.dp.toPx() }
        val topPadding = with(density) { 16.dp.toPx() }

        val plotLeft = leftAxisWidth
        val plotTop = topPadding
        val plotRight = size.width - rightAltitudeWidth - rightWindWidth
        val plotBottom = size.height - bottomAxisHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        // Background
        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
        )

        // --- Reference lines ---

        // Isotherms (vertical lines)
        val tempMin = -80f
        val tempMax = 45f
        val tempStep = 10f
        var t = tempMin
        while (t <= tempMax) {
            val x = temperatureToX(t, tempMin, tempMax, plotLeft, plotRight)
            val alpha = if (t.toInt() % 20 == 0) 0.35f else 0.15f
            drawLine(
                color = outlineColor.copy(alpha = alpha),
                start = Offset(x, plotTop),
                end = Offset(x, plotBottom),
                strokeWidth = 1.dp.toPx(),
            )
            t += tempStep
        }

        // Isobars (horizontal lines)
        val pressureLabels = listOf(
            1000f, 950f, 900f, 850f, 800f, 750f, 700f, 650f, 600f,
            550f, 500f, 450f, 400f, 350f, 300f, 250f, 200f,
        ).filter { it >= stuveTopPressure }
        pressureLabels.forEach { p ->
            val y = pressureToY(p, plotTop, plotBottom, stuveTopPressure)
            val alpha = if (p.toInt() % 200 == 0) 0.4f else 0.2f
            drawLine(
                color = outlineColor.copy(alpha = alpha),
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Dry adiabats (straight green lines)
        STUVE_DRY_ADIABAT_THETAS_K.forEach { theta ->
            drawDryAdiabat(
                thetaK = theta,
                tempMin = tempMin,
                tempMax = tempMax,
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                topPressure = stuveTopPressure,
                color = Color(0xFF44AA44).copy(alpha = 0.35f),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Moist adiabats (dashed cyan curves)
        STUVE_MOIST_ADIABAT_THETAS_K.forEach { thetaW ->
            drawMoistAdiabat(
                thetaWK = thetaW,
                tempMin = tempMin,
                tempMax = tempMax,
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                topPressure = stuveTopPressure,
                color = Color(0xFF00AACC).copy(alpha = 0.3f),
                strokeWidth = 1.dp.toPx(),
                dashOn = 6.dp.toPx(),
                dashOff = 4.dp.toPx(),
            )
        }

        // Mixing ratio lines (dotted blue)
        STUVE_MIXING_RATIO_VALUES_GKG.forEach { w ->
            drawMixingRatioLine(
                wGKg = w,
                tempMin = tempMin,
                tempMax = tempMax,
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                topPressure = stuveTopPressure,
                color = Color(0xFF5588CC).copy(alpha = 0.3f),
                strokeWidth = 1.dp.toPx(),
                dotOn = 3.dp.toPx(),
                dotOff = 4.dp.toPx(),
            )
        }

        // --- Data profiles ---

        // Temperature profile (red, thick)
        drawProfile(
            points = chart.temperatureProfile,
            tempMin = tempMin,
            tempMax = tempMax,
            plotLeft = plotLeft,
            plotRight = plotRight,
            plotTop = plotTop,
            plotBottom = plotBottom,
            topPressure = stuveTopPressure,
            color = Color(0xFFDD2222),
            strokeWidth = 2.5f.dp.toPx(),
            drawDataDots = true,
            dataDotRadius = 4.dp.toPx(),
        )

        // Dewpoint profile (blue)
        drawProfile(
            points = chart.dewpointProfile,
            tempMin = tempMin,
            tempMax = tempMax,
            plotLeft = plotLeft,
            plotRight = plotRight,
            plotTop = plotTop,
            plotBottom = plotBottom,
            topPressure = stuveTopPressure,
            color = Color(0xFF2255CC),
            strokeWidth = 2f.dp.toPx(),
            drawDataDots = true,
            dataDotRadius = 4.dp.toPx(),
        )

        // Parcel ascent path (dashed black — thermal prediction line)
        drawProfile(
            points = chart.parcelAscentPath,
            tempMin = tempMin,
            tempMax = tempMax,
            plotLeft = plotLeft,
            plotRight = plotRight,
            plotTop = plotTop,
            plotBottom = plotBottom,
            topPressure = stuveTopPressure,
            color = Color(0xFF333333),
            strokeWidth = 2f.dp.toPx(),
            dashOn = 8.dp.toPx(),
            dashOff = 6.dp.toPx(),
        )

        // LCL marker
        chart.lclPressureHpa?.let { lcl ->
            val lclY = pressureToY(lcl, plotTop, plotBottom, stuveTopPressure)
            drawLine(
                color = Color(0xFF888888).copy(alpha = 0.6f),
                start = Offset(plotLeft, lclY),
                end = Offset(plotRight, lclY),
                strokeWidth = 1.5f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(4.dp.toPx(), 6.dp.toPx()),
                ),
            )
        }

        // --- Plot outline ---
        drawRect(
            color = outlineColor.copy(alpha = 0.5f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            style = Stroke(width = 1.dp.toPx()),
        )

        // --- Wind barbs on the right ---
        chart.windBarbs.forEach { barb ->
            val y = pressureToY(barb.pressureHpa, plotTop, plotBottom, stuveTopPressure)
            if (y in plotTop..plotBottom) {
                drawWindBarb(
                    centerX = plotRight + rightAltitudeWidth + rightWindWidth / 2f,
                    centerY = y,
                    speedKmh = barb.speedKmh,
                    directionDeg = barb.directionDeg,
                    barbSize = with(density) { 20.dp.toPx() },
                    color = onSurfaceColor,
                )
            }
        }

        // --- Axis labels ---
        drawIntoCanvas { canvas ->
            // Pressure labels (left axis — pressure only)
            pressureLabels.forEach { p ->
                val y = pressureToY(p, plotTop, plotBottom, stuveTopPressure)
                if (y in plotTop..plotBottom) {
                    canvas.nativeCanvas.drawText(
                        "${p.toInt()}",
                        leftAxisWidth - 4.dp.toPx(),
                        y + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }

            // Temperature labels (bottom axis)
            var tempLabel = tempMin
            while (tempLabel <= tempMax) {
                if (tempLabel.toInt() % 20 == 0) {
                    val x = temperatureToX(
                        tempLabel, tempMin, tempMax, plotLeft, plotRight,
                    )
                    canvas.nativeCanvas.drawText(
                        "${tempLabel.toInt()}°",
                        x,
                        plotBottom + tempLabelPaint.textSize + 6.dp.toPx(),
                        tempLabelPaint,
                    )
                }
                tempLabel += tempStep
            }

            // Mixing ratio labels (top of plot)
            STUVE_MIXING_RATIO_VALUES_GKG.forEach { w ->
                val tempAtTop = mixingRatioTemperatureC(w, stuveTopPressure)
                if (tempAtTop in tempMin..tempMax) {
                    val x = temperatureToX(
                        tempAtTop, tempMin, tempMax, plotLeft, plotRight,
                    )
                    canvas.nativeCanvas.drawText(
                        if (w < 1f) String.format(Locale.US, "%.1f", w)
                        else "${w.toInt()}",
                        x,
                        plotTop - 2.dp.toPx(),
                        mixingRatioLabelPaint,
                    )
                }
            }

            // LCL label
            chart.lclPressureHpa?.let { lcl ->
                val lclY = pressureToY(lcl, plotTop, plotBottom, stuveTopPressure)
                if (lclY in plotTop..plotBottom) {
                    val lclLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color(0xFF666666).toArgb()
                        textSize = with(density) { 9.sp.toPx() }
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                    canvas.nativeCanvas.drawText(
                        "LCL",
                        plotLeft + 4.dp.toPx(),
                        lclY - 3.dp.toPx(),
                        lclLabelPaint,
                    )
                }
            }

            // Wind speed labels
            chart.windBarbs.forEach { barb ->
                val y = pressureToY(barb.pressureHpa, plotTop, plotBottom, stuveTopPressure)
                if (y in plotTop..plotBottom) {
                    val windLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = axisLabelColor.toArgb()
                        textSize = with(density) { 8.sp.toPx() }
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(
                        "${barb.speedKmh.toInt()}",
                        plotRight + rightAltitudeWidth + rightWindWidth / 2f,
                        y + with(density) { 22.dp.toPx() },
                        windLabelPaint,
                    )
                }
            }

            // Altitude labels (right axis, meters)
            pressureLabels.forEach { p ->
                val y = pressureToY(p, plotTop, plotBottom, stuveTopPressure)
                if (y in plotTop..plotBottom) {
                    val heightM = pressureToApproxHeightMeters(p)
                    val label = if (heightM >= 1000) {
                        String.format(Locale.US, "%.1fk", heightM / 1000f)
                    } else {
                        "${heightM}m"
                    }
                    canvas.nativeCanvas.drawText(
                        label,
                        plotRight + 4.dp.toPx(),
                        y + altitudeLabelPaint.textSize * 0.35f,
                        altitudeLabelPaint,
                    )
                }
            }
        }
    }
}

// --- Coordinate mapping ---

private const val STUVE_MIN_TOP_PRESSURE = 200f
private const val STUVE_BOTTOM_PRESSURE = 1050f
private const val STUVE_KAPPA = 0.286f

/** Convert altitude in km to an approximate pressure in hPa (inverse of ISA table). */
private fun altitudeKmToApproxPressureHpa(altitudeKm: Float): Float {
    // Simple barometric approximation: P = 1013.25 * (1 - 0.0000225577 * h)^5.25588
    val hMeters = (altitudeKm * 1000f).coerceAtLeast(0f)
    return (1013.25f * (1f - 0.0000225577f * hMeters).pow(5.25588f))
}

private fun pressureToY(
    pressureHpa: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
): Float {
    val pNorm = (pressureHpa.pow(STUVE_KAPPA) - topPressure.pow(STUVE_KAPPA)) /
        (STUVE_BOTTOM_PRESSURE.pow(STUVE_KAPPA) - topPressure.pow(STUVE_KAPPA))
    return plotTop + pNorm * (plotBottom - plotTop)
}

private fun temperatureToX(
    temperatureC: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
): Float {
    val tNorm = (temperatureC - tempMin) / (tempMax - tempMin)
    return plotLeft + tNorm * (plotRight - plotLeft)
}

// --- Drawing helpers ---

private fun DrawScope.drawDryAdiabat(
    thetaK: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    color: Color,
    strokeWidth: Float,
) {
    val pressures = STUVE_PRESSURE_LEVELS.filter {
        it in topPressure..STUVE_BOTTOM_PRESSURE
    }
    val points = pressures.map { p ->
        val tempC = dryAdiabatTemperatureC(thetaK, p)
        Offset(
            x = temperatureToX(tempC, tempMin, tempMax, plotLeft, plotRight),
            y = pressureToY(p, plotTop, plotBottom, topPressure),
        )
    }.filter { it.x in plotLeft..plotRight && it.y in plotTop..plotBottom }

    for (i in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[i],
            end = points[i + 1],
            strokeWidth = strokeWidth,
        )
    }
}

private fun DrawScope.drawMoistAdiabat(
    thetaWK: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    color: Color,
    strokeWidth: Float,
    dashOn: Float,
    dashOff: Float,
) {
    val pressures = buildList {
        var p = STUVE_BOTTOM_PRESSURE
        while (p >= topPressure) {
            add(p)
            p -= 25f
        }
    }
    val points = pressures.map { p ->
        val tempC = moistAdiabatTemperatureC(thetaWK, p)
        Offset(
            x = temperatureToX(tempC, tempMin, tempMax, plotLeft, plotRight),
            y = pressureToY(p, plotTop, plotBottom, topPressure),
        )
    }.filter { it.x in plotLeft..plotRight && it.y in plotTop..plotBottom }

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff))
    for (i in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[i],
            end = points[i + 1],
            strokeWidth = strokeWidth,
            pathEffect = dashEffect,
        )
    }
}

private fun DrawScope.drawMixingRatioLine(
    wGKg: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    color: Color,
    strokeWidth: Float,
    dotOn: Float,
    dotOff: Float,
) {
    val pressures = buildList {
        var p = STUVE_BOTTOM_PRESSURE
        while (p >= topPressure) {
            add(p)
            p -= 50f
        }
    }
    val points = pressures.map { p ->
        val tempC = mixingRatioTemperatureC(wGKg, p)
        Offset(
            x = temperatureToX(tempC, tempMin, tempMax, plotLeft, plotRight),
            y = pressureToY(p, plotTop, plotBottom, topPressure),
        )
    }.filter { it.x in plotLeft..plotRight && it.y in plotTop..plotBottom }

    val dotEffect = PathEffect.dashPathEffect(floatArrayOf(dotOn, dotOff))
    for (i in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[i],
            end = points[i + 1],
            strokeWidth = strokeWidth,
            pathEffect = dotEffect,
        )
    }
}

private fun DrawScope.drawProfile(
    points: List<StuveProfilePoint>,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    color: Color,
    strokeWidth: Float,
    dashOn: Float? = null,
    dashOff: Float? = null,
    drawDataDots: Boolean = false,
    dataDotRadius: Float = 0f,
) {
    val offsets = points.map { pt ->
        Offset(
            x = temperatureToX(pt.temperatureC, tempMin, tempMax, plotLeft, plotRight),
            y = pressureToY(pt.pressureHpa, plotTop, plotBottom, topPressure),
        )
    }

    val pathEffect = if (dashOn != null && dashOff != null) {
        PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff))
    } else {
        null
    }

    for (i in 0 until offsets.size - 1) {
        val start = offsets[i]
        val end = offsets[i + 1]
        if (start.y in plotTop..plotBottom || end.y in plotTop..plotBottom) {
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                pathEffect = pathEffect,
            )
        }
    }

    // Draw dots at real backend data points
    if (drawDataDots && dataDotRadius > 0f) {
        points.forEachIndexed { i, pt ->
            if (pt.isRealData) {
                val offset = offsets[i]
                if (offset.x in plotLeft..plotRight && offset.y in plotTop..plotBottom) {
                    drawCircle(
                        color = color,
                        radius = dataDotRadius,
                        center = offset,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawWindBarb(
    centerX: Float,
    centerY: Float,
    speedKmh: Float,
    directionDeg: Float,
    barbSize: Float,
    color: Color,
) {
    val angleRad = (directionDeg - 90f) * PI.toFloat() / 180f
    val halfSize = barbSize / 2f

    val tipX = centerX + cos(angleRad) * halfSize
    val tipY = centerY + sin(angleRad) * halfSize
    val tailX = centerX - cos(angleRad) * halfSize
    val tailY = centerY - sin(angleRad) * halfSize

    drawLine(
        color = color,
        start = Offset(tailX, tailY),
        end = Offset(tipX, tipY),
        strokeWidth = 1.5f,
    )

    val arrowLen = halfSize * 0.4f
    val arrowAngle = PI.toFloat() / 6f
    drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(
            tipX - cos(angleRad - arrowAngle) * arrowLen,
            tipY - sin(angleRad - arrowAngle) * arrowLen,
        ),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(
            tipX - cos(angleRad + arrowAngle) * arrowLen,
            tipY - sin(angleRad + arrowAngle) * arrowLen,
        ),
        strokeWidth = 1.5f,
    )
}

// --- Previews ---

@Preview(name = "Stuve Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun StuveForecastViewPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.STUVE),
        )
    }
}

@Preview(name = "Stuve Error", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun StuveForecastViewErrorPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.STUVE,
                errorMessage = "Unable to refresh forecast layers right now.",
            ),
        )
    }
}

@Preview(name = "Stuve Afternoon", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun StuveForecastViewAfternoonPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.STUVE),
        )
    }
}
