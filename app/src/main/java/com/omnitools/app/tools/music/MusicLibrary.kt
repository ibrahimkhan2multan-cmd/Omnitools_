package com.omnitools.app.tools.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

/** The user's playlists, saved in a file on this device only. Call from the main thread. */
object MusicLibrary {
    var playlists by mutableStateOf<List<Playlist>>(emptyList()); private set
    var loaded by mutableStateOf(false); private set

    private var file: File? = null
    private val writer = Executors.newSingleThreadExecutor()   // one thread keeps saves in order

    fun init(context: Context) {
        if (loaded) return
        val f = File(context.applicationContext.filesDir, "music_playlists.txt")
        file = f
        playlists = try {
            if (f.exists()) PlaylistCodec.decode(f.readText()) else emptyList()
        } catch (e: Exception) {
            try { f.copyTo(File(f.parentFile, "music_playlists.bak"), overwrite = true) } catch (_: Exception) {}
            emptyList()
        }
        loaded = true
    }

    private fun update(list: List<Playlist>) {
        playlists = list
        val f = file ?: return
        val text = PlaylistCodec.encode(list)
        writer.execute {
            try {
                val tmp = File(f.parentFile, f.name + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(f)) { f.writeText(text); tmp.delete() }
            } catch (_: Exception) { /* a failed save must not crash playback */ }
        }
    }

    fun find(id: String?): Playlist? = playlists.firstOrNull { it.id == id }

    fun create(name: String, tracks: List<Track> = emptyList()): Playlist {
        val p = Playlist(UUID.randomUUID().toString(), cleanName(name), tracks)
        update(playlists + p)
        return p
    }

    fun rename(id: String, name: String) = update(playlists.map { if (it.id == id) it.copy(name = cleanName(name)) else it })
    fun delete(id: String) = update(playlists.filterNot { it.id == id })
    fun addTracks(id: String, tracks: List<Track>) = update(playlists.map { if (it.id == id) it.copy(tracks = it.tracks + tracks) else it })
    fun removeTrack(id: String, index: Int) = update(playlists.map {
        if (it.id == id && index in it.tracks.indices) it.copy(tracks = it.tracks.filterIndexed { i, _ -> i != index }) else it
    })
    fun moveTrack(id: String, index: Int, delta: Int) = update(playlists.map {
        if (it.id == id) it.copy(tracks = QueueLogic.moved(it.tracks, index, delta)) else it
    })

    fun cleanName(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(60).ifEmpty { "My playlist" }
}
