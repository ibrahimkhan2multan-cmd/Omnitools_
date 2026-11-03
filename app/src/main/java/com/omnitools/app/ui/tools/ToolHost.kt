package com.omnitools.app.ui.tools

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.omnitools.app.data.Prefs
import com.omnitools.app.data.ToolRegistry
import com.omnitools.app.tools.calc.CalculatorScreen
import com.omnitools.app.tools.calc.DateScreen
import com.omnitools.app.tools.calc.PercentScreen
import com.omnitools.app.tools.color.ColorScreen
import com.omnitools.app.tools.random.RandomScreen
import com.omnitools.app.tools.regex.RegexScreen
import com.omnitools.app.tools.calc.BmiScreen
import com.omnitools.app.tools.calc.LoanScreen
import com.omnitools.app.tools.calc.NumberWordsScreen
import com.omnitools.app.tools.calc.RomanScreen
import com.omnitools.app.tools.calc.TipScreen
import com.omnitools.app.tools.dev.TimestampScreen
import com.omnitools.app.tools.qr.BarcodeScreen
import com.omnitools.app.tools.text.LoremScreen
import com.omnitools.app.tools.text.MorseScreen
import com.omnitools.app.tools.hash.HashScreen
import com.omnitools.app.tools.image.ImageToolsScreen
import com.omnitools.app.tools.pdf.ImagesToPdfScreen
import com.omnitools.app.tools.pdf.PdfReaderScreen
import com.omnitools.app.tools.tasks.TasksScreen
import com.omnitools.app.tools.timer.StopwatchScreen
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
        "calculator" -> CalculatorScreen(prefs, onBack)
        "unit_converter" -> UnitConverterScreen(onBack)
        "device_info" -> DeviceInfoScreen(onBack)
        "password_generator" -> PasswordScreen(onBack)
        "percent_calc" -> PercentScreen(onBack)
        "date_calc" -> DateScreen(onBack)
        "stopwatch" -> StopwatchScreen(onBack)
        "tasks" -> TasksScreen(onBack)
        "random_tools" -> RandomScreen(onBack)
        "color_tool" -> ColorScreen(onBack)
        "regex_tool" -> RegexScreen(onBack)
        "hash_tool" -> HashScreen(onBack)
        "image_tools" -> ImageToolsScreen(onBack)
        "image_pdf" -> ImagesToPdfScreen(onBack)
        "pdf_reader" -> PdfReaderScreen(onBack)
        "loan_calc" -> LoanScreen(onBack)
        "bmi_calc" -> BmiScreen(onBack)
        "tip_split" -> TipScreen(onBack)
        "roman_numerals" -> RomanScreen(onBack)
        "number_words" -> NumberWordsScreen(onBack)
        "timestamp_tool" -> TimestampScreen(onBack)
        "morse_code" -> MorseScreen(onBack)
        "lorem_ipsum" -> LoremScreen(onBack)
        "barcode_gen" -> BarcodeScreen(onBack)
        else -> ScreenScaffold(ToolRegistry.find(toolId)?.name ?: "Unknown tool", onBack) { padding ->
            EmptyState("This tool is not available.", Modifier.padding(padding))
        }
    }
}
