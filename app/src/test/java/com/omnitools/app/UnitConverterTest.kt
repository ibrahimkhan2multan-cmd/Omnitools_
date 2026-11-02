package com.omnitools.app

import com.omnitools.app.tools.calc.UnitConverter
import com.omnitools.app.tools.calc.UnitConverter.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UnitConverterTest {
    private fun unit(kind: Kind, symbol: String) = UnitConverter.unitsFor(kind).first { it.symbol == symbol }
    private fun conv(kind: Kind, v: Double, from: String, to: String) =
        UnitConverter.convert(kind, v, unit(kind, from), unit(kind, to))

    @Test fun length() {
        assertEquals(1000.0, conv(Kind.LENGTH, 1.0, "km", "m"), 1e-9)
        assertEquals(2.54, conv(Kind.LENGTH, 1.0, "in", "cm"), 1e-9)
        assertEquals(5280.0, conv(Kind.LENGTH, 1.0, "mi", "ft"), 1e-6)
    }

    @Test fun weight() {
        assertEquals(2.2046, conv(Kind.WEIGHT, 1.0, "kg", "lb"), 1e-4)
        assertEquals(16.0, conv(Kind.WEIGHT, 1.0, "lb", "oz"), 1e-6)
    }

    @Test fun speedAndVolume() {
        assertEquals(36.0, conv(Kind.SPEED, 10.0, "m/s", "km/h"), 1e-9)
        assertEquals(3.785411784, conv(Kind.VOLUME, 1.0, "gal", "L"), 1e-9)
    }

    @Test fun dataDecimalVersusBinary() {
        assertEquals(1000.0, conv(Kind.DATA, 1.0, "KB", "B"), 1e-9)
        assertEquals(1024.0, conv(Kind.DATA, 1.0, "KiB", "B"), 1e-9)
        assertEquals(8.0, conv(Kind.DATA, 1.0, "B", "bit"), 1e-9)
        assertEquals(1024.0, conv(Kind.DATA, 1.0, "GiB", "MiB"), 1e-6)
    }

    @Test fun temperature() {
        assertEquals(212.0, UnitConverter.convertTemperature(100.0, "°C", "°F"), 1e-9)
        assertEquals(0.0, UnitConverter.convertTemperature(32.0, "°F", "°C"), 1e-9)
        assertEquals(273.15, UnitConverter.convertTemperature(0.0, "°C", "K"), 1e-9)
        assertEquals(-40.0, UnitConverter.convertTemperature(-40.0, "°C", "°F"), 1e-9)
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convertTemperature(-300.0, "°C", "K") }
        assertEquals(-459.67, UnitConverter.convertTemperature(0.0, "K", "°F"), 1e-9) // absolute zero is allowed
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convertTemperature(-1.0, "K", "°F") }
    }

    @Test fun numberBases() {
        assertEquals("11111111", UnitConverter.convertBase("255", 10, 2))
        assertEquals("FF", UnitConverter.convertBase("255", 10, 16))
        assertEquals("377", UnitConverter.convertBase("ff", 16, 8))
        assertEquals("0", UnitConverter.convertBase("0", 10, 2))
        assertEquals("FFFFFFFFFFFFFFFFFFFF", UnitConverter.convertBase("1208925819614629174706175", 10, 16))
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convertBase("12", 2, 10) }
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convertBase("", 10, 2) }
        assertThrows(IllegalArgumentException::class.java) { UnitConverter.convertBase("1", 1, 2) }
    }
}
