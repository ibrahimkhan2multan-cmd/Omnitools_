package com.omnitools.app.tools.html

/** What changed in the Code Runner, shown in its Updates tab and copyable to send to someone. */
object RunnerChangelog {
    data class Entry(val version: String, val date: String, val title: String, val items: List<String>)

    val entries: List<Entry> = listOf(
        Entry(
            "Build 59", "12 Nov 2026", "Photo Viewer, Video Player and polished icons",
            listOf(
                "New Photo Viewer: open one or many pictures (JPEG, PNG, WebP, GIF, BMP, and HEIC or AVIF on phones that support them), swipe between them, pinch or double-tap to zoom, share. Animated GIFs show their first frame.",
                "New Video Player: plays MP4, MKV, WebM, 3GP, TS and more offline, with a playlist, speed from 0.5x to 2x, seeking and a rotate button. What plays depends on your phone's codecs; videos it cannot play show a message and the player moves on.",
                "Icons are now rounded and every category has its own colour with a soft gradient badge.",
            ),
        ),
        Entry(
            "Build 58", "11 Nov 2026", "Animated background themes",
            listOf(
                "Settings > Background theme: Galaxy (twinkling stars), Candy, Rainbow, Aurora, Sunset and Ocean. Every screen sits on the animated picture; Plain keeps the old look.",
                "Each theme brings its own colours and its own light or dark look, so cards and text stay readable.",
            ),
        ),
        Entry(
            "Build 57", "11 Nov 2026", "Sound themes and a click on every button",
            listOf(
                "Every tap on a button now makes a sound, including back arrows, toolbar icons, switches and cards. The back gesture makes a soft back sound.",
                "Settings > Sound & touch: pick a sound theme (Classic, Galaxy, Bubble, Retro, Crystal or Wood), set the volume, and try each sound.",
                "All sounds are made by the app itself, so there is still no internet use and nothing extra to download.",
            ),
        ),
        Entry(
            "Build 56", "11 Nov 2026", "Update log moved to the Home screen",
            listOf(
                "The update log now has its own button at the top of the Home screen, next to Settings. A small dot shows when there is something you have not read yet.",
                "It no longer lives inside the Code Runner, so the Code Runner tabs are just your code and the preview.",
            ),
        ),
        Entry(
            "Build 55", "11 Nov 2026", "Calculator, text and number-tool fixes",
            listOf(
                "Calculator: whole numbers up to 15 digits now show every digit (2^49 used to show 562949953421000). tan(90) now says it is undefined instead of showing a huge number.",
                "Calculator: implicit multiplication works, so 2(3), 2π, 3sin(30) and 2√9 are accepted.",
                "Number to Words: digits typed with an Urdu or Arabic keyboard no longer crash the tool; it asks for digits 0-9. A very long loan term no longer wraps around into a short one.",
                "Timestamp Converter: a date with an extreme year (such as +999999999) no longer crashes the screen; years 0001 to 9999 are accepted. Number Base: a doubled sign such as --5 is rejected instead of being read as 5. ITF barcodes reject non-ASCII digits with a clear message.",
                "Text Tools: JSON format and minify keep numbers exactly as written (1.0 stays 1.0, long IDs are not rounded) and reject invalid JSON such as single quotes or trailing commas. Base64 decode accepts URL-safe text, missing padding and spaces.",
            ),
        ),
        Entry(
            "Build 54", "9 Nov 2026", "Full code review: Music Player, Video to Audio and sounds",
            listOf(
                "Fixed: pressing Play right after Stop could make the music cut out again.",
                "Fixed: Pause pressed while a song is still loading now works, and the buttons show the right icon.",
                "Music now carries on by itself after a short interruption (a call, an alarm) and turns down for short notification sounds, instead of staying paused.",
                "Shuffle keeps its order when you add or remove songs, so Previous and already-played songs behave properly.",
                "Songs that cannot be played are skipped safely even in long queues, and Android's focus is kept between songs.",
                "Video to Audio: a failed save at the very end now falls back to WAV instead of leaving a broken file; leftover half-written files are cleaned up; Save a copy now uses the real audio type.",
                ".m4a and .ogg files that the phone labels as video are now accepted in the Music Player.",
                "The app logo no longer flashes empty on later visits, and its blade tips are no longer clipped. Smoother record animation. Fewer odd flashes of the mini player at startup.",
                "Privacy policy updated for the music notification.",
            ),
        ),
        Entry(
            "Build 54", "9 Nov 2026", "Bug-fix pass on music, audio extraction and sounds",
            listOf(
                "Music: pressing Stop in the notification and then Play straight away could cut the music out. Fixed.",
                "Music: Shuffle no longer re-shuffles the whole queue when you add or remove a song, so Previous and the play order stay consistent.",
                "Music: a short call or alarm now pauses the music and it resumes by itself afterwards; a short notification sound only turns it down. Songs no longer lose audio focus between tracks.",
                "Music: Pause pressed while a song is still loading now works. Long runs of missing songs are skipped safely.",
                "Music: .m4a, .ogg and .3gp files that some phones label as video are now accepted. A note appears when Android cannot remember more picked files for playlists.",
                "Video to Audio: a damaged copy is no longer kept as a result (it falls back to WAV), leftovers from an interrupted extraction are cleaned up, and Save a copy uses the right audio type.",
                "App logo: no empty flash when returning to Home, and the blade tips are no longer clipped. Vinyl record animation is smoother.",
            ),
        ),
        Entry(
            "Build 53", "9 Nov 2026", "New tool: Video to Audio",
            listOf(
                "Pick a video from your phone and extract its sound. Nothing is uploaded; it all happens on the phone.",
                "Original keeps the exact sound from the video with no quality loss and is fast (M4A for AAC sound, OGG or WebM for Opus/Vorbis). When the video's sound cannot be copied as is (for example MP3 or AC-3), it is converted to a WAV file instead.",
                "Extracted audio is saved inside OmniTools, where you can play it in the Music Player, add it to the music queue, save a copy anywhere on your phone, or delete it.",
                "Shows the sound format, length and progress, and can be cancelled. Videos with no sound are reported clearly.",
            ),
        ),
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

    /** The newest build in the log, e.g. "Build 56". Used for the "new" dot on the Home button. */
    val latestVersion: String get() = entries.first().version

    fun asText(): String = buildString {
        append("OmniTools update log\n")
        for (e in entries) {
            append("\n").append(e.version).append(" (").append(e.date).append("): ").append(e.title).append('\n')
            e.items.forEach { append("- ").append(it).append('\n') }
        }
        append("\n").append(LIMITS).append('\n')
    }
}
