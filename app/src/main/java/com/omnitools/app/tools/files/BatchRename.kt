package com.omnitools.app.tools.files

enum class CaseMode(val label: String) { KEEP("Keep"), LOWER("lowercase"), UPPER("UPPERCASE"), TITLE("Title Case") }

data class RenameRules(
    val find: String = "",
    val replaceWith: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val numbering: Boolean = false,
    val numberName: String = "",
    val start: Int = 1,
    val digits: Int = 2,
    val caseMode: CaseMode = CaseMode.KEEP,
    val keepExtension: Boolean = true,
)

/** Works out new names for a batch of files and flags anything that would clash. Pure Kotlin, unit tested. */
object BatchRename {
    enum class Status { UNCHANGED, OK, DUPLICATE, EXISTS, INVALID }
    data class Plan(val old: String, val new: String, val status: Status)

    private const val MAX_LEN = 255
    private val illegal = Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]")

    fun naturalOrder(names: List<String>): List<String> =
        names.sortedWith(compareBy<String> { it.lowercase().replace(Regex("\\d+")) { m -> m.value.padStart(12, '0') } }.thenBy { it })

    private fun splitName(name: String, keepExt: Boolean): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        return if (!keepExt || dot <= 0 || dot == name.length - 1) name to "" else name.substring(0, dot) to name.substring(dot)
    }

    private fun title(s: String) = s.split(' ').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }

    fun newName(old: String, index: Int, r: RenameRules): String {
        val (base0, ext) = splitName(old, r.keepExtension)
        var base = base0
        if (r.find.isNotEmpty()) base = base.replace(r.find, r.replaceWith, ignoreCase = true)
        base = r.prefix + base + r.suffix
        if (r.numbering) {
            val n = (r.start + index).toString().padStart(r.digits.coerceIn(1, 9), '0')
            base = (r.numberName.ifBlank { base }) + " " + n
        }
        base = when (r.caseMode) {
            CaseMode.KEEP -> base
            CaseMode.LOWER -> base.lowercase()
            CaseMode.UPPER -> base.uppercase()
            CaseMode.TITLE -> title(base)
        }
        return base.replace(illegal, "_").trim() + if (ext.isEmpty()) "" else ext
    }

    /** [names] are the files in one folder, in the order numbering should follow. [others] are other names in that folder that stay as they are. */
    fun plan(names: List<String>, r: RenameRules, others: Set<String> = emptySet()): List<Plan> {
        val raw = names.mapIndexed { i, n -> n to newName(n, i, r) }
        val finalCount = HashMap<String, Int>()
        raw.forEach { (_, new) -> finalCount.merge(new.lowercase(), 1, Int::plus) }
        val currentNames = names.map { it.lowercase() }.toSet() + others.map { it.lowercase() }
        return raw.map { (old, new) ->
            val status = when {
                new == old -> Status.UNCHANGED
                new.isBlank() || new.length > MAX_LEN || new.trim('.').isEmpty() || (new.startsWith(".") && !old.startsWith(".")) -> Status.INVALID
                (finalCount[new.lowercase()] ?: 0) > 1 -> Status.DUPLICATE
                new.lowercase() in currentNames && new.lowercase() != old.lowercase() -> Status.EXISTS
                else -> Status.OK
            }
            Plan(old, new, status)
        }
    }
}
