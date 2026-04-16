package com.cloudbasepredictor.domain.forecast

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Result of a full parcel analysis for one time slot.
 *
 * All altitudes are in km ASL (above sea level).
 * Pure data class — no Android / UI dependencies.
 */
data class ParcelAnalysisResult(
    /** Altitude where the dry adiabat parcel cools below the environmental profile, km ASL. */
    val dryThermalTopKm: Float,
    /** Lifting Condensation Level: where surface parcel reaches saturation on dry adiabat, km ASL. */
    val lclKm: Float,
    /** Convective Condensation Level: where surface mixing ratio meets env. temperature, km ASL. */
    val cclKm: Float,
    /**
     * Cloud base altitude for cumulus formation, km ASL.
     * Equals max(LCL, CCL) only if the parcel can actually reach it (dry top >= cloud base).
     * Null if cumulus formation is not expected.
     */
    val cloudBaseKm: Float?,
    /** Equilibrium level of the moist (saturated) parcel, km ASL. Null if no moist convection. */
    val moistEquilibriumTopKm: Float?,
    /** CAPE computed from the environmental profile and parcel ascent (J/kg). */
    val computedCapeJKg: Float,
    /** CIN computed from the environmental profile and parcel ascent (J/kg, positive value = inhibition). */
    val computedCinJKg: Float,
    /** Model-supplied CAPE from the weather model (J/kg). Null if unavailable. */
    val modelCapeJKg: Float?,
    /** Local buoyancy-based thermal strength per altitude band below dry top, m/s. */
    val thermalCells: List<ThermalCell>,
    /** Surface heating estimate used for the parcel launch temperature, °C above T2m. */
    val surfaceHeatingC: Float,
)

/**
 * Thermal updraft strength in a single altitude band.
 */
data class ThermalCell(
    /** Bottom of band, km ASL. */
    val startAltitudeKm: Float,
    /** Top of band, km ASL. */
    val endAltitudeKm: Float,
    /** Estimated updraft strength, m/s. */
    val strengthMps: Float,
    /** Local buoyancy (parcel T - env T), °C. Positive = buoyant. */
    val buoyancyC: Float,
)

/**
 * One level in the atmospheric profile used for parcel analysis.
 */
data class ProfileLevel(
    val pressureHpa: Float,
    val temperatureC: Float,
    val dewPointC: Float?,
    val heightKm: Float,
)

/**
 * Input parameters for surface heating estimation.
 */
data class SurfaceHeatingInput(
    val hourOfDay: Int,
    val shortwaveRadiationWm2: Float?,
    val cloudCoverLowPercent: Float?,
    val cloudCoverMidPercent: Float?,
    val cloudCoverHighPercent: Float?,
    val precipitationMm: Float?,
    val isDay: Boolean?,
)

// ────────────────────────────────────────────────────────────────────
// Thermodynamic constants
// ────────────────────────────────────────────────────────────────────

/** Poisson constant R_d / C_pd for dry air. */
private const val KAPPA = 0.286f

/** Gravity, m/s². */
private const val G = 9.81f

/** Specific gas constant for dry air, J/(kg·K). */
private const val RD = 287f

/** Minimum parcel buoyancy (°C) to count as a usable thermal. */
private const val MIN_BUOYANCY_C = 0.3f

/** Cap for surface heating estimate, °C. */
private const val MAX_SURFACE_HEATING_C = 8f

/** Default conservative surface heating when no radiation data available, °C. */
private const val DEFAULT_SURFACE_HEATING_C = 2f

/** Minimum surface heating during daytime, °C. */
private const val MIN_DAYTIME_SURFACE_HEATING_C = 0.5f

/**
 * Maximum reference shortwave radiation for scaling, W/m².
 * A very sunny summer midday ~ 800–1000 W/m².
 */
private const val REFERENCE_RADIATION_WM2 = 900f

