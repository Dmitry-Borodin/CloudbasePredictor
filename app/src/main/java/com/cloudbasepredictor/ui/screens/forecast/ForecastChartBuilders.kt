package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.remote.HourlyForecastData
import com.cloudbasepredictor.data.remote.HourlyPoint
import kotlin.math.pow

/**
 * Builds chart UI models from real [HourlyForecastData] returned by the backend.
 * Falls back to placeholder builders when hourly data is not available.
 */

// --- Stüve chart from real data ---

/** Parcel launch temperature is this many °C above the environmental T₂ₘ. */
private const val PARCEL_SURFACE_HEATING_C = 3f

internal fun buildStuveChartFromData(
    hourlyData: HourlyForecastData,
    dayIndex: Int,
    hour: Int,
): StuveForecastChartUiModel {
    val pointsByDate = hourlyData.pointsByDate()
    val dates = pointsByDate.keys.sorted()
    val dateKey = dates.getOrNull(dayIndex) ?: return buildPlaceholderStuveChart(hour, dayIndex)
    val dayPoints = pointsByDate[dateKey] ?: return buildPlaceholderStuveChart(hour, dayIndex)

    val hourPoint = dayPoints.firstOrNull { it.hour == hour }
        ?: dayPoints.minByOrNull { kotlin.math.abs(it.hour - hour) }
        ?: return buildPlaceholderStuveChart(hour, dayIndex)

    val pressureLevels = hourPoint.pressureLevels.sortedByDescending { it.pressureHpa }

    if (pressureLevels.isEmpty()) return buildPlaceholderStuveChart(hour, dayIndex)

    val temperatureProfile = pressureLevels.map { pl ->
        StuveProfilePoint(
            pressureHpa = pl.pressureHpa.toFloat(),
            temperatureC = pl.temperatureC.toFloat(),
            isRealData = true,
        )
    }

    val dewpointProfile = pressureLevels.mapNotNull { pl ->
        pl.dewPointC?.let { dew ->
            StuveProfilePoint(
                pressureHpa = pl.pressureHpa.toFloat(),
                temperatureC = dew.toFloat(),
                isRealData = true,
            )
        }
    }

    // Parcel ascent from surface — lifted 3 °C above current T₂ₘ (convective heating)
    val surfacePoint = pressureLevels.firstOrNull() ?: return buildPlaceholderStuveChart(hour, dayIndex)
    val surfaceTemp = (hourPoint.temperature2mC?.toFloat() ?: surfacePoint.temperatureC.toFloat()) + PARCEL_SURFACE_HEATING_C
    val surfaceDew = hourPoint.dewPoint2mC?.toFloat()
        ?: dewpointProfile.firstOrNull()?.temperatureC
        ?: (surfaceTemp - 8f)
    // Estimate surface pressure from station elevation (ISA)
    val surfacePressure = estimateSurfacePressureHpa(hourlyData.elevation ?: 0.0)
    val kappa = 0.286f
    val surfaceThetaK = (surfaceTemp + 273.15f) * (1000f / surfacePressure).pow(kappa)
    val surfaceMixingRatio = saturationMixingRatioGKg(surfaceDew, surfacePressure)

    var reachedLcl = false
    var lclPressure: Float? = null
    val parcelPath = STUVE_PRESSURE_LEVELS
        .filter { it <= surfacePressure }
        .map { p ->
            if (!reachedLcl) {
                val dryTemp = dryAdiabatTemperatureC(surfaceThetaK, p)
                val satMr = saturationMixingRatioGKg(dryTemp, p)
                if (satMr <= surfaceMixingRatio) {
                    reachedLcl = true
                    lclPressure = p
                    StuveProfilePoint(p, dryTemp)
                } else {
                    StuveProfilePoint(p, dryTemp)
                }
            } else {
                val thetaW = (surfaceTemp + 273.15f)
                val moistTemp = moistAdiabatTemperatureC(thetaW, p)
                StuveProfilePoint(p, moistTemp)
            }
        }

    val windBarbs = pressureLevels.mapNotNull { pl ->
        val speed = pl.windSpeedKmh ?: return@mapNotNull null
        val dir = pl.windDirectionDeg ?: return@mapNotNull null
        StuveWindBarb(
            pressureHpa = pl.pressureHpa.toFloat(),
            speedKmh = speed.toFloat(),
            directionDeg = dir.toFloat(),
        )
    }

    return StuveForecastChartUiModel(
        pressureLevels = STUVE_PRESSURE_LEVELS,
        temperatureProfile = temperatureProfile,
        dewpointProfile = dewpointProfile,
        parcelAscentPath = parcelPath,
        windBarbs = windBarbs,
        lclPressureHpa = lclPressure,
        selectedHour = hour,
    )
}

