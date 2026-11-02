package com.omnitools.app.tools.qr

import java.net.URI

/**
 * Inspects scanned text before the user is offered a chance to open it.
 * Nothing here opens anything: it only describes what the content is and why it may be risky.
 */
object UrlSafety {

    data class Analysis(
        /** True when the text parsed as an absolute URI with a scheme. */
        val isUri: Boolean,
        val scheme: String?,
        val host: String?,
        /** Only plain http/https links can ever be opened from the scanner. */
        val canOpen: Boolean,
        val warnings: List<String>,
    )

    private val shorteners = setOf(
        "bit.ly", "t.co", "tinyurl.com", "goo.gl", "is.gd", "cutt.ly", "ow.ly", "buff.ly", "rebrand.ly", "shorturl.at",
    )

    private val ipv4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    fun analyze(raw: String): Analysis {
        val text = raw.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) {
            return Analysis(false, null, null, false, emptyList())
        }
        val uri = try {
            URI(text)
        } catch (e: Exception) {
            return Analysis(false, null, null, false, emptyList())
        }
        val scheme = uri.scheme?.lowercase() ?: return Analysis(false, null, null, false, emptyList())

        val warnings = mutableListOf<String>()
        val host = uri.host?.lowercase()

        if (scheme != "http" && scheme != "https") {
            warnings += "This is a \"$scheme:\" link, not a web page. OmniTools will not open it."
            return Analysis(true, scheme, host, false, warnings)
        }
        if (host.isNullOrEmpty()) {
            warnings += "The link has no readable website address."
            return Analysis(true, scheme, null, false, warnings)
        }

        if (scheme == "http") warnings += "Not encrypted (http). Information you enter could be read by others."
        if (uri.rawUserInfo != null) warnings += "Contains a username section before the address, a common trick to disguise the real destination."
        if (ipv4.matches(host) || host.startsWith("[")) warnings += "Points to a raw IP address instead of a website name."
        if (host.split('.').any { it.startsWith("xn--") }) warnings += "Uses an internationalized (punycode) domain that can imitate another site."
        if (host in shorteners) warnings += "Link shortener: the final destination is hidden until it is opened."
        if (uri.port != -1 && uri.port != 80 && uri.port != 443) warnings += "Uses an unusual port (${uri.port})."
        if (text.length > 200) warnings += "Unusually long link."

        return Analysis(true, scheme, host, true, warnings)
    }
}
