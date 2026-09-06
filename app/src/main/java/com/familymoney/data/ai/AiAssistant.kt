package com.familymoney.data.ai

import com.familymoney.data.model.ExpenseCategory
import com.familymoney.engine.FinanceSnapshot
import com.familymoney.engine.Recommendation
import com.familymoney.util.Dates
import com.familymoney.util.Money

/**
 * Section 27: the "ask my money" assistant.
 *
 * The model only ever receives an aggregated financial brief — never raw
 * transaction rows, account numbers or card digits.
 */
class AiAssistant(private val client: GroqClient) {

    companion object {
        private const val SYSTEM_PROMPT = """
אתה יועץ פיננסי אישי בתוך אפליקציה ישראלית לניהול כספי משפחה.

כללים:
- ענה תמיד בעברית, בגוף שני, בטון ידידותי וישיר.
- התבסס אך ורק על הנתונים שבתמונת המצב שתקבל. אם חסר נתון — אמור זאת במפורש ואל תמציא מספרים.
- כל הסכומים בשקלים חדשים, בפורמט ₪1,234.
- תשובה קצרה: 2-5 משפטים, או רשימת בולטים קצרה. בלי הקדמות ובלי "כמובן".
- כשמתאים, סיים בהצעת פעולה קונקרטית אחת.
- אתה יכול לבצע חישובים (ריבית דריבית, תחזיות, אחוזים) ולהסביר אותם.
- אינך יועץ השקעות מורשה. אל תמליץ על ניירות ערך, מניות או מוצרים פיננסיים ספציפיים.
  אם נשאלת על כך, הסבר שאתה מציג חישובים כלליים בלבד והפנה לייעוץ מורשה.
- אל תבקש ואל תזכיר פרטי כרטיס אשראי, סיסמאות או פרטי חשבון בנק.
- כתוב טקסט רגיל בלבד. אל תשתמש ב־Markdown: בלי כוכביות, בלי סולמיות ובלי הדגשות.
"""
    }

