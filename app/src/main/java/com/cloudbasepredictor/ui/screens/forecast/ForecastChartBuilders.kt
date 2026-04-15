package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.remote.HourlyForecastData
import com.cloudbasepredictor.data.remote.HourlyPoint
import kotlin.math.pow

/**
 * Builds chart UI models from real [HourlyForecastData] returned by the backend.
 * Falls back to placeholder builders when hourly data is not available.
 */

private const val THERMIC_MIN_DISPLAY_STRENGTH_MPS = 0.2f

// --- Stüve chart from real data ---

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

    // Estimate surface pressure from station elevation (ISA)
    val surfacePressure = estimateSurfacePressureHpa(hourlyData.elevation ?: 0.0)

    // Build temperature profile: surface 2 m point + pressure levels above surface
    val temperatureProfile = buildList {
        hourPoint.temperature2mC?.let { t2m ->
            add(StuveProfilePoint(surfacePressure, t2m.toFloat(), isRealData = true))
        }
        pressureLevels
            .filter { it.pressureHpa.toFloat() < surfacePressure }
            .forEach { pl ->
                add(StuveProfilePoint(pl.pressureHpa.toFloat(), pl.temperatureC.toFloat(), isRealData = true))
            }
    }

    if (temperatureProfile.isEmpty()) return buildPlaceholderStuveChart(hour, dayIndex)

    // Dewpoint profile: surface 2 m point + pressure levels above surface
    val dewpointProfile = buildList {
        hourPoint.dewPoint2mC?.let { dew ->
            add(StuveProfilePoint(surfacePressure, dew.toFloat(), isRealData = true))
        }
        pressureLevels
            .filter { it.pressureHpa.toFloat() < surfacePressure }
            .forEach { pl ->
                pl.dewPointC?.let { dew ->
                    add(StuveProfilePoint(pl.pressureHpa.toFloat(), dew.toFloat(), isRealData = true))
                }
            }
    }

    // Parcel ascent from surface — lifted PARCEL_SURFACE_HEATING_C above current T₂ₘ
    val surfaceEnvTemp = hourPoint.temperature2mC?.toFloat()
        ?: temperatureProfile.firstOrNull()?.temperatureC
        ?: return buildPlaceholderStuveChart(hour, dayIndex)
    val surfaceTemp = surfaceEnvTemp + PARCEL_SURFACE_HEATING_C
    val surfaceDew = hourPoint.dewPoint2mC?.toFloat()
        ?: dewpointProfile.firstOrNull()?.temperatureC
        ?: (surfaceTemp - 8f)
    val kappa = 0.286f
    val surfaceThetaK = (surfaceTemp + 273.15f) * (1000f / surfacePressure).pow(kappa)
    val surfaceMixingRatio = saturationMixingRatioGKg(surfaceDew, surfacePressure)

    // Parcel pressures: start exactly at surface, then standard levels above
    val parcelPressures = buildList {
        add(surfacePressure)
        addAll(STUVE_PRESSURE_LEVELS.filter { it < surfacePressure })
    }

    var reachedLcl = false
    var lclPressure: Float? = null
    val parcelPath = parcelPressures.map { p ->
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
            val moistTemp = moistAdiabatTemperatureC(surfaceThetaK, p)
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
        surfacePressureHpa = surfacePressure,
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
    val elevationKm = elevation.toFloat() / 1000f

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
        var thermalTopKm = elevationKm
        for (pl in pressureLevels.reversed()) {
            val heightAsl = (pl.geopotentialHeightM ?: 0.0).toFloat() / 1000f
            if (heightAsl < elevationKm) continue
            val parcelTemp = dryAdiabatTemperatureC(surfaceThetaK, pl.pressureHpa.toFloat())
            if (parcelTemp < pl.temperatureC.toFloat()) {
                thermalTopKm = heightAsl.coerceAtLeast(elevationKm)
                break
            }
            thermalTopKm = heightAsl
        }

        // Build thermal cells below the top, using actual pressure level heights
        // for granularity that matches model data resolution.
        val levelHeightsKm = pressureLevels
            .mapNotNull { pl -> (pl.geopotentialHeightM ?: return@mapNotNull null).toFloat() / 1000f }
            .filter { it in elevationKm..thermalTopKm }
            .sorted()
            .toMutableList()

        // Ensure we have boundaries at elevation and thermal top
        if (levelHeightsKm.isEmpty() || levelHeightsKm.first() > elevationKm + 0.01f) {
            levelHeightsKm.add(0, elevationKm)
        }
        if (levelHeightsKm.last() < thermalTopKm - 0.01f) {
            levelHeightsKm.add(thermalTopKm)
        }

        for (i in 0 until levelHeightsKm.size - 1) {
            val currentAlt = levelHeightsKm[i]
            val nextAlt = levelHeightsKm[i + 1]
            if (nextAlt <= currentAlt + 0.001f) continue

            val bandCenter = (currentAlt + nextAlt) / 2f
            val altFraction = if (thermalTopKm > elevationKm) (bandCenter - elevationKm) / (thermalTopKm - elevationKm) else 0f
            // Thermal strength from CAPE: scaled sqrt(2*CAPE) for realistic thermal values
            val maxUpdraft = (kotlin.math.sqrt(2.0 * capeJKg).toFloat() * 0.15f).coerceAtMost(10f)
            val strength = (maxUpdraft * (1f - altFraction * 0.6f)).coerceIn(0f, 10f)

            cells += ThermicForecastCellUiModel(
                startMinuteOfDayLocal = startMinute,
                startAltitudeKm = currentAlt,
                endAltitudeKm = nextAlt,
                strengthMps = ((strength * 10f).toInt() / 10f).coerceIn(0f, 10f),
            )
        }

        // Cloud marker at thermal top if we have enough lift
        if (thermalTopKm > elevationKm + 0.5f && capeJKg > 50.0) {
            cloudMarkers += ThermicForecastCloudMarkerUiModel(
                startMinuteOfDayLocal = startMinute,
                altitudeKm = thermalTopKm + 0.1f,
            )
        }
    }

    // Drop negligible thermals (rounding artefacts)
    cells.removeAll { it.strengthMps < THERMIC_MIN_DISPLAY_STRENGTH_MPS }

    if (cells.isEmpty()) return ThermicForecastChartUiModel(
        timeSlots = timeSlots,
        cells = emptyList(),
        cloudMarkers = emptyList(),
    )

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
    val elevationKm = elevation.toFloat() / 1000f
    val hours = daytimePoints.map { it.hour }

    // Collect all altitude bands from pressure level data (ASL)
    val altitudeSet = sortedSetOf<Float>()
    val cellList = mutableListOf<WindForecastCellUiModel>()

    daytimePoints.forEach { hp ->
        // Surface wind
        val surfWindSpeed = hp.windSpeed10mKmh
        val surfWindDir = hp.windDirection10mDeg
        if (surfWindSpeed != null && surfWindDir != null) {
            val surfAlt = elevationKm + 0.01f // ~10m above terrain, ASL
            altitudeSet.add(surfAlt)
            cellList += WindForecastCellUiModel(
                hour = hp.hour,
                altitudeKm = surfAlt,
                speedKmh = surfWindSpeed.toFloat(),
                directionDeg = surfWindDir.toFloat(),
            )
        }

        hp.pressureLevels.forEach { pl ->
            val heightAsl = (pl.geopotentialHeightM ?: return@forEach).toFloat() / 1000f
            if (heightAsl < elevationKm || heightAsl > elevationKm + maxAltitudeKm) return@forEach
            val speed = pl.windSpeedKmh ?: return@forEach
            val dir = pl.windDirectionDeg ?: return@forEach
            altitudeSet.add(heightAsl)
            cellList += WindForecastCellUiModel(
                hour = hp.hour,
                altitudeKm = heightAsl,
                speedKmh = speed.toFloat(),
                directionDeg = dir.toFloat(),
            )
        }
    }

    if (cellList.isEmpty()) {
        return buildPlaceholderWindForecastChart(dayIndex, maxAltitudeKm = maxAltitudeKm)
    }

    // Freezing level (0 °C isotherm) — from API, ASL
    val freezingLevelMarkers = daytimePoints.mapNotNull { hp ->
        val flAsl = hp.freezingLevelHeightM ?: return@mapNotNull null
        val flAslKm = (flAsl / 1000.0).toFloat()
        if (flAslKm < elevationKm) return@mapNotNull null
        WindLevelMarker(hour = hp.hour, altitudeKm = flAslKm)
    }

    // CCL (Convective Condensation Level) — where surface mixing ratio meets env. temp, ASL
    val surfacePressure = estimateSurfacePressureHpa(elevation)
    val cclMarkers = daytimePoints.mapNotNull { hp ->
        val surfDew = hp.dewPoint2mC?.toFloat() ?: return@mapNotNull null
        val surfMr = saturationMixingRatioGKg(surfDew, surfacePressure)
        val pls = hp.pressureLevels
            .filter { it.pressureHpa.toFloat() < surfacePressure }
            .sortedByDescending { it.pressureHpa }
        for (pl in pls) {
            val satMr = saturationMixingRatioGKg(pl.temperatureC.toFloat(), pl.pressureHpa.toFloat())
            if (satMr <= surfMr) {
                val heightAsl = (pl.geopotentialHeightM ?: continue).toFloat() / 1000f
                if (heightAsl > elevationKm) {
                    return@mapNotNull WindLevelMarker(hour = hp.hour, altitudeKm = heightAsl)
                }
            }
        }
        null
    }

    // Compute altitude bands: each data level gets ±200m or half distance to neighbor
    val sortedAlts = altitudeSet.toList()
    val altitudeBands = sortedAlts.mapIndexed { idx, alt ->
        val lowerDist = if (idx > 0) (alt - sortedAlts[idx - 1]) / 2f else 0.2f
        val upperDist = if (idx < sortedAlts.lastIndex) (sortedAlts[idx + 1] - alt) / 2f else 0.2f
        WindAltitudeBand(
            centerKm = alt,
            bottomKm = alt - lowerDist.coerceAtMost(0.2f),
            topKm = alt + upperDist.coerceAtMost(0.2f),
        )
    }

    return WindForecastChartUiModel(
        hours = hours,
        altitudeBandsKm = sortedAlts,
        altitudeBands = altitudeBands,
        cells = cellList,
        freezingLevelKm = freezingLevelMarkers,
        cclKm = cclMarkers,
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
