package com.omnitools.app.tools.qr

/** Explains what a piece of QR/barcode text is and lists its fields. Never opens anything. */
object QrInspector {
    data class Info(val type: String, val fields: List<Pair<String, String>>, val warnings: List<String>)

    private fun splitWifi(body: String): List<String> {
        val parts = ArrayList<String>(); val cur = StringBuilder(); var esc = false
        for (c in body) {
            when {
                esc -> { cur.append(c); esc = false }
                c == '\\' -> esc = true
                c == ';' -> { parts.add(cur.toString()); cur.setLength(0) }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) parts.add(cur.toString())
        return parts
    }

    fun inspect(raw: String): Info {
        val text = raw.trim()
        require(text.isNotEmpty()) { "Enter or paste the text from a code." }
        val lower = text.lowercase()
        return when {
            lower.startsWith("wifi:") -> {
                val map = splitWifi(text.substring(5)).mapNotNull { p -> p.indexOf(':').takeIf { it > 0 }?.let { p.substring(0, it).uppercase() to p.substring(it + 1) } }.toMap()
                val warnings = ArrayList<String>()
                val sec = map["T"].orEmpty().ifEmpty { "none" }
                if (sec.equals("nopass", true) || sec.equals("none", true)) warnings.add("This network has no password.")
                if (sec.equals("WEP", true)) warnings.add("WEP security is broken. Avoid this network.")
                Info("Wi-Fi network", listOf("Network name" to map["S"].orEmpty(), "Security" to sec, "Password" to map["P"].orEmpty().ifEmpty { "(none)" }, "Hidden" to (map["H"]?.equals("true", true) == true).toString()), warnings)
            }
            lower.startsWith("begin:vcard") -> {
                val fields = ArrayList<Pair<String, String>>()
                for (line in text.lines()) {
                    val k = line.substringBefore(':', "").substringBefore(';').uppercase()
                    val v = line.substringAfter(':', "").trim()
                    val label = when (k) { "FN" -> "Name"; "N" -> "Name (parts)"; "TEL" -> "Phone"; "EMAIL" -> "Email"; "ORG" -> "Organization"; "TITLE" -> "Title"; "URL" -> "Website"; "ADR" -> "Address"; else -> null }
                    if (label != null && v.isNotEmpty()) fields.add(label to v.replace(";", " ").trim())
                }
                Info("Contact (vCard)", fields, emptyList())
            }
            lower.startsWith("mailto:") -> Info("Email address", listOf("To" to text.substring(7).substringBefore('?')) + queryFields(text), emptyList())
            lower.startsWith("tel:") -> Info("Phone number", listOf("Number" to text.substring(4)), emptyList())
            lower.startsWith("smsto:") || lower.startsWith("sms:") -> {
                val body = text.substringAfter(':')
                val message = if (':' in body) body.substringAfter(':') else queryFields("?" + body.substringAfter('?', "")).firstOrNull { it.first.equals("body", true) }?.second.orEmpty()
                Info("Text message", listOf("To" to body.substringBefore(':').substringBefore('?'), "Message" to message), listOf("Check the number: premium-rate numbers can cost money."))
            }
            lower.startsWith("geo:") -> Info("Location", listOf("Coordinates" to text.substring(4).substringBefore('?')), emptyList())
            lower.startsWith("http://") || lower.startsWith("https://") -> {
                val a = UrlSafety.analyze(text)
                Info("Web address", listOf("Scheme" to (a.scheme ?: ""), "Host" to (a.host ?: "")), a.warnings)
            }
            Regex("^[0-9]{8,14}$").matches(text) -> Info("Product barcode number", listOf("Digits" to text, "Length" to "${text.length}"), emptyList())
            else -> Info("Plain text", listOf("Length" to "${text.length} characters"), if (text.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }) listOf("Contains hidden control characters.") else emptyList())
        }
    }

    private fun queryFields(text: String): List<Pair<String, String>> =
        text.substringAfter('?', "").split('&').filter { '=' in it }.map { it.substringBefore('=').replaceFirstChar { c -> c.uppercase() } to it.substringAfter('=') }
}