/** Scaling factor to convert local buoyancy (K) into updraft m/s. */
private const val BUOYANCY_TO_UPDRAFT_SCALE = 0.75f

/** Maximum thermal strength cap, m/s. */
private const val MAX_THERMAL_STRENGTH_MPS = 10f

/** Minimum displayable thermal strength, m/s. */
private const val MIN_DISPLAY_STRENGTH_MPS = 0.2f

// ────────────────────────────────────────────────────────────────────
// Public API
// ────────────────────────────────────────────────────────────────────

/**
 * Performs a full parcel analysis for one time slot.
 *
 * @param profile Environmental profile, sorted by decreasing pressure (surface first).
 *                Must have at least 2 levels with heights above [elevationKm].
 * @param surfaceTemperatureC Surface temperature at 2 m, °C.
 * @param surfaceDewPointC Surface dewpoint at 2 m, °C.
 * @param surfacePressureHpa Surface pressure, hPa.
 * @param elevationKm Station elevation, km ASL.
 * @param heatingInput Parameters for surface heating estimation.
 * @param modelCapeJKg Model-supplied CAPE (J/kg), if available.
 * @return Analysis result, or null if the profile is insufficient.
 */
fun analyzeParcel(
    profile: List<ProfileLevel>,
    surfaceTemperatureC: Float,
    surfaceDewPointC: Float,
    surfacePressureHpa: Float,
    elevationKm: Float,
    heatingInput: SurfaceHeatingInput,
    modelCapeJKg: Float? = null,
): ParcelAnalysisResult? {
    // Filter to levels above surface and sort descending pressure (low altitude first)
    val aboveSurface = profile
        .filter { it.heightKm >= elevationKm - 0.01f && it.pressureHpa < surfacePressureHpa + 1f }
        .sortedByDescending { it.pressureHpa }

    if (aboveSurface.size < 2) return null

    // Surface heating
    val surfaceHeatingC = estimateSurfaceHeating(heatingInput)
    val parcelTempC = surfaceTemperatureC + surfaceHeatingC
    val parcelThetaK = potentialTemperatureK(parcelTempC, surfacePressureHpa)
    val surfaceMixingRatio = satMixingRatioGKg(surfaceDewPointC, surfacePressureHpa)

    // ── Find LCL: where dry adiabat parcel reaches saturation ──
    val lclResult = findLcl(parcelThetaK, surfaceMixingRatio, surfacePressureHpa, aboveSurface, elevationKm)

    // ── Find CCL: where surface mixing ratio meets environmental temperature ──
    val cclResult = findCcl(surfaceMixingRatio, aboveSurface, elevationKm)

    // ── Find dry thermal top: where dry adiabat parcel T < environment T ──
    val dryTopKm = findDryThermalTop(parcelThetaK, aboveSurface, elevationKm)

    // ── Cloud base determination ──
    val rawCloudBaseKm = maxOf(lclResult.heightKm, cclResult)
    val cloudBaseKm = if (dryTopKm >= rawCloudBaseKm - 0.05f) rawCloudBaseKm else null

    // ── Moist ascent above LCL / cloud base ──
    var moistEquilibriumTopKm: Float? = null
    if (cloudBaseKm != null) {
        moistEquilibriumTopKm = findMoistEquilibriumTop(
            parcelThetaK, lclResult.pressureHpa, aboveSurface, cloudBaseKm,
        )
    }

    // ── CAPE / CIN computation ──
    val capeCin = computeCapeCin(parcelThetaK, surfaceMixingRatio, surfacePressureHpa, aboveSurface)

    // ── Build thermal cells from local buoyancy ──
    val thermalCells = buildThermalCells(
        parcelThetaK = parcelThetaK,
        surfaceMixingRatio = surfaceMixingRatio,
        lclPressureHpa = lclResult.pressureHpa,
        profile = aboveSurface,
        elevationKm = elevationKm,
        dryTopKm = dryTopKm,
        modelCapeJKg = modelCapeJKg,
        computedCapeJKg = capeCin.first,
    )

    return ParcelAnalysisResult(
        dryThermalTopKm = dryTopKm,
        lclKm = lclResult.heightKm,
        cclKm = cclResult,
        cloudBaseKm = cloudBaseKm,
        moistEquilibriumTopKm = moistEquilibriumTopKm,
        computedCapeJKg = capeCin.first,
        computedCinJKg = capeCin.second,
        modelCapeJKg = modelCapeJKg,
        thermalCells = thermalCells,
        surfaceHeatingC = surfaceHeatingC,
    )
}

