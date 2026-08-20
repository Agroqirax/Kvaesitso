package de.mm20.launcher2.unitconverter.converters

import android.content.Context
import de.mm20.launcher2.search.data.UnitConverter
import de.mm20.launcher2.unitconverter.*

internal class TemperatureConverter(context: Context) : Converter {
    override val dimension = Dimension.Temperature

    val units = listOf(
        TemperatureMeasureUnit(
            context.getString(R.string.unit_degree_celsius_symbol),
            R.plurals.unit_degree_celsius,
            TemperatureUnit.DegreeCelsius,
        ),
        TemperatureMeasureUnit(
            context.getString(R.string.unit_degree_fahrenheit_symbol),
            R.plurals.unit_degree_fahrenheit,
            TemperatureUnit.DegreeFahrenheit,
        ),
        TemperatureMeasureUnit(
            context.getString(R.string.unit_kelvin_symbol),
            R.plurals.unit_kelvin,
            TemperatureUnit.Kelvin,
        ),
    )

    override suspend fun isValidUnit(symbol: String, lenient: Boolean): Boolean {
        return units.any { it.matches(symbol, lenient) }
    }

    /** Strict before lenient, matching how the repository picks a converter. */
    private fun resolve(symbol: String): TemperatureMeasureUnit {
        return units.firstOrNull { it.matches(symbol, lenient = false) }
            ?: units.first { it.matches(symbol, lenient = true) }
    }

    override suspend fun convert(
        context: Context,
        fromUnit: String,
        value: Double,
        toUnit: String?
    ): UnitConverter {
        val from = resolve(fromUnit)
        val to = toUnit?.let { resolve(it) }

        val values = mutableListOf<UnitValue>()

        if (to != null) {
            val toValue = convertTemperature(value, from.unit, to.unit)

            values += UnitValue(
                value = toValue,
                symbol = to.symbol,
                formattedName = ConverterUtils.formatName(context, to, toValue),
                formattedValue = ConverterUtils.formatValue(context, to, toValue),
            )
        } else {
            for (to in units) {
                if (to.symbol == from.symbol) continue
                val v = convertTemperature(value, from.unit, to.unit)
                values += UnitValue(
                    v,
                    to.symbol,
                    ConverterUtils.formatName(context, to, v),
                    ConverterUtils.formatValue(context, to, v)
                )
            }
        }
        return UnitConverter(
            dimension = Dimension.Temperature,
            inputValue = UnitValue(
                value = value,
                // The resolved symbol, not the raw input: lenient matching accepts "c" for "°C".
                symbol = from.symbol,
                formattedName = ConverterUtils.formatName(context, from, value),
                formattedValue = ConverterUtils.formatValue(context, from, value),
            ),
            values = values
        )
    }

    override suspend fun getSupportedUnits(): List<MeasureUnit> {
        return units
    }
}

/** Top level so the arithmetic is reachable without a [Context]. */
internal fun convertTemperature(
    value: Double,
    from: TemperatureUnit,
    to: TemperatureUnit
): Double {
    if (from == to) return value
    if (from == TemperatureUnit.Kelvin && to == TemperatureUnit.DegreeCelsius) {
        return value - 273.15
    }
    if (from == TemperatureUnit.DegreeCelsius && to == TemperatureUnit.Kelvin) {
        return value + 273.15
    }
    if (from === TemperatureUnit.DegreeCelsius && to == TemperatureUnit.DegreeFahrenheit) {
        return value * (9.0 / 5.0) + 32.0
    }
    if (from === TemperatureUnit.DegreeFahrenheit && to == TemperatureUnit.DegreeCelsius) {
        return (value - 32.0) * (5.0 / 9.0)
    }

    if (from === TemperatureUnit.Kelvin && to == TemperatureUnit.DegreeFahrenheit) {
        return (value - 273.15) * (9.0 / 5.0) + 32.0
    }
    if (from === TemperatureUnit.DegreeFahrenheit && to == TemperatureUnit.Kelvin) {
        return (value - 32.0) * (5.0 / 9.0) + 273.15
    }
    throw IllegalArgumentException()
}

data class TemperatureMeasureUnit(
    override val symbol: String,
    val nameResource: Int,
    val unit: TemperatureUnit,
) : MeasureUnit {
    override fun formatName(context: Context, value: Double): String {
        return context.resources.getQuantityString(nameResource, value.toInt())
    }

    /**
     * When [lenient], casing is ignored and the degree sign may be left off ("C" for "°C").
     * Lenient-only because Arabic spells celsius "°س" and an hour "س".
     */
    internal fun matches(input: String, lenient: Boolean = false): Boolean {
        if (symbol == input) return true
        if (!lenient) return false
        return symbol.equals(input, ignoreCase = true) ||
                symbol.removePrefix("°").equals(input, ignoreCase = true)
    }
}

enum class TemperatureUnit {
    DegreeCelsius,
    DegreeFahrenheit,
    Kelvin,
}