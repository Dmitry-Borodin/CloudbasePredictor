package com.cloudbasepredictor.ui.screens.forecast.views

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudbasepredictor.model.ForecastMode
import com.cloudbasepredictor.ui.preview.PreviewData
import com.cloudbasepredictor.ui.screens.forecast.CloudForecastChartUiModel
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_LAYERS_ROW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_RADIATION_ROW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_RAIN_ROW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_SCROLL
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_SUNSHINE_ROW
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.util.Locale
import kotlin.math.max

@Composable
internal fun CloudForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(CLOUD_VIEW),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(CLOUD_SCROLL),
        ) {
            // Sunshine duration row (short)
            CloudSunshineCanvas(
                chart = uiState.cloudChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SUNSHINE_ROW_HEIGHT)
                    .testTag(CLOUD_SUNSHINE_ROW),
            )

            // Shortwave radiation row (bar chart like rain)
            CloudRadiationCanvas(
                chart = uiState.cloudChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RADIATION_ROW_HEIGHT)
                    .testTag(CLOUD_RADIATION_ROW),
            )

            // Cloud layers (High / Mid / Low)
            CloudLayersCanvas(
                chart = uiState.cloudChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CLOUD_LAYERS_HEIGHT)
                    .testTag(CLOUD_LAYERS_ROW),
            )

            // Rain row (bar chart)
            CloudRainCanvas(
                chart = uiState.cloudChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RAIN_ROW_HEIGHT)
                    .testTag(CLOUD_RAIN_ROW),
            )

            // Time axis at the bottom
            CloudTimeAxisCanvas(
                chart = uiState.cloudChart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TIME_AXIS_HEIGHT),
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

// ── Sunshine row ──────────────────────────────────────────────────

