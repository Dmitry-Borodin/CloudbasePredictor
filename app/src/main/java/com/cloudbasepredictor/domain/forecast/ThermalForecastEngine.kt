package com.cloudbasepredictor.domain.forecast

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class ThermalForecastConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

enum class ThermalLimitingReason {
    SURFACE_HEATING,
    INVERSION,
    CLOUD_BASE,
    PROFILE_TOP,
    PRECIPITATION,
    WEAK_RADIATION,
    WIND_SHEAR,
    MISSING_DATA,
}

data class ThermalSourceLevel(
    val pressureHpa: Float,
    val altitudeKm: Float,
    val isSynthetic: Boolean,
)

data class ThermalForecastInput(
    val profile: List<ProfileLevel>,
    val surfaceTemperatureC: Float,
    val surfaceDewPointC: Float,
    val surfacePressureHpa: Float,
    val elevationKm: Float,
    val heatingInput: SurfaceHeatingInput,
    val modelCapeJKg: Float? = null,
    val modelCinJKg: Float? = null,
    val liftedIndexC: Float? = null,
    val boundaryLayerHeightM: Float? = null,
)

data class ThermalForecastResult(
    val topLowKm: Float,
    val topNominalKm: Float,
    val topHighKm: Float,
    val updraftLowMps: Float,
    val updraftNominalMps: Float,
    val updraftHighMps: Float,
    val confidence: ThermalForecastConfidence,
    val limitingReason: ThermalLimitingReason,
    val lowerSourceLevel: ThermalSourceLevel?,
    val upperSourceLevel: ThermalSourceLevel?,
    val lclKm: Float,
    val cclKm: Float,
    val cloudBaseKm: Float?,
    val moistEquilibriumTopKm: Float?,
    val thermalEnergyJKg: Float,
    val modelCapeJKg: Float?,
    val modelCinJKg: Float?,
    val liftedIndexC: Float?,
    val boundaryLayerHeightM: Float?,
    val layers: List<ThermalForecastLayer>,
    val pressureLevelAltitudesKm: List<Float>,
)

data class ThermalForecastLayer(
    val startAltitudeKm: Float,
    val endAltitudeKm: Float,
    val updraftLowMps: Float,
    val updraftNominalMps: Float,
    val updraftHighMps: Float,
    val confidence: ThermalForecastConfidence,
)

