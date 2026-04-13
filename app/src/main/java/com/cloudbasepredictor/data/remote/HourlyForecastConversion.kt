package com.cloudbasepredictor.data.remote

import com.cloudbasepredictor.model.DailyForecast
import kotlinx.serialization.Serializable

/**
 * Converts the hourly response into [HourlyForecastData] — an intermediate
 * domain model that the UI layer transforms into chart models.
 *
 * The conversion groups raw parallel arrays (one value per hourly time step)
 * into per-hour, per-pressure-level data points.
 */
fun OpenMeteoHourlyForecastResponse.toHourlyForecastData(): HourlyForecastData {
    val hourly = this.hourly
    val times = hourly.time  // ISO-8601 local, e.g. "2026-04-12T14:00"

    val pressureLevels = listOf(1000, 975, 950, 925, 900, 875, 850, 800, 750, 700, 650, 600, 550, 500)
    val tempByPressure = hourly.temperaturesByPressure().toMap()
    val dewByPressure = hourly.dewPointsByPressure().toMap()
    val windSpeedByPressure = hourly.windSpeedsByPressure().toMap()
    val windDirByPressure = hourly.windDirectionsByPressure().toMap()
    val geoHeightByPressure = hourly.geopotentialHeightsByPressure().toMap()

    val hourlyPoints = times.mapIndexed { i, isoTime ->
        val dateStr = isoTime.substringBefore("T")   // "2026-04-12"
        val hourStr = isoTime.substringAfter("T").substringBefore(":")
        val hour = hourStr.toIntOrNull() ?: 0

        val pressureLevelData = pressureLevels.mapNotNull { pHpa ->
            val temp = tempByPressure[pHpa]?.getOrNull(i)
            val dew = dewByPressure[pHpa]?.getOrNull(i)
            val ws = windSpeedByPressure[pHpa]?.getOrNull(i)
            val wd = windDirByPressure[pHpa]?.getOrNull(i)
            val gh = geoHeightByPressure[pHpa]?.getOrNull(i)
            if (temp == null) return@mapNotNull null
            PressureLevelPoint(
                pressureHpa = pHpa,
                temperatureC = temp,
                dewPointC = dew,
                windSpeedKmh = ws,
                windDirectionDeg = wd,
                geopotentialHeightM = gh,
            )
        }

        HourlyPoint(
            date = dateStr,
            hour = hour,
            temperature2mC = hourly.temperature2m?.getOrNull(i),
            dewPoint2mC = hourly.dewPoint2m?.getOrNull(i),
            cloudCoverLowPercent = hourly.cloudCoverLow?.getOrNull(i),
            cloudCoverMidPercent = hourly.cloudCoverMid?.getOrNull(i),
            cloudCoverHighPercent = hourly.cloudCoverHigh?.getOrNull(i),
            precipitationMm = hourly.precipitation?.getOrNull(i),
            precipitationProbabilityPercent = hourly.precipitationProbability?.getOrNull(i),
            windSpeed10mKmh = hourly.windSpeed10m?.getOrNull(i),
            windDirection10mDeg = hourly.windDirection10m?.getOrNull(i),
            capeJKg = hourly.cape?.getOrNull(i),
            freezingLevelHeightM = hourly.freezingLevelHeight?.getOrNull(i),
            pressureLevels = pressureLevelData,
        )
    }

    val dailyForecasts = daily?.let {
        OpenMeteoForecastResponse(daily = it).toDomainModels()
    } ?: emptyList()

    return HourlyForecastData(
        latitude = latitude,
        longitude = longitude,
        elevation = elevation,
        hourlyPoints = hourlyPoints,
        dailyForecasts = dailyForecasts,
    )
}

/**
 * Intermediate domain model holding all forecast data needed for chart construction.
 */
@Serializable
data class HourlyForecastData(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val hourlyPoints: List<HourlyPoint>,
    val dailyForecasts: List<DailyForecast>,
) {
    /** Group hourly points by their date string (yyyy-MM-dd). */
    fun pointsByDate(): Map<String, List<HourlyPoint>> =
        hourlyPoints.groupBy { it.date }
}

/**
 * One hour of forecast data combining surface and pressure-level variables.
 */
@Serializable
data class HourlyPoint(
    /** Date in yyyy-MM-dd format. */
    val date: String,
    /** Local hour (0–23). */
    val hour: Int,
    /** Surface temperature at 2 m, °C. */
    val temperature2mC: Double?,
    /** Surface dewpoint at 2 m, °C. */
    val dewPoint2mC: Double?,
    /** Low cloud cover (0–3 km AGL), percent. */
    val cloudCoverLowPercent: Double?,
    /** Mid cloud cover (3–8 km AGL), percent. */
    val cloudCoverMidPercent: Double?,
    /** High cloud cover (>8 km AGL), percent. */
    val cloudCoverHighPercent: Double?,
    /** Precipitation in preceding hour, mm. */
    val precipitationMm: Double?,
    /** Precipitation probability, percent. */
    val precipitationProbabilityPercent: Double?,
    /** Surface wind speed at 10 m, km/h. */
    val windSpeed10mKmh: Double?,
    /** Surface wind direction at 10 m, degrees (meteorological). */
    val windDirection10mDeg: Double?,
    /** Convective Available Potential Energy, J/kg. */
    val capeJKg: Double?,
    /** Freezing level (0 °C isotherm) height, metres above sea level. */
    val freezingLevelHeightM: Double?,
    /** Data at each requested pressure level. */
    val pressureLevels: List<PressureLevelPoint>,
)

/**
 * Atmospheric data at a single pressure level for one hour.
 */
@Serializable
data class PressureLevelPoint(
    /** Pressure level, hPa. */
    val pressureHpa: Int,
    /** Temperature, °C. */
    val temperatureC: Double,
    /** Dewpoint temperature, °C. Null if not available. */
    val dewPointC: Double?,
    /** Wind speed, km/h. */
    val windSpeedKmh: Double?,
    /** Wind direction, degrees (meteorological). */
    val windDirectionDeg: Double?,
    /** Geopotential height, metres above sea level. */
    val geopotentialHeightM: Double?,
)
