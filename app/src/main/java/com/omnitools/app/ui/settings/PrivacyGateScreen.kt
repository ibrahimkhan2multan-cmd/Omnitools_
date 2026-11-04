package com.omnitools.app.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.LinkRow
import com.omnitools.app.ui.components.OmniLogo

/**
 * Shown before anything else until the user taps Accept. Declining closes the app; nothing is stored either way
 * except the accepted flag, so existing notes, favorites and history are untouched.
 */
@Composable
fun PrivacyGateScreen(onAccept: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 12.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OmniLogo(size = 72.dp)
            Text("Before you start", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Please read and accept the privacy policy to use OmniTools.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GateInfo(Icons.Filled.Shield, "Your data stays on your device", "No internet permission, no account, no analytics, no ads. Nothing is uploaded or shared.")
            GateInfo(Icons.Filled.Camera, "Permissions", "Camera only for the scanner. Notifications, sound and full-screen alert only so timers can ring when the app is closed. Files only through the system picker.")
            GateInfo(Icons.Filled.Key, "Passwords", "Generated on your device and never saved or sent.")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            LinkRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Read the full privacy policy",
                subtitle = "Opens in your browser",
                trailing = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = {
                    try {
                        uriHandler.openUri(POLICY_URL)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No browser found to open the policy", Toast.LENGTH_LONG).show()
                    }
                },
            )
        }
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Accept and continue") }
        TextButton(
            onClick = { (context as? Activity)?.finishAffinity() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Decline and close the app") }
    }
}

@Composable
private fun GateInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    com.omnitools.app.ui.components.OmniCard {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            com.omnitools.app.ui.components.IconBadge(icon, size = 36)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