object ThermalForecastEngine {
    fun analyze(input: ThermalForecastInput): ThermalForecastResult? {
        val profile = buildProfileWithSurface(input)
        if (profile.size < 2) return null

        val baseHeatingC = estimateSurfaceHeating(input.heatingInput)
            .coerceIn(0f, MAX_NOMINAL_SURFACE_HEATING_C)
        val scenarios = listOf(
            ThermalScenario(HEATING_CONSERVATIVE, baseHeatingC * 0.55f),
            ThermalScenario(HEATING_NOMINAL, baseHeatingC),
            ThermalScenario(
                HEATING_OPTIMISTIC,
                (baseHeatingC * 1.2f + optimisticHeatingBonus(input.heatingInput))
                    .coerceIn(baseHeatingC, MAX_OPTIMISTIC_SURFACE_HEATING_C),
            ),
        )
        val scenarioResults = scenarios.associate { scenario ->
            scenario.name to analyzeTopScenario(input, profile, scenario.surfaceHeatingC)
        }
        val conservativeTop = scenarioResults.getValue(HEATING_CONSERVATIVE)
        val nominalTop = scenarioResults.getValue(HEATING_NOMINAL)
        val optimisticTop = scenarioResults.getValue(HEATING_OPTIMISTIC)

        val nominalThetaK = potentialTemperatureK(
            input.surfaceTemperatureC + scenarios[1].surfaceHeatingC,
            input.surfacePressureHpa,
        )
        val surfaceMixingRatio = satMixingRatioGKg(input.surfaceDewPointC, input.surfacePressureHpa)
        val lcl = findThermalLcl(nominalThetaK, surfaceMixingRatio, profile, input.elevationKm)
        val ccl = findThermalCcl(surfaceMixingRatio, profile, input.elevationKm)
        val rawCloudBaseKm = max(lcl.heightKm, ccl.heightKm)
        val cloudBaseKm = if (nominalTop.topKm >= rawCloudBaseKm - CLOUD_BASE_REACH_TOLERANCE_KM) {
            rawCloudBaseKm
        } else {
            null
        }
        val moistTopKm = cloudBaseKm?.let {
            findMoistTop(
                lclTemperatureC = dryAdiabatTempC(nominalThetaK, lcl.pressureHpa),
                lclPressureHpa = lcl.pressureHpa,
                profile = profile,
                cloudBaseKm = it,
            )
        }

        val damping = dampingFactor(input, profile, nominalTop.topKm)
        val layers = buildLayers(
            input = input,
            profile = profile,
            scenarios = scenarios,
            nominalTopKm = nominalTop.topKm,
            damping = damping,
        )
        val updraftLow = layers.maxOfOrNull { it.updraftLowMps } ?: 0f
        val updraftNominal = layers.maxOfOrNull { it.updraftNominalMps } ?: 0f
        val updraftHigh = layers.maxOfOrNull { it.updraftHighMps } ?: 0f
        val topLowKm = min(conservativeTop.topKm, nominalTop.bracketLower?.heightKm ?: nominalTop.topKm)
            .coerceAtLeast(input.elevationKm)
        val topHighKm = max(optimisticTop.topKm, nominalTop.bracketUpper?.heightKm ?: nominalTop.topKm)
            .coerceAtLeast(topLowKm)
        val confidence = resolveConfidence(
            profile = profile,
            nominalTop = nominalTop,
            damping = damping,
            input = input,
        )
        val limitingReason = resolveLimitingReason(
            input = input,
            nominalTop = nominalTop,
            cloudBaseKm = cloudBaseKm,
            damping = damping,
        )

        return ThermalForecastResult(
            topLowKm = topLowKm,
            topNominalKm = nominalTop.topKm,
            topHighKm = topHighKm,
            updraftLowMps = updraftLow,
            updraftNominalMps = updraftNominal,
            updraftHighMps = updraftHigh,
            confidence = confidence,
            limitingReason = limitingReason,
            lowerSourceLevel = nominalTop.bracketLower?.toSourceLevel(),
            upperSourceLevel = nominalTop.bracketUpper?.toSourceLevel(),
            lclKm = lcl.heightKm,
            cclKm = ccl.heightKm,
            cloudBaseKm = cloudBaseKm,
            moistEquilibriumTopKm = moistTopKm,
            thermalEnergyJKg = computeDryThermalEnergy(nominalThetaK, profile, nominalTop.topKm),
            modelCapeJKg = input.modelCapeJKg,
            modelCinJKg = input.modelCinJKg,
            liftedIndexC = input.liftedIndexC,
            boundaryLayerHeightM = input.boundaryLayerHeightM,
            layers = layers,
            pressureLevelAltitudesKm = profile
                .filter { !it.isSynthetic && it.heightKm >= input.elevationKm - HEIGHT_EPSILON_KM }
                .map { it.heightKm }
                .distinctBy { (it * 1000f).toInt() }
                .sorted(),
        )
    }

    private fun buildProfileWithSurface(input: ThermalForecastInput): List<ProfileLevel> {
        val hasSurface = input.profile.any { level ->
            abs(level.heightKm - input.elevationKm) <= SURFACE_MATCH_TOLERANCE_KM ||
                abs(level.pressureHpa - input.surfacePressureHpa) <= SURFACE_PRESSURE_TOLERANCE_HPA
        }
        val surfaceLevel = ProfileLevel(
            pressureHpa = input.surfacePressureHpa,
            temperatureC = input.surfaceTemperatureC,
            dewPointC = input.surfaceDewPointC,
            heightKm = input.elevationKm,
            windSpeedKmh = null,
            isSynthetic = false,
        )
        return (if (hasSurface) input.profile else listOf(surfaceLevel) + input.profile)
            .filter { it.heightKm >= input.elevationKm - HEIGHT_EPSILON_KM }
            .filter { it.pressureHpa <= input.surfacePressureHpa + SURFACE_PRESSURE_TOLERANCE_HPA }
            .distinctBy { (it.pressureHpa * 10f).toInt() }
            .sortedByDescending(ProfileLevel::pressureHpa)
    }

