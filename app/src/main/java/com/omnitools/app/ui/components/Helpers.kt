package com.omnitools.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.util.Actions

/** Copy and Share buttons for a piece of text. */
@Composable
fun CopyShareRow(text: String, label: String = "Result") {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { Actions.copy(context, label, text); com.omnitools.app.ui.feedback.Feedback.play(com.omnitools.app.ui.feedback.Cue.SUCCESS) }, modifier = Modifier.weight(1f)) { Text("Copy") }
        OutlinedButton(onClick = { Actions.shareText(context, text) }, modifier = Modifier.weight(1f)) { Text("Share") }
    }
}

/** A row of choices that scrolls sideways when it does not fit. */
@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** One selectable option inside a [ChipRow]. */
@Composable
fun Choice(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private const val SAVE_LIMIT = 100_000

/** Keeps long pasted text out of the saved-state bundle (Android crashes the app when it grows past about 1 MB). */
private val BoundedStringSaver: Saver<String, Any> = Saver(save = { if (it.length <= SAVE_LIMIT) it else null }, restore = { it as String })

val BoundedTextFieldValueSaver: Saver<TextFieldValue, Any> = Saver(
    save = { if (it.text.length <= SAVE_LIMIT) with(TextFieldValue.Saver) { save(it) } else null },
    restore = { TextFieldValue.Saver.restore(it) },
)

/** Like rememberSaveable { mutableStateOf("") } but long text is simply not saved. */
@Composable
fun rememberText(initial: String = ""): MutableState<String> = rememberSaveable(stateSaver = BoundedStringSaver) { mutableStateOf(initial) }
