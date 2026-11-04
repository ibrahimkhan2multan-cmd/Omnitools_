package com.omnitools.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.omnitools.app.BuildConfig
import com.omnitools.app.ui.components.IconBadge
import com.omnitools.app.ui.components.LinkRow
import com.omnitools.app.ui.components.OmniCard
import com.omnitools.app.ui.components.OmniLogo
import com.omnitools.app.ui.components.ScreenScaffold

const val POLICY_URL = "https://github.com/ibrahimkhan2multan-cmd/Omnitools_/blob/main/PRIVACY_POLICY.md"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    ScreenScaffold(title = "About & privacy", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Compact identity card instead of a tall centered block.
            OmniCard(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OmniLogo(size = 56.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("OmniTools", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "All-in-One Utility Suite",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            InfoCard(
                Icons.Filled.Shield,
                "What this app does with your data",
                "OmniTools has no internet permission, no account, no analytics and no ads. Notes, favorites, " +
                    "scan history and the running timer are stored only on this device. Nothing is uploaded or shared.",
            )
            InfoCard(
                Icons.Filled.Camera,
                "Permissions",
                "Camera: only when you open the scanner; analysed on-device.\n\n" +
                    "Notifications, sound, vibration, full-screen alert and restart-after-reboot: only so a timer can ring " +
                    "when the app is closed.\n\n" +
                    "Files are opened only through the system picker, and only the ones you choose.",
            )
            InfoCard(
                Icons.Filled.Key,
                "Generated passwords",
                "Passwords are created on your device with a secure random generator and are never saved or sent.",
            )

            HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            LinkRow(
                icon = Icons.Filled.PrivacyTip,
                title = "View Full Privacy Policy",
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
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    OmniCard(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(icon, size = 36)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
