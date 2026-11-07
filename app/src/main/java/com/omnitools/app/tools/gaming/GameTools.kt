package com.omnitools.app.tools.gaming

import com.omnitools.app.tools.advanced.Json
import java.util.Locale
import java.util.UUID

/** UUID helpers for pack and mod manifests. */
object Uuids {
    private val pattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun isValid(s: String) = pattern.matches(s.trim())

    fun random(): String = UUID.randomUUID().toString()

    fun format(uuid: String, upper: Boolean, dashes: Boolean, braces: Boolean): String {
        require(isValid(uuid)) { "That is not a valid UUID." }
        var s = uuid.trim().lowercase(Locale.ROOT)
        if (!dashes) s = s.replace("-", "")
        if (upper) s = s.uppercase(Locale.ROOT)
        return if (braces) "{$s}" else s
    }

    fun many(count: Int, upper: Boolean, dashes: Boolean, braces: Boolean): List<String> {
        require(count in 1..50) { "Choose between 1 and 50 UUIDs." }
        return List(count) { format(random(), upper, dashes, braces) }
    }
}

/** Builds a Minecraft Bedrock add-on manifest.json (format version 2). */
object AddonManifest {
    enum class Kind(val label: String, val moduleType: String) {
        BEHAVIOR("Behavior pack", "data"),
        RESOURCE("Resource pack", "resources"),
    }

    class Input(
        val name: String, val description: String, val kind: Kind,
        val version: List<Int>, val minEngine: List<Int>,
        val headerUuid: String, val moduleUuid: String,
        val dependencyUuid: String? = null, val dependencyVersion: List<Int> = listOf(1, 0, 0),
    )

    fun parseVersion(text: String): List<Int> {
        val parts = text.trim().split('.')
        require(parts.size == 3 && parts.all { p -> p.isNotEmpty() && p.length <= 4 && p.all { it in '0'..'9' } }) { "A version looks like 1.0.0 (three numbers)." }
        return parts.map { it.toInt() }
    }

    fun build(i: Input): String {
        require(i.name.isNotBlank()) { "Give the pack a name." }
        require(i.name.length <= 100) { "The name is too long (100 characters at most)." }
        require(i.description.length <= 300) { "The description is too long (300 characters at most)." }
        require(Uuids.isValid(i.headerUuid)) { "The pack UUID is not valid." }
        require(Uuids.isValid(i.moduleUuid)) { "The module UUID is not valid." }
        require(!i.headerUuid.equals(i.moduleUuid, ignoreCase = true)) { "The pack and module UUIDs must be different." }
        require(i.version.size == 3 && i.minEngine.size == 3) { "Versions need three numbers." }
        i.dependencyUuid?.let {
            require(Uuids.isValid(it)) { "The dependency UUID is not valid." }
            require(!it.equals(i.headerUuid, ignoreCase = true)) { "A pack cannot depend on itself." }
        }
        fun ver(v: List<Int>) = Json.Node.Arr(v.map { Json.Node.Num(it.toString()) })
        fun str(s: String) = Json.Node.Str(s)
        val header = Json.Node.Obj(listOf(
            "name" to str(i.name.trim()), "description" to str(i.description.trim()), "uuid" to str(i.headerUuid.trim().lowercase(Locale.ROOT)),
            "version" to ver(i.version), "min_engine_version" to ver(i.minEngine),
        ))
        val module = Json.Node.Obj(listOf(
            "type" to str(i.kind.moduleType), "uuid" to str(i.moduleUuid.trim().lowercase(Locale.ROOT)), "version" to ver(i.version),
            "description" to str(i.description.trim().ifEmpty { i.name.trim() }),
        ))
        val fields = ArrayList<Pair<String, Json.Node>>()
        fields.add("format_version" to Json.Node.Num("2"))
        fields.add("header" to header)
        fields.add("modules" to Json.Node.Arr(listOf(module)))
        i.dependencyUuid?.let { fields.add("dependencies" to Json.Node.Arr(listOf(Json.Node.Obj(listOf("uuid" to str(it.trim().lowercase(Locale.ROOT)), "version" to ver(i.dependencyVersion)))))) }
        return Json.print(Json.Node.Obj(fields))
    }
}

/** Minecraft chat/formatting codes ("&a" or "§a"). */
object McFormat {
    data class Span(
        val text: String, val rgb: Int?, val bold: Boolean = false, val italic: Boolean = false,
        val underline: Boolean = false, val strike: Boolean = false, val obfuscated: Boolean = false,
    )

    val colors: Map<Char, Int> = mapOf(
        '0' to 0x000000, '1' to 0x0000AA, '2' to 0x00AA00, '3' to 0x00AAAA, '4' to 0xAA0000, '5' to 0xAA00AA, '6' to 0xFFAA00, '7' to 0xAAAAAA,
        '8' to 0x555555, '9' to 0x5555FF, 'a' to 0x55FF55, 'b' to 0x55FFFF, 'c' to 0xFF5555, 'd' to 0xFF55FF, 'e' to 0xFFFF55, 'f' to 0xFFFFFF,
    )
    private val styles = setOf('k', 'l', 'm', 'n', 'o', 'r')