// ────────────────────────────────────────────────────────────────────
// Surface heating estimation
// ────────────────────────────────────────────────────────────────────

/**
 * Estimates convective surface heating above T2m based on radiation, clouds,
 * precipitation and time of day. Returns °C offset.
 */
fun estimateSurfaceHeating(input: SurfaceHeatingInput): Float {
    // Night time — no convective heating
    if (input.isDay == false || input.hourOfDay < 6 || input.hourOfDay > 20) {
        return 0f
    }

    // If we have shortwave radiation, use it as primary driver
    val radiationFraction = input.shortwaveRadiationWm2?.let {
        (it / REFERENCE_RADIATION_WM2).coerceIn(0f, 1.2f)
    }

    // Cloud penalty
    val cloudPenalty = run {
        val low = (input.cloudCoverLowPercent ?: 0f) / 100f
        val mid = (input.cloudCoverMidPercent ?: 0f) / 100f
        val high = (input.cloudCoverHighPercent ?: 0f) / 100f
        // Low clouds block most radiation, high clouds less
        (low * 0.7f + mid * 0.4f + high * 0.15f).coerceIn(0f, 0.85f)
    }

    // Precipitation suppresses thermals
    val precipPenalty = if ((input.precipitationMm ?: 0f) > 0.1f) 0.6f else 0f

    // Solar elevation proxy from hour of day
    val solarFactor = solarElevationFactor(input.hourOfDay)

    val baseHeating = if (radiationFraction != null) {
        // Radiation-driven: up to ~MAX_SURFACE_HEATING_C at peak radiation
        radiationFraction * MAX_SURFACE_HEATING_C * solarFactor
    } else {
        // No radiation data: use conservative default with solar curve
        DEFAULT_SURFACE_HEATING_C * solarFactor * (1f - cloudPenalty)
    }

    val heating = baseHeating * (1f - cloudPenalty) * (1f - precipPenalty)
    return heating.coerceIn(MIN_DAYTIME_SURFACE_HEATING_C, MAX_SURFACE_HEATING_C)
}

/** Simple solar elevation factor peaking at 13:00 local. */
internal fun solarElevationFactor(hourOfDay: Int): Float {
    val dist = kotlin.math.abs(hourOfDay - 13f)
    return (1f - dist / 8f).coerceIn(0f, 1f)
}

// ────────────────────────────────────────────────────────────────────
// Thermodynamic helpers (pure functions)
// ────────────────────────────────────────────────────────────────────

/** Potential temperature θ (K) from temperature (°C) and pressure (hPa). */
fun potentialTemperatureK(temperatureC: Float, pressureHpa: Float): Float {
    return (temperatureC + 273.15f) * (1000f / pressureHpa).pow(KAPPA)
}

/** Temperature (°C) from potential temperature θ (K) at a given pressure. */
fun dryAdiabatTempC(thetaK: Float, pressureHpa: Float): Float {
    return thetaK * (pressureHpa / 1000f).pow(KAPPA) - 273.15f
}

/** Saturation mixing ratio (g/kg) at given temperature and pressure. */
fun satMixingRatioGKg(temperatureC: Float, pressureHpa: Float): Float {
    val es = satVaporPressureHpa(temperatureC)
    val denom = pressureHpa - es
    return if (denom > 0.01f) 622f * es / denom else 622f * es / 0.01f
}

