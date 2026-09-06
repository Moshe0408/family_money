package com.familymoney.engine

import com.familymoney.data.model.RecCategory
import com.familymoney.data.model.RecPriority

/**
 * Everything the recommendation engine needs, decoupled from Room and the UI so
 * it can be unit-tested in isolation (spec section 47).
 */
data class FinanceSnapshot(
    val today: Long,
    val liquidBalance: Double,
    val savingsBalance: Double,
    val investmentValue: Double,
    val childrenValue: Double,
    val liabilities: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val expectedIncomeRestOfMonth: Double,
    val upcomingCharges: Double,
    val averageDailySpend: Double,
    val daysLeftInMonth: Int,
    val safetyBuffer: Double,
    val categorySpendThisMonth: Map<String, Double>,
    val categoryAverage: Map<String, Double>,
    val budgets: Map<String, Double>,
    val goals: List<GoalSnapshot>,
    val recurringMonthlyTotal: Double,
    val subscriptionCount: Int,
    val subscriptionMonthlyCost: Double,
    val monthlyContributionsToInvestments: Double
) {
    val netWorth: Double
        get() = liquidBalance + savingsBalance + investmentValue + childrenValue - liabilities

    val monthSaving: Double get() = monthIncome - monthExpense

    val savingRatePercent: Double
        get() = if (monthIncome <= 0.0) 0.0 else (monthSaving / monthIncome) * 100.0

    /** Spend we still expect before the month closes, from the daily run-rate. */
    val projectedRemainingSpend: Double get() = averageDailySpend * daysLeftInMonth

    /** Section 8: "how will the month end?" */
    val projectedEndOfMonthBalance: Double
        get() = liquidBalance + expectedIncomeRestOfMonth - upcomingCharges - projectedRemainingSpend

    /**
     * Section 7 + 49: only money above the safety buffer *and* above everything
     * already committed counts as free to move into savings.
     */
    val availableToSave: Double
        get() = (projectedEndOfMonthBalance - safetyBuffer).coerceAtLeast(0.0)
}

data class GoalSnapshot(
    val id: String,
    val name: String,
    val emoji: String,
    val target: Double,
    val current: Double,
    val targetDate: Long,
    val monthsRemaining: Int
) {
    val progressPercent: Double
        get() = if (target <= 0.0) 0.0 else (current / target * 100.0).coerceIn(0.0, 100.0)

    val remaining: Double get() = (target - current).coerceAtLeast(0.0)

    /** What we must put aside each month from here to land on time. */
    val requiredMonthly: Double
        get() = if (monthsRemaining <= 0) remaining else remaining / monthsRemaining
}

data class Recommendation(
    val id: String,
    val category: RecCategory,
    val priority: RecPriority,
    val title: String,
    val reason: String,
    val suggestedAction: String,
    val actionLabel: String,
    val amount: Double? = null,
    val confidence: Double = 0.8,
    val createdAt: Long = System.currentTimeMillis()
)

data class Insight(
    val emoji: String,
    val category: RecCategory,
    val title: String,
    val body: String,
    val trendPercent: Double? = null
)

/** One row of the "how does the month end?" table (spec section 8). */
data class ForecastLine(val label: String, val amount: Double, val isTotal: Boolean = false)

data class CashFlowForecast(
    val lines: List<ForecastLine>,
    val projectedBalance: Double,
    val safetyBuffer: Double,
    val availableToSave: Double
)
