package com.omnitools.app.tools.html

/** What changed in the Code Runner, shown in its Updates tab and copyable to send to someone. */
object RunnerChangelog {
    data class Entry(val version: String, val date: String, val title: String, val items: List<String>)

    val entries: List<Entry> = listOf(
        Entry(
            "Build 52", "9 Nov 2026", "Music Player: mini player, landscape and designs",
            listOf(
                "A slim mini player now sits at the bottom of every screen while music is loaded, in portrait and landscape: play/pause, previous, next and close, a progress line, and a long song name that scrolls. Tap it to open the full player.",
                "The Music Player has a landscape layout: controls on the left, queue and playlists on the right.",
                "Song names are shown in full (wrapped onto more lines) instead of being cut with dots.",
                "Customize player design: four styles (Classic, Gradient, Vinyl with a spinning record, Minimal) and six colours. The mini player can be turned off. Your choice is saved.",
            ),
        ),
        Entry(
            "Build 51", "8 Nov 2026", "Music Player: plays outside the app, with playlists",
            listOf(
                "Music now keeps playing when you leave OmniTools or turn the screen off. A notification (and the lock screen) has Previous, Play/Pause, Next and Stop buttons. Headset buttons work, and unplugging headphones pauses.",
                "Pick many songs at once. They form a queue with Next, Previous, Shuffle and Repeat (off, all, one). Songs that cannot be played are skipped with a message.",
                "Playlists: create, rename and delete them, add songs, reorder, remove, and play or shuffle a playlist. Playlists are saved on this device only. Save the current queue as a playlist.",
                "Android 13 and newer will ask once to allow notifications; music still plays if you say no, but the controls will not show.",
            ),
        ),
        Entry(
            "Build 50", "8 Nov 2026", "New tool: Music Player",
            listOf(
                "Pick an MP3, M4A, WAV, OGG or FLAC file from your phone with the system file picker and play it offline. Nothing is downloaded or streamed.",
                "Play, Pause, Stop, a seek bar, 10-second skip buttons and repeat. The song name is shown above the controls.",
                "The music keeps playing while you open other tools. It stops when you close the app, and pauses if another app (like a call) needs the sound.",
                "Cancelling the picker changes nothing; a file that cannot be played shows a clear message instead of crashing.",
            ),
        ),
        Entry(
            "Build 49", "8 Nov 2026", "New icon, sounds and animations",
            listOf(
                "New app icon: a six-colour pinwheel on a glowing dark background. The logo inside the app spins in to match.",
                "Sounds: a soft click on every tap, a rising tone when code runs, a chime when you copy, all made by the app itself. Switch them off in Settings > Sound & touch.",
                "A tiny vibration tick on taps (also switchable).",
                "Home screen header now has a soft colour glow.",
            ),
        ),
        Entry(
            "Build 48", "7 Nov 2026", "Code Runner: SVG preview fixed",
            listOf(
                "SVG drawings now scale to fit the phone screen instead of being cut off on the right and bottom, including SVGs that only have a width and height and no viewBox.",
                "New Fit, 100%, zoom out and zoom in buttons above the preview, with the current zoom shown. Zoom in and scroll in any direction to see every part.",
                "Checked with 21 different SVG shapes and sizes (tiny icons, very tall, very wide, centimetres, percentages, no size at all) on four screen sizes.",
            ),
        ),
        Entry(
            "Build 46", "7 Nov 2026", "Code Runner: Python, TypeScript, SQL and Lua",
            listOf(
                "Python 3.14 runs fully offline, with the standard library (math, random, json, re, collections, itertools, datetime, asyncio, sqlite3 and more).",
                "Python input() reads from the new Input tab, one line per input() call. Errors show a clean traceback with your own line numbers.",
                "TypeScript: types, interfaces, enums, generics and top-level await are supported. Types are removed before running; they are not checked for mistakes.",
                "SQL: a fresh in-memory SQLite database for every run. Several statements run in order, SELECT results appear as tables, and results before an error are kept.",
                "Lua 5.3 with the standard libraries.",
                "JavaScript mode now also reports errors from promises that were never handled.",
                "New Updates tab (this page) with a Copy and Share button.",
            ),
        ),
        Entry(
            "Build 45", "7 Nov 2026", "New section: Gaming & Modding (5 tools)",
            listOf(
                "UUID Generator: random UUIDs for add-on packs and mods, in the format you need.",
                "Add-on Manifest Builder: a Minecraft Bedrock manifest.json for a behavior or resource pack.",
                "Color Code Preview: see Minecraft colour and style codes and convert & to the game's section sign.",
                "Coordinates & Chunks: Nether and Overworld conversion, chunk, region file and distance.",
                "Pack Builder: zip a pack folder into .mcpack, .mcaddon or .zip, with a check that manifest.json is at the top level.",
            ),
        ),
        Entry(
            "Build 44", "7 Nov 2026", "Review pass: fixes, no new features",
            listOf(
                "Long pasted text no longer risks a crash when you switch apps.",
                "IPv6 converter, CSV to JSON (hidden characters, duplicate column names, extra values), Password Strength, QR text messages and Color Palette percentages were fixed.",
                "Compress to Size shows a message instead of crashing on a very large photo. Flashcards save safely. PDF Merge and Extract limit memory use.",
            ),
        ),
        Entry(
            "Builds 42-43", "6 Nov 2026", "Every section now has at least five tools (28 new tools)",
            listOf(
                "Network Utilities: Subnet Calculator, IP Converter, URL Parser, HTTP Status Codes, Port Numbers.",
                "Advanced Tools: JSON Formatter, Text Compare, JWT Decoder, CSV and JSON converter, Chmod Calculator.",
                "Device Information: Battery, Storage & Memory, Display Info, Sensors.",
                "Images & Media: Color Picker, Color Palette, Photo Details (EXIF) with a clean copy, Compress to Size.",
                "PDF & Documents: Merge PDFs, Extract PDF Pages, PDF to Images (pages are copied as pictures, so text in the result cannot be selected).",
                "Security & Privacy: Password Strength, Passphrase Generator, Text Encryptor (AES-256 with a password).",
                "QR & Barcode: QR Content Inspector, QR Code Sheet (printable PDF).",
                "Files & Archives: File Info & Compare. Notes & Text: Flashcards.",
            ),
        ),
        Entry(
            "Earlier", "Before 6 Nov 2026", "Foundations",
            listOf(
                "Signed APK, privacy policy and a screen to accept it before using the app. The app has no internet permission.",
                "Files & Archives: Folder Analyzer, Duplicate Finder and Batch Rename.",
                "More tools: number base converter, discount calculator, countdown, habit tracker, Encode/Decode and Time Zones.",
                "Reliability and security pass: calculator, loan and settings fixes, and a build check that fails if a network permission ever appears.",
                "Code Runner: taps reach the page so canvas games work, on-screen game keys, and JavaScript, Markdown, SVG and JSON modes.",
                "Full tool and section names are shown without being cut off.",
            ),
        ),
    )

    const val LIMITS = "Code Runner limits. Not supported: Java, C, C++, Kotlin, Go, Rust, PHP and other languages that need a compiler or a server. " +
        "Python has the standard library only (no pip, numpy or pandas). An endless loop freezes the preview; leave the Preview tab and run again. " +
        "Python needs a few seconds to start the first time. Everything runs on the phone and nothing is sent anywhere."

    fun asText(): String = buildString {
        append("OmniTools update log\n")
        for (e in entries) {
            append("\n").append(e.version).append(" (").append(e.date).append("): ").append(e.title).append('\n')
            e.items.forEach { append("- ").append(it).append('\n') }
        }
        append("\n").append(LIMITS).append('\n')
    }
}
