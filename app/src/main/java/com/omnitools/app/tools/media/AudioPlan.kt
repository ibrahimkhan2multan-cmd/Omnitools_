package com.omnitools.app.tools.media

/** The kinds of audio file the extractor can write. */
enum class Container(val ext: String, val mime: String, val label: String) {
    M4A("m4a", "audio/mp4", "M4A (AAC)"),
    OGG("ogg", "audio/ogg", "OGG (Opus)"),
    WEBM("weba", "audio/webm", "WebM audio"),
    WAV("wav", "audio/wav", "WAV"),
}

/** Decisions and file-format maths for audio extraction. Pure functions (unit tested). */
object AudioPlan {
    /**
     * Which file to write. Sound that is AAC, Opus or Vorbis can be copied out untouched (fast, no quality loss).
     * Anything else (MP3, AC-3 ...) has no container Android can write, so it is decoded to WAV.
     */
    fun choose(mime: String?, sdk: Int, wantWav: Boolean): Container {
        if (wantWav) return Container.WAV
        return when (mime?.lowercase()) {
            "audio/mp4a-latm" -> Container.M4A
            "audio/opus" -> if (sdk >= 29) Container.OGG else Container.WEBM
            "audio/vorbis" -> Container.WEBM
            else -> Container.WAV
        }
    }

    fun codecName(mime: String?): String = when (mime?.lowercase()) {
        "audio/mp4a-latm" -> "AAC"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/mpeg" -> "MP3"
        "audio/ac3" -> "AC-3"
        "audio/eac3" -> "E-AC-3"
        "audio/flac" -> "FLAC"
        "audio/3gpp" -> "AMR"
        "audio/amr-wb" -> "AMR-WB"
        "audio/raw" -> "PCM"
        null -> "unknown"
        else -> mime.substringAfter('/').uppercase()
    }

    fun channelsLabel(n: Int): String = when (n) { 1 -> "mono"; 2 -> "stereo"; in 3..Int.MAX_VALUE -> "$n channels"; else -> "" }

    /** A safe file name for [videoName] with the container's extension, not clashing with [taken] names. */
    fun outputName(videoName: String?, container: Container, taken: Set<String>): String {
        val base = (videoName ?: "").substringBeforeLast('.', videoName ?: "")
            .map { if (it.code < 32 || it in "/\\:*?\"<>|") ' ' else it }.joinToString("")
            .trim().replace(Regex("\\s+"), " ").take(80).trim().trim('.').ifEmpty { "audio" }
        val lower = taken.map { it.lowercase() }.toSet()
        var name = "$base.${container.ext}"
        var n = 2
        while (name.lowercase() in lower) { name = "$base ($n).${container.ext}"; n++ }
        return name
    }

    /** The 44-byte header of a 16-bit PCM WAV file with [dataBytes] bytes of sound. */
    fun wavHeader(sampleRate: Int, channels: Int, dataBytes: Long): ByteArray {
        val h = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val data = dataBytes.coerceIn(0, 0xFFFF_FFFFL - 36)
        h.put("RIFF".toByteArray()).putInt((36 + data).toInt()).put("WAVE".toByteArray())
        h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort())
        h.putInt(sampleRate).putInt(sampleRate * channels * 2).putShort((channels * 2).toShort()).putShort(16)
        h.put("data".toByteArray()).putInt(data.toInt())
        return h.array()
    }

    /** "12.3 MB" style size. */
    fun size(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1048576.0)
        else -> "%.2f GB".format(bytes / 1073741824.0)
    }
}
