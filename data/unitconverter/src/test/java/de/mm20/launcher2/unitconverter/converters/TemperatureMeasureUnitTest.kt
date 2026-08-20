package de.mm20.launcher2.unitconverter.converters

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureMeasureUnitTest {

    private val celsius = TemperatureMeasureUnit(
        symbol = "°C",
        nameResource = 0,
        unit = TemperatureUnit.DegreeCelsius,
    )
    private val kelvin = TemperatureMeasureUnit(
        symbol = "K",
        nameResource = 0,
        unit = TemperatureUnit.Kelvin,
    )

    // Arabic spells celsius "°س" and an hour "س".
    private val celsiusArabic = TemperatureMeasureUnit(
        symbol = "°س",
        nameResource = 0,
        unit = TemperatureUnit.DegreeCelsius,
    )

    @Test
    fun matches_strictModeRequiresTheExactSymbol() {
        assertTrue(celsius.matches("°C"))
        assertFalse(celsius.matches("°c"))
        assertFalse(celsius.matches("C"))
    }

    @Test
    fun matches_lenientModeIgnoresCase() {
        assertTrue(celsius.matches("°c", lenient = true))
        assertTrue(kelvin.matches("k", lenient = true))
    }

    @Test
    fun matches_lenientModeAcceptsTheSymbolWithoutItsDegreeSign() {
        assertTrue(celsius.matches("C", lenient = true))
        assertTrue(celsius.matches("c", lenient = true))
    }

    @Test
    fun matches_degreeSignIsNotOptionalInTheStrictPass() {
        // Otherwise "30 س" is ambiguous between an hour and 30 °C.
        assertFalse(celsiusArabic.matches("س"))
        assertTrue(celsiusArabic.matches("°س"))
        assertTrue(celsiusArabic.matches("س", lenient = true))
    }

    @Test
    fun matches_unrelatedInputDoesNotMatch() {
        assertFalse(celsius.matches("f", lenient = true))
        assertFalse(celsius.matches("K", lenient = true))
    }

    @Test
    fun matches_unitWithoutADegreeSignOnlyMatchesItsSymbol() {
        assertTrue(kelvin.matches("K"))
        assertFalse(kelvin.matches("c", lenient = true))
    }
}
