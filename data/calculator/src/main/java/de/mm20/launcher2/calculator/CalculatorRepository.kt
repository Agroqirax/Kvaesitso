package de.mm20.launcher2.calculator

import de.mm20.launcher2.preferences.search.CalculatorSearchSettings
import de.mm20.launcher2.search.data.Calculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.mariuszgromada.math.mxparser.Expression

interface CalculatorRepository {
    fun search(query: String): Flow<Calculator?>
}

class CalculatorRepositoryImpl(
    private val settings: CalculatorSearchSettings
) : CalculatorRepository, KoinComponent {


    override fun search(query: String): Flow<Calculator?> {
        return settings.enabled.map {
            if (it && query.isNotBlank()) {
                queryCalculator(query)
            } else {
                null
            }
        }
    }

    private suspend fun queryCalculator(query: String): Calculator? {
        return when {
            query.matches(Regex("0x[0-9a-fA-F]+")) -> {
                val solution = query.substring(2).toIntOrNull(16) ?: run {
                    return null
                }
                Calculator(term = query, solution = solution.toDouble())
            }

            query.matches(Regex("0b[01]+")) -> {
                val solution = query.substring(2).toIntOrNull(2) ?: run {
                    return null
                }
                Calculator(term = query, solution = solution.toDouble())
            }

            query.matches(Regex("0[0-7]+")) -> {
                val solution = query.substring(1).toIntOrNull(8) ?: run {
                    return null
                }
                Calculator(term = query, solution = solution.toDouble())
            }

            else -> {
                withContext(Dispatchers.Default) {
                    try {
                        val preprocessed = ExpressionPreprocessor.preprocess(query)
                        val exp = Expression(preprocessed)
                        if (exp.checkSyntax()) {
                            Calculator(term = preprocessed, solution = exp.calculate())
                        } else {
                            // Preprocess the swapped query rather than swapping the
                            // preprocessed string: rewrites emit commas of their own
                            // ("∛8" -> "root(3, 8)") that must survive the swap.
                            val swapped = ExpressionPreprocessor.preprocess(
                                query.replace(',', '.').replace(';', ',')
                            )
                            val exp2 = Expression(swapped)
                            if (exp2.checkSyntax()) {
                                // Display the swapped form, not "preprocessed": the latter is
                                // the string checkSyntax just rejected, so showing it would
                                // pair an unparseable term with a solution ("√0,5" would read
                                // "sqrt(0,5) = 0.707").
                                Calculator(term = swapped, solution = exp2.calculate())
                            } else null
                        }
                    } catch (e: ArithmeticException) {
                        null
                    }
                }
            }
        }
    }
}