package com.cloudbasepredictor.ui.screens.forecast

import com.cloudbasepredictor.data.remote.HourlyForecastData
import com.cloudbasepredictor.data.remote.HourlyPoint
import com.cloudbasepredictor.domain.forecast.ProfileLevel
import com.cloudbasepredictor.domain.forecast.SurfaceHeatingInput
import com.cloudbasepredictor.domain.forecast.analyzeParcel
import com.cloudbasepredictor.domain.forecast.estimateSurfacePressure
import com.cloudbasepredictor.domain.forecast.satMixingRatioGKg

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
    val surfacePressure = hourPoint.surfacePressureHpa?.toFloat()
        ?: estimateSurfacePressure(hourlyData.elevation ?: 0.0)
    val surfaceHeightMeters = hourlyData.elevation?.toFloat()
        ?: pressureToApproxHeightMeters(surfacePressure).toFloat()
    val surfaceTemperatureC = hourPoint.temperature2mC?.toFloat()
        ?: return buildPlaceholderStuveChart(hour, dayIndex)
    val surfaceDewPointC = hourPoint.dewPoint2mC?.toFloat()
        ?: return buildPlaceholderStuveChart(hour, dayIndex)

    val temperatureProfile = buildList {
        add(
            StuveProfilePoint(
                pressureHpa = surfacePressure,
                temperatureC = surfaceTemperatureC,
                heightMeters = surfaceHeightMeters,
                isRealData = true,
            ),
        )
        pressureLevels
            .filter { it.pressureHpa.toFloat() < surfacePressure }
            .forEach { pl ->
                add(
                    StuveProfilePoint(
                        pressureHpa = pl.pressureHpa.toFloat(),
                        temperatureC = pl.temperatureC.toFloat(),
                        heightMeters = pl.geopotentialHeightM?.toFloat()
                            ?: pressureToApproxHeightMeters(pl.pressureHpa.toFloat()).toFloat(),
                        isRealData = true,
                    ),
                )
            }
    }

    if (temperatureProfile.isEmpty()) return buildPlaceholderStuveChart(hour, dayIndex)

    val dewpointProfile = buildList {
        add(
            StuveProfilePoint(
                pressureHpa = surfacePressure,
                temperatureC = surfaceDewPointC,
                heightMeters = surfaceHeightMeters,
                isRealData = true,
            ),
        )
        pressureLevels
            .filter { it.pressureHpa.toFloat() < surfacePressure }
            .forEach { pl ->
                pl.dewPointC?.let { dew ->
                    add(
                        StuveProfilePoint(
                            pressureHpa = pl.pressureHpa.toFloat(),
                            temperatureC = dew.toFloat(),
                            heightMeters = pl.geopotentialHeightM?.toFloat()
                                ?: pressureToApproxHeightMeters(pl.pressureHpa.toFloat()).toFloat(),
                            isRealData = true,
                        ),
                    )
                }
            }
    }

    val profileLevels = buildList {
        add(
            ProfileLevel(
                pressureHpa = surfacePressure,
                temperatureC = surfaceTemperatureC,
                dewPointC = surfaceDewPointC,
                heightKm = surfaceHeightMeters / 1000f,
            ),
        )
        addAll(
            pressureLevels
                .filter { it.pressureHpa.toFloat() < surfacePressure }
                .map { pressureLevel ->
                    ProfileLevel(
                        pressureHpa = pressureLevel.pressureHpa.toFloat(),
                        temperatureC = pressureLevel.temperatureC.toFloat(),
                        dewPointC = pressureLevel.dewPointC?.toFloat(),
                        heightKm = (
                            pressureLevel.geopotentialHeightM?.toFloat()
                                ?: pressureToApproxHeightMeters(pressureLevel.pressureHpa.toFloat()).toFloat()
                            ) / 1000f,
                    )
                },
        )
    }

    val heatingInput = SurfaceHeatingInput(
        hourOfDay = hourPoint.hour,
        shortwaveRadiationWm2 = hourPoint.shortwaveRadiationWm2?.toFloat(),
        cloudCoverLowPercent = hourPoint.cloudCoverLowPercent?.toFloat(),
        cloudCoverMidPercent = hourPoint.cloudCoverMidPercent?.toFloat(),
        cloudCoverHighPercent = hourPoint.cloudCoverHighPercent?.toFloat(),
        precipitationMm = hourPoint.precipitationMm?.toFloat(),
        isDay = hourPoint.isDay?.let { it > 0.5 },
    )
    val analysis = analyzeParcel(
        profile = profileLevels,
        surfaceTemperatureC = surfaceTemperatureC,
        surfaceDewPointC = surfaceDewPointC,
        surfacePressureHpa = surfacePressure,
        elevationKm = surfaceHeightMeters / 1000f,
        heatingInput = heatingInput,
        modelCapeJKg = hourPoint.capeJKg?.toFloat(),
    ) ?: return buildPlaceholderStuveChart(hour, dayIndex)

    val parcelPath = buildParcelAscentPath(
        pressures = buildRenderableParcelPressures(
            surfacePressureHpa = surfacePressure,
            profilePressures = profileLevels.map { it.pressureHpa },
        ),
        profile = profileLevels,
        surfaceTemperatureC = surfaceTemperatureC,
        surfaceDewPointC = surfaceDewPointC,
        surfacePressureHpa = surfacePressure,
        surfaceHeatingC = analysis.surfaceHeatingC,
    )

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
        lclPressureHpa = analysis.lclPressureHpa,
        cclPressureHpa = analysis.cclPressureHpa,
        tconC = analysis.tconC,
        moistureBands = buildMoistureBands(temperatureProfile, dewpointProfile),
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

    val timeSlots = daytimePoints.map { it.hour * 60 }

    val elevation = hourlyData.elevation ?: 0.0
    val elevationKm = elevation.toFloat() / 1000f

    val cells = mutableListOf<ThermicForecastCellUiModel>()
    val cloudMarkers = mutableListOf<ThermicForecastCloudMarkerUiModel>()
    val diagnostics = mutableListOf<ThermicSlotDiagnostics>()

    daytimePoints.forEach { hp ->
        val startMinute = hp.hour * 60
        val surfaceTemp = hp.temperature2mC?.toFloat() ?: return@forEach
        val surfaceDew = hp.dewPoint2mC?.toFloat() ?: return@forEach

        // Surface pressure: prefer model value, fall back to ISA estimate
        val surfacePressure = hp.surfacePressureHpa?.toFloat()
            ?: estimateSurfacePressure(elevation)

        // Build atmospheric profile from pressure levels
        val profile = hp.pressureLevels
            .filter { it.geopotentialHeightM != null }
            .map { pl ->
                ProfileLevel(
                    pressureHpa = pl.pressureHpa.toFloat(),
                    temperatureC = pl.temperatureC.toFloat(),
                    dewPointC = pl.dewPointC?.toFloat(),
                    heightKm = (pl.geopotentialHeightM!! / 1000.0).toFloat(),
                )
            }
            .sortedByDescending { it.pressureHpa }

        if (profile.size < 2) return@forEach

        val heatingInput = SurfaceHeatingInput(
            hourOfDay = hp.hour,
            shortwaveRadiationWm2 = hp.shortwaveRadiationWm2?.toFloat(),
            cloudCoverLowPercent = hp.cloudCoverLowPercent?.toFloat(),
            cloudCoverMidPercent = hp.cloudCoverMidPercent?.toFloat(),
            cloudCoverHighPercent = hp.cloudCoverHighPercent?.toFloat(),
            precipitationMm = hp.precipitationMm?.toFloat(),
            isDay = hp.isDay?.let { it > 0.5 },
        )

        val analysis = analyzeParcel(
            profile = profile,
            surfaceTemperatureC = surfaceTemp,
            surfaceDewPointC = surfaceDew,
            surfacePressureHpa = surfacePressure,
            elevationKm = elevationKm,
            heatingInput = heatingInput,
            modelCapeJKg = hp.capeJKg?.toFloat(),
        ) ?: return@forEach

        // Add thermal cells (usable thermals up to dry top)
        analysis.thermalCells.forEach { tc ->
            cells += ThermicForecastCellUiModel(
                startMinuteOfDayLocal = startMinute,
                startAltitudeKm = tc.startAltitudeKm,
                endAltitudeKm = tc.endAltitudeKm,
                strengthMps = tc.strengthMps,
            )
        }

        // Cloud markers from cloud base to moist equilibrium top
        val cloudBase = analysis.cloudBaseKm
        val cloudTop = analysis.moistEquilibriumTopKm ?: cloudBase
        if (cloudBase != null && cloudBase > elevationKm + 0.3f) {
            val top = cloudTop?.coerceAtLeast(cloudBase) ?: cloudBase
            val cloudRange = top - cloudBase
            val stepKm = if (cloudRange > 0.01f) {
                // Space markers ~0.3 km apart, at least 1 marker
                (cloudRange / ((cloudRange / 0.3f).toInt().coerceAtLeast(1))).coerceAtMost(0.3f)
            } else {
                0.3f
            }
            var alt = cloudBase
            while (alt <= top + 0.001f) {
                cloudMarkers += ThermicForecastCloudMarkerUiModel(
                    startMinuteOfDayLocal = startMinute,
                    altitudeKm = alt,
                )
                alt += stepKm
                if (stepKm < 0.01f) break
            }
        }

        diagnostics += ThermicSlotDiagnostics(
            startMinuteOfDayLocal = startMinute,
            dryThermalTopKm = analysis.dryThermalTopKm,
            cloudBaseKm = analysis.cloudBaseKm,
            moistEquilibriumTopKm = analysis.moistEquilibriumTopKm,
            modelCapeJKg = analysis.modelCapeJKg,
            computedCapeJKg = analysis.computedCapeJKg,
            computedCinJKg = analysis.computedCinJKg,
            lclKm = analysis.lclKm,
            cclKm = analysis.cclKm,
        )
    }

    // Drop negligible thermals (rounding artefacts)
    cells.removeAll { it.strengthMps < THERMIC_MIN_DISPLAY_STRENGTH_MPS }

    if (cells.isEmpty()) return ThermicForecastChartUiModel(
        timeSlots = timeSlots,
        cells = emptyList(),
        cloudMarkers = emptyList(),
        slotDiagnostics = diagnostics,
    )

    return ThermicForecastChartUiModel(
        timeSlots = timeSlots,
        cells = cells,
        cloudMarkers = cloudMarkers,
        slotDiagnostics = diagnostics,
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
            val rawHeightAsl = (pl.geopotentialHeightM ?: return@forEach).toFloat() / 1000f
            if (rawHeightAsl < elevationKm || rawHeightAsl > elevationKm + maxAltitudeKm) return@forEach
            val speed = pl.windSpeedKmh ?: return@forEach
            val dir = pl.windDirectionDeg ?: return@forEach
            // Round to nearest 50m to collapse per-hour geopotential height variations
            val heightAsl = kotlin.math.round(rawHeightAsl * 20f) / 20f
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
    val cclMarkers = daytimePoints.mapNotNull { hp ->
        val surfacePressure = hp.surfacePressureHpa?.toFloat()
            ?: estimateSurfacePressure(elevation)
        val surfDew = hp.dewPoint2mC?.toFloat() ?: return@mapNotNull null
        val surfMr = satMixingRatioGKg(surfDew, surfacePressure)
        val pls = hp.pressureLevels
            .filter { it.pressureHpa.toFloat() < surfacePressure }
            .sortedByDescending { it.pressureHpa }
        for (pl in pls) {
            val satMr = satMixingRatioGKg(pl.temperatureC.toFloat(), pl.pressureHpa.toFloat())
            if (satMr <= surfMr) {
                val heightAsl = (pl.geopotentialHeightM ?: continue).toFloat() / 1000f
                if (heightAsl > elevationKm) {
                    return@mapNotNull WindLevelMarker(hour = hp.hour, altitudeKm = heightAsl)
                }
            }
        }
        null
    }

    // Compute altitude bands: each data level extends halfway to its neighbors
    val sortedAlts = altitudeSet.toList()
    val altitudeBands = sortedAlts.mapIndexed { idx, alt ->
        val lowerDist = if (idx > 0) (alt - sortedAlts[idx - 1]) / 2f else 0.2f
        val upperDist = if (idx < sortedAlts.lastIndex) (sortedAlts[idx + 1] - alt) / 2f else 0.2f
        WindAltitudeBand(
            centerKm = alt,
            bottomKm = alt - lowerDist,
            topKm = alt + upperDist,
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

    val radiation = daytimePoints.map { hp ->
        CloudRadiationUiModel(
            hour = hp.hour,
            radiationWm2 = hp.shortwaveRadiationWm2?.toFloat() ?: 0f,
        )
    }

    val sunshine = daytimePoints.map { hp ->
        CloudSunshineUiModel(
            hour = hp.hour,
            durationS = hp.sunshineDurationS?.toFloat() ?: 0f,
        )
    }

    return CloudForecastChartUiModel(
        hours = hours,
        layers = layers,
        precipitation = precipitation,
        radiation = radiation,
        sunshine = sunshine,
    )
}
