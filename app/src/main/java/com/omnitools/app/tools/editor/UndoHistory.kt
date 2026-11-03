package com.omnitools.app.tools.editor

/** Snapshot-based undo/redo for the editor. Pure Kotlin, unit tested. */
class UndoHistory(initial: String, private val limit: Int = 100) {
    private val snapshots = ArrayList<String>().apply { add(initial) }
    private var index = 0

    val current: String get() = snapshots[index]
    val canUndo: Boolean get() = index > 0
    val canRedo: Boolean get() = index < snapshots.size - 1

    /** Remembers [text] as a new step unless it equals the current one. Drops any redo steps. Returns true if recorded. */
    fun record(text: String): Boolean {
        if (text == snapshots[index]) return false
        while (snapshots.size > index + 1) snapshots.removeAt(snapshots.size - 1)
        snapshots.add(text)
        index++
        if (snapshots.size > limit) {
            snapshots.removeAt(0)
            index--
        }
        return true
    }

    fun undo(): String? = if (canUndo) snapshots[--index] else null
    fun redo(): String? = if (canRedo) snapshots[++index] else null

    /** Starts over from [text], for a newly opened file. */
    fun reset(text: String) {
        snapshots.clear()
        snapshots.add(text)
        index = 0
    }
}
