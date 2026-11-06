package com.omnitools.app.tools.calc

/** Unit and number-base conversion. Pure Kotlin so it can be unit tested. */
object UnitConverter {

    data class Unit(val name: String, val symbol: String, val toBase: Double)

    enum class Kind(val title: String) {
        LENGTH("Length"),
        WEIGHT("Weight"),
        AREA("Area"),
        VOLUME("Volume"),
        SPEED("Speed"),
        DATA("Data size"),
        TEMPERATURE("Temperature"),
    }

    // Base units: metre, kilogram, square metre, litre, metre/second, byte.
    private val units: Map<Kind, List<Unit>> = mapOf(
        Kind.LENGTH to listOf(
            Unit("Millimetre", "mm", 0.001), Unit("Centimetre", "cm", 0.01), Unit("Metre", "m", 1.0),
            Unit("Kilometre", "km", 1000.0), Unit("Inch", "in", 0.0254), Unit("Foot", "ft", 0.3048),
            Unit("Yard", "yd", 0.9144), Unit("Mile", "mi", 1609.344),
        ),
        Kind.WEIGHT to listOf(
            Unit("Milligram", "mg", 1e-6), Unit("Gram", "g", 0.001), Unit("Kilogram", "kg", 1.0),
            Unit("Tonne", "t", 1000.0), Unit("Ounce", "oz", 0.028349523125), Unit("Pound", "lb", 0.45359237),
        ),
        Kind.AREA to listOf(
            Unit("Square centimetre", "cm²", 0.0001), Unit("Square metre", "m²", 1.0),
            Unit("Hectare", "ha", 10_000.0), Unit("Square kilometre", "km²", 1_000_000.0),
            Unit("Square foot", "ft²", 0.09290304), Unit("Acre", "ac", 4046.8564224),
        ),
        Kind.VOLUME to listOf(
            Unit("Millilitre", "mL", 0.001), Unit("Litre", "L", 1.0), Unit("Cubic metre", "m³", 1000.0),
            Unit("Teaspoon (US)", "tsp", 0.00492892159375), Unit("Cup (US)", "cup", 0.2365882365),
            Unit("Gallon (US)", "gal", 3.785411784),
        ),
        Kind.SPEED to listOf(
            Unit("Metre/second", "m/s", 1.0), Unit("Kilometre/hour", "km/h", 1.0 / 3.6),
            Unit("Mile/hour", "mph", 0.44704), Unit("Knot", "kn", 1852.0 / 3600.0),
        ),
        // Decimal (SI) and binary (IEC) prefixes are listed separately so there is no ambiguity.
        Kind.DATA to listOf(
            Unit("Bit", "bit", 0.125), Unit("Byte", "B", 1.0),
            Unit("Kilobyte (1000 B)", "KB", 1e3), Unit("Megabyte (1000² B)", "MB", 1e6),
            Unit("Gigabyte (1000³ B)", "GB", 1e9), Unit("Terabyte (1000⁴ B)", "TB", 1e12),
            Unit("Kibibyte (1024 B)", "KiB", 1024.0), Unit("Mebibyte (1024² B)", "MiB", 1024.0 * 1024),
            Unit("Gibibyte (1024³ B)", "GiB", 1024.0 * 1024 * 1024), Unit("Tebibyte (1024⁴ B)", "TiB", 1024.0 * 1024 * 1024 * 1024),
        ),
        Kind.TEMPERATURE to listOf(
            Unit("Celsius", "°C", 0.0), Unit("Fahrenheit", "°F", 0.0), Unit("Kelvin", "K", 0.0),
        ),
    )

    fun unitsFor(kind: Kind): List<Unit> = units.getValue(kind)

    fun convert(kind: Kind, value: Double, from: Unit, to: Unit): Double {
        if (kind == Kind.TEMPERATURE) return convertTemperature(value, from.symbol, to.symbol)
        return value * from.toBase / to.toBase
    }

    fun convertTemperature(value: Double, from: String, to: String): Double {
        val celsius = when (from) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> throw IllegalArgumentException("Unknown temperature unit $from")
        }
        if (celsius < -273.15 - 1e-9) throw IllegalArgumentException("Below absolute zero")
        return when (to) {
            "°C" -> celsius
            "°F" -> celsius * 9.0 / 5.0 + 32.0
            "K" -> celsius + 273.15
            else -> throw IllegalArgumentException("Unknown temperature unit $to")
        }
    }

    /**
     * Converts an integer written in [fromBase] to [toBase] (2..36). Returns uppercase digits.
     * Throws [IllegalArgumentException] with a readable message for invalid input.
     */
    fun convertBase(input: String, fromBase: Int, toBase: Int): String {
        require(fromBase in 2..36 && toBase in 2..36) { "Base must be between 2 and 36." }
        val text = input.trim()
        require(text.isNotEmpty()) { "Enter a number." }
        val value = try {
            java.math.BigInteger(text, fromBase)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("\"$text\" is not a valid base-$fromBase number.")
        }
        return value.toString(toBase).uppercase()
    }
}