@Composable
private fun CloudSunshineCanvas(
    chart: CloudForecastChartUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridBackgroundColor = cloudGridBackground()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val labelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val valuePaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFFFF8F00).toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier) {
        val leftAxisWidth = with(density) { LEFT_AXIS_WIDTH.toPx() }
        val plotLeft = leftAxisWidth
        val plotWidth = size.width - plotLeft
        if (plotWidth <= 0f || chart.hours.isEmpty()) return@Canvas

        drawRect(color = gridBackgroundColor, topLeft = Offset.Zero, size = size)

        val columnWidth = plotWidth / chart.hours.size

        drawVerticalGrid(chart, plotLeft, columnWidth, outlineColor, 0f, size.height)

        // Sun circles sized by fraction
        val sunColor = Color(0xFFFFB300)
        chart.sunshine.forEach { sun ->
            val hourIndex = chart.hours.indexOf(sun.hour)
            if (hourIndex < 0) return@forEach
            val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f
            val fraction = (sun.durationS / 3600f).coerceIn(0f, 1f)
            if (fraction > 0.01f) {
                val maxRadius = (minOf(columnWidth, size.height) * 0.35f)
                val radius = maxRadius * (0.4f + 0.6f * fraction)
                drawCircle(
                    color = sunColor.copy(alpha = 0.3f + 0.5f * fraction),
                    radius = radius,
                    center = Offset(cx, size.height / 2f),
                )
            }
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "☀ h",
                8.dp.toPx(),
                size.height / 2f + labelPaint.textSize * 0.35f,
                labelPaint,
            )

            val labelWidth = valuePaint.measureText("0.9")
            val cluster = max(1, kotlin.math.ceil(labelWidth * 1.3f / columnWidth).toInt())
            chart.sunshine.forEachIndexed { idx, sun ->
                if (idx % cluster != cluster / 2) return@forEachIndexed
                val hourIndex = chart.hours.indexOf(sun.hour)
                if (hourIndex < 0) return@forEachIndexed
                val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f
                val hours = sun.durationS / 3600f
                if (hours > 0.05f) {
                    canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%.1f", hours),
                        cx,
                        size.height / 2f + valuePaint.textSize * 0.35f,
                        valuePaint,
                    )
                }
            }
        }

        drawLine(
            color = outlineColor.copy(alpha = 0.4f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// ── Shortwave radiation row ───────────────────────────────────────

@Composable
private fun CloudRadiationCanvas(
    chart: CloudForecastChartUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridBackgroundColor = cloudGridBackground()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val labelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val valuePaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFFFF8F00).toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }

    Canvas(modifier = modifier) {
        val leftAxisWidth = with(density) { LEFT_AXIS_WIDTH.toPx() }
        val plotLeft = leftAxisWidth
        val plotWidth = size.width - plotLeft
        if (plotWidth <= 0f || chart.hours.isEmpty()) return@Canvas

        drawRect(color = gridBackgroundColor, topLeft = Offset.Zero, size = size)

        val columnWidth = plotWidth / chart.hours.size
        val maxRadiation = 800f

        drawVerticalGrid(chart, plotLeft, columnWidth, outlineColor, 0f, size.height)

        chart.radiation.forEach { rad ->
            val hourIndex = chart.hours.indexOf(rad.hour)
            if (hourIndex < 0) return@forEach
            val x = plotLeft + hourIndex * columnWidth

            if (rad.radiationWm2 > 0f) {
                val barHeight = (rad.radiationWm2 / maxRadiation).coerceIn(0f, 1f) * (size.height * 0.7f)
                val barTop = size.height - barHeight - 2.dp.toPx()
                drawRoundRect(
                    color = radiationColor(rad.radiationWm2),
                    topLeft = Offset(x + columnWidth * 0.15f, barTop),
                    size = Size(columnWidth * 0.7f, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "W/m²",
                8.dp.toPx(),
                size.height / 2f + labelPaint.textSize * 0.35f,
                labelPaint,
            )

            val labelWidth = valuePaint.measureText("999")
            val cluster = max(1, kotlin.math.ceil(labelWidth * 1.3f / columnWidth).toInt())
            chart.radiation.forEachIndexed { idx, rad ->
                if (idx % cluster != cluster / 2) return@forEachIndexed
                val hourIndex = chart.hours.indexOf(rad.hour)
                if (hourIndex < 0) return@forEachIndexed
                val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f
                if (rad.radiationWm2 > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${rad.radiationWm2.toInt()}",
                        cx,
                        valuePaint.textSize + 2.dp.toPx(),
                        valuePaint,
                    )
                }
            }
        }

        drawLine(
            color = outlineColor.copy(alpha = 0.4f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// ── Cloud layers (High / Mid / Low) ───────────────────────────────

@Composable
private fun CloudLayersCanvas(
    chart: CloudForecastChartUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridBackgroundColor = cloudGridBackground()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val axisLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 11.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val percentPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier) {
        val leftAxisWidth = with(density) { LEFT_AXIS_WIDTH.toPx() }
        val plotLeft = leftAxisWidth
        val plotWidth = size.width - plotLeft
        if (plotWidth <= 0f || chart.hours.isEmpty()) return@Canvas

        drawRect(color = gridBackgroundColor, topLeft = Offset.Zero, size = size)

        val layerSpacing = with(density) { 4.dp.toPx() }
        val layerHeight = (size.height - layerSpacing * 2f) / 3f
        val columnWidth = plotWidth / chart.hours.size

        fun layerTopY(index: Int): Float = index * (layerHeight + layerSpacing)

        drawVerticalGrid(chart, plotLeft, columnWidth, outlineColor, 0f, size.height)

        for (i in 1..2) {
            val dividerY = layerTopY(i) - layerSpacing / 2f
            drawLine(
                color = outlineColor.copy(alpha = 0.3f),
                start = Offset(0f, dividerY),
                end = Offset(size.width, dividerY),
                strokeWidth = 1.dp.toPx(),
            )
        }

        chart.layers.forEach { layer ->
            val hourIndex = chart.hours.indexOf(layer.hour)
            if (hourIndex < 0) return@forEach
            val x = plotLeft + hourIndex * columnWidth

            drawCloudCell(x, layerTopY(0), columnWidth, layerHeight, layer.highCloudPercent, CLOUD_COLOR)
            drawCloudCell(x, layerTopY(1), columnWidth, layerHeight, layer.midCloudPercent, CLOUD_COLOR)
            drawCloudCell(x, layerTopY(2), columnWidth, layerHeight, layer.lowCloudPercent, CLOUD_COLOR)
        }

        for (i in 0..2) {
            drawRect(
                color = outlineColor.copy(alpha = 0.4f),
                topLeft = Offset(plotLeft, layerTopY(i)),
                size = Size(plotWidth, layerHeight),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        drawIntoCanvas { canvas ->
            val layerNames = listOf("High", "Mid", "Low")
            layerNames.forEachIndexed { index, name ->
                val centerY = layerTopY(index) + layerHeight / 2f
                canvas.nativeCanvas.drawText(
                    name,
                    8.dp.toPx(),
                    centerY + axisLabelPaint.textSize * 0.35f,
                    axisLabelPaint,
                )
            }

            val labelWidth = percentPaint.measureText("99%")
            val cluster = max(1, kotlin.math.ceil(labelWidth * 1.3f / columnWidth).toInt())

            chart.layers.forEachIndexed { idx, layer ->
                if (idx % cluster != cluster / 2) return@forEachIndexed
                val hourIndex = chart.hours.indexOf(layer.hour)
                if (hourIndex < 0) return@forEachIndexed
                val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f

                if (layer.highCloudPercent > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${layer.highCloudPercent.toInt()}%", cx,
                        layerTopY(0) + layerHeight / 2f + percentPaint.textSize * 0.35f,
                        percentPaint,
                    )
                }
                if (layer.midCloudPercent > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${layer.midCloudPercent.toInt()}%", cx,
                        layerTopY(1) + layerHeight / 2f + percentPaint.textSize * 0.35f,
                        percentPaint,
                    )
                }
                if (layer.lowCloudPercent > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${layer.lowCloudPercent.toInt()}%", cx,
                        layerTopY(2) + layerHeight / 2f + percentPaint.textSize * 0.35f,
                        percentPaint,
                    )
                }
            }
        }

        drawLine(
            color = outlineColor.copy(alpha = 0.4f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// ── Rain row ──────────────────────────────────────────────────────

@Composable
private fun CloudRainCanvas(
    chart: CloudForecastChartUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridBackgroundColor = cloudGridBackground()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val labelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val precipPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF1565C0).toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }

    Canvas(modifier = modifier) {
        val leftAxisWidth = with(density) { LEFT_AXIS_WIDTH.toPx() }
        val plotLeft = leftAxisWidth
        val plotWidth = size.width - plotLeft
        if (plotWidth <= 0f || chart.hours.isEmpty()) return@Canvas

        drawRect(color = gridBackgroundColor, topLeft = Offset.Zero, size = size)

        val columnWidth = plotWidth / chart.hours.size

        drawVerticalGrid(chart, plotLeft, columnWidth, outlineColor, 0f, size.height)

        chart.precipitation.forEach { precip ->
            val hourIndex = chart.hours.indexOf(precip.hour)
            if (hourIndex < 0) return@forEach
            val x = plotLeft + hourIndex * columnWidth

            if (precip.amountMm > 0f) {
                val barHeight = (precip.amountMm / 8f).coerceIn(0f, 1f) * (size.height * 0.6f)
                val barTop = size.height - barHeight - 2.dp.toPx()
                drawRoundRect(
                    color = precipColor(precip.amountMm),
                    topLeft = Offset(x + columnWidth * 0.15f, barTop),
                    size = Size(columnWidth * 0.7f, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                "Rain",
                8.dp.toPx(),
                size.height / 2f + labelPaint.textSize * 0.35f,
                labelPaint,
            )

            val labelWidth = precipPaint.measureText("99%")
            val cluster = max(1, kotlin.math.ceil(labelWidth * 1.3f / columnWidth).toInt())

            chart.precipitation.forEachIndexed { idx, precip ->
                if (idx % cluster != cluster / 2) return@forEachIndexed
                val hourIndex = chart.hours.indexOf(precip.hour)
                if (hourIndex < 0) return@forEachIndexed
                val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f

                if (precip.amountMm > 0f) {
                    canvas.nativeCanvas.drawText(
                        "${precip.probabilityPercent.toInt()}%",
                        cx,
                        precipPaint.textSize + 2.dp.toPx(),
                        precipPaint,
                    )
                    canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%.1f", precip.amountMm),
                        cx,
                        precipPaint.textSize * 2f + 4.dp.toPx(),
                        precipPaint,
                    )
                } else if (precip.probabilityPercent > 10f) {
                    canvas.nativeCanvas.drawText(
                        "${precip.probabilityPercent.toInt()}%",
                        cx,
                        precipPaint.textSize + 2.dp.toPx(),
                        precipPaint,
                    )
                }
            }
        }

        drawLine(
            color = outlineColor.copy(alpha = 0.4f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// ── Time axis ─────────────────────────────────────────────────────

@Composable
private fun CloudTimeAxisCanvas(
    chart: CloudForecastChartUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridBackgroundColor = cloudGridBackground()

    val hourLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
    }

    Canvas(modifier = modifier) {
        val leftAxisWidth = with(density) { LEFT_AXIS_WIDTH.toPx() }
        val plotLeft = leftAxisWidth
        val plotWidth = size.width - plotLeft
        if (plotWidth <= 0f || chart.hours.isEmpty()) return@Canvas

        drawRect(color = gridBackgroundColor, topLeft = Offset.Zero, size = size)

        val columnWidth = plotWidth / chart.hours.size

        drawIntoCanvas { canvas ->
            chart.hours.forEachIndexed { index, hour ->
                if ((hour - chart.hours.first()) % 3 != 0) return@forEachIndexed
                val labelCenterX = plotLeft + index * columnWidth + columnWidth / 2f
                canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%02d", hour),
                    labelCenterX,
                    hourLabelPaint.textSize + 4.dp.toPx(),
                    hourLabelPaint,
                )
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────

@Composable
private fun cloudGridBackground(): Color = lerp(
    start = MaterialTheme.colorScheme.surface,
    stop = MaterialTheme.colorScheme.onSurface,
    fraction = 0.035f,
)

private fun DrawScope.drawVerticalGrid(
    chart: CloudForecastChartUiModel,
    plotLeft: Float,
    columnWidth: Float,
    outlineColor: Color,
    top: Float,
    bottom: Float,
) {
    chart.hours.forEachIndexed { index, hour ->
        val x = plotLeft + index * columnWidth
        val alpha = if ((hour - chart.hours.first()) % 3 == 0) 0.4f else 0.18f
        drawLine(
            color = outlineColor.copy(alpha = alpha),
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

private fun DrawScope.drawCloudCell(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    percent: Float,
    color: Color,
) {
    val alpha = (percent / 100f).coerceIn(0f, 1f) * 0.7f
    if (alpha > 0.02f) {
        drawRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(x, y),
            size = Size(width, height),
        )
    }
}

private fun precipColor(amountMm: Float): Color {
    val normalized = (amountMm / 8f).coerceIn(0f, 1f)
    val light = Color(0xFF90CAF9)
    val medium = Color(0xFF42A5F5)
    val heavy = Color(0xFF1565C0)
    return if (normalized <= 0.5f) {
        lerp(light, medium, normalized / 0.5f)
    } else {
        lerp(medium, heavy, (normalized - 0.5f) / 0.5f)
    }
}

private fun radiationColor(wm2: Float): Color {
    val normalized = (wm2 / 800f).coerceIn(0f, 1f)
    val low = Color(0xFFFFF9C4) // pale yellow
    val mid = Color(0xFFFFB300) // amber
    val high = Color(0xFFFF6F00) // deep orange
    return if (normalized <= 0.5f) {
        lerp(low, mid, normalized / 0.5f)
    } else {
        lerp(mid, high, (normalized - 0.5f) / 0.5f)
    }
}

private val CLOUD_COLOR = Color(0xFF78909C)

private val LEFT_AXIS_WIDTH = 60.dp
private val SUNSHINE_ROW_HEIGHT = 32.dp
private val RADIATION_ROW_HEIGHT = 48.dp
private val CLOUD_LAYERS_HEIGHT = 144.dp
private val RAIN_ROW_HEIGHT = 48.dp
private val TIME_AXIS_HEIGHT = 28.dp

// ── Previews ──────────────────────────────────────────────────────

@Preview(name = "Cloud Default", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CloudForecastViewPreview() {
    CloudbasePredictorTheme {
        CloudForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.CLOUD),
        )
    }
}

@Preview(name = "Cloud Loading", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CloudForecastViewLoadingPreview() {
    CloudbasePredictorTheme {
        CloudForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.CLOUD,
                isLoading = true,
            ),
        )
    }
}

@Preview(name = "Cloud Error", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun CloudForecastViewErrorPreview() {
    CloudbasePredictorTheme {
        CloudForecastView(
            uiState = PreviewData.forecastUiStateForMode(
                mode = ForecastMode.CLOUD,
                errorMessage = "Unable to refresh cloud forecast.",
            ),
        )
    }
}

@Preview(
    name = "Cloud Dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CloudForecastViewDarkPreview() {
    CloudbasePredictorTheme(darkTheme = true) {
        CloudForecastView(
            uiState = PreviewData.forecastUiStateForMode(ForecastMode.CLOUD),
        )
    }
}
