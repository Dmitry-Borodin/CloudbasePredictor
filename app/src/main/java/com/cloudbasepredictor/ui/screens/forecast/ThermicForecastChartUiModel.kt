package com.cloudbasepredictor.ui.screens.forecast

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

data class ThermicForecastChartUiModel(
    val timeSlots: List<Int>,
    val cells: List<ThermicForecastCellUiModel>,
    val cloudMarkers: List<ThermicForecastCloudMarkerUiModel>,
)

data class ThermicForecastCellUiModel(
    val startMinuteOfDayLocal: Int,
    val startAltitudeKm: Float,
    val endAltitudeKm: Float,
    val strengthMps: Float,
)

data class ThermicForecastCloudMarkerUiModel(
    val startMinuteOfDayLocal: Int,
    val altitudeKm: Float,
)

internal fun buildPlaceholderThermicForecastChart(
    dayIndex: Int,
    timeSlots: List<Int> = THERMIC_FORECAST_TIME_SLOTS,
): ThermicForecastChartUiModel {
    val dayPhase = dayIndex * 0.33f
    val cells = buildList {
        timeSlots.forEach { startMinute ->
            val timeNormalized = (startMinute - timeSlots.first()).toFloat() /
                (timeSlots.last() - timeSlots.first()).toFloat()
            val hour = startMinute / MINUTES_PER_HOUR.toFloat()
            val solarPeak = (1f - (abs(hour - 14f) / 8f)).coerceIn(0f, 1f)
            val wave = ((sin((timeNormalized * PI.toFloat() * 2f) + dayPhase) + 1f) / 2f)
            val thermalBaseKm = 0f
            val thermalTopKm = (
                0.9f +
                    (solarPeak * 1.9f) +
                    (wave * 0.55f) +
                    ((((dayIndex * 3) + startMinute / FORECAST_TIME_SLOT_STEP_MINUTES) % 4) * 0.05f)
                ).coerceIn(0.6f, 3.6f)

            var currentAltitudeKm = thermalBaseKm
            while (currentAltitudeKm < thermalTopKm) {
                val nextAltitudeKm = (currentAltitudeKm + THERMIC_ALTITUDE_STEP_KM)
                    .coerceAtMost(thermalTopKm)
                val bandCenterKm = (currentAltitudeKm + nextAltitudeKm) / 2f
                val altitudePenalty = (bandCenterKm / thermalTopKm).coerceIn(0f, 1f)
                val rawStrength = (
                    0.35f +
                        (solarPeak * 1.8f) +
                        (wave * 0.65f) -
                        (altitudePenalty * 1.35f)
                    ).coerceIn(0f, MAX_THERMIC_STRENGTH_MPS)

                add(
                    ThermicForecastCellUiModel(
                        startMinuteOfDayLocal = startMinute,
                        startAltitudeKm = currentAltitudeKm,
                        endAltitudeKm = nextAltitudeKm,
                        strengthMps = roundDisplayedStrength(rawStrength),
                    ),
                )

                currentAltitudeKm = nextAltitudeKm
            }
        }
    }

    val cloudMarkers = buildList {
        timeSlots.forEach { startMinute ->
            val slotCells = cells.filter { it.startMinuteOfDayLocal == startMinute }
            val topCell = slotCells.maxByOrNull(ThermicForecastCellUiModel::endAltitudeKm) ?: return@forEach
            val markerCount = (((topCell.endAltitudeKm - 1.2f) / 0.55f).roundToInt() + 1).coerceIn(0, 5)
            repeat(markerCount) { index ->
                add(
                    ThermicForecastCloudMarkerUiModel(
                        startMinuteOfDayLocal = startMinute,
                        altitudeKm = topCell.endAltitudeKm + 0.35f + (index * 0.32f),
                    ),
                )
            }
        }
    }

    return ThermicForecastChartUiModel(
        timeSlots = timeSlots,
        cells = cells,
        cloudMarkers = cloudMarkers,
    )
}

