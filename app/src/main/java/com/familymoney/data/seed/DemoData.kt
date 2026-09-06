package com.familymoney.data.seed

import com.familymoney.data.db.AccountEntity
import com.familymoney.data.db.AppDatabase
import com.familymoney.data.db.BudgetEntity
import com.familymoney.data.db.CardEntity
import com.familymoney.data.db.ChildEntity
import com.familymoney.data.db.GoalEntity
import com.familymoney.data.db.InvestmentEntity
import com.familymoney.data.db.NetWorthSnapshotEntity
import com.familymoney.data.db.RecurringEntity
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.AccountType
import com.familymoney.data.model.CardType
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.IncomeCategory
import com.familymoney.data.model.InvestmentType
import com.familymoney.data.model.TxSource
import com.familymoney.data.model.TxType
import com.familymoney.util.Dates
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

/**
 * A realistic Israeli household so every screen, chart and recommendation has
 * something to show before the user has entered anything.
 */
object DemoData {

    private val merchantsByCategory = mapOf(
        ExpenseCategory.GROCERY to listOf("שופרסל", "רמי לוי", "יוחננוף", "ויקטורי", "אושר עד"),
        ExpenseCategory.RESTAURANTS to listOf("Wolt", "קפה גרג", "בורגרים", "תן ביס", "פיצה האט"),
        ExpenseCategory.FUEL to listOf("פז", "סונול", "דור אלון", "דלק"),
        ExpenseCategory.CAR to listOf("פנגו", "מוסך מרכזי", "צמיגי גורדון", "כביש 6"),
        ExpenseCategory.SHOPPING to listOf("Zara", "Fox", "IKEA", "Terminal X", "AliExpress"),
        ExpenseCategory.HEALTH to listOf("סופר פארם", "מכבי", "בית מרקחת"),
        ExpenseCategory.LEISURE to listOf("יס פלאנט", "פארק המים", "הופעה", "בריכה"),
        ExpenseCategory.KIDS to listOf("חוג כדורגל", "צהרון", "צעצועים"),
        ExpenseCategory.OTHER to listOf("דואר ישראל", "משרד הפנים", "שונות")
    )

