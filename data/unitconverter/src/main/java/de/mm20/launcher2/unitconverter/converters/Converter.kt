package de.mm20.launcher2.unitconverter.converters

import android.content.Context
import de.mm20.launcher2.search.data.UnitConverter
import de.mm20.launcher2.unitconverter.Dimension
import de.mm20.launcher2.unitconverter.MeasureUnit

interface Converter {
    val dimension: Dimension

    /**
     * @param lenient also accept shorthand spellings: different casing, a plain-digit
     * exponent ("cm2" for "cm²"), or a temperature without its degree sign ("C" for "°C").
     *
     * Callers must try every converter strictly before retrying leniently, so an exact
     * spelling beats another unit's shorthand - Spanish "m" (metre) vs "M" (nautical mile).
     * Use `List<Converter>.findConverter` rather than reimplementing that ordering.
     */
    suspend fun isValidUnit(symbol: String, lenient: Boolean = false): Boolean

    suspend fun convert(
        context: Context,
        fromUnit: String,
        value: Double,
        toUnit: String?
    ): UnitConverter

    suspend fun getSupportedUnits(): List<MeasureUnit>
}

