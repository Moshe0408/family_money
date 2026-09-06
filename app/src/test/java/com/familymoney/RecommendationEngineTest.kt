package com.familymoney

import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.RecCategory
import com.familymoney.data.model.RecPriority
import com.familymoney.engine.FinanceSnapshot
import com.familymoney.engine.GoalSnapshot
import com.familymoney.engine.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private fun snapshot(
        liquid: Double = 15_000.0,
        savings: Double = 18_000.0,
        investments: Double = 25_000.0,
        childrenValue: Double = 7_000.0,
        liabilities: Double = 0.0,
        monthIncome: Double = 18_000.0,
        monthExpense: Double = 12_000.0,
        expectedIncome: Double = 0.0,
        upcomingCharges: Double = 2_400.0,
        dailySpend: Double = 200.0,
        daysLeft: Int = 10,
        buffer: Double = 3_000.0,
        categorySpend: Map<String, Double> = emptyMap(),
        categoryAverage: Map<String, Double> = emptyMap(),
        budgets: Map<String, Double> = emptyMap(),
        goals: List<GoalSnapshot> = emptyList(),
        recurringTotal: Double = 0.0,
        subscriptionCount: Int = 0,
        subscriptionCost: Double = 0.0,
        investContributions: Double = 0.0
    ) = FinanceSnapshot(
        today = 1_757_000_000_000L,
        liquidBalance = liquid,
        savingsBalance = savings,
        investmentValue = investments,
        childrenValue = childrenValue,
        liabilities = liabilities,
        monthIncome = monthIncome,
        monthExpense = monthExpense,
        expectedIncomeRestOfMonth = expectedIncome,
        upcomingCharges = upcomingCharges,
        averageDailySpend = dailySpend,
        daysLeftInMonth = daysLeft,
        safetyBuffer = buffer,
        categorySpendThisMonth = categorySpend,
        categoryAverage = categoryAverage,
        budgets = budgets,
        goals = goals,
        recurringMonthlyTotal = recurringTotal,
        subscriptionCount = subscriptionCount,
        subscriptionMonthlyCost = subscriptionCost,
        monthlyContributionsToInvestments = investContributions
    )

    // ---------------------------------------------------------- core arithmetic

    @Test
    fun `net worth sums assets and subtracts liabilities`() {
        val s = snapshot(
            liquid = 15_230.0, savings = 18_000.0,
            investments = 25_800.0, childrenValue = 7_620.0, liabilities = 5_000.0
        )
        assertEquals(61_650.0, s.netWorth, 0.01)
    }

    @Test
    fun `projected end of month subtracts charges and run-rate spend`() {
        val s = snapshot(
            liquid = 9_800.0, expectedIncome = 4_000.0,
            upcomingCharges = 2_400.0, dailySpend = 200.0, daysLeft = 10
        )
        // 9800 + 4000 - 2400 - (200 * 10)
        assertEquals(9_400.0, s.projectedEndOfMonthBalance, 0.01)
    }

    @Test
    fun `available to save excludes the safety buffer`() {
        val s = snapshot(
            liquid = 9_800.0, expectedIncome = 4_000.0,
            upcomingCharges = 2_400.0, dailySpend = 200.0, daysLeft = 10, buffer = 3_000.0
        )
        assertEquals(6_400.0, s.availableToSave, 0.01)
    }

    @Test
    fun `available to save never goes negative`() {
        val s = snapshot(
            liquid = 1_000.0, expectedIncome = 0.0,
            upcomingCharges = 5_000.0, dailySpend = 100.0, daysLeft = 10, buffer = 3_000.0
        )
        assertEquals(0.0, s.availableToSave, 0.01)
    }

    @Test
    fun `saving rate is zero when there is no income`() {
        assertEquals(0.0, snapshot(monthIncome = 0.0).savingRatePercent, 0.01)
    }

    // ------------------------------------------------------------ saving advice

    @Test
    fun `suggests saving when surplus is comfortable`() {
        val s = snapshot(
            liquid = 20_000.0, expectedIncome = 4_000.0,
            upcomingCharges = 2_000.0, dailySpend = 150.0, daysLeft = 10, buffer = 3_000.0
        )
        val rec = RecommendationEngine.analyze(s).firstOrNull { it.id == "saving_now" }
        assertNotNull("expected a saving recommendation", rec)
        assertEquals(RecCategory.SAVING, rec!!.category)
        assertTrue("amount should be positive", (rec.amount ?: 0.0) > 0)
        assertTrue("should not exceed the free amount", rec.amount!! <= s.availableToSave)
    }

    @Test
    fun `does not suggest saving when there is no surplus`() {
        val s = snapshot(
            liquid = 3_100.0, expectedIncome = 0.0,
            upcomingCharges = 0.0, dailySpend = 0.0, daysLeft = 5, buffer = 3_000.0
        )
        assertNull(RecommendationEngine.analyze(s).firstOrNull { it.id == "saving_now" })
    }

    @Test
    fun `keeps a bigger cushion when liquidity is thin`() {
        val thin = snapshot(
            liquid = 5_000.0, expectedIncome = 6_000.0, upcomingCharges = 0.0,
            dailySpend = 0.0, daysLeft = 5, buffer = 3_000.0
        )
        val flush = snapshot(
            liquid = 30_000.0, expectedIncome = 6_000.0, upcomingCharges = 0.0,
            dailySpend = 0.0, daysLeft = 5, buffer = 3_000.0
        )
        val thinRec = RecommendationEngine.analyze(thin).first { it.id == "saving_now" }
        val flushRec = RecommendationEngine.analyze(flush).first { it.id == "saving_now" }

        val thinShare = thinRec.amount!! / thin.availableToSave
        val flushShare = flushRec.amount!! / flush.availableToSave
        assertTrue(
            "thin liquidity should move a smaller share ($thinShare vs $flushShare)",
            thinShare < flushShare
        )
    }

    // ---------------------------------------------------------- cash-flow risk

    @Test
    fun `warns when commitments exceed available money`() {
        val s = snapshot(
            liquid = 2_000.0, expectedIncome = 1_000.0,
            upcomingCharges = 4_000.0, dailySpend = 100.0, daysLeft = 10
        )
        val rec = RecommendationEngine.analyze(s).firstOrNull { it.id == "cashflow_risk" }
        assertNotNull(rec)
        assertEquals(RecPriority.HIGH, rec!!.priority)
        // 4000 + 1000 - 2000 - 1000
        assertEquals(2_000.0, rec.amount!!, 0.01)
    }

    @Test
    fun `no cash-flow warning when money covers commitments`() {
        val s = snapshot(
            liquid = 20_000.0, expectedIncome = 4_000.0,
            upcomingCharges = 2_000.0, dailySpend = 100.0, daysLeft = 10
        )
        assertNull(RecommendationEngine.analyze(s).firstOrNull { it.id == "cashflow_risk" })
    }

    // ------------------------------------------------------- anomaly detection

    @Test
    fun `flags a category well above its own average`() {
        val s = snapshot(
            categorySpend = mapOf(ExpenseCategory.RESTAURANTS.name to 1_600.0),
            categoryAverage = mapOf(ExpenseCategory.RESTAURANTS.name to 1_000.0)
        )
        val rec = RecommendationEngine.analyze(s)
            .firstOrNull { it.id == "overspend_${ExpenseCategory.RESTAURANTS.name}" }
        assertNotNull(rec)
        assertEquals(RecPriority.HIGH, rec!!.priority) // +60% clears the 50% bar
        assertEquals(600.0, rec.amount!!, 0.01)
    }

    @Test
    fun `does not flag a category just above average`() {
        val s = snapshot(
            categorySpend = mapOf(ExpenseCategory.GROCERY.name to 1_100.0),
            categoryAverage = mapOf(ExpenseCategory.GROCERY.name to 1_000.0)
        )
        assertNull(
            RecommendationEngine.analyze(s)
                .firstOrNull { it.id == "overspend_${ExpenseCategory.GROCERY.name}" }
        )
    }

    @Test
    fun `ignores categories with no history`() {
        val s = snapshot(
            categorySpend = mapOf(ExpenseCategory.CAR.name to 5_000.0),
            categoryAverage = emptyMap()
        )
        assertTrue(RecommendationEngine.analyze(s).none { it.id.startsWith("overspend_") })
    }

    // ------------------------------------------------------------------ budgets

    @Test
    fun `reports a budget overrun with the exact amount`() {
        val s = snapshot(
            categorySpend = mapOf(ExpenseCategory.GROCERY.name to 2_800.0),
            budgets = mapOf(ExpenseCategory.GROCERY.name to 2_500.0)
        )
        val rec = RecommendationEngine.analyze(s)
            .first { it.id == "budget_${ExpenseCategory.GROCERY.name}" }
        assertEquals(RecPriority.HIGH, rec.priority)
        assertEquals(300.0, rec.amount!!, 0.01)
    }

    @Test
    fun `warns before a budget is fully used`() {
        val s = snapshot(
            categorySpend = mapOf(ExpenseCategory.KIDS.name to 950.0),
            budgets = mapOf(ExpenseCategory.KIDS.name to 1_000.0)
        )
        val rec = RecommendationEngine.analyze(s)
            .first { it.id == "budget_${ExpenseCategory.KIDS.name}" }
        assertEquals(RecPriority.MEDIUM, rec.priority)
        assertNull("no overrun amount yet", rec.amount)
    }

    @Test
    fun `stays quiet on a budget that is comfortably under`() {
        val s = snapshot(
            categorySpend = mapOf(ExpenseCategory.CAR.name to 500.0),
            budgets = mapOf(ExpenseCategory.CAR.name to 1_500.0)
        )
        assertTrue(RecommendationEngine.analyze(s).none { it.id.startsWith("budget_") })
    }

    // -------------------------------------------------------------------- goals

    @Test
    fun `required monthly splits the gap over the remaining months`() {
        val goal = GoalSnapshot(
            "g1", "חופשה", "✈️", target = 15_000.0, current = 8_400.0,
            targetDate = 0L, monthsRemaining = 10
        )
        assertEquals(660.0, goal.requiredMonthly, 0.01)
        assertEquals(56.0, goal.progressPercent, 0.01)
    }

    @Test
    fun `overdue goal asks for the whole remaining amount`() {
        val goal = GoalSnapshot(
            "g2", "רכב", "🚗", target = 10_000.0, current = 4_000.0,
            targetDate = 0L, monthsRemaining = 0
        )
        assertEquals(6_000.0, goal.requiredMonthly, 0.01)
    }

    @Test
    fun `completed goal produces no recommendation`() {
        val s = snapshot(
            goals = listOf(
                GoalSnapshot("g3", "יעד", "🎯", 5_000.0, 5_000.0, 0L, 6)
            )
        )
        assertTrue(RecommendationEngine.analyze(s).none { it.id == "goal_g3" })
    }

    @Test
    fun `progress percent is clamped at one hundred`() {
        val goal = GoalSnapshot("g4", "יעד", "🎯", 1_000.0, 5_000.0, 0L, 3)
        assertEquals(100.0, goal.progressPercent, 0.01)
    }

    // ------------------------------------------------------ emergency fund rule

    @Test
    fun `nudges when liquid savings cover under three months`() {
        val s = snapshot(liquid = 5_000.0, savings = 5_000.0, monthExpense = 12_000.0)
        val rec = RecommendationEngine.analyze(s).firstOrNull { it.id == "emergency_fund" }
        assertNotNull(rec)
        assertEquals(26_000.0, rec!!.amount!!, 0.01) // 12000*3 - 10000
    }

    @Test
    fun `no nudge once three months are covered`() {
        val s = snapshot(liquid = 20_000.0, savings = 20_000.0, monthExpense = 12_000.0)
        assertNull(RecommendationEngine.analyze(s).firstOrNull { it.id == "emergency_fund" })
    }

    // ------------------------------------------------------------ subscriptions

    @Test
    fun `surfaces subscriptions once there are at least three`() {
        val s = snapshot(subscriptionCount = 8, subscriptionCost = 286.0)
        val rec = RecommendationEngine.analyze(s).firstOrNull { it.id == "subscriptions" }
        assertNotNull(rec)
        assertTrue(rec!!.reason.contains("3,432")) // 286 * 12
    }

    @Test
    fun `two subscriptions are not worth mentioning`() {
        val s = snapshot(subscriptionCount = 2, subscriptionCost = 70.0)
        assertNull(RecommendationEngine.analyze(s).firstOrNull { it.id == "subscriptions" })
    }

    // ----------------------------------------------------------------- ordering

    @Test
    fun `high priority items come first`() {
        val s = snapshot(
            liquid = 2_000.0, expectedIncome = 0.0, upcomingCharges = 6_000.0,
            dailySpend = 100.0, daysLeft = 10,
            subscriptionCount = 5, subscriptionCost = 200.0
        )
        val recs = RecommendationEngine.analyze(s)
        assertTrue(recs.size >= 2)
        val weights = recs.map { it.priority.weight }
        assertEquals("list must be sorted by priority", weights.sortedDescending(), weights)
    }

    // ---------------------------------------------------------------- forecast

    @Test
    fun `forecast ends with the projected balance as the total row`() {
        val s = snapshot(
            liquid = 9_800.0, expectedIncome = 4_000.0,
            upcomingCharges = 2_400.0, dailySpend = 200.0, daysLeft = 10
        )
        val f = RecommendationEngine.forecast(s)
        assertEquals(5, f.lines.size)
        assertTrue(f.lines.last().isTotal)
        assertEquals(9_400.0, f.lines.last().amount, 0.01)
        assertEquals(9_400.0, f.projectedBalance, 0.01)
    }

    // ---------------------------------------------------------------- insights

    @Test
    fun `insights report the saving rate`() {
        val s = snapshot(monthIncome = 18_000.0, monthExpense = 12_000.0)
        val insight = RecommendationEngine.insights(s).firstOrNull { it.title == "שיעור חיסכון" }
        assertNotNull(insight)
        assertEquals(33.3, insight!!.trendPercent!!, 0.1)
    }

    @Test
    fun `insights name the largest category`() {
        val s = snapshot(
            monthExpense = 5_000.0,
            categorySpend = mapOf(
                ExpenseCategory.GROCERY.name to 2_000.0,
                ExpenseCategory.CAR.name to 3_000.0
            )
        )
        val insight = RecommendationEngine.insights(s)
            .firstOrNull { it.title == "הקטגוריה הגדולה ביותר" }
        assertNotNull(insight)
        assertTrue(insight!!.body.contains(ExpenseCategory.CAR.he))
    }

    @Test
    fun `empty snapshot produces no crash and no noise`() {
        val s = snapshot(
            liquid = 0.0, savings = 0.0, investments = 0.0, childrenValue = 0.0,
            monthIncome = 0.0, monthExpense = 0.0, upcomingCharges = 0.0,
            dailySpend = 0.0, daysLeft = 0
        )
        assertTrue(RecommendationEngine.analyze(s).isEmpty())
        assertTrue(RecommendationEngine.insights(s).isEmpty())
    }
}
