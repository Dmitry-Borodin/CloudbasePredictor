package com.cloudbasepredictor.ui.screens.forecast.views

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as ComposeGraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.cloudbasepredictor.domain.forecast.ProfileLevel
import com.cloudbasepredictor.ui.screens.forecast.StuveActiveThetaKKey
import com.cloudbasepredictor.ui.screens.forecast.buildParcelAscentPath
import com.cloudbasepredictor.ui.screens.forecast.buildRenderableParcelPressures
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.DEFAULT_TOP_ALTITUDE_KM
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_SELECTED_HOUR
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_CHART_CANVAS
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_TIME_SLIDER
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.STUVE_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.screens.forecast.MAX_TOP_ALTITUDE_KM
import com.cloudbasepredictor.ui.screens.forecast.STUVE_DRY_ADIABAT_THETAS_K
import com.cloudbasepredictor.ui.screens.forecast.STUVE_MIXING_RATIO_VALUES_GKG
import com.cloudbasepredictor.ui.screens.forecast.STUVE_MOIST_ADIABAT_THETAS_K
import com.cloudbasepredictor.ui.screens.forecast.StuveForecastChartUiModel
import com.cloudbasepredictor.ui.screens.forecast.StuveProfilePoint
import com.cloudbasepredictor.ui.screens.forecast.interpolateProfileHeightMeters
import com.cloudbasepredictor.ui.screens.forecast.interpolateProfileTemperature
import com.cloudbasepredictor.ui.screens.forecast.pressureToApproxHeightMeters
import com.cloudbasepredictor.ui.screens.forecast.zoomedTopAltitudeKm
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import com.cloudbasepredictor.domain.forecast.dryAdiabatTempC
import com.cloudbasepredictor.domain.forecast.mixingRatioTemperatureC
import com.cloudbasepredictor.domain.forecast.moistAdiabatTempFromPointC
import com.cloudbasepredictor.domain.forecast.moistAdiabatTempC
import com.cloudbasepredictor.domain.forecast.potentialTemperatureK
import com.cloudbasepredictor.domain.forecast.satMixingRatioGKg
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun StuveForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
    onStuveHourChanged: (Int) -> Unit = {},
) {
    val stuveChart = uiState.stuveChart
    val chartSessionKey = Triple(
        uiState.selectedPlace?.id,
        uiState.selectedDayIndex,
        uiState.resolvedModel ?: uiState.selectedModel,
    )
    val autoFitTopAltitudeKm = remember(stuveChart.temperatureProfile, stuveChart.windBarbs) {
        recommendedStuveTopAltitudeKm(stuveChart)
    }
    val initialRequestedTopAltitudeKm = remember(chartSessionKey) {
        uiState.chartViewport.visibleTopAltitudeKm
    }
    var effectiveTopAltitudeKm by remember(chartSessionKey) {
        mutableFloatStateOf(
            maxOf(
                uiState.chartViewport.visibleTopAltitudeKm,
                autoFitTopAltitudeKm,
            ),
        )
    }

    LaunchedEffect(chartSessionKey, autoFitTopAltitudeKm) {
        val fittedTopAltitudeKm = maxOf(
            uiState.chartViewport.visibleTopAltitudeKm,
            autoFitTopAltitudeKm,
        )
        effectiveTopAltitudeKm = fittedTopAltitudeKm
        if (abs(uiState.chartViewport.visibleTopAltitudeKm - fittedTopAltitudeKm) > 0.01f) {
            onVisibleTopAltitudeChange(fittedTopAltitudeKm)
        }
    }

    LaunchedEffect(uiState.chartViewport.visibleTopAltitudeKm, chartSessionKey, autoFitTopAltitudeKm) {
        val requestedTopAltitudeKm = uiState.chartViewport.visibleTopAltitudeKm
        val isInitialUnderZoomedRequest =
            abs(requestedTopAltitudeKm - initialRequestedTopAltitudeKm) <= 0.01f &&
                requestedTopAltitudeKm < autoFitTopAltitudeKm - 0.01f
        if (!isInitialUnderZoomedRequest && abs(requestedTopAltitudeKm - effectiveTopAltitudeKm) > 0.01f) {
            effectiveTopAltitudeKm = requestedTopAltitudeKm
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag(STUVE_VIEW),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            SkewTDiagramCanvas(
                chart = stuveChart,
                visibleTopAltitudeKm = effectiveTopAltitudeKm,
                onVisibleTopAltitudeChange = { topAltitudeKm ->
                    effectiveTopAltitudeKm = topAltitudeKm
                    onVisibleTopAltitudeChange(topAltitudeKm)
                },
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag(STUVE_TIME_SLIDER),
        ) {
            Text(
                text = "06",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onHourChanged(it.toInt())
                },
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
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag(STUVE_SELECTED_HOUR),
        )
    }
}

private data class SkewTCursorState(
    val y: Float,
    val x: Float,
    val isPinned: Boolean = false,
)

private data class CursorReadout(
    val pressureHpa: Float,
    val altitudeMeters: Int,
    val temperatureC: Float?,
    val dewpointC: Float?,
    val parcelTemperatureC: Float?,
    val guideTemperatureC: Float?,
    val guideDryThetaK: Float?,
    val guideMixingRatioGKg: Float?,
    val parcelSurfaceTemperatureC: Float?,
    val criticalSurfaceDewpointC: Float?,
    val windSpeedKmh: Float?,
    val windDirectionDeg: Float?,
)

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
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridBackgroundColor = lerp(
        start = MaterialTheme.colorScheme.surface,
        stop = MaterialTheme.colorScheme.onSurface,
        fraction = 0.02f,
    )
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val latestVisibleTopAltitudeKm = rememberUpdatedState(visibleTopAltitudeKm)

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
            color = Color(0xFF5C88B4).toArgb()
            textSize = with(density) { 8.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }
    var cursorState by remember(
        chart.selectedHour,
        chart.surfacePressureHpa,
        chart.temperatureProfile,
        chart.dewpointProfile,
    ) {
        mutableStateOf<SkewTCursorState?>(null)
    }

    // Heating-handle state: tracks how many °C the user has shifted the parcel start
    // temperature away from the forecast default via the bottom drag handle.
    // Reset whenever the selected hour or surface pressure changes (new sounding).
    var heatingDeltaC by remember(chart.selectedHour, chart.surfacePressureHpa) {
        mutableFloatStateOf(0f)
    }

    // Canvas pixel dimensions — updated by onSizeChanged, used to compute layout outside the
    // Canvas draw block for gesture detection and semantics.
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    // Layout constants that mirror the computation inside the Canvas draw block.
    val leftAxisWidthPx = with(density) { 40.dp.toPx() }
    val rightAltitudeWidthPx = with(density) { 42.dp.toPx() }
    val rightWindWidthPx = with(density) { 58.dp.toPx() }
    val bottomAxisHeightPx = with(density) { 34.dp.toPx() }
    val topPaddingPx = with(density) { 16.dp.toPx() }

    // Derived layout (may be zero on first composition before onSizeChanged fires).
    val outerPlotLeft = leftAxisWidthPx
    val outerPlotRight = (canvasWidth - rightAltitudeWidthPx - rightWindWidthPx).coerceAtLeast(0f)
    val outerPlotWidth = (outerPlotRight - outerPlotLeft).coerceAtLeast(0f)
    val outerPlotBottom = (canvasHeight - bottomAxisHeightPx).coerceAtLeast(0f)

    val outerChartBottomPressure = (chart.surfacePressureHpa + 20f).coerceAtMost(SKEWT_BOTTOM_PRESSURE)
    val outerTopPressure = altitudeKmToApproxPressureHpa(visibleTopAltitudeKm)
        .coerceIn(SKEWT_MIN_TOP_PRESSURE, outerChartBottomPressure - 50f)
    val outerTempAxisRange = remember(chart, outerTopPressure, outerChartBottomPressure) {
        buildVisibleTemperatureAxisRange(chart, outerTopPressure, outerChartBottomPressure)
    }
    val outerSkewFactor = outerPlotWidth * SKEWT_SKEW_RATIO

    // Default parcel start temperature (first point of the pre-built ascent path).
    val defaultParcelStartTempC = remember(chart.parcelAscentPath, chart.temperatureProfile) {
        chart.parcelAscentPath.firstOrNull()?.temperatureC
            ?: chart.temperatureProfile.firstOrNull()?.temperatureC
            ?: 15f
    }

    // Memoised profile levels and parcel pressures used for live parcel recomputation.
    val profileLevels = remember(chart.temperatureProfile, chart.dewpointProfile) {
        buildMinimalProfileLevels(chart)
    }
    val parcelPressures = remember(chart.surfacePressureHpa, chart.pressureLevels) {
        buildRenderableParcelPressures(chart.surfacePressureHpa, chart.pressureLevels)
    }

    // Anchor temperature for the active cursor, computed from its X position.
    // Null when no cursor is active or the canvas has not been sized yet.
    val anchorTemperatureC: Float? = remember(cursorState, outerPlotWidth, outerTempAxisRange) {
        val cursor = cursorState ?: return@remember null
        if (outerPlotWidth <= 0f) return@remember null
        val pressure = yToPressure(
            cursor.y, topPaddingPx, outerPlotBottom, outerTopPressure, outerChartBottomPressure,
        ).coerceIn(outerTopPressure, outerChartBottomPressure)
        xToSkewTTemperature(
            x = cursor.x,
            pressureHpa = pressure,
            tempMin = outerTempAxisRange.minC,
            tempMax = outerTempAxisRange.maxC,
            plotLeft = outerPlotLeft,
            plotRight = outerPlotRight,
            skewFactor = outerSkewFactor,
            topPressure = outerTopPressure,
            bottomPressure = outerChartBottomPressure,
        )
    }

    // Active parcel guide theta K exposed through semantics so that tests can verify
    // that different tap positions produce different parcel guides.
    val activeGuideThetaK: Float? = remember(anchorTemperatureC, cursorState, heatingDeltaC) {
        when {
            anchorTemperatureC != null && cursorState != null -> {
                val pressure = yToPressure(
                    cursorState!!.y, topPaddingPx, outerPlotBottom, outerTopPressure, outerChartBottomPressure,
                ).coerceIn(outerTopPressure, outerChartBottomPressure)
                potentialTemperatureK(anchorTemperatureC, pressure)
            }
            heatingDeltaC != 0f ->
                potentialTemperatureK(defaultParcelStartTempC + heatingDeltaC, chart.surfacePressureHpa)
            else -> null
        }
    }

    // Updated lambdas for gesture callbacks — always capture the latest state.
    val latestIsInHeatingZone = rememberUpdatedState { x: Float, y: Float ->
        if (outerPlotWidth <= 0f) return@rememberUpdatedState false
        val handleX = skewTToX(
            temperatureC = defaultParcelStartTempC + heatingDeltaC,
            pressureHpa = outerChartBottomPressure,
            tempMin = outerTempAxisRange.minC,
            tempMax = outerTempAxisRange.maxC,
            plotLeft = outerPlotLeft,
            plotRight = outerPlotRight,
            skewFactor = outerSkewFactor,
            topPressure = outerTopPressure,
            bottomPressure = outerChartBottomPressure,
        ).coerceIn(outerPlotLeft, outerPlotRight)
        val handleY = outerPlotBottom
        val touchRadiusPx = with(density) { 28.dp.toPx() }
        val dx = x - handleX
        val dy = y - handleY
        kotlin.math.sqrt(dx * dx + dy * dy) < touchRadiusPx
    }
    val latestOnHeatingDragDelta = rememberUpdatedState { deltaX: Float ->
        if (outerPlotWidth > 0f) {
            val tempSpan = outerTempAxisRange.maxC - outerTempAxisRange.minC
            heatingDeltaC = (heatingDeltaC + deltaX / outerPlotWidth * tempSpan)
                .coerceIn(-20f, 20f)
        }
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .testTag(STUVE_CHART_CANVAS)
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
                canvasHeight = size.height.toFloat()
            }
            .semantics {
                stateDescription = when {
                    cursorState?.isPinned == true -> "pinned"
                    cursorState != null -> "tracking"
                    else -> "idle"
                }
                activeGuideThetaK?.let { set(StuveActiveThetaKKey, it) }
            }
            .pointerInput(chart.selectedHour, chart.surfacePressureHpa) {
                detectSkewTGestures(
                    currentTopAltitudeKm = { latestVisibleTopAltitudeKm.value },
                    onVisibleTopAltitudeChange = onVisibleTopAltitudeChange,
                    onCursorStateChanged = { cursorState = it },
                    isInHeatingZone = { x, y -> latestIsInHeatingZone.value(x, y) },
                    onHeatingHandleDragDelta = { deltaX -> latestOnHeatingDragDelta.value(deltaX) },
                )
            },
    ) {
        drawRect(
            color = surfaceColor,
            size = size,
        )

        val chartBottomPressure = (chart.surfacePressureHpa + 20f)
            .coerceAtMost(SKEWT_BOTTOM_PRESSURE)
        val topPressure = altitudeKmToApproxPressureHpa(visibleTopAltitudeKm)
            .coerceIn(SKEWT_MIN_TOP_PRESSURE, chartBottomPressure - 50f)

        val tempAxisRange = buildVisibleTemperatureAxisRange(
            chart = chart,
            topPressure = topPressure,
            bottomPressure = chartBottomPressure,
        )

        val leftAxisWidth = with(density) { 40.dp.toPx() }
        val rightAltitudeWidth = with(density) { 42.dp.toPx() }
        val rightWindWidth = with(density) { 58.dp.toPx() }
        val bottomAxisHeight = with(density) { 34.dp.toPx() }
        val topPadding = with(density) { 16.dp.toPx() }

        val plotLeft = leftAxisWidth
        val plotTop = topPadding
        val plotRight = size.width - rightAltitudeWidth - rightWindWidth
        val plotBottom = size.height - bottomAxisHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

        val skewFactor = plotWidth * SKEWT_SKEW_RATIO

        fun pressureToY(pressureHpa: Float) = pressureToY(
            pressureHpa = pressureHpa,
            plotTop = plotTop,
            plotBottom = plotBottom,
            topPressure = topPressure,
            bottomPressure = chartBottomPressure,
        )
        fun temperatureToX(temperatureC: Float, pressureHpa: Float) = skewTToX(
            temperatureC = temperatureC,
            pressureHpa = pressureHpa,
            tempMin = tempAxisRange.minC,
            tempMax = tempAxisRange.maxC,
            plotLeft = plotLeft,
            plotRight = plotRight,
            skewFactor = skewFactor,
            topPressure = topPressure,
            bottomPressure = chartBottomPressure,
        )
        fun yToPressure(y: Float) = yToPressure(
            y = y,
            plotTop = plotTop,
            plotBottom = plotBottom,
            topPressure = topPressure,
            bottomPressure = chartBottomPressure,
        )

        val pressureLabels = selectPressureLabels(
            topPressure = topPressure,
            plotHeight = plotHeight,
        )

        // Resolve anchor temperature from cursor X position once, at Canvas block scope,
        // so it can be used both inside clipRect (overlay drawing) and inside drawIntoCanvas
        // (inline label drawing) without re-computing it twice.
        val drawAnchorTemperatureC: Float? = cursorState?.let { cursor ->
            val pressure = yToPressure(cursor.y).coerceIn(topPressure, chartBottomPressure)
            xToSkewTTemperature(
                x = cursor.x,
                pressureHpa = pressure,
                tempMin = tempAxisRange.minC,
                tempMax = tempAxisRange.maxC,
                plotLeft = plotLeft,
                plotRight = plotRight,
                skewFactor = skewFactor,
                topPressure = topPressure,
                bottomPressure = chartBottomPressure,
            )
        }

        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
        )

        drawMoistureCueStrip(
            chart = chart,
            plotTop = plotTop,
            plotBottom = plotBottom,
            plotRight = plotRight,
            pressureToY = ::pressureToY,
            density = density,
        )

        clipRect(plotLeft, plotTop, plotRight, plotBottom) {
            var isothermTemp = floorToStep(tempAxisRange.minC, TEMP_STEP)
            while (isothermTemp <= tempAxisRange.maxC + 0.01f) {
                val alpha = if (isothermTemp.toInt() % 20 == 0) 0.35f else 0.15f
                drawLine(
                    color = outlineColor.copy(alpha = alpha),
                    start = Offset(temperatureToX(isothermTemp, chartBottomPressure), pressureToY(chartBottomPressure)),
                    end = Offset(temperatureToX(isothermTemp, topPressure), pressureToY(topPressure)),
                    strokeWidth = 1.dp.toPx(),
                )
                isothermTemp += TEMP_STEP
            }

            pressureLabels.forEach { pressure ->
                val alpha = if (pressure.toInt() % 200 == 0) 0.4f else 0.2f
                drawLine(
                    color = outlineColor.copy(alpha = alpha),
                    start = Offset(plotLeft, pressureToY(pressure)),
                    end = Offset(plotRight, pressureToY(pressure)),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            STUVE_DRY_ADIABAT_THETAS_K.forEach { theta ->
                drawAdiabat(
                    pressures = STUVE_DRY_REFERENCE_PRESSURES.filter { it in topPressure..chartBottomPressure },
                    computeTemp = { pressure -> dryAdiabatTempC(theta, pressure) },
                    mapXY = { temperature, pressure -> Offset(temperatureToX(temperature, pressure), pressureToY(pressure)) },
                    plotLeft = plotLeft,
                    plotRight = plotRight,
                    plotTop = plotTop,
                    plotBottom = plotBottom,
                    color = Color(0xFF4E9B64).copy(alpha = 0.32f),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val moistPressures = buildReferencePressures(chartBottomPressure, topPressure, stepHpa = 25f)
            STUVE_MOIST_ADIABAT_THETAS_K.forEach { theta ->
                drawAdiabat(
                    pressures = moistPressures,
                    computeTemp = { pressure -> moistAdiabatTempC(theta, pressure) },
                    mapXY = { temperature, pressure -> Offset(temperatureToX(temperature, pressure), pressureToY(pressure)) },
                    plotLeft = plotLeft,
                    plotRight = plotRight,
                    plotTop = plotTop,
                    plotBottom = plotBottom,
                    color = Color(0xFF2F8BAA).copy(alpha = 0.28f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            }

            val mixingRatioPressures = buildReferencePressures(chartBottomPressure, topPressure, stepHpa = 50f)
            STUVE_MIXING_RATIO_VALUES_GKG.forEach { mixingRatio ->
                drawAdiabat(
                    pressures = mixingRatioPressures,
                    computeTemp = { pressure -> mixingRatioTemperatureC(mixingRatio, pressure) },
                    mapXY = { temperature, pressure -> Offset(temperatureToX(temperature, pressure), pressureToY(pressure)) },
                    plotLeft = plotLeft,
                    plotRight = plotRight,
                    plotTop = plotTop,
                    plotBottom = plotBottom,
                    color = Color(0xFF6E93C0).copy(alpha = 0.24f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                )
            }

            drawSkewTProfile(
                points = chart.temperatureProfile,
                mapXY = { temperature, pressure -> Offset(temperatureToX(temperature, pressure), pressureToY(pressure)) },
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                color = Color(0xFFD83A3A),
                strokeWidth = 2.6f.dp.toPx(),
                drawDataDots = true,
                dataDotRadius = 2.6f.dp.toPx(),
            )

            drawSkewTProfile(
                points = chart.dewpointProfile,
                mapXY = { temperature, pressure -> Offset(temperatureToX(temperature, pressure), pressureToY(pressure)) },
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                color = Color(0xFF2E6FB5),
                strokeWidth = 2.1f.dp.toPx(),
                drawDataDots = true,
                dataDotRadius = 2.2f.dp.toPx(),
            )

            drawSkewTProfile(
                points = chart.parcelAscentPath,
                mapXY = { temperature, pressure -> Offset(temperatureToX(temperature, pressure), pressureToY(pressure)) },
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                plotBottom = plotBottom,
                color = onSurfaceColor.copy(alpha = 0.58f),
                strokeWidth = 2f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())),
            )

            // ── Interactive parcel overlay ──────────────────────────────────────
            val interactiveParcelPath: List<StuveProfilePoint>? = when {
                drawAnchorTemperatureC != null && cursorState != null -> {
                    val anchorPressure = yToPressure(cursorState!!.y)
                        .coerceIn(topPressure, chartBottomPressure)
                    buildInteractiveParcelFromPoint(
                        anchorTemperatureC = drawAnchorTemperatureC,
                        anchorPressureHpa = anchorPressure,
                        chart = chart,
                        parcelPressures = parcelPressures,
                    )
                }
                heatingDeltaC != 0f ->
                    buildInteractiveParcelFromSurface(
                        parcelStartTempC = defaultParcelStartTempC + heatingDeltaC,
                        chart = chart,
                        profileLevels = profileLevels,
                        parcelPressures = parcelPressures,
                    )
                else -> null
            }
            val activeParcelPath = interactiveParcelPath ?: chart.parcelAscentPath

            interactiveParcelPath?.let { path ->
                if (drawAnchorTemperatureC != null && cursorState != null) {
                    val anchorPressure = yToPressure(cursorState!!.y)
                        .coerceIn(topPressure, chartBottomPressure)
                    val anchorPoint = StuveProfilePoint(
                        pressureHpa = anchorPressure,
                        temperatureC = drawAnchorTemperatureC,
                    )
                    val drySegment = listOf(anchorPoint) + path
                        .filter { it.pressureHpa > anchorPressure + 0.01f }
                        .sortedBy { it.pressureHpa }
                    val moistSegment = listOf(anchorPoint) + path
                        .filter { it.pressureHpa < anchorPressure - 0.01f }
                        .sortedByDescending { it.pressureHpa }

                    if (drySegment.size > 1) {
                        drawSkewTProfile(
                            points = drySegment,
                            mapXY = { temperature, pressure ->
                                Offset(temperatureToX(temperature, pressure), pressureToY(pressure))
                            },
                            plotLeft = plotLeft,
                            plotRight = plotRight,
                            plotTop = plotTop,
                            plotBottom = plotBottom,
                            color = Color(0xFF59A36A).copy(alpha = 0.88f),
                            strokeWidth = 0.8f.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                            ),
                        )
                    }
                    if (moistSegment.size > 1) {
                        drawSkewTProfile(
                            points = moistSegment,
                            mapXY = { temperature, pressure ->
                                Offset(temperatureToX(temperature, pressure), pressureToY(pressure))
                            },
                            plotLeft = plotLeft,
                            plotRight = plotRight,
                            plotTop = plotTop,
                            plotBottom = plotBottom,
                            color = Color(0xFF59A36A).copy(alpha = 0.88f),
                            strokeWidth = 2.4f.dp.toPx(),
                        )
                    }
                } else {
                    drawSkewTProfile(
                        points = path,
                        mapXY = { temperature, pressure ->
                            Offset(temperatureToX(temperature, pressure), pressureToY(pressure))
                        },
                        plotLeft = plotLeft,
                        plotRight = plotRight,
                        plotTop = plotTop,
                        plotBottom = plotBottom,
                        color = Color(0xFF59A36A).copy(alpha = 0.88f),
                        strokeWidth = 2.4f.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10.dp.toPx(), 5.dp.toPx()),
                        ),
                    )
                }
            }

            chart.cclPressureHpa?.let { pressure ->
                val y = pressureToY(pressure)
                drawLine(
                    color = Color(0xFFB36A27).copy(alpha = 0.5f),
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx())),
                )
            }

            val activeCursor = cursorState
            if (activeCursor != null) {
                val readout = buildCursorReadout(
                    chart = chart,
                    pressureHpa = yToPressure(activeCursor.y),
                    anchorTemperatureC = drawAnchorTemperatureC,
                    parcelPath = activeParcelPath,
                )
                val cursorY = pressureToY(readout.pressureHpa)
                if (cursorY in plotTop..plotBottom) {
                    drawCursorOverlay(
                        readout = readout,
                        cursorY = cursorY,
                        topPressure = topPressure,
                        bottomPressure = chartBottomPressure,
                        plotLeft = plotLeft,
                        plotRight = plotRight,
                        onSurfaceColor = onSurfaceColor,
                        temperatureToX = ::temperatureToX,
                        pressureToY = ::pressureToY,
                        showThermoGuides = false,
                    )
                }
            }
        }

        drawRect(
            color = outlineColor.copy(alpha = 0.5f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            style = Stroke(width = 1.dp.toPx()),
        )

        // ── Heating handle: drawn at plotBottom, outside the clipped area ──────
        val activeParcelStartTempC = defaultParcelStartTempC + heatingDeltaC
        val handleX = skewTToX(
            temperatureC = activeParcelStartTempC,
            pressureHpa = chartBottomPressure,
            tempMin = tempAxisRange.minC,
            tempMax = tempAxisRange.maxC,
            plotLeft = plotLeft,
            plotRight = plotRight,
            skewFactor = skewFactor,
            topPressure = topPressure,
            bottomPressure = chartBottomPressure,
        ).coerceIn(plotLeft, plotRight)
        val handleColor = Color(0xFF59A36A)
        // Vertical stem from bottom of plot area down to the handle circle.
        drawLine(
            color = handleColor.copy(alpha = 0.75f),
            start = Offset(handleX, plotBottom),
            end = Offset(handleX, plotBottom + with(density) { 8.dp.toPx() }),
            strokeWidth = 2.dp.toPx(),
        )
        // Handle circle — tap / drag target.
        drawCircle(
            color = handleColor,
            radius = with(density) { 6.dp.toPx() },
            center = Offset(handleX, plotBottom + with(density) { 8.dp.toPx() }),
        )
        // Small tick on the plot bottom edge to show the handle position.
        drawLine(
            color = handleColor,
            start = Offset(handleX, plotBottom - with(density) { 4.dp.toPx() }),
            end = Offset(handleX, plotBottom),
            strokeWidth = 2.dp.toPx(),
        )

        chart.windBarbs.forEach { barb ->
            val y = pressureToY(barb.pressureHpa)
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

        drawIntoCanvas { canvas ->
            pressureLabels.forEach { pressure ->
                val y = pressureToY(pressure)
                if (y in plotTop..plotBottom) {
                    canvas.nativeCanvas.drawText(
                        "${pressure.toInt()}",
                        leftAxisWidth - 4.dp.toPx(),
                        y + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }

            val temperatureAxisBaseline = plotBottom + tempLabelPaint.textSize +
                with(density) { 6.dp.toPx() }
            val temperatureReadoutBaseline = plotBottom + tempLabelPaint.textSize +
                with(density) { 22.dp.toPx() }
            val temperatureReadoutLabelRight = (plotRight + rightAltitudeWidth + with(density) { 8.dp.toPx() })
                .coerceAtMost(size.width - with(density) { 4.dp.toPx() })
            buildTemperatureAxisLabels(tempAxisRange).forEach { tempLabel ->
                val x = temperatureToX(tempLabel, chartBottomPressure)
                if (x in plotLeft..plotRight) {
                    canvas.nativeCanvas.drawText(
                        "${tempLabel.toInt()}°",
                        x,
                        temperatureAxisBaseline,
                        tempLabelPaint,
                    )
                }
            }

            STUVE_MIXING_RATIO_VALUES_GKG.forEach { mixingRatio ->
                val x = temperatureToX(mixingRatioTemperatureC(mixingRatio, topPressure), topPressure)
                if (x in plotLeft..plotRight) {
                    canvas.nativeCanvas.drawText(
                        if (mixingRatio < 1f) String.format(Locale.US, "%.1f", mixingRatio)
                        else "${mixingRatio.toInt()}",
                        x,
                        plotTop - 2.dp.toPx(),
                        mixingRatioLabelPaint,
                    )
                }
            }

            chart.cclPressureHpa?.let { pressure ->
                drawMarkerLabel(
                    canvas = canvas,
                    text = "CCL",
                    y = pressureToY(pressure),
                    x = plotLeft + 4.dp.toPx(),
                    color = Color(0xFFB36A27),
                    density = density,
                    yOffsetPx = with(density) { 12.dp.toPx() },
                )
            }

            chart.windBarbs.forEach { barb ->
                val y = pressureToY(barb.pressureHpa)
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

            pressureLabels.forEach { pressure ->
                val y = pressureToY(pressure)
                if (y in plotTop..plotBottom) {
                    val heightMeters = interpolateProfileHeightMeters(chart.temperatureProfile, pressure)
                        ?: pressureToApproxHeightMeters(pressure).toFloat()
                    canvas.nativeCanvas.drawText(
                        formatAxisHeight(heightMeters),
                        plotRight + 4.dp.toPx(),
                        y + altitudeLabelPaint.textSize * 0.35f,
                        altitudeLabelPaint,
                    )
                }
            }

            cursorState?.let { activeCursor ->
                val activeParcelPath = when {
                    drawAnchorTemperatureC != null -> {
                        val anchorPressure = yToPressure(activeCursor.y)
                            .coerceIn(topPressure, chartBottomPressure)
                        buildInteractiveParcelFromPoint(
                            anchorTemperatureC = drawAnchorTemperatureC,
                            anchorPressureHpa = anchorPressure,
                            chart = chart,
                            parcelPressures = parcelPressures,
                        )
                    }
                    heatingDeltaC != 0f ->
                        buildInteractiveParcelFromSurface(
                            parcelStartTempC = defaultParcelStartTempC + heatingDeltaC,
                            chart = chart,
                            profileLevels = profileLevels,
                            parcelPressures = parcelPressures,
                        )
                    else -> chart.parcelAscentPath
                }
                val readout = buildCursorReadout(
                    chart = chart,
                    pressureHpa = yToPressure(activeCursor.y),
                    anchorTemperatureC = drawAnchorTemperatureC,
                    parcelPath = activeParcelPath,
                )
                val cursorY = pressureToY(readout.pressureHpa)
                if (cursorY in plotTop..plotBottom) {
                    drawCursorInlineLabels(
                        canvas = canvas,
                        readout = readout,
                        cursorY = cursorY,
                        plotLeft = plotLeft,
                        plotRight = plotRight,
                        plotTop = plotTop,
                        plotBottom = plotBottom,
                        bottomPressure = chartBottomPressure,
                        rightWindCenterX = plotRight + rightAltitudeWidth + rightWindWidth / 2f,
                        axisLabelPaint = axisLabelPaint,
                        altitudeLabelPaint = altitudeLabelPaint,
                        temperatureAxisBaseline = temperatureAxisBaseline,
                        temperatureReadoutBaseline = temperatureReadoutBaseline,
                        temperatureReadoutLabelLeft = plotLeft,
                        temperatureReadoutLabelRight = temperatureReadoutLabelRight,
                        temperatureAxisRange = tempAxisRange,
                        temperatureAxisLabelPaint = tempLabelPaint,
                        density = density,
                        temperatureToX = ::temperatureToX,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMoistureCueStrip(
    chart: StuveForecastChartUiModel,
    plotTop: Float,
    plotBottom: Float,
    plotRight: Float,
    pressureToY: (Float) -> Float,
    density: androidx.compose.ui.unit.Density,
) {
    if (chart.moistureBands.isEmpty()) return

    val stripWidth = with(density) { 9.dp.toPx() }
    val stripLeft = plotRight + 2.dp.toPx()

    chart.moistureBands.forEach { band ->
        val bandTopY = pressureToY(band.topPressureHpa).coerceIn(plotTop, plotBottom)
        val bandBottomY = pressureToY(band.bottomPressureHpa).coerceIn(plotTop, plotBottom)
        if (bandBottomY <= bandTopY) return@forEach

        val intensity = ((band.relativeHumidityFraction - 0.55f) / 0.45f).coerceIn(0f, 1f)
        if (intensity <= 0f) return@forEach

        drawRect(
            color = lerp(
                start = Color(0xFFD7EAF4),
                stop = Color(0xFF4E7C9A),
                fraction = intensity,
            ).copy(alpha = 0.16f + intensity * 0.42f),
            topLeft = Offset(stripLeft, bandTopY),
            size = Size(stripWidth, bandBottomY - bandTopY),
        )
    }
}

private fun DrawScope.drawCursorOverlay(
    readout: CursorReadout,
    cursorY: Float,
    topPressure: Float,
    bottomPressure: Float,
    plotLeft: Float,
    plotRight: Float,
    onSurfaceColor: Color,
    temperatureToX: (Float, Float) -> Float,
    pressureToY: (Float) -> Float,
    showThermoGuides: Boolean = true,
) {
    drawLine(
        color = onSurfaceColor.copy(alpha = 0.58f),
        start = Offset(plotLeft, cursorY),
        end = Offset(plotRight, cursorY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
    )

    readout.temperatureC?.let { temperature ->
        drawLine(
            color = Color(0xFFD83A3A).copy(alpha = 0.45f),
            start = Offset(temperatureToX(temperature, bottomPressure), pressureToY(bottomPressure)),
            end = Offset(temperatureToX(temperature, topPressure), pressureToY(topPressure)),
            strokeWidth = 1.2f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }

    if (showThermoGuides) {
        readout.guideDryThetaK?.let { thetaK ->
            drawAdiabat(
                pressures = buildReferencePressures(bottomPressure, readout.pressureHpa, stepHpa = 25f) +
                    listOf(readout.pressureHpa),
                computeTemp = { pressure -> dryAdiabatTempC(thetaK, pressure) },
                mapXY = { temperature, pressure ->
                    Offset(temperatureToX(temperature, pressure), pressureToY(pressure))
                },
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = pressureToY(topPressure),
                plotBottom = pressureToY(bottomPressure),
                color = Color(0xFF59A36A).copy(alpha = 0.72f),
                strokeWidth = 0.8f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
            )
        }
    }

    if (showThermoGuides) {
        readout.guideTemperatureC?.let { guideTemperatureC ->
            drawAdiabat(
                pressures = (listOf(readout.pressureHpa) + buildReferencePressures(readout.pressureHpa, topPressure, stepHpa = 25f))
                    .distinct()
                    .sortedDescending(),
                computeTemp = { pressure ->
                    moistAdiabatTempFromPointC(
                        startTemperatureC = guideTemperatureC,
                        startPressureHpa = readout.pressureHpa,
                        targetPressureHpa = pressure,
                    )
                },
                mapXY = { temperature, pressure ->
                    Offset(temperatureToX(temperature, pressure), pressureToY(pressure))
                },
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = pressureToY(topPressure),
                plotBottom = pressureToY(bottomPressure),
                color = Color(0xFF59A36A).copy(alpha = 0.86f),
                strokeWidth = 1.8f.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
            )
        }
    }

    readout.guideMixingRatioGKg?.let { mixingRatio ->
        drawAdiabat(
            pressures = buildReferencePressures(bottomPressure, topPressure, stepHpa = 25f),
            computeTemp = { pressure -> mixingRatioTemperatureC(mixingRatio, pressure) },
            mapXY = { temperature, pressure ->
                Offset(temperatureToX(temperature, pressure), pressureToY(pressure))
            },
            plotLeft = plotLeft,
            plotRight = plotRight,
            plotTop = pressureToY(topPressure),
            plotBottom = pressureToY(bottomPressure),
            color = onSurfaceColor.copy(alpha = 0.55f),
            strokeWidth = 1.5f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
        )
    }

    readout.temperatureC?.let { temperature ->
        drawCircle(
            color = Color(0xFFD83A3A),
            radius = 4.dp.toPx(),
            center = Offset(temperatureToX(temperature, readout.pressureHpa), cursorY),
        )
    }
    readout.dewpointC?.let { dewpoint ->
        drawCircle(
            color = Color(0xFF2E6FB5),
            radius = 3.6f.dp.toPx(),
            center = Offset(temperatureToX(dewpoint, readout.pressureHpa), cursorY),
        )
    }
    readout.parcelTemperatureC?.let { parcelTemperature ->
        val parcelX = temperatureToX(parcelTemperature, readout.pressureHpa)
        drawCircle(
            color = onSurfaceColor.copy(alpha = 0.65f),
            radius = 3.2f.dp.toPx(),
            center = Offset(parcelX, cursorY),
            style = Stroke(width = 1.6f.dp.toPx()),
        )

        readout.temperatureC?.let { ambientTemperature ->
            drawLine(
                color = Color(0xFFE2A85F).copy(alpha = 0.55f),
                start = Offset(temperatureToX(ambientTemperature, readout.pressureHpa), cursorY),
                end = Offset(parcelX, cursorY),
                strokeWidth = 1.8f.dp.toPx(),
            )
        }
    }

    readout.guideTemperatureC?.let { guideTemperature ->
        drawCircle(
            color = Color(0xFF59A36A),
            radius = 4.2f.dp.toPx(),
            center = Offset(temperatureToX(guideTemperature, readout.pressureHpa), cursorY),
            style = Stroke(width = 1.8f.dp.toPx()),
        )
    }
}

private fun drawMarkerLabel(
    canvas: ComposeGraphicsCanvas,
    text: String,
    y: Float,
    x: Float,
    color: Color,
    density: androidx.compose.ui.unit.Density,
    yOffsetPx: Float = 0f,
) {
    if (!y.isFinite()) return
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = with(density) { 9.sp.toPx() }
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.nativeCanvas.drawText(
        text,
        x,
        y - with(density) { 3.dp.toPx() } + yOffsetPx,
        labelPaint,
    )
}

private fun drawCursorInlineLabels(
    canvas: ComposeGraphicsCanvas,
    readout: CursorReadout,
    cursorY: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    plotBottom: Float,
    bottomPressure: Float,
    rightWindCenterX: Float,
    axisLabelPaint: Paint,
    altitudeLabelPaint: Paint,
    temperatureAxisBaseline: Float,
    temperatureReadoutBaseline: Float,
    temperatureReadoutLabelLeft: Float,
    temperatureReadoutLabelRight: Float,
    temperatureAxisRange: TempAxisRange,
    temperatureAxisLabelPaint: Paint,
    density: androidx.compose.ui.unit.Density,
    temperatureToX: (Float, Float) -> Float,
) {
    drawBadgeLabel(
        canvas = canvas,
        lines = listOf(
            formatReadoutHeight(readout.altitudeMeters),
            "${readout.pressureHpa.roundToInt()} hPa",
        ),
        centerX = plotLeft - with(density) { 10.dp.toPx() },
        centerY = cursorY,
        density = density,
        textPaint = Paint(axisLabelPaint).apply {
            color = Color(0xFF2B2B2B).toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        },
        backgroundColor = Color(0xFFF3F3F3),
        minWidth = with(density) { 46.dp.toPx() },
    )

    drawBadgeLabel(
        canvas = canvas,
        lines = listOf(formatAxisHeight(readout.altitudeMeters.toFloat())),
        centerX = plotRight + with(density) { 18.dp.toPx() },
        centerY = cursorY,
        density = density,
        textPaint = Paint(altitudeLabelPaint).apply {
            color = Color(0xFF2B2B2B).toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        },
        backgroundColor = Color(0xFFF3F3F3).copy(alpha = 0.92f),
        minWidth = with(density) { 34.dp.toPx() },
    )

    val pointLabelPaint = fun(color: Color): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    readout.temperatureC?.let { temperature ->
        val pointX = temperatureToX(temperature, readout.pressureHpa)
        drawPointValue(
            canvas = canvas,
            text = String.format(Locale.US, "T %.1f°", temperature),
            x = pointX + with(density) { 6.dp.toPx() },
            y = cursorY - with(density) { 6.dp.toPx() },
            paint = pointLabelPaint(Color(0xFFD83A3A)),
            maxX = plotRight - with(density) { 4.dp.toPx() },
            minX = plotLeft + with(density) { 4.dp.toPx() },
        )
    }

    readout.dewpointC?.let { dewpoint ->
        val pointX = temperatureToX(dewpoint, readout.pressureHpa)
        drawPointValue(
            canvas = canvas,
            text = String.format(Locale.US, "Td %.1f°", dewpoint),
            x = pointX - with(density) { 6.dp.toPx() },
            y = cursorY + with(density) { 16.dp.toPx() },
            paint = pointLabelPaint(Color(0xFF2E6FB5)).apply {
                textAlign = Paint.Align.RIGHT
            },
            maxX = plotRight - with(density) { 4.dp.toPx() },
            minX = plotLeft + with(density) { 4.dp.toPx() },
        )
    }

    readout.parcelTemperatureC?.let { parcelTemperature ->
        val pointX = temperatureToX(parcelTemperature, readout.pressureHpa)
        drawPointValue(
            canvas = canvas,
            text = String.format(Locale.US, "Parcel %.1f°", parcelTemperature),
            x = pointX + with(density) { 6.dp.toPx() },
            y = cursorY + with(density) { 28.dp.toPx() },
            paint = pointLabelPaint(Color(0xFF59A36A)),
            maxX = plotRight - with(density) { 4.dp.toPx() },
            minX = plotLeft + with(density) { 4.dp.toPx() },
        )
    }

    val axisBottomLabels = listOf(
        BottomAxisLabel(
            text = "${temperatureAxisRange.minC.roundToInt()}°",
            preferredX = plotLeft + temperatureAxisLabelPaint.measureText("${temperatureAxisRange.minC.roundToInt()}°") / 2f,
            paint = Paint(temperatureAxisLabelPaint).apply { textAlign = Paint.Align.CENTER },
        ),
        BottomAxisLabel(
            text = "${temperatureAxisRange.maxC.roundToInt()}°",
            preferredX = plotRight - temperatureAxisLabelPaint.measureText("${temperatureAxisRange.maxC.roundToInt()}°") / 2f,
            paint = Paint(temperatureAxisLabelPaint).apply { textAlign = Paint.Align.CENTER },
        ),
    )

    val readoutBottomLabels = buildList {
        readout.temperatureC?.let { temperature ->
            add(
                BottomAxisLabel(
                    text = String.format(Locale.US, "T %.0f°", temperature),
                    preferredX = temperatureToX(temperature, bottomPressure),
                    paint = pointLabelPaint(Color(0xFFD83A3A)).apply { textAlign = Paint.Align.CENTER },
                ),
            )
        }

        readout.dewpointC?.let { dewpoint ->
            add(
                BottomAxisLabel(
                    text = String.format(Locale.US, "Td %.0f°", dewpoint),
                    preferredX = temperatureToX(dewpoint, bottomPressure),
                    paint = pointLabelPaint(Color(0xFF2E6FB5)).apply { textAlign = Paint.Align.CENTER },
                ),
            )
        }

        readout.parcelSurfaceTemperatureC?.let { parcelSurfaceTemperature ->
            val bottomTemp = readout.guideDryThetaK?.let { dryAdiabatTempC(it, bottomPressure) } ?: return@let
            add(
                BottomAxisLabel(
                    text = String.format(Locale.US, "Parcel %.0f°", parcelSurfaceTemperature),
                    preferredX = temperatureToX(bottomTemp, bottomPressure),
                    paint = pointLabelPaint(Color(0xFF59A36A)).apply { textAlign = Paint.Align.CENTER },
                ),
            )
        }

        readout.criticalSurfaceDewpointC?.let { criticalSurfaceDewpoint ->
            val bottomTemp = readout.guideMixingRatioGKg?.let {
                mixingRatioTemperatureC(it, bottomPressure)
            } ?: return@let
            add(
                BottomAxisLabel(
                    text = String.format(Locale.US, "Crit Td %.0f°", criticalSurfaceDewpoint),
                    preferredX = temperatureToX(bottomTemp, bottomPressure),
                    paint = pointLabelPaint(Color(0xFFB7BCC7)).apply { textAlign = Paint.Align.CENTER },
                ),
            )
        }
    }

    drawBottomAxisLabels(
        canvas = canvas,
        labels = axisBottomLabels,
        y = temperatureAxisBaseline,
        plotLeft = plotLeft,
        plotRight = plotRight,
        minimumGapPx = with(density) { 10.dp.toPx() },
    )

    drawBottomAxisLabels(
        canvas = canvas,
        labels = readoutBottomLabels,
        y = temperatureReadoutBaseline,
        plotLeft = temperatureReadoutLabelLeft,
        plotRight = temperatureReadoutLabelRight,
        minimumGapPx = with(density) { 14.dp.toPx() },
    )

    if (readout.windSpeedKmh != null && readout.windDirectionDeg != null) {
        drawBadgeLabel(
            canvas = canvas,
            lines = listOf(
                String.format(Locale.US, "%.0f km/h %03.0f°", readout.windSpeedKmh, readout.windDirectionDeg),
            ),
            centerX = rightWindCenterX,
            centerY = (cursorY - with(density) { 10.dp.toPx() })
                .coerceAtLeast(plotTop + with(density) { 10.dp.toPx() }),
            density = density,
            textPaint = pointLabelPaint(Color(0xFF2B2B2B)).apply {
                textAlign = Paint.Align.CENTER
            },
            backgroundColor = Color(0xFFF3F3F3),
            minWidth = with(density) { 74.dp.toPx() },
        )
    }
}

private fun drawBadgeLabel(
    canvas: ComposeGraphicsCanvas,
    lines: List<String>,
    centerX: Float,
    centerY: Float,
    density: androidx.compose.ui.unit.Density,
    textPaint: Paint,
    backgroundColor: Color,
    minWidth: Float = 0f,
) {
    if (lines.isEmpty()) return

    val paddingHorizontal = with(density) { 6.dp.toPx() }
    val paddingVertical = with(density) { 4.dp.toPx() }
    val lineSpacing = with(density) { 2.dp.toPx() }
    val lineHeight = textPaint.textSize
    val maxTextWidth = lines.maxOf { textPaint.measureText(it) }
    val boxWidth = maxOf(minWidth, maxTextWidth + paddingHorizontal * 2f)
    val boxHeight = (lineHeight * lines.size) + lineSpacing * (lines.size - 1) + paddingVertical * 2f
    val rect = RectF(
        centerX - boxWidth / 2f,
        centerY - boxHeight / 2f,
        centerX + boxWidth / 2f,
        centerY + boxHeight / 2f,
    )

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor.toArgb()
    }
    canvas.nativeCanvas.drawRoundRect(
        rect,
        with(density) { 4.dp.toPx() },
        with(density) { 4.dp.toPx() },
        backgroundPaint,
    )

    val firstBaseline = rect.top + paddingVertical + lineHeight * 0.8f
    lines.forEachIndexed { index, line ->
        canvas.nativeCanvas.drawText(
            line,
            centerX,
            firstBaseline + index * (lineHeight + lineSpacing),
            textPaint,
        )
    }
}

private fun drawPointValue(
    canvas: ComposeGraphicsCanvas,
    text: String,
    x: Float,
    y: Float,
    paint: Paint,
    maxX: Float,
    minX: Float,
) {
    val measuredWidth = paint.measureText(text)
    val drawX = when (paint.textAlign) {
        Paint.Align.RIGHT -> x.coerceAtMost(maxX).coerceAtLeast(minX + measuredWidth)
        Paint.Align.CENTER -> x.coerceIn(minX + measuredWidth / 2f, maxX - measuredWidth / 2f)
        else -> x.coerceAtLeast(minX).coerceAtMost(maxX - measuredWidth)
    }
    canvas.nativeCanvas.drawText(text, drawX, y, paint)
}

private data class BottomAxisLabel(
    val text: String,
    val preferredX: Float,
    val paint: Paint,
)

private fun drawBottomAxisLabels(
    canvas: ComposeGraphicsCanvas,
    labels: List<BottomAxisLabel>,
    y: Float,
    plotLeft: Float,
    plotRight: Float,
    minimumGapPx: Float,
) {
    if (labels.isEmpty()) return

    val layout = labels
        .map { label -> label to label.paint.measureText(label.text) }
        .sortedBy { it.first.preferredX }
    val centers = layoutBottomAxisLabelCenters(
        preferredCenters = layout.map { it.first.preferredX },
        widths = layout.map { it.second },
        left = plotLeft,
        right = plotRight,
        minimumGapPx = minimumGapPx,
    )

    layout.zip(centers).forEach { (positioned, centerX) ->
        canvas.nativeCanvas.drawText(
            positioned.first.text,
            centerX,
            y,
            positioned.first.paint,
        )
    }
}

internal fun layoutBottomAxisLabelCenters(
    preferredCenters: List<Float>,
    widths: List<Float>,
    left: Float,
    right: Float,
    minimumGapPx: Float,
): List<Float> {
    require(preferredCenters.size == widths.size)
    if (preferredCenters.isEmpty()) return emptyList()

    val availableWidth = right - left
    if (availableWidth <= 0f) return preferredCenters

    val totalLabelWidth = widths.fold(0f) { sum, width -> sum + width }
    val effectiveGap = if (widths.size > 1) {
        minOf(
            minimumGapPx,
            ((availableWidth - totalLabelWidth) / (widths.size - 1)).coerceAtLeast(0f),
        )
    } else {
        0f
    }

    fun minCenter(index: Int) = left + widths[index] / 2f
    fun maxCenter(index: Int) = right - widths[index] / 2f
    fun clampedCenter(index: Int, center: Float): Float {
        val minCenter = minCenter(index)
        val maxCenter = maxCenter(index)
        return if (minCenter <= maxCenter) {
            center.coerceIn(minCenter, maxCenter)
        } else {
            (left + right) / 2f
        }
    }

    fun requiredCenterAfter(previousIndex: Int, currentIndex: Int): Float =
        widths[previousIndex] / 2f + effectiveGap + widths[currentIndex] / 2f

    val centers = preferredCenters
        .mapIndexed { index, center -> clampedCenter(index, center) }
        .toMutableList()

    for (index in 1 until centers.size) {
        centers[index] = maxOf(
            centers[index],
            centers[index - 1] + requiredCenterAfter(index - 1, index),
        )
    }

    val rightOverflow = centers.last() + widths.last() / 2f - right
    if (rightOverflow > 0f) {
        for (index in centers.indices) {
            centers[index] -= rightOverflow
        }
    }

    val leftOverflow = left - (centers.first() - widths.first() / 2f)
    if (leftOverflow > 0f) {
        for (index in centers.indices) {
            centers[index] += leftOverflow
        }
    }

    for (index in 1 until centers.size) {
        centers[index] = maxOf(
            centers[index],
            centers[index - 1] + requiredCenterAfter(index - 1, index),
        )
    }

    val finalRightOverflow = centers.last() + widths.last() / 2f - right
    if (finalRightOverflow > 0f) {
        for (index in centers.lastIndex downTo 0) {
            centers[index] -= finalRightOverflow
        }
    }

    for (index in centers.lastIndex - 1 downTo 0) {
        centers[index] = minOf(
            centers[index],
            centers[index + 1] - requiredCenterAfter(index, index + 1),
        )
    }

    return centers.mapIndexed { index, center -> clampedCenter(index, center) }
}

private fun buildCursorReadout(
    chart: StuveForecastChartUiModel,
    pressureHpa: Float,
    anchorTemperatureC: Float? = null,
    parcelPath: List<StuveProfilePoint> = chart.parcelAscentPath,
): CursorReadout {
    val clampedPressure = pressureHpa.coerceIn(
        chart.temperatureProfile.lastOrNull()?.pressureHpa ?: pressureHpa,
        chart.surfacePressureHpa,
    )
    val altitudeMeters = (
        interpolateProfileHeightMeters(chart.temperatureProfile, clampedPressure)
            ?: pressureToApproxHeightMeters(clampedPressure).toFloat()
        ).roundToInt()
    val nearestWind = chart.windBarbs.minByOrNull { abs(it.pressureHpa - clampedPressure) }
        ?.takeIf { abs(it.pressureHpa - clampedPressure) <= 60f }

    val envTemperatureC = interpolateProfileTemperature(chart.temperatureProfile, clampedPressure)
    // The guide (adiabat lines, parcel surface temperature) is derived from the tapped temperature
    // when the user selects a specific chart X position; otherwise fall back to the environmental
    // temperature at this level (backward-compatible behaviour).
    val guideTemperatureC = anchorTemperatureC ?: envTemperatureC

    return CursorReadout(
        pressureHpa = clampedPressure,
        altitudeMeters = altitudeMeters,
        temperatureC = envTemperatureC,
        dewpointC = interpolateProfileTemperature(chart.dewpointProfile, clampedPressure),
        parcelTemperatureC = interpolateProfileTemperature(parcelPath, clampedPressure),
        guideTemperatureC = guideTemperatureC,
        guideDryThetaK = guideTemperatureC?.let { temperature ->
            potentialTemperatureK(temperature, clampedPressure)
        },
        guideMixingRatioGKg = guideTemperatureC?.let { temperature ->
            satMixingRatioGKg(temperature, clampedPressure)
        },
        parcelSurfaceTemperatureC = guideTemperatureC?.let { temperature ->
            dryAdiabatTempC(
                potentialTemperatureK(temperature, clampedPressure),
                chart.surfacePressureHpa,
            )
        },
        criticalSurfaceDewpointC = guideTemperatureC?.let { temperature ->
            mixingRatioTemperatureC(
                satMixingRatioGKg(temperature, clampedPressure),
                chart.surfacePressureHpa,
            )
        },
        windSpeedKmh = nearestWind?.speedKmh,
        windDirectionDeg = nearestWind?.directionDeg,
    )
}

private suspend fun PointerInputScope.detectSkewTGestures(
    currentTopAltitudeKm: () -> Float,
    onVisibleTopAltitudeChange: (Float) -> Unit,
    onCursorStateChanged: (SkewTCursorState?) -> Unit,
    isInHeatingZone: (x: Float, y: Float) -> Boolean,
    onHeatingHandleDragDelta: (deltaX: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        if (isInHeatingZone(down.position.x, down.position.y)) {
            // ── Heating-handle drag: updates parcel start temperature ──
            var prevX = down.position.x
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled && event.changes.count { it.pressed } == 1) {
                    event.changes.firstOrNull { it.pressed }?.let { change ->
                        onHeatingHandleDragDelta(change.position.x - prevX)
                        prevX = change.position.x
                        change.consume()
                    }
                }
            } while (event.changes.any { it.pressed })
        } else {
            // ── Normal cursor / pinch-zoom gesture ──
            var cumulativeZoom = 1f
            var isZooming = false
            var gestureTopAltitudeKm = currentTopAltitudeKm()
            var hasDragged = false

            val startY = down.position.y
            val startX = down.position.x
            var latestY = startY
            var latestX = startX
            onCursorStateChanged(SkewTCursorState(y = latestY, x = latestX, isPinned = false))

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val pressedPointers = event.changes.count { it.pressed }
                    if (pressedPointers >= 2) {
                        if (!isZooming) {
                            isZooming = true
                            onCursorStateChanged(null)
                        }

                        val zoomChange = event.calculateZoom()
                        cumulativeZoom *= zoomChange
                        val zoomMotion = abs(1 - cumulativeZoom) * event.calculateCentroidSize(useCurrent = false)
                        if (zoomMotion > viewConfiguration.touchSlop && zoomChange != 1f) {
                            gestureTopAltitudeKm = zoomedTopAltitudeKm(
                                currentTopAltitudeKm = gestureTopAltitudeKm,
                                zoomChange = zoomChange,
                            )
                            onVisibleTopAltitudeChange(gestureTopAltitudeKm)
                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        }
                    } else {
                        event.changes.firstOrNull { it.pressed }?.let { change ->
                            latestY = change.position.y
                            latestX = change.position.x
                            if (abs(latestY - startY) > viewConfiguration.touchSlop ||
                                abs(latestX - startX) > viewConfiguration.touchSlop
                            ) {
                                hasDragged = true
                            }
                            onCursorStateChanged(SkewTCursorState(y = latestY, x = latestX, isPinned = false))
                        }
                    }
                }
            } while (event.changes.any { it.pressed })

            if (isZooming || hasDragged) {
                onCursorStateChanged(null)
            } else {
                onCursorStateChanged(SkewTCursorState(y = latestY, x = latestX, isPinned = true))
            }
        }
    }
}

private const val SKEWT_MIN_TOP_PRESSURE = 250f
private const val SKEWT_BOTTOM_PRESSURE = 1050f
private const val TEMP_MIN = -30f
private const val TEMP_MAX = 40f
private const val TEMP_STEP = 10f
private const val SKEWT_SKEW_RATIO = 0.45f
private const val STUVE_AUTO_FIT_MARGIN_KM = 0.35f
private const val TEMP_AXIS_FOCUS_TOP_PRESSURE_HPA = 650f
private const val TEMP_AXIS_LEFT_PADDING_C = 6f
private const val TEMP_AXIS_RIGHT_PADDING_C = 10f
private const val TEMP_AXIS_MIN_SPAN_C = 34f
private const val TEMP_AXIS_MAX_SPAN_C = 48f
private const val TEMP_AXIS_MAX_DEWPOINT_EXTENSION_C = 14f
private const val STUVE_INITIAL_AUTO_FIT_MAX_KM = 6.5f

private val ISOBAR_LABELS = listOf(
    1000f, 950f, 900f, 850f, 800f, 750f, 700f, 650f,
    600f, 550f, 500f, 450f, 400f, 350f, 300f, 250f,
)

private val STUVE_DRY_REFERENCE_PRESSURES = listOf(
    1050f, 1000f, 975f, 950f, 925f, 900f, 875f, 850f, 825f, 800f, 775f, 750f, 725f, 700f,
    675f, 650f, 625f, 600f, 575f, 550f, 525f, 500f, 475f, 450f, 425f, 400f, 375f, 350f,
    325f, 300f, 275f, 250f,
)

private fun pressureToY(
    pressureHpa: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    bottomPressure: Float = SKEWT_BOTTOM_PRESSURE,
): Float {
    val logPressure = ln(pressureHpa)
    val logBottom = ln(bottomPressure)
    val logTop = ln(topPressure)
    val fraction = (logPressure - logTop) / (logBottom - logTop)
    return plotTop + fraction * (plotBottom - plotTop)
}

private fun yToPressure(
    y: Float,
    plotTop: Float,
    plotBottom: Float,
    topPressure: Float,
    bottomPressure: Float = SKEWT_BOTTOM_PRESSURE,
): Float {
    val logBottom = ln(bottomPressure)
    val logTop = ln(topPressure)
    val fraction = ((y - plotTop) / (plotBottom - plotTop)).coerceIn(0f, 1f)
    val logPressure = logTop + fraction * (logBottom - logTop)
    return kotlin.math.exp(logPressure)
}

private fun skewTToX(
    temperatureC: Float,
    pressureHpa: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    skewFactor: Float,
    topPressure: Float,
    bottomPressure: Float,
): Float {
    val plotWidth = plotRight - plotLeft
    val normalizedTemperature = (temperatureC - tempMin) / (tempMax - tempMin)
    val logPressure = ln(pressureHpa)
    val logBottom = ln(bottomPressure)
    val logTop = ln(topPressure)
    val heightFraction = ((logBottom - logPressure) / (logBottom - logTop)).coerceIn(0f, 1f)
    return plotLeft + normalizedTemperature * plotWidth + heightFraction * skewFactor
}

/**
 * Inverse of [skewTToX]: converts a canvas X pixel coordinate at a given pressure level back
 * to the temperature in degrees Celsius. The skew factor cancels out because the slope of the
 * temperature mapping is constant across all pressure levels.
 */
private fun xToSkewTTemperature(
    x: Float,
    pressureHpa: Float,
    tempMin: Float,
    tempMax: Float,
    plotLeft: Float,
    plotRight: Float,
    skewFactor: Float,
    topPressure: Float,
    bottomPressure: Float,
): Float {
    val plotWidth = plotRight - plotLeft
    if (plotWidth <= 0f) return tempMin
    val logPressure = ln(pressureHpa)
    val logBottom = ln(bottomPressure)
    val logTop = ln(topPressure)
    val heightFraction = ((logBottom - logPressure) / (logBottom - logTop)).coerceIn(0f, 1f)
    val normalizedTemperature = (x - plotLeft - heightFraction * skewFactor) / plotWidth
    return normalizedTemperature * (tempMax - tempMin) + tempMin
}

private fun altitudeKmToApproxPressureHpa(altitudeKm: Float): Float {
    val heightMeters = (altitudeKm * 1000f).coerceAtLeast(0f)
    return 1013.25f * (1f - 0.0000225577f * heightMeters).pow(5.25588f)
}

private fun buildReferencePressures(
    bottomPressure: Float,
    topPressure: Float,
    stepHpa: Float,
): List<Float> {
    return buildList {
        var pressure = bottomPressure
        while (pressure >= topPressure) {
            add(pressure)
            pressure -= stepHpa
        }
    }
}

private fun selectPressureLabels(
    topPressure: Float,
    plotHeight: Float,
): List<Float> {
    val visibleLabels = ISOBAR_LABELS.filter { it >= topPressure }
    return if (visibleLabels.isEmpty()) {
        emptyList()
    } else if (plotHeight / visibleLabels.size >= 28f) {
        visibleLabels
    } else {
        visibleLabels.filter { pressure ->
            pressure.toInt() % 100 == 0 || pressure in listOf(950f, 850f, 700f, 500f, 300f, 250f)
        }
    }
}

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
    val points = pressures.map { pressure ->
        mapXY(computeTemp(pressure), pressure)
    }.filter { point ->
        point.x in (plotLeft - 24f)..(plotRight + 24f) && point.y in plotTop..plotBottom
    }

    for (index in 0 until points.size - 1) {
        drawLine(
            color = color,
            start = points[index],
            end = points[index + 1],
            strokeWidth = strokeWidth,
            pathEffect = pathEffect,
        )
    }
}

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
    val offsets = points.map { point -> mapXY(point.temperatureC, point.pressureHpa) }

    for (index in 0 until offsets.size - 1) {
        val start = offsets[index]
        val end = offsets[index + 1]
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

    if (drawDataDots && dataDotRadius > 0f) {
        points.forEachIndexed { index, point ->
            if (point.isRealData) {
                val offset = offsets[index]
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

    val arrowLength = halfSize * 0.4f
    val arrowAngle = PI.toFloat() / 6f
    drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(
            tipX - cos(angleRad - arrowAngle) * arrowLength,
            tipY - sin(angleRad - arrowAngle) * arrowLength,
        ),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = color,
        start = Offset(tipX, tipY),
        end = Offset(
            tipX - cos(angleRad + arrowAngle) * arrowLength,
            tipY - sin(angleRad + arrowAngle) * arrowLength,
        ),
        strokeWidth = 1.5f,
    )
}

private fun formatHeightSummary(heightMeters: Float): String {
    return if (heightMeters >= 1000f) {
        String.format(Locale.US, "%.1f km", heightMeters / 1000f)
    } else {
        "${heightMeters.roundToInt()} m"
    }
}

private fun formatAxisHeight(heightMeters: Float): String {
    return if (heightMeters >= 1000f) {
        String.format(Locale.US, "%.1fk", heightMeters / 1000f)
    } else {
        "${heightMeters.roundToInt()}m"
    }
}

private fun formatReadoutHeight(heightMeters: Int): String {
    return if (heightMeters >= 1000) {
        String.format(Locale.US, "%.1f km", heightMeters / 1000f)
    } else {
        "${heightMeters} m"
    }
}

internal fun recommendedStuveTopAltitudeKm(chart: StuveForecastChartUiModel): Float {
    val topHeightMeters = chart.temperatureProfile.lastOrNull()?.heightMeters
        ?: chart.dewpointProfile.lastOrNull()?.heightMeters
        ?: chart.windBarbs.minByOrNull { it.pressureHpa }?.let { windBarb ->
            pressureToApproxHeightMeters(windBarb.pressureHpa).toFloat()
        }
        ?: pressureToApproxHeightMeters(
            chart.temperatureProfile.lastOrNull()?.pressureHpa ?: SKEWT_MIN_TOP_PRESSURE,
        ).toFloat()

    return ((topHeightMeters / 1000f) + STUVE_AUTO_FIT_MARGIN_KM)
        .coerceIn(DEFAULT_TOP_ALTITUDE_KM, STUVE_INITIAL_AUTO_FIT_MAX_KM)
}

internal data class TempAxisRange(
    val minC: Float,
    val maxC: Float,
)

internal fun buildVisibleTemperatureAxisRange(
    chart: StuveForecastChartUiModel,
    topPressure: Float,
    bottomPressure: Float,
): TempAxisRange {
    val visibleTemperatures = collectProfileTemperatures(
        profile = chart.temperatureProfile,
        topPressure = topPressure,
        bottomPressure = bottomPressure,
    )
    val visibleDewpoints = collectProfileTemperatures(
        profile = chart.dewpointProfile,
        topPressure = topPressure,
        bottomPressure = bottomPressure,
    )

    if (visibleTemperatures.isEmpty() && visibleDewpoints.isEmpty()) {
        return TempAxisRange(TEMP_MIN, TEMP_MAX)
    }

    val focusTopPressure = maxOf(topPressure, TEMP_AXIS_FOCUS_TOP_PRESSURE_HPA)
    val focusedTemperatures = collectProfileTemperatures(
        profile = chart.temperatureProfile,
        topPressure = focusTopPressure,
        bottomPressure = bottomPressure,
    )
    val focusedDewpoints = collectProfileTemperatures(
        profile = chart.dewpointProfile,
        topPressure = focusTopPressure,
        bottomPressure = bottomPressure,
    )
    val temperatureReference = (focusedTemperatures.ifEmpty { visibleTemperatures }).maxOrNull()
        ?: visibleTemperatures.maxOrNull()
        ?: TEMP_MAX
    val lowerReferenceTemperatures = focusedTemperatures.ifEmpty { visibleTemperatures }
    val lowerReferenceDewpoints = focusedDewpoints.ifEmpty { visibleDewpoints }
    val temperatureMin = lowerReferenceTemperatures.minOrNull() ?: temperatureReference
    val boundedDewpointMin = lowerReferenceDewpoints.minOrNull()
        ?.coerceAtLeast(temperatureMin - TEMP_AXIS_MAX_DEWPOINT_EXTENSION_C)
        ?: temperatureMin
    val heatedSurfaceMax = maxOf(
        chart.temperatureProfile.firstOrNull()?.temperatureC ?: temperatureReference,
        chart.parcelAscentPath.firstOrNull()?.temperatureC ?: temperatureReference,
        chart.tconC ?: temperatureReference,
        temperatureReference,
    )

    val rawMin = minOf(temperatureMin, boundedDewpointMin) - TEMP_AXIS_LEFT_PADDING_C
    val rawMax = heatedSurfaceMax + TEMP_AXIS_RIGHT_PADDING_C
    val span = (rawMax - rawMin).coerceIn(TEMP_AXIS_MIN_SPAN_C, TEMP_AXIS_MAX_SPAN_C)
    val center = (rawMin + rawMax) / 2f
    return TempAxisRange(
        minC = floorToStep(center - span / 2f, TEMP_STEP),
        maxC = ceilToStep(center + span / 2f, TEMP_STEP),
    )
}

private fun collectProfileTemperatures(
    profile: List<StuveProfilePoint>,
    topPressure: Float,
    bottomPressure: Float,
): List<Float> = buildList {
    profile
        .filter { it.pressureHpa in topPressure..bottomPressure }
        .forEach { point -> add(point.temperatureC) }
    interpolateProfileTemperature(profile, topPressure)?.let(::add)
    interpolateProfileTemperature(profile, bottomPressure)?.let(::add)
}

private fun buildTemperatureAxisLabels(range: TempAxisRange): List<Float> {
    val labels = mutableListOf<Float>()
    var value = ceilToStep(range.minC, TEMP_STEP)
    while (value <= range.maxC + 0.01f) {
        labels += value
        value += TEMP_STEP
    }
    return labels.ifEmpty { listOf(range.minC, range.maxC) }
}

private fun floorToStep(value: Float, step: Float): Float =
    floor(value / step) * step

private fun ceilToStep(value: Float, step: Float): Float =
    ceil(value / step) * step

/**
 * Constructs a minimal list of [ProfileLevel] objects from the chart's temperature and
 * dewpoint profiles. Used as input to [buildParcelAscentPath] for live parcel recomputation.
 */
private fun buildMinimalProfileLevels(
    chart: StuveForecastChartUiModel,
): List<ProfileLevel> = chart.temperatureProfile.map { point ->
    ProfileLevel(
        pressureHpa = point.pressureHpa,
        temperatureC = point.temperatureC,
        dewPointC = interpolateProfileTemperature(chart.dewpointProfile, point.pressureHpa),
        heightKm = (point.heightMeters ?: pressureToApproxHeightMeters(point.pressureHpa).toFloat()) / 1000f,
    )
}

/**
 * Builds a parcel guide anchored to a specific chart point selected by the user.
 *
 * The branch below the anchor follows the dry adiabat through the selected point, and the branch
 * above the anchor follows the moist adiabat through the same point. This guarantees that the
 * rendered interactive guide passes through the clicked point and visually separates the dry and
 * moist regimes around that anchor.
 */
internal fun buildInteractiveParcelFromPoint(
    anchorTemperatureC: Float,
    anchorPressureHpa: Float,
    chart: StuveForecastChartUiModel,
    parcelPressures: List<Float>,
): List<StuveProfilePoint> {
    val dryThetaK = potentialTemperatureK(anchorTemperatureC, anchorPressureHpa)
    val denseReferencePressures = buildReferencePressures(
        bottomPressure = maxOf(anchorPressureHpa, parcelPressures.maxOrNull() ?: anchorPressureHpa),
        topPressure = minOf(anchorPressureHpa, parcelPressures.minOrNull() ?: anchorPressureHpa),
        stepHpa = 10f,
    )
    val renderablePressures = (parcelPressures + denseReferencePressures + anchorPressureHpa)
        .distinct()
        .sortedDescending()

    return renderablePressures.map { pressure ->
        val heightMeters = interpolateProfileHeightMeters(chart.temperatureProfile, pressure)
            ?: pressureToApproxHeightMeters(pressure).toFloat()
        val temperatureC = if (pressure >= anchorPressureHpa) {
            dryAdiabatTempC(dryThetaK, pressure)
        } else {
            moistAdiabatTempFromPointC(
                startTemperatureC = anchorTemperatureC,
                startPressureHpa = anchorPressureHpa,
                targetPressureHpa = pressure,
            )
        }
        StuveProfilePoint(
            pressureHpa = pressure,
            temperatureC = temperatureC,
            heightMeters = heightMeters,
        )
    }
}

/**
 * Builds a live parcel ascent path starting at [parcelStartTempC] at the surface pressure.
 * Used when the bottom heating handle is dragged.
 */
private fun buildInteractiveParcelFromSurface(
    parcelStartTempC: Float,
    chart: StuveForecastChartUiModel,
    profileLevels: List<ProfileLevel>,
    parcelPressures: List<Float>,
): List<StuveProfilePoint> {
    val surfaceDewPointC = chart.dewpointProfile.firstOrNull()?.temperatureC
        ?: (parcelStartTempC - 8f)
    return buildParcelAscentPath(
        pressures = parcelPressures,
        profile = profileLevels,
        surfaceTemperatureC = parcelStartTempC,
        surfaceDewPointC = surfaceDewPointC,
        surfacePressureHpa = chart.surfacePressureHpa,
        surfaceHeatingC = 0f,
    )
}

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
