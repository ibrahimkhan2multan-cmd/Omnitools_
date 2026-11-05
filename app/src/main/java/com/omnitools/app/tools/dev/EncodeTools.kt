package com.omnitools.app.tools.dev

import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64

enum class EncodeKind(val label: String) { BASE64("Base64"), URL("URL"), HTML("HTML entities"), HEX("Hex"), BINARY("Binary") }

/** Text encoders and decoders. Everything works on UTF-8. Invalid input throws IllegalArgumentException with a readable message. */
object EncodeTools {
    fun encode(kind: EncodeKind, text: String): String = when (kind) {
        EncodeKind.BASE64 -> Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        EncodeKind.URL -> URLEncoder.encode(text, "UTF-8").replace("+", "%20")
        EncodeKind.HTML -> text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
        EncodeKind.HEX -> text.toByteArray(Charsets.UTF_8).joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }
        EncodeKind.BINARY -> text.toByteArray(Charsets.UTF_8).joinToString(" ") { Integer.toBinaryString(it.toInt() and 0xFF).padStart(8, '0') }
    }

    fun decode(kind: EncodeKind, text: String): String = try {
        when (kind) {
            EncodeKind.BASE64 -> {
                val bytes = Base64.getDecoder().decode(text.filter { !it.isWhitespace() }.replace('-', '+').replace('_', '/'))
                strictUtf8(bytes)
            }
            EncodeKind.URL -> URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
            EncodeKind.HTML -> text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")
            EncodeKind.HEX -> strictUtf8(
                text.filter { !it.isWhitespace() }.removePrefix("0x").also {
                    require(it.length % 2 == 0 && it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' }) { "Not valid hex" }
                }.chunked(2).map { it.toInt(16).toByte() }.toByteArray(),
            )
            EncodeKind.BINARY -> strictUtf8(
                text.filter { !it.isWhitespace() }.also {
                    require(it.isNotEmpty() && it.length % 8 == 0 && it.all { c -> c == '0' || c == '1' }) { "Binary must be groups of 8 bits (0 and 1)" }
                }.chunked(8).map { it.toInt(2).toByte() }.toByteArray(),
            )
        }
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException(e.message?.takeIf { it.startsWith("Not valid") || it.startsWith("Binary") || it.startsWith("Result") } ?: "That is not valid ${kind.label} text")
    }

    private fun strictUtf8(bytes: ByteArray): String {
        val dec = Charsets.UTF_8.newDecoder()
        return try { dec.decode(java.nio.ByteBuffer.wrap(bytes)).toString() } catch (e: java.nio.charset.CharacterCodingException) {
            throw IllegalArgumentException("Result is not readable UTF-8 text")
        }
    }
}

/** Converts one moment between time zones. */
object TimeZones {
    data class Row(val zone: String, val label: String, val time: String, val date: String, val offset: String, val dayShift: Int)

    val popular = listOf(
        "UTC", "America/Los_Angeles", "America/New_York", "America/Sao_Paulo", "Europe/London", "Europe/Paris",
        "Africa/Cairo", "Asia/Dubai", "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka", "Asia/Shanghai", "Asia/Tokyo", "Australia/Sydney",
    )

    fun isValid(id: String) = id in ZoneId.getAvailableZoneIds()

    fun search(query: String, limit: Int = 8): List<String> {
        val q = query.trim().lowercase().replace(' ', '_')
        if (q.isEmpty()) return emptyList()
        return ZoneId.getAvailableZoneIds().filter { q in it.lowercase() }.sorted().take(limit)
    }

    /** [dateTime] is the wall-clock time in [from]; each row is the same moment in another zone. Gaps/overlaps follow java.time rules. */
    fun convert(dateTime: LocalDateTime, from: String, targets: List<String>): List<Row> {
        val source = dateTime.atZone(ZoneId.of(from))
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM")
        return targets.filter { isValid(it) }.map { id ->
            val z = source.withZoneSameInstant(ZoneId.of(id))
            val shift = ChronoUnit.DAYS.between(source.toLocalDate(), z.toLocalDate()).toInt()
            Row(id, id.substringAfterLast('/').replace('_', ' '), z.format(timeFmt), z.format(dateFmt), z.offset.id.replace("Z", "+00:00"), shift)
        }
    }

    fun parseTime(text: String): LocalTime? = try {
        val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(text.trim()) ?: return null
        LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
    } catch (e: java.time.DateTimeException) { null }

    fun today(zone: String): LocalDate = LocalDate.now(ZoneId.of(zone))
}
