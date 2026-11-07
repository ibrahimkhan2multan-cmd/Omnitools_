package com.omnitools.app.tools.net

import java.net.URLDecoder

/** IPv4 helpers and the subnet calculator. Pure Kotlin. Nothing here touches the network. */
object Ipv4 {
    fun parse(text: String): Long? {
        val parts = text.trim().split('.')
        if (parts.size != 4) return null
        var v = 0L
        for (p in parts) {
            if (p.isEmpty() || p.length > 3 || !p.all { it in '0'..'9' }) return null
            val n = p.toInt()
            if (n > 255) return null
            v = v * 256 + n
        }
        return v
    }

    fun format(v: Long): String = "${(v shr 24) and 255}.${(v shr 16) and 255}.${(v shr 8) and 255}.${v and 255}"

    fun binary(v: Long): String = (3 downTo 0).joinToString(".") { Integer.toBinaryString(((v shr (it * 8)) and 255).toInt()).padStart(8, '0') }

    fun maskOf(prefix: Int): Long = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL

    /** Prefix length of a contiguous mask like 255.255.255.0, or null if the mask is not valid. */
    fun prefixOf(mask: Long): Int? {
        for (p in 0..32) if (maskOf(p) == mask) return p
        return null
    }

    data class Subnet(
        val address: Long, val prefix: Int, val mask: Long, val network: Long, val broadcast: Long,
        val firstHost: Long, val lastHost: Long, val hosts: Long, val kind: String, val netClass: String,
    )

    /** Accepts "192.168.1.10/24" or "192.168.1.10 255.255.255.0". Throws IllegalArgumentException with a readable message. */
    fun subnet(input: String): Subnet {
        val text = input.trim()
        require(text.isNotEmpty()) { "Enter an address like 192.168.1.10/24." }
        val addrText: String
        val prefix: Int
        if ('/' in text) {
            addrText = text.substringBefore('/').trim()
            val p = text.substringAfter('/').trim()
            require(p.isNotEmpty() && p.length <= 2 && p.all { it in '0'..'9' } && p.toInt() in 0..32) { "The prefix after / must be 0 to 32." }
            prefix = p.toInt()
        } else if (text.contains(' ')) {
            addrText = text.substringBefore(' ')
            val m = parse(text.substringAfter(' ').trim()) ?: throw IllegalArgumentException("That subnet mask is not valid.")
            prefix = prefixOf(m) ?: throw IllegalArgumentException("That subnet mask is not valid (its 1 bits must come first).")
        } else {
            addrText = text
            prefix = 24
        }
        val addr = parse(addrText) ?: throw IllegalArgumentException("That is not a valid IPv4 address (four numbers 0 to 255).")
        val mask = maskOf(prefix)
        val network = addr and mask
        val broadcast = network or (mask.inv() and 0xFFFFFFFFL)
        val (first, last, hosts) = when {
            prefix == 32 -> Triple(network, network, 1L)
            prefix == 31 -> Triple(network, broadcast, 2L)
            else -> Triple(network + 1, broadcast - 1, broadcast - network - 1)
        }
        return Subnet(addr, prefix, mask, network, broadcast, first, last, hosts, kindOf(addr), classOf(addr))
    }

    fun classOf(a: Long): String = when (((a shr 24) and 255).toInt()) {
        in 0..127 -> "A"
        in 128..191 -> "B"
        in 192..223 -> "C"
        in 224..239 -> "D (multicast)"
        else -> "E (reserved)"
    }

    fun kindOf(a: Long): String {
        val o1 = ((a shr 24) and 255).toInt()
        val o2 = ((a shr 16) and 255).toInt()
        return when {
            o1 == 10 -> "Private (10.0.0.0/8)"
            o1 == 172 && o2 in 16..31 -> "Private (172.16.0.0/12)"
            o1 == 192 && o2 == 168 -> "Private (192.168.0.0/16)"
            o1 == 127 -> "Loopback"
            o1 == 169 && o2 == 254 -> "Link-local"
            o1 == 100 && o2 in 64..127 -> "Carrier-grade NAT (100.64.0.0/10)"
            o1 == 0 -> "This network (0.0.0.0/8)"
            o1 in 224..239 -> "Multicast"
            o1 >= 240 -> "Reserved"
            else -> "Public"
        }
    }
}