/** Moist (pseudo-adiabatic) temperature at given pressure, starting from θ at LCL. */
fun moistAdiabatTempC(thetaWK: Float, pressureHpa: Float): Float {
    var tempK = thetaWK * (pressureHpa / 1000f).pow(KAPPA)
    repeat(4) {
        val es = satVaporPressureHpa(tempK - 273.15f)
        val ws = 0.622f * es / (pressureHpa - es).coerceAtLeast(0.01f)
        val lv = 2.501e6f - 2370f * (tempK - 273.15f)
        val correction = (lv * ws) / (1004f * tempK)
        tempK = thetaWK * (pressureHpa / 1000f).pow(KAPPA / (1f + correction))
    }
    return tempK - 273.15f
}

/** Tetens formula: saturation vapor pressure (hPa) from temperature (°C). */
fun satVaporPressureHpa(temperatureC: Float): Float {
    return 6.112f * exp(17.67f * temperatureC / (temperatureC + 243.5f))
}

/** Estimate surface pressure (hPa) from elevation (m) using ISA barometric formula. */
fun estimateSurfacePressure(elevationM: Double): Float {
    return (1013.25 * (1.0 - 0.0065 * elevationM / 288.15).pow(5.2561)).toFloat()
}

// ────────────────────────────────────────────────────────────────────
// Internal analysis steps
// ────────────────────────────────────────────────────────────────────

private data class LclResult(val pressureHpa: Float, val heightKm: Float)

/**
 * Finds the Lifting Condensation Level by ascending the dry adiabat until
 * the parcel's saturation mixing ratio drops to the surface mixing ratio.
 */
private fun findLcl(
    parcelThetaK: Float,
    surfaceMixingRatio: Float,
    surfacePressureHpa: Float,
    profile: List<ProfileLevel>,
    elevationKm: Float,
): LclResult {
    var prevLevel: ProfileLevel? = null
    for (level in profile) {
        val dryTemp = dryAdiabatTempC(parcelThetaK, level.pressureHpa)
        val satMr = satMixingRatioGKg(dryTemp, level.pressureHpa)
        if (satMr <= surfaceMixingRatio) {
            // Interpolate between previous and current level
            if (prevLevel != null) {
                val prevDryTemp = dryAdiabatTempC(parcelThetaK, prevLevel.pressureHpa)
                val prevSatMr = satMixingRatioGKg(prevDryTemp, prevLevel.pressureHpa)
                val frac = if ((prevSatMr - satMr) > 0.001f) {
                    (prevSatMr - surfaceMixingRatio) / (prevSatMr - satMr)
                } else 0.5f
                val interpHeight = prevLevel.heightKm + frac * (level.heightKm - prevLevel.heightKm)
                val interpPressure = prevLevel.pressureHpa + frac * (level.pressureHpa - prevLevel.pressureHpa)
                return LclResult(interpPressure, interpHeight.coerceAtLeast(elevationKm))
            }
            return LclResult(level.pressureHpa, level.heightKm.coerceAtLeast(elevationKm))
        }
        prevLevel = level
    }
    // LCL above entire profile — use top level
    val top = profile.last()
    return LclResult(top.pressureHpa, top.heightKm)
}

/**
 * Finds the Convective Condensation Level: the altitude where the surface
 * mixing ratio line intersects the environmental temperature profile.
 */
