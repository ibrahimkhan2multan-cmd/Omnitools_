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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.OutlinedButton
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

/** The language runtimes that ship inside the app (see scripts/fetch-runtimes.sh) and how they are served to the page. */
object RuntimeAssets {
    const val ORIGIN = "https://omnitools.local"
    const val PREFIX = "$ORIGIN/rt/"
    const val PAGE_BASE = "$ORIGIN/page/"

    /** Only these exact files can be requested; anything else is refused. */
    val files: Set<String> = setOf(
        "pyodide.js", "pyodide.asm.mjs", "pyodide.asm.wasm", "python_stdlib.zip", "pyodide-lock.json",
        "sql-wasm.js", "sql-wasm.wasm", "fengari-web.js", "sucrase.js",
    )

    fun nameOf(url: String): String? =
        if (url.startsWith(PREFIX)) url.removePrefix(PREFIX).substringBefore('?').substringBefore('#').takeIf { it in files } else null

    fun mime(name: String): String = when (name.substringAfterLast('.')) {
        "js", "mjs" -> "text/javascript"
        "wasm" -> "application/wasm"
        "json" -> "application/json"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    fun isText(name: String): Boolean = name.substringAfterLast('.') in setOf("js", "mjs", "json")
}

/**
 * A WebView with no way to reach the app or the network: JavaScript runs, but there is no Java bridge, file and
 * content access are off, every request is answered with 403, links cannot navigate away, and the page carries a
 * Content-Security-Policy (see [HtmlDocument.POLICY]) that also covers WebSockets. [runId] reloads the page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SandboxedWebView(
    document: String,
    runId: Int,
    modifier: Modifier = Modifier,
    handle: SandboxHandle? = null,
    runtime: Boolean = false,
    onConsole: (String) -> Unit,
) {
    // The WebView is created once, so its callbacks must always call the newest onConsole.
    val latestConsole by rememberUpdatedState(onConsole)
    // A brand-new WebView for every run, so a run can never show the page from an earlier run.
    key(runId) { AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                handle?.web = this
                // Touch games: taps must reach the page. Keep the view focused, stop parents from stealing the
                // gesture, and turn off pinch / double-tap zoom, which swallows rapid taps.
                isFocusable = true
                isFocusableInTouchMode = true
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            v.requestFocus()
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
                with(settings) {
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
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
                        // Language pages may load the bundled runtimes, and nothing else.
                        if (runtime) {
                            val name = RuntimeAssets.nameOf(url)
                            if (name != null) {
                                return try {
                                    WebResourceResponse(
                                        RuntimeAssets.mime(name), if (RuntimeAssets.isText(name)) "utf-8" else null, 200, "OK",
                                        mapOf("Cache-Control" to "no-store"), view.context.assets.open("rt/$name"),
                                    )
                                } catch (e: java.io.IOException) {
                                    WebResourceResponse("text/plain", "utf-8", 404, "Not found", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                                }
                            }
                        }
                        return WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

                    override fun onPageFinished(view: WebView, url: String?) { view.requestFocus() }
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
                web.loadDataWithBaseURL(if (runtime) RuntimeAssets.PAGE_BASE else null, document, "text/html", "utf-8", null)
            }
        },
        onRelease = { if (handle?.web === it) handle.web = null; it.destroy() },
    ) }
}

/** Lets the screen talk to the running page (key presses for games). */
class SandboxHandle {
    var web: WebView? = null
    fun sendKey(key: String, code: String, keyCode: Int) {
        web?.evaluateJavascript(GameKeys.script(key, code, keyCode), null)
    }
}

/** The running page with its console output underneath. */
@Composable
fun HtmlPreviewPanel(document: String, runId: Int, modifier: Modifier = Modifier, runtime: Boolean = false) {
    val console = remember(runId) { mutableStateListOf<String>() }
    val handle = remember { SandboxHandle() }
    Column(modifier) {
        SandboxedWebView(
            document = document,
            runId = runId,
            handle = handle,
            runtime = runtime,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onConsole = { line ->
                if (console.size < 300) console.add(line)
            },
        )
        KeyBar(handle)
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

/** On-screen keys for games that listen for the keyboard instead of touch. */
@Composable
private fun KeyBar(handle: SandboxHandle) {
    val keys = listOf(
        Triple("Space", " " to "Space", 32), Triple("\u2190", "ArrowLeft" to "ArrowLeft", 37), Triple("\u2191", "ArrowUp" to "ArrowUp", 38),
        Triple("\u2193", "ArrowDown" to "ArrowDown", 40), Triple("\u2192", "ArrowRight" to "ArrowRight", 39), Triple("Enter", "Enter" to "Enter", 13),
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.forEach { (label, kc, num) ->
            OutlinedButton(
                onClick = { handle.sendKey(kc.first, kc.second, num) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
            ) { Text(label, style = MaterialTheme.typography.labelMedium) }
        }
    }
}