    private fun analyzeTopScenario(
        input: ThermalForecastInput,
        profile: List<ProfileLevel>,
        surfaceHeatingC: Float,
    ): ThermalTopScenarioResult {
        val thetaK = potentialTemperatureK(input.surfaceTemperatureC + surfaceHeatingC, input.surfacePressureHpa)
        var previous = profile.first()
        var previousDiff = dryAdiabatTempC(thetaK, previous.pressureHpa) - previous.temperatureC
        if (previousDiff <= MIN_TOP_BUOYANCY_C) {
            return ThermalTopScenarioResult(
                topKm = input.elevationKm,
                bracketLower = previous,
                bracketUpper = profile.getOrNull(1),
                profileTopLimited = false,
            )
        }
        for (index in 1 until profile.size) {
            val current = profile[index]
            val currentDiff = dryAdiabatTempC(thetaK, current.pressureHpa) - current.temperatureC
            if (currentDiff <= MIN_TOP_BUOYANCY_C) {
                val fraction = (if (previousDiff - currentDiff > 0.001f) {
                    previousDiff / (previousDiff - currentDiff)
                } else {
                    0.5f
                }).coerceIn(0f, 1f)
                return ThermalTopScenarioResult(
                    topKm = previous.heightKm + fraction * (current.heightKm - previous.heightKm),
                    bracketLower = previous,
                    bracketUpper = current,
                    profileTopLimited = false,
                )
            }
            previous = current
            previousDiff = currentDiff
        }
        return ThermalTopScenarioResult(
            topKm = profile.last().heightKm,
            bracketLower = profile.getOrNull(profile.lastIndex - 1),
            bracketUpper = profile.last(),
            profileTopLimited = true,
        )
    }

