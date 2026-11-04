package com.omnitools.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitools.app.BuildConfig
import com.omnitools.app.R
import com.omnitools.app.ui.components.OmniLogo
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "About & privacy", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OmniLogo(size = 84.dp, modifier = Modifier.padding(vertical = 12.dp))
            Text("OmniTools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "All-in-One Utility Suite",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Column(Modifier.fillMaxWidth()) {
                SectionTitle("What this app does with your data")
                Body(
                    "OmniTools has no internet permission, no account, no analytics and no ads. Notes, favorites, " +
                        "scan history and the running timer are stored only on this device. Nothing is uploaded or shared."
                )
                SectionTitle("Permissions")
                Body(
                    "Camera: only when you open the scanner; analysed on-device. Notifications, sound, vibration, " +
                        "full-screen alert and restart-after-reboot: only so a timer can ring when the app is closed. " +
                        "Files are opened only through the system picker, only the ones you choose."
                )
                SectionTitle("Generated passwords")
                Body("Passwords are created on your device with a secure random generator and are never saved or sent.")
                SectionTitle("Full privacy policy")
                Body("github.com/ibrahimkhan2multan-cmd/Omnitools_ (PRIVACY_POLICY.md)")
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}
