package com.familymoney.data.bank

import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.IncomeCategory
import com.familymoney.data.model.TxSource
import com.familymoney.data.model.TxType
import com.familymoney.util.Dates
import com.familymoney.util.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Imports the CSV / Excel-exported statements the Israeli banks and card issuers
 * hand out. Column names differ per issuer, so headers are matched by keyword
 * rather than by position.
 */
object CsvImporter {

    data class Row(
        val date: Long,
        val merchant: String,
        val amount: Double,
        val type: TxType,
        val note: String = ""
    )

    data class Report(
        val rows: List<Row>,
        val skipped: Int,
        val detectedColumns: Map<String, String>,
        val error: String? = null
    )

    private val DATE_KEYS = listOf(
        "תאריך עסקה", "תאריך העסקה", "תאריך חיוב", "תאריך", "date", "transaction date", "value date"
    )
    private val MERCHANT_KEYS = listOf(
        "שם בית עסק", "בית עסק", "תיאור", "פירוט", "תיאור העסקה", "פעולה",
        "merchant", "description", "details", "name", "narrative"
    )
    private val DEBIT_KEYS = listOf("חובה", "חיוב", "debit", "withdrawal", "משיכה")
    private val CREDIT_KEYS = listOf("זכות", "credit", "deposit", "הפקדה")
    private val AMOUNT_KEYS = listOf(
        "סכום חיוב", "סכום עסקה", "סכום", "amount", "sum", "charge", "total", "value"
    )

    private val DATE_FORMATS = listOf(
        "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy-MM-dd",
        "dd.MM.yyyy", "d.M.yyyy", "MM/dd/yyyy", "dd/MM/yy", "d/M/yy"
    ).map { DateTimeFormatter.ofPattern(it) }

    fun parse(content: String, defaultType: TxType = TxType.EXPENSE): Report {
        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return Report(emptyList(), 0, emptyMap(), "הקובץ ריק.")

        val delimiter = detectDelimiter(lines)
        val headerIndex = lines.indexOfFirst { line ->
            val cells = splitLine(line, delimiter)
            cells.any { c -> DATE_KEYS.any { c.normalized().contains(it) } }
        }
        if (headerIndex < 0) {
            return Report(emptyList(), 0, emptyMap(), "לא נמצאה שורת כותרות עם עמודת תאריך.")
        }

        val headers = splitLine(lines[headerIndex], delimiter).map { it.normalized() }

        val dateCol = findColumn(headers, DATE_KEYS) ?: return Report(
            emptyList(), 0, emptyMap(), "לא זוהתה עמודת תאריך."
        )
        val merchantCol = findColumn(headers, MERCHANT_KEYS)
        val debitCol = findColumn(headers, DEBIT_KEYS)
        val creditCol = findColumn(headers, CREDIT_KEYS)
        val amountCol = findColumn(headers, AMOUNT_KEYS)

        if (amountCol == null && debitCol == null && creditCol == null) {
            return Report(emptyList(), 0, emptyMap(), "לא זוהתה עמודת סכום.")
        }

        val detected = buildMap {
            put("תאריך", headers.getOrElse(dateCol) { "" })
            merchantCol?.let { put("בית עסק", headers.getOrElse(it) { "" }) }
            amountCol?.let { put("סכום", headers.getOrElse(it) { "" }) }
            debitCol?.let { put("חובה", headers.getOrElse(it) { "" }) }
            creditCol?.let { put("זכות", headers.getOrElse(it) { "" }) }
        }

        val rows = mutableListOf<Row>()
        var skipped = 0

        for (i in (headerIndex + 1) until lines.size) {
            val cells = splitLine(lines[i], delimiter)
            if (cells.size <= dateCol) { skipped++; continue }

            val date = parseDate(cells[dateCol])
            if (date == null) { skipped++; continue }

            val debit = debitCol?.let { cells.getOrNull(it)?.let(Money::parse) } ?: 0.0
            val credit = creditCol?.let { cells.getOrNull(it)?.let(Money::parse) } ?: 0.0
            val plain = amountCol?.let { cells.getOrNull(it)?.let(Money::parse) }

            val (amount, type) = when {
                debit > 0.0 -> debit to TxType.EXPENSE
                credit > 0.0 -> credit to TxType.INCOME
                plain != null && plain < 0 -> -plain to TxType.EXPENSE
                plain != null && plain > 0 ->
                    // A statement with only one amount column is a card statement
                    // most of the time, so positive means "charged".
                    plain to defaultType
                else -> { skipped++; continue }
            }
            if (amount <= 0.0) { skipped++; continue }

            val merchant = merchantCol?.let { cells.getOrNull(it) }?.trim().orEmpty()
                .ifBlank { "עסקה מיובאת" }

            rows += Row(date = date, merchant = merchant, amount = amount, type = type)
        }

        return Report(rows, skipped, detected)
    }