    suspend fun populate(db: AppDatabase, userName: String) {
        val familyId = "demo-family"
        val partner = "בת הזוג"

        // --- accounts -------------------------------------------------------
        val checking = AccountEntity(
            id = "acc_checking", familyId = familyId, ownerName = userName,
            provider = "בנק הפועלים", type = AccountType.CHECKING,
            name = "עו\"ש משותף", balance = 15_230.0
        )
        val savings = AccountEntity(
            id = "acc_savings", familyId = familyId, ownerName = userName,
            provider = "בנק הפועלים", type = AccountType.SAVINGS,
            name = "חיסכון", balance = 18_000.0, interestRatePercent = 4.2
        )
        val deposit = AccountEntity(
            id = "acc_deposit", familyId = familyId, ownerName = partner,
            provider = "לאומי", type = AccountType.DEPOSIT,
            name = "פיקדון שנתי", balance = 13_000.0, interestRatePercent = 4.8
        )
        db.accountDao().upsertAll(listOf(checking, savings, deposit))

        // --- cards ----------------------------------------------------------
        db.cardDao().upsertAll(
            listOf(
                CardEntity(
                    id = "card_visa", familyId = familyId, ownerName = userName,
                    provider = "ויזה כאל", last4 = "1234", type = CardType.CREDIT,
                    creditLimit = 15_000.0, usedCredit = 7_820.0,
                    nextChargeAmount = 3_420.0,
                    nextChargeDate = Dates.nextOccurrence(10),
                    linkedAccountId = checking.id
                ),
                CardEntity(
                    id = "card_max", familyId = familyId, ownerName = partner,
                    provider = "מקס", last4 = "8891", type = CardType.CREDIT,
                    creditLimit = 12_000.0, usedCredit = 4_110.0,
                    nextChargeAmount = 2_180.0,
                    nextChargeDate = Dates.nextOccurrence(2),
                    linkedAccountId = checking.id
                ),
                CardEntity(
                    id = "card_debit", familyId = familyId, ownerName = userName,
                    provider = "דביט הפועלים", last4 = "5567", type = CardType.DEBIT,
                    creditLimit = 0.0, usedCredit = 0.0, nextChargeAmount = 0.0,
                    linkedAccountId = checking.id
                )
            )
        )

        // --- 8 months of transactions ---------------------------------------
        val rnd = Random(20260906)
        val txs = mutableListOf<TransactionEntity>()
        val thisMonth = YearMonth.from(Dates.today())

        for (back in 7 downTo 0) {
            val ym = thisMonth.minusMonths(back.toLong())
            val isCurrent = back == 0
            val daysInMonth = ym.lengthOfMonth()
            val maxDay = if (isCurrent) Dates.today().dayOfMonth else daysInMonth

            // Salaries land on the 10th; the partner's on the 1st.
            if (maxDay >= 10) {
                txs += income(familyId, userName, checking.id, 12_400.0 + rnd.nextInt(-200, 400),
                    "משכורת ${userName}", IncomeCategory.SALARY, ym, 10, recurring = true)
            }
            if (maxDay >= 1) {
                txs += income(familyId, partner, checking.id, 6_200.0 + rnd.nextInt(-150, 250),
                    "משכורת $partner", IncomeCategory.SALARY, ym, 1, recurring = true)
            }
            if (ym.monthValue % 3 == 0 && maxDay >= 20) {
                txs += income(familyId, userName, savings.id, 190.0 + rnd.nextInt(0, 60),
                    "ריבית על חיסכון", IncomeCategory.INTEREST, ym, 20)
            }

            // Standing orders.
            val fixed = listOf(
                Triple("שכר דירה", 5_200.0, ExpenseCategory.HOUSING) to 2,
                Triple("גן ילדים", 2_500.0, ExpenseCategory.KIDS) to 5,
                Triple("ביטוח רכב הראל", 380.0, ExpenseCategory.INSURANCE) to 8,
                Triple("חברת חשמל", 420.0, ExpenseCategory.BILLS) to 12,
                Triple("נטפליקס", 49.9, ExpenseCategory.SUBSCRIPTIONS) to 15,
                Triple("Spotify", 21.9, ExpenseCategory.SUBSCRIPTIONS) to 15,
                Triple("סלקום", 189.0, ExpenseCategory.BILLS) to 18,
                Triple("חדר כושר Holmes", 199.0, ExpenseCategory.LEISURE) to 20
            )
            fixed.forEach { (item, day) ->
                if (day <= maxDay) {
                    val (name, amount, cat) = item
                    txs += expense(
                        familyId, userName, checking.id, null,
                        amount, name, cat, ym, day, recurring = true
                    )
                }
            }

            // Variable spending. The current month deliberately overshoots on
            // restaurants and shopping so the anomaly rules have something to find.
            val variableCount = if (isCurrent) 22 else 34
            repeat(variableCount) {
                val day = rnd.nextInt(1, maxDay + 1)
                val category = merchantsByCategory.keys.random(rnd)
                val merchant = merchantsByCategory[category]!!.random(rnd)
                var amount = when (category) {
                    ExpenseCategory.GROCERY -> rnd.nextInt(120, 480).toDouble()
                    ExpenseCategory.RESTAURANTS -> rnd.nextInt(45, 260).toDouble()
                    ExpenseCategory.FUEL -> rnd.nextInt(180, 340).toDouble()
                    ExpenseCategory.SHOPPING -> rnd.nextInt(90, 620).toDouble()
                    ExpenseCategory.CAR -> rnd.nextInt(30, 900).toDouble()
                    ExpenseCategory.HEALTH -> rnd.nextInt(40, 300).toDouble()
                    ExpenseCategory.LEISURE -> rnd.nextInt(60, 400).toDouble()
                    ExpenseCategory.KIDS -> rnd.nextInt(50, 350).toDouble()
                    else -> rnd.nextInt(30, 200).toDouble()
                }
                if (isCurrent && (category == ExpenseCategory.RESTAURANTS ||
                        category == ExpenseCategory.SHOPPING)
                ) {
                    amount *= 1.5
                }
                val card = if (rnd.nextBoolean()) "card_visa" else "card_max"
                txs += expense(familyId, userName, null, card, amount, merchant, category, ym, day)
            }
        }
        db.transactionDao().upsertAll(txs)

        // --- standing orders as first-class rows ----------------------------
        db.recurringDao().upsertAll(
            listOf(
                recurringExpense(familyId, "שכר דירה", 5_200.0, ExpenseCategory.HOUSING, 2, checking.id),
                recurringExpense(familyId, "גן ילדים", 2_500.0, ExpenseCategory.KIDS, 5, checking.id),
                recurringExpense(familyId, "ביטוח רכב הראל", 380.0, ExpenseCategory.INSURANCE, 8, checking.id),
                recurringExpense(familyId, "חברת חשמל", 420.0, ExpenseCategory.BILLS, 12, checking.id),
                recurringExpense(familyId, "נטפליקס", 49.9, ExpenseCategory.SUBSCRIPTIONS, 15, checking.id, sub = true),
                recurringExpense(familyId, "Spotify", 21.9, ExpenseCategory.SUBSCRIPTIONS, 15, checking.id, sub = true),
                recurringExpense(familyId, "סלקום", 189.0, ExpenseCategory.BILLS, 18, checking.id, sub = true),
                recurringExpense(familyId, "חדר כושר Holmes", 199.0, ExpenseCategory.LEISURE, 20, checking.id, sub = true),
                recurringExpense(familyId, "iCloud", 19.9, ExpenseCategory.SUBSCRIPTIONS, 22, checking.id, sub = true),
                RecurringEntity(
                    id = "rec_salary", familyId = familyId, name = "משכורת $userName",
                    type = TxType.INCOME, amount = 12_400.0, category = IncomeCategory.SALARY.name,
                    dayOfMonth = 10, accountId = checking.id
                )
            )
        )

        // --- budgets (section 18) -------------------------------------------
        db.budgetDao().upsertAll(
            listOf(
                budget(familyId, ExpenseCategory.GROCERY, 2_500.0),
                budget(familyId, ExpenseCategory.CAR, 1_500.0),
                budget(familyId, ExpenseCategory.LEISURE, 800.0),
                budget(familyId, ExpenseCategory.SHOPPING, 1_200.0),
                budget(familyId, ExpenseCategory.KIDS, 1_000.0),
                budget(familyId, ExpenseCategory.RESTAURANTS, 900.0)
            )
        )

        // --- goals (section 20) ---------------------------------------------
        db.goalDao().upsertAll(
            listOf(
                GoalEntity(
                    id = "goal_vacation", familyId = familyId, name = "חופשה משפחתית",
                    emoji = "✈️", targetAmount = 15_000.0, currentAmount = 8_400.0,
                    targetDate = Dates.toMillis(Dates.today().plusMonths(10)),
                    monthlyContribution = 850.0
                ),
                GoalEntity(
                    id = "goal_emergency", familyId = familyId, name = "קרן חירום",
                    emoji = "🛟", targetAmount = 40_000.0, currentAmount = 24_000.0,
                    targetDate = Dates.toMillis(Dates.today().plusMonths(18)),
                    monthlyContribution = 900.0
                ),
                GoalEntity(
                    id = "goal_car", familyId = familyId, name = "רכב חדש",
                    emoji = "🚗", targetAmount = 90_000.0, currentAmount = 12_500.0,
                    targetDate = Dates.toMillis(Dates.today().plusMonths(36)),
                    monthlyContribution = 2_100.0
                )
            )
        )

        // --- children (sections 22-23) --------------------------------------
        val uri = ChildEntity(
            id = "child_uri", familyId = familyId, name = "אורי", emoji = "👦",
            currentAge = 5, targetAge = 18, initialAmount = 4_120.0,
            monthlyContribution = 500.0, expectedAnnualReturn = 6.0
        )
        val noa = ChildEntity(
            id = "child_noa", familyId = familyId, name = "נועה", emoji = "👧",
            currentAge = 8, targetAge = 18, initialAmount = 3_500.0,
            monthlyContribution = 450.0, expectedAnnualReturn = 6.0
        )
        db.childDao().upsertAll(listOf(uri, noa))

        // --- investments (sections 21-22) -----------------------------------
        db.investmentDao().upsertAll(
            listOf(
                InvestmentEntity(
                    id = "inv_sp", familyId = familyId, ownerName = userName,
                    name = "מחקה S&P 500", symbol = "SPY", type = InvestmentType.ETF,
                    quantity = 32.0, averagePrice = 430.0, currentPrice = 468.0,
                    dividendsYtd = 640.0
                ),
                InvestmentEntity(
                    id = "inv_ta125", familyId = familyId, ownerName = partner,
                    name = "מחקה ת\"א 125", symbol = "TA125", type = InvestmentType.FUND,
                    quantity = 40.0, averagePrice = 152.0, currentPrice = 163.5,
                    dividendsYtd = 210.0
                ),
                InvestmentEntity(
                    id = "inv_bond", familyId = familyId, ownerName = userName,
                    name = "אג\"ח ממשלתי צמוד", symbol = "GOV", type = InvestmentType.BOND,
                    quantity = 25.0, averagePrice = 100.0, currentPrice = 103.2,
                    interestYtd = 180.0
                ),
                InvestmentEntity(
                    id = "inv_uri", familyId = familyId, ownerName = userName,
                    name = "תיק אורי", type = InvestmentType.FUND,
                    quantity = 1.0, averagePrice = 3_750.0, currentPrice = 4_120.0,
                    childId = uri.id
                ),
                InvestmentEntity(
                    id = "inv_noa", familyId = familyId, ownerName = userName,
                    name = "תיק נועה", type = InvestmentType.FUND,
                    quantity = 1.0, averagePrice = 3_155.0, currentPrice = 3_500.0,
                    childId = noa.id
                )
            )
        )

        // --- net-worth history (section 34) ---------------------------------
        val history = mutableListOf<NetWorthSnapshotEntity>()
        var base = 120_000.0
        for (back in 7 downTo 0) {
            val ym = thisMonth.minusMonths(back.toLong())
            base += rnd.nextInt(4_000, 11_000)
            history += NetWorthSnapshotEntity(
                yearMonth = Dates.yearMonthKey(ym),
                liquid = 15_230.0 + rnd.nextInt(-3000, 3000),
                savings = 18_000.0 + (7 - back) * 900,
                investments = 25_800.0 + (7 - back) * 1_400,
                childrenValue = 7_620.0 - back * 300,
                liabilities = 0.0,
                total = base
            )
        }
        db.snapshotDao().upsertAll(history)
    }

