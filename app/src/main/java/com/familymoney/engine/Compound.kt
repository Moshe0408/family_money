package com.familymoney.engine

import kotlin.math.pow

/** Section 24 + 25: compound-interest simulator and scenario ranges. */
object Compound {

    data class YearPoint(
        val year: Int,
        val contributed: Double,
        val value: Double
    ) {
        val profit: Double get() = value - contributed
    }

    data class Projection(
        val points: List<YearPoint>,
        val initial: Double,
        val monthly: Double,
        val annualRatePercent: Double,
        val years: Int
    ) {
        val finalValue: Double get() = points.lastOrNull()?.value ?: initial
        val totalContributed: Double get() = points.lastOrNull()?.contributed ?: initial
        val totalProfit: Double get() = finalValue - totalContributed
    }

    /**
     * Monthly compounding with a deposit at the end of each month, which is how
     * a standing order into a savings/investment account actually behaves.
     */
    fun project(
        initial: Double,
        monthlyContribution: Double,
        annualRatePercent: Double,
        years: Int
    ): Projection {
        val months = (years * 12).coerceAtLeast(0)
        val monthlyRate = annualRatePercent / 100.0 / 12.0

        var value = initial
        var contributed = initial
        val points = mutableListOf(YearPoint(0, contributed, value))

        for (m in 1..months) {
            value = value * (1 + monthlyRate) + monthlyContribution
            contributed += monthlyContribution
            if (m % 12 == 0) points += YearPoint(m / 12, contributed, value)
        }
        return Projection(points, initial, monthlyContribution, annualRatePercent, years)
    }

    /** Section 25: conservative / moderate / high side by side. */
    fun scenarios(
        initial: Double,
        monthlyContribution: Double,
        years: Int,
        rates: List<Double> = listOf(4.0, 6.0, 8.0)
    ): Map<Double, Projection> =
        rates.associateWith { project(initial, monthlyContribution, it, years) }

    /** Section 23: value of a child portfolio at the target age. */
    fun childProjection(
        currentAge: Int,
        targetAge: Int,
        existing: Double,
        monthly: Double,
        annualRatePercent: Double
    ): Projection = project(existing, monthly, annualRatePercent, (targetAge - currentAge).coerceAtLeast(0))

    /**
     * Monthly deposit needed to reach [target] in [months] at [annualRatePercent].
     * Falls back to plain division when the rate is zero.
     */
    fun requiredMonthly(
        target: Double,
        current: Double,
        months: Int,
        annualRatePercent: Double
    ): Double {
        if (months <= 0) return (target - current).coerceAtLeast(0.0)
        val r = annualRatePercent / 100.0 / 12.0
        if (r == 0.0) return ((target - current) / months).coerceAtLeast(0.0)
        val growth = (1 + r).pow(months)
        val futureOfCurrent = current * growth
        val gap = target - futureOfCurrent
        if (gap <= 0) return 0.0
        return gap * r / (growth - 1)
    }
}
