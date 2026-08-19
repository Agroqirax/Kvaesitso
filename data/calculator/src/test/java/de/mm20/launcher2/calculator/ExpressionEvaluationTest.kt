package de.mm20.launcher2.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mariuszgromada.math.mxparser.Expression

/** Checks the rewrites against the real mxparser engine, not just the string shape. */
class ExpressionEvaluationTest {

    private fun evaluate(rawExpression: String): Double {
        val preprocessed = ExpressionPreprocessor.preprocess(rawExpression)
        val expression = Expression(preprocessed)
        assertTrue("expected valid syntax for \"$preprocessed\"", expression.checkSyntax())
        return expression.calculate()
    }

    @Test
    fun unicodeSquareRootEvaluatesCorrectly() {
        assertEquals(2.0, evaluate("√4"), 1e-9)
        assertEquals(2.0, evaluate("√(4)"), 1e-9)
        assertEquals(2.0, evaluate("√ 4"), 1e-9)
        assertEquals(2.0, evaluate("√√16"), 1e-9)
    }

    @Test
    fun unicodeArithmeticOperatorsEvaluateCorrectly() {
        assertEquals(6.0, evaluate("2×3"), 1e-9)
        assertEquals(2.0, evaluate("6÷3"), 1e-9)
        assertEquals(2.0, evaluate("6∕3"), 1e-9)
        assertEquals(-1.0, evaluate("2−3"), 1e-9)
    }

    @Test
    fun unicodeComparisonAndBooleanOperatorsEvaluateCorrectly() {
        assertEquals(1.0, evaluate("1≠2"), 1e-9)
        assertEquals(0.0, evaluate("1≥2"), 1e-9)
        assertEquals(1.0, evaluate("1≤2"), 1e-9)
        assertEquals(0.0, evaluate("1∧0"), 1e-9)
        assertEquals(1.0, evaluate("1∨0"), 1e-9)
    }

    @Test
    fun doubleAsteriskEvaluatesAsExponentiation() {
        assertEquals(8.0, evaluate("2**3"), 1e-9)
    }

    @Test
    fun bareFunctionArgumentsEvaluateCorrectly() {
        assertEquals(0.0, evaluate("sin 0"), 1e-9)
        assertEquals(2.0, evaluate("sqrt sqrt 16"), 1e-9)
    }

    @Test
    fun constantsAsBareArgumentsEvaluateCorrectly() {
        assertEquals(0.0, evaluate("sin pi"), 1e-9)
        assertEquals(-1.0, evaluate("cos pi"), 1e-9)
        assertEquals(1.0, evaluate("ln e"), 1e-9)
        assertEquals(1.7724538509055159, evaluate("√pi"), 1e-9)
    }

    @Test
    fun bracketedConstantsAsBareArgumentsEvaluateCorrectly() {
        assertEquals(1.272019649514069, evaluate("sqrt [phi]"), 1e-9)
        assertEquals(1.0, evaluate("abs [true]"), 1e-9)
    }

    @Test
    fun uppercaseFunctionNamesEvaluateCorrectly() {
        // mxparser rejects "COS(4)" outright, so this only passes because of the case fold.
        assertEquals(-0.6536436208636119, evaluate("COS(4)"), 1e-9)
        assertEquals(2.0, evaluate("SQRT(4)"), 1e-9)
        assertEquals(2.0, evaluate("CBRT(8)"), 1e-9)
        assertEquals(0.0, evaluate("SIN PI"), 1e-9)
    }

    @Test
    fun nthRootIsSupportedAsATwoArgumentFunction() {
        // mxparser has a "root(rootorder, number)" function (since 4.1).
        assertEquals(2.0, evaluate("root(3, 8)"), 1e-9)
        assertEquals(3.0, evaluate("root(2, 9)"), 1e-9)
    }

