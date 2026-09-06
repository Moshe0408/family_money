package com.familymoney.data.model

/** Direction of money movement. */
enum class TxType { INCOME, EXPENSE }

enum class ExpenseCategory(val he: String, val emoji: String) {
    HOUSING("דיור", "🏠"),
    FOOD("מזון", "🍎"),
    GROCERY("סופר", "🛒"),
    CAR("רכב", "🚗"),
    FUEL("דלק", "⛽"),
    BILLS("חשבונות", "🧾"),
    INSURANCE("ביטוחים", "🛡️"),
    KIDS("ילדים", "🧒"),
    HEALTH("בריאות", "💊"),
    SHOPPING("קניות", "🛍️"),
    LEISURE("בילויים", "🎡"),
    RESTAURANTS("מסעדות", "🍔"),
    LOANS("הלוואות", "🏦"),
    SUBSCRIPTIONS("מנויים", "📺"),
    EDUCATION("לימודים", "🎓"),
    OTHER("אחר", "📦");

    companion object {
        fun fromKey(key: String?): ExpenseCategory =
            entries.firstOrNull { it.name.equals(key, true) } ?: OTHER
    }
}

enum class IncomeCategory(val he: String, val emoji: String) {
    SALARY("משכורת", "💼"),
    BUSINESS("עסק", "🏢"),
    BONUS("בונוס", "🎁"),
    INTEREST("ריבית", "📈"),
    DIVIDEND("דיבידנד", "💵"),
    REFUND("החזר", "↩️"),
    OTHER("הכנסה נוספת", "➕");

    companion object {
        fun fromKey(key: String?): IncomeCategory =
            entries.firstOrNull { it.name.equals(key, true) } ?: OTHER
    }
}

enum class AccountType(val he: String) {
    CHECKING("עו\"ש"),
    SAVINGS("חיסכון"),
    DEPOSIT("פיקדון"),
    INVESTMENT("השקעות"),
    CHILD("השקעות ילדים"),
    CASH("מזומן")
}

enum class CardType(val he: String) {
    CREDIT("אשראי"),
    DEBIT("דביט"),
    PREPAID("נטען")
}

enum class MemberRole(val he: String) {
    OWNER("בעלים"),
    PARTNER("שותף"),
    VIEWER("צופה")
}

enum class SyncStatus { LOCAL, CONNECTED, ERROR, EXPIRED }

enum class GoalStatus { ACTIVE, COMPLETED, PAUSED }

enum class InvestmentType(val he: String) {
    STOCK("מניה"),
    ETF("ETF"),
    BOND("אג\"ח"),
    FUND("קרן"),
    DEPOSIT("פיקדון"),
    SAVINGS("חיסכון"),
    CRYPTO("קריפטו"),
    OTHER("אחר")
}

enum class RecPriority(val he: String, val weight: Int) {
    HIGH("גבוהה", 3), MEDIUM("בינונית", 2), LOW("נמוכה", 1)
}

enum class RecCategory(val he: String, val emoji: String) {
    SAVING("חיסכון", "🟢"),
    SPENDING("הוצאות", "🟡"),
    CASHFLOW("תזרים", "🔵"),
    GOAL("יעדים", "🎯"),
    INVESTMENT("השקעות", "🟣"),
    BUDGET("תקציב", "📊"),
    ALERT("חריגה", "⚠️")
}

enum class TxSource { MANUAL, IMPORT, BANK, CARD, RECURRING }
