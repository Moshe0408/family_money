package com.familymoney.data.repo

import android.content.Context
import com.familymoney.data.db.AccountEntity
import com.familymoney.data.db.AppDatabase
import com.familymoney.data.db.AuditEntity
import com.familymoney.data.db.BudgetEntity
import com.familymoney.data.db.CardEntity
import com.familymoney.data.db.CategoryRuleEntity
import com.familymoney.data.db.ChildEntity
import com.familymoney.data.db.FamilyMemberEntity
import com.familymoney.data.db.GoalEntity
import com.familymoney.data.db.InvestmentEntity
import com.familymoney.data.db.LiabilityEntity
import com.familymoney.data.db.NetWorthSnapshotEntity
import com.familymoney.data.db.ProfileEntity
import com.familymoney.data.db.RecurringEntity
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.AccountType
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.TxType
import com.familymoney.engine.FinanceSnapshot
import com.familymoney.engine.GoalSnapshot
import com.familymoney.engine.RecurringDetector
import com.familymoney.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth

/**
 * Single access point for everything financial. Screens observe the flows here;
 * the recommendation engine receives the [FinanceSnapshot] this class builds.
 */
class FinanceRepository(context: Context) {

    private val db = AppDatabase.get(context)

    val profile: Flow<ProfileEntity?> = db.profileDao().observe()
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val accounts: Flow<List<AccountEntity>> = db.accountDao().observeAll()
    val cards: Flow<List<CardEntity>> = db.cardDao().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgetDao().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goalDao().observeAll()
    val investments: Flow<List<InvestmentEntity>> = db.investmentDao().observeAll()
    val children: Flow<List<ChildEntity>> = db.childDao().observeAll()
    val recurring: Flow<List<RecurringEntity>> = db.recurringDao().observeAll()
    val members: Flow<List<FamilyMemberEntity>> = db.familyMemberDao().observeAll()
    val liabilities: Flow<List<LiabilityEntity>> = db.liabilityDao().observeAll()
    val snapshots: Flow<List<NetWorthSnapshotEntity>> = db.snapshotDao().observeAll()
    val categoryRules: Flow<List<CategoryRuleEntity>> = db.categoryRuleDao().observeAll()
    val auditLog: Flow<List<AuditEntity>> = db.auditDao().observeAll()

    // ------------------------------------------------------------------ writes

    suspend fun saveProfile(p: ProfileEntity) = db.profileDao().upsert(p)
    suspend fun getProfile(): ProfileEntity? = db.profileDao().get()

    suspend fun addTransaction(tx: TransactionEntity) {
        db.transactionDao().upsert(tx)
        if (tx.type == TxType.EXPENSE && tx.merchant.isNotBlank()) {
            learnCategory(tx.merchant, tx.category)
        }
        applyBalanceEffect(tx)
        audit(tx.createdByName, "add_transaction", "${tx.merchant} ${tx.amount}")
    }