/** IPv6 parsing, expanding and compressing (RFC 4291 / RFC 5952). */
object Ipv6 {
    fun parse(text: String): IntArray? {
        var s = text.trim().lowercase()
        if (s.isEmpty() || s.any { !(it.isDigit() || it in 'a'..'f' || it == ':' || it == '.') }) return null
        // An embedded IPv4 tail such as ::ffff:192.168.1.1
        var tail: List<Int> = emptyList()
        if ('.' in s) {
            val idx = s.lastIndexOf(':')
            if (idx < 0) return null
            val v4 = Ipv4.parse(s.substring(idx + 1)) ?: return null
            tail = listOf(((v4 shr 16) and 0xFFFF).toInt(), (v4 and 0xFFFF).toInt())
            s = s.substring(0, idx + 1) + "0:0"
        }
        if (s.count { it == ':' } > 8 || ":::" in s) return null
        val dbl = s.indexOf("::")
        if (dbl >= 0 && s.indexOf("::", dbl + 1) >= 0) return null
        fun groups(part: String): List<Int>? {
            if (part.isEmpty()) return emptyList()
            return part.split(':').map { g -> if (g.isEmpty() || g.length > 4) return null else (g.toIntOrNull(16) ?: return null) }
        }
        val result: List<Int> = if (dbl >= 0) {
            val head = groups(s.substring(0, dbl)) ?: return null
            val rest = groups(s.substring(dbl + 2)) ?: return null
            if (head.size + rest.size > 7) return null
            head + List(8 - head.size - rest.size) { 0 } + rest
        } else {
            groups(s)?.takeIf { it.size == 8 } ?: return null
        }
        val out = result.toIntArray()
        if (tail.isNotEmpty()) { out[6] = tail[0]; out[7] = tail[1] }
        return out
    }

    fun expand(g: IntArray): String = g.joinToString(":") { Integer.toHexString(it).padStart(4, '0') }

    fun compress(g: IntArray): String {
        var bestStart = -1; var bestLen = 0
        var i = 0
        while (i < 8) {
            if (g[i] == 0) {
                var j = i
                while (j < 8 && g[j] == 0) j++
                if (j - i > bestLen) { bestStart = i; bestLen = j - i }
                i = j
            } else i++
        }
        if (bestLen < 2) return g.joinToString(":") { Integer.toHexString(it) }
        val head = g.take(bestStart).joinToString(":") { Integer.toHexString(it) }
        val tail = g.drop(bestStart + bestLen).joinToString(":") { Integer.toHexString(it) }
        return "$head::$tail"
    }

    fun kind(g: IntArray): String = when {
        g.all { it == 0 } -> "Unspecified (::)"
        g.take(7).all { it == 0 } && g[7] == 1 -> "Loopback (::1)"
        (g[0] and 0xFFC0) == 0xFE80 -> "Link-local (fe80::/10)"
        (g[0] and 0xFE00) == 0xFC00 -> "Unique local (fc00::/7)"
        (g[0] and 0xFF00) == 0xFF00 -> "Multicast (ff00::/8)"
        g[0] == 0x2001 && g[1] == 0x0db8 -> "Documentation (2001:db8::/32)"
        (g[0] and 0xE000) == 0x2000 -> "Global unicast (2000::/3)"
        else -> "Other"
    }
}

