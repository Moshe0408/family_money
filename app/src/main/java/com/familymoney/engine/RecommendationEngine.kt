package com.familymoney.engine

import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.RecCategory
import com.familymoney.data.model.RecPriority
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure rule engine (spec sections 47-49). No Android or Room types on purpose —
 * this is the piece the unit tests exercise directly.
 */
object RecommendationEngine {

    /** A category has to be this much above its own average before we shout. */
    private const val OVERSPEND_FACTOR = 1.25

    /** Below this we are not going to bother the user with a saving nudge. */
    private const val MIN_SAVING_SUGGESTION = 200.0

    fun analyze(s: FinanceSnapshot): List<Recommendation> {
        val out = mutableListOf<Recommendation?>()

        out += savingOpportunity(s)
        out += cashFlowWarning(s)
        out += overspendAlerts(s)
        out += budgetAlerts(s)
        out += goalGaps(s)
        out += subscriptionNudge(s)
        out += investmentProjection(s)
        out += emergencyFundNudge(s)

        return out.filterNotNull()
            .sortedWith(compareByDescending<Recommendation> { it.priority.weight }
                .thenByDescending { it.amount ?: 0.0 })
    }

    // --- Section 6/7: how much is genuinely free to move to savings ---------

    private fun savingOpportunity(s: FinanceSnapshot): Recommendation? {
        val free = s.availableToSave
        if (free < MIN_SAVING_SUGGESTION) return null

        // Keep part of the surplus as a cushion, move the rest. Anything under
        // three months of buffer coverage stays conservative.
        val cushionShare = if (s.liquidBalance < s.safetyBuffer * 2) 0.6 else 0.35
        val keep = (free * cushionShare).roundToNearest(50)
        val move = (free - keep).roundToNearest(50)
        if (move < MIN_SAVING_SUGGESTION) return null

        return Recommendation(
            id = "saving_now",
            category = RecCategory.SAVING,
            priority = RecPriority.HIGH,
            title = "אפשר לחסוך ${money(move)}",
            reason = "נשארו כ־${money(free)} פנויים אחרי ההוצאות הצפויות " +
                "(${money(s.projectedRemainingSpend)}), החיובים הקרובים " +
                "(${money(s.upcomingCharges)}) וכרית הביטחון (${money(s.safetyBuffer)}).",
            suggestedAction = "transfer_to_savings",
            actionLabel = "העבר לחיסכון",
            amount = move,
            confidence = if (s.daysLeftInMonth < 20) 0.85 else 0.7
        )
    }

    // --- Section 6: cash-flow timing ---------------------------------------

    private fun cashFlowWarning(s: FinanceSnapshot): Recommendation? {
        val committed = s.upcomingCharges + s.projectedRemainingSpend
        if (committed <= s.liquidBalance + s.expectedIncomeRestOfMonth) return null

        val shortfall = committed - s.liquidBalance - s.expectedIncomeRestOfMonth
        return Recommendation(
            id = "cashflow_risk",
            category = RecCategory.CASHFLOW,
            priority = RecPriority.HIGH,
            title = "צפוי מחסור של ${money(shortfall)}",
            reason = "לפי הקצב הנוכחי, ההוצאות והחיובים עד סוף החודש גבוהים " +
                "מהיתרה וההכנסות הצפויות. מומלץ לא להעביר כסף לחיסכון כרגע.",
            suggestedAction = "reduce_spending",
            actionLabel = "בדוק הוצאות",
            amount = shortfall,
            confidence = 0.75
        )
    }

    // --- Section 19: anomaly detection -------------------------------------

    private fun overspendAlerts(s: FinanceSnapshot): List<Recommendation> =
        s.categorySpendThisMonth.mapNotNull { (key, spent) ->
            val avg = s.categoryAverage[key] ?: return@mapNotNull null
            if (avg <= 0.0 || spent <= avg * OVERSPEND_FACTOR) return@mapNotNull null

            val cat = ExpenseCategory.fromKey(key)
            val risePercent = ((spent - avg) / avg * 100.0).roundToLong()
            Recommendation(
                id = "overspend_$key",
                category = RecCategory.ALERT,
                priority = if (risePercent >= 50) RecPriority.HIGH else RecPriority.MEDIUM,
                title = "${cat.emoji} ${cat.he} גבוה ב־$risePercent%",
                reason = "החודש ${money(spent)} מול ממוצע של ${money(avg)}.",
                suggestedAction = "review_category:$key",
                actionLabel = "בדוק למה",
                amount = spent - avg,
                confidence = 0.9
            )
        }

    // --- Section 18: budget tracking ---------------------------------------