    /** Compact, privacy-preserving brief handed to the model on every turn. */
    fun buildContext(
        s: FinanceSnapshot,
        recommendations: List<Recommendation>,
        familyName: String,
        userName: String
    ): String = buildString {
        appendLine("=== תמונת מצב פיננסית ===")
        appendLine("תאריך: ${Dates.formatDay(s.today)}")
        appendLine("משתמש: $userName${if (familyName.isNotBlank()) " | משפחה: $familyName" else ""}")
        appendLine()
        appendLine("שווי פיננסי כולל: ${Money.format(s.netWorth)}")
        appendLine("  עו\"ש/מזומן: ${Money.format(s.liquidBalance)}")
        appendLine("  חיסכון ופיקדונות: ${Money.format(s.savingsBalance)}")
        appendLine("  השקעות: ${Money.format(s.investmentValue)}")
        appendLine("  השקעות ילדים: ${Money.format(s.childrenValue)}")
        if (s.liabilities > 0) appendLine("  התחייבויות: ${Money.format(s.liabilities)}")
        appendLine()
        appendLine("החודש הנוכחי:")
        appendLine("  הכנסות: ${Money.format(s.monthIncome)}")
        appendLine("  הוצאות: ${Money.format(s.monthExpense)}")
        appendLine("  חיסכון: ${Money.format(s.monthSaving)} (${Money.percent(s.savingRatePercent)})")
        appendLine("  נותרו ${s.daysLeftInMonth} ימים בחודש")
        appendLine("  קצב הוצאה יומי: ${Money.format(s.averageDailySpend)}")
        appendLine()
        appendLine("תחזית לסוף החודש:")
        appendLine("  הכנסות שעוד צפויות: ${Money.format(s.expectedIncomeRestOfMonth)}")
        appendLine("  חיובים צפויים (הו\"ק + אשראי): ${Money.format(s.upcomingCharges)}")
        appendLine("  הוצאות שוטפות צפויות: ${Money.format(s.projectedRemainingSpend)}")
        appendLine("  יתרה צפויה: ${Money.format(s.projectedEndOfMonthBalance)}")
        appendLine("  כרית ביטחון מוגדרת: ${Money.format(s.safetyBuffer)}")
        appendLine("  סכום פנוי להעברה לחיסכון: ${Money.format(s.availableToSave)}")

        if (s.categorySpendThisMonth.isNotEmpty()) {
            appendLine()
            appendLine("הוצאות לפי קטגוריה החודש (מול ממוצע 6 חודשים):")
            s.categorySpendThisMonth.entries
                .sortedByDescending { it.value }
                .take(10)
                .forEach { (key, amount) ->
                    val cat = ExpenseCategory.fromKey(key)
                    val avg = s.categoryAverage[key]
                    val avgText = if (avg != null && avg > 0)
                        " (ממוצע ${Money.format(avg)})" else ""
                    appendLine("  ${cat.he}: ${Money.format(amount)}$avgText")
                }
        }

        if (s.budgets.isNotEmpty()) {
            appendLine()
            appendLine("תקציבים:")
            s.budgets.forEach { (key, limit) ->
                val spent = s.categorySpendThisMonth[key] ?: 0.0
                appendLine("  ${ExpenseCategory.fromKey(key).he}: ${Money.format(spent)} / ${Money.format(limit)}")
            }
        }

        if (s.goals.isNotEmpty()) {
            appendLine()
            appendLine("יעדי חיסכון:")
            s.goals.forEach { g ->
                appendLine(
                    "  ${g.name}: ${Money.format(g.current)} מתוך ${Money.format(g.target)} " +
                        "(${Money.percent(g.progressPercent)}), נותרו ${g.monthsRemaining} חודשים, " +
                        "נדרש ${Money.format(g.requiredMonthly)} לחודש"
                )
            }
        }

        if (s.recurringMonthlyTotal > 0) {
            appendLine()
            appendLine("הוצאות קבועות: ${Money.format(s.recurringMonthlyTotal)} לחודש")
            if (s.subscriptionCount > 0) {
                appendLine("  מתוכן ${s.subscriptionCount} מנויים בעלות ${Money.format(s.subscriptionMonthlyCost)} לחודש")
            }
        }

        if (recommendations.isNotEmpty()) {
            appendLine()
            appendLine("המלצות שהמערכת כבר חישבה:")
            recommendations.take(5).forEach { appendLine("  - ${it.title}: ${it.reason}") }
        }
        appendLine("=== סוף תמונת המצב ===")
    }

    suspend fun ask(
        question: String,
        context: String,
        history: List<GroqClient.Message> = emptyList()
    ): GroqClient.Result {
        val messages = buildList {
            add(GroqClient.Message("system", SYSTEM_PROMPT.trim()))
            add(GroqClient.Message("system", context))
            addAll(history.takeLast(8))
            add(GroqClient.Message("user", question))
        }
        return client.chat(messages)
    }

    /** Section 5: one paragraph for the "what should we do now?" card. */
    suspend fun dailyBriefing(context: String): GroqClient.Result {
        val messages = listOf(
            GroqClient.Message("system", SYSTEM_PROMPT.trim()),
            GroqClient.Message("system", context),
            GroqClient.Message(
                "user",
                "כתוב סיכום קצר של 2-3 משפטים בלבד: מה מצב הכסף שלנו החודש ומה הדבר " +
                    "הכי חשוב לעשות עכשיו. בלי כותרות, בלי בולטים, טקסט רץ."
            )
        )
        return client.chat(messages, temperature = 0.4)
    }

    /** Suggested prompts shown as chips on the assistant screen. */
    val starterQuestions = listOf(
        "כמה אני יכול להוציא החודש?",
        "למה אני לא מצליח לחסוך?",
        "כמה הוצאנו על אוכל השנה?",
        "כמה כסף כדאי לשים בצד עכשיו?",
        "אם אחסוך ₪2,000 בחודש כמה יהיה לי בעוד 10 שנים?",
        "אפשר להרשות לעצמנו חופשה ב־₪15,000?",
        "מה ההוצאה הכי גדולה שלנו?",
        "כמה אנחנו חוסכים בממוצע?"
    )
}