/** Describes an IP address typed in any common form. */
object IpConvert {
    fun describe(input: String): List<Pair<String, String>> {
        val s = input.trim()
        require(s.isNotEmpty()) { "Enter an IP address or a number." }
        if (':' in s) {
            val g = Ipv6.parse(s) ?: throw IllegalArgumentException("That is not a valid IPv6 address.")
            return listOf("Full" to Ipv6.expand(g), "Shortest" to Ipv6.compress(g), "Type" to Ipv6.kind(g))
        }
        val v4 = Ipv4.parse(s)
        if (v4 != null) return v4Rows(v4)
        if (s.all { it in '0'..'9' } && s.length <= 10) {
            val n = s.toLong()
            require(n <= 0xFFFFFFFFL) { "An IPv4 number cannot be larger than 4294967295." }
            return v4Rows(n)
        }
        val hex = s.removePrefix("0x").removePrefix("0X")
        if (s.startsWith("0x", true) && hex.isNotEmpty() && hex.length <= 8 && hex.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return v4Rows(hex.toLong(16))
        throw IllegalArgumentException("Enter an IPv4 address (1.2.3.4), an IPv6 address, or a number from 0 to 4294967295.")
    }

    private fun v4Rows(v: Long) = listOf(
        "Dotted" to Ipv4.format(v), "Decimal" to v.toString(), "Hex" to "0x" + v.toString(16).uppercase().padStart(8, '0'),
        "Binary" to Ipv4.binary(v), "Type" to Ipv4.kindOf(v), "Class" to Ipv4.classOf(v),
    )
}

/** Splits a URL into parts. Pure string work, no network. */
object UrlParts {
    data class Parsed(
        val scheme: String, val userInfo: String?, val host: String?, val port: Int?, val path: String,
        val query: List<Pair<String, String>>, val fragment: String?,
    )

    private val pattern = Regex("^(?:([A-Za-z][A-Za-z0-9+.-]*):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?$", RegexOption.DOT_MATCHES_ALL)

    fun decode(s: String): String = try { URLDecoder.decode(s, "UTF-8") } catch (e: IllegalArgumentException) { s }

    fun parse(input: String): Parsed {
        val text = input.trim()
        require(text.isNotEmpty()) { "Enter a web address." }
        require(text.length <= 8000) { "That address is too long." }
        val m = pattern.matchEntire(text) ?: throw IllegalArgumentException("Could not read that address.")
        val scheme = m.groups[1]?.value ?: throw IllegalArgumentException("The address needs a scheme such as https://")
        val authority = m.groups[2]?.value
        var userInfo: String? = null
        var host: String? = null
        var port: Int? = null
        if (authority != null) {
            var rest = authority
            if ('@' in rest) { userInfo = rest.substringBeforeLast('@'); rest = rest.substringAfterLast('@') }
            if (rest.startsWith("[")) {
                val end = rest.indexOf(']')
                require(end > 0) { "The IPv6 address is missing a closing ]." }
                host = rest.substring(0, end + 1)
                val after = rest.substring(end + 1)
                if (after.startsWith(":")) port = portOf(after.substring(1))
            } else if (':' in rest) {
                host = rest.substringBeforeLast(':')
                port = portOf(rest.substringAfterLast(':'))
            } else host = rest
            if (host.isNullOrEmpty()) host = null
        }
        val query = m.groups[4]?.value?.takeIf { it.isNotEmpty() }?.split('&')?.filter { it.isNotEmpty() }?.map {
            val k = it.substringBefore('=')
            val v = if ('=' in it) it.substringAfter('=') else ""
            decode(k) to decode(v)
        } ?: emptyList()
        return Parsed(scheme.lowercase(), userInfo, host, port, m.groups[3]?.value.orEmpty(), query, m.groups[5]?.value)
    }

    private fun portOf(s: String): Int? {
        if (s.isEmpty()) return null
        require(s.all { it in '0'..'9' } && s.length <= 5 && s.toInt() in 0..65535) { "The port must be a number from 0 to 65535." }
        return s.toInt()
    }
}

/** Offline reference tables. */
object NetReference {
    data class Item(val key: String, val name: String, val detail: String)

