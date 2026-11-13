package com.omnitools.app.tools.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { TasksRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        tasks = repo.load()
        loaded = true
    }

    fun update(new: List<Task>) {
        tasks = new
        scope.launch { repo.save(new) }
    }

    fun add() {
        if (input.isBlank()) return
        update(listOf(TasksRepository.newTask(input)) + tasks)
        input = ""
    }

    val doneCount = tasks.count { it.done }

    ScreenScaffold(
        title = "Task List",
        onBack = onBack,
        actions = {
            if (doneCount > 0) {
                IconButton(onClick = { update(tasks.filterNot { it.done }) }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Remove completed tasks")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("New task") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                trailingIcon = {
                    IconButton(onClick = { add() }, enabled = input.isNotBlank()) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add task")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (tasks.isNotEmpty()) {
                Text(
                    "$doneCount of ${tasks.size} done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                LinearProgressIndicator(progress = { doneCount.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth())
            }
            when {
                !loaded -> Unit
                tasks.isEmpty() -> EmptyState("No tasks yet. Type one above and press Done. Tasks stay on this device.")
                else -> LazyColumn(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.done,
                                onCheckedChange = { checked -> update(tasks.map { if (it.id == task.id) it.copy(done = checked) else it }) },
                            )
                            Text(
                                task.title,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                            )
                            IconButton(onClick = { update(tasks.filterNot { it.id == task.id }) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Delete task")
                            }
                        }
                    }
                }
            }
        }
    }
}
