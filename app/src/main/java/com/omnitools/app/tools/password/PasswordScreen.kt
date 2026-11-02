package com.omnitools.app.tools.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.util.Actions
import java.util.UUID

@Composable
fun PasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var options by remember { mutableStateOf(PasswordGenerator.Options()) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var uuid by remember { mutableStateOf("") }

    fun regenerate(o: PasswordGenerator.Options = options) {
        try {
            password = PasswordGenerator.generate(o)
            error = null
        } catch (e: IllegalArgumentException) {
            password = ""
            error = e.message
        }
    }
    LaunchedEffect(Unit) { regenerate() }

    fun update(o: PasswordGenerator.Options) {
        options = o
        regenerate(o)
    }

    ScreenScaffold(title = "Password Generator", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        password.ifEmpty { "—" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (password.isNotEmpty()) {
                        val bits = PasswordGenerator.entropyBits(options)
                        Text(
                            "${PasswordGenerator.strengthLabel(bits)} · about ${bits.toInt()} bits",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { regenerate() }, modifier = Modifier.weight(1f)) { Text("Generate") }
                OutlinedButton(
                    onClick = { if (password.isNotEmpty()) Actions.copy(context, "Password", password) },
                    enabled = password.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text("Copy") }
            }

            Text("Length: ${options.length}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = options.length.toFloat(),
                onValueChange = { update(options.copy(length = it.toInt())) },
                valueRange = PasswordGenerator.MIN_LENGTH.toFloat()..PasswordGenerator.MAX_LENGTH.toFloat(),
                steps = PasswordGenerator.MAX_LENGTH - PasswordGenerator.MIN_LENGTH - 1,
            )
            Option("Lowercase letters (a–z)", options.lower) { update(options.copy(lower = it)) }
            Option("Uppercase letters (A–Z)", options.upper) { update(options.copy(upper = it)) }
            Option("Digits (0–9)", options.digits) { update(options.copy(digits = it)) }
            Option("Symbols (!@#…)", options.symbols) { update(options.copy(symbols = it)) }
            Option("Avoid look-alike characters (l, 1, I, O, 0)", options.avoidAmbiguous) { update(options.copy(avoidAmbiguous = it)) }

            Text(
                "Passwords are made on this device with a secure random generator and are never saved. " +
                    "Copying places the text on the system clipboard, which other apps may be able to read on some Android versions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("UUID")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        uuid.ifEmpty { "—" },
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { uuid = UUID.randomUUID().toString() }) { Text("Generate UUID") }
                        OutlinedButton(onClick = { Actions.copy(context, "UUID", uuid) }, enabled = uuid.isNotEmpty()) { Text("Copy") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Option(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
