package com.omnitools.app.ui.tools

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.omnitools.app.data.Prefs
import com.omnitools.app.data.ToolRegistry
import com.omnitools.app.tools.calc.CalculatorScreen
import com.omnitools.app.tools.calc.UnitConverterScreen
import com.omnitools.app.tools.device.DeviceInfoScreen
import com.omnitools.app.tools.notes.NotesScreen
import com.omnitools.app.tools.password.PasswordScreen
import com.omnitools.app.tools.qr.QrGenerateScreen
import com.omnitools.app.tools.qr.ScanScreen
import com.omnitools.app.tools.text.TextToolsScreen
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.ScreenScaffold

/** Maps a tool id from [ToolRegistry] to the screen that implements it. */
@Composable
fun ToolHost(toolId: String, prefs: Prefs, onBack: () -> Unit) {
    when (toolId) {
        "qr_scan" -> ScanScreen(prefs, onBack)
        "qr_generate" -> QrGenerateScreen(onBack)
        "notes" -> NotesScreen(onBack)
        "text_tools" -> TextToolsScreen(onBack)
        "calculator" -> CalculatorScreen(onBack)
        "unit_converter" -> UnitConverterScreen(onBack)
        "device_info" -> DeviceInfoScreen(onBack)
        "password_generator" -> PasswordScreen(onBack)
        else -> ScreenScaffold(ToolRegistry.find(toolId)?.name ?: "Unknown tool", onBack) { padding ->
            EmptyState("This tool is not available.", Modifier.padding(padding))
        }
    }
}