private fun findCcl(
    surfaceMixingRatio: Float,
    profile: List<ProfileLevel>,
    elevationKm: Float,
): Float {
    var prevLevel: ProfileLevel? = null
    for (level in profile) {
        val envSatMr = satMixingRatioGKg(level.temperatureC, level.pressureHpa)
        if (envSatMr <= surfaceMixingRatio) {
            if (prevLevel != null) {
                val prevEnvSatMr = satMixingRatioGKg(prevLevel.temperatureC, prevLevel.pressureHpa)
                val frac = if ((prevEnvSatMr - envSatMr) > 0.001f) {
                    (prevEnvSatMr - surfaceMixingRatio) / (prevEnvSatMr - envSatMr)
                } else 0.5f
                return (prevLevel.heightKm + frac * (level.heightKm - prevLevel.heightKm))
                    .coerceAtLeast(elevationKm)
            }
            return level.heightKm.coerceAtLeast(elevationKm)
        }
        prevLevel = level
    }
    return profile.lastOrNull()?.heightKm ?: elevationKm
}

/**
 * Finds the dry thermal top: the altitude where the dry-adiabat parcel
 * temperature falls below the environmental temperature.
 */
private fun findDryThermalTop(
    parcelThetaK: Float,
    profile: List<ProfileLevel>,
    elevationKm: Float,
): Float {
    var prevLevel: ProfileLevel? = null
    for (level in profile) {
        val parcelTemp = dryAdiabatTempC(parcelThetaK, level.pressureHpa)
        if (parcelTemp < level.temperatureC) {
            if (prevLevel != null) {
                val prevParcelTemp = dryAdiabatTempC(parcelThetaK, prevLevel.pressureHpa)
                val prevDiff = prevParcelTemp - prevLevel.temperatureC
                val currDiff = parcelTemp - level.temperatureC
                val frac = if ((prevDiff - currDiff) > 0.001f) {
                    prevDiff / (prevDiff - currDiff)
                } else 0.5f
                return (prevLevel.heightKm + frac * (level.heightKm - prevLevel.heightKm))
                    .coerceAtLeast(elevationKm)
            }
            return level.heightKm.coerceAtLeast(elevationKm)
        }
        prevLevel = level
    }
    // Parcel still buoyant at the top of profile
    return profile.lastOrNull()?.heightKm ?: elevationKm
}

/**
 * Finds the moist equilibrium top: where the moist-adiabat parcel temperature
 * falls below the environmental temperature above cloud base.
 */
private fun findMoistEquilibriumTop(
    parcelThetaK: Float,
    lclPressureHpa: Float,
    profile: List<ProfileLevel>,
    cloudBaseKm: Float,
): Float? {
    val aboveCloudBase = profile.filter { it.heightKm >= cloudBaseKm - 0.01f }
    if (aboveCloudBase.size < 2) return null

    var foundBuoyant = false
    var prevLevel: ProfileLevel? = null
    for (level in aboveCloudBase) {
        val moistTemp = moistAdiabatTempC(parcelThetaK, level.pressureHpa)
        if (moistTemp > level.temperatureC) {
            foundBuoyant = true
        } else if (foundBuoyant) {
            // Equilibrium level found
            if (prevLevel != null) {
                val prevMoistTemp = moistAdiabatTempC(parcelThetaK, prevLevel.pressureHpa)
                val prevDiff = prevMoistTemp - prevLevel.temperatureC
                val currDiff = moistTemp - level.temperatureC
                val frac = if ((prevDiff - currDiff) > 0.001f) {
                    prevDiff / (prevDiff - currDiff)
                } else 0.5f
                return prevLevel.heightKm + frac * (level.heightKm - prevLevel.heightKm)
            }
            return level.heightKm
        }
        prevLevel = level
    }

    // If buoyant all the way to top, return top
    return if (foundBuoyant) profile.last().heightKm else null
}

/**
 * Computes CAPE and CIN from the parcel ascent relative to the environmental profile.
 *
 * @return Pair(capeJKg, cinJKg) where CIN is a positive value representing inhibition.
 */