// --- Thermic chart from real data ---

internal fun buildThermicChartFromData(
    hourlyData: HourlyForecastData,
    dayIndex: Int,
): ThermicForecastChartUiModel {
    val pointsByDate = hourlyData.pointsByDate()
    val dates = pointsByDate.keys.sorted()
    val dateKey = dates.getOrNull(dayIndex) ?: return buildPlaceholderThermicForecastChart(dayIndex)
    val dayPoints = pointsByDate[dateKey] ?: return buildPlaceholderThermicForecastChart(dayIndex)

    // Filter to daytime hours (6:00–22:45)
    val daytimePoints = dayPoints.filter { it.hour in 6..22 }
    if (daytimePoints.isEmpty()) return buildPlaceholderThermicForecastChart(dayIndex)

    val minuteStep = 60 // one slot per hour from real data
    val timeSlots = daytimePoints.map { it.hour * 60 }

    val elevation = hourlyData.elevation ?: 0.0

    val cells = mutableListOf<ThermicForecastCellUiModel>()
    val cloudMarkers = mutableListOf<ThermicForecastCloudMarkerUiModel>()

    daytimePoints.forEach { hp ->
        val startMinute = hp.hour * 60
        val capeJKg = hp.capeJKg ?: 0.0
        val surfaceTemp = hp.temperature2mC ?: return@forEach
        val surfaceDew = hp.dewPoint2mC ?: return@forEach

        // Estimate thermal top from pressure level data
        val pressureLevels = hp.pressureLevels.sortedBy { it.pressureHpa }
        // Estimate surface pressure from station elevation (ISA)
        val surfacePressure = estimateSurfacePressureHpa(elevation)

        // Compute parcel temperature at each level
        val kappa = 0.286f
        val surfaceThetaK = (surfaceTemp.toFloat() + 273.15f) * (1000f / surfacePressure).pow(kappa)

        // Find thermal top: where parcel temp < environment temp
        var thermalTopKm = 0f
        for (pl in pressureLevels.reversed()) {
            val heightAgl = ((pl.geopotentialHeightM ?: 0.0) - elevation).toFloat() / 1000f
            if (heightAgl < 0f) continue
            val parcelTemp = dryAdiabatTemperatureC(surfaceThetaK, pl.pressureHpa.toFloat())
            if (parcelTemp < pl.temperatureC.toFloat()) {
                thermalTopKm = heightAgl.coerceAtLeast(0f)
                break
            }
            thermalTopKm = heightAgl
        }

        // Build thermal cells below the top
        val altitudeStepKm = 0.2f
        var currentAlt = 0f
        while (currentAlt < thermalTopKm) {
            val nextAlt = (currentAlt + altitudeStepKm).coerceAtMost(thermalTopKm)
            val bandCenter = (currentAlt + nextAlt) / 2f
            val altFraction = if (thermalTopKm > 0f) bandCenter / thermalTopKm else 0f
            // Thermal strength from CAPE: roughly sqrt(2*CAPE) gives max updraft
            val maxUpdraft = kotlin.math.sqrt(2.0 * capeJKg).toFloat().coerceAtMost(3f)
            val strength = (maxUpdraft * (1f - altFraction * 0.6f)).coerceIn(0f, 3f)

            cells += ThermicForecastCellUiModel(
                startMinuteOfDayLocal = startMinute,
                startAltitudeKm = currentAlt,
                endAltitudeKm = nextAlt,
                strengthMps = ((strength * 10f).toInt() / 10f).coerceIn(0f, 3f),
            )
            currentAlt = nextAlt
        }

        // Cloud marker at thermal top if we have enough lift
        if (thermalTopKm > 0.5f && capeJKg > 50.0) {
            cloudMarkers += ThermicForecastCloudMarkerUiModel(
                startMinuteOfDayLocal = startMinute,
                altitudeKm = thermalTopKm + 0.1f,
            )
        }
    }

    if (cells.isEmpty()) return buildPlaceholderThermicForecastChart(dayIndex)

    return ThermicForecastChartUiModel(
        timeSlots = timeSlots,
        cells = cells,
        cloudMarkers = cloudMarkers,
    )
}

