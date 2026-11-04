package com.omnitools.app.tools.html

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

/** Builds the page that runs in the sandbox. Pure Kotlin, unit tested. */
object HtmlDocument {
    /** Everything is blocked except inline script/style and data: or blob: media. No network of any kind. */
    const val POLICY = "default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval'; style-src 'unsafe-inline'; " +
        "img-src data: blob:; font-src data:; media-src data: blob:; connect-src 'none'; frame-src 'none'; " +
        "form-action 'none'; base-uri 'none'"
    const val META = "<meta http-equiv=\"Content-Security-Policy\" content=\"$POLICY\">"

    private val htmlTag = Regex("<html[\\s>]", RegexOption.IGNORE_CASE)

    private fun insertBefore(doc: String, tag: String, insert: String): String {
        val m = Regex(tag, RegexOption.IGNORE_CASE).find(doc) ?: return doc + insert
        return doc.substring(0, m.range.first) + insert + doc.substring(m.range.first)
    }

    /** [html] may be a fragment (it is wrapped) or a whole document (the policy is added at the top of its head). */
    fun build(html: String, css: String, js: String): String {
        val style = if (css.isBlank()) "" else "<style>\n$css\n</style>\n"
        val script = if (js.isBlank()) "" else "<script>\n$js\n</script>\n"
        if (!htmlTag.containsMatchIn(html)) {
            return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">$META" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n$style</head><body>\n$html\n$script</body></html>"
        }
        var doc = html
        val head = Regex("<head(\\s[^>]*)?>", RegexOption.IGNORE_CASE).find(doc)
        doc = if (head != null) {
            doc.substring(0, head.range.last + 1) + META + doc.substring(head.range.last + 1)
        } else {
            val h = htmlTag.find(doc)!!
            val close = doc.indexOf('>', h.range.first)
            doc.substring(0, close + 1) + "<head>$META</head>" + doc.substring(close + 1)
        }
        if (style.isNotEmpty()) doc = insertBefore(doc, "</head>", style)
        if (script.isNotEmpty()) doc = insertBefore(doc, "</body>", script)
        return doc
    }
}

/**
 * A WebView with no way to reach the app or the network: JavaScript runs, but there is no Java bridge, file and
 * content access are off, every request is answered with 403, links cannot navigate away, and the page carries a
 * Content-Security-Policy (see [HtmlDocument.POLICY]) that also covers WebSockets. [runId] reloads the page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxedWebView(document: String, runId: Int, modifier: Modifier = Modifier, onConsole: (String) -> Unit) {
    // The WebView is created once, so its callbacks must always call the newest onConsole.
    val latestConsole by rememberUpdatedState(onConsole)
    // A brand-new WebView for every run, so a run can never show the page from an earlier run.
    key(runId) { AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                with(settings) {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    databaseEnabled = false
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
                    setGeolocationEnabled(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val url = request.url.toString()
                        if (url.startsWith("data:") || url.startsWith("blob:") || url == "about:blank") return null
                        return WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        latestConsole("${message.messageLevel().name.lowercase()}: ${message.message()} (line ${message.lineNumber()})")
                        return true
                    }

                    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
                        latestConsole("alert: $message")
                        result.confirm()
                        return true
                    }

                    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
                        latestConsole("confirm: $message (answered No)")
                        result.cancel()
                        return true
                    }
                }
            }
        },
        update = { web ->
            // Reload whenever the run number OR the code itself changed since this view last loaded.
            val loadedKey = runId to document
            if (web.tag != loadedKey) {
                web.tag = loadedKey
                web.stopLoading()
                web.clearCache(true)
                web.clearHistory()
                web.loadDataWithBaseURL(null, document, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
    ) }
}

/** The running page with its console output underneath. */
@Composable
fun HtmlPreviewPanel(document: String, runId: Int, modifier: Modifier = Modifier) {
    val console = remember(runId) { mutableStateListOf<String>() }
    Column(modifier) {
        SandboxedWebView(
            document = document,
            runId = runId,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onConsole = { line ->
                if (console.size < 300) console.add(line)
            },
        )
        Text(
            "Console (${console.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Column(Modifier.fillMaxWidth().height(110.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (console.isEmpty()) {
                Text("Nothing logged. console.log(...) and errors appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            console.forEach { line ->
                Text(
                    line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (line.startsWith("error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