    private fun budgetAlerts(s: FinanceSnapshot): List<Recommendation> =
        s.budgets.mapNotNull { (key, limit) ->
            if (limit <= 0.0) return@mapNotNull null
            val spent = s.categorySpendThisMonth[key] ?: 0.0
            val used = spent / limit
            if (used < 0.9) return@mapNotNull null

            val cat = ExpenseCategory.fromKey(key)
            val over = spent - limit
            Recommendation(
                id = "budget_$key",
                category = RecCategory.BUDGET,
                priority = if (used >= 1.0) RecPriority.HIGH else RecPriority.MEDIUM,
                title = if (used >= 1.0)
                    "חריגה מתקציב ${cat.he}: ${money(over)}"
                else
                    "${cat.he} כמעט בתקציב (${(used * 100).roundToLong()}%)",
                reason = "${money(spent)} מתוך ${money(limit)}.",
                suggestedAction = "open_budget:$key",
                actionLabel = "פתח תקציב",
                amount = if (used >= 1.0) over else null,
                confidence = 0.95
            )
        }

    // --- Section 20: goals on/off track ------------------------------------

    private fun goalGaps(s: FinanceSnapshot): List<Recommendation> =
        s.goals.mapNotNull { g ->
            if (g.remaining <= 0.0) return@mapNotNull null
            val required = g.requiredMonthly
            if (required <= 0.0) return@mapNotNull null

            val behind = required > g.let { it.target / (it.monthsRemaining.coerceAtLeast(1) + 1) }
            Recommendation(
                id = "goal_${g.id}",
                category = RecCategory.GOAL,
                priority = if (g.monthsRemaining <= 3 && behind) RecPriority.HIGH else RecPriority.MEDIUM,
                title = "${g.emoji} ${g.name}: ${money(required)} בחודש",
                reason = "חסרים ${money(g.remaining)} מתוך ${money(g.target)}. " +
                    if (g.monthsRemaining > 0) "נשארו ${g.monthsRemaining} חודשים ליעד."
                    else "תאריך היעד עבר.",
                suggestedAction = "contribute_goal:${g.id}",
                actionLabel = "הוסף הפקדה",
                amount = required,
                confidence = 0.9
            )
        }

    // --- Section 33: subscriptions -----------------------------------------

    private fun subscriptionNudge(s: FinanceSnapshot): Recommendation? {
        if (s.subscriptionCount < 3) return null
        return Recommendation(
            id = "subscriptions",
            category = RecCategory.SPENDING,
            priority = RecPriority.LOW,
            title = "מצאנו ${s.subscriptionCount} מנויים",
            reason = "עלות חודשית ${money(s.subscriptionMonthlyCost)}, " +
                "כלומר ${money(s.subscriptionMonthlyCost * 12)} בשנה.",
            suggestedAction = "open_recurring",
            actionLabel = "הצג מנויים",
            amount = s.subscriptionMonthlyCost,
            confidence = 0.8
        )
    }

    // --- Section 6: general investment maths (never advice) ----------------

    private fun investmentProjection(s: FinanceSnapshot): Recommendation? {
        val monthly = s.monthlyContributionsToInvestments
        if (monthly < 100.0) return null
        return Recommendation(
            id = "investment_projection",
            category = RecCategory.INVESTMENT,
            priority = RecPriority.LOW,
            title = "בקצב הזה: ${money(monthly * 12)} בשנה",
            reason = "אם תמשיכו להפקיד ${money(monthly)} בחודש, ההפקדות יסתכמו " +
                "בכ־${money(monthly * 12)} בשנה. זהו חישוב בלבד ולא הבטחת תשואה.",
            suggestedAction = "open_simulator",
            actionLabel = "פתח סימולטור",
            amount = monthly * 12,
            confidence = 1.0
        )
    }

    // --- Section 49: safety buffer coverage --------------------------------

    private fun emergencyFundNudge(s: FinanceSnapshot): Recommendation? {
        val liquidTotal = s.liquidBalance + s.savingsBalance
        val monthsCovered = if (s.monthExpense <= 0.0) 0.0 else liquidTotal / s.monthExpense
        if (monthsCovered >= 3.0 || s.monthExpense <= 0.0) return null

        val target = s.monthExpense * 3
        return Recommendation(
            id = "emergency_fund",
            category = RecCategory.SAVING,
            priority = RecPriority.MEDIUM,
            title = "קרן חירום מכסה ${format1(monthsCovered)} חודשים",
            reason = "הנזיל והחיסכון מכסים כרגע פחות מ־3 חודשי הוצאות. " +
                "יעד מקובל הוא ${money(target)}.",
            suggestedAction = "create_goal:emergency",
            actionLabel = "צור יעד חירום",
            amount = (target - liquidTotal).coerceAtLeast(0.0),
            confidence = 0.85
        )
    }

    // --- Section 28: insights feed -----------------------------------------

