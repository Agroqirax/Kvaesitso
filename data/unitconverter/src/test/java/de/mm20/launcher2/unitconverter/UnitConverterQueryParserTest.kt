package de.mm20.launcher2.unitconverter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitConverterQueryParserTest {

    // What each language's unit_converter_separators resource would hold. English is one
    // configuration among several here, not a baseline the others are measured against.
    private val en = UnitConverterQueryParser.separators("to")
    private val de = UnitConverterQueryParser.separators("in|nach")
    private val nl = UnitConverterQueryParser.separators("in|naar")
    private val fr = UnitConverterQueryParser.separators("en")

    private fun parse(query: String, separators: Set<String> = en) =
        UnitConverterQueryParser.parse(query, separators)

    @Test
    fun parse_valueAndUnitWithoutOutputUnit() {
        assertEquals(ParsedUnitQuery(10.0, "cm", null), parse("10cm"))
    }

    @Test
    fun parse_dashSeparatorForm() {
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10cm-in"))
    }

    @Test
    fun parse_angleBracketSeparatorForm() {
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10cm>in"))
    }

    @Test
    fun parse_keywordSeparatorFormWithSpaces() {
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10 cm to in"))
    }

    @Test
    fun parse_keywordSeparatorFormWithoutSpaces() {
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10cm to in"))
    }

    @Test
    fun parse_exponentSuffixIsAbsorbedIntoTheUnitToken() {
        assertEquals(ParsedUnitQuery(10.0, "cm2", "m2"), parse("10cm2 to m2"))
        assertEquals(ParsedUnitQuery(10.0, "cm2", null), parse("10cm2"))
        assertEquals(ParsedUnitQuery(10.0, "m3", null), parse("10m3"))
    }

    @Test
    fun parse_literalSuperscriptExponentIsKeptAsIs() {
        assertEquals(ParsedUnitQuery(10.0, "cm²", "m²"), parse("10cm² to m²"))
    }

    @Test
    fun parse_negativeValue() {
        assertEquals(ParsedUnitQuery(-5.0, "C", "F"), parse("-5C to F"))
    }

    @Test
    fun parse_commaDecimalSeparator() {
        assertEquals(ParsedUnitQuery(10.5, "cm", "in"), parse("10,5cm-in"))
    }

    @Test
    fun parse_missingUnitReturnsNull() {
        assertNull(parse("10"))
    }

    @Test
    fun parse_blankQueryReturnsNull() {
        assertNull(parse(""))
    }

    @Test
    fun parse_ambiguousOutputUnitReturnsNull() {
        // Two candidate output units ("5", "ft") - rejected rather than guessed at.
        assertNull(parse("10cm5ft"))
    }

    @Test
    fun parse_multiWordUnitsWithPunctuationSurviveIntact() {
        assertEquals(ParsedUnitQuery(10.0, "fl oz US", "mL"), parse("10 fl oz US to mL"))
        assertEquals(ParsedUnitQuery(3.0, "ton (EE. UU.)", null), parse("3 ton (EE. UU.)"))
        assertEquals(ParsedUnitQuery(10.0, "m/s", "km/h"), parse("10 m/s to km/h"))
    }

    @Test
    fun separators_splitsTheTranslatedListOnVerticalBars() {
        assertEquals(setOf("in", "naar"), UnitConverterQueryParser.separators("in|naar"))
        assertEquals(setOf("en"), UnitConverterQueryParser.separators("en"))
    }

    @Test
    fun separators_areExactlyWhatTheLanguageDeclares() {
        // No language gets another language's keywords bolted on: English is not special.
        // An unrecognised keyword is not a separator, so it is swallowed by the unit token
        // and no converter will match it.
        assertEquals(setOf("to"), UnitConverterQueryParser.separators("to"))
        assertEquals(ParsedUnitQuery(10.0, "cm to mm", null), parse("10 cm to mm", de))
        assertEquals(ParsedUnitQuery(10.0, "cm in mm", null), parse("10 cm in mm", en))
        // A language that does want English "to" says so in its own translation.
        assertEquals(
            ParsedUnitQuery(10.0, "cm", "mm"),
            parse("10 cm to mm", UnitConverterQueryParser.separators("in|nach|to")),
        )
    }

    @Test
    fun parse_everyDeclaredKeywordIsAccepted() {
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm in mm", nl))
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm naar mm", nl))
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm in mm", de))
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm nach mm", de))
        assertEquals(ParsedUnitQuery(5.0, "Go", "To"), parse("5 Go en To", fr))
    }

    @Test
    fun parse_dashAndAngleBracketWorkWithoutAnyKeyword() {
        // They are not part of the translated list, so they survive an empty translation.
        val none = UnitConverterQueryParser.separators("")
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10cm-in", none))
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10cm>in", none))
    }

    @Test
    fun parse_toleratesSloppyTranslations() {
        // Padding and stray bars are dropped rather than compiled into the regex.
        val sloppy = UnitConverterQueryParser.separators(" in | naar |")
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm naar mm", sloppy))
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm in mm", sloppy))
    }

    @Test
    fun parse_separatorIsNotMatchedInsideALongerWord() {
        // "in" must not split "min", nor "to" split "ton".
        assertEquals(ParsedUnitQuery(90.0, "min", "h"), parse("90 min in h", de))
        assertEquals(ParsedUnitQuery(2.0, "long ton", "kg"), parse("2 long ton in kg", de))
        assertEquals(ParsedUnitQuery(2.0, "long ton", "kg"), parse("2 long ton to kg"))
        // Spanish spells the long ton "ton imp".
        assertEquals(ParsedUnitQuery(2.0, "ton imp", "kg"), parse("2 ton imp to kg"))
    }

    @Test
    fun parse_separatorThatIsAlsoAUnitSymbolStaysUsableAsAUnit() {
        // German spells both the separator and the inch "in".
        assertEquals(ParsedUnitQuery(10.0, "in", null), parse("10 in", de))
        assertEquals(ParsedUnitQuery(10.0, "in", "cm"), parse("10 in in cm", de))
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10 cm in in", de))
        // French spells terabyte "To", which collides with the English keyword.
        assertEquals(ParsedUnitQuery(5.0, "To", null), parse("5 To"))
        assertEquals(ParsedUnitQuery(5.0, "To", null), parse("5To"))
        assertEquals(ParsedUnitQuery(5.0, "To", "Go"), parse("5 To to Go"))
        assertEquals(ParsedUnitQuery(5.0, "To", "Go"), parse("5To-Go"))
        assertEquals(ParsedUnitQuery(5.0, "Go", "To"), parse("5 Go to To"))
        // ...and does not collide at all once French declares its own keyword.
        assertEquals(ParsedUnitQuery(5.0, "To", "Go"), parse("5 To en Go", fr))
    }

    @Test
    fun parse_longerSeparatorWinsOverAPrefixOfIt() {
        val separators = UnitConverterQueryParser.separators("in|into")
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm into mm", separators))
    }

    @Test
    fun parse_separatorMatchingIsCaseInsensitive() {
        assertEquals(ParsedUnitQuery(10.0, "cm", "in"), parse("10 cm TO in"))
        assertEquals(ParsedUnitQuery(10.0, "cm", "mm"), parse("10 cm IN mm", de))
    }
}
