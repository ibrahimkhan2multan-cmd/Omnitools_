package com.omnitools.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(
    name = "omnitools_prefs",
    // A damaged settings file must never stop the app from starting: start fresh instead.
    corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler { androidx.datastore.preferences.core.emptyPreferences() },
)

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
        val onboarded = booleanPreferencesKey("onboarded")
        val privacyAccepted = booleanPreferencesKey("privacy_accepted")
        val saveScans = booleanPreferencesKey("save_scans")
        val showRecents = booleanPreferencesKey("show_recents")
        val calcDegrees = booleanPreferencesKey("calc_degrees")
        val sounds = booleanPreferencesKey("sounds")
        val bgTheme = stringPreferencesKey("bg_theme")
        val soundTheme = stringPreferencesKey("sound_theme")
        val soundVolume = androidx.datastore.preferences.core.floatPreferencesKey("sound_volume")
        val seenUpdate = stringPreferencesKey("seen_update")
        val musicStyle = stringPreferencesKey("music_style")
        val musicAccent = androidx.datastore.preferences.core.intPreferencesKey("music_accent")
        val musicMini = booleanPreferencesKey("music_mini")
        val haptics = booleanPreferencesKey("haptics")
    }

    private val store get() = context.dataStore

    val favorites: Flow<Set<String>> = store.data.map { it[Keys.favorites] ?: emptySet() }

    val recents: Flow<List<String>> = store.data.map { decodeRecents(it[Keys.recents]) }

    val themeMode: Flow<String> = store.data.map { it[Keys.themeMode] ?: "system" }

    val scanHistory: Flow<List<ScanEntry>> = store.data.map { decodeScans(it[Keys.scanHistory]) }

    /** False until the user has made the one-time dark/light choice on first launch. */
    val onboarded: Flow<Boolean> = store.data.map { it[Keys.onboarded] ?: false }

    /** False until the user has tapped Accept on the privacy policy screen. */
    val privacyAccepted: Flow<Boolean> = store.data.map { it[Keys.privacyAccepted] ?: false }

    suspend fun acceptPrivacy() { store.edit { it[Keys.privacyAccepted] = true } }

    /** Whether scanned codes are saved to the local history (default on). */
    val saveScans: Flow<Boolean> = store.data.map { it[Keys.saveScans] ?: true }

    /** Whether the home screen shows the "Recently used" section (default on). */
    val showRecents: Flow<Boolean> = store.data.map { it[Keys.showRecents] ?: true }

    /** Default calculator angle unit: true = degrees, false = radians. */
    val calcDegrees: Flow<Boolean> = store.data.map { it[Keys.calcDegrees] ?: true }

    /** How the Music Player looks (style, accent colour, mini player on every screen). */
    val musicDesign: Flow<com.omnitools.app.tools.music.MusicDesign> = store.data.map {
        com.omnitools.app.tools.music.MusicDesign(
            style = com.omnitools.app.tools.music.PlayerStyle.from(it[Keys.musicStyle]),
            accent = com.omnitools.app.tools.music.MusicAccents.coerce(it[Keys.musicAccent] ?: 0),
            miniPlayer = it[Keys.musicMini] ?: true,
        )
    }
    suspend fun setMusicStyle(style: com.omnitools.app.tools.music.PlayerStyle) { store.edit { it[Keys.musicStyle] = style.name } }
    suspend fun setMusicAccent(index: Int) { store.edit { it[Keys.musicAccent] = com.omnitools.app.tools.music.MusicAccents.coerce(index) } }
    suspend fun setMusicMini(value: Boolean) { store.edit { it[Keys.musicMini] = value } }

    /** The newest update-log entry the user has opened (drives the "new" dot on Home). */
    val seenUpdate: Flow<String?> = store.data.map { it[Keys.seenUpdate] }
    suspend fun setSeenUpdate(version: String) { store.edit { it[Keys.seenUpdate] = version } }

    /** The animated background (Plain, Galaxy, Candy, ...). */
    val backgroundTheme: Flow<com.omnitools.app.ui.theme.BackgroundTheme> = store.data.map { com.omnitools.app.ui.theme.BackgroundTheme.from(it[Keys.bgTheme]) }
    suspend fun setBackgroundTheme(theme: com.omnitools.app.ui.theme.BackgroundTheme) { store.edit { it[Keys.bgTheme] = theme.name } }

    /** The chosen sound theme and volume (0..1). */
    val soundTheme: Flow<com.omnitools.app.ui.feedback.SoundTheme> = store.data.map { com.omnitools.app.ui.feedback.SoundTheme.from(it[Keys.soundTheme]) }
    val soundVolume: Flow<Float> = store.data.map { (it[Keys.soundVolume] ?: 0.8f).coerceIn(0f, 1f) }
    suspend fun setSoundTheme(theme: com.omnitools.app.ui.feedback.SoundTheme) { store.edit { it[Keys.soundTheme] = theme.name } }
    suspend fun setSoundVolume(volume: Float) { store.edit { it[Keys.soundVolume] = volume.coerceIn(0f, 1f) } }

    /** Button sounds (default on). */
    val sounds: Flow<Boolean> = store.data.map { it[Keys.sounds] ?: true }

    /** Small vibration on taps (default on). */
    val haptics: Flow<Boolean> = store.data.map { it[Keys.haptics] ?: true }

    suspend fun setSounds(value: Boolean) { store.edit { it[Keys.sounds] = value } }
    suspend fun setHaptics(value: Boolean) { store.edit { it[Keys.haptics] = value } }

    /** Stores the first-run choice and marks onboarding as done in one write. */
    suspend fun completeOnboarding(themeMode: String) {
        store.edit {
            it[Keys.themeMode] = themeMode
            it[Keys.onboarded] = true
        }
    }

    suspend fun setSaveScans(value: Boolean) { store.edit { it[Keys.saveScans] = value } }
    suspend fun setShowRecents(value: Boolean) { store.edit { it[Keys.showRecents] = value } }
    suspend fun setCalcDegrees(value: Boolean) { store.edit { it[Keys.calcDegrees] = value } }

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

    suspend fun clearFavorites() {
        store.edit { it.remove(Keys.favorites) }
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

        internal fun encodeScans(list: List<ScanEntry>): String {
            val arr = JSONArray()
            list.forEach {
                arr.put(JSONObject().put("t", it.timestamp).put("f", it.format).put("x", it.text))
            }
            return arr.toString()
        }

        internal fun decodeScans(raw: String?): List<ScanEntry> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                // One bad entry is skipped; it must not wipe the whole history.
                (0 until arr.length()).mapNotNull {
                    try {
                        val o = arr.getJSONObject(it)
                        ScanEntry(o.getLong("t"), o.getString("f"), o.getString("x"))
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
