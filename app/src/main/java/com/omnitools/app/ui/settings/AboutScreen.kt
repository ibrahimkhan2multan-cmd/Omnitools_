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
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold(title = "About & privacy", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "OmniTools logo",
                modifier = Modifier.size(96.dp),
            )
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
                    "Notes, favorites, recently used tools and scan history are stored only on this device. " +
                        "OmniTools has no account system, no analytics and no advertising, and its own code " +
                        "does not upload your notes, scans or generated codes anywhere."
                )
                SectionTitle("Camera")
                Body(
                    "The camera permission is requested only when you open the scanner. If you deny it, " +
                        "every other tool keeps working. Scanning runs on-device using the barcode model " +
                        "bundled with Google ML Kit. Scanned content is never opened automatically."
                )
                SectionTitle("Generated passwords")
                Body("Passwords are created on your device with a secure random generator and are never saved or sent.")
                SectionTitle("Current limits")
                Body(
                    "This is an early version. Only the tools listed on the home screen exist; more " +
                        "(PDF, images, ZIP, code editor) will be added in later releases."
                )
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
