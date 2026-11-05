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
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
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
        Tool(
            id = "hash_tool",
            name = "Hash & Checksum",
            description = "MD5, SHA-1, SHA-256 and SHA-512 of a file or text, with checksum compare and file details.",
            category = Category.SECURITY,
            icon = Icons.Filled.Fingerprint,
            keywords = listOf("md5", "sha", "sha256", "checksum", "verify", "file info", "digest"),
        ),
        Tool(
            id = "image_tools",
            name = "Image Editor",
            description = "Rotate, flip, crop and resize a picture, then save as JPEG, PNG or WebP with a size preview.",
            category = Category.IMAGES,
            icon = Icons.Filled.Crop,
            keywords = listOf("photo", "resize", "compress", "convert", "jpg", "png", "webp", "rotate", "crop"),
        ),
        Tool(
            id = "image_pdf",
            name = "Images to PDF",
            description = "Combine pictures into one PDF with page size, margin and quality options.",
            category = Category.PDF,
            icon = Icons.Filled.PictureAsPdf,
            keywords = listOf("jpg", "photos", "scan", "combine", "merge", "convert"),
        ),
        Tool(
            id = "pdf_reader",
            name = "PDF Reader",
            description = "Open a PDF, flip through pages, jump to a page and zoom. No text search.",
            category = Category.PDF,
            icon = Icons.Filled.Visibility,
            keywords = listOf("view", "open", "read", "pages", "document"),
        ),
        Tool(
            id = "loan_calc",
            name = "Loan & EMI Calculator",
            description = "Monthly payment, total interest and the first payments of a fixed-rate loan.",
            category = Category.CALC,
            icon = Icons.Filled.AccountBalance,
            keywords = listOf("emi", "mortgage", "interest", "payment", "credit", "car loan"),
        ),
        Tool(
            id = "base_converter",
            name = "Number Base Converter",
            description = "Convert whole numbers between binary, octal, decimal and hexadecimal.",
            category = Category.CALC,
            icon = Icons.Filled.Numbers,
            keywords = listOf("binary", "hex", "octal", "decimal", "base"),
        ),
        Tool(
            id = "discount_calc",
            name = "Discount & Sale Price",
            description = "Price after a discount and tax, or work out how many percent off a sale price is.",
            category = Category.CALC,
            icon = Icons.Filled.LocalOffer,
            keywords = listOf("sale", "percent off", "tax", "vat", "shopping"),
        ),
        Tool(
            id = "countdown",
            name = "Countdown",
            description = "Days left until your events, saved on this device.",
            category = Category.PRODUCTIVITY,
            icon = Icons.Filled.HourglassBottom,
            keywords = listOf("days until", "birthday", "deadline", "event"),
        ),
        Tool(
            id = "habit_tracker",
            name = "Habit Tracker",
            description = "Tick off daily habits and keep your streaks, saved on this device.",
            category = Category.PRODUCTIVITY,
            icon = Icons.Filled.CheckCircle,
            keywords = listOf("streak", "routine", "daily"),
        ),
        Tool(
            id = "folder_analyzer",
            name = "Folder Analyzer",
            description = "See what takes space in a folder: totals, types and the biggest files. Read-only.",
            category = Category.FILES,
            icon = Icons.Filled.Storage,
            keywords = listOf("storage", "space", "size", "largest", "disk", "cleanup"),
        ),
        Tool(
            id = "duplicate_finder",
            name = "Duplicate Finder",
            description = "Find identical files in a folder and free space. Nothing is deleted without your confirmation.",
            category = Category.FILES,
            icon = Icons.Filled.ContentCopy,
            keywords = listOf("duplicates", "same files", "clean", "free space", "copies"),
        ),
        Tool(
            id = "batch_rename",
            name = "Batch Rename",
            description = "Rename many files at once: find and replace, prefix, numbering, letter case. Preview before anything changes.",
            category = Category.FILES,
            icon = Icons.Filled.DriveFileRenameOutline,
            keywords = listOf("rename", "files", "numbering", "bulk", "photos", "names"),
        ),
        Tool(
            id = "encode_tool",
            name = "Encode / Decode",
            description = "Base64, URL, HTML entities, hex and binary, both directions, on your device.",
            category = Category.DEV,
            icon = Icons.Filled.Link,
            keywords = listOf("base64", "url", "percent", "html", "entities", "hex", "binary", "encode", "decode"),
        ),
        Tool(
            id = "time_zones",
            name = "Time Zones",
            description = "See what time it is in other cities for a time you pick. Works offline.",
            category = Category.PRODUCTIVITY,
            icon = Icons.Filled.Public,
            keywords = listOf("timezone", "world clock", "utc", "meeting", "convert time"),
        ),
        Tool(
            id = "bmi_calc",
            name = "BMI Calculator",
            description = "Body mass index in kg/cm or lb/ft, with the weight range for your height.",
            category = Category.CALC,
            icon = Icons.Filled.MonitorWeight,
            keywords = listOf("weight", "height", "health", "body mass"),
        ),
        Tool(
            id = "tip_split",
            name = "Tip & Bill Split",
            description = "Add a tip and split a bill between people, with optional rounding up.",
            category = Category.CALC,
            icon = Icons.Filled.Receipt,
            keywords = listOf("restaurant", "split", "share", "gratuity"),
        ),
        Tool(
            id = "roman_numerals",
            name = "Roman Numerals",
            description = "Convert numbers 1 to 3999 to Roman numerals and back.",
            category = Category.CALC,
            icon = Icons.Filled.FormatListNumbered,
            keywords = listOf("roman", "numeral", "xiv", "convert"),
        ),
        Tool(
            id = "number_words",
            name = "Number to Words",
            description = "Write any number in English words, such as for cheques and documents.",
            category = Category.CALC,
            icon = Icons.Filled.Tag,
            keywords = listOf("spell", "cheque", "check", "words", "say"),
        ),
        Tool(
            id = "timestamp_tool",
            name = "Timestamp Converter",
            description = "Unix timestamps to dates and back, in UTC or your time zone.",
            category = Category.DEV,
            icon = Icons.Filled.Schedule,
            keywords = listOf("unix", "epoch", "date", "time", "milliseconds"),
        ),
        Tool(
            id = "morse_code",
            name = "Morse Code",
            description = "Translate text to Morse code and Morse code to text.",
            category = Category.NOTES,
            icon = Icons.Filled.GraphicEq,
            keywords = listOf("dot", "dash", "sos", "telegraph"),
        ),
        Tool(
            id = "lorem_ipsum",
            name = "Lorem Ipsum Generator",
            description = "Placeholder text by paragraphs, sentences or words.",
            category = Category.NOTES,
            icon = Icons.Filled.ViewHeadline,
            keywords = listOf("dummy", "placeholder", "filler", "design"),
        ),
        Tool(
            id = "barcode_gen",
            name = "Barcode Generator",
            description = "Code 128, Code 39, EAN-13, EAN-8, UPC-A and ITF barcodes. Save or share as PNG.",
            category = Category.QR,
            icon = Icons.Filled.ViewWeek,
            keywords = listOf("ean", "upc", "code128", "label", "product", "stripes"),
        ),
        Tool(
            id = "zip_manager",
            name = "ZIP Manager",
            description = "See what is inside a ZIP, extract it to a folder (unsafe paths are blocked), or zip files together.",
            category = Category.FILES,
            icon = Icons.Filled.FolderZip,
            keywords = listOf("archive", "compress", "unzip", "extract", "zip", "rar"),
        ),
        Tool(
            id = "code_editor",
            name = "Code Editor",
            description = "Open, edit and save text and code files with line numbers, colours, find & replace and undo.",
            category = Category.DEV,
            icon = Icons.Filled.Terminal,
            keywords = listOf("source", "program", "kotlin", "python", "javascript", "json", "edit file", "text file"),
        ),
        Tool(
            id = "html_runner",
            name = "Code Runner",
            description = "Run HTML, CSS, JavaScript, Markdown, SVG and JSON in a locked-down page with a console. Touch games work.",
            category = Category.DEV,
            icon = Icons.Filled.Html,
            keywords = listOf("html", "js", "web", "javascript", "css", "markdown", "svg", "json", "preview", "sandbox", "playground", "game", "canvas", "runner"),
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
