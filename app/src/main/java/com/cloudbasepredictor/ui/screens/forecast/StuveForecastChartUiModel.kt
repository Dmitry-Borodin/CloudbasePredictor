package com.cloudbasepredictor.ui.screens.forecast

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin

/**
 * UI model for a Stüve thermodynamic diagram.
 *
 * Pressure on the Y-axis (exponential scale, 1050 hPa at bottom to ~200 hPa at top).
 * Temperature on the X-axis (°C). Dry adiabats, moist adiabats, and mixing ratio
 * reference lines are drawn as chart overlays.
 *
 * The model is designed so that the external data source builds it and the Canvas
 * view just renders it — no atmospheric math in the view layer.
 */
data class StuveForecastChartUiModel(
    val pressureLevels: List<Float>,
    val temperatureProfile: List<StuveProfilePoint>,
    val dewpointProfile: List<StuveProfilePoint>,
    val parcelAscentPath: List<StuveProfilePoint>,
    val windBarbs: List<StuveWindBarb>,
    val lclPressureHpa: Float?,
    val selectedHour: Int,
)

data class StuveProfilePoint(
    val pressureHpa: Float,
    val temperatureC: Float,
)

data class StuveWindBarb(
    val pressureHpa: Float,
    val speedKmh: Float,
    val directionDeg: Float,
)

// --- Standard pressure levels used in the diagram ---
val STUVE_PRESSURE_LEVELS: List<Float> = listOf(
    1050f, 1000f, 950f, 900f, 850f, 800f, 750f,
    700f, 650f, 600f, 550f, 500f, 450f, 400f,
    350f, 300f, 250f, 200f,
)

// Approximate height in meters for standard pressure levels (ISA)
private val PRESSURE_TO_HEIGHT_MAP: Map<Float, Int> = mapOf(
    1050f to -300, 1000f to 111, 950f to 540, 900f to 988,
    850f to 1457, 800f to 1949, 750f to 2466, 700f to 3013,
    650f to 3591, 600f to 4206, 550f to 4865, 500f to 5574,
    450f to 6344, 400f to 7185, 350f to 8117, 300f to 9164,
    250f to 10363, 200f to 11784,
)

fun pressureToApproxHeightMeters(pressureHpa: Float): Int {
    val sorted = PRESSURE_TO_HEIGHT_MAP.entries.sortedByDescending { it.key }
    val lower = sorted.lastOrNull { it.key >= pressureHpa }
    val upper = sorted.firstOrNull { it.key < pressureHpa }
    if (lower == null) return sorted.last().value
    if (upper == null) return sorted.first().value
    val frac = (lower.key - pressureHpa) / (lower.key - upper.key)
    return (lower.value + frac * (upper.value - lower.value)).toInt()
}

// --- Dry adiabat computation ---
// θ = T * (1000 / P)^(R/Cp), where R/Cp ≈ 0.286
private const val KAPPA = 0.286f

fun dryAdiabatTemperatureC(thetaK: Float, pressureHpa: Float): Float {
    return thetaK * (pressureHpa / 1000f).pow(KAPPA) - 273.15f
}

// --- Moist (saturated) adiabat computation (approximate) ---
fun moistAdiabatTemperatureC(thetaWK: Float, pressureHpa: Float): Float {
    // Simplified iterative pseudo-adiabatic lapse rate
    var tempK = thetaWK * (pressureHpa / 1000f).pow(KAPPA)
    repeat(3) {
        val es = saturationVaporPressureHpa(tempK - 273.15f)
        val ws = 0.622f * es / (pressureHpa - es)
        val lv = 2.501e6f - 2370f * (tempK - 273.15f)
        val correction = (lv * ws) / (1004f * tempK)
        tempK = thetaWK * (pressureHpa / 1000f).pow(KAPPA / (1f + correction))
    }
    return tempK - 273.15f
}

// --- Saturation mixing ratio ---
fun saturationMixingRatioGKg(temperatureC: Float, pressureHpa: Float): Float {
    val es = saturationVaporPressureHpa(temperatureC)
    return 622f * es / (pressureHpa - es)
}

