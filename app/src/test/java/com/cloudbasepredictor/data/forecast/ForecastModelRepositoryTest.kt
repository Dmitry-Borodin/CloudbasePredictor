package com.cloudbasepredictor.data.forecast

import android.content.SharedPreferences
import com.cloudbasepredictor.model.ForecastModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastModelRepositoryTest {

    @Test
    fun selectedModel_defaultsToIconSeamless() {
        val prefs = FakeSharedPreferences()
        val repo = InMemoryForecastModelRepository(prefs)
        assertEquals(ForecastModel.ICON_SEAMLESS, repo.selectedModel.value)
    }

    @Test
    fun selectModel_updatesFlow() {
        val prefs = FakeSharedPreferences()
        val repo = InMemoryForecastModelRepository(prefs)
        repo.selectModel(ForecastModel.ICON_D2)
        assertEquals(ForecastModel.ICON_D2, repo.selectedModel.value)
    }

    @Test
    fun selectModel_persistsToPrefs() {
        val prefs = FakeSharedPreferences()
        val repo = InMemoryForecastModelRepository(prefs)
        repo.selectModel(ForecastModel.ECMWF_IFS)
        assertEquals("ecmwf_ifs025", prefs.getString("selected_forecast_model", null))
    }

    @Test
    fun loadFromPrefs_restoresSavedModel() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("selected_forecast_model", "icon_d2").apply()
        val repo = InMemoryForecastModelRepository(prefs)
        assertEquals(ForecastModel.ICON_D2, repo.selectedModel.value)
    }

    @Test
    fun loadFromPrefs_unknownApiName_fallsToDefaultModel() {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("selected_forecast_model", "unknown_model").apply()
        val repo = InMemoryForecastModelRepository(prefs)
        assertEquals(ForecastModel.ICON_SEAMLESS, repo.selectedModel.value)
    }

    /**
     * Minimal SharedPreferences fake backed by a HashMap.
     */
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
