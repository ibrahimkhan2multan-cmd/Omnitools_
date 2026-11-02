package com.omnitools.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "omnitools_prefs")

/** One saved QR / barcode scan. Stored locally only. */
data class ScanEntry(
    val timestamp: Long,
    val format: String,
    val text: String,
)

/** All small persistent state: favorites, recents, theme and scan history. Everything stays on-device. */
class Prefs(private val context: Context) {

    private object Keys {
        val favorites = stringSetPreferencesKey("favorites")
        val recents = stringPreferencesKey("recents")
        val themeMode = stringPreferencesKey("theme_mode")
        val scanHistory = stringPreferencesKey("scan_history")
    }

    private val store get() = context.dataStore

    val favorites: Flow<Set<String>> = store.data.map { it[Keys.favorites] ?: emptySet() }

    val recents: Flow<List<String>> = store.data.map { decodeRecents(it[Keys.recents]) }

    val themeMode: Flow<String> = store.data.map { it[Keys.themeMode] ?: "system" }

    val scanHistory: Flow<List<ScanEntry>> = store.data.map { decodeScans(it[Keys.scanHistory]) }

    suspend fun toggleFavorite(toolId: String) {
        store.edit { p ->
            val current = p[Keys.favorites] ?: emptySet()
            p[Keys.favorites] = if (toolId in current) current - toolId else current + toolId
        }
    }

    suspend fun pushRecent(toolId: String) {
        store.edit { p ->
            p[Keys.recents] = encodeRecents(addRecent(decodeRecents(p[Keys.recents]), toolId))
        }
    }

    suspend fun clearRecents() {
        store.edit { it.remove(Keys.recents) }
    }

    suspend fun setThemeMode(mode: String) {
        store.edit { it[Keys.themeMode] = mode }
    }

    suspend fun addScan(entry: ScanEntry) {
        store.edit { p ->
            val list = decodeScans(p[Keys.scanHistory])
            // Avoid stacking identical consecutive scans while the camera keeps seeing one code.
            val updated = if (list.firstOrNull()?.text == entry.text) list else listOf(entry) + list
            p[Keys.scanHistory] = encodeScans(updated.take(MAX_SCANS))
        }
    }

    suspend fun deleteScan(timestamp: Long) {
        store.edit { p ->
            val list = decodeScans(p[Keys.scanHistory]).filterNot { it.timestamp == timestamp }
            p[Keys.scanHistory] = encodeScans(list)
        }
    }

    suspend fun clearScans() {
        store.edit { it.remove(Keys.scanHistory) }
    }

    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    companion object {
        const val MAX_RECENTS = 8
        const val MAX_SCANS = 200

        /** Moves [toolId] to the front, de-duplicates and caps the list. Pure function (unit tested). */
        fun addRecent(current: List<String>, toolId: String): List<String> =
            (listOf(toolId) + current.filterNot { it == toolId }).take(MAX_RECENTS)

        fun encodeRecents(list: List<String>): String = list.joinToString(",")

        fun decodeRecents(raw: String?): List<String> =
            raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        private fun encodeScans(list: List<ScanEntry>): String {
            val arr = JSONArray()
            list.forEach {
                arr.put(JSONObject().put("t", it.timestamp).put("f", it.format).put("x", it.text))
            }
            return arr.toString()
        }

        private fun decodeScans(raw: String?): List<ScanEntry> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    ScanEntry(o.getLong("t"), o.getString("f"), o.getString("x"))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
