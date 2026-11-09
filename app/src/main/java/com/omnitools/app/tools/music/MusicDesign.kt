package com.omnitools.app.tools.music

/** The look of the player. Chosen in the Music Player and used by the full player and the mini player. */
enum class PlayerStyle(val label: String) {
    CLASSIC("Classic"), GRADIENT("Gradient"), VINYL("Vinyl"), MINIMAL("Minimal");

    companion object {
        fun from(name: String?): PlayerStyle = values().firstOrNull { it.name == name } ?: CLASSIC
    }
}

object MusicAccents {
    /** Name and two colours (ARGB). Index 0 uses the app theme colours instead. */
    val names = listOf("Theme", "Ocean", "Sunset", "Forest", "Grape", "Mono")
    val pairs: List<Pair<Long, Long>> = listOf(
        0L to 0L,
        0xFF2563EBL to 0xFF06B6D4L,
        0xFFF97316L to 0xFFEC4899L,
        0xFF059669L to 0xFF65A30DL,
        0xFF7C3AEDL to 0xFFC026D3L,
        0xFF4B5563L to 0xFF9CA3AFL,
    )
    fun coerce(i: Int): Int = i.coerceIn(0, names.lastIndex)
}

data class MusicDesign(
    val style: PlayerStyle = PlayerStyle.CLASSIC,
    val accent: Int = 0,
    val miniPlayer: Boolean = true,
)
