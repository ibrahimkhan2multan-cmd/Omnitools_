package com.omnitools.app.tools.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.OmniCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FlashcardsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val file = remember { File(context.filesDir, "flashcards.json") }
    var cards by remember { mutableStateOf<List<Flashcards.Card>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var queue by remember { mutableStateOf<List<Flashcards.Card>?>(null) }
    var showAnswer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cards = withContext(Dispatchers.IO) { try { if (file.exists()) Flashcards.decode(file.readText()) else emptyList() } catch (e: Exception) { emptyList() } }
        loaded = true
    }
    suspend fun persist(list: List<Flashcards.Card>) {
        withContext(Dispatchers.IO) {
            try {
                val tmp = File(file.parentFile, "flashcards.json.tmp")
                tmp.writeText(Flashcards.encode(list))
                if (!tmp.renameTo(file)) { file.writeText(Flashcards.encode(list)); tmp.delete() }
            } catch (e: Exception) { /* storage full: cards stay in memory this session */ }
        }
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun update(list: List<Flashcards.Card>) { cards = list; scope.launch { persist(list) } }

    val q = queue
    ScreenScaffold(title = "Flashcards", onBack = { if (q != null) queue = null else onBack() }) { padding ->
        ToolPage(padding) {
            if (q != null) {
                val current = q.firstOrNull()
                if (current == null) {
                    Text("All done. Nice work!", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { queue = null }, modifier = Modifier.fillMaxWidth()) { Text("Back to cards") }
                } else {
                    Text("${q.size} left", style = MaterialTheme.typography.labelLarge)
                    OmniCard(Modifier.fillMaxWidth().clickable { showAnswer = !showAnswer }) {
                        Column(Modifier.padding(24.dp).heightIn(min = 160.dp), verticalArrangement = Arrangement.Center) {
                            Text(if (showAnswer) "Answer" else "Question", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (showAnswer) current.back else current.front, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    if (!showAnswer) Button(onClick = { showAnswer = true }, modifier = Modifier.fillMaxWidth()) { Text("Show answer") }
                    else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { queue = q.drop(1) + current; showAnswer = false }, modifier = Modifier.weight(1f)) { Text("Again") }
                        Button(onClick = { update(Flashcards.mark(cards, current.id, true)); queue = q.drop(1); showAnswer = false }, modifier = Modifier.weight(1f)) { Text("Got it") }
                    }
                }
            } else {
                val todo = cards.count { !it.known }
                Button(onClick = { queue = Flashcards.studyQueue(cards); showAnswer = false }, enabled = todo > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(if (todo > 0) "Study $todo card${if (todo == 1) "" else "s"}" else "Nothing to study yet")
                }
                if (cards.any { it.known }) TextButton(onClick = { update(cards.map { it.copy(known = false) }) }) { Text("Reset progress (${cards.count { it.known }} learned)") }
                SectionTitle("New card")
                TextInput("Question or front", front, { front = it.take(Flashcards.MAX_TEXT) }, singleLine = false, minLines = 2)
                TextInput("Answer or back", back, { back = it.take(Flashcards.MAX_TEXT) }, singleLine = false, minLines = 2)
                ErrorText(error)
                Button(onClick = {
                    try { update(Flashcards.add(cards, front, back, System.currentTimeMillis())); front = ""; back = ""; error = null }
                    catch (e: IllegalArgumentException) { error = e.message }
                }, modifier = Modifier.fillMaxWidth()) { Text("Add card") }
                if (loaded && cards.isEmpty()) Text("No cards yet. Add a few above, then study them. Cards stay on this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (cards.isNotEmpty()) SectionTitle("Your cards (${cards.size})")
                cards.forEach { c ->
                    OmniCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.front, style = MaterialTheme.typography.titleSmall)
                                Text(c.back + if (c.known) "  ·  learned" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { update(cards.filterNot { it.id == c.id }) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