    val httpStatus: List<Item> = listOf(
        Item("100", "Continue", "The server received the request headers; the client should send the body."),
        Item("101", "Switching Protocols", "The server is switching protocols as requested (for example to WebSocket)."),
        Item("200", "OK", "The request succeeded."),
        Item("201", "Created", "The request succeeded and a new resource was created."),
        Item("202", "Accepted", "The request was accepted but is not finished yet."),
        Item("204", "No Content", "Success, with nothing to send back."),
        Item("206", "Partial Content", "Only part of the resource was sent (range request)."),
        Item("301", "Moved Permanently", "The resource has a new permanent address."),
        Item("302", "Found", "The resource is temporarily at another address."),
        Item("303", "See Other", "Get the result from another address using GET."),
        Item("304", "Not Modified", "The cached copy is still valid."),
        Item("307", "Temporary Redirect", "Repeat the same request at another address, temporarily."),
        Item("308", "Permanent Redirect", "Repeat the same request at another address, permanently."),
        Item("400", "Bad Request", "The server could not understand the request."),
        Item("401", "Unauthorized", "Authentication is required or has failed."),
        Item("403", "Forbidden", "The server understood the request but refuses it."),
        Item("404", "Not Found", "The resource does not exist."),
        Item("405", "Method Not Allowed", "That HTTP method is not allowed for this resource."),
        Item("406", "Not Acceptable", "The server cannot produce a response the client accepts."),
        Item("408", "Request Timeout", "The server gave up waiting for the request."),
        Item("409", "Conflict", "The request conflicts with the current state of the resource."),
        Item("410", "Gone", "The resource used to exist and was removed on purpose."),
        Item("413", "Content Too Large", "The request body is larger than the server allows."),
        Item("414", "URI Too Long", "The address is longer than the server accepts."),
        Item("415", "Unsupported Media Type", "The server does not support the request's content type."),
        Item("418", "I'm a teapot", "A joke status from RFC 2324."),
        Item("422", "Unprocessable Content", "The request was well formed but its content is invalid."),
        Item("429", "Too Many Requests", "Rate limit hit: too many requests in a short time."),
        Item("500", "Internal Server Error", "The server hit an unexpected problem."),
        Item("501", "Not Implemented", "The server does not support what was asked."),
        Item("502", "Bad Gateway", "A gateway got an invalid answer from the server behind it."),
        Item("503", "Service Unavailable", "The server is overloaded or down for maintenance."),
        Item("504", "Gateway Timeout", "A gateway did not get an answer from the server in time."),
    )

    val ports: List<Item> = listOf(
        Item("20", "FTP data", "TCP. File transfer, data channel."),
        Item("21", "FTP", "TCP. File transfer control."),
        Item("22", "SSH / SFTP", "TCP. Secure shell and secure file transfer."),
        Item("23", "Telnet", "TCP. Unencrypted remote terminal. Avoid."),
        Item("25", "SMTP", "TCP. Sending email between servers."),
        Item("53", "DNS", "UDP/TCP. Domain name lookups."),
        Item("67", "DHCP server", "UDP. Hands out IP addresses."),
        Item("68", "DHCP client", "UDP. Receives IP addresses."),
        Item("80", "HTTP", "TCP. Web pages without encryption."),
        Item("110", "POP3", "TCP. Downloading email."),
        Item("123", "NTP", "UDP. Time synchronization."),
        Item("143", "IMAP", "TCP. Reading email on a server."),
        Item("161", "SNMP", "UDP. Network device monitoring."),
        Item("389", "LDAP", "TCP. Directory services."),
        Item("443", "HTTPS", "TCP. Encrypted web pages."),
        Item("445", "SMB", "TCP. Windows file sharing."),
        Item("465", "SMTPS", "TCP. Email submission over TLS."),
        Item("514", "Syslog", "UDP. System log messages."),
        Item("587", "SMTP submission", "TCP. Sending email from a client (STARTTLS)."),
        Item("993", "IMAPS", "TCP. IMAP over TLS."),
        Item("995", "POP3S", "TCP. POP3 over TLS."),
        Item("1194", "OpenVPN", "UDP/TCP. VPN."),
        Item("1433", "Microsoft SQL Server", "TCP. Database."),
        Item("1723", "PPTP", "TCP. Old VPN protocol."),
        Item("3306", "MySQL / MariaDB", "TCP. Database."),
        Item("3389", "RDP", "TCP. Windows remote desktop."),
        Item("5060", "SIP", "UDP/TCP. Voice over IP signalling."),
        Item("5432", "PostgreSQL", "TCP. Database."),
        Item("5900", "VNC", "TCP. Remote desktop."),
        Item("6379", "Redis", "TCP. In-memory database."),
        Item("8080", "HTTP alternate", "TCP. Often used for proxies and test servers."),
        Item("8443", "HTTPS alternate", "TCP. Often used for admin panels and test servers."),
        Item("27017", "MongoDB", "TCP. Database."),
        Item("51820", "WireGuard", "UDP. VPN."),
    )

    fun search(items: List<Item>, query: String): List<Item> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items.filter { q in it.key.lowercase() || q in it.name.lowercase() || q in it.detail.lowercase() }
    }
}