private fun computeCapeCin(
    parcelThetaK: Float,
    surfaceMixingRatio: Float,
    surfacePressureHpa: Float,
    profile: List<ProfileLevel>,
): Pair<Float, Float> {
    if (profile.size < 2) return 0f to 0f

    var cape = 0f
    var cin = 0f
    var reachedLcl = false

    for (i in 0 until profile.size - 1) {
        val lower = profile[i]
        val upper = profile[i + 1]
        val dz = (upper.heightKm - lower.heightKm) * 1000f // meters
        if (dz <= 0f) continue

        // Parcel temperature at midpoint pressure
        val midPressure = (lower.pressureHpa + upper.pressureHpa) / 2f
        val envTempMid = (lower.temperatureC + upper.temperatureC) / 2f

        val parcelTemp = if (!reachedLcl) {
            val dryTemp = dryAdiabatTempC(parcelThetaK, midPressure)
            val satMr = satMixingRatioGKg(dryTemp, midPressure)
            if (satMr <= surfaceMixingRatio) {
                reachedLcl = true
                moistAdiabatTempC(parcelThetaK, midPressure)
            } else {
                dryTemp
            }
        } else {
            moistAdiabatTempC(parcelThetaK, midPressure)
        }

        val envTempK = envTempMid + 273.15f
        val buoyancy = G * (parcelTemp - envTempMid) / envTempK * dz

        if (buoyancy > 0) {
            cape += buoyancy
        } else {
            cin -= buoyancy // CIN as positive value
        }
    }

    return cape to cin
}

/**
 * Builds thermal updraft strength cells from local buoyancy between profile levels,
 * up to the dry thermal top.
 */
private fun buildThermalCells(
    parcelThetaK: Float,
    surfaceMixingRatio: Float,
    lclPressureHpa: Float,
    profile: List<ProfileLevel>,
    elevationKm: Float,
    dryTopKm: Float,
    modelCapeJKg: Float?,
    computedCapeJKg: Float,
): List<ThermalCell> {
    val cells = mutableListOf<ThermalCell>()
    if (profile.size < 2) return cells

    // Calibration factor from model CAPE comparison (0.5 .. 1.5 range)
    val calibrationFactor = if (modelCapeJKg != null && computedCapeJKg > 10f) {
        val ratio = modelCapeJKg / computedCapeJKg
        ratio.coerceIn(0.5f, 1.5f)
    } else {
        1f
    }

    for (i in 0 until profile.size - 1) {
        val lower = profile[i]
        val upper = profile[i + 1]
        if (upper.heightKm <= elevationKm) continue
        if (lower.heightKm >= dryTopKm) break

        val cellBottom = max(lower.heightKm, elevationKm)
        val cellTop = min(upper.heightKm, dryTopKm)
        if (cellTop <= cellBottom + 0.001f) continue

        // Parcel temperature at midpoint
        val midPressure = (lower.pressureHpa + upper.pressureHpa) / 2f
        val parcelTemp = dryAdiabatTempC(parcelThetaK, midPressure)
        val envTemp = (lower.temperatureC + upper.temperatureC) / 2f
        val buoyancyC = parcelTemp - envTemp

        if (buoyancyC < MIN_BUOYANCY_C) continue

        // Convert buoyancy to updraft velocity using simple w ~ sqrt(2 * B * dz)
        // where B = g * ΔT/T_env
        val dz = (cellTop - cellBottom) * 1000f
        val envTempK = envTemp + 273.15f
        val buoyancyAccel = G * buoyancyC / envTempK
        // Local updraft estimate using integrated buoyancy
        val rawStrength = sqrt(max(0f, 2f * buoyancyAccel * dz)) * BUOYANCY_TO_UPDRAFT_SCALE * calibrationFactor
        val strength = rawStrength.coerceIn(0f, MAX_THERMAL_STRENGTH_MPS)

        if (strength >= MIN_DISPLAY_STRENGTH_MPS) {
            cells += ThermalCell(
                startAltitudeKm = cellBottom,
                endAltitudeKm = cellTop,
                strengthMps = ((strength * 10f).toInt() / 10f),
                buoyancyC = buoyancyC,
            )
        }
    }

    return cells
}
