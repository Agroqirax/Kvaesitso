package de.mm20.launcher2.calculator

/**
 * Rewrites notations mXparser doesn't accept: unicode math symbols, vulgar fractions,
 * superscript exponents, "**" -> "^", uppercase function names and constants, implicit
 * multiplication ("2√4"), and bare arguments ("sin 4" -> "sin(4)").
 *
 * Bracketing repeats to a fixpoint, so two levels of chaining resolve inside-out
 * ("sqrt sin 4" -> "sqrt(sin(4))"); deeper nesting still needs explicit brackets.
 */
internal object ExpressionPreprocessor {
    // "cbrt"/"root4" are placeholders, rewritten to mXparser's "root(order, x)" at the end.
    // The trailing space keeps a word boundary when the symbol is followed directly by its
    // argument ("√4"), which the bare-argument step relies on.
    private val unicodeOperatorAliases = mapOf(
        "√" to "sqrt ",
        "∛" to "cbrt ",
        "∜" to "root4 ",
        "−" to "-",
        "×" to "*",
        "÷" to "/",
        "∕" to "/",
        "≠" to "!=",
        "≥" to ">=",
        "≤" to "<=",
        "∧" to "&&",
        "∨" to "||",
    )

    private val fractionValues = mapOf(
        '¼' to "1/4", '½' to "1/2", '¾' to "3/4",
        '⅐' to "1/7", '⅑' to "1/9", '⅒' to "1/10",
        '⅓' to "1/3", '⅔' to "2/3",
        '⅕' to "1/5", '⅖' to "2/5", '⅗' to "3/5", '⅘' to "4/5",
        '⅙' to "1/6", '⅚' to "5/6",
        '⅛' to "1/8", '⅜' to "3/8", '⅝' to "5/8", '⅞' to "7/8",
        '↉' to "0/3",
    )

    // The whole part of a mixed number has to be an integer, so the lookbehind keeps digits
    // that follow a decimal separator out of it: "1.5½" is a product, not "1.(5+1/2)".
    private val vulgarFraction = Regex("(?<![.,])(\\d+)?[${fractionValues.keys.joinToString("")}]")

    private val superscriptCharacters = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-',
    )

    // A whole run at once, so "2¹⁰" becomes "2^(10)" rather than "2^(1)^(0)".
    private val superscriptRun = Regex("[${superscriptCharacters.keys.joinToString("")}]+")

    // Longer names come first where one is a prefix of another ("sinh" before "sin"): the
    // alternation takes the first match, not the longest. "Pi" is excluded on purpose - it
    // is mXparser's prime-counting function, not a casing of the constant "pi".
    private const val FUNCTION_NAMES =
        """asin|acos|atan|arcsin|arccos|arctan|sinh|cosh|tanh|sin|cos|tan|cot|sec|csc|ln|log2|log10|exp|sqrt|cbrt|root4|abs|floor|ceil|sgn|deg|rad"""

    // mXparser is case sensitive. Requiring a following "(" leaves ordinary words alone.
    private val bracketedFunctionCall = Regex("""(?i)\b($FUNCTION_NAMES)(\s*\()""")

    // The only two of mXparser's constants written as bare words; the rest are bracketed.
    // "e" needs a preceding space so words ending in it ("cosine") aren't read as a call.
    // Grouped so it stays one alternative wherever it is spliced into a larger pattern.
    private const val BARE_CONSTANTS = """(?:pi\b|(?<=\s)e\b)"""

    // Any bracketed constant or unit ("[phi]", "[km]"), self-delimiting so no list is needed.
    private const val BRACKETED_TOKEN = """\[[^\[\]]+\]"""

    // Case folding for the bare constants, so "2*PI" works.
    // Bracketed tokens are matched first so they keep their casing. "Pi(" is left alone: that
    // is the prime-counting function. \b keeps "2e5" and "cosine" intact.
    private val bareConstant = Regex("""(?i)$BRACKETED_TOKEN|\b(?:pi|e)\b(?!\s*\()""")

    private val bareFunctionArgument = Regex(
        """(?i)\b($FUNCTION_NAMES)\s*([+-]?\d+(?:[.,]\d+)?|(?:$FUNCTION_NAMES)\([^()]*\)|$BRACKETED_TOKEN|$BARE_CONSTANTS)"""
    )

    // Juxtaposition means multiplication: "2√4", "2(3+4)", "(1+2)(3+4)", "½3", "2[phi]".
    // Zero width, so each alternative just marks a position to insert "*" at.
    //
    // Only a digit, ")" or "]" can be the left side - a letter there would be a function call
    // ("sin(4)", "Pi(10)"). The right side is never a bare digit, so "10 20" stays two numbers
    // rather than becoming a product.
    private val implicitMultiplication = Regex(
        // ")(" ")2" ") pi" ")[phi]"
        """(?<=[)\]])(?=\s*[\w(\[])""" +
                // "2(" and "2pi", but not a digit that ends a function name or placeholder
                // ("log2(8)", "root4 sqrt 4"), and not scientific notation ("2e5", "5e-3")
                """|(?<![a-zA-Z]\d{0,9})(?<=\d)(?=\s*[(\[a-zA-Z])(?!\s*[eE][+-]?\d)"""
    )

    private val cubeRootCall = Regex("""cbrt\s*\(""")
    private val fourthRootCall = Regex("""root4\s*\(""")

    // Only guards against pathological input; bracketing normally reaches a fixpoint sooner.
    private const val MAX_BRACKETING_PASSES = 4

    fun preprocess(expression: String): String {
        var result = expression
        for ((symbol, replacement) in unicodeOperatorAliases) {
            result = result.replace(symbol, replacement)
        }
        // Parenthesized so a surrounding operator can't tear the fraction apart ("¾^2"). A
        // preceding whole number makes it a mixed number ("3½" -> "(3+1/2)").
        result = vulgarFraction.replace(result) { match ->
            val whole = match.groupValues[1]
            val fraction = fractionValues.getValue(match.value.last())
            if (whole.isEmpty()) "($fraction)" else "($whole+$fraction)"
        }
        // Parenthesized for the same reason as fractions, and to keep a sign with its
        // exponent ("10⁻³" -> "10^(-3)").
        result = superscriptRun.replace(result) { match ->
            match.value.map { superscriptCharacters.getValue(it) }.joinToString("", "^(", ")")
        }
        result = result.replace("**", "^")
        result = bracketedFunctionCall.replace(result) { match ->
            match.groupValues[1].lowercase() + match.groupValues[2]
        }
        result = bareConstant.replace(result) { match ->
            if (match.value.startsWith("[")) match.value else match.value.lowercase()
        }
        // Before bracketing, so the "*" gives "2√4" -> "2sqrt 4" the word boundary that the
        // bare-argument step needs to reach the "sqrt".
        result = implicitMultiplication.replace(result, "*")
        var passes = 0
        while (passes++ < MAX_BRACKETING_PASSES) {
            val next = bareFunctionArgument.replace(result) { match ->
                val argument = match.groupValues[2]
                // Bracketed tokens keep their casing: "[M]" (mega) differs from "[m]" (milli).
                val normalized = if (argument.all { it.isLetter() }) argument.lowercase() else argument
                "${match.groupValues[1].lowercase()}($normalized)"
            }
            if (next == result) break
            result = next
        }
        result = cubeRootCall.replace(result, "root(3, ")
        result = fourthRootCall.replace(result, "root(4, ")
        return result
    }
}
