package com.omnitools.app.tools.gaming

import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.omnitools.app.tools.files.FileEntry
import com.omnitools.app.tools.files.SafWalker
import com.omnitools.app.ui.components.Choice
import com.omnitools.app.ui.components.ChipRow
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage
import com.omnitools.app.ui.components.rememberText
import com.omnitools.app.util.Format
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun UuidScreen(onBack: () -> Unit) {
    var count by rememberSaveable { mutableStateOf(1) }
    var upper by rememberSaveable { mutableStateOf(false) }
    var dashes by rememberSaveable { mutableStateOf(true) }
    var braces by rememberSaveable { mutableStateOf(false) }
    var seed by rememberSaveable { mutableStateOf(0) }
    val list = remember(count, upper, dashes, braces, seed) { Uuids.many(count, upper, dashes, braces) }
    ScreenScaffold(title = "UUID Generator", onBack = onBack) { padding ->
        ToolPage(padding) {
            ChipRow { listOf(1, 2, 5, 10, 25).forEach { n -> Choice(count == n, if (n == 1) "1 UUID" else "$n UUIDs") { count = n } } }
            ChipRow {
                Choice(upper, "UPPERCASE") { upper = !upper }
                Choice(dashes, "Dashes") { dashes = !dashes }
                Choice(braces, "{Braces}") { braces = !braces }
            }
            Button(onClick = { seed++ }, modifier = Modifier.fillMaxWidth()) { Text("Generate new") }
            list.forEach { ResultCard("UUID", it) }
            CopyShareRow(list.joinToString("\n"), "UUIDs")
            Note("Random version 4 UUIDs from the phone's secure generator. Add-on packs, mods and databases use them as unique IDs. Every pack needs its own: never reuse one.")
        }
    }
}

@Composable
fun ManifestScreen(onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("My Pack") }
    var desc by rememberSaveable { mutableStateOf("Made with OmniTools") }
    var kind by rememberSaveable { mutableStateOf(AddonManifest.Kind.BEHAVIOR) }
    var version by rememberSaveable { mutableStateOf("1.0.0") }
    var engine by rememberSaveable { mutableStateOf("1.20.0") }
    var headerUuid by rememberSaveable { mutableStateOf(Uuids.random()) }
    var moduleUuid by rememberSaveable { mutableStateOf(Uuids.random()) }
    var depends by rememberSaveable { mutableStateOf(false) }
    var depUuid by rememberSaveable { mutableStateOf("") }
    val computed = remember<Pair<String?, String?>>(name, desc, kind, version, engine, headerUuid, moduleUuid, depends, depUuid) {
        try {
            AddonManifest.build(AddonManifest.Input(
                name, desc, kind, AddonManifest.parseVersion(version), AddonManifest.parseVersion(engine), headerUuid, moduleUuid,
                if (depends) depUuid.trim() else null,
            )) to null
        } catch (e: IllegalArgumentException) { null to e.message }
    }
    val json = computed.first
    ScreenScaffold(title = "Add-on Manifest Builder", onBack = onBack) { padding ->
        ToolPage(padding) {
            ChipRow { AddonManifest.Kind.values().forEach { k -> Choice(kind == k, k.label) { kind = k } } }
            TextInput("Pack name", name, { name = it.take(100) })
            TextInput("Description", desc, { desc = it.take(300) })
            TextInput("Pack version (1.0.0)", version, { version = it.filter { c -> c.isDigit() || c == '.' }.take(14) }, keyboardType = KeyboardType.Decimal)
            TextInput("Minimum game version (1.20.0)", engine, { engine = it.filter { c -> c.isDigit() || c == '.' }.take(14) }, keyboardType = KeyboardType.Decimal)
            TextInput("Pack UUID", headerUuid, { headerUuid = it.trim().take(36) })
            OutlinedButton(onClick = { headerUuid = Uuids.random() }, modifier = Modifier.fillMaxWidth()) { Text("New pack UUID") }
            TextInput("Module UUID", moduleUuid, { moduleUuid = it.trim().take(36) })
            OutlinedButton(onClick = { moduleUuid = Uuids.random() }, modifier = Modifier.fillMaxWidth()) { Text("New module UUID") }
            ChipRow { Choice(depends, "Depends on another pack") { depends = !depends } }
            if (depends) TextInput("UUID of the other pack (e.g. your resource pack)", depUuid, { depUuid = it.trim().take(36) })
            ErrorText(computed.second)
            if (json != null) {
                SectionTitle("manifest.json")
                ResultCard("Ready to save as manifest.json", json)
                CopyShareRow(json, "manifest.json")
            }
            Note("Makes a Minecraft Bedrock add-on manifest (format version 2). Put the file at the top level of your pack folder. Script modules are not included.")
        }
    }
}

