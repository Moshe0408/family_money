package com.familymoney.data.sync

import com.familymoney.data.db.AccountEntity
import com.familymoney.data.db.BudgetEntity
import com.familymoney.data.db.CardEntity
import com.familymoney.data.db.ChildEntity
import com.familymoney.data.db.GoalEntity
import com.familymoney.data.db.InvestmentEntity
import com.familymoney.data.db.LiabilityEntity
import com.familymoney.data.db.RecurringEntity
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.AccountType
import com.familymoney.data.model.CardType
import com.familymoney.data.model.GoalStatus
import com.familymoney.data.model.InvestmentType
import com.familymoney.data.model.SyncStatus
import com.familymoney.data.model.TxSource
import com.familymoney.data.model.TxType

/**
 * Entity <-> plain map conversion for the sync layer. Kept hand-written rather
 * than reflective so the wire format stays stable and reviewable.
 */
object SyncMapper {

    private fun Map<String, Any?>.str(k: String, d: String = "") = (this[k] as? String) ?: d
    private fun Map<String, Any?>.strOrNull(k: String) = this[k] as? String
    private fun Map<String, Any?>.dbl(k: String, d: Double = 0.0) =
        (this[k] as? Number)?.toDouble() ?: d
    private fun Map<String, Any?>.lng(k: String, d: Long = 0L) =
        (this[k] as? Number)?.toLong() ?: d
    private fun Map<String, Any?>.int(k: String, d: Int = 0) =
        (this[k] as? Number)?.toInt() ?: d
    private fun Map<String, Any?>.bool(k: String, d: Boolean = false) =
        (this[k] as? Boolean) ?: d

    // ------------------------------------------------------------ transactions

    fun toMap(t: TransactionEntity): Map<String, Any?> = mapOf(
        "id" to t.id, "familyId" to t.familyId, "createdByName" to t.createdByName,
        "accountId" to t.accountId, "cardId" to t.cardId, "type" to t.type.name,
        "amount" to t.amount, "currency" to t.currency, "merchant" to t.merchant,
        "category" to t.category, "date" to t.date, "isRecurring" to t.isRecurring,
        "note" to t.note, "source" to t.source.name, "pending" to t.pending,
        "updatedAt" to t.updatedAt
    )

    fun toTransaction(m: Map<String, Any?>) = TransactionEntity(
        id = m.str("id"), familyId = m.str("familyId"), createdByName = m.str("createdByName"),
        accountId = m.strOrNull("accountId"), cardId = m.strOrNull("cardId"),
        type = enumOr(m.str("type"), TxType.EXPENSE), amount = m.dbl("amount"),
        currency = m.str("currency", "ILS"), merchant = m.str("merchant"),
        category = m.str("category", "OTHER"), date = m.lng("date"),
        isRecurring = m.bool("isRecurring"), note = m.str("note"),
        source = enumOr(m.str("source"), TxSource.MANUAL), pending = m.bool("pending"),
        updatedAt = m.lng("updatedAt")
    )

    // ---------------------------------------------------------------- accounts

    fun toMap(a: AccountEntity): Map<String, Any?> = mapOf(
        "id" to a.id, "familyId" to a.familyId, "ownerName" to a.ownerName,
        "provider" to a.provider, "type" to a.type.name, "name" to a.name,
        "balance" to a.balance, "currency" to a.currency,
        "interestRatePercent" to a.interestRatePercent, "lastSync" to a.lastSync,
        "status" to a.status.name, "includeInNetWorth" to a.includeInNetWorth,
        "archived" to a.archived
    )

    fun toAccount(m: Map<String, Any?>) = AccountEntity(
        id = m.str("id"), familyId = m.str("familyId"), ownerName = m.str("ownerName"),
        provider = m.str("provider"), type = enumOr(m.str("type"), AccountType.CHECKING),
        name = m.str("name"), balance = m.dbl("balance"), currency = m.str("currency", "ILS"),
        interestRatePercent = m.dbl("interestRatePercent"), lastSync = m.lng("lastSync"),
        status = enumOr(m.str("status"), SyncStatus.LOCAL),
        includeInNetWorth = m.bool("includeInNetWorth", true), archived = m.bool("archived")
    )

