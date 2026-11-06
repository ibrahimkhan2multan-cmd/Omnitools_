package com.omnitools.app.tools.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.Choice
import com.omnitools.app.ui.components.ChipRow
import com.omnitools.app.ui.components.CopyShareRow
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.ui.components.ToolPage

@Composable
private fun Note(text: String) = Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SecretField(label: String, value: String, show: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordStrengthScreen(onBack: () -> Unit) {
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val r = if (pw.isEmpty()) null else PasswordStrength.evaluate(pw)
    ScreenScaffold(title = "Password Strength", onBack = onBack) { padding ->
        ToolPage(padding) {
            SecretField("Type a password to test", pw, show) { pw = it.take(200) }
            ChipRow { Choice(show, "Show password") { show = !show } }
            r?.let {
                ResultCard("Rating", it.rating)
                LinearProgressIndicator(progress = { (it.bits / 100.0).coerceIn(0.0, 1.0).toFloat() }, modifier = Modifier.fillMaxWidth())
                ResultCard("Estimated strength", "${it.bits.toInt()} bits")
                ResultCard("Time to guess (fast attacker)", it.crackTime)
                it.tips.forEach { tip -> Text("• $tip", style = MaterialTheme.typography.bodyMedium) }
            }
            Note("This is an estimate, not a guarantee. The password is checked on your phone and never stored or sent anywhere.")
        }
    }
}

@Composable
fun PassphraseScreen(onBack: () -> Unit) {
    var count by rememberSaveable { mutableStateOf(5f) }
    var sep by rememberSaveable { mutableStateOf(0) }
    var capital by rememberSaveable { mutableStateOf(false) }
    var number by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var bits by remember { mutableStateOf(0) }
    val separators = listOf("-", " ", ".", "")
    fun make() {
        val r = Passphrase.generate(count.toInt(), separators[sep], capital, number)
        result = r.text; bits = r.bits
    }
    ScreenScaffold(title = "Passphrase Generator", onBack = onBack) { padding ->
        ToolPage(padding) {
            Text("Words: ${count.toInt()}", style = MaterialTheme.typography.titleSmall)
            Slider(value = count, onValueChange = { count = it }, valueRange = Passphrase.MIN_WORDS.toFloat()..Passphrase.MAX_WORDS.toFloat(), steps = Passphrase.MAX_WORDS - Passphrase.MIN_WORDS - 1)
            ChipRow {
                Choice(sep == 0, "Dash") { sep = 0 }
                Choice(sep == 1, "Space") { sep = 1 }
                Choice(sep == 2, "Dot") { sep = 2 }
                Choice(sep == 3, "None") { sep = 3 }
            }
            ChipRow {
                Choice(capital, "Capitalize") { capital = !capital }
                Choice(number, "Add number") { number = !number }
            }
            Button(onClick = { make() }, modifier = Modifier.fillMaxWidth()) { Text("Generate") }
            result?.let {
                ResultCard("Passphrase", it)
                ResultCard("Strength", "about $bits bits")
                CopyShareRow(it, "Passphrase")
            }
            Note("Words are picked with the phone's secure random generator from a ${Passphrase.words.size}-word list. Long passphrases of random words are easy to remember and hard to guess.")
        }
    }
}

@Composable
fun TextCryptoScreen(onBack: () -> Unit) {
    var encryptMode by rememberSaveable { mutableStateOf(true) }
    var text by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ScreenScaffold(title = "Text Encryptor", onBack = onBack) { padding ->
        ToolPage(padding) {
            ChipRow {
                Choice(encryptMode, "Encrypt") { encryptMode = true; output = null; error = null }
                Choice(!encryptMode, "Decrypt") { encryptMode = false; output = null; error = null }
            }
            TextInput(if (encryptMode) "Message to protect" else "Encrypted text (starts with OT1.)", text, { text = it.take(200_000) }, singleLine = false, minLines = 4)
            SecretField("Password (at least ${TextCrypto.MIN_PASSWORD} characters)", pw, show) { pw = it.take(200) }
            ChipRow { Choice(show, "Show password") { show = !show } }
            ErrorText(error)
            Button(
                enabled = !busy && text.isNotEmpty(),
                onClick = {
                    busy = true
                    val enc = encryptMode; val t = text; val p = pw
                    scope.launch {
                        try {
                            output = withContext(Dispatchers.Default) { if (enc) TextCrypto.encrypt(t, p) else TextCrypto.decrypt(t, p) }
                            error = null
                        } catch (e: IllegalArgumentException) { output = null; error = e.message ?: "That did not work." }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (encryptMode) "Encrypt" else "Decrypt") }
            output?.let {
                ResultCard(if (encryptMode) "Encrypted text" else "Original message", it)
                CopyShareRow(it, "Text")
            }
            Note("AES-256-GCM with a key made from your password (PBKDF2, 210,000 rounds). There is no way to recover a forgotten password, and a weak password is a weak lock.")
        }
    }
}
