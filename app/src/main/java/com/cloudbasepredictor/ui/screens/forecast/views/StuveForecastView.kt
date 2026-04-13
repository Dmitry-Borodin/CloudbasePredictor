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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────────────────────
// Public Composable
// ──────────────────────────────────────────────────────────────────────────────

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
            SkewTDiagramCanvas(
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

// ──────────────────────────────────────────────────────────────────────────────
// Canvas
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkewTDiagramCanvas(
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
        modifier = modifier
            .clipToBounds()
            .pointerInput(visibleTopAltitudeKm) {
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
        // Chart bottom matches the surface pressure (+ 20 hPa margin)
        val chartBottomPressure = (chart.surfacePressureHpa + 20f)
            .coerceAtMost(SKEWT_BOTTOM_PRESSURE)

        val topPressure = altitudeKmToApproxPressureHpa(visibleTopAltitudeKm)
            .coerceIn(SKEWT_MIN_TOP_PRESSURE, chartBottomPressure - 50f)

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

        // Skew factor: pixels per log-pressure unit, scaled so isotherms tilt ~45°
        val skewFactor = plotWidth * SKEWT_SKEW_RATIO

        // Helper closures that capture the current plot geometry
        fun pToY(p: Float) = pressureToY(p, plotTop, plotBottom, topPressure, chartBottomPressure)
        fun tpToX(t: Float, p: Float) = skewTToX(
            t, p, TEMP_MIN, TEMP_MAX, plotLeft, plotRight, topPressure, skewFactor,
        )

        // ── Background ──────────────────────────────────────────────────
        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
        )

        // All reference lines are clipped to the plot area
        clipRect(plotLeft, plotTop, plotRight, plotBottom) {

            // ── Isotherms (skewed 45° lines) ────────────────────────────
            var t = TEMP_MIN
            while (t <= TEMP_MAX) {
                val alpha = if (t.toInt() % 20 == 0) 0.35f else 0.15f
                val pBottom = chartBottomPressure
                val pTop = topPressure
                drawLine(
                    color = outlineColor.copy(alpha = alpha),
                    start = Offset(tpToX(t, pBottom), pToY(pBottom)),
                    end = Offset(tpToX(t, pTop), pToY(pTop)),
                    strokeWidth = 1.dp.toPx(),
                )
                t += TEMP_STEP
            }

            // ── Isobars (horizontal) ────────────────────────────────────
            val pressureLabels = ISOBAR_LABELS.filter { it >= topPressure }
            pressureLabels.forEach { p ->
                val y = pToY(p)
                val alpha = if (p.toInt() % 200 == 0) 0.4f else 0.2f
                drawLine(
                    color = outlineColor.copy(alpha = alpha),
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // ── Dry adiabats (green curved lines) ───────────────────────
            STUVE_DRY_ADIABAT_THETAS_K.forEach { theta ->
                drawAdiabat(
                    pressures = STUVE_PRESSURE_LEVELS.filter { it in topPressure..chartBottomPressure },
                    computeTemp = { p -> dryAdiabatTemperatureC(theta, p) },
                    mapXY = { temp, p -> Offset(tpToX(temp, p), pToY(p)) },
                    plotLeft = plotLeft, plotRight = plotRight,
                    plotTop = plotTop, plotBottom = plotBottom,
                    color = Color(0xFF44AA44).copy(alpha = 0.35f),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // ── Moist adiabats (dashed teal curves) ─────────────────────
            val moistPressures = buildList {
                var p = chartBottomPressure; while (p >= topPressure) { add(p); p -= 25f }
            }
            STUVE_MOIST_ADIABAT_THETAS_K.forEach { thetaW ->
                drawAdiabat(
                    pressures = moistPressures,
                    computeTemp = { p -> moistAdiabatTemperatureC(thetaW, p) },
                    mapXY = { temp, p -> Offset(tpToX(temp, p), pToY(p)) },
                    plotLeft = plotLeft, plotRight = plotRight,
                    plotTop = plotTop, plotBottom = plotBottom,
                    color = Color(0xFF00AACC).copy(alpha = 0.3f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                    ),
                )
            }

            // ── Mixing ratio lines (dotted blue) ────────────────────────
            val mixPressures = buildList {
                var p = chartBottomPressure; while (p >= topPressure) { add(p); p -= 50f }
            }
            STUVE_MIXING_RATIO_VALUES_GKG.forEach { w ->
                drawAdiabat(
                    pressures = mixPressures,
                    computeTemp = { p -> mixingRatioTemperatureC(w, p) },
                    mapXY = { temp, p -> Offset(tpToX(temp, p), pToY(p)) },
                    plotLeft = plotLeft, plotRight = plotRight,
                    plotTop = plotTop, plotBottom = plotBottom,
                    color = Color(0xFF5588CC).copy(alpha = 0.3f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(3.dp.toPx(), 4.dp.toPx()),
                    ),
                )
            }

            // ── Temperature sounding (red) ──────────────────────────────
            drawSkewTProfile(
                points = chart.temperatureProfile,
                mapXY = { temp, p -> Offset(tpToX(temp, p), pToY(p)) },
                plotLeft = plotLeft, plotRight = plotRight,
                plotTop = plotTop, plotBottom = plotBottom,
                color = Color(0xFFDD2222),
                strokeWidth = 2.5f.dp.toPx(),
                drawDataDots = true,
                dataDotRadius = 2.5f.dp.toPx() * 1.5f, // 3× line half-width
            )

            // ── Dewpoint sounding (blue) ────────────────────────────────
            drawSkewTProfile(
                points = chart.dewpointProfile,
                mapXY = { temp, p -> Offset(tpToX(temp, p), pToY(p)) },
                plotLeft = plotLeft, plotRight = plotRight,
                plotTop = plotTop, plotBottom = plotBottom,
                color = Color(0xFF2255CC),
                strokeWidth = 2f.dp.toPx(),
                drawDataDots = true,
                dataDotRadius = 2f.dp.toPx() * 1.5f,
            )

            // ── Parcel ascent path (dashed) ──────────────────────────────
            drawSkewTProfile(
                points = chart.parcelAscentPath,
                mapXY = { temp, p -> Offset(tpToX(temp, p), pToY(p)) },
                plotLeft = plotLeft, plotRight = plotRight,
                plotTop = plotTop, plotBottom = plotBottom,
                color = onSurfaceColor.copy(alpha = 0.55f),
                strokeWidth = 2f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                ),
            )

            // ── LCL marker ──────────────────────────────────────────────
            chart.lclPressureHpa?.let { lcl ->
                val lclY = pToY(lcl)
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.35f),
                    start = Offset(plotLeft, lclY),
                    end = Offset(plotRight, lclY),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 6.dp.toPx()),
                    ),
                )
            }
        }

        // ── Plot outline ─────────────────────────────────────────────────
        drawRect(
            color = outlineColor.copy(alpha = 0.5f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            style = Stroke(width = 1.dp.toPx()),
        )

        // ── Wind barbs (right edge) ─────────────────────────────────────
        chart.windBarbs.forEach { barb ->
            val y = pToY(barb.pressureHpa)
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

        // ── Axis labels ──────────────────────────────────────────────────
        drawIntoCanvas { canvas ->
            // Pressure labels (left)
            val pressureLabels = ISOBAR_LABELS.filter { it >= topPressure }
            pressureLabels.forEach { p ->
                val y = pToY(p)
                if (y in plotTop..plotBottom) {
                    canvas.nativeCanvas.drawText(
                        "${p.toInt()}",
                        leftAxisWidth - 4.dp.toPx(),
                        y + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }

            // Temperature labels (bottom) — placed at the bottom isobar intersection
            var tempLabel = TEMP_MIN
            while (tempLabel <= TEMP_MAX) {
                if (tempLabel.toInt() % 10 == 0) {
                    val x = tpToX(tempLabel, chartBottomPressure)
                    if (x in plotLeft..plotRight) {
                        canvas.nativeCanvas.drawText(
                            "${tempLabel.toInt()}°",
                            x,
                            plotBottom + tempLabelPaint.textSize + 6.dp.toPx(),
                            tempLabelPaint,
                        )
                    }
                }
                tempLabel += TEMP_STEP
            }

            // Mixing ratio labels (top of plot)
            STUVE_MIXING_RATIO_VALUES_GKG.forEach { w ->
                val tempAtTop = mixingRatioTemperatureC(w, topPressure)
                val x = tpToX(tempAtTop, topPressure)
                if (x in plotLeft..plotRight) {
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
                val lclY = pToY(lcl)
                if (lclY in plotTop..plotBottom) {
                    val lclLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = axisLabelColor.toArgb()
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
                val y = pToY(barb.pressureHpa)
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

            // Altitude labels (right side, metres)
            pressureLabels.forEach { p ->
                val y = pToY(p)
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

// ──────────────────────────────────────────────────────────────────────────────
// Coordinate mapping — Skew-T log-P
// ──────────────────────────────────────────────────────────────────────────────

private const val SKEWT_MIN_TOP_PRESSURE = 200f
private const val SKEWT_BOTTOM_PRESSURE = 1050f
private const val SKEWT_KAPPA = 0.286f
private const val TEMP_MIN = -30f
private const val TEMP_MAX = 40f
private const val TEMP_STEP = 10f

/**
 * Controls how much isotherms are skewed. 1.0 means a full plot-width of skew
 * between bottom and top of the diagram, giving roughly 45° lines when the
 * temperature range equals the skewed shift range.
 */
private const val SKEWT_SKEW_RATIO = 0.85f

private val ISOBAR_LABELS = listOf(
    1000f, 950f, 900f, 850f, 800f, 750f, 700f, 650f,
    600f, 550f, 500f, 450f, 400f, 350f, 300f, 250f, 200f,
)

/** Y-axis: log-P scale, bottom = [bottomPressure], top = [topPressure]. */
private fun pressureToY(
    pressureHpa: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    bottomPressure: Float = SKEWT_BOTTOM_PRESSURE,
): Float {
    val logP = ln(pressureHpa)
    val logBottom = ln(bottomPressure)
    val logTop = ln(topPressure)
    // logBottom → plotBottom, logTop → plotTop
    val frac = (logP - logTop) / (logBottom - logTop)
    return plotTop + frac * (plotBottom - plotTop)
}

/**
 * X-axis: Skew-T mapping.
 *
 * At the bottom isobar the temperature axis is placed normally (tempMin→plotLeft,
 * tempMax→plotRight). As pressure decreases (higher altitude), isotherms shift
 * to the right by [skewFactor] × normalised log-pressure distance, producing the
 * characteristic 45° tilt.
 *
 * The skew is always computed against the FULL pressure range (200–1050 hPa) so
 * the isotherm angle stays constant regardless of the current zoom level.
 */
private fun skewTToX(
    temperatureC: Float,
    pressureHpa: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    @Suppress("UNUSED_PARAMETER") topPressure: Float,
    skewFactor: Float,
): Float {
    val plotWidth = plotRight - plotLeft
    val tNorm = (temperatureC - tempMin) / (tempMax - tempMin)
    // Skew offset: 0 at bottom (1050 hPa), increases toward top
    // Uses full range so skew angle is zoom-independent
    val logP = ln(pressureHpa)
    val logBottom = ln(SKEWT_BOTTOM_PRESSURE)
    val logTop = ln(SKEWT_MIN_TOP_PRESSURE)
    val heightFrac = ((logBottom - logP) / (logBottom - logTop)).coerceIn(0f, 1f)
    val skewOffset = heightFrac * skewFactor
    return plotLeft + tNorm * plotWidth + skewOffset
}

/** Convert altitude in km to an approximate pressure in hPa. */
private fun altitudeKmToApproxPressureHpa(altitudeKm: Float): Float {
    val hMeters = (altitudeKm * 1000f).coerceAtLeast(0f)
    return (1013.25f * (1f - 0.0000225577f * hMeters).pow(5.25588f))
}

// ──────────────────────────────────────────────────────────────────────────────
// Drawing helpers
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Generic curve drawer for dry adiabats, moist adiabats, and mixing ratio lines.
 */
private fun DrawScope.drawAdiabat(
    pressures: List<Float>,
    computeTemp: (Float) -> Float,
    mapXY: (Float, Float) -> Offset,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    color: Color,
    strokeWidth: Float,
    pathEffect: PathEffect? = null,
) {
    val points = pressures.map { p ->
        val temp = computeTemp(p)
        mapXY(temp, p)
    }.filter { it.x in (plotLeft - 20f)..(plotRight + 20f) && it.y in plotTop..plotBottom }

    for (i in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[i],
            end = points[i + 1],
            strokeWidth = strokeWidth,
            pathEffect = pathEffect,
        )
    }
}

/**
 * Draw a temperature or dewpoint sounding profile with optional data dots.
 */
private fun DrawScope.drawSkewTProfile(
    points: List<StuveProfilePoint>,
    mapXY: (Float, Float) -> Offset,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    color: Color,
    strokeWidth: Float,
    pathEffect: PathEffect? = null,
    drawDataDots: Boolean = false,
    dataDotRadius: Float = 0f,
) {
    val offsets = points.map { pt -> mapXY(pt.temperatureC, pt.pressureHpa) }

    val effect = pathEffect

    for (i in 0 until offsets.size - 1) {
        val start = offsets[i]
        val end = offsets[i + 1]
        if (start.y in plotTop..plotBottom || end.y in plotTop..plotBottom) {
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                pathEffect = effect,
            )
        }
    }

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

// ──────────────────────────────────────────────────────────────────────────────
// Previews
// ──────────────────────────────────────────────────────────────────────────────

@Preview(name = "Skew-T Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun SkewTForecastViewPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.STUVE),
        )
    }
}

@Preview(
    name = "Skew-T Dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 720,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SkewTForecastViewDarkPreview() {
    CloudbasePredictorTheme(darkTheme = true) {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.STUVE),
        )
    }
}

@Preview(name = "Skew-T Error", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun SkewTForecastViewErrorPreview() {
    CloudbasePredictorTheme {
        StuveForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.STUVE,
                errorMessage = "Unable to refresh forecast layers right now.",
            ),
        )
    }
}
