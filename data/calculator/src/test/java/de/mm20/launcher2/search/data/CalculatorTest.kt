package de.mm20.launcher2.search.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CalculatorTest {

    // DecimalFormat follows the default locale; pin it so expectations are stable.
    private lateinit var defaultLocale: Locale

    @Before
    fun pinLocale() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.ROOT)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun nanSolution_formattedAsNaNInEveryBase() {
        val calculator = Calculator(term = "0/0", solution = Double.NaN)
        assertEquals("NaN", calculator.formattedString)
        assertEquals("NaN", calculator.formattedBinaryString)
        assertEquals("NaN", calculator.formattedOctString)
        assertEquals("NaN", calculator.formattedHexString)
    }

    @Test
    fun formattedString_plainNumbers() {
        assertEquals("10", Calculator(term = "10", solution = 10.0).formattedString)
        assertEquals("2.5", Calculator(term = "2.5", solution = 2.5).formattedString)
        assertEquals("0", Calculator(term = "0", solution = 0.0).formattedString)
    }

    @Test
    fun getBeatifiedTerm_hexLiteral() {
        val calculator = Calculator(term = "0x1A", solution = 26.0)
        assertEquals("1A₁₆", calculator.getBeatifiedTerm())
    }

    @Test
    fun getBeatifiedTerm_binaryLiteral() {
        val calculator = Calculator(term = "0b101", solution = 5.0)
        assertEquals("101₂", calculator.getBeatifiedTerm())
    }

    @Test
    fun getBeatifiedTerm_octalLiteral() {
        val calculator = Calculator(term = "0755", solution = 493.0)
        assertEquals("755₈", calculator.getBeatifiedTerm())
    }

    @Test
    fun getBeatifiedTerm_arithmeticOperatorsAreSpacedAndSymbolized() {
        val calculator = Calculator(term = "2+3*4-1/2", solution = 13.5)
        assertEquals("2 + 3 × 4 − 1 ∕ 2", calculator.getBeatifiedTerm())
    }

    @Test
    fun getBeatifiedTerm_comparisonAndBooleanOperatorsAreSymbolized() {
        assertEquals("1 ≠ 2", Calculator(term = "1!=2", solution = 1.0).getBeatifiedTerm())
        assertEquals("1 ≥ 2", Calculator(term = "1>=2", solution = 0.0).getBeatifiedTerm())
        assertEquals("1 ≤ 2", Calculator(term = "1<=2", solution = 1.0).getBeatifiedTerm())
        assertEquals("1 ∧ 2", Calculator(term = "1&&2", solution = 1.0).getBeatifiedTerm())
        assertEquals("1 ∨ 2", Calculator(term = "1||2", solution = 1.0).getBeatifiedTerm())
    }

    @Test
    fun getBeatifiedTerm_whitespaceInTermIsStripped() {
        // Asserting on "2 + 3" would prove nothing: the "+" rule puts those spaces back.
        assertEquals("12", Calculator(term = "1 2", solution = 12.0).getBeatifiedTerm())
        assertEquals("sqrt(4)", Calculator(term = "sqrt (4)", solution = 2.0).getBeatifiedTerm())
        assertEquals("2 + 3", Calculator(term = "2   +   3", solution = 5.0).getBeatifiedTerm())
    }

    @Test
    fun getBeatifiedTerm_preprocessedTermsAreDisplayedAsEvaluated() {
        // The repository deliberately passes the rewritten expression as the term, so the
        // user sees what was actually evaluated once bare arguments made the input ambiguous.
        assertEquals("root(3,8)", Calculator(term = "root(3, 8)", solution = 2.0).getBeatifiedTerm())
        assertEquals("sqrt( π )", Calculator(term = "sqrt(pi)", solution = 1.77).getBeatifiedTerm())
    }
}
