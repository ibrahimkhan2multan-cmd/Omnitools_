package com.omnitools.app.tools.text

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.Actions

private val TABS = listOf("Style", "Case", "Lines", "Encode", "Find & replace")
private const val MAX_UNDO = 50

/**
 * One text box. Tap a button and the text in the box changes right there (only the selected part if some text is
 * selected). Undo and Redo step through the changes made by the buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var value by rememberSaveable(stateSaver = com.omnitools.app.ui.components.BoundedTextFieldValueSaver) { mutableStateOf(TextFieldValue("")) }
    val undo = remember { mutableStateListOf<TextFieldValue>() }
    val redo = remember { mutableStateListOf<TextFieldValue>() }
    var tab by rememberSaveable { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var find by rememberSaveable { mutableStateOf("") }
    var replace by rememberSaveable { mutableStateOf("") }
    var ignoreCase by rememberSaveable { mutableStateOf(true) }

    fun say(text: String, error: Boolean = false) {
        message = text
        isError = error
    }

    fun commit(old: TextFieldValue, new: TextFieldValue) {
        undo.add(old)
        if (undo.size > MAX_UNDO) undo.removeAt(0)
        redo.clear()
        value = new
    }

    /** Runs [action] on the selected text, or on everything when nothing is selected. */
    fun transform(action: (String) -> String) {
        val old = value
        if (old.text.isEmpty()) {
            say("Type or paste some text first.", error = true)
            return
        }
        val sel = old.selection
        val useSelection = !sel.collapsed
        try {
            val new = if (useSelection) {
                val a = sel.min
                val b = sel.max
                val mid = action(old.text.substring(a, b))
                TextFieldValue(old.text.substring(0, a) + mid + old.text.substring(b), TextRange(a, a + mid.length))
            } else {
                val result = action(old.text)
                TextFieldValue(result, TextRange(result.length))
            }
            if (new.text == old.text) {
                say("Nothing to change.")
                return
            }
            commit(old, new)
            say(if (useSelection) "Changed the selected text." else "Changed all the text.")
        } catch (e: TextTools.TextToolException) {
            say(e.message ?: "That did not work.", error = true)
        } catch (e: Exception) {
            say("Something went wrong: ${e.message}", error = true)
        }
    }

    fun doUndo() {
        if (undo.isEmpty()) return
        redo.add(value)
        value = undo.removeAt(undo.lastIndex)
        say("Undone.")
    }

    fun doRedo() {
        if (redo.isEmpty()) return
        undo.add(value)
        value = redo.removeAt(redo.lastIndex)
        say("Redone.")
    }

    val stats = remember(value.text) { TextTools.stats(value.text) }

    ScreenScaffold(
        title = "Text Tools",
        onBack = onBack,
        actions = {
            IconButton(onClick = ::doUndo, enabled = undo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo") }
            IconButton(onClick = ::doRedo, enabled = redo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo") }
            IconButton(onClick = { Actions.copy(context, "Text", value.text); say("Copied.") }, enabled = value.text.isNotEmpty()) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy all")
            }
            IconButton(onClick = { Actions.shareText(context, value.text) }, enabled = value.text.isNotEmpty()) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(
                onClick = {
                    if (value.text.isNotEmpty()) {
                        commit(value, TextFieldValue(""))
                        say("Cleared. Tap Undo to bring it back.")
                    }
                },
                enabled = value.text.isNotEmpty(),
            ) { Icon(Icons.Filled.Delete, contentDescription = "Clear") }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Type or paste your text") },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )

            Column(Modifier.animateContentSize()) {
                val selected = value.selection.length
                Text(
                    "${stats.words} words · ${stats.characters} characters · ${stats.lines} lines" +
                        if (selected > 0) " · $selected selected" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedContent(
                    targetState = message,
                    transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() },
                    label = "message",
                ) { m ->
                    Text(
                        m ?: "Select part of the text to change only that part, or tap a button to change everything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            m == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TABS.forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
                }
            }

            Column(
                Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (tab) {
                    0 -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FancyText.Style.values().forEach { style ->
                                // The label is shown in its own style so you can see what you will get.
                                Chip(FancyText.apply(style, style.label)) { transform { FancyText.apply(style, it) } }
                            }
                            Chip("Remove styles") { transform { FancyText.plain(it) } }
                        }
                        Text(
                            "Bold, italic and the others are special characters, not real formatting, so you can paste " +
                                "them into chats and posts. They change English letters and digits only, and some apps or " +
                                "fonts may show boxes instead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    1 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Chip("UPPERCASE") { transform(TextTools::upper) }
                        Chip("lowercase") { transform(TextTools::lower) }
                        Chip("Title Case") { transform(TextTools::titleCase) }
                        Chip("Sentence case") { transform(TextTools::sentenceCase) }
                        Chip("tOGGLE cASE") { transform(TextTools::toggleCase) }
                        Chip("aLtErNaTiNg") { transform(TextTools::alternatingCase) }
                        Chip("camelCase") { transform(TextTools::camelCase) }
                        Chip("snake_case") { transform(TextTools::snakeCase) }
                        Chip("kebab-case") { transform(TextTools::kebabCase) }
                    }
                    2 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Chip("Trim spaces") { transform(TextTools::removeExtraWhitespace) }
                        Chip("Remove empty lines") { transform(TextTools::removeEmptyLines) }
                        Chip("Join into one line") { transform(TextTools::removeLineBreaks) }
                        Chip("Remove duplicates") { transform(TextTools::removeDuplicateLines) }
                        Chip("Sort A→Z") { transform { TextTools.sortLines(it) } }
                        Chip("Sort Z→A") { transform { TextTools.sortLines(it, descending = true) } }
                        Chip("Reverse text") { transform(TextTools::reverseText) }
                        Chip("Number lines") { transform(TextTools::numberLines) }
                        Chip("Add bullets") { transform(TextTools::addBullets) }
                    }
                    3 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Chip("Base64 encode") { transform(TextTools::base64Encode) }
                        Chip("Base64 decode") { transform(TextTools::base64Decode) }
                        Chip("URL encode") { transform(TextTools::urlEncode) }
                        Chip("URL decode") { transform(TextTools::urlDecode) }
                        Chip("JSON format") { transform(TextTools::jsonFormat) }
                        Chip("JSON minify") { transform(TextTools::jsonMinify) }
                    }
                    else -> {
                        OutlinedTextField(find, { find = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(replace, { replace = it }, label = { Text("Replace with") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = ignoreCase, onCheckedChange = { ignoreCase = it })
                            Text("Ignore case")
                        }
                        OutlinedButton(
                            onClick = {
                                var count = 0
                                transform { part ->
                                    val (text, n) = TextTools.findReplace(part, find, replace, ignoreCase)
                                    count = n
                                    text
                                }
                                if (!isError && count > 0) say("Replaced $count occurrence${if (count == 1) "" else "s"}.")
                                else if (!isError) say("No matches for \"$find\".")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Replace all") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}
