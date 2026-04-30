package com.cloudbasepredictor.domain.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalForecastEngineTest {

    private val heatingInput = SurfaceHeatingInput(
        hourOfDay = 13,
        shortwaveRadiationWm2 = 700f,
        cloudCoverLowPercent = 10f,
        cloudCoverMidPercent = 5f,
        cloudCoverHighPercent = 0f,
        precipitationMm = 0f,
        isDay = true,
    )

    @Test
    fun analyze_includesExplicitSurfaceLevelForLowLevelBuoyancy() {
        val result = analyze(profile = crossingProfileWithoutSurface())

        assertNotNull(result)
        result!!
        assertTrue("Surface-to-first-level layer should be present", result.layers.isNotEmpty())
        assertEquals(
            "Thermic layers should start at launch elevation, not at first pressure level",
            ELEVATION_KM,
            result.layers.first().startAltitudeKm,
            0.001f,
        )
        assertTrue("Top should be above launch elevation", result.topNominalKm > ELEVATION_KM)
    }

    @Test
    fun analyze_topRangeIncludesPressureLevelBracket() {
        val result = analyze(profile = crossingProfileWithoutSurface())

        assertNotNull(result)
        result!!
        val lower = requireNotNull(result.lowerSourceLevel)
        val upper = requireNotNull(result.upperSourceLevel)

        assertTrue("Nominal top should be inside its raw bracket", result.topNominalKm >= lower.altitudeKm - 0.001f)
        assertTrue("Nominal top should be inside its raw bracket", result.topNominalKm <= upper.altitudeKm + 0.001f)
        assertTrue("Low top should include the lower source level", result.topLowKm <= lower.altitudeKm + 0.001f)
        assertTrue("High top should include the upper source level", result.topHighKm >= upper.altitudeKm - 0.001f)
        assertTrue("Source pressures should be ordered by altitude", lower.pressureHpa > upper.pressureHpa)
    }

    @Test
    fun analyze_coarseVerticalSpacingWidensTopRangeAndLowersConfidence() {
        val dense = analyze(profile = denseCrossingProfile())
        val coarse = analyze(profile = coarseCrossingProfile())

        assertNotNull(dense)
        assertNotNull(coarse)
        dense!!
        coarse!!

        val denseWidthKm = dense.topHighKm - dense.topLowKm
        val coarseWidthKm = coarse.topHighKm - coarse.topLowKm
        assertTrue("Coarse pressure spacing should widen top uncertainty", coarseWidthKm > denseWidthKm)
        assertTrue(
            "Coarse pressure spacing should not have higher confidence than dense spacing",
            coarse.confidence.ordinal >= dense.confidence.ordinal,
        )
    }

    @Test
    fun analyze_modelCapeDoesNotChangeUpdraftSpeed() {
        val noCape = analyze(profile = crossingProfileWithoutSurface(), modelCapeJKg = 0f)
        val highCape = analyze(profile = crossingProfileWithoutSurface(), modelCapeJKg = 2500f)

        assertNotNull(noCape)
        assertNotNull(highCape)
        noCape!!
        highCape!!
        assertEquals(noCape.updraftLowMps, highCape.updraftLowMps, 0.0001f)
        assertEquals(noCape.updraftNominalMps, highCape.updraftNominalMps, 0.0001f)
        assertEquals(noCape.updraftHighMps, highCape.updraftHighMps, 0.0001f)
    }

    @Test
    fun analyze_precipitationCloudAndWindShearDampUpdraftAndConfidence() {
        val baseline = analyze(profile = crossingProfileWithoutSurface())
        val degraded = analyze(
            profile = crossingProfileWithoutSurface(windSpeedsKmh = listOf(5f, 12f, 45f, 55f, 60f)),
            heatingInput = heatingInput.copy(
                shortwaveRadiationWm2 = 160f,
                cloudCoverLowPercent = 90f,
                cloudCoverMidPercent = 70f,
                precipitationMm = 1f,
            ),
        )

        assertNotNull(baseline)
        assertNotNull(degraded)
        baseline!!
        degraded!!
        assertTrue("Degraded weather should lower updraft", degraded.updraftNominalMps < baseline.updraftNominalMps)
        assertTrue(
            "Degraded weather should not improve confidence",
            degraded.confidence.ordinal >= baseline.confidence.ordinal,
        )
        assertTrue(
            "Main limiter should report one of the degrading factors",
            degraded.limitingReason in setOf(
                ThermalLimitingReason.PRECIPITATION,
                ThermalLimitingReason.WEAK_RADIATION,
                ThermalLimitingReason.WIND_SHEAR,
            ),
        )
    }

    @Test
    fun analyze_missingNewOpenMeteoFieldsStillProducesLowerConfidenceForecast() {
        val result = analyze(
            profile = crossingProfileWithoutSurface(includeMoistureDiagnostics = false),
            boundaryLayerHeightM = null,
        )

        assertNotNull(result)
        result!!
        assertTrue("Forecast should still have a usable top", result.topNominalKm > ELEVATION_KM)
        assertTrue("Missing raw humidity/cloud/PBL fields should lower confidence", result.confidence.ordinal > 0)
    }

    private fun analyze(
        profile: List<ProfileLevel>,
        heatingInput: SurfaceHeatingInput = this.heatingInput,
        modelCapeJKg: Float? = 500f,
        boundaryLayerHeightM: Float? = 1600f,
    ): ThermalForecastResult? {
        return ThermalForecastEngine.analyze(
            ThermalForecastInput(
                profile = profile,
                surfaceTemperatureC = SURFACE_TEMPERATURE_C,
                surfaceDewPointC = SURFACE_DEW_POINT_C,
                surfacePressureHpa = SURFACE_PRESSURE_HPA,
                elevationKm = ELEVATION_KM,
                heatingInput = heatingInput,
                modelCapeJKg = modelCapeJKg,
                modelCinJKg = 20f,
                liftedIndexC = -1.5f,
                boundaryLayerHeightM = boundaryLayerHeightM,
            ),
        )
    }

    private fun crossingProfileWithoutSurface(
        includeMoistureDiagnostics: Boolean = true,
        windSpeedsKmh: List<Float> = listOf(8f, 12f, 16f, 20f, 24f),
    ): List<ProfileLevel> {
        return listOf(
            level(950f, 18f, 8f, 0.65f, includeMoistureDiagnostics, windSpeedsKmh[0]),
            level(900f, 17f, 7f, 1.00f, includeMoistureDiagnostics, windSpeedsKmh[1]),
            level(850f, 14f, 4f, 1.50f, includeMoistureDiagnostics, windSpeedsKmh[2]),
            level(800f, 18f, 2f, 2.00f, includeMoistureDiagnostics, windSpeedsKmh[3]),
            level(750f, 21f, -4f, 2.60f, includeMoistureDiagnostics, windSpeedsKmh[4]),
        )
    }

    private fun denseCrossingProfile(): List<ProfileLevel> {
        return listOf(
            level(950f, 18f, 8f, 0.65f),
            level(925f, 17f, 7f, 0.82f),
            level(900f, 16f, 6f, 1.00f),
            level(875f, 15f, 5f, 1.25f),
            level(850f, 14f, 4f, 1.50f),
            level(825f, 16f, 3f, 1.75f),
            level(800f, 18f, 2f, 2.00f),
            level(775f, 19f, 0f, 2.30f),
            level(750f, 21f, -4f, 2.60f),
        )
    }

    private fun coarseCrossingProfile(): List<ProfileLevel> {
        return listOf(
            level(950f, 18f, 8f, 0.65f),
            level(900f, 17f, 7f, 1.00f),
            level(800f, 18f, 2f, 2.00f),
            level(700f, 22f, -8f, 3.30f),
        )
    }

    private fun level(
        pressureHpa: Float,
        temperatureC: Float,
        dewPointC: Float,
        heightKm: Float,
        includeMoistureDiagnostics: Boolean = true,
        windSpeedKmh: Float = 12f,
    ): ProfileLevel {
        return ProfileLevel(
            pressureHpa = pressureHpa,
            temperatureC = temperatureC,
            dewPointC = dewPointC,
            heightKm = heightKm,
            relativeHumidityPercent = if (includeMoistureDiagnostics) 45f else null,
            cloudCoverPercent = if (includeMoistureDiagnostics) 5f else null,
            windSpeedKmh = windSpeedKmh,
            isSynthetic = false,
        )
    }

    private companion object {
        const val SURFACE_TEMPERATURE_C = 22f
        const val SURFACE_DEW_POINT_C = 10f
        const val SURFACE_PRESSURE_HPA = 955f
        const val ELEVATION_KM = 0.58f
    }
}
