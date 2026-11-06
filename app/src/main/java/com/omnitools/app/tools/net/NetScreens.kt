package com.omnitools.app.tools.net

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.OmniCard
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun SubnetScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("192.168.1.10/24") }
    var rows: List<Pair<String, String>> = emptyList()
    var error: String? = null
    try {
        val s = Ipv4.subnet(input)
        rows = listOf(
            "Network address" to Ipv4.format(s.network), "Broadcast address" to Ipv4.format(s.broadcast),
            "Subnet mask" to "${Ipv4.format(s.mask)}  (/${s.prefix})", "Wildcard mask" to Ipv4.format(s.mask.inv() and 0xFFFFFFFFL),
            "First usable host" to Ipv4.format(s.firstHost), "Last usable host" to Ipv4.format(s.lastHost),
            "Usable hosts" to "%,d".format(java.util.Locale.US, s.hosts), "Address type" to s.kind, "Class" to s.netClass,
            "Mask in binary" to Ipv4.binary(s.mask),
        )
    } catch (e: IllegalArgumentException) { if (input.isNotBlank()) error = e.message }
    ScreenScaffold(title = "Subnet Calculator", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Address with prefix (192.168.1.10/24) or mask", input, { input = it.take(60) })
            ErrorText(error)
            rows.forEach { (k, v) -> ResultCard(k, v) }
            if (rows.isNotEmpty()) CopyShareRow(rows.joinToString("\n") { "${it.first}: ${it.second}" }, "Subnet")
            Note("IPv4 only. All maths happens on your phone; nothing is looked up online.")
        }
    }
}

@Composable
fun IpConvertScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("192.168.1.1") }
    var rows: List<Pair<String, String>> = emptyList()
    var error: String? = null
    try { if (input.isNotBlank()) rows = IpConvert.describe(input) } catch (e: IllegalArgumentException) { error = e.message }
    ScreenScaffold(title = "IP Converter", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("IPv4, IPv6, decimal number or 0x hex", input, { input = it.take(60) })
            ErrorText(error)
            rows.forEach { (k, v) -> ResultCard(k, v) }
            if (rows.isNotEmpty()) CopyShareRow(rows.joinToString("\n") { "${it.first}: ${it.second}" }, "IP address")
            Note("Convert between dotted, decimal, hex and binary forms, and expand or shorten IPv6 addresses.")
        }
    }
}

@Composable
fun UrlPartsScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("https://user@example.com:8443/a/b.html?q=hello%20world&x=1#top") }
    var parsed: UrlParts.Parsed? = null
    var error: String? = null
    try { if (input.isNotBlank()) parsed = UrlParts.parse(input) } catch (e: IllegalArgumentException) { error = e.message }
    ScreenScaffold(title = "URL Parser", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("Web address", input, { input = it.take(8000) }, singleLine = false, minLines = 2)
            ErrorText(error)
            parsed?.let { p ->
                ResultCard("Scheme", p.scheme)
                p.host?.let { ResultCard("Host", it) }
                p.port?.let { ResultCard("Port", it.toString()) }
                p.userInfo?.let { ResultCard("User info", it) }
                if (p.path.isNotEmpty()) ResultCard("Path", p.path)
                p.fragment?.let { ResultCard("Fragment", it) }
                if (p.query.isNotEmpty()) {
                    SectionTitle("Query parameters")
                    p.query.forEach { (k, v) -> ResultCard(k.ifEmpty { "(empty name)" }, v.ifEmpty { "(empty)" }) }
                }
                if (p.userInfo != null) Text("A user name inside an address is a common phishing trick. Check the real host above.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Note("Splits the address only. Nothing is opened or contacted.")
        }
    }
}

@Composable
fun ReferenceScreen(title: String, hint: String, items: List<NetReference.Item>, onBack: () -> Unit) {
    var query by rememberSaveable(title) { mutableStateOf("") }
    val shown = NetReference.search(items, query)
    ScreenScaffold(title = title, onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput(hint, query, { query = it.take(40) })
            if (shown.isEmpty()) Note("Nothing matches \"$query\".")
            shown.forEach { item ->
                OmniCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("${item.key}  ${item.name}", style = MaterialTheme.typography.titleSmall)
                        Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
