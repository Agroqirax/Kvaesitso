package de.mm20.launcher2.unitconverter

import de.mm20.launcher2.unitconverter.converters.Converter

/**
 * Picks the converter that handles [unit], and [outputUnit] too when one was given.
 *
 * Every converter is tried strictly before any is tried leniently, so an exact spelling beats
 * another unit's shorthand - Spanish "m" (metre) vs "M" (nautical mile). See
 * [Converter.isValidUnit].
 */
internal suspend fun List<Converter>.findConverter(unit: String, outputUnit: String?): Converter? {
    for (lenient in listOf(false, true)) {
        for (converter in this) {
            if (!converter.isValidUnit(unit, lenient)) continue
            if (outputUnit != null && !converter.isValidUnit(outputUnit, lenient)) continue
            return converter
        }
    }
    return null
}
