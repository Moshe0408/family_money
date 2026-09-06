package com.familymoney.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familymoney.data.model.*
import java.util.UUID

private fun newId() = UUID.randomUUID().toString()

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String = "me",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val familyId: String = "",
    val familyName: String = "",
    val role: MemberRole = MemberRole.OWNER,
    val currency: String = "ILS",
    val monthlyIncomeEstimate: Double = 0.0,
    val monthlyExpenseEstimate: Double = 0.0,
    val safetyBuffer: Double = 3000.0,
    val salaryDayOfMonth: Int = 10,
    val onboardingDone: Boolean = false,
    val aiEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "family_members", indices = [Index("familyId")])
data class FamilyMemberEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String,
    val userId: String,
    val name: String,
    val email: String = "",
    val role: MemberRole = MemberRole.PARTNER,
    val status: String = "active",
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "accounts", indices = [Index("familyId")])
data class AccountEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val ownerName: String = "",
    val provider: String = "",
    val type: AccountType = AccountType.CHECKING,
    val name: String,
    val balance: Double = 0.0,
    val currency: String = "ILS",
    val interestRatePercent: Double = 0.0,
    val lastSync: Long = 0L,
    val status: SyncStatus = SyncStatus.LOCAL,
    val includeInNetWorth: Boolean = true,
    val archived: Boolean = false
)

@Entity(tableName = "cards", indices = [Index("familyId")])
data class CardEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val ownerName: String = "",
    val provider: String = "",
    val last4: String = "",
    val type: CardType = CardType.CREDIT,
    val creditLimit: Double = 0.0,
    val usedCredit: Double = 0.0,
    val nextChargeAmount: Double = 0.0,
    val nextChargeDate: Long = 0L,
    val linkedAccountId: String? = null,
    val lastSync: Long = 0L,
    val status: SyncStatus = SyncStatus.LOCAL,
    val archived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("accountId"), Index("cardId"), Index("category")]
)
data class TransactionEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val createdByName: String = "",
    val accountId: String? = null,
    val cardId: String? = null,
    val type: TxType,
    val amount: Double,
    val currency: String = "ILS",
    val merchant: String = "",
    /** Enum name of ExpenseCategory or IncomeCategory depending on [type]. */
    val category: String = "OTHER",
    val date: Long,
    val isRecurring: Boolean = false,
    val note: String = "",
    val source: TxSource = TxSource.MANUAL,
    val pending: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets", indices = [Index(value = ["category", "yearMonth"], unique = true)])
data class BudgetEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val category: String,
    /** "2026-09"; empty string means "applies to every month". */
    val yearMonth: String = "",
    val limitAmount: Double
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val name: String,
    val emoji: String = "🎯",
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long,
    val monthlyContribution: Double = 0.0,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val ownerName: String = "",
    val name: String,
    val symbol: String = "",
    val type: InvestmentType = InvestmentType.ETF,
    val quantity: Double = 1.0,
    val averagePrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val currency: String = "ILS",
    val dividendsYtd: Double = 0.0,
    val interestYtd: Double = 0.0,
    /** Non-null when this holding belongs to a child portfolio. */
    val childId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val investedAmount: Double get() = quantity * averagePrice
    val currentValue: Double get() = quantity * currentPrice
    val profit: Double get() = currentValue - investedAmount
    val returnPercent: Double
        get() = if (investedAmount <= 0.0) 0.0 else (profit / investedAmount) * 100.0
}

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val name: String,
    val emoji: String = "🧒",
    val currentAge: Int = 0,
    val targetAge: Int = 18,
    val initialAmount: Double = 0.0,
    val monthlyContribution: Double = 0.0,
    val expectedAnnualReturn: Double = 6.0,
    val createdAt: Long = System.currentTimeMillis()
)

/** Standing orders / known upcoming charges used by the cash-flow forecast. */
@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val name: String,
    val type: TxType = TxType.EXPENSE,
    val amount: Double,
    val category: String = "OTHER",
    val dayOfMonth: Int = 1,
    val accountId: String? = null,
    val cardId: String? = null,
    val isSubscription: Boolean = false,
    val active: Boolean = true,
    val detected: Boolean = false
)

/** Learned merchant -> category mapping (section 31 of the spec). */
@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey val merchantKey: String,
    val category: String,
    val hits: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey val yearMonth: String,
    val liquid: Double,
    val savings: Double,
    val investments: Double,
    val childrenValue: Double,
    val liabilities: Double,
    val total: Double,
    val takenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "liabilities")
data class LiabilityEntity(
    @PrimaryKey val id: String = newId(),
    val familyId: String = "",
    val name: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val monthlyPayment: Double,
    val interestRatePercent: Double = 0.0,
    val endDate: Long = 0L
)

@Entity(tableName = "audit_log")
data class AuditEntity(
    @PrimaryKey val id: String = newId(),
    val actor: String,
    val action: String,
    val detail: String = "",
    val at: Long = System.currentTimeMillis()
)