    fun insights(s: FinanceSnapshot): List<Insight> {
        val out = mutableListOf<Insight>()

        if (s.monthIncome > 0) {
            out += Insight(
                emoji = "🟢",
                category = RecCategory.SAVING,
                title = "שיעור חיסכון",
                body = "החודש חסכתם ${format1(s.savingRatePercent)}% מההכנסה " +
                    "(${money(s.monthSaving)} מתוך ${money(s.monthIncome)}).",
                trendPercent = s.savingRatePercent
            )
        }

        val biggest = s.categorySpendThisMonth.maxByOrNull { it.value }
        if (biggest != null && biggest.value > 0) {
            val cat = ExpenseCategory.fromKey(biggest.key)
            val share = if (s.monthExpense <= 0) 0.0 else biggest.value / s.monthExpense * 100
            out += Insight(
                emoji = cat.emoji,
                category = RecCategory.SPENDING,
                title = "הקטגוריה הגדולה ביותר",
                body = "${cat.he}: ${money(biggest.value)} — ${format1(share)}% מסך ההוצאות."
            )
        }

        val worstDelta = s.categorySpendThisMonth
            .mapNotNull { (k, v) ->
                val avg = s.categoryAverage[k] ?: return@mapNotNull null
                if (avg <= 0) null else Triple(k, v, (v - avg) / avg * 100)
            }
            .maxByOrNull { abs(it.third) }
        if (worstDelta != null && abs(worstDelta.third) >= 15) {
            val cat = ExpenseCategory.fromKey(worstDelta.first)
            val up = worstDelta.third > 0
            out += Insight(
                emoji = if (up) "🟡" else "🟢",
                category = RecCategory.SPENDING,
                title = if (up) "עלייה בהוצאות" else "ירידה בהוצאות",
                body = "${cat.he} ${if (up) "עלו" else "ירדו"} ב־" +
                    "${abs(worstDelta.third).roundToLong()}% מול הממוצע.",
                trendPercent = worstDelta.third
            )
        }

        val aheadGoal = s.goals.firstOrNull { it.progressPercent >= 50 && it.monthsRemaining > 6 }
        if (aheadGoal != null) {
            out += Insight(
                emoji = "🔵",
                category = RecCategory.GOAL,
                title = "יעד בקצב טוב",
                body = "${aheadGoal.name}: ${aheadGoal.progressPercent.roundToLong()}% " +
                    "עם ${aheadGoal.monthsRemaining} חודשים לפני היעד.",
                trendPercent = aheadGoal.progressPercent
            )
        }

        val financialAssets = s.investmentValue + s.childrenValue + s.savingsBalance
        if (s.childrenValue > 0 && financialAssets > 0) {
            out += Insight(
                emoji = "🟣",
                category = RecCategory.INVESTMENT,
                title = "השקעות ילדים",
                body = "מהוות ${format1(s.childrenValue / financialAssets * 100)}% " +
                    "מהנכסים הפיננסיים שלכם (${money(s.childrenValue)})."
            )
        }

        if (s.recurringMonthlyTotal > 0) {
            out += Insight(
                emoji = "🔁",
                category = RecCategory.SPENDING,
                title = "הוצאות קבועות",
                body = "${money(s.recurringMonthlyTotal)} בחודש יוצאים אוטומטית — " +
                    "זה ${if (s.monthIncome > 0) format1(s.recurringMonthlyTotal / s.monthIncome * 100) + "% " else ""}" +
                    "מההכנסה."
            )
        }

        return out
    }

    // --- Section 8: month-end forecast --------------------------------------

    fun forecast(s: FinanceSnapshot): CashFlowForecast {
        val lines = listOf(
            ForecastLine("יתרה היום", s.liquidBalance),
            ForecastLine("+ הכנסות צפויות", s.expectedIncomeRestOfMonth),
            ForecastLine("− חיובים צפויים", -s.upcomingCharges),
            ForecastLine("− הוצאות צפויות", -s.projectedRemainingSpend),
            ForecastLine("יתרה צפויה בסוף החודש", s.projectedEndOfMonthBalance, isTotal = true)
        )
        return CashFlowForecast(
            lines = lines,
            projectedBalance = s.projectedEndOfMonthBalance,
            safetyBuffer = s.safetyBuffer,
            availableToSave = s.availableToSave
        )
    }
}

internal fun Double.roundToNearest(step: Int): Double =
    if (step <= 0) this else (this / step).roundToLong() * step.toDouble()

internal fun money(v: Double): String = "₪" + formatGrouped(v)

internal fun formatGrouped(v: Double): String {
    val rounded = kotlin.math.round(v).toLong()
    val neg = rounded < 0
    val digits = abs(rounded).toString()
    val sb = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return (if (neg) "-" else "") + sb
}

internal fun format1(v: Double): String {
    val r = kotlin.math.round(v * 10) / 10.0
    return if (r == kotlin.math.floor(r)) r.toLong().toString() else r.toString()
}
