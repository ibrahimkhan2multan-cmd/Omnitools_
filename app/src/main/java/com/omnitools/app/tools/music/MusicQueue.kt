package com.omnitools.app.tools.music

import java.util.Random

/** One song: where it is (a content:// address from the file picker) and its display name. */
data class Track(val uri: String, val name: String)

/** A saved, named list of songs. */
data class Playlist(val id: String, val name: String, val tracks: List<Track>)

enum class RepeatMode {
    OFF, ALL, ONE;
    fun next(): RepeatMode = values()[(ordinal + 1) % values().size]
}

/** Which song comes next or before, with shuffle and repeat. Pure functions (unit tested). */
object QueueLogic {
    /** Order the songs are played in: 0..size-1, or shuffled with [current] first. */
    fun order(size: Int, current: Int, shuffle: Boolean, random: Random = Random()): List<Int> {
        if (size <= 0) return emptyList()
        val all = (0 until size).toList()
        if (!shuffle) return all
        val cur = current.coerceIn(0, size - 1)
        val rest = all.filter { it != cur }.toMutableList()
        java.util.Collections.shuffle(rest, random)
        return listOf(cur) + rest
    }

    /** The song after [current]. [auto] is true when the song ended by itself (Repeat One then repeats it). Null = end of the queue. */
    fun next(order: List<Int>, current: Int, repeat: RepeatMode, auto: Boolean): Int? {
        if (order.isEmpty()) return null
        if (auto && repeat == RepeatMode.ONE) return current
        val pos = order.indexOf(current)
        if (pos < 0) return order.first()
        return when {
            pos + 1 < order.size -> order[pos + 1]
            repeat == RepeatMode.ALL -> order.first()
            else -> null
        }
    }

    /** The song before [current]. Null = already at the start. */
    fun previous(order: List<Int>, current: Int, repeat: RepeatMode): Int? {
        if (order.isEmpty()) return null
        val pos = order.indexOf(current)
        if (pos < 0) return order.first()
        return when {
            pos > 0 -> order[pos - 1]
            repeat == RepeatMode.ALL -> order.last()
            else -> null
        }
    }

    /** Keeps the existing play order when [added] songs are appended to a queue that had [oldSize]; new ones go last, shuffled. */
    fun orderAfterAppend(order: List<Int>, oldSize: Int, added: Int, shuffle: Boolean, random: Random = Random()): List<Int> {
        val fresh = (oldSize until oldSize + added).toMutableList()
        if (shuffle) java.util.Collections.shuffle(fresh, random)
        return order + fresh
    }

    /** Keeps the existing play order when the song at [removed] leaves the queue. */
    fun orderAfterRemoval(order: List<Int>, removed: Int): List<Int> =
        order.filter { it != removed }.map { if (it > removed) it - 1 else it }

    /** Which song is current after [removed] leaves a queue now holding [newSize] songs. */
    fun indexAfterRemoval(current: Int, removed: Int, newSize: Int): Int =
        if (removed == current) removed.coerceAtMost((newSize - 1).coerceAtLeast(0)) else indexAfterRemoval(current, removed)

    /** Where the current song sits after the song at [removed] is taken out. */
    fun indexAfterRemoval(current: Int, removed: Int): Int = if (removed < current) current - 1 else current

    /** Moves the item at [from] by [delta] places; out-of-range moves leave the list unchanged. */
    fun <T> moved(list: List<T>, from: Int, delta: Int): List<T> {
        val to = from + delta
        if (from !in list.indices || to !in list.indices || delta == 0) return list
        val m = list.toMutableList()
        m.add(to, m.removeAt(from))
        return m
    }
}

/** Saves playlists as plain text: one line per playlist ("P") or song ("T"), tab separated, with escaping. */
object PlaylistCodec {
    private fun esc(s: String) = buildString {
        for (c in s) when (c) {
            '\\' -> append("\\\\")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(c)
        }
    }

    private fun unesc(s: String): String {
        val b = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) { 't' -> b.append('\t'); 'n' -> b.append('\n'); 'r' -> b.append('\r'); else -> b.append(s[i + 1]) }
                i += 2
            } else { b.append(c); i++ }
        }
        return b.toString()
    }

    fun encode(playlists: List<Playlist>): String = buildString {
        for (p in playlists) {
            append("P\t").append(esc(p.id)).append('\t').append(esc(p.name)).append('\n')
            for (t in p.tracks) append("T\t").append(esc(t.uri)).append('\t').append(esc(t.name)).append('\n')
        }
    }

    /** Reads what [encode] wrote. Lines it does not understand are skipped, so a damaged file never crashes the app. */
    fun decode(text: String): List<Playlist> {
        val out = ArrayList<Playlist>()
        var id: String? = null; var name = ""; var tracks = ArrayList<Track>()
        fun flush() { id?.let { out.add(Playlist(it, name, tracks)) }; id = null; tracks = ArrayList() }
        for (line in text.split('\n')) {
            val parts = line.split('\t')
            when {
                parts.size == 3 && parts[0] == "P" -> { flush(); id = unesc(parts[1]); name = unesc(parts[2]) }
                parts.size == 3 && parts[0] == "T" && id != null -> tracks.add(Track(unesc(parts[1]), unesc(parts[2])))
            }
        }
        flush()
        return out
    }
}