    // ------------------------------------------------------------------- cards

    fun toMap(c: CardEntity): Map<String, Any?> = mapOf(
        "id" to c.id, "familyId" to c.familyId, "ownerName" to c.ownerName,
        "provider" to c.provider, "last4" to c.last4, "type" to c.type.name,
        "creditLimit" to c.creditLimit, "usedCredit" to c.usedCredit,
        "nextChargeAmount" to c.nextChargeAmount, "nextChargeDate" to c.nextChargeDate,
        "linkedAccountId" to c.linkedAccountId, "lastSync" to c.lastSync,
        "status" to c.status.name, "archived" to c.archived
    )

    fun toCard(m: Map<String, Any?>) = CardEntity(
        id = m.str("id"), familyId = m.str("familyId"), ownerName = m.str("ownerName"),
        provider = m.str("provider"), last4 = m.str("last4"),
        type = enumOr(m.str("type"), CardType.CREDIT), creditLimit = m.dbl("creditLimit"),
        usedCredit = m.dbl("usedCredit"), nextChargeAmount = m.dbl("nextChargeAmount"),
        nextChargeDate = m.lng("nextChargeDate"), linkedAccountId = m.strOrNull("linkedAccountId"),
        lastSync = m.lng("lastSync"), status = enumOr(m.str("status"), SyncStatus.LOCAL),
        archived = m.bool("archived")
    )

    // ----------------------------------------------------------------- budgets

    fun toMap(b: BudgetEntity): Map<String, Any?> = mapOf(
        "id" to b.id, "familyId" to b.familyId, "category" to b.category,
        "yearMonth" to b.yearMonth, "limitAmount" to b.limitAmount
    )

    fun toBudget(m: Map<String, Any?>) = BudgetEntity(
        id = m.str("id"), familyId = m.str("familyId"), category = m.str("category"),
        yearMonth = m.str("yearMonth"), limitAmount = m.dbl("limitAmount")
    )

    // ------------------------------------------------------------------- goals

    fun toMap(g: GoalEntity): Map<String, Any?> = mapOf(
        "id" to g.id, "familyId" to g.familyId, "name" to g.name, "emoji" to g.emoji,
        "targetAmount" to g.targetAmount, "currentAmount" to g.currentAmount,
        "targetDate" to g.targetDate, "monthlyContribution" to g.monthlyContribution,
        "status" to g.status.name, "createdAt" to g.createdAt
    )

    fun toGoal(m: Map<String, Any?>) = GoalEntity(
        id = m.str("id"), familyId = m.str("familyId"), name = m.str("name"),
        emoji = m.str("emoji", "🎯"), targetAmount = m.dbl("targetAmount"),
        currentAmount = m.dbl("currentAmount"), targetDate = m.lng("targetDate"),
        monthlyContribution = m.dbl("monthlyContribution"),
        status = enumOr(m.str("status"), GoalStatus.ACTIVE), createdAt = m.lng("createdAt")
    )

    // ------------------------------------------------------------- investments

    fun toMap(i: InvestmentEntity): Map<String, Any?> = mapOf(
        "id" to i.id, "familyId" to i.familyId, "ownerName" to i.ownerName,
        "name" to i.name, "symbol" to i.symbol, "type" to i.type.name,
        "quantity" to i.quantity, "averagePrice" to i.averagePrice,
        "currentPrice" to i.currentPrice, "currency" to i.currency,
        "dividendsYtd" to i.dividendsYtd, "interestYtd" to i.interestYtd,
        "childId" to i.childId, "updatedAt" to i.updatedAt
    )