    suspend fun updateTransaction(tx: TransactionEntity) {
        db.transactionDao().upsert(tx.copy(updatedAt = System.currentTimeMillis()))
        if (tx.type == TxType.EXPENSE && tx.merchant.isNotBlank()) {
            learnCategory(tx.merchant, tx.category)
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        db.transactionDao().delete(tx)
        audit(tx.createdByName, "delete_transaction", tx.merchant)
    }

    suspend fun importTransactions(list: List<TransactionEntity>) {
        db.transactionDao().upsertAll(list)
        audit("import", "import_transactions", "${list.size} עסקאות")
    }

    /** Keep the linked account/card in sync so balances stay believable. */
    private suspend fun applyBalanceEffect(tx: TransactionEntity) {
        tx.accountId?.let { id ->
            db.accountDao().byId(id)?.let { acc ->
                val delta = if (tx.type == TxType.INCOME) tx.amount else -tx.amount
                db.accountDao().upsert(acc.copy(balance = acc.balance + delta))
            }
        }
        tx.cardId?.let { id ->
            db.cardDao().all().firstOrNull { it.id == id }?.let { card ->
                if (tx.type == TxType.EXPENSE) {
                    db.cardDao().upsert(
                        card.copy(
                            usedCredit = card.usedCredit + tx.amount,
                            nextChargeAmount = card.nextChargeAmount + tx.amount
                        )
                    )
                }
            }
        }
    }

    suspend fun saveAccount(a: AccountEntity) = db.accountDao().upsert(a)
    suspend fun deleteAccount(a: AccountEntity) = db.accountDao().delete(a)
    suspend fun saveCard(c: CardEntity) = db.cardDao().upsert(c)
    suspend fun deleteCard(c: CardEntity) = db.cardDao().delete(c)
    suspend fun saveBudget(b: BudgetEntity) = db.budgetDao().upsert(b)
    suspend fun deleteBudget(b: BudgetEntity) = db.budgetDao().delete(b)
    suspend fun saveGoal(g: GoalEntity) = db.goalDao().upsert(g)
    suspend fun deleteGoal(g: GoalEntity) = db.goalDao().delete(g)
    suspend fun saveInvestment(i: InvestmentEntity) = db.investmentDao().upsert(i)
    suspend fun deleteInvestment(i: InvestmentEntity) = db.investmentDao().delete(i)
    suspend fun saveChild(c: ChildEntity) = db.childDao().upsert(c)
    suspend fun deleteChild(c: ChildEntity) = db.childDao().delete(c)
    suspend fun saveRecurring(r: RecurringEntity) = db.recurringDao().upsert(r)
    suspend fun deleteRecurring(r: RecurringEntity) = db.recurringDao().delete(r)
    suspend fun saveMember(m: FamilyMemberEntity) = db.familyMemberDao().upsert(m)
    suspend fun deleteMember(m: FamilyMemberEntity) = db.familyMemberDao().delete(m)
    suspend fun saveLiability(l: LiabilityEntity) = db.liabilityDao().upsert(l)
    suspend fun deleteLiability(l: LiabilityEntity) = db.liabilityDao().delete(l)
    suspend fun saveSnapshot(s: NetWorthSnapshotEntity) = db.snapshotDao().upsert(s)

    suspend fun audit(actor: String, action: String, detail: String = "") =
        db.auditDao().add(AuditEntity(actor = actor, action = action, detail = detail))

    /** Contribute to a goal and record the movement as a real transaction. */
    suspend fun contributeToGoal(goal: GoalEntity, amount: Double, fromAccountId: String?, actor: String) {
        db.goalDao().upsert(goal.copy(currentAmount = goal.currentAmount + amount))
        fromAccountId?.let { id ->
            db.accountDao().byId(id)?.let { acc ->
                db.accountDao().upsert(acc.copy(balance = acc.balance - amount))
            }
        }
        audit(actor, "goal_contribution", "${goal.name}: $amount")
    }

    // -------------------------------------------------------- category learning

    suspend fun learnCategory(merchant: String, category: String) {
        val key = RecurringDetector.normalize(merchant)
        if (key.isBlank()) return
        val existing = db.categoryRuleDao().byMerchant(key)
        db.categoryRuleDao().upsert(
            CategoryRuleEntity(
                merchantKey = key,
                category = category,
                hits = (existing?.hits ?: 0) + 1
            )
        )
    }

    /** Section 11/31: guess a category from what the user chose before. */
    suspend fun suggestCategory(merchant: String): ExpenseCategory {
        val key = RecurringDetector.normalize(merchant)
        db.categoryRuleDao().byMerchant(key)?.let { return ExpenseCategory.fromKey(it.category) }
        return guessFromKeywords(merchant)
    }

    private fun guessFromKeywords(merchant: String): ExpenseCategory {
        val m = merchant.lowercase()
        val table = listOf(
            ExpenseCategory.GROCERY to listOf("שופרסל", "רמי לוי", "ויקטורי", "יוחננוף", "אושר עד", "טיב טעם", "am:pm", "מגה", "סופר"),
            ExpenseCategory.RESTAURANTS to listOf("wolt", "וולט", "מקדונלד", "בורגר", "קפה", "מסעדה", "ten bis", "תן ביס", "pizza", "פיצה"),
            ExpenseCategory.FUEL to listOf("פז", "דלק", "סונול", "דור אלון", "ten", "yellow"),
            ExpenseCategory.CAR to listOf("מוסך", "צמיג", "חניון", "pango", "פנגו", "cellopark", "כביש 6", "ביטוח רכב"),
            ExpenseCategory.BILLS to listOf("חשמל", "מים", "ארנונה", "בזק", "hot", "פרטנר", "סלקום", "גז"),
            ExpenseCategory.SUBSCRIPTIONS to listOf("netflix", "spotify", "youtube", "icloud", "apple.com", "google", "microsoft", "adobe", "openai", "anthropic"),
            ExpenseCategory.HEALTH to listOf("סופר פארם", "בית מרקחת", "מכבי", "כללית", "מאוחדת", "לאומית", "רופא"),
            ExpenseCategory.KIDS to listOf("גן ", "צהרון", "בייביסיטר", "משפחתון", "חוג"),
            ExpenseCategory.SHOPPING to listOf("zara", "castro", "fox", "ikea", "aliexpress", "amazon", "shein", "terminal x"),
            ExpenseCategory.INSURANCE to listOf("ביטוח", "הראל", "מגדל", "כלל", "מנורה", "איילון", "פניקס"),
            ExpenseCategory.HOUSING to listOf("שכירות", "משכנתא", "ועד בית"),
            ExpenseCategory.LEISURE to listOf("סינמה", "יס פלאנט", "הופעה", "כרטיס", "מלון", "booking", "airbnb")
        )
        table.forEach { (cat, keys) -> if (keys.any { m.contains(it) }) return cat }
        return ExpenseCategory.OTHER
    }

    // --------------------------------------------------------------- snapshot

    /**
     * Assembles the full picture the engine reasons over. Combines every source
     * flow so recommendations refresh the instant anything changes.
     */
    val snapshot: Flow<FinanceSnapshot> = combine(
        listOf(
            profile, transactions, accounts, cards, budgets,
            goals, investments, children, recurring, liabilities
        )
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        buildSnapshot(
            profile = values[0] as ProfileEntity?,
            txs = values[1] as List<TransactionEntity>,
            accounts = values[2] as List<AccountEntity>,
            cards = values[3] as List<CardEntity>,
            budgets = values[4] as List<BudgetEntity>,
            goals = values[5] as List<GoalEntity>,
            investments = values[6] as List<InvestmentEntity>,
            children = values[7] as List<ChildEntity>,
            recurring = values[8] as List<RecurringEntity>,
            liabilities = values[9] as List<LiabilityEntity>
        )
    }

    fun buildSnapshot(
        profile: ProfileEntity?,
        txs: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        cards: List<CardEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        investments: List<InvestmentEntity>,
        children: List<ChildEntity>,
        recurring: List<RecurringEntity>,
        liabilities: List<LiabilityEntity>,
        now: Long = System.currentTimeMillis()
    ): FinanceSnapshot {
        val ym = YearMonth.from(Dates.toLocalDate(now))
        val monthFrom = Dates.monthStart(ym)
        val monthTo = Dates.monthEnd(ym)

        val monthTxs = txs.filter { it.date in monthFrom..monthTo }
        val monthIncome = monthTxs.filter { it.type == TxType.INCOME }.sumOf { it.amount }
        val monthExpense = monthTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

        val liquid = accounts.filter { it.type == AccountType.CHECKING || it.type == AccountType.CASH }
            .sumOf { it.balance }
        val savings = accounts.filter { it.type == AccountType.SAVINGS || it.type == AccountType.DEPOSIT }
            .sumOf { it.balance }

        val childInvestmentIds = children.map { it.id }.toSet()
        val childrenValue = investments.filter { it.childId in childInvestmentIds }.sumOf { it.currentValue } +
            accounts.filter { it.type == AccountType.CHILD }.sumOf { it.balance }
        val investmentValue = investments.filter { it.childId == null }.sumOf { it.currentValue } +
            accounts.filter { it.type == AccountType.INVESTMENT }.sumOf { it.balance }

        // Per-category spend this month vs the average of the 6 months before it.
        val categorySpend = monthTxs.filter { it.type == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

        val historyFrom = Dates.monthStart(ym.minusMonths(6))
        val historyTxs = txs.filter { it.type == TxType.EXPENSE && it.date in historyFrom until monthFrom }
        val monthsOfHistory = historyTxs
            .map { Dates.yearMonthKey(it.date) }.distinct().size.coerceAtLeast(1)
        val categoryAverage = historyTxs.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } / monthsOfHistory }

        val today = Dates.toLocalDate(now)
        val daysLeft = Dates.daysLeftInMonth(today)
        val daysElapsed = today.dayOfMonth.coerceAtLeast(1)

        // Run-rate excludes recurring charges — those are counted separately as
        // "upcoming charges" so we do not double-count them.
        val variableSpend = monthTxs
            .filter { it.type == TxType.EXPENSE && !it.isRecurring }
            .sumOf { it.amount }
        val averageDaily = variableSpend / daysElapsed

        val remainingRecurringExpense = recurring
            .filter { it.active && it.type == TxType.EXPENSE && it.dayOfMonth > today.dayOfMonth }
            .sumOf { it.amount }
        val cardCharges = cards.filter { !it.archived }
            .filter { it.nextChargeDate == 0L || it.nextChargeDate in now..monthTo }
            .sumOf { it.nextChargeAmount }

        val expectedIncome = recurring
            .filter { it.active && it.type == TxType.INCOME && it.dayOfMonth > today.dayOfMonth }
            .sumOf { it.amount }
            .let { fromRecurring ->
                // Fall back to the onboarding estimate when the salary has not
                // landed yet and no standing order describes it.
                val salaryArrived = monthTxs.any {
                    it.type == com.familymoney.data.model.TxType.INCOME && it.amount >= 1000
                }
                val estimate = profile?.monthlyIncomeEstimate ?: 0.0
                if (fromRecurring > 0) fromRecurring
                else if (!salaryArrived && estimate > 0 && (profile?.salaryDayOfMonth ?: 1) > today.dayOfMonth) estimate
                else 0.0
            }

        val subscriptions = recurring.filter { it.active && it.isSubscription }
        val recurringMonthly = recurring.filter { it.active && it.type == TxType.EXPENSE }.sumOf { it.amount }

        val monthlyToInvestments = recurring
            .filter { it.active && it.type == TxType.EXPENSE && it.category == "INVESTMENT" }
            .sumOf { it.amount } + children.sumOf { it.monthlyContribution }

        val goalSnapshots = goals
            .filter { it.status == com.familymoney.data.model.GoalStatus.ACTIVE }
            .map {
                GoalSnapshot(
                    id = it.id,
                    name = it.name,
                    emoji = it.emoji,
                    target = it.targetAmount,
                    current = it.currentAmount,
                    targetDate = it.targetDate,
                    monthsRemaining = Dates.monthsBetween(now, it.targetDate)
                )
            }

        val budgetMap = budgets
            .filter { it.yearMonth.isEmpty() || it.yearMonth == Dates.yearMonthKey(ym) }
            .associate { it.category to it.limitAmount }

        return FinanceSnapshot(
            today = now,
            liquidBalance = liquid,
            savingsBalance = savings,
            investmentValue = investmentValue,
            childrenValue = childrenValue,
            liabilities = liabilities.sumOf { it.remainingAmount },
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            expectedIncomeRestOfMonth = expectedIncome,
            upcomingCharges = remainingRecurringExpense + cardCharges,
            averageDailySpend = averageDaily,
            daysLeftInMonth = daysLeft,
            safetyBuffer = profile?.safetyBuffer ?: 3000.0,
            categorySpendThisMonth = categorySpend,
            categoryAverage = categoryAverage,
            budgets = budgetMap,
            goals = goalSnapshots,
            recurringMonthlyTotal = recurringMonthly,
            subscriptionCount = subscriptions.size,
            subscriptionMonthlyCost = subscriptions.sumOf { it.amount },
            monthlyContributionsToInvestments = monthlyToInvestments
        )
    }