    fun isCode(c: Char) = c.lowercaseChar() in colors || c.lowercaseChar() in styles

    /** [marker] is '&' or '§'. A marker not followed by a valid code is kept as normal text. */
    fun parse(text: String, marker: Char): List<Span> {
        val out = ArrayList<Span>()
        val buf = StringBuilder()
        var rgb: Int? = null; var b = false; var ital = false; var u = false; var s = false; var k = false
        fun flush() { if (buf.isNotEmpty()) { out.add(Span(buf.toString(), rgb, b, ital, u, s, k)); buf.setLength(0) } }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == marker && i + 1 < text.length && isCode(text[i + 1])) {
                flush()
                when (val code = text[i + 1].lowercaseChar()) {
                    'r' -> { rgb = null; b = false; ital = false; u = false; s = false; k = false }
                    'l' -> b = true; 'o' -> ital = true; 'n' -> u = true; 'm' -> s = true; 'k' -> k = true
                    else -> { rgb = colors[code]; b = false; ital = false; u = false; s = false; k = false } // a colour resets styles
                }
                i += 2
            } else { buf.append(c); i++ }
        }
        flush()
        return out
    }

    /** Text with every code removed. */
    fun strip(text: String, marker: Char): String = parse(text, marker).joinToString("") { it.text }

    /** Swaps '&' codes for the real section sign used inside the game files. */
    fun toSection(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '&' && i + 1 < text.length && isCode(text[i + 1])) { sb.append('§').append(text[i + 1]); i += 2 } else { sb.append(text[i]); i++ }
        }
        return sb.toString()
    }
}

/** Minecraft coordinate maths: dimensions, chunks, regions, distance. */
object McCoords {
    const val LIMIT = 30_000_000L

    data class Result(
        val nether: Pair<Long, Long>, val overworld: Pair<Long, Long>,
        val chunk: Pair<Long, Long>, val inChunk: Pair<Long, Long>, val chunkOrigin: Pair<Long, Long>,
        val region: Pair<Long, Long>, val regionFile: String,
    )

    fun parse(text: String, label: String): Long {
        val v = text.trim().toLongOrNull() ?: throw IllegalArgumentException("$label must be a whole number.")
        require(v in -LIMIT..LIMIT) { "$label must be between -30,000,000 and 30,000,000." }
        return v
    }

    fun compute(x: Long, z: Long): Result {
        val cx = Math.floorDiv(x, 16L); val cz = Math.floorDiv(z, 16L)
        val rx = Math.floorDiv(cx, 32L); val rz = Math.floorDiv(cz, 32L)
        return Result(
            nether = Math.floorDiv(x, 8L) to Math.floorDiv(z, 8L),
            overworld = x * 8 to z * 8,
            chunk = cx to cz,
            inChunk = Math.floorMod(x, 16L) to Math.floorMod(z, 16L),
            chunkOrigin = cx * 16 to cz * 16,
            region = rx to rz,
            regionFile = "r.$rx.$rz.mca",
        )
    }

    fun distance2d(x1: Long, z1: Long, x2: Long, z2: Long): Double = Math.hypot((x2 - x1).toDouble(), (z2 - z1).toDouble())

    fun distance3d(x1: Long, y1: Long, z1: Long, x2: Long, y2: Long, z2: Long): Double =
        Math.sqrt(Math.pow((x2 - x1).toDouble(), 2.0) + Math.pow((y2 - y1).toDouble(), 2.0) + Math.pow((z2 - z1).toDouble(), 2.0))
}

/** Packing a folder into an add-on or mod archive. */
object PackZip {
    const val MAX_FILES = 20_000
    const val MAX_BYTES = 500L * 1024 * 1024

    /** A safe entry name for the zip, or null if the path could escape the archive. */
    fun safeEntryName(path: String): String? {
        val p = path.replace('\\', '/').trimStart('/')
        if (p.isEmpty() || p.endsWith("/")) return null
        val parts = p.split('/')
        if (parts.any { it == ".." || it == "." || it.isEmpty() }) return null
        return p
    }

    /** Warns when the pack file would not be found by the game; null when manifest.json sits at the top. */
    fun manifestWarning(paths: List<String>): String? {
        if (paths.any { it == "manifest.json" }) return null
        val nested = paths.firstOrNull { it.endsWith("/manifest.json") }
        return if (nested != null) "manifest.json is inside \"${nested.substringBeforeLast('/')}\". The game expects it at the top level, so choose that inner folder instead."
        else "No manifest.json was found. Minecraft packs need one at the top level."
    }

    fun suggestedName(folderName: String, extension: String): String {
        val base = folderName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifEmpty { "pack" }.take(80)
        return "$base.$extension"
    }
}