    fun toInvestment(m: Map<String, Any?>) = InvestmentEntity(
        id = m.str("id"), familyId = m.str("familyId"), ownerName = m.str("ownerName"),
        name = m.str("name"), symbol = m.str("symbol"),
        type = enumOr(m.str("type"), InvestmentType.ETF), quantity = m.dbl("quantity", 1.0),
        averagePrice = m.dbl("averagePrice"), currentPrice = m.dbl("currentPrice"),
        currency = m.str("currency", "ILS"), dividendsYtd = m.dbl("dividendsYtd"),
        interestYtd = m.dbl("interestYtd"), childId = m.strOrNull("childId"),
        updatedAt = m.lng("updatedAt")
    )

    // ---------------------------------------------------------------- children

    fun toMap(c: ChildEntity): Map<String, Any?> = mapOf(
        "id" to c.id, "familyId" to c.familyId, "name" to c.name, "emoji" to c.emoji,
        "currentAge" to c.currentAge, "targetAge" to c.targetAge,
        "initialAmount" to c.initialAmount, "monthlyContribution" to c.monthlyContribution,
        "expectedAnnualReturn" to c.expectedAnnualReturn, "createdAt" to c.createdAt
    )

    fun toChild(m: Map<String, Any?>) = ChildEntity(
        id = m.str("id"), familyId = m.str("familyId"), name = m.str("name"),
        emoji = m.str("emoji", "🧒"), currentAge = m.int("currentAge"),
        targetAge = m.int("targetAge", 18), initialAmount = m.dbl("initialAmount"),
        monthlyContribution = m.dbl("monthlyContribution"),
        expectedAnnualReturn = m.dbl("expectedAnnualReturn", 6.0), createdAt = m.lng("createdAt")
    )

    // --------------------------------------------------------------- recurring

    fun toMap(r: RecurringEntity): Map<String, Any?> = mapOf(
        "id" to r.id, "familyId" to r.familyId, "name" to r.name, "type" to r.type.name,
        "amount" to r.amount, "category" to r.category, "dayOfMonth" to r.dayOfMonth,
        "accountId" to r.accountId, "cardId" to r.cardId,
        "isSubscription" to r.isSubscription, "active" to r.active, "detected" to r.detected
    )

    fun toRecurring(m: Map<String, Any?>) = RecurringEntity(
        id = m.str("id"), familyId = m.str("familyId"), name = m.str("name"),
        type = enumOr(m.str("type"), TxType.EXPENSE), amount = m.dbl("amount"),
        category = m.str("category", "OTHER"), dayOfMonth = m.int("dayOfMonth", 1),
        accountId = m.strOrNull("accountId"), cardId = m.strOrNull("cardId"),
        isSubscription = m.bool("isSubscription"), active = m.bool("active", true),
        detected = m.bool("detected")
    )

    // ------------------------------------------------------------- liabilities

    fun toMap(l: LiabilityEntity): Map<String, Any?> = mapOf(
        "id" to l.id, "familyId" to l.familyId, "name" to l.name,
        "totalAmount" to l.totalAmount, "remainingAmount" to l.remainingAmount,
        "monthlyPayment" to l.monthlyPayment,
        "interestRatePercent" to l.interestRatePercent, "endDate" to l.endDate
    )

    fun toLiability(m: Map<String, Any?>) = LiabilityEntity(
        id = m.str("id"), familyId = m.str("familyId"), name = m.str("name"),
        totalAmount = m.dbl("totalAmount"), remainingAmount = m.dbl("remainingAmount"),
        monthlyPayment = m.dbl("monthlyPayment"),
        interestRatePercent = m.dbl("interestRatePercent"), endDate = m.lng("endDate")
    )

    // ----------------------------------------------------------------- members

    fun toMap(m: SyncMember): Map<String, Any?> =
        mapOf("uid" to m.uid, "name" to m.name, "role" to m.role, "joinedAt" to m.joinedAt)

    fun toMember(m: Map<String, Any?>) = SyncMember(
        uid = m.str("uid"), name = m.str("name"),
        role = m.str("role", "PARTNER"), joinedAt = m.lng("joinedAt")
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        runCatching { enumValueOf<E>(name) }.getOrDefault(fallback)
}