    private fun buildLayers(
        input: ThermalForecastInput,
        profile: List<ProfileLevel>,
        scenarios: List<ThermalScenario>,
        nominalTopKm: Float,
        damping: ThermalDamping,
    ): List<ThermalForecastLayer> {
        if (nominalTopKm <= input.elevationKm + HEIGHT_EPSILON_KM) return emptyList()
        return buildList {
            for (index in 0 until profile.size - 1) {
                val lower = profile[index]
                val upper = profile[index + 1]
                if (upper.heightKm <= input.elevationKm) continue
                if (lower.heightKm >= nominalTopKm) break

                val startKm = max(lower.heightKm, input.elevationKm)
                val endKm = min(upper.heightKm, nominalTopKm)
                if (endKm <= startKm + HEIGHT_EPSILON_KM) continue

                val updrafts = scenarios.map { scenario ->
                    layerUpdraftMps(
                        input = input,
                        lower = lower,
                        upper = upper,
                        startKm = startKm,
                        endKm = endKm,
                        surfaceHeatingC = scenario.surfaceHeatingC,
                        dampingFactor = damping.factor,
                    )
                }
                val low = updrafts.minOrNull() ?: 0f
                val nominal = updrafts.getOrNull(1) ?: low
                val high = updrafts.maxOrNull() ?: nominal
                if (high >= MIN_DISPLAY_UPDRAFT_MPS) {
                    add(
                        ThermalForecastLayer(
                            startAltitudeKm = startKm,
                            endAltitudeKm = endKm,
                            updraftLowMps = roundUpdraft(low),
                            updraftNominalMps = roundUpdraft(nominal),
                            updraftHighMps = roundUpdraft(high),
                            confidence = if (lower.isSynthetic || upper.isSynthetic) {
                                ThermalForecastConfidence.MEDIUM
                            } else {
                                ThermalForecastConfidence.HIGH
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun layerUpdraftMps(
        input: ThermalForecastInput,
        lower: ProfileLevel,
        upper: ProfileLevel,
        startKm: Float,
        endKm: Float,
        surfaceHeatingC: Float,
        dampingFactor: Float,
    ): Float {
        val thetaK = potentialTemperatureK(input.surfaceTemperatureC + surfaceHeatingC, input.surfacePressureHpa)
        val midPressure = (lower.pressureHpa + upper.pressureHpa) / 2f
        val envTempC = (lower.temperatureC + upper.temperatureC) / 2f
        val parcelTempC = dryAdiabatTempC(thetaK, midPressure)
        val buoyancyC = (parcelTempC - envTempC).coerceAtLeast(0f)
        if (buoyancyC < MIN_LAYER_BUOYANCY_C) return 0f

        val dzMeters = (endKm - startKm) * 1000f
        val buoyancyAccel = GRAVITY_MPS2 * buoyancyC / (envTempC + KELVIN_OFFSET)
        val raw = sqrt(max(0f, 2f * buoyancyAccel * dzMeters)) * UPDRAFT_SCALE * dampingFactor
        return raw.coerceIn(0f, MAX_UPDRAFT_MPS)
    }

    private fun computeDryThermalEnergy(
        thetaK: Float,
        profile: List<ProfileLevel>,
        topKm: Float,
    ): Float {
        var energy = 0f
        for (index in 0 until profile.size - 1) {
            val lower = profile[index]
            val upper = profile[index + 1]
            if (lower.heightKm >= topKm) break
            val endKm = min(upper.heightKm, topKm)
            val dz = (endKm - lower.heightKm) * 1000f
            if (dz <= 0f) continue
            val midPressure = (lower.pressureHpa + upper.pressureHpa) / 2f
            val envTempC = (lower.temperatureC + upper.temperatureC) / 2f
            val parcelTempC = dryAdiabatTempC(thetaK, midPressure)
            val buoyancy = GRAVITY_MPS2 * (parcelTempC - envTempC) / (envTempC + KELVIN_OFFSET) * dz
            if (buoyancy > 0f) energy += buoyancy
        }
        return energy
    }

    private fun dampingFactor(
        input: ThermalForecastInput,
        profile: List<ProfileLevel>,
        topKm: Float,
    ): ThermalDamping {
        val radiation = input.heatingInput.shortwaveRadiationWm2
        val radiationFactor = when {
            radiation == null -> 0.85f
            radiation < 150f -> 0.55f
            radiation < 300f -> 0.75f
            else -> 1f
        }
        val cloudPenalty = (
            ((input.heatingInput.cloudCoverLowPercent ?: 0f) / 100f) * 0.45f +
                ((input.heatingInput.cloudCoverMidPercent ?: 0f) / 100f) * 0.25f +
                ((input.heatingInput.cloudCoverHighPercent ?: 0f) / 100f) * 0.10f
            ).coerceIn(0f, 0.55f)
        val profileCloudPenalty = profile
            .filter { it.heightKm <= topKm + 0.2f }
            .mapNotNull(ProfileLevel::cloudCoverPercent)
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
            ?.let { (it / 100f * 0.25f).coerceIn(0f, 0.35f) }
            ?: 0f
        val precipFactor = if ((input.heatingInput.precipitationMm ?: 0f) > 0.1f) 0.55f else 1f
        val shearKmh = lowLevelWindShearKmh(profile, topKm)
        val shearFactor = when {
            shearKmh == null -> 1f
            shearKmh > 40f -> 0.65f
            shearKmh > 25f -> 0.8f
            else -> 1f
        }
        val factor = radiationFactor *
            (1f - cloudPenalty) *
            (1f - profileCloudPenalty) *
            precipFactor *
            shearFactor
        return ThermalDamping(
            factor = factor.coerceIn(0.25f, 1f),
            weakRadiation = radiation != null && radiation < 150f,
            precipitation = (input.heatingInput.precipitationMm ?: 0f) > 0.1f,
            strongWindShear = shearKmh != null && shearKmh > 25f,
        )
    }

    private fun lowLevelWindShearKmh(profile: List<ProfileLevel>, topKm: Float): Float? {
        val samples = profile
            .filter { it.heightKm <= topKm.coerceAtMost(profile.first().heightKm + 2f) + 0.01f }
            .mapNotNull(ProfileLevel::windSpeedKmh)
        if (samples.size < 2) return null
        return (samples.maxOrNull() ?: 0f) - (samples.minOrNull() ?: 0f)
    }

    private fun resolveConfidence(
        profile: List<ProfileLevel>,
        nominalTop: ThermalTopScenarioResult,
        damping: ThermalDamping,
        input: ThermalForecastInput,
    ): ThermalForecastConfidence {
        var score = 4
        val bracketDepthKm = nominalTop.bracketDepthKm
        if (bracketDepthKm > 0.5f) score -= 1
        if (bracketDepthKm > 1.0f) score -= 1
        if (nominalTop.profileTopLimited) score -= 1
        if (nominalTop.bracketLower?.isSynthetic == true || nominalTop.bracketUpper?.isSynthetic == true) score -= 1
        if (!hasHumidityOrCloudNearTop(profile, nominalTop.topKm)) score -= 2
        if (damping.precipitation) score -= 1
        if (damping.weakRadiation) score -= 1
        if (damping.strongWindShear) score -= 1
        if (input.boundaryLayerHeightM == null) score -= 1

        return when {
            score >= 3 -> ThermalForecastConfidence.HIGH
            score >= 1 -> ThermalForecastConfidence.MEDIUM
            else -> ThermalForecastConfidence.LOW
        }
    }

    private fun hasHumidityOrCloudNearTop(profile: List<ProfileLevel>, topKm: Float): Boolean {
        return profile
            .filter { abs(it.heightKm - topKm) <= TOP_MOISTURE_CONTEXT_KM }
            .any { it.relativeHumidityPercent != null || it.cloudCoverPercent != null }
    }

    private fun resolveLimitingReason(
        input: ThermalForecastInput,
        nominalTop: ThermalTopScenarioResult,
        cloudBaseKm: Float?,
        damping: ThermalDamping,
    ): ThermalLimitingReason {
        return when {
            input.heatingInput.isDay == false -> ThermalLimitingReason.SURFACE_HEATING
            damping.precipitation -> ThermalLimitingReason.PRECIPITATION
            damping.weakRadiation -> ThermalLimitingReason.WEAK_RADIATION
            nominalTop.profileTopLimited -> ThermalLimitingReason.PROFILE_TOP
            nominalTop.topKm <= input.elevationKm + SHALLOW_THERMAL_DEPTH_KM -> ThermalLimitingReason.INVERSION
            cloudBaseKm != null && cloudBaseKm <= nominalTop.topKm + CLOUD_BASE_REACH_TOLERANCE_KM ->
                ThermalLimitingReason.CLOUD_BASE
            damping.strongWindShear -> ThermalLimitingReason.WIND_SHEAR
            else -> ThermalLimitingReason.SURFACE_HEATING
        }
    }

    private fun findThermalLcl(
        parcelThetaK: Float,
        surfaceMixingRatio: Float,
        profile: List<ProfileLevel>,
        elevationKm: Float,
    ): PressureHeight {
        var previous: ProfileLevel? = null
        for (level in profile) {
            val dryTemp = dryAdiabatTempC(parcelThetaK, level.pressureHpa)
            val saturationMixingRatio = satMixingRatioGKg(dryTemp, level.pressureHpa)
            if (saturationMixingRatio <= surfaceMixingRatio) {
                previous?.let { prev ->
                    val previousDryTemp = dryAdiabatTempC(parcelThetaK, prev.pressureHpa)
                    val previousMixingRatio = satMixingRatioGKg(previousDryTemp, prev.pressureHpa)
                    val fraction = (if (previousMixingRatio - saturationMixingRatio > 0.001f) {
                        (previousMixingRatio - surfaceMixingRatio) /
                            (previousMixingRatio - saturationMixingRatio)
                    } else {
                        0.5f
                    }).coerceIn(0f, 1f)
                    return PressureHeight(
                        pressureHpa = prev.pressureHpa + fraction * (level.pressureHpa - prev.pressureHpa),
                        heightKm = (prev.heightKm + fraction * (level.heightKm - prev.heightKm))
                            .coerceAtLeast(elevationKm),
                    )
                }
                return PressureHeight(level.pressureHpa, level.heightKm.coerceAtLeast(elevationKm))
            }
            previous = level
        }
        val top = profile.last()
        return PressureHeight(top.pressureHpa, top.heightKm)
    }

    private fun findThermalCcl(
        surfaceMixingRatio: Float,
        profile: List<ProfileLevel>,
        elevationKm: Float,
    ): PressureHeight {
        var previous: ProfileLevel? = null
        for (level in profile) {
            val envSaturationMixingRatio = satMixingRatioGKg(level.temperatureC, level.pressureHpa)
            if (envSaturationMixingRatio <= surfaceMixingRatio) {
                previous?.let { prev ->
                    val prevMixingRatio = satMixingRatioGKg(prev.temperatureC, prev.pressureHpa)
                    val fraction = (if (prevMixingRatio - envSaturationMixingRatio > 0.001f) {
                        (prevMixingRatio - surfaceMixingRatio) /
                            (prevMixingRatio - envSaturationMixingRatio)
                    } else {
                        0.5f
                    }).coerceIn(0f, 1f)
                    return PressureHeight(
                        pressureHpa = prev.pressureHpa + fraction * (level.pressureHpa - prev.pressureHpa),
                        heightKm = (prev.heightKm + fraction * (level.heightKm - prev.heightKm))
                            .coerceAtLeast(elevationKm),
                    )
                }
                return PressureHeight(level.pressureHpa, level.heightKm.coerceAtLeast(elevationKm))
            }
            previous = level
        }
        val top = profile.last()
        return PressureHeight(top.pressureHpa, top.heightKm)
    }

    private fun findMoistTop(
        lclTemperatureC: Float,
        lclPressureHpa: Float,
        profile: List<ProfileLevel>,
        cloudBaseKm: Float,
    ): Float? {
        val aboveCloud = profile.filter { it.heightKm >= cloudBaseKm - HEIGHT_EPSILON_KM }
        if (aboveCloud.size < 2) return null
        var foundBuoyancy = false
        var previous: ProfileLevel? = null
        for (level in aboveCloud) {
            val moistTemp = moistAdiabatTempFromPointC(lclTemperatureC, lclPressureHpa, level.pressureHpa)
            if (moistTemp > level.temperatureC) {
                foundBuoyancy = true
            } else if (foundBuoyancy) {
                previous?.let { prev ->
                    val previousMoistTemp = moistAdiabatTempFromPointC(
                        lclTemperatureC,
                        lclPressureHpa,
                        prev.pressureHpa,
                    )
                    val prevDiff = previousMoistTemp - prev.temperatureC
                    val currentDiff = moistTemp - level.temperatureC
                    val fraction = (if (prevDiff - currentDiff > 0.001f) {
                        prevDiff / (prevDiff - currentDiff)
                    } else {
                        0.5f
                    }).coerceIn(0f, 1f)
                    return prev.heightKm + fraction * (level.heightKm - prev.heightKm)
                }
                return level.heightKm
            }
            previous = level
        }
        return if (foundBuoyancy) aboveCloud.last().heightKm else null
    }

    private fun optimisticHeatingBonus(input: SurfaceHeatingInput): Float {
        val radiation = input.shortwaveRadiationWm2 ?: return 0f
        val lowCloud = input.cloudCoverLowPercent ?: 0f
        return if (radiation >= 550f && lowCloud <= 25f && (input.precipitationMm ?: 0f) <= 0.1f) {
            0.5f
        } else {
            0f
        }
    }

    private fun ProfileLevel.toSourceLevel(): ThermalSourceLevel {
        return ThermalSourceLevel(
            pressureHpa = pressureHpa,
            altitudeKm = heightKm,
            isSynthetic = isSynthetic,
        )
    }

    private fun roundUpdraft(value: Float): Float {
        return ((value * 10f).toInt() / 10f).coerceIn(0f, MAX_UPDRAFT_MPS)
    }

    private data class ThermalScenario(
        val name: String,
        val surfaceHeatingC: Float,
    )

    private data class ThermalTopScenarioResult(
        val topKm: Float,
        val bracketLower: ProfileLevel?,
        val bracketUpper: ProfileLevel?,
        val profileTopLimited: Boolean,
    ) {
        val bracketDepthKm: Float
            get() = if (bracketLower != null && bracketUpper != null) {
                abs(bracketUpper.heightKm - bracketLower.heightKm)
            } else {
                0f
            }
    }

    private data class ThermalDamping(
        val factor: Float,
        val weakRadiation: Boolean,
        val precipitation: Boolean,
        val strongWindShear: Boolean,
    )

    private data class PressureHeight(
        val pressureHpa: Float,
        val heightKm: Float,
    )

    private const val HEATING_CONSERVATIVE = "conservative"
    private const val HEATING_NOMINAL = "nominal"
    private const val HEATING_OPTIMISTIC = "optimistic"
    private const val GRAVITY_MPS2 = 9.81f
    private const val KELVIN_OFFSET = 273.15f
    private const val UPDRAFT_SCALE = 0.55f
    private const val MAX_UPDRAFT_MPS = 10f
    private const val MIN_DISPLAY_UPDRAFT_MPS = 0.2f
    private const val MIN_LAYER_BUOYANCY_C = 0.25f
    private const val MIN_TOP_BUOYANCY_C = 0f
    private const val MAX_NOMINAL_SURFACE_HEATING_C = 6f
    private const val MAX_OPTIMISTIC_SURFACE_HEATING_C = 7f
    private const val HEIGHT_EPSILON_KM = 0.001f
    private const val SURFACE_MATCH_TOLERANCE_KM = 0.03f
    private const val SURFACE_PRESSURE_TOLERANCE_HPA = 1f
    private const val CLOUD_BASE_REACH_TOLERANCE_KM = 0.05f
    private const val SHALLOW_THERMAL_DEPTH_KM = 0.2f
    private const val TOP_MOISTURE_CONTEXT_KM = 0.75f
}
