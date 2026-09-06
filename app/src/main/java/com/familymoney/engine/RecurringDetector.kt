package com.familymoney.engine

import com.familymoney.data.db.RecurringEntity
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.TxType
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * Sections 32-33: spot charges that repeat every month so the forecast and the
 * subscription screen have something to work with even without a bank feed.
 */
object RecurringDetector {

    private const val MIN_OCCURRENCES = 3
    private const val AMOUNT_TOLERANCE = 0.15
    private const val DAY_TOLERANCE = 4

    private val SUBSCRIPTION_HINTS = listOf(
        "netflix", "spotify", "youtube", "apple", "google", "icloud", "disney",
        "hbo", "max", "prime", "dropbox", "office", "microsoft", "adobe",
        "chatgpt", "openai", "claude", "anthropic", "cellcom", "partner", "hot",
        "yes", "פרטנר", "סלקום", "הוט", "נטפליקס", "ספוטיפיי", "מנוי", "חדר כושר",
        "holmes", "gym", "icount", "wix", "canva"
    )

    fun detect(transactions: List<TransactionEntity>, zone: ZoneId = ZoneId.systemDefault()): List<RecurringEntity> {
        val byMerchant = transactions
            .filter { it.merchant.isNotBlank() }
            .groupBy { normalize(it.merchant) }

        return byMerchant.mapNotNull { (key, list) ->
            if (list.size < MIN_OCCURRENCES) return@mapNotNull null

            val months = list.map { yearMonthOf(it.date, zone) }.toSet()
            if (months.size < MIN_OCCURRENCES) return@mapNotNull null

            val amounts = list.map { abs(it.amount) }
            val median = amounts.sorted()[amounts.size / 2]
            if (median <= 0.0) return@mapNotNull null

            val stable = amounts.count { abs(it - median) <= median * AMOUNT_TOLERANCE }
            if (stable < list.size * 0.6) return@mapNotNull null

            val days = list.map { dayOfMonthOf(it.date, zone) }
            val medianDay = days.sorted()[days.size / 2]
            val alignedDays = days.count { abs(it - medianDay) <= DAY_TOLERANCE }
            if (alignedDays < list.size * 0.5) return@mapNotNull null

            val sample = list.first()
            RecurringEntity(
                id = "auto_$key",
                familyId = sample.familyId,
                name = sample.merchant,
                type = sample.type,
                amount = median,
                category = sample.category,
                dayOfMonth = medianDay,
                accountId = sample.accountId,
                cardId = sample.cardId,
                isSubscription = sample.type == TxType.EXPENSE && looksLikeSubscription(sample.merchant, median),
                active = true,
                detected = true
            )
        }.sortedByDescending { it.amount }
    }

    private fun looksLikeSubscription(merchant: String, amount: Double): Boolean {
        val m = merchant.lowercase()
        if (SUBSCRIPTION_HINTS.any { m.contains(it) }) return true
        // Small, perfectly regular charges are almost always subscriptions.
        return amount in 10.0..250.0
    }

    fun normalize(merchant: String): String =
        merchant.lowercase()
            .replace(Regex("[0-9]"), "")
            .replace(Regex("[^\\p{L}\\p{M} ]"), " ")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(40)

    private fun yearMonthOf(epochMillis: Long, zone: ZoneId): String {
        val d = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return "%04d-%02d".format(d.year, d.monthValue)
    }

    private fun dayOfMonthOf(epochMillis: Long, zone: ZoneId): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().dayOfMonth
}
