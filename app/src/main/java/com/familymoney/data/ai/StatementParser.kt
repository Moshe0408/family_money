package com.familymoney.data.ai

import com.familymoney.data.bank.CsvImporter
import com.familymoney.data.model.TxType
import com.familymoney.util.Dates
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Turns text copied out of a bank or credit-card statement into transactions.
 *
 * PDF statements from Israeli banks are RTL tables whose column order shifts
 * between banks and even between report types. Rather than fight that with
 * regexes, the text is handed to the model, which is good at exactly this kind
 * of loose structure recovery. The result is then validated locally — the model
 * is trusted to *read*, never to decide what lands in the database.
 */
class StatementParser(private val client: GroqClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class ParsedRow(
        val date: String = "",
        val merchant: String = "",
        val amount: Double = 0.0,
        val type: String = "EXPENSE"
    )

    @Serializable
    private data class ParsedResponse(val transactions: List<ParsedRow> = emptyList())

    sealed interface Result {
        data class Ok(val rows: List<CsvImporter.Row>, val skipped: Int) : Result
        data class Error(val message: String) : Result
    }

    /** Rough guard so a giant paste does not blow the context window. */
    private val maxChars = 12_000

    suspend fun parse(rawText: String): Result {
        val text = rawText.trim()
        if (text.length < 20) {
            return Result.Error("הטקסט קצר מדי. העתיקו את טבלת התנועות מהדף.")
        }

        val truncated = text.length > maxChars
        val payload = if (truncated) text.take(maxChars) else text

        val messages = listOf(
            GroqClient.Message("system", SYSTEM_PROMPT),
            GroqClient.Message("user", payload)
        )

        return when (val r = client.chat(messages, temperature = 0.0, jsonMode = true, maxTokens = 8000)) {
            is GroqClient.Result.Error -> Result.Error(r.message)
            is GroqClient.Result.Ok -> convert(r.text, truncated)
        }
    }

    private fun convert(raw: String, truncated: Boolean): Result {
        val parsed = runCatching {
            json.decodeFromString(ParsedResponse.serializer(), extractJsonObject(raw))
        }.getOrElse {
            return Result.Error("לא הצלחנו לפענח את הטקסט. נסו להעתיק שוב, כולל שורת הכותרות.")
        }

        if (parsed.transactions.isEmpty()) {
            return Result.Error("לא נמצאו עסקאות בטקסט שהודבק.")
        }

        var skipped = 0
        val rows = parsed.transactions.mapNotNull { row ->
            val date = parseIsoDate(row.date)
            val amount = kotlin.math.abs(row.amount)
            // Every field is re-validated here; a hallucinated row without a
            // usable date or a positive amount is dropped rather than stored.
            if (date == null || amount <= 0.0) {
                skipped++
                return@mapNotNull null
            }
            CsvImporter.Row(
                date = date,
                merchant = row.merchant.trim().ifBlank { "עסקה מיובאת" },
                amount = amount,
                type = if (row.type.equals("INCOME", true)) TxType.INCOME else TxType.EXPENSE
            )
        }

        if (rows.isEmpty()) {
            return Result.Error("העסקאות שזוהו לא היו תקינות. נסו להעתיק קטע קצר יותר.")
        }
        return Result.Ok(rows.sortedByDescending { it.date }, skipped)
    }

    /** The model occasionally wraps its JSON in prose or a code fence. */
    private fun extractJsonObject(raw: String): String {
        val cleaned = raw.replace(Regex("```(?:json)?"), "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }

    private fun parseIsoDate(value: String): Long? {
        val v = value.trim()
        if (v.isEmpty()) return null
        return runCatching {
            Dates.toMillis(LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE))
        }.getOrElse {
            // Tolerate the model echoing the statement's own format.
            listOf("dd/MM/yyyy", "dd/MM/yy", "dd.MM.yyyy", "d/M/yyyy").firstNotNullOfOrNull { p ->
                runCatching {
                    Dates.toMillis(LocalDate.parse(v, DateTimeFormatter.ofPattern(p)))
                }.getOrNull()
            }
        }
    }

    private companion object {
        val SYSTEM_PROMPT = """
אתה מחלץ עסקאות מדפי חשבון בנק וכרטיסי אשראי ישראליים.

הקלט הוא טקסט גולמי שהועתק מקובץ PDF או מאתר הבנק. הטבלה עלולה להיות משובשת,
עם עמודות שהתערבבו ועם כיווניות RTL הפוכה. המשימה שלך היא לשחזר ממנו את העסקאות.

החזר אך ורק JSON במבנה הבא, בלי טקסט נוסף:

{"transactions":[{"date":"YYYY-MM-DD","merchant":"שם בית העסק","amount":123.45,"type":"EXPENSE"}]}

כללים:
- date תמיד בפורמט YYYY-MM-DD. אם בדף מופיעה שנה דו־ספרתית כמו 26, פרש אותה כ־2026.
- amount תמיד מספר חיובי, בלי סימן מטבע ובלי פסיקים.
- type הוא "EXPENSE" עבור חובה, חיוב, משיכה, רכישה או תשלום.
- type הוא "INCOME" עבור זכות, הפקדה, משכורת, זיכוי, החזר, ריבית או דיבידנד.
- בדף בנק עם עמודות חובה וזכות, העמודה שבה מופיע הסכום קובעת את הסוג.
- merchant הוא שם בית העסק או תיאור הפעולה בלבד. הסר מספרי אסמכתא, מספרי חשבון
  ומספרי כרטיס.
- דלג על שורות שאינן עסקאות: כותרות, יתרת פתיחה, יתרת סגירה, סיכומים,
  "סה\"כ", מספרי עמוד וכותרות תחתונות.
- אל תמציא עסקאות. אם שורה אינה ברורה, פשוט אל תכלול אותה.
- אם לא נמצאה אף עסקה, החזר {"transactions":[]}.
""".trim()
    }
}
