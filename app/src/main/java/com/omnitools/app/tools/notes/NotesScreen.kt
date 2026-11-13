package com.omnitools.app.tools.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.omnitools.app.tools.text.TextTools
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.util.Actions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun NotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NotesRepository(context.applicationContext) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }

    suspend fun refresh() {
        notes = repo.list()
        loaded = true
    }
    LaunchedEffect(editing == null) { if (editing == null) refresh() }

    val current = editing
    if (current != null) {
        NoteEditor(repo = repo, initial = current, onClose = { editing = null })
        return
    }

    val visible = notes.filter { NotesRepository.matches(it, query) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    ScreenScaffold(title = "Notepad", onBack = onBack) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search notes") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                when {
                    !loaded -> Unit
                    notes.isEmpty() -> EmptyState("No notes yet. Tap + to write one. Notes stay on this device.")
                    visible.isEmpty() -> EmptyState("No notes match \"${query.trim()}\".")
                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visible, key = { it.id }) { note ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { editing = note },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        note.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                                                            )
                                    if (note.body.isNotBlank()) {
                                        Text(
                                            note.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        dateFormat.format(Date(note.updated)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { editing = Note(NotesRepository.newId(), "", "", System.currentTimeMillis()) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = "New note") }
        }
    }
}

@Composable
private fun NoteEditor(repo: NotesRepository, initial: Note, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(initial.id) { mutableStateOf(initial.title) }
    var body by remember(initial.id) { mutableStateOf(initial.body) }
    var savedTitle by remember(initial.id) { mutableStateOf(initial.title) }
    var savedBody by remember(initial.id) { mutableStateOf(initial.body) }
    var confirmDelete by remember { mutableStateOf(false) }

    suspend fun persist() {
        if (title == savedTitle && body == savedBody) {
            // Brand-new note that was never typed into: make sure nothing is left behind.
            if (title.isBlank() && body.isBlank()) repo.delete(initial.id)
            return
        }
        if (title.isBlank() && body.isBlank()) {
            repo.delete(initial.id)
        } else {
            repo.save(Note(initial.id, title, body, System.currentTimeMillis()))
        }
        savedTitle = title
        savedBody = body
    }

    // Autosave shortly after the user stops typing.
    LaunchedEffect(title, body) {
        delay(700)
        persist()
    }
    // And whenever the app leaves the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { scope.launch { persist() } }

    fun saveAndClose() {
        scope.launch {
            persist()
            onClose()
        }
    }
    BackHandler { saveAndClose() }

    ScreenScaffold(
        title = "Edit note",
        onBack = ::saveAndClose,
        actions = {
            IconButton(onClick = { Actions.copy(context, "Note", body) }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy note text")
            }
            IconButton(onClick = { Actions.shareText(context, if (title.isBlank()) body else "$title\n\n$body") }) {
                Icon(Icons.Rounded.Share, contentDescription = "Share note")
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete note")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
            )
            val stats = remember(body) { TextTools.stats(body) }
            Text(
                "${stats.words} words · ${stats.characters} characters",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        repo.delete(initial.id)
                        savedTitle = title // prevent the autosave from recreating it
                        savedBody = body
                        onClose()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
