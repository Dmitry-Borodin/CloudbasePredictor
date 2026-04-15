package com.cloudbasepredictor.data.forecast

import android.content.SharedPreferences
import com.cloudbasepredictor.ui.screens.forecast.DEFAULT_TOP_ALTITUDE_KM
import com.cloudbasepredictor.ui.screens.forecast.MAX_TOP_ALTITUDE_KM
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastViewportRepositoryTest {

    @Test
    fun defaultAltitude_isDefault() {
        val prefs = FakeSharedPreferences()
        val repo = SharedPrefsForecastViewportRepository(prefs)
        assertEquals(DEFAULT_TOP_ALTITUDE_KM, repo.visibleTopAltitudeKm.value)
    }

    @Test
    fun setVisibleTopAltitudeKm_updatesFlow() {
        val prefs = FakeSharedPreferences()
        val repo = SharedPrefsForecastViewportRepository(prefs)
        repo.setVisibleTopAltitudeKm(6.0f)
        assertEquals(6.0f, repo.visibleTopAltitudeKm.value)
    }

    @Test
    fun setVisibleTopAltitudeKm_persistsToPrefs() {
        val prefs = FakeSharedPreferences()
        val repo = SharedPrefsForecastViewportRepository(prefs)
        repo.setVisibleTopAltitudeKm(5.5f)
        assertEquals(5.5f, prefs.getFloat("forecast_visible_top_altitude_km", 0f))
    }

    @Test
    fun loadFromPrefs_restoresSavedAltitude() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putFloat("forecast_visible_top_altitude_km", 6.2f).apply()
        val repo = SharedPrefsForecastViewportRepository(prefs)
        assertEquals(6.2f, repo.visibleTopAltitudeKm.value)
    }

    @Test
    fun setVisibleTopAltitudeKm_clampsAboveMax() {
        val prefs = FakeSharedPreferences()
        val repo = SharedPrefsForecastViewportRepository(prefs)
        repo.setVisibleTopAltitudeKm(20f)
        assertEquals(MAX_TOP_ALTITUDE_KM, repo.visibleTopAltitudeKm.value)
    }

    @Test
    fun setVisibleTopAltitudeKm_rejectsNaN() {
        val prefs = FakeSharedPreferences()
        val repo = SharedPrefsForecastViewportRepository(prefs)
        repo.setVisibleTopAltitudeKm(Float.NaN)
        assertEquals(DEFAULT_TOP_ALTITUDE_KM, repo.visibleTopAltitudeKm.value)
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()
        private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (data[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int =
            data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long =
            data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float =
            data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) { listener?.let { listeners.add(it) } }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) { listener?.let { listeners.remove(it) } }

        private inner class FakeEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                key?.let { pending[it] = value }; return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                key?.let { pending[it] = values }; return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                key?.let { pending[it] = value }; return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                key?.let { pending[it] = value }; return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                key?.let { pending[it] = value }; return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                key?.let { pending[it] = value }; return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                key?.let { pending[it] = null }; return this
            }
            override fun clear(): SharedPreferences.Editor { clear = true; return this }

            override fun commit(): Boolean { flush(); return true }
            override fun apply() { flush() }

            private fun flush() {
                if (clear) data.clear()
                pending.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
            }
        }
    }
}
