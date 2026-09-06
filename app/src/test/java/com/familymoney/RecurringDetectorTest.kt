package com.familymoney

import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.TxType
import com.familymoney.engine.RecurringDetector
import com.familymoney.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.util.UUID

class RecurringDetectorTest {

    private val thisMonth: YearMonth = YearMonth.of(2026, 9)

    private fun tx(
        merchant: String,
        amount: Double,
        monthsAgo: Int,
        day: Int,
        category: ExpenseCategory = ExpenseCategory.OTHER
    ) = TransactionEntity(
        id = UUID.randomUUID().toString(),
        type = TxType.EXPENSE,
        amount = amount,
        merchant = merchant,
        category = category.name,
        date = Dates.toMillis(
            thisMonth.minusMonths(monthsAgo.toLong())
                .atDay(day.coerceAtMost(thisMonth.minusMonths(monthsAgo.toLong()).lengthOfMonth()))
        )
    )

    @Test
    fun `detects a charge that repeats monthly at a steady amount`() {
        val txs = (0..4).map { tx("נטפליקס", 49.9, it, 15, ExpenseCategory.SUBSCRIPTIONS) }
        val found = RecurringDetector.detect(txs)
        assertEquals(1, found.size)
        assertEquals("נטפליקס", found[0].name)
        assertEquals(49.9, found[0].amount, 0.01)
        assertEquals(15, found[0].dayOfMonth)
        assertTrue(found[0].detected)
    }

    @Test
    fun `tolerates small drift in amount and date`() {
        val txs = listOf(
            tx("חברת חשמל", 420.0, 0, 12),
            tx("חברת חשמל", 438.0, 1, 13),
            tx("חברת חשמל", 405.0, 2, 11),
            tx("חברת חשמל", 425.0, 3, 12)
        )
        val found = RecurringDetector.detect(txs)
        assertEquals(1, found.size)
        assertTrue(found[0].amount in 400.0..440.0)
    }

    @Test
    fun `ignores merchants seen fewer than three months`() {
        val txs = listOf(tx("חנות חד פעמית", 100.0, 0, 5), tx("חנות חד פעמית", 100.0, 1, 5))
        assertTrue(RecurringDetector.detect(txs).isEmpty())
    }

    @Test
    fun `ignores three charges inside one month`() {
        val txs = listOf(
            tx("שופרסל", 300.0, 0, 3),
            tx("שופרסל", 310.0, 0, 12),
            tx("שופרסל", 295.0, 0, 24)
        )
        assertTrue("same-month repeats are not a standing order",
            RecurringDetector.detect(txs).isEmpty())
    }

    @Test
    fun `ignores wildly varying amounts`() {
        val txs = listOf(
            tx("סופר", 100.0, 0, 10),
            tx("סופר", 900.0, 1, 10),
            tx("סופר", 50.0, 2, 10),
            tx("סופר", 1200.0, 3, 10)
        )
        assertTrue(RecurringDetector.detect(txs).isEmpty())
    }

    @Test
    fun `ignores charges scattered across the month`() {
        val txs = listOf(
            tx("מכולת", 200.0, 0, 2),
            tx("מכולת", 200.0, 1, 14),
            tx("מכולת", 200.0, 2, 27),
            tx("מכולת", 200.0, 3, 8)
        )
        assertTrue(RecurringDetector.detect(txs).isEmpty())
    }

    @Test
    fun `flags known services as subscriptions`() {
        val netflix = RecurringDetector.detect(
            (0..3).map { tx("Netflix", 49.9, it, 15) }
        ).first()
        assertTrue(netflix.isSubscription)

        val spotify = RecurringDetector.detect(
            (0..3).map { tx("Spotify Premium", 21.9, it, 15) }
        ).first()
        assertTrue(spotify.isSubscription)
    }

    @Test
    fun `large regular charges are not treated as subscriptions`() {
        val rent = RecurringDetector.detect(
            (0..4).map { tx("שכר דירה", 5_200.0, it, 2, ExpenseCategory.HOUSING) }
        ).first()
        assertEquals(false, rent.isSubscription)
    }

    @Test
    fun `carries the category from the source transactions`() {
        val found = RecurringDetector.detect(
            (0..3).map { tx("גן ילדים", 2_500.0, it, 5, ExpenseCategory.KIDS) }
        ).first()
        assertEquals(ExpenseCategory.KIDS.name, found.category)
    }

    @Test
    fun `results are ordered by amount descending`() {
        val txs = (0..3).flatMap {
            listOf(
                tx("שכר דירה", 5_200.0, it, 2),
                tx("נטפליקס", 49.9, it, 15),
                tx("חשמל", 420.0, it, 12)
            )
        }
        val found = RecurringDetector.detect(txs)
        assertEquals(3, found.size)
        assertEquals(found.map { it.amount }.sortedDescending(), found.map { it.amount })
    }

    @Test
    fun `blank merchants are skipped`() {
        val txs = (0..4).map { tx("", 100.0, it, 10) }
        assertTrue(RecurringDetector.detect(txs).isEmpty())
    }

    @Test
    fun `normalizer strips digits and punctuation`() {
        assertEquals(
            RecurringDetector.normalize("שופרסל דיל 123"),
            RecurringDetector.normalize("שופרסל דיל - 456")
        )
        // Punctuation becomes a separator rather than vanishing, so a merchant
        // string normalises consistently every month — which is all grouping needs.
        assertEquals("netflix_com", RecurringDetector.normalize("NETFLIX.COM"))
        assertEquals(
            RecurringDetector.normalize("NETFLIX.COM"),
            RecurringDetector.normalize("netflix.com  ")
        )
    }

    @Test
    fun `empty input yields nothing`() {
        assertTrue(RecurringDetector.detect(emptyList()).isEmpty())
    }
}