    /** Section 32: rescan history and store what looks like a standing charge. */
    suspend fun refreshRecurringDetection() {
        val txs = db.transactionDao().recent(600)
        val detected = RecurringDetector.detect(txs)
        val existing = db.recurringDao().all().filter { !it.detected }
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        val fresh = detected.filter { it.name.lowercase() !in existingNames }
        if (fresh.isNotEmpty()) db.recurringDao().upsertAll(fresh)
    }

    /** Section 34: store this month's net worth so the chart has a history. */
    suspend fun captureNetWorthSnapshot(s: FinanceSnapshot) {
        db.snapshotDao().upsert(
            NetWorthSnapshotEntity(
                yearMonth = Dates.yearMonthKey(s.today),
                liquid = s.liquidBalance,
                savings = s.savingsBalance,
                investments = s.investmentValue,
                childrenValue = s.childrenValue,
                liabilities = s.liabilities,
                total = s.netWorth
            )
        )
    }

    suspend fun wipeAll() {
        db.transactionDao().clear()
        db.accountDao().clear()
        db.cardDao().clear()
        db.budgetDao().clear()
        db.goalDao().clear()
        db.investmentDao().clear()
        db.childDao().clear()
        db.recurringDao().clear()
        db.categoryRuleDao().clear()
        db.familyMemberDao().clear()
        db.liabilityDao().clear()
        db.snapshotDao().clear()
        db.auditDao().clear()
    }

    suspend fun transactionCount(): Int = db.transactionDao().count()

    val monthlyTotals: Flow<List<MonthTotal>> = transactions.map { txs ->
        txs.groupBy { Dates.yearMonthKey(it.date) }
            .map { (key, list) ->
                MonthTotal(
                    yearMonth = key,
                    income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount },
                    expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                )
            }
            .sortedBy { it.yearMonth }
    }

    internal fun database() = db
}

data class MonthTotal(val yearMonth: String, val income: Double, val expense: Double) {
    val saving: Double get() = income - expense
    val savingRate: Double get() = if (income <= 0) 0.0 else saving / income * 100
}
