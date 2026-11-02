package com.omnitools.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Notes
import androidx.compose.ui.graphics.vector.ImageVector

enum class Category(val title: String, val icon: ImageVector) {
    FILES("Files & Archives", Icons.Filled.Folder),
    DEV("Coding & Developer Tools", Icons.Filled.Code),
    PDF("PDF & Documents", Icons.Filled.Description),
    IMAGES("Images & Media", Icons.Filled.Image),
    QR("QR & Barcode", Icons.Filled.QrCode2),
    DEVICE("Device Information", Icons.Filled.PhoneAndroid),
    NOTES("Notes & Text", Icons.Filled.Notes),
    CALC("Calculators & Converters", Icons.Filled.Calculate),
    PRODUCTIVITY("Productivity", Icons.Filled.Timer),
    SECURITY("Security & Privacy", Icons.Filled.Security),
    NETWORK("Network Utilities", Icons.Filled.Wifi),
    ADVANCED("Advanced Tools", Icons.Filled.Construction),
}

/**
 * A tool shown on the dashboard. Only tools that open a real, working screen are registered:
 * categories with no registered tools are simply hidden until a later part ships them.
 */
data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val category: Category,
    val icon: ImageVector,
    val keywords: List<String> = emptyList(),
)

object ToolRegistry {

    val all: List<Tool> = listOf(
        Tool(
            id = "qr_scan",
            name = "QR & Barcode Scanner",
            description = "Scan QR codes and common barcodes with the camera, with link safety checks and history.",
            category = Category.QR,
            icon = Icons.Filled.QrCodeScanner,
            keywords = listOf("scan", "camera", "barcode", "ean", "upc", "history"),
        ),
        Tool(
            id = "qr_generate",
            name = "QR Code Generator",
            description = "Create QR codes for text, links, Wi-Fi and contacts. Works fully offline.",
            category = Category.QR,
            icon = Icons.Filled.QrCode2,
            keywords = listOf("create", "make", "wifi", "contact", "vcard", "url", "share"),
        ),
        Tool(
            id = "notes",
            name = "Notepad",
            description = "Create, edit, search and delete notes. Saved on this device only.",
            category = Category.NOTES,
            icon = Icons.Filled.EditNote,
            keywords = listOf("note", "write", "memo", "autosave"),
        ),
        Tool(
            id = "text_tools",
            name = "Text Tools",
            description = "Word counter, case converter, sort, dedupe, find & replace, Base64, URL encode, JSON format.",
            category = Category.NOTES,
            icon = Icons.Filled.TextFields,
            keywords = listOf("count", "words", "characters", "uppercase", "lowercase", "base64", "url", "json", "minify", "reverse", "duplicate"),
        ),
        Tool(
            id = "calculator",
            name = "Calculator",
            description = "Scientific calculator with brackets, percentages, powers, roots and trigonometry.",
            category = Category.CALC,
            icon = Icons.Filled.Calculate,
            keywords = listOf("math", "scientific", "percent", "sqrt"),
        ),
        Tool(
            id = "unit_converter",
            name = "Unit Converter",
            description = "Length, weight, area, volume, temperature, speed, data size and number bases.",
            category = Category.CALC,
            icon = Icons.Filled.SwapHoriz,
            keywords = listOf("convert", "length", "weight", "temperature", "binary", "hex", "bytes", "celsius", "fahrenheit"),
        ),
        Tool(
            id = "device_info",
            name = "Device Information",
            description = "Model, Android version, screen, memory, storage, battery and supported ABIs.",
            category = Category.DEVICE,
            icon = Icons.Filled.PhoneAndroid,
            keywords = listOf("phone", "specs", "battery", "ram", "storage", "cpu", "api"),
        ),
        Tool(
            id = "password_generator",
            name = "Password Generator",
            description = "Strong random passwords from a secure random source. Never stored or sent anywhere.",
            category = Category.SECURITY,
            icon = Icons.Filled.Key,
            keywords = listOf("random", "secure", "strong", "uuid"),
        ),
        Tool(
            id = "percent_calc",
            name = "Percentage & Discount",
            description = "X% of Y, percentage change, discounts with optional tax.",
            category = Category.CALC,
            icon = Icons.Filled.Percent,
            keywords = listOf("sale", "price", "tax", "increase", "decrease", "percent"),
        ),
        Tool(
            id = "date_calc",
            name = "Date & Age Calculator",
            description = "Days between dates, exact age, next birthday, add or subtract time from a date.",
            category = Category.CALC,
            icon = Icons.Filled.Event,
            keywords = listOf("birthday", "difference", "calendar", "days", "weeks"),
        ),
        Tool(
            id = "stopwatch",
            name = "Stopwatch & Timer",
            description = "Stopwatch with laps and a countdown timer with alarm sound.",
            category = Category.PRODUCTIVITY,
            icon = Icons.Filled.Timer,
            keywords = listOf("lap", "countdown", "alarm", "clock"),
        ),
        Tool(
            id = "tasks",
            name = "Task List",
            description = "Simple to-do list with progress. Saved on this device.",
            category = Category.PRODUCTIVITY,
            icon = Icons.Filled.Checklist,
            keywords = listOf("todo", "to-do", "checklist", "reminder"),
        ),
        Tool(
            id = "random_tools",
            name = "Dice, Coin & Random",
            description = "Roll dice, flip a coin, random numbers, or pick one option from a list.",
            category = Category.PRODUCTIVITY,
            icon = Icons.Filled.Casino,
            keywords = listOf("random", "d20", "heads", "tails", "choose", "decide", "raffle"),
        ),
        Tool(
            id = "color_tool",
            name = "Color Converter",
            description = "HEX, RGB and HSL conversion with a live preview and contrast check.",
            category = Category.DEV,
            icon = Icons.Filled.Palette,
            keywords = listOf("hex", "rgb", "hsl", "picker", "contrast", "css"),
        ),
        Tool(
            id = "regex_tool",
            name = "Regex Tester",
            description = "Test regular expressions with match positions and groups. Time-limited for safety.",
            category = Category.DEV,
            icon = Icons.Filled.FindInPage,
            keywords = listOf("regular expression", "pattern", "match", "groups"),
        ),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Tool? = byId[id]

    /** Categories that currently contain at least one working tool. */
    fun activeCategories(): List<Category> =
        Category.values().filter { c -> all.any { it.category == c } }

    fun inCategory(category: Category): List<Tool> = all.filter { it.category == category }

    /** Matches the query against tool name, description, category title and keywords (case-insensitive). */
    fun search(query: String): List<Tool> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        return all.filter { tool ->
            val haystack = buildString {
                append(tool.name).append(' ')
                append(tool.description).append(' ')
                append(tool.category.title).append(' ')
                append(tool.keywords.joinToString(" "))
            }.lowercase()
            terms.all { haystack.contains(it) }
        }
    }
}
