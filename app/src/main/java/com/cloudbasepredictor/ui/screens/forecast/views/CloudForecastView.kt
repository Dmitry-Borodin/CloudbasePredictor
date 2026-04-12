package com.cloudbasepredictor.ui.screens.forecast.views

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.cloudbasepredictor.ui.screens.forecast.ForecastTestTags.CLOUD_VIEW
import com.cloudbasepredictor.ui.screens.forecast.ForecastUiState
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import java.util.Locale

@Composable
internal fun CloudForecastView(
    uiState: ForecastUiState,
    modifier: Modifier = Modifier,
    onVisibleTopAltitudeChange: (Float) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(CLOUD_VIEW),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            CloudChartCanvas(
                chart = uiState.cloudChart,
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
}

@Composable
private fun CloudChartCanvas(
    chart: CloudForecastChartUiModel,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridBackgroundColor = lerp(
        start = MaterialTheme.colorScheme.surface,
        stop = MaterialTheme.colorScheme.onSurface,
        fraction = 0.035f,
    )
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    val axisLabelPaint = remember(density, axisLabelColor) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisLabelColor.toArgb()
            textSize = with(density) { 11.sp.toPx() }
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
    val legendPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF444444).toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
    }
    val percentPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF333333).toArgb()
            textSize = with(density) { 9.sp.toPx() }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
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
        val leftAxisWidth = with(density) { 60.dp.toPx() }
        val bottomAxisHeight = with(density) { 28.dp.toPx() }
        val precipBarHeight = with(density) { 48.dp.toPx() }

        val plotLeft = leftAxisWidth
        val plotTop = 0f
        val plotRight = size.width
        val plotBottom = size.height - bottomAxisHeight - precipBarHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        if (plotWidth <= 0f || plotHeight <= 0f || chart.hours.isEmpty()) return@Canvas

        // Background for axis and plot
        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(0f, plotTop),
            size = Size(leftAxisWidth, plotHeight),
        )
        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
        )

        // Three equal rows for High, Mid, Low clouds (top to bottom)
        val layerHeight = plotHeight / 3f
        val columnWidth = plotWidth / chart.hours.size

        // Draw layer backgrounds and labels
        val layerNames = listOf("High", "Mid", "Low")
        val layerDividerYs = listOf(plotTop + layerHeight, plotTop + 2 * layerHeight)

        // Horizontal layer dividers
        layerDividerYs.forEach { y ->
            drawLine(
                color = outlineColor.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Vertical grid lines
        chart.hours.forEachIndexed { index, hour ->
            val x = plotLeft + index * columnWidth
            val alpha = if ((hour - chart.hours.first()) % 3 == 0) 0.4f else 0.18f
            drawLine(
                color = outlineColor.copy(alpha = alpha),
                start = Offset(x, plotTop),
                end = Offset(x, plotBottom + precipBarHeight),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Cloud coverage cells
        chart.layers.forEach { layer ->
            val hourIndex = chart.hours.indexOf(layer.hour)
            if (hourIndex < 0) return@forEach
            val x = plotLeft + hourIndex * columnWidth

            // High cloud (row 0)
            drawCloudCell(
                x = x,
                y = plotTop,
                width = columnWidth,
                height = layerHeight,
                percent = layer.highCloudPercent,
                color = CLOUD_HIGH_COLOR,
            )

            // Mid cloud (row 1)
            drawCloudCell(
                x = x,
                y = plotTop + layerHeight,
                width = columnWidth,
                height = layerHeight,
                percent = layer.midCloudPercent,
                color = CLOUD_MID_COLOR,
            )

            // Low cloud (row 2)
            drawCloudCell(
                x = x,
                y = plotTop + 2 * layerHeight,
                width = columnWidth,
                height = layerHeight,
                percent = layer.lowCloudPercent,
                color = CLOUD_LOW_COLOR,
            )
        }

        // Precipitation bar area
        val precipTop = plotBottom
        val precipBottom = plotBottom + precipBarHeight

        drawRect(
            color = gridBackgroundColor,
            topLeft = Offset(0f, precipTop),
            size = Size(size.width, precipBarHeight),
        )
        drawLine(
            color = outlineColor.copy(alpha = 0.5f),
            start = Offset(0f, precipTop),
            end = Offset(plotRight, precipTop),
            strokeWidth = 1.dp.toPx(),
        )

        // Precipitation bars
        chart.precipitation.forEach { precip ->
            val hourIndex = chart.hours.indexOf(precip.hour)
            if (hourIndex < 0) return@forEach
            val x = plotLeft + hourIndex * columnWidth

            if (precip.amountMm > 0f) {
                val barHeight = (precip.amountMm / 8f).coerceIn(0f, 1f) * (precipBarHeight * 0.6f)
                val barTop = precipBottom - barHeight - 2.dp.toPx()
                drawRoundRect(
                    color = precipColor(precip.amountMm),
                    topLeft = Offset(x + columnWidth * 0.15f, barTop),
                    size = Size(columnWidth * 0.7f, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
        }

        // Plot outline
        drawRect(
            color = outlineColor.copy(alpha = 0.4f),
            topLeft = Offset(plotLeft, plotTop),
            size = Size(plotWidth, plotHeight),
            style = Stroke(width = 1.dp.toPx()),
        )

        // Labels
        drawIntoCanvas { canvas ->
            // Layer names on the left axis
            layerNames.forEachIndexed { index, name ->
                val layerCenterY = plotTop + index * layerHeight + layerHeight / 2f
                canvas.nativeCanvas.drawText(
                    name,
                    8.dp.toPx(),
                    layerCenterY + axisLabelPaint.textSize * 0.35f,
                    axisLabelPaint,
                )
            }

            // Precipitation label on left axis
            canvas.nativeCanvas.drawText(
                "Rain",
                8.dp.toPx(),
                precipTop + precipBarHeight / 2f + legendPaint.textSize * 0.35f,
                legendPaint,
            )

            // Hour labels at bottom
            chart.hours.forEachIndexed { index, hour ->
                if ((hour - chart.hours.first()) % 3 != 0) return@forEachIndexed
                val labelCenterX = plotLeft + index * columnWidth + columnWidth / 2f
                canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%02d", hour),
                    labelCenterX,
                    precipBottom + hourLabelPaint.textSize + 6.dp.toPx(),
                    hourLabelPaint,
                )
            }

            // Percent labels inside cloud cells (clustered to avoid overlap)
            val labelWidth = percentPaint.measureText("99%")
            val labelCluster = kotlin.math.max(
                1,
                kotlin.math.ceil(labelWidth * 1.3f / columnWidth).toInt(),
            )

            chart.layers.forEachIndexed { idx, layer ->
                if (idx % labelCluster != labelCluster / 2) return@forEachIndexed
                val hourIndex = chart.hours.indexOf(layer.hour)
                if (hourIndex < 0) return@forEachIndexed

                val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f

                // High
                if (layer.highCloudPercent > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${layer.highCloudPercent.toInt()}%",
                        cx,
                        plotTop + layerHeight / 2f + percentPaint.textSize * 0.35f,
                        percentPaint,
                    )
                }
                // Mid
                if (layer.midCloudPercent > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${layer.midCloudPercent.toInt()}%",
                        cx,
                        plotTop + layerHeight + layerHeight / 2f + percentPaint.textSize * 0.35f,
                        percentPaint,
                    )
                }
                // Low
                if (layer.lowCloudPercent > 5f) {
                    canvas.nativeCanvas.drawText(
                        "${layer.lowCloudPercent.toInt()}%",
                        cx,
                        plotTop + 2 * layerHeight + layerHeight / 2f +
                            percentPaint.textSize * 0.35f,
                        percentPaint,
                    )
                }
            }

            // Precipitation labels
            chart.precipitation.forEachIndexed { idx, precip ->
                if (idx % labelCluster != labelCluster / 2) return@forEachIndexed
                val hourIndex = chart.hours.indexOf(precip.hour)
                if (hourIndex < 0) return@forEachIndexed

                val cx = plotLeft + hourIndex * columnWidth + columnWidth / 2f

                if (precip.probabilityPercent > 10f) {
                    canvas.nativeCanvas.drawText(
                        "${precip.probabilityPercent.toInt()}%",
                        cx,
                        precipTop + precipPaint.textSize + 2.dp.toPx(),
                        precipPaint,
                    )
                }
                if (precip.amountMm > 0.1f) {
                    canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "%.1f", precip.amountMm),
                        cx,
                        precipTop + precipPaint.textSize * 2f + 4.dp.toPx(),
                        precipPaint,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudCell(
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

private val CLOUD_HIGH_COLOR = Color(0xFFB0BEC5) // Light slate
private val CLOUD_MID_COLOR = Color(0xFF78909C)  // Medium slate
private val CLOUD_LOW_COLOR = Color(0xFF546E7A)   // Dark slate

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
