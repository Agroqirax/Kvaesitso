package de.mm20.launcher2.unitconverter

internal data class ParsedUnitQuery(
    val value: Double,
    val unit: String,
    val outputUnit: String?,
)

internal object UnitConverterQueryParser {

    /**
     * Splits `R.string.unit_converter_separators`, the complete keyword set for its language.
     * A "|"-separated string rather than a string-array because Weblate cannot translate
     * Android string-arrays. Blanks are dropped by [parse].
     */
    fun separators(configured: String): Set<String> = configured.split('|').toSet()

    // Only changes with the system locale, so one slot is plenty; a lost race rebuilds an
    // identical regex.
    @Volatile
    private var cachedRegex: Pair<Set<String>, Regex>? = null

    fun parse(query: String, separators: Set<String>): ParsedUnitQuery? {
        val keywords = separators.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val matches = regexFor(keywords).findAll(query)
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()

        var inputValue: Double? = null
        var inputUnit: String? = null
        var outputUnit: String? = null

        for ((i, value) in matches.withIndex()) {
            when (i) {
                0 -> {
                    val inputStr = value.trim()
                    inputValue = inputStr.toDoubleOrNull()
                        ?: inputStr.replace(',', '.').toDoubleOrNull()
                                ?: return null
                }
                // Unconditional, so a unit spelled like a keyword still works: German "10 in".
                1 -> inputUnit = value.trim()
                2 -> {
                    if (!value.contains("-") && !value.contains(">") &&
                        !keywords.any { it.equals(value.trim(), ignoreCase = true) }
                    ) {
                        outputUnit = value.trim()
                    }
                }
                3 -> {
                    if (outputUnit == null) {
                        outputUnit = value.trim()
                        break
                    } else {
                        return null
                    }
                }
                else -> return null
            }
        }

        if (inputValue == null || inputUnit == null) return null

        return ParsedUnitQuery(inputValue, inputUnit, outputUnit)
    }

    private fun regexFor(keywords: Set<String>): Regex {
        cachedRegex?.let { (cachedKeywords, regex) -> if (cachedKeywords == keywords) return regex }
        val regex = buildRegex(keywords)
        cachedRegex = keywords to regex
        return regex
    }

    private fun buildRegex(keywords: Set<String>): Regex {
        // Longest first, so the alternation can't settle for a prefix of a longer keyword.
        val alternatives = keywords
            .sortedWith(compareByDescending<String> { it.length }.thenBy { it })
            .joinToString("|") { Regex.escape(it) }
        // \b on both sides, or a keyword would cut "long ton" and "min" in half.
        val keyword = if (alternatives.isEmpty()) null else """\b(?:$alternatives)\b"""

        val branches = listOfNotNull(
            // Ahead of the number branch: "e+-,." are all number characters, so a keyword
            // like French "en" would otherwise be split into "e" and "n".
            keyword,
            """[+\-]?[\d+\-e,.]+""",
            // The run can never consume a digit, so [23]? picks up the one it stopped at,
            // keeping a typed exponent in the unit token ("cm2").
            """(?:(?!\d|>|-${keyword?.let { "|$it" }.orEmpty()}).)+[23]?""",
        )
        return Regex("""(?i)(${branches.joinToString("|")})""")
    }
}
