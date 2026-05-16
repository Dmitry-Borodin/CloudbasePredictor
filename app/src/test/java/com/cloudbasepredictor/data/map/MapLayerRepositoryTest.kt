package com.cloudbasepredictor.data.map

import android.content.SharedPreferences
import com.cloudbasepredictor.data.place.FavoritePlacesBackupStore
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLayerRepositoryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun mapLayerEntries_keepExternalEsriLayerLast() {
        assertEquals(
            listOf(
                MapLayerPreference.OPENFREEMAP,
                MapLayerPreference.OPENTOPOMAP,
                MapLayerPreference.NASA_GIBS,
                MapLayerPreference.ESRI_WORLD_IMAGERY,
            ),
            MapLayerPreference.entries.toList(),
        )
    }

    @Test
    fun validLocalPreferenceLoadsBeforeBackup() {
        val prefs = FakeSharedPreferences().apply {
            edit().putString("map_layer", MapLayerPreference.OPENFREEMAP.name).apply()
        }
        val backupStore = FavoritePlacesBackupStore(FakeSharedPreferences(), json).apply {
            saveMapLayer(MapLayerPreference.NASA_GIBS)
        }

        val repo = SharedPrefsMapLayerRepository(
            prefs = prefs,
            backupStore = backupStore,
        )

        assertEquals(MapLayerPreference.OPENFREEMAP, repo.selectedLayer.value)
    }

    @Test
    fun backupPreferenceRestoresWhenLocalPreferenceIsMissing() {
        val prefs = FakeSharedPreferences()
        val backupStore = FavoritePlacesBackupStore(FakeSharedPreferences(), json).apply {
            saveMapLayer(MapLayerPreference.NASA_GIBS)
        }

        val repo = SharedPrefsMapLayerRepository(
            prefs = prefs,
            backupStore = backupStore,
        )

        assertEquals(MapLayerPreference.NASA_GIBS, repo.selectedLayer.value)
        assertEquals("NASA_GIBS", prefs.getString("map_layer", null))
    }

    @Test
    fun oldBackupWithoutMapLayerKeepsOpenFreeMapDefault() {
        val backupPrefs = FakeSharedPreferences().apply {
            edit()
                .putString(
                    "payload",
                    """
                        {
                          "schemaVersion": 2,
                          "places": []
                        }
                    """.trimIndent(),
                )
                .apply()
        }

        val repo = SharedPrefsMapLayerRepository(
            prefs = FakeSharedPreferences(),
            backupStore = FavoritePlacesBackupStore(backupPrefs, json),
        )

        assertEquals(MapLayerPreference.OPENFREEMAP, repo.selectedLayer.value)
    }

    @Test
    fun setMapLayerPersistsToLocalPrefsAndBackup() {
        val prefs = FakeSharedPreferences()
        val backupStore = FavoritePlacesBackupStore(FakeSharedPreferences(), json)
        val repo = SharedPrefsMapLayerRepository(
            prefs = prefs,
            backupStore = backupStore,
        )

        repo.selectLayer(MapLayerPreference.ESRI_WORLD_IMAGERY)

        assertEquals(MapLayerPreference.ESRI_WORLD_IMAGERY, repo.selectedLayer.value)
        assertEquals("ESRI_WORLD_IMAGERY", prefs.getString("map_layer", null))
        assertEquals(MapLayerPreference.ESRI_WORLD_IMAGERY, backupStore.readMapLayer())
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
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            listener?.let { listeners.add(it) }
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            listener?.let { listeners.remove(it) }
        }

        private inner class FakeEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                key?.let { pending[it] = values }
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                key?.let { pending[it] = value }
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                key?.let { pending[it] = null }
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }

            override fun commit(): Boolean {
                flush()
                return true
            }

            override fun apply() {
                flush()
            }

            private fun flush() {
                if (clear) data.clear()
                pending.forEach { (key, value) ->
                    if (value == null) {
                        data.remove(key)
                    } else {
                        data[key] = value
                    }
                    listeners.forEach { it.onSharedPreferenceChanged(this@FakeSharedPreferences, key) }
                }
            }
        }
    }
}