    // ---------------------------------------------------------------- helpers

    private fun income(
        familyId: String, by: String, accountId: String, amount: Double,
        merchant: String, category: IncomeCategory, ym: YearMonth, day: Int,
        recurring: Boolean = false
    ) = TransactionEntity(
        id = UUID.randomUUID().toString(), familyId = familyId, createdByName = by,
        accountId = accountId, type = TxType.INCOME, amount = amount, merchant = merchant,
        category = category.name,
        date = Dates.toMillis(ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))),
        isRecurring = recurring, source = TxSource.MANUAL
    )

    private fun expense(
        familyId: String, by: String, accountId: String?, cardId: String?, amount: Double,
        merchant: String, category: ExpenseCategory, ym: YearMonth, day: Int,
        recurring: Boolean = false
    ) = TransactionEntity(
        id = UUID.randomUUID().toString(), familyId = familyId, createdByName = by,
        accountId = accountId, cardId = cardId, type = TxType.EXPENSE, amount = amount,
        merchant = merchant, category = category.name,
        date = Dates.toMillis(ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))),
        isRecurring = recurring,
        source = if (cardId != null) TxSource.CARD else TxSource.MANUAL
    )

    private fun recurringExpense(
        familyId: String, name: String, amount: Double, category: ExpenseCategory,
        day: Int, accountId: String, sub: Boolean = false
    ) = RecurringEntity(
        id = "rec_${name.hashCode()}", familyId = familyId, name = name,
        type = TxType.EXPENSE, amount = amount, category = category.name,
        dayOfMonth = day, accountId = accountId, isSubscription = sub
    )

    private fun budget(familyId: String, category: ExpenseCategory, limit: Double) =
        BudgetEntity(
            id = "budget_${category.name}", familyId = familyId,
            category = category.name, limitAmount = limit
        )
}
