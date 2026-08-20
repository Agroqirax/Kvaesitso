package de.mm20.launcher2.unitconverter.converters

import android.content.Context
import de.mm20.launcher2.search.data.UnitConverter
import de.mm20.launcher2.unitconverter.ConverterUtils
import de.mm20.launcher2.unitconverter.MeasureUnit
import de.mm20.launcher2.unitconverter.UnitValue
import kotlin.math.roundToInt

/**
 * A converter for units that can converted into each other by simple multiplication with a constant factor
 */
internal abstract class SimpleFactorConverter: Converter {
    open val standardUnits: List<MeasureUnitWithFactor> = emptyList()

    /**
     * Returns true if a symbol is a valid unit of this converter
     */
    override suspend fun isValidUnit(symbol: String, lenient: Boolean): Boolean {
        return standardUnits.any { it.matches(symbol, lenient) }
    }

    /** Strict before lenient, matching how the repository picks a converter. */
    private fun resolve(symbol: String): MeasureUnitWithFactor {
        return standardUnits.firstOrNull { it.matches(symbol, lenient = false) }
            ?: standardUnits.first { it.matches(symbol, lenient = true) }
    }

    override suspend fun convert(context: Context, fromUnit: String, value: Double, toUnit: String?): UnitConverter {
        val results = mutableListOf<UnitValue>()
        val unit = resolve(fromUnit)
        if (toUnit == null) {
            for (targetUnit in standardUnits) {
                if (targetUnit.symbol == unit.symbol) continue
                val v = value * targetUnit.factor / unit.factor
                results += UnitValue(v, targetUnit.symbol, ConverterUtils.formatName(context, targetUnit, v), ConverterUtils.formatValue(context, unit, v))
            }
        } else {
            val targetUnit = resolve(toUnit)
            val v = value * targetUnit.factor / unit.factor
            results += UnitValue(v, targetUnit.symbol, ConverterUtils.formatName(context, targetUnit, v), ConverterUtils.formatValue(context, unit, v))
        }
        // The resolved symbol, not the raw input: lenient matching means "mb" resolves to "MB".
        val inputValue = UnitValue(value, unit.symbol, ConverterUtils.formatName(context, unit, value), ConverterUtils.formatValue(context, unit, value))
        return UnitConverter(dimension, inputValue, results)
    }

    override suspend fun getSupportedUnits(): List<MeasureUnit> {
        return standardUnits
    }
}


data class MeasureUnitWithFactor(
    val factor: Double,
    override val symbol: String,
    val nameResource: Int
): MeasureUnit {
    override fun formatName(context: Context, value: Double): String {
        return context.resources.getQuantityString(nameResource, value.roundToInt())
    }

    /**
     * See [Converter.isValidUnit] for why the two modes must not be mixed. Plain-digit
     * exponents ("cm2") are accepted in both, since no locale spells one unit "cm2" and
     * another "cm²".
     */
    internal fun matches(input: String, lenient: Boolean = false): Boolean {
        fun matchesExactly(candidate: String) =
            if (lenient) candidate.equals(input, ignoreCase = true) else candidate == input

        if (matchesExactly(symbol)) return true
        if (symbol.none { it == '²' || it == '³' }) return false
        return matchesExactly(symbol.replace('²', '2').replace('³', '3'))
    }
}