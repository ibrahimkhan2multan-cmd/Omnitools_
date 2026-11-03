package com.omnitools.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.OmniLogo
import com.omnitools.app.ui.components.pressable
import com.omnitools.app.R
import com.omnitools.app.ui.theme.omniColorScheme

/**
 * Shown once, the first time the app is opened. [onChosen] receives "dark", "light" or "system".
 * The choice can be changed later in Settings.
 */
@Composable
fun ThemeChoiceScreen(onChosen: (String) -> Unit) {
    // Highlight starts on nothing, so the user makes a deliberate choice.
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OmniLogo(size = 84.dp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Text("Welcome to OmniTools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Choose how the app should look. You can change this any time in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemePreviewCard("Dark", dark = true, selected = selected == "dark", modifier = Modifier.weight(1f)) { selected = "dark" }
            ThemePreviewCard("Light", dark = false, selected = selected == "light", modifier = Modifier.weight(1f)) { selected = "light" }
        }

        Button(
            onClick = { selected?.let(onChosen) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Continue") }

        TextButton(onClick = { onChosen("system") }) { Text("Follow my phone's setting instead") }
    }
}

/** A small mock screen drawn with the real dark or light colors so the choice is visible before it is made. */
@Composable
private fun ThemePreviewCard(label: String, dark: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = omniColorScheme(dark)
    Surface(
        modifier = modifier.pressable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = scheme.background,
        border = BorderStroke(
            width = animateDpAsState(if (selected) 3.dp else 1.dp, label = "border width").value,
            color = animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                label = "border color",
            ).value,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth(0.55f).height(10.dp).background(scheme.onBackground, RoundedCornerShape(5.dp)))
            repeat(2) {
                Box(
                    Modifier.fillMaxWidth().height(34.dp).background(scheme.surface, RoundedCornerShape(10.dp)).padding(8.dp),
                ) {
                    Box(Modifier.size(18.dp).background(scheme.primary, RoundedCornerShape(6.dp)))
                    Box(
                        Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.55f).height(6.dp)
                            .background(scheme.onSurfaceVariant, RoundedCornerShape(3.dp)),
                    )
                }
            }
            Text(
                if (selected) "$label ✓" else label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
