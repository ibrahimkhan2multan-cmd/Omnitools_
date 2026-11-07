package com.omnitools.app.tools.html

/** What changed in the Code Runner, shown in its Updates tab and copyable to send to someone. */
object RunnerChangelog {
    data class Entry(val version: String, val date: String, val title: String, val items: List<String>)

    val entries: List<Entry> = listOf(
        Entry(
            "2.0", "7 Nov 2026", "Python, TypeScript, SQL and Lua",
            listOf(
                "Python 3.14 now runs fully offline, with the standard library (math, random, json, re, collections, itertools, datetime, asyncio, sqlite3 and more).",
                "Python input() reads from the new Input tab, one line per input() call. Errors show a clean traceback with your line numbers.",
                "TypeScript: types, interfaces, enums, generics and top-level await are supported. Types are removed before running; they are not checked for mistakes.",
                "SQL: a fresh in-memory SQLite database for every run. Several statements run in order, SELECT results appear as tables, and results before an error are kept.",
                "Lua 5.3 with the standard libraries.",
                "JavaScript mode now also reports errors from promises that were never handled.",
                "New Updates tab (this page) with a Copy and Share button.",
            ),
        ),
        Entry(
            "1.1", "Earlier", "Touch, games and more formats",
            listOf(
                "Taps now reach the page, so canvas games such as Flappy Bird start and respond.",
                "On-screen keys (Space, arrows, Enter) for games written for a keyboard.",
                "New modes: JavaScript on its own, Markdown preview, SVG preview and JSON formatter.",
            ),
        ),
        Entry(
            "1.0", "Earlier", "First version",
            listOf(
                "HTML, CSS and JavaScript in three tabs with a live preview and a console.",
                "Runs in a locked-down page: no internet, no access to your files or to the app.",
            ),
        ),
    )

    const val LIMITS = "Not supported: Java, C, C++, Kotlin, Go, Rust, PHP and other languages that need a compiler or a server. " +
        "Python has the standard library only (no pip, numpy or pandas). An endless loop freezes the preview; leave the Preview tab and run again. " +
        "Python needs a few seconds to start the first time. Everything runs on the phone and nothing is sent anywhere."

    fun asText(): String = buildString {
        append("OmniTools Code Runner: update log\n")
        for (e in entries) {
            append("\n").append(e.version).append(" (").append(e.date).append("): ").append(e.title).append('\n')
            e.items.forEach { append("- ").append(it).append('\n') }
        }
        append("\n").append(LIMITS).append('\n')
    }
}
