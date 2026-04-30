package com.cloudbasepredictor.domain.forecast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelAnalysisTest {

    // ── Standard test profile: ~600 m ASL, typical Central European summer day ──

    private val standardProfile = listOf(
        ProfileLevel(pressureHpa = 950f, temperatureC = 18f, dewPointC = 8f, heightKm = 0.60f),
        ProfileLevel(pressureHpa = 925f, temperatureC = 15f, dewPointC = 6f, heightKm = 0.80f),
        ProfileLevel(pressureHpa = 900f, temperatureC = 12f, dewPointC = 4f, heightKm = 1.00f),
        ProfileLevel(pressureHpa = 850f, temperatureC = 8f, dewPointC = 0f, heightKm = 1.50f),
        ProfileLevel(pressureHpa = 800f, temperatureC = 4f, dewPointC = -4f, heightKm = 2.00f),
        ProfileLevel(pressureHpa = 700f, temperatureC = -3f, dewPointC = -15f, heightKm = 3.00f),
        ProfileLevel(pressureHpa = 600f, temperatureC = -10f, dewPointC = -25f, heightKm = 4.20f),
        ProfileLevel(pressureHpa = 500f, temperatureC = -20f, dewPointC = -35f, heightKm = 5.50f),
    )

    private val standardHeatingInput = SurfaceHeatingInput(
        hourOfDay = 13,
        shortwaveRadiationWm2 = 700f,
        cloudCoverLowPercent = 10f,
        cloudCoverMidPercent = 5f,
        cloudCoverHighPercent = 0f,
        precipitationMm = 0f,
        isDay = true,
    )

    // ── Thermodynamic helpers ──

    @Test
    fun potentialTemperature_atSurface() {
        // θ at 1000 hPa, 20°C should be close to 293.15 K
        val theta = potentialTemperatureK(20f, 1000f)
        assertEquals(293.15f, theta, 1f)
    }

    @Test
    fun dryAdiabat_roundTrip() {
        val theta = potentialTemperatureK(20f, 1000f)
        val t = dryAdiabatTempC(theta, 1000f)
        assertEquals(20f, t, 0.1f)
    }

    @Test
    fun dryAdiabat_decreasesWithAltitude() {
        val theta = potentialTemperatureK(20f, 1000f)
        val t1000 = dryAdiabatTempC(theta, 1000f)
        val t850 = dryAdiabatTempC(theta, 850f)
        val t700 = dryAdiabatTempC(theta, 700f)
        assertTrue("Temperature should decrease with altitude", t1000 > t850)
        assertTrue("Temperature should decrease with altitude", t850 > t700)
    }

    @Test
    fun satMixingRatio_increaseWithTemperature() {
        val mr10 = satMixingRatioGKg(10f, 1000f)
        val mr20 = satMixingRatioGKg(20f, 1000f)
        val mr30 = satMixingRatioGKg(30f, 1000f)
        assertTrue("Mixing ratio should increase with T", mr20 > mr10)
        assertTrue("Mixing ratio should increase with T", mr30 > mr20)
    }

    @Test
    fun satMixingRatio_realisticValues() {
        val mr = satMixingRatioGKg(15f, 1000f)
        // At 15°C, 1000 hPa: expect ~10.5 g/kg
        assertTrue("Sat mixing ratio should be ~10 g/kg at 15°C", mr in 8f..14f)
    }

    @Test
    fun relativeHumidityFraction_isOneAtSaturation() {
        val humidity = relativeHumidityFraction(12f, 12f)
        assertEquals(1f, humidity, 0.001f)
    }

    @Test
    fun mixingRatioTemperature_roundTripsSaturationMixingRatio() {
        val originalTemperature = 7f
        val pressure = 850f
        val mixingRatio = satMixingRatioGKg(originalTemperature, pressure)
        val reconstructedTemperature = mixingRatioTemperatureC(mixingRatio, pressure)
        assertEquals(originalTemperature, reconstructedTemperature, 0.3f)
    }

    @Test
    fun moistAdiabat_warmerThanDryAboveLcl() {
        val theta = potentialTemperatureK(20f, 1000f)
        val dryTemp = dryAdiabatTempC(theta, 600f)
        val moistTemp = moistAdiabatTempC(theta, 600f)
        assertTrue("Moist adiabat should be warmer than dry at same pressure",
            moistTemp > dryTemp)
    }

    @Test
    fun estimateSurfacePressure_seaLevel() {
        val p = estimateSurfacePressure(0.0)
        assertEquals(1013.25f, p, 1f)
    }

    @Test
    fun estimateSurfacePressure_highElevation() {
        val p = estimateSurfacePressure(1500.0)
        assertTrue("Surface pressure at 1500m should be ~850 hPa", p in 830f..860f)
    }

    // ── Surface heating ──

    @Test
    fun surfaceHeating_peakMidday_sunny() {
        val input = SurfaceHeatingInput(
            hourOfDay = 13,
            shortwaveRadiationWm2 = 800f,
            cloudCoverLowPercent = 0f,
            cloudCoverMidPercent = 0f,
            cloudCoverHighPercent = 0f,
            precipitationMm = 0f,
            isDay = true,
        )
        val heating = estimateSurfaceHeating(input)
        assertTrue("Peak sunny midday heating should be >4°C", heating >= 4f)
        assertTrue("Heating should not exceed 8°C", heating <= 8f)
    }

    @Test
    fun surfaceHeating_nightTime_zero() {
        val input = SurfaceHeatingInput(
            hourOfDay = 3,
            shortwaveRadiationWm2 = 0f,
            cloudCoverLowPercent = 0f,
            cloudCoverMidPercent = 0f,
            cloudCoverHighPercent = 0f,
            precipitationMm = 0f,
            isDay = false,
        )
        val heating = estimateSurfaceHeating(input)
        assertEquals(0f, heating, 0.01f)
    }

    @Test
    fun surfaceHeating_overcast_reduced() {
        val clear = SurfaceHeatingInput(
            hourOfDay = 13,
            shortwaveRadiationWm2 = 700f,
            cloudCoverLowPercent = 0f,
            cloudCoverMidPercent = 0f,
            cloudCoverHighPercent = 0f,
            precipitationMm = 0f,
            isDay = true,
        )
        val overcast = clear.copy(cloudCoverLowPercent = 80f, cloudCoverMidPercent = 50f)
        val heatingClear = estimateSurfaceHeating(clear)
        val heatingOvercast = estimateSurfaceHeating(overcast)
        assertTrue("Overcast heating should be less than clear", heatingOvercast < heatingClear)
    }

    @Test
    fun surfaceHeating_precipitation_reduces() {
        val dry = SurfaceHeatingInput(
            hourOfDay = 13,
            shortwaveRadiationWm2 = 500f,
            cloudCoverLowPercent = 30f,
            cloudCoverMidPercent = 10f,
            cloudCoverHighPercent = 0f,
            precipitationMm = 0f,
            isDay = true,
        )
        val rainy = dry.copy(precipitationMm = 2f)
        val heatingDry = estimateSurfaceHeating(dry)
        val heatingRainy = estimateSurfaceHeating(rainy)
        assertTrue("Rain should reduce heating", heatingRainy < heatingDry)
    }

    @Test
    fun surfaceHeating_noRadiationData_usesConservativeDefault() {
        val input = SurfaceHeatingInput(
            hourOfDay = 12,
            shortwaveRadiationWm2 = null,
            cloudCoverLowPercent = null,
            cloudCoverMidPercent = null,
            cloudCoverHighPercent = null,
            precipitationMm = null,
            isDay = true,
        )
        val heating = estimateSurfaceHeating(input)
        assertTrue("Should still provide some heating", heating > 0f)
        assertTrue("Should be conservative", heating < 5f)
    }

    // ── Full parcel analysis ──

    @Test
    fun parcelAnalysis_standardProfile_findsUsableThermals() {
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
            modelCapeJKg = 300f,
        )

        assertNotNull("Should produce analysis", result)
        result!!

        assertTrue("Should have thermal cells", result.thermalCells.isNotEmpty())
        assertTrue("Dry top should be above elevation", result.dryThermalTopKm > 0.58f)
        assertTrue("LCL should be above elevation", result.lclKm > 0.58f)
        assertTrue("CCL should be above elevation", result.cclKm > 0.58f)
        assertTrue("LCL pressure should be below the surface", result.lclPressureHpa < 955f)
        assertTrue("CCL pressure should be below the surface", result.cclPressureHpa < 955f)
        assertNotNull("TCON should be available for the standard profile", result.tconC)
    }

    @Test
    fun parcelAnalysis_dryTopBelowLcl_noCloudBase() {
        // Very stable profile — inversion at 800m, LCL will be above dry top
        val stableProfile = listOf(
            ProfileLevel(pressureHpa = 950f, temperatureC = 15f, dewPointC = 2f, heightKm = 0.60f),
            ProfileLevel(pressureHpa = 925f, temperatureC = 16f, dewPointC = 1f, heightKm = 0.80f), // Inversion!
            ProfileLevel(pressureHpa = 900f, temperatureC = 14f, dewPointC = -2f, heightKm = 1.00f),
            ProfileLevel(pressureHpa = 850f, temperatureC = 10f, dewPointC = -6f, heightKm = 1.50f),
            ProfileLevel(pressureHpa = 700f, temperatureC = -3f, dewPointC = -20f, heightKm = 3.00f),
        )
        val result = analyzeParcel(
            profile = stableProfile,
            surfaceTemperatureC = 16f,
            surfaceDewPointC = 2f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput.copy(shortwaveRadiationWm2 = 200f),
            modelCapeJKg = 10f,
        )

        assertNotNull(result)
        result!!
        // With a strong inversion, cloud base may be null (dry top below LCL)
        // This is acceptable - we're testing the logic handles it correctly
        assertTrue("Dry top should be below 2 km in this stable profile",
            result.dryThermalTopKm < 2f)
    }

    @Test
    fun parcelAnalysis_computedCapeIsPositive_withBuoyantProfile() {
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
            modelCapeJKg = 500f,
        )

        assertNotNull(result)
        result!!
        assertTrue("Computed CAPE should be positive", result.computedCapeJKg > 0f)
    }

    @Test
    fun parcelAnalysis_thermalCellsOnlyBelowDryTop() {
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
        )

        assertNotNull(result)
        result!!

        result.thermalCells.forEach { cell ->
            assertTrue(
                "Cell top ${cell.endAltitudeKm} should be <= dry top ${result.dryThermalTopKm}",
                cell.endAltitudeKm <= result.dryThermalTopKm + 0.01f,
            )
        }
    }

    @Test
    fun parcelAnalysis_cellsAreContiguous() {
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
        )

        assertNotNull(result)
        result!!
        val cells = result.thermalCells
        if (cells.size >= 2) {
            val sorted = cells.sortedBy { it.startAltitudeKm }
            for (i in 0 until sorted.size - 1) {
                assertEquals(
                    "Cell end should equal next cell start",
                    sorted[i].endAltitudeKm,
                    sorted[i + 1].startAltitudeKm,
                    0.02f,
                )
            }
        }
    }

    @Test
    fun parcelAnalysis_insufficientProfile_returnsNull() {
        val thinProfile = listOf(
            ProfileLevel(pressureHpa = 950f, temperatureC = 18f, dewPointC = 8f, heightKm = 0.60f),
        )
        val result = analyzeParcel(
            profile = thinProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
        )
        assertNull("Single-level profile should return null", result)
    }

    @Test
    fun parcelAnalysis_emptyProfile_returnsNull() {
        val result = analyzeParcel(
            profile = emptyList(),
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
        )
        assertNull("Empty profile should return null", result)
    }

    @Test
    fun parcelAnalysis_allLevelsBelowElevation_returnsNull() {
        val lowProfile = listOf(
            ProfileLevel(pressureHpa = 1000f, temperatureC = 20f, dewPointC = 12f, heightKm = 0.10f),
            ProfileLevel(pressureHpa = 975f, temperatureC = 18f, dewPointC = 10f, heightKm = 0.30f),
        )
        val result = analyzeParcel(
            profile = lowProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 850f,
            elevationKm = 1.5f,
            heatingInput = standardHeatingInput,
        )
        assertNull("Profile below elevation should return null", result)
    }

    @Test
    fun parcelAnalysis_modelCapeIsDiagnosticOnly() {
        val resultNoModel = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
            modelCapeJKg = null,
        )
        val resultWithModel = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
            modelCapeJKg = 800f,
        )

        assertNotNull(resultNoModel)
        assertNotNull(resultWithModel)
        // Both should produce the same thermic cells; model CAPE is carried as a diagnostic only.
        assertTrue(resultNoModel!!.thermalCells.isNotEmpty())
        assertTrue(resultWithModel!!.thermalCells.isNotEmpty())
        resultNoModel.thermalCells.zip(resultWithModel.thermalCells).forEach { (withoutCape, withCape) ->
            assertEquals(withoutCape.strengthMps, withCape.strengthMps, 0.0001f)
        }
    }

    @Test
    fun parcelAnalysis_cloudBaseKm_isMaxOfLclAndCcl() {
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
        )

        assertNotNull(result)
        result!!

        val cloudBase = result.cloudBaseKm
        if (cloudBase != null) {
            assertTrue(
                "Cloud base should be >= LCL",
                cloudBase >= result.lclKm - 0.05f,
            )
            assertTrue(
                "Cloud base should be >= CCL",
                cloudBase >= result.cclKm - 0.05f,
            )
        }
    }

    @Test
    fun parcelAnalysis_strengthValues_areReasonable() {
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
        )

        assertNotNull(result)
        result!!

        result.thermalCells.forEach { cell ->
            assertTrue("Strength should be positive: ${cell.strengthMps}",
                cell.strengthMps > 0f)
            assertTrue("Strength should be <= 10 m/s: ${cell.strengthMps}",
                cell.strengthMps <= 10f)
        }
    }

    // ── Solar elevation factor ──

    @Test
    fun solarElevationFactor_peaksAtMidday() {
        val f13 = solarElevationFactor(13)
        val f8 = solarElevationFactor(8)
        val f18 = solarElevationFactor(18)
        assertTrue("Peak should be at 13", f13 >= f8)
        assertTrue("Peak should be at 13", f13 >= f18)
        assertEquals(1f, f13, 0.01f)
    }

    @Test
    fun solarElevationFactor_zeroAtNight() {
        val f21 = solarElevationFactor(21)
        assertEquals(0f, f21, 0.01f)
    }

    // ── CAPE discrepancy analysis ──

    @Test
    fun capeDiscrepancy_documentedFactors() {
        // This test documents why computed CAPE differs from model CAPE.
        // Computed CAPE is retained for parcel diagnostics, not for thermic strength calibration.
        val result = analyzeParcel(
            profile = standardProfile,
            surfaceTemperatureC = 22f,
            surfaceDewPointC = 10f,
            surfacePressureHpa = 955f,
            elevationKm = 0.58f,
            heatingInput = standardHeatingInput,
            modelCapeJKg = 300f,
        )

        assertNotNull(result)
        result!!

        // The discrepancy arises from:
        // 1. Surface heating (+2 to +8°C) boosts parcel θ above model's T2m-based CAPE
        // 2. Coarse profile (8 levels) vs model's fine vertical grid (~90 levels)
        // 3. No virtual temperature correction (moisture buoyancy ignored)
        // 4. Simplified moist adiabat (4 Newton iterations vs full integration)
        // 5. Model likely uses mixed-layer CAPE (avg lowest 100 hPa) vs our surface-based parcel
        //
        assertTrue(
            "Computed CAPE (${result.computedCapeJKg}) should be positive",
            result.computedCapeJKg > 0f,
        )

        // Surface heating shifts the parcel significantly warmer, typically resulting
        // in computed CAPE exceeding model CAPE when radiation is strong.
        assertTrue(
            "Surface heating should be > 0",
            result.surfaceHeatingC > 0f,
        )
    }
}