// --- Wind chart from real data ---

internal fun buildWindChartFromData(
    hourlyData: HourlyForecastData,
    dayIndex: Int,
    maxAltitudeKm: Float,
): WindForecastChartUiModel {
    val pointsByDate = hourlyData.pointsByDate()
    val dates = pointsByDate.keys.sorted()
    val dateKey = dates.getOrNull(dayIndex)
        ?: return buildPlaceholderWindForecastChart(dayIndex, maxAltitudeKm = maxAltitudeKm)
    val dayPoints = pointsByDate[dateKey]
        ?: return buildPlaceholderWindForecastChart(dayIndex, maxAltitudeKm = maxAltitudeKm)

    val daytimePoints = dayPoints.filter { it.hour in 6..22 }
    if (daytimePoints.isEmpty()) {
        return buildPlaceholderWindForecastChart(dayIndex, maxAltitudeKm = maxAltitudeKm)
    }

    val elevation = hourlyData.elevation ?: 0.0
    val hours = daytimePoints.map { it.hour }

    // Collect all altitude bands from pressure level data
    val altitudeSet = sortedSetOf<Float>()
    val cellList = mutableListOf<WindForecastCellUiModel>()

    daytimePoints.forEach { hp ->
        // Surface wind
        val surfWindSpeed = hp.windSpeed10mKmh
        val surfWindDir = hp.windDirection10mDeg
        if (surfWindSpeed != null && surfWindDir != null) {
            val surfAlt = 0.01f // ~10m AGL
            altitudeSet.add(surfAlt)
            cellList += WindForecastCellUiModel(
                hour = hp.hour,
                altitudeKm = surfAlt,
                speedKmh = surfWindSpeed.toFloat(),
                directionDeg = surfWindDir.toFloat(),
            )
        }

        hp.pressureLevels.forEach { pl ->
            val heightAgl = ((pl.geopotentialHeightM ?: return@forEach) - elevation).toFloat() / 1000f
            if (heightAgl < 0f || heightAgl > maxAltitudeKm) return@forEach
            val speed = pl.windSpeedKmh ?: return@forEach
            val dir = pl.windDirectionDeg ?: return@forEach
            val roundedAlt = (heightAgl * 4f).toInt() / 4f // round to 0.25km
            altitudeSet.add(roundedAlt)
            cellList += WindForecastCellUiModel(
                hour = hp.hour,
                altitudeKm = roundedAlt,
                speedKmh = speed.toFloat(),
                directionDeg = dir.toFloat(),
            )
        }
    }

    if (cellList.isEmpty()) {
        return buildPlaceholderWindForecastChart(dayIndex, maxAltitudeKm = maxAltitudeKm)
    }

    return WindForecastChartUiModel(
        hours = hours,
        altitudeBandsKm = altitudeSet.toList(),
        cells = cellList,
    )
}

// --- Cloud chart from real data ---

internal fun buildCloudChartFromData(
    hourlyData: HourlyForecastData,
    dayIndex: Int,
): CloudForecastChartUiModel {
    val pointsByDate = hourlyData.pointsByDate()
    val dates = pointsByDate.keys.sorted()
    val dateKey = dates.getOrNull(dayIndex) ?: return buildPlaceholderCloudForecastChart(dayIndex)
    val dayPoints = pointsByDate[dateKey] ?: return buildPlaceholderCloudForecastChart(dayIndex)

    val daytimePoints = dayPoints.filter { it.hour in 6..22 }
    if (daytimePoints.isEmpty()) return buildPlaceholderCloudForecastChart(dayIndex)

    val hours = daytimePoints.map { it.hour }

    val layers = daytimePoints.map { hp ->
        CloudLayerUiModel(
            hour = hp.hour,
            lowCloudPercent = hp.cloudCoverLowPercent?.toFloat() ?: 0f,
            midCloudPercent = hp.cloudCoverMidPercent?.toFloat() ?: 0f,
            highCloudPercent = hp.cloudCoverHighPercent?.toFloat() ?: 0f,
        )
    }

    val precipitation = daytimePoints.map { hp ->
        CloudPrecipitationUiModel(
            hour = hp.hour,
            probabilityPercent = hp.precipitationProbabilityPercent?.toFloat() ?: 0f,
            amountMm = hp.precipitationMm?.toFloat() ?: 0f,
        )
    }

    return CloudForecastChartUiModel(
        hours = hours,
        layers = layers,
        precipitation = precipitation,
    )
}