@Composable
fun ColorCodesScreen(onBack: () -> Unit) {
    var text by rememberText("&6&lWelcome &r&ato &b&nmy server")
    var marker by rememberSaveable { mutableStateOf('&') }
    val spans = remember(text, marker) { McFormat.parse(text, marker) }
    val annotated = remember(spans) {
        buildAnnotatedString {
            spans.forEach { s ->
                val deco = listOfNotNull(if (s.underline) TextDecoration.Underline else null, if (s.strike) TextDecoration.LineThrough else null)
                withStyle(SpanStyle(
                    color = Color(0xFF000000.toInt() or (s.rgb ?: 0xFFFFFF)),
                    fontWeight = if (s.bold) FontWeight.Bold else null,
                    fontStyle = if (s.italic) FontStyle.Italic else null,
                    textDecoration = if (deco.isEmpty()) null else TextDecoration.combine(deco),
                )) { append(s.text) }
            }
        }
    }
    val converted = remember(text, marker) { if (marker == '&') McFormat.toSection(text) else text }
    ScreenScaffold(title = "Color Code Preview", onBack = onBack) { padding ->
        ToolPage(padding) {
            ChipRow { Choice(marker == '&', "& codes") { marker = '&' }; Choice(marker == '§', "§ codes") { marker = '§' } }
            TextInput("Text with colour codes (&a green, &l bold, &r reset)", text, { text = it.take(2000) }, singleLine = false, minLines = 3)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B1B1B)).padding(16.dp)) {
                Text(annotated, style = MaterialTheme.typography.titleMedium)
            }
            if (spans.any { it.obfuscated }) Note("The &k (scrambled) effect animates in the game; it is shown as normal text here.")
            ResultCard("For game files (uses the § sign)", converted)
            ResultCard("Plain text without codes", McFormat.strip(text, marker))
            CopyShareRow(converted, "Formatted text")
            Note("Codes: 0-9 and a-f are colours, l bold, o italic, n underline, m strikethrough, k scrambled, r reset. A colour code also clears earlier styles, as in the game.")
        }
    }
}

@Composable
fun CoordsScreen(onBack: () -> Unit) {
    var xs by rememberSaveable { mutableStateOf("100") }
    var ys by rememberSaveable { mutableStateOf("64") }
    var zs by rememberSaveable { mutableStateOf("-250") }
    var x2 by rememberSaveable { mutableStateOf("") }
    var y2 by rememberSaveable { mutableStateOf("") }
    var z2 by rememberSaveable { mutableStateOf("") }
    val sign = { s: String -> s.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(9) }
    var error: String? = null
    var r: McCoords.Result? = null
    var x = 0L; var y = 0L; var z = 0L
    try { x = McCoords.parse(xs, "X"); y = McCoords.parse(ys, "Y"); z = McCoords.parse(zs, "Z"); r = McCoords.compute(x, z) } catch (e: IllegalArgumentException) { error = if (xs.isBlank() || zs.isBlank()) null else e.message }
    val second = if (x2.isNotBlank() && z2.isNotBlank()) try {
        val bx = McCoords.parse(x2, "Second X"); val bz = McCoords.parse(z2, "Second Z"); val by = if (y2.isBlank()) y else McCoords.parse(y2, "Second Y")
        Triple(McCoords.distance2d(x, z, bx, bz), McCoords.distance3d(x, y, z, bx, by, bz), null as String?)
    } catch (e: IllegalArgumentException) { Triple(0.0, 0.0, e.message) } else null
    ScreenScaffold(title = "Coordinates & Chunks", onBack = onBack) { padding ->
        ToolPage(padding) {
            TextInput("X", xs, { xs = sign(it) }, keyboardType = KeyboardType.Number)
            TextInput("Y (height, optional)", ys, { ys = sign(it) }, keyboardType = KeyboardType.Number)
            TextInput("Z", zs, { zs = sign(it) }, keyboardType = KeyboardType.Number)
            ErrorText(error)
            r?.let {
                SectionTitle("Dimensions")
                ResultCard("Nether equivalent", "${it.nether.first}, ${it.nether.second}")
                ResultCard("Overworld equivalent (if these are Nether coordinates)", "${it.overworld.first}, ${it.overworld.second}")
                SectionTitle("Chunk")
                ResultCard("Chunk", "${it.chunk.first}, ${it.chunk.second}")
                ResultCard("Position inside the chunk", "${it.inChunk.first}, ${it.inChunk.second}")
                ResultCard("Chunk starts at", "${it.chunkOrigin.first}, ${it.chunkOrigin.second}")
                ResultCard("Region file", it.regionFile)
                CopyShareRow("X ${xs} Y ${ys} Z ${zs}\nNether: ${it.nether.first}, ${it.nether.second}\nChunk: ${it.chunk.first}, ${it.chunk.second}\nRegion: ${it.regionFile}", "Coordinates")
            }
            SectionTitle("Distance to a second point (optional)")
            TextInput("Second X", x2, { x2 = sign(it) }, keyboardType = KeyboardType.Number)
            TextInput("Second Y (leave empty to ignore height)", y2, { y2 = sign(it) }, keyboardType = KeyboardType.Number)
            TextInput("Second Z", z2, { z2 = sign(it) }, keyboardType = KeyboardType.Number)
            second?.let {
                if (it.third != null) ErrorText(it.third) else {
                    ResultCard("Distance (flat)", String.format(Locale.US, "%.1f blocks", it.first))
                    ResultCard("Distance (with height)", String.format(Locale.US, "%.1f blocks", it.second))
                }
            }
            Note("Uses Minecraft's rules: 1 Nether block = 8 Overworld blocks, a chunk is 16 x 16 blocks and a region file holds 32 x 32 chunks. Negative coordinates are handled correctly.")
        }
    }
}

