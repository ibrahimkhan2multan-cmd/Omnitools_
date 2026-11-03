package com.omnitools.app.tools.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omnitools.app.tools.editor.SyntaxHighlighter.Kind
import com.omnitools.app.tools.editor.SyntaxHighlighter.Language
import com.omnitools.app.tools.html.HtmlDocument
import com.omnitools.app.tools.html.HtmlPreviewPanel
import com.omnitools.app.tools.text.TextTools
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_HIGHLIGHT_CHARS = 300_000

@Composable
fun CodeEditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var value by remember { mutableStateOf(TextFieldValue("")) }
    var savedText by remember { mutableStateOf("") }
    var docUri by remember { mutableStateOf<Uri?>(null) }
    var docName by remember { mutableStateOf("untitled.txt") }
    var crlf by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(Language.PLAIN) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showFind by remember { mutableStateOf(false) }
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var languageMenu by remember { mutableStateOf(false) }
    var previewRun by remember { mutableStateOf(0) }
    var showPreview by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0) } // bumped when the undo history changes, so the buttons refresh
    val history = remember { UndoHistory("") }

    val dirty = value.text != savedText

    fun say(text: String, error: Boolean = false) {
        message = text
        isError = error
    }

    // Remember the text as an undo step once typing pauses.
    LaunchedEffect(value.text) {
        delay(600)
        if (history.record(value.text)) tick++
    }

    fun loadText(text: String, uri: Uri?, name: String, windows: Boolean) {
        value = TextFieldValue(text)
        savedText = text
        docUri = uri
        docName = name
        crlf = windows
        language = SyntaxHighlighter.languageFor(name)
        history.reset(text)
        tick++
        message = null
    }

    fun guard(action: () -> Unit) {
        if (dirty) pendingAction = action else action()
    }

    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw java.io.IOException("The file cannot be written.")
        out.use { it.write(EditorText.encode(text, crlf)) }
    }

    val openPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val (name, raw) = withContext(Dispatchers.IO) {
                        val details = FileInfo.query(context, uri)
                        if ((details.sizeBytes ?: 0L) > EditorText.MAX_BYTES) {
                            throw EditorText.EditorException("This file is larger than 1 MB, which is more than the editor can handle comfortably.")
                        }
                        val bytes = context.contentResolver.openInputStream(uri)?.use { EditorText.readLimited(it) }
                            ?: throw EditorText.EditorException("Could not open the file.")
                        (details.name ?: "file") to EditorText.decodeUtf8(bytes)
                    }
                    val (text, windows) = EditorText.normalizeNewlines(raw)
                    loadText(text, uri, name, windows)
                    say("Opened $name.")
                } catch (e: EditorText.EditorException) {
                    say(e.message ?: "Could not open the file.", error = true)
                } catch (e: Exception) {
                    say("Could not open the file: ${e.message ?: "unknown error"}", error = true)
                }
            }
        }
    }

    val saveAsPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) {
            val text = value.text
            scope.launch {
                try {
                    write(uri, text)
                    val name = withContext(Dispatchers.IO) { FileInfo.query(context, uri).name } ?: docName
                    docUri = uri
                    docName = name
                    savedText = text
                    language = SyntaxHighlighter.languageFor(name)
                    say("Saved $name.")
                } catch (e: Exception) {
                    say("Could not save: ${e.message ?: "unknown error"}", error = true)
                }
            }
        }
    }

    fun save() {
        val uri = docUri
        if (uri == null) {
            saveAsPicker.launch(docName)
            return
        }
        val text = value.text
        scope.launch {
            try {
                write(uri, text)
                savedText = text
                say("Saved $docName.")
            } catch (e: Exception) {
                say("Could not save here (${e.message ?: "no permission"}). Use Save as to pick a new place.", error = true)
            }
        }
    }

    fun undo() {
        if (history.record(value.text)) tick++
        history.undo()?.let { value = TextFieldValue(it, TextRange(it.length)); tick++ }
    }

    fun redo() {
        history.redo()?.let { value = TextFieldValue(it, TextRange(it.length)); tick++ }
    }

    fun findNext() {
        if (find.isEmpty()) return
        val t = value.text
        var i = t.indexOf(find, value.selection.max, ignoreCase = !matchCase)
        if (i < 0) i = t.indexOf(find, 0, ignoreCase = !matchCase)
        if (i < 0) {
            say("\"$find\" was not found.", error = true)
        } else {
            value = value.copy(selection = TextRange(i, i + find.length))
            message = null
        }
    }

    fun replaceAll() {
        try {
            if (history.record(value.text)) tick++
            val (text, count) = TextTools.findReplace(value.text, find, replace, ignoreCase = !matchCase)
            if (count > 0) {
                value = TextFieldValue(text, TextRange(0))
                say("Replaced $count occurrence${if (count == 1) "" else "s"}.")
            } else {
                say("\"$find\" was not found.", error = true)
            }
        } catch (e: TextTools.TextToolException) {
            say(e.message ?: "Enter text to find.", error = true)
        }
    }

    BackHandler(enabled = dirty) { pendingAction = onBack }

    val canUndo = tick >= 0 && (history.canUndo || value.text != history.current)
    val canRedo = tick >= 0 && history.canRedo

    ScreenScaffold(
        title = "Code Editor",
        onBack = { guard(onBack) },
        actions = {
            IconButton(onClick = ::undo, enabled = canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo") }
            IconButton(onClick = ::redo, enabled = canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo") }
            IconButton(onClick = ::save) { Icon(Icons.Filled.Save, contentDescription = "Save") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(onClick = { guard { openPicker.launch(arrayOf("*/*")) } }, label = { Text("Open") })
                AssistChip(onClick = { guard { loadText("", null, "untitled.txt", false) } }, label = { Text("New") })
                AssistChip(onClick = { saveAsPicker.launch(docName) }, label = { Text("Save as") })
                AssistChip(onClick = { showFind = !showFind }, label = { Text("Find & replace") })
                Box {
                    AssistChip(onClick = { languageMenu = true }, label = { Text(language.label) })
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        Language.values().forEach { l ->
                            DropdownMenuItem(text = { Text(l.label) }, onClick = { language = l; languageMenu = false })
                        }
                    }
                }
                if (language == Language.HTML || language == Language.JAVASCRIPT) {
                    AssistChip(onClick = { previewRun++; showPreview = true }, label = { Text("Preview ▶") })
                }
            }

            AnimatedVisibility(visible = showFind) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(find, { find = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = ::findNext) { Text("Next") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(replace, { replace = it }, label = { Text("Replace with") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = ::replaceAll) { Text("Replace all") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = matchCase, onCheckedChange = { matchCase = it })
                        Text("Match case")
                    }
                }
            }

            CodeField(value, { value = it }, language, Modifier.weight(1f).fillMaxWidth())

            val (line, col) = EditorText.lineAndColumn(value.text, value.selection.start)
            val lines = value.text.count { it == '\n' } + 1
            Text(
                (message ?: "$docName${if (dirty) " ●" else ""}") + "   ·   Ln $line, Col $col   ·   $lines lines" +
                    if (docUri == null) "   ·   not saved yet" else "",
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
                maxLines = 2,
            )
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Discard unsaved changes?") },
            text = { Text("$docName has changes that are not saved.") },
            confirmButton = { TextButton(onClick = { pendingAction = null; action() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { pendingAction = null }) { Text("Keep editing") } },
        )
    }

    if (showPreview) {
        val document = if (language == Language.HTML) HtmlDocument.build(value.text, "", "") else HtmlDocument.build("", "", value.text)
        Dialog(onDismissRequest = { showPreview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Preview (no internet, no file access)", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { showPreview = false }) { Icon(Icons.Filled.Close, contentDescription = "Close preview") }
                    }
                    HtmlPreviewPanel(document, previewRun, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

private class HighlightTransformation(
    private val language: Language,
    private val styles: Map<Kind, SpanStyle>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (language == Language.PLAIN || text.length > MAX_HIGHLIGHT_CHARS) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text.text)
        for (span in SyntaxHighlighter.highlight(text.text, language)) {
            styles[span.kind]?.let { builder.addStyle(it, span.start, span.end) }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
private fun CodeField(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, language: Language, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, color = scheme.onSurface)
    val styles = remember(dark) {
        mapOf(
            Kind.COMMENT to SpanStyle(color = if (dark) Color(0xFF7C8AA5) else Color(0xFF6B7794)),
            Kind.STRING to SpanStyle(color = if (dark) Color(0xFF7EE0A0) else Color(0xFF1B7F4C)),
            Kind.NUMBER to SpanStyle(color = if (dark) Color(0xFFFFB86C) else Color(0xFFB35C00)),
            Kind.KEYWORD to SpanStyle(color = if (dark) Color(0xFF8FB4FF) else Color(0xFF2F5DD0), fontWeight = FontWeight.Bold),
            Kind.TAG to SpanStyle(color = if (dark) Color(0xFFFF7EB6) else Color(0xFFC22A73)),
        )
    }
    val transformation = remember(language, styles) { HighlightTransformation(language, styles) }
    val lineCount = value.text.count { it == '\n' } + 1
    val numbers = remember(lineCount) { (1..lineCount).joinToString("\n") }

    BoxWithConstraints(modifier.background(scheme.surface)) {
        val minWidth = maxWidth
        val minHeight = maxHeight
        Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                numbers,
                style = textStyle.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.End),
                modifier = Modifier.background(scheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(scheme.primary),
                    visualTransformation = transformation,
                    softWrap = false,
                    modifier = Modifier.defaultMinSize(minWidth = minWidth, minHeight = minHeight).padding(8.dp),
                )
            }
        }
    }
}