fun mixingRatioTemperatureC(wGKg: Float, pressureHpa: Float): Float {
    // Invert: w = 622 * es / (P - es) → es = w*P / (622 + w)
    val es = wGKg * pressureHpa / (622f + wGKg)
    // Invert Tetens: es = 6.112 * exp(17.67*T / (T+243.5))
    val lnRatio = ln(es / 6.112f)
    return (243.5f * lnRatio) / (17.67f - lnRatio)
}

// Tetens formula for saturation vapor pressure
private fun saturationVaporPressureHpa(temperatureC: Float): Float {
    return 6.112f * exp(17.67f * temperatureC / (temperatureC + 243.5f))
}

// --- Reference line sets ---
val STUVE_DRY_ADIABAT_THETAS_K: List<Float> = listOf(
    253f, 263f, 273f, 283f, 293f, 303f, 313f, 323f, 333f, 343f, 353f, 363f, 373f,
)

val STUVE_MOIST_ADIABAT_THETAS_K: List<Float> = listOf(
    263f, 273f, 278f, 283f, 288f, 293f, 298f, 303f, 313f, 323f,
)

val STUVE_MIXING_RATIO_VALUES_GKG: List<Float> = listOf(
    0.4f, 1f, 2f, 3f, 5f, 8f, 12f, 16f, 20f, 28f,
)

// --- Placeholder builder ---
internal fun buildPlaceholderStuveChart(
    hour: Int = 12,
    dayIndex: Int = 0,
): StuveForecastChartUiModel {
    val dayPhase = dayIndex * 0.4f
    val solarFactor = (1f - abs(hour - 13f) / 8f).coerceIn(0.1f, 1f)

    // Generate a realistic-ish temperature sounding
    val temperatureProfile = STUVE_PRESSURE_LEVELS.map { p ->
        val heightKm = pressureToApproxHeightMeters(p) / 1000f
        // Standard atmosphere lapse rate ~6.5 °C/km with some perturbation
        val baseTemp = 25f - 6.5f * heightKm + solarFactor * 3f
        val perturbation = sin(heightKm * 1.5f + dayPhase) * 2f
        // Add a small inversion around 1.5 km
        val inversion = if (heightKm in 1.2f..1.8f) 2f * solarFactor else 0f
        StuveProfilePoint(
            pressureHpa = p,
            temperatureC = baseTemp + perturbation + inversion,
        )
    }

    // Dewpoint is typically 5-15°C below temperature
    val dewpointProfile = temperatureProfile.map { pt ->
        val heightKm = pressureToApproxHeightMeters(pt.pressureHpa) / 1000f
        val dewpointDepression = 8f + heightKm * 2.5f - solarFactor * 2f
        StuveProfilePoint(
            pressureHpa = pt.pressureHpa,
            temperatureC = pt.temperatureC - dewpointDepression.coerceAtLeast(3f),
        )
    }

    // Parcel ascent from surface: dry adiabat then moist
    val surfaceTemp = temperatureProfile.first().temperatureC
    val surfaceDewpoint = dewpointProfile.first().temperatureC
    val surfacePressure = temperatureProfile.first().pressureHpa
    val surfaceThetaK = (surfaceTemp + 273.15f) * (1000f / surfacePressure).pow(KAPPA)
    val surfaceMixingRatio = saturationMixingRatioGKg(surfaceDewpoint, surfacePressure)

    var reachedLcl = false
    var lclPressure: Float? = null
    val parcelPath = STUVE_PRESSURE_LEVELS.map { p ->
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
            // Simplified: moist adiabat from LCL
            val thetaW = (surfaceTemp + 273.15f)
            val moistTemp = moistAdiabatTemperatureC(thetaW, p)
            StuveProfilePoint(p, moistTemp)
        }
    }

    val windBarbs = STUVE_PRESSURE_LEVELS
        .filter { it <= 1000f && it >= 300f }
        .mapIndexed { index, p ->
            val heightKm = pressureToApproxHeightMeters(p) / 1000f
            StuveWindBarb(
                pressureHpa = p,
                speedKmh = 10f + heightKm * 8f + sin(heightKm + dayPhase) * 5f,
                directionDeg = 240f + sin(heightKm * 0.8f + dayPhase) * 60f,
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
