package de.mm20.launcher2.unitconverter.converters

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertTemperatureTest {

    @Test
    fun celsiusToFahrenheit() {
        assertEquals(32.0, convert(0.0, C, F), 1e-9)
        assertEquals(212.0, convert(100.0, C, F), 1e-9)
        assertEquals(-40.0, convert(-40.0, C, F), 1e-9)
    }

    @Test
    fun fahrenheitToCelsius() {
        assertEquals(0.0, convert(32.0, F, C), 1e-9)
        assertEquals(100.0, convert(212.0, F, C), 1e-9)
        assertEquals(-40.0, convert(-40.0, F, C), 1e-9)
    }

    @Test
    fun celsiusToKelvin() {
        assertEquals(273.15, convert(0.0, C, K), 1e-9)
        assertEquals(0.0, convert(-273.15, C, K), 1e-9)
    }

    @Test
    fun kelvinToCelsius() {
        assertEquals(-273.15, convert(0.0, K, C), 1e-9)
        assertEquals(0.0, convert(273.15, K, C), 1e-9)
    }

    @Test
    fun kelvinToFahrenheit() {
        assertEquals(-459.67, convert(0.0, K, F), 1e-9)
        assertEquals(32.0, convert(273.15, K, F), 1e-9)
    }

    @Test
    fun fahrenheitToKelvin() {
        assertEquals(0.0, convert(-459.67, F, K), 1e-9)
        assertEquals(273.15, convert(32.0, F, K), 1e-9)
    }

    @Test
    fun sameUnitIsReturnedUnchanged() {
        assertEquals(21.5, convert(21.5, C, C), 1e-9)
        assertEquals(21.5, convert(21.5, F, F), 1e-9)
        assertEquals(21.5, convert(21.5, K, K), 1e-9)
    }

    @Test
    fun everyPairRoundTrips() {
        val units = listOf(C, F, K)
        for (from in units) {
            for (to in units) {
                assertEquals(
                    "$from -> $to -> $from",
                    21.5,
                    convert(convert(21.5, from, to), to, from),
                    1e-9,
                )
            }
        }
    }

    private val C get() = TemperatureUnit.DegreeCelsius
    private val F get() = TemperatureUnit.DegreeFahrenheit
    private val K get() = TemperatureUnit.Kelvin

    private fun convert(value: Double, from: TemperatureUnit, to: TemperatureUnit) =
        convertTemperature(value, from, to)
}