internal fun ThermicForecastChartUiModel.aggregatedForDisplay(
    timeBucketSlotCount: Int,
    altitudeBucketStepKm: Float,
): ThermicForecastChartUiModel {
    if (timeSlots.isEmpty()) {
        return this
    }

    val slotCount = timeBucketSlotCount.coerceAtLeast(1)
    val baseAltitudeStepKm = cells.minOfOrNull { it.endAltitudeKm - it.startAltitudeKm }
        ?.coerceAtLeast(THERMIC_ALTITUDE_STEP_KM)
        ?: THERMIC_ALTITUDE_STEP_KM
    val altitudeStepMultiplier = maxOf(
        1,
        ceil(altitudeBucketStepKm / baseAltitudeStepKm).toInt(),
    )
    val resolvedAltitudeBucketStepKm = baseAltitudeStepKm * altitudeStepMultiplier

    if (slotCount == 1 && resolvedAltitudeBucketStepKm <= baseAltitudeStepKm + THERMIC_EPSILON) {
        return this
    }

    val groupedSlots = timeSlots.chunked(slotCount)
    val aggregatedCells = mutableListOf<ThermicForecastCellUiModel>()
    val aggregatedCloudMarkers = mutableListOf<ThermicForecastCloudMarkerUiModel>()

    groupedSlots.forEach { slotGroup ->
        val groupStartMinute = slotGroup.first()
        val slotCells = cells.filter { it.startMinuteOfDayLocal in slotGroup }
        val slotCloudMarkers = cloudMarkers.filter { it.startMinuteOfDayLocal in slotGroup }

        if (slotCells.isNotEmpty()) {
            val minimumAltitudeKm = slotCells.minOf(ThermicForecastCellUiModel::startAltitudeKm)
            val thermalTopKm = slotCells.maxOf(ThermicForecastCellUiModel::endAltitudeKm)
            var currentAltitudeKm = minimumAltitudeKm

            while (currentAltitudeKm < thermalTopKm - THERMIC_EPSILON) {
                val nextAltitudeKm = (currentAltitudeKm + resolvedAltitudeBucketStepKm)
                    .coerceAtMost(thermalTopKm)
                val overlappingCells = slotCells.filter { cell ->
                    intervalsOverlap(
                        firstStart = cell.startAltitudeKm,
                        firstEnd = cell.endAltitudeKm,
                        secondStart = currentAltitudeKm,
                        secondEnd = nextAltitudeKm,
                    )
                }

                if (overlappingCells.isNotEmpty()) {
                    val averagedStrength = overlappingCells.weightedAverageStrength(
                        bucketStartAltitudeKm = currentAltitudeKm,
                        bucketEndAltitudeKm = nextAltitudeKm,
                    )
                    aggregatedCells += ThermicForecastCellUiModel(
                        startMinuteOfDayLocal = groupStartMinute,
                        startAltitudeKm = currentAltitudeKm,
                        endAltitudeKm = nextAltitudeKm,
                        strengthMps = roundDisplayedStrength(averagedStrength),
                    )
                }

                currentAltitudeKm = nextAltitudeKm
            }
        }

        slotCloudMarkers
            .sortedBy(ThermicForecastCloudMarkerUiModel::altitudeKm)
            .distinctBy { marker -> marker.altitudeKm.roundToInt() }
            .forEach { marker ->
                aggregatedCloudMarkers += marker.copy(startMinuteOfDayLocal = groupStartMinute)
            }
    }

    return ThermicForecastChartUiModel(
        timeSlots = groupedSlots.map(List<Int>::first),
        cells = aggregatedCells,
        cloudMarkers = aggregatedCloudMarkers,
    )
}

private fun List<ThermicForecastCellUiModel>.weightedAverageStrength(
    bucketStartAltitudeKm: Float,
    bucketEndAltitudeKm: Float,
): Float {
    var weightedStrengthSum = 0f
    var weightSum = 0f

    forEach { cell ->
        val overlapStart = maxOf(cell.startAltitudeKm, bucketStartAltitudeKm)
        val overlapEnd = minOf(cell.endAltitudeKm, bucketEndAltitudeKm)
        val overlapHeight = (overlapEnd - overlapStart).coerceAtLeast(0f)

        if (overlapHeight > THERMIC_EPSILON) {
            weightedStrengthSum += cell.strengthMps * overlapHeight
            weightSum += overlapHeight
        }
    }

    return if (weightSum <= THERMIC_EPSILON) {
        0f
    } else {
        weightedStrengthSum / weightSum
    }
}

private fun intervalsOverlap(
    firstStart: Float,
    firstEnd: Float,
    secondStart: Float,
    secondEnd: Float,
): Boolean {
    return minOf(firstEnd, secondEnd) - maxOf(firstStart, secondStart) > THERMIC_EPSILON
}

private fun roundDisplayedStrength(value: Float): Float {
    return ((value * 10f).roundToInt() / 10f).coerceIn(0f, MAX_THERMIC_STRENGTH_MPS)
}

private const val FORECAST_TIME_SLOT_STEP_MINUTES = 15
private const val MINUTES_PER_HOUR = 60
private const val THERMIC_ALTITUDE_STEP_KM = 0.05f
private const val MAX_THERMIC_STRENGTH_MPS = 3f
private const val THERMIC_EPSILON = 0.0001f
private val THERMIC_FORECAST_TIME_SLOTS = buildList {
    var currentMinute = 6 * MINUTES_PER_HOUR
    val lastMinute = (22 * MINUTES_PER_HOUR) + 45
    while (currentMinute <= lastMinute) {
        add(currentMinute)
        currentMinute += FORECAST_TIME_SLOT_STEP_MINUTES
    }
}
