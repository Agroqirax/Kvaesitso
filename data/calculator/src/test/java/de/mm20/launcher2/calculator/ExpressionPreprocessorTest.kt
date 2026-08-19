package de.mm20.launcher2.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressionPreprocessorTest {

    @Test
    fun preprocess_doubleAsteriskIsRewrittenToCaret() {
        assertEquals("2^3", ExpressionPreprocessor.preprocess("2**3"))
        assertEquals("2 ^ 3", ExpressionPreprocessor.preprocess("2 ** 3"))
        assertEquals("2^-3", ExpressionPreprocessor.preprocess("2**-3"))
    }

    @Test
    fun preprocess_bareNumericArgumentGetsParenthesized() {
        assertEquals("sin(4)", ExpressionPreprocessor.preprocess("sin 4"))
        assertEquals("sin(4)", ExpressionPreprocessor.preprocess("sin4"))
        assertEquals("asin(-0.5)", ExpressionPreprocessor.preprocess("asin -0.5"))
        assertEquals("sqrt(0,5)", ExpressionPreprocessor.preprocess("sqrt 0,5"))
    }

    @Test
    fun preprocess_functionNameCasingIsFoldedToLowercase() {
        assertEquals("sin(4)", ExpressionPreprocessor.preprocess("SIN 4"))
        assertEquals("sin(4)", ExpressionPreprocessor.preprocess("Sin4"))
    }

    @Test
    fun preprocess_twoLevelsOfBareChainingResolveInsideOut() {
        assertEquals("sqrt(sin(4))", ExpressionPreprocessor.preprocess("sqrt sin 4"))
        assertEquals("sqrt(sin(4))", ExpressionPreprocessor.preprocess("sqrt sin(4)"))
    }

    @Test
    fun preprocess_threeLevelsOfBareChainingOnlyPartiallyResolve() {
        // Limitation: the argument match can't see through nested parentheses.
        assertEquals("sqrt sin(cos(4))", ExpressionPreprocessor.preprocess("sqrt sin cos 4"))
    }

    @Test
    fun preprocess_alreadyBracketedCallsKeepTheirBrackets() {
        assertEquals("sin(30)+cos(60)", ExpressionPreprocessor.preprocess("sin(30)+cos(60)"))
        assertEquals("sqrt(sin(4))", ExpressionPreprocessor.preprocess("sqrt(sin(4))"))
    }

    @Test
    fun preprocess_bracketedCallCasingIsFoldedToLowercase() {
        // mxparser is case sensitive, so "COS(4)" would otherwise be a syntax error.
        assertEquals("cos(4)", ExpressionPreprocessor.preprocess("COS(4)"))
        assertEquals("sin(30)+cos(60)", ExpressionPreprocessor.preprocess("SIN(30)+Cos(60)"))
        assertEquals("sqrt (4)", ExpressionPreprocessor.preprocess("SQRT (4)"))
        assertEquals("sqrt(sin(4))", ExpressionPreprocessor.preprocess("SQRT SIN(4)"))
    }

    @Test
    fun preprocess_bracketedRootPlaceholderCasingIsFoldedBeforeRewriting() {
        assertEquals("root(3, 8)", ExpressionPreprocessor.preprocess("CBRT(8)"))
        assertEquals("root(4, 16)", ExpressionPreprocessor.preprocess("Root4(16)"))
    }

    @Test
    fun preprocess_wordsContainingAFunctionNameAreNotRecased() {
        // Only a name directly followed by "(" is folded, so ordinary words survive intact.
        assertEquals("COSINE", ExpressionPreprocessor.preprocess("COSINE"))
        assertEquals("TANGENT 4", ExpressionPreprocessor.preprocess("TANGENT 4"))
    }

    @Test
    fun preprocess_constantsAreAcceptedAsBareArguments() {
        assertEquals("sqrt(pi)", ExpressionPreprocessor.preprocess("sqrt pi"))
        assertEquals("sqrt(pi)", ExpressionPreprocessor.preprocess("√pi"))
        assertEquals("sin(pi)", ExpressionPreprocessor.preprocess("SIN PI"))
        assertEquals("ln(e)", ExpressionPreprocessor.preprocess("ln e"))
    }

    @Test
    fun preprocess_bracketedConstantsAndUnitsAreAcceptedAsBareArguments() {
        // Every constant other than "pi" and "e" is bracketed, so needs no list.
        assertEquals("sqrt([phi])", ExpressionPreprocessor.preprocess("sqrt [phi]"))
        assertEquals("ln([gam])", ExpressionPreprocessor.preprocess("ln [gam]"))
        assertEquals("sqrt([c])", ExpressionPreprocessor.preprocess("√[c]"))
        assertEquals("abs([Earth-R])", ExpressionPreprocessor.preprocess("abs [Earth-R]"))
    }

    @Test
    fun preprocess_bracketedTokenCasingIsPreserved() {
        // mxparser distinguishes "[M]" (mega) from "[m]" (milli).
        assertEquals("sqrt([M])", ExpressionPreprocessor.preprocess("sqrt [M]"))
        assertEquals("sqrt([m])", ExpressionPreprocessor.preprocess("sqrt [m]"))
    }

    @Test
    fun preprocess_wordEndingInEIsNotReadAsACallOnTheConstant() {
        // "e" only counts as a bare argument when separated by a space.
        assertEquals("sine", ExpressionPreprocessor.preprocess("sine"))
        assertEquals("cosine", ExpressionPreprocessor.preprocess("cosine"))
    }

    @Test
    fun preprocess_wordsWithNoSupportedFunctionNameAreLeftUntouched() {
        assertEquals("foo 4", ExpressionPreprocessor.preprocess("foo 4"))
        assertEquals("log(2,3)", ExpressionPreprocessor.preprocess("log(2,3)"))
    }

    @Test
    fun preprocess_scientificNotationIsNotMistakenForABareExpCall() {
        assertEquals("2e5", ExpressionPreprocessor.preprocess("2e5"))
        assertEquals("5e-3", ExpressionPreprocessor.preprocess("5e-3"))
    }

    @Test
    fun preprocess_plainExpressionsWithoutFunctionsAreUnaffected() {
        assertEquals("2+3*4-1/2", ExpressionPreprocessor.preprocess("2+3*4-1/2"))
    }

    @Test
    fun preprocess_unicodeSquareRootIsRewrittenAndParenthesized() {
        assertEquals("sqrt(4)", ExpressionPreprocessor.preprocess("√4"))
        // The extra space before "(" is harmless; see ExpressionEvaluationTest.
        assertEquals("sqrt (4)", ExpressionPreprocessor.preprocess("√(4)"))
        assertEquals("sqrt(4)", ExpressionPreprocessor.preprocess("√ 4"))
        assertEquals("sqrt(sqrt(4))", ExpressionPreprocessor.preprocess("√√4"))
    }

    @Test
    fun preprocess_unicodeMinusIsRewrittenToHyphen() {
        assertEquals("2-3", ExpressionPreprocessor.preprocess("2−3"))
        assertEquals("-3", ExpressionPreprocessor.preprocess("−3"))
    }

    @Test
    fun preprocess_unicodeMultiplicationAndDivisionAreRewrittenToAsciiEquivalents() {
        assertEquals("2*3", ExpressionPreprocessor.preprocess("2×3"))
        assertEquals("2/3", ExpressionPreprocessor.preprocess("2÷3"))
        assertEquals("2/3", ExpressionPreprocessor.preprocess("2∕3"))
    }

    @Test
    fun preprocess_unicodeComparisonAndBooleanOperatorsAreRewritten() {
        assertEquals("1!=2", ExpressionPreprocessor.preprocess("1≠2"))
        assertEquals("1>=2", ExpressionPreprocessor.preprocess("1≥2"))
        assertEquals("1<=2", ExpressionPreprocessor.preprocess("1≤2"))
        assertEquals("1&&2", ExpressionPreprocessor.preprocess("1∧2"))
        assertEquals("1||2", ExpressionPreprocessor.preprocess("1∨2"))
    }

    @Test
    fun preprocess_unicodeOperatorsCombineWithOtherRewrites() {
        assertEquals("2*sqrt(4)^2", ExpressionPreprocessor.preprocess("2×√4**2"))
    }

    @Test
    fun preprocess_unicodeCubeAndFourthRootAreRewrittenToRootCalls() {
        assertEquals("root(3, 8)", ExpressionPreprocessor.preprocess("∛8"))
        assertEquals("root(3, 8)", ExpressionPreprocessor.preprocess("∛(8)"))
        assertEquals("root(4, 16)", ExpressionPreprocessor.preprocess("∜16"))
    }

    @Test
    fun preprocess_chainedUnicodeRootsResolveInsideOut() {
        assertEquals("sqrt(root(3, 8))", ExpressionPreprocessor.preprocess("√∛8"))
    }

    @Test
    fun preprocess_standaloneVulgarFractionBecomesParenthesizedDivision() {
        assertEquals("(1/2)", ExpressionPreprocessor.preprocess("½"))
        assertEquals("(3/4)", ExpressionPreprocessor.preprocess("¾"))
        assertEquals("(1/3)", ExpressionPreprocessor.preprocess("⅓"))
    }

    @Test
    fun preprocess_vulgarFractionWithPrecedingWholeNumberBecomesAMixedNumber() {
        assertEquals("(3+1/2)", ExpressionPreprocessor.preprocess("3½"))
        assertEquals("(12+1/2)", ExpressionPreprocessor.preprocess("12½"))
        assertEquals("-(3+1/2)", ExpressionPreprocessor.preprocess("-3½"))
    }

    @Test
    fun preprocess_decimalNumberBeforeAFractionIsAProductNotAMixedNumber() {
        // A mixed number needs a whole number in front, so this falls through to the
        // juxtaposition rule the way "3 ½" does, rather than splitting "1.5" into "1.(5+1/2)".
        assertEquals("1.5*(1/2)", ExpressionPreprocessor.preprocess("1.5½"))
        assertEquals("1,5*(1/2)", ExpressionPreprocessor.preprocess("1,5½"))
    }

    @Test
    fun preprocess_vulgarFractionIsProtectedFromSurroundingOperatorPrecedence() {
        assertEquals("(3+1/2)*2", ExpressionPreprocessor.preprocess("3½×2"))
        assertEquals("(3/4)^2", ExpressionPreprocessor.preprocess("¾^2"))
    }

    @Test
    fun preprocess_superscriptDigitBecomesAnExponent() {
        assertEquals("4^(2)", ExpressionPreprocessor.preprocess("4²"))
        assertEquals("4^(3)", ExpressionPreprocessor.preprocess("4³"))
        assertEquals("4^(9)", ExpressionPreprocessor.preprocess("4⁹"))
    }

    @Test
    fun preprocess_runOfSuperscriptDigitsBecomesASingleExponent() {
        assertEquals("2^(10)", ExpressionPreprocessor.preprocess("2¹⁰"))
        assertEquals("2^(100)", ExpressionPreprocessor.preprocess("2¹⁰⁰"))
    }

    @Test
    fun preprocess_superscriptSignIsKeptWithItsExponent() {
        assertEquals("10^(-3)", ExpressionPreprocessor.preprocess("10⁻³"))
        assertEquals("10^(+3)", ExpressionPreprocessor.preprocess("10⁺³"))
    }

    @Test
    fun preprocess_superscriptExponentCombinesWithOtherRewrites() {
        assertEquals("sqrt(4)^(2)", ExpressionPreprocessor.preprocess("√4²"))
        assertEquals("(3/4)^(2)", ExpressionPreprocessor.preprocess("¾²"))
        assertEquals("2*4^(2)", ExpressionPreprocessor.preprocess("2×4²"))
    }

    @Test
    fun preprocess_standaloneConstantCasingIsFoldedToLowercase() {
        // Previously only folded in argument position, so "sin PI" worked but "2*PI" did not.
        assertEquals("2*pi", ExpressionPreprocessor.preprocess("2*PI"))
        assertEquals("2^pi", ExpressionPreprocessor.preprocess("2^PI"))
        assertEquals("e", ExpressionPreprocessor.preprocess("E"))
        assertEquals("e+1", ExpressionPreprocessor.preprocess("E+1"))
    }

    @Test
    fun preprocess_primeCountingFunctionIsNotFoldedIntoTheConstant() {
        // mxparser's "Pi(x)" counts primes; lowercasing it would silently change the meaning.
        assertEquals("Pi(10)", ExpressionPreprocessor.preprocess("Pi(10)"))
        assertEquals("2*Pi(10)", ExpressionPreprocessor.preprocess("2*Pi(10)"))
    }

    @Test
    fun preprocess_constantFoldingLeavesBracketedTokenCasingAlone() {
        // "[M]" (mega) differs from "[m]" (milli), and "[Earth-R]" contains a bare "E".
        assertEquals("[Earth-R]", ExpressionPreprocessor.preprocess("[Earth-R]"))
        assertEquals("2*[M]", ExpressionPreprocessor.preprocess("2*[M]"))
    }

    @Test
    fun preprocess_constantFoldingDoesNotTouchScientificNotationOrWords() {
        assertEquals("2e5", ExpressionPreprocessor.preprocess("2e5"))
        assertEquals("2E5", ExpressionPreprocessor.preprocess("2E5"))
        assertEquals("cosine", ExpressionPreprocessor.preprocess("cosine"))
    }

    @Test
    fun preprocess_juxtapositionBecomesMultiplication() {
        assertEquals("2*(3+4)", ExpressionPreprocessor.preprocess("2(3+4)"))
        assertEquals("(1+2)*(3+4)", ExpressionPreprocessor.preprocess("(1+2)(3+4)"))
        assertEquals("(1+2)*3", ExpressionPreprocessor.preprocess("(1+2)3"))
        assertEquals("2*pi", ExpressionPreprocessor.preprocess("2pi"))
        assertEquals("2*[phi]", ExpressionPreprocessor.preprocess("2[phi]"))
        assertEquals("sqrt(4)*2", ExpressionPreprocessor.preprocess("sqrt(4)2"))
    }

    @Test
    fun preprocess_juxtapositionReachesAFunctionThatNeedsBracketing() {
        // "2sqrt 4" has no word boundary before "sqrt", so the "*" has to be inserted first.
        assertEquals("2*sqrt(4)", ExpressionPreprocessor.preprocess("2√4"))
        assertEquals("2*root(3, 8)", ExpressionPreprocessor.preprocess("2∛8"))
        assertEquals("(1/2)*3", ExpressionPreprocessor.preprocess("½3"))
        assertEquals("(3+1/2)*(1/2)", ExpressionPreprocessor.preprocess("3½½"))
    }

    @Test
    fun preprocess_juxtapositionBridgesWhitespace() {
        assertEquals("2* sqrt(4)", ExpressionPreprocessor.preprocess("2 √4"))
        assertEquals("2* (3+4)", ExpressionPreprocessor.preprocess("2 (3+4)"))
        assertEquals("2* pi", ExpressionPreprocessor.preprocess("2 pi"))
        assertEquals("(1+2)* (3+4)", ExpressionPreprocessor.preprocess("(1+2) (3+4)"))
    }

    @Test
    fun preprocess_spaceDistinguishesAProductFromAMixedNumber() {
        assertEquals("(3+1/2)", ExpressionPreprocessor.preprocess("3½"))
        assertEquals("3* (1/2)", ExpressionPreprocessor.preprocess("3 ½"))
    }

    @Test
    fun preprocess_juxtapositionDoesNotSplitScientificNotation() {
        assertEquals("2e5", ExpressionPreprocessor.preprocess("2e5"))
        assertEquals("5e-3", ExpressionPreprocessor.preprocess("5e-3"))
        assertEquals("1.5e3", ExpressionPreprocessor.preprocess("1.5e3"))
        assertEquals("2 e5", ExpressionPreprocessor.preprocess("2 e5"))
        // "e" not followed by an exponent is the constant, so it does multiply.
        assertEquals("2*e", ExpressionPreprocessor.preprocess("2e"))
        assertEquals("2*exp(0)", ExpressionPreprocessor.preprocess("2exp(0)"))
    }

    @Test
    fun preprocess_juxtapositionDoesNotBreakADigitInsideAFunctionName() {
        assertEquals("log2(8)", ExpressionPreprocessor.preprocess("log2(8)"))
        assertEquals("log10(100)", ExpressionPreprocessor.preprocess("log10(100)"))
        assertEquals("log2 (8)", ExpressionPreprocessor.preprocess("log2 (8)"))
        // "root4" is the internal placeholder for "∜" and ends in a digit too.
        assertEquals("root(4, 16)", ExpressionPreprocessor.preprocess("root4 16"))
        assertEquals("root(4, sqrt(4))", ExpressionPreprocessor.preprocess("∜√4"))
    }

    @Test
    fun preprocess_juxtapositionLeavesFunctionCallsAndPlainNumbersAlone() {
        assertEquals("sin(4)", ExpressionPreprocessor.preprocess("sin(4)"))
        assertEquals("Pi(10)", ExpressionPreprocessor.preprocess("Pi(10)"))
        assertEquals("sin(4) + 3", ExpressionPreprocessor.preprocess("sin(4) + 3"))
        // Two numbers are not a product; that is far more likely to be a typo.
        assertEquals("10 20", ExpressionPreprocessor.preprocess("10 20"))
    }
}
