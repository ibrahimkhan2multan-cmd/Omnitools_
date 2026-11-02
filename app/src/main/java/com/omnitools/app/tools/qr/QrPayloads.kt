package com.omnitools.app.tools.qr

/** Builds the text payloads that phone cameras recognise as Wi-Fi networks and contacts. Pure and unit tested. */
object QrPayloads {

    enum class WifiSecurity(val label: String, val code: String) {
        WPA("WPA/WPA2/WPA3", "WPA"),
        WEP("WEP", "WEP"),
        NONE("Open (no password)", "nopass"),
    }

    /** Escapes the characters that are special in the WIFI: format. */
    internal fun escapeWifi(value: String): String =
        buildString {
            for (c in value) {
                if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') append('\\')
                append(c)
            }
        }

    fun wifi(ssid: String, password: String, security: WifiSecurity, hidden: Boolean): String {
        require(ssid.isNotBlank()) { "Network name is required." }
        if (security != WifiSecurity.NONE) require(password.isNotEmpty()) { "Password is required for secured networks." }
        return buildString {
            append("WIFI:T:").append(security.code).append(';')
            append("S:").append(escapeWifi(ssid)).append(';')
            if (security != WifiSecurity.NONE) append("P:").append(escapeWifi(password)).append(';')
            if (hidden) append("H:true;")
            append(';')
        }
    }

    /** Escapes commas, semicolons, backslashes and newlines for vCard 3.0 values. */
    internal fun escapeVCard(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n")

    fun contact(name: String, phone: String, email: String, organization: String): String {
        require(name.isNotBlank() || phone.isNotBlank() || email.isNotBlank()) {
            "Enter at least a name, phone number or email."
        }
        return buildString {
            append("BEGIN:VCARD\n")
            append("VERSION:3.0\n")
            append("FN:").append(escapeVCard(name.trim())).append('\n')
            append("N:").append(escapeVCard(name.trim())).append(";;;;\n")
            if (organization.isNotBlank()) append("ORG:").append(escapeVCard(organization.trim())).append('\n')
            if (phone.isNotBlank()) append("TEL:").append(escapeVCard(phone.trim())).append('\n')
            if (email.isNotBlank()) append("EMAIL:").append(escapeVCard(email.trim())).append('\n')
            append("END:VCARD")
        }
    }
}