    @Test
    fun unicodeCubeAndFourthRootEvaluateCorrectly() {
        assertEquals(2.0, evaluate("∛8"), 1e-9)
        assertEquals(3.0, evaluate("∛27"), 1e-9)
        assertEquals(2.0, evaluate("∜16"), 1e-9)
        assertEquals(1.4142135623730951, evaluate("√∛8"), 1e-9) // sqrt(cbrt(8)) = sqrt(2)
    }

    @Test
    fun unicodeVulgarFractionsEvaluateCorrectly() {
        assertEquals(0.5, evaluate("½"), 1e-9)
        assertEquals(0.75, evaluate("¾"), 1e-9)
        assertEquals(1.0 / 3.0, evaluate("⅓"), 1e-9)
        assertEquals(0.8333333333333, evaluate("⅚"), 1e-9)
    }

    @Test
    fun mixedNumberVulgarFractionsEvaluateCorrectly() {
        assertEquals(3.5, evaluate("3½"), 1e-9)
        assertEquals(12.5, evaluate("12½"), 1e-9)
        assertEquals(-3.5, evaluate("-3½"), 1e-9)
    }

    @Test
    fun decimalNumberBeforeAFractionEvaluatesAsAProduct() {
        assertEquals(0.75, evaluate("1.5½"), 1e-9)
    }

    @Test
    fun superscriptExponentsEvaluateCorrectly() {
        assertEquals(16.0, evaluate("4²"), 1e-9)
        assertEquals(1024.0, evaluate("2¹⁰"), 1e-9)
        assertEquals(0.001, evaluate("10⁻³"), 1e-9)
        assertEquals(4.0, evaluate("√4²"), 1e-9)
        assertEquals(0.5625, evaluate("¾²"), 1e-9)
    }

    @Test
    fun standaloneConstantsEvaluateRegardlessOfCasing() {
        assertEquals(6.283185307179586, evaluate("2×PI"), 1e-9)
        assertEquals(2.718281828459045, evaluate("E"), 1e-9)
        assertEquals(9.869604401089358, evaluate("PI×PI"), 1e-9)
    }

    @Test
    fun implicitMultiplicationEvaluatesCorrectly() {
        assertEquals(4.0, evaluate("2√4"), 1e-9)
        assertEquals(4.0, evaluate("2 √4"), 1e-9)
        assertEquals(14.0, evaluate("2(3+4)"), 1e-9)
        assertEquals(21.0, evaluate("(1+2)(3+4)"), 1e-9)
        assertEquals(9.0, evaluate("(1+2)3"), 1e-9)
        assertEquals(6.283185307179586, evaluate("2pi"), 1e-9)
        assertEquals(4.0, evaluate("2∛8"), 1e-9)
        assertEquals(6.0, evaluate("√4 √9"), 1e-9)
        assertEquals(1.5, evaluate("½3"), 1e-9)
    }

    @Test
    fun scientificNotationSurvivesImplicitMultiplication() {
        assertEquals(200000.0, evaluate("2e5"), 1e-9)
        assertEquals(0.005, evaluate("5e-3"), 1e-9)
        assertEquals(1500.0, evaluate("1.5e3"), 1e-9)
        assertEquals(5.43656365691809, evaluate("2e"), 1e-9)
    }

    @Test
    fun functionNamesEndingInADigitSurviveImplicitMultiplication() {
        assertEquals(3.0, evaluate("log2(8)"), 1e-9)
        assertEquals(2.0, evaluate("log10(100)"), 1e-9)
        assertEquals(2.0, evaluate("root4 16"), 1e-9)
    }

    @Test
    fun primeCountingFunctionStillEvaluatesAsItself() {
        // Four primes below 10, i.e. not lowercased into the constant "pi".
        assertEquals(4.0, evaluate("Pi(10)"), 1e-9)
    }

    @Test
    fun vulgarFractionsAreNotTornApartBySurroundingOperatorPrecedence() {
        // A naive "3/4^2" would divide 3 by 4^2 instead.
        assertEquals(0.5625, evaluate("¾^2"), 1e-9)
        // A naive "3+1/2*2" would apply * before the implied +.
        assertEquals(7.0, evaluate("3½×2"), 1e-9)
    }
}
