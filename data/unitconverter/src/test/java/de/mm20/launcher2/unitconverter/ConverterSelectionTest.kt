package de.mm20.launcher2.unitconverter

import android.content.Context
import de.mm20.launcher2.search.data.UnitConverter
import de.mm20.launcher2.unitconverter.converters.Converter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Selection only calls [Converter.isValidUnit], never convert(). */
class ConverterSelectionTest {

    /** Accepts [exact] always, and [shorthand] only in the lenient pass. */
    private class FakeConverter(
        val name: String,
        private val exact: String,
        private val shorthand: String? = null,
    ) : Converter {
        override val dimension = Dimension.Length

        override suspend fun isValidUnit(symbol: String, lenient: Boolean): Boolean {
            return symbol == exact || (lenient && symbol == shorthand)
        }

        override suspend fun convert(
            context: Context,
            fromUnit: String,
            value: Double,
            toUnit: String?
        ): UnitConverter = throw UnsupportedOperationException()

        override suspend fun getSupportedUnits(): List<MeasureUnit> = emptyList()
    }

    private fun select(
        converters: List<FakeConverter>,
        unit: String,
        outputUnit: String? = null,
    ): String? = runBlocking {
        (converters.findConverter(unit, outputUnit) as FakeConverter?)?.name
    }

    @Test
    fun exactMatchWinsOverAnotherConvertersShorthand_evenWhenItComesLater() {
        val converters = listOf(
            FakeConverter("early", exact = "x", shorthand = "m"),
            FakeConverter("late", exact = "m"),
        )
        // "early" would match "m" leniently and is first, but "late" spells it exactly.
        assertEquals("late", select(converters, "m"))
    }

    @Test
    fun shorthandIsUsedOnlyWhenNoConverterMatchesExactly() {
        val converters = listOf(
            FakeConverter("early", exact = "x", shorthand = "m"),
            FakeConverter("late", exact = "km"),
        )
        assertEquals("early", select(converters, "m"))
    }

    @Test
    fun listOrderBreaksTiesWithinASinglePass() {
        val converters = listOf(
            FakeConverter("first", exact = "m"),
            FakeConverter("second", exact = "m"),
        )
        assertEquals("first", select(converters, "m"))
    }

    @Test
    fun bothUnitsMustResolveInTheSameConverter() {
        val converters = listOf(
            FakeConverter("length", exact = "m"),
            FakeConverter("mass", exact = "kg"),
        )
        assertNull(select(converters, "m", "kg"))
        assertEquals("length", select(converters, "m", "m"))
    }

    @Test
    fun outputUnitIsHeldToTheSamePassAsTheInputUnit() {
        val converters = listOf(
            FakeConverter("lenientBoth", exact = "x", shorthand = "m"),
            FakeConverter("strictInputOnly", exact = "m", shorthand = "ft"),
        )
        // "strictInputOnly" matches "m" exactly but needs the lenient pass for "ft", so the
        // strict pass rejects it rather than mixing the two modes.
        assertEquals("strictInputOnly", select(converters, "m", "ft"))
    }

    @Test
    fun unknownUnitMatchesNothing() {
        val converters = listOf(FakeConverter("length", exact = "m", shorthand = "M"))
        assertNull(select(converters, "parsec"))
    }

    @Test
    fun emptyConverterListMatchesNothing() {
        assertNull(select(emptyList(), "m"))
    }
}