    suspend fun toTransactions(
        rows: List<Row>,
        familyId: String,
        createdBy: String,
        accountId: String?,
        cardId: String?,
        categorize: suspend (String) -> ExpenseCategory
    ): List<TransactionEntity> = rows.map { r ->
        val category = if (r.type == TxType.EXPENSE) {
            categorize(r.merchant).name
        } else {
            guessIncomeCategory(r.merchant).name
        }
        TransactionEntity(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            createdByName = createdBy,
            accountId = accountId,
            cardId = cardId,
            type = r.type,
            amount = r.amount,
            merchant = r.merchant,
            category = category,
            date = r.date,
            note = r.note,
            source = TxSource.IMPORT
        )
    }

    private fun guessIncomeCategory(merchant: String): IncomeCategory {
        val m = merchant.lowercase()
        return when {
            m.contains("משכורת") || m.contains("שכר") || m.contains("salary") -> IncomeCategory.SALARY
            m.contains("ריבית") || m.contains("interest") -> IncomeCategory.INTEREST
            m.contains("דיבידנד") || m.contains("dividend") -> IncomeCategory.DIVIDEND
            m.contains("זיכוי") || m.contains("החזר") || m.contains("refund") -> IncomeCategory.REFUND
            m.contains("בונוס") || m.contains("מענק") -> IncomeCategory.BONUS
            else -> IncomeCategory.OTHER
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun detectDelimiter(lines: List<String>): Char {
        val sample = lines.take(10).joinToString("\n")
        val counts = mapOf(
            ',' to sample.count { it == ',' },
            '\t' to sample.count { it == '\t' },
            ';' to sample.count { it == ';' }
        )
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    /** Minimal RFC-4180 handling: quoted cells may contain the delimiter. */
    private fun splitLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> { out += sb.toString(); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out.map { it.trim().removeSurrounding("\"").trim() }
    }

    private fun findColumn(headers: List<String>, keys: List<String>): Int? {
        keys.forEach { key ->
            val exact = headers.indexOfFirst { it == key }
            if (exact >= 0) return exact
        }
        keys.forEach { key ->
            val partial = headers.indexOfFirst { it.contains(key) }
            if (partial >= 0) return partial
        }
        return null
    }

    private fun parseDate(raw: String): Long? {
        val cleaned = raw.trim().replace("‏", "").replace("‎", "")
        if (cleaned.isEmpty()) return null
        DATE_FORMATS.forEach { fmt ->
            runCatching { return Dates.toMillis(LocalDate.parse(cleaned, fmt)) }
        }
        // Excel serial dates (days since 1899-12-30) show up in some exports.
        cleaned.toDoubleOrNull()?.let { serial ->
            if (serial in 20000.0..60000.0) {
                return Dates.toMillis(LocalDate.of(1899, 12, 30).plusDays(serial.toLong()))
            }
        }
        return null
    }

    private fun String.normalized(): String =
        trim().lowercase()
            .replace("‏", "")
            .replace("‎", "")
            .replace("\"", "")
            .replace(Regex("\\s+"), " ")
}
