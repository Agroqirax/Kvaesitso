package de.mm20.launcher2.unitconverter.converters

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureUnitWithFactorTest {

    private val centimeter = MeasureUnitWithFactor(factor = 0.01, symbol = "cm", nameResource = 0)
    private val squareCentimeter = MeasureUnitWithFactor(factor = 0.0001, symbol = "cm²", nameResource = 0)
    private val cubicMeter = MeasureUnitWithFactor(factor = 1.0, symbol = "m³", nameResource = 0)

    // Spanish and Chinese distinguish these by case alone.
    private val megabyte = MeasureUnitWithFactor(factor = 1e6, symbol = "MB", nameResource = 0)
    private val megabit = MeasureUnitWithFactor(factor = 1.25e5, symbol = "Mb", nameResource = 0)

    @Test
    fun matches_strictModeRequiresTheExactSymbol() {
        assertTrue(centimeter.matches("cm"))
        assertFalse(centimeter.matches("CM"))
        assertFalse(centimeter.matches("Cm"))
    }

    @Test
    fun matches_lenientModeIgnoresCase() {
        assertTrue(centimeter.matches("CM", lenient = true))
        assertTrue(centimeter.matches("Cm", lenient = true))
    }

    @Test
    fun matches_caseOnlyDistinctionsSurviveTheStrictPass() {
        // The point of the strict pass: "Mb" must not resolve to a megabyte.
        assertTrue(megabit.matches("Mb"))
        assertFalse(megabyte.matches("Mb"))
        assertTrue(megabyte.matches("MB"))
        assertFalse(megabit.matches("MB"))
    }

    @Test
    fun matches_unrelatedSymbolDoesNotMatchInEitherMode() {
        assertFalse(centimeter.matches("mm"))
        assertFalse(centimeter.matches("mm", lenient = true))
        assertFalse(centimeter.matches("c", lenient = true))
    }

    @Test
    fun matches_superscriptExponentAcceptsPlainDigitEquivalentInBothModes() {
        // No locale spells one unit "cm2" and another "cm²".
        assertTrue(squareCentimeter.matches("cm2"))
        assertTrue(cubicMeter.matches("m3"))
        assertTrue(squareCentimeter.matches("CM2", lenient = true))
        assertFalse(squareCentimeter.matches("CM2"))
    }

    @Test
    fun matches_superscriptExponentDoesNotAcceptTheWrongDigit() {
        assertFalse(squareCentimeter.matches("cm3", lenient = true))
        assertFalse(cubicMeter.matches("m2", lenient = true))
    }

    @Test
    fun matches_plainSymbolWithoutSuperscriptDoesNotAcceptAnyDigitSuffix() {
        assertFalse(centimeter.matches("cm2", lenient = true))
        assertFalse(centimeter.matches("cm3", lenient = true))
    }
}