@Composable
fun PackZipScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tree by remember { mutableStateOf<Uri?>(null) }
    var folderName by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var truncated by remember { mutableStateOf(false) }
    var ext by rememberSaveable { mutableStateOf("mcpack") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { t ->
        if (t != null) scope.launch {
            busy = true; error = null
            val walk = withContext(Dispatchers.IO) { SafWalker.walk(context.contentResolver, t, PackZip.MAX_FILES, { false }, { }) }
            tree = t; entries = walk.entries; truncated = walk.truncated
            folderName = try {
                context.contentResolver.query(DocumentsContract.buildDocumentUriUsingTree(t, DocumentsContract.getTreeDocumentId(t)), arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            } catch (e: Exception) { null } ?: "pack"
            busy = false
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(if (ext == "zip") "application/zip" else "application/octet-stream")) { dest ->
        val t = tree
        if (dest != null && t != null) scope.launch {
            busy = true; error = null; progress = 0
            try {
                val n = withContext(Dispatchers.IO) {
                    val out = context.contentResolver.openOutputStream(dest, "w") ?: throw java.io.IOException("Could not open the destination file.")
                    var count = 0
                    var total = 0L
                    val seen = HashSet<String>()
                    ZipOutputStream(out.buffered()).use { zip ->
                        val buf = ByteArray(64 * 1024)
                        for (e in entries) {
                            val name = PackZip.safeEntryName(e.path) ?: continue
                            if (!seen.add(name)) continue
                            zip.putNextEntry(ZipEntry(name))
                            context.contentResolver.openInputStream(SafWalker.documentUri(t, e.id))?.use { input ->
                                while (true) {
                                    val r = input.read(buf); if (r < 0) break
                                    total += r
                                    if (total > PackZip.MAX_BYTES) throw java.io.IOException("The folder is larger than ${Format.bytes(PackZip.MAX_BYTES)}.")
                                    zip.write(buf, 0, r)
                                }
                            }
                            zip.closeEntry()
                            count++
                            if (count % 20 == 0) progress = count
                        }
                    }
                    count
                }
                Toast.makeText(context, "Saved $n files", Toast.LENGTH_SHORT).show()
            } catch (e: java.io.IOException) { error = e.message ?: "Could not write the file."
            } catch (e: Exception) { error = "Could not create the archive: ${e.message ?: "unknown error"}"
            } catch (e: OutOfMemoryError) { error = "Not enough memory." }
            busy = false
        }
    }
    val warning = remember(entries) { if (entries.isEmpty()) null else PackZip.manifestWarning(entries.map { it.path }) }
    ScreenScaffold(title = "Pack Builder", onBack = onBack) { padding ->
        ToolPage(padding) {
            Button(onClick = { pick.launch(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (tree == null) "Choose your pack folder" else "Choose another folder") }
            ErrorText(error)
            if (busy) { CircularProgressIndicator(); if (progress > 0) Text("$progress files") }
            if (tree != null && !busy) {
                ResultCard("Folder", folderName)
                ResultCard("Files", "${entries.size}" + if (truncated) "+ (limit ${PackZip.MAX_FILES})" else "")
                ResultCard("Total size", Format.bytes(entries.sumOf { it.size }))
                if (warning != null) Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                else Text("manifest.json found at the top level.", style = MaterialTheme.typography.bodyMedium)
                if (truncated) Text("There are too many files. Remove some and try again.", color = MaterialTheme.colorScheme.error)
                ChipRow {
                    Choice(ext == "mcpack", ".mcpack") { ext = "mcpack" }
                    Choice(ext == "mcaddon", ".mcaddon") { ext = "mcaddon" }
                    Choice(ext == "zip", ".zip") { ext = "zip" }
                }
                Button(
                    enabled = entries.isNotEmpty() && !truncated,
                    onClick = { save.launch(PackZip.suggestedName(folderName, ext)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Create .$ext file") }
            }
            Note("Zips the folder you choose and names it .mcpack (one pack), .mcaddon (several packs inside) or .zip. Open the result with Minecraft to import it. Your folder is not changed.")
        }
    }
}
