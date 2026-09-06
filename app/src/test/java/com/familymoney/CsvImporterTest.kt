package com.familymoney

import com.familymoney.data.bank.CsvImporter
import com.familymoney.data.model.TxType
import com.familymoney.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImporterTest {

    @Test
    fun `parses an Israeli card statement`() {
        val csv = """
            תאריך עסקה,שם בית עסק,סכום חיוב
            05/09/2026,שופרסל דיל,342.50
            07/09/2026,Wolt,89.00
            10/09/2026,פז,280.00
        """.trimIndent()

        val report = CsvImporter.parse(csv, TxType.EXPENSE)
        assertNull(report.error)
        assertEquals(3, report.rows.size)
        assertEquals(342.5, report.rows[0].amount, 0.01)
        assertEquals("שופרסל דיל", report.rows[0].merchant)
        assertTrue(report.rows.all { it.type == TxType.EXPENSE })
    }

    @Test
    fun `debit and credit columns set direction`() {
        val csv = """
            תאריך,תיאור,חובה,זכות
            01/09/2026,משכורת,,12400.00
            02/09/2026,שכר דירה,5200.00,
            05/09/2026,ריבית,,190.00
        """.trimIndent()

        val report = CsvImporter.parse(csv)
        assertEquals(3, report.rows.size)
        assertEquals(TxType.INCOME, report.rows[0].type)
        assertEquals(12_400.0, report.rows[0].amount, 0.01)
        assertEquals(TxType.EXPENSE, report.rows[1].type)
        assertEquals(5_200.0, report.rows[1].amount, 0.01)
        assertEquals(TxType.INCOME, report.rows[2].type)
    }

    @Test
    fun `negative amounts become expenses`() {
        val csv = """
            date,description,amount
            2026-09-05,Grocery,-342.50
            2026-09-06,Refund,120.00
        """.trimIndent()

        val report = CsvImporter.parse(csv, TxType.INCOME)
        assertEquals(TxType.EXPENSE, report.rows[0].type)
        assertEquals(342.5, report.rows[0].amount, 0.01)
        assertEquals(TxType.INCOME, report.rows[1].type)
    }

    @Test
    fun `handles quoted fields containing the delimiter`() {
        val csv = """
            תאריך,שם בית עסק,סכום
            05/09/2026,"רמי לוי, סניף מרכז",342.50
        """.trimIndent()

        val report = CsvImporter.parse(csv)
        assertEquals(1, report.rows.size)
        assertEquals("רמי לוי, סניף מרכז", report.rows[0].merchant)
        assertEquals(342.5, report.rows[0].amount, 0.01)
    }

    @Test
    fun `accepts semicolon and tab delimiters`() {
        val semi = "תאריך;בית עסק;סכום\n05/09/2026;שופרסל;342.50"
        assertEquals(1, CsvImporter.parse(semi).rows.size)

        val tab = "תאריך\tבית עסק\tסכום\n05/09/2026\tשופרסל\t342.50"
        assertEquals(1, CsvImporter.parse(tab).rows.size)
    }

    @Test
    fun `skips preamble rows before the header`() {
        val csv = """
            דוח תנועות בחשבון
            מספר חשבון: 12-345-678

            תאריך,תיאור,חובה,זכות
            01/09/2026,משכורת,,12400.00
        """.trimIndent()

        val report = CsvImporter.parse(csv)
        assertNull(report.error)
        assertEquals(1, report.rows.size)
    }

    @Test
    fun `counts unparseable rows as skipped`() {
        val csv = """
            תאריך,בית עסק,סכום
            05/09/2026,שופרסל,342.50
            not-a-date,גיבריש,100.00
            07/09/2026,ללא סכום,
        """.trimIndent()

        val report = CsvImporter.parse(csv)
        assertEquals(1, report.rows.size)
        assertEquals(2, report.skipped)
    }

    @Test
    fun `reports an error when no date column exists`() {
        val report = CsvImporter.parse("בית עסק,סכום\nשופרסל,342.50")
        assertNotNull(report.error)
        assertTrue(report.rows.isEmpty())
    }

    @Test
    fun `reports an error on an empty file`() {
        assertNotNull(CsvImporter.parse("").error)
    }

    @Test
    fun `parses several date formats`() {
        val csv = """
            תאריך,בית עסק,סכום
            05/09/2026,א,100
            2026-09-06,ב,100
            07.09.2026,ג,100
            08-09-2026,ד,100
        """.trimIndent()

        val report = CsvImporter.parse(csv)
        assertEquals(4, report.rows.size)
        assertTrue("dates should be ordered", report.rows[0].date < report.rows[3].date)
    }

    @Test
    fun `reports which columns it matched`() {
        val csv = "תאריך עסקה,שם בית עסק,סכום חיוב\n05/09/2026,שופרסל,342.50"
        val report = CsvImporter.parse(csv)
        assertEquals("תאריך עסקה", report.detectedColumns["תאריך"])
        assertEquals("שם בית עסק", report.detectedColumns["בית עסק"])
    }

    // ------------------------------------------------------------- Money.parse

    @Test
    fun `money parser tolerates shekel signs and separators`() {
        assertEquals(1_234.5, Money.parse("₪1,234.50")!!, 0.01)
        assertEquals(1_234.5, Money.parse("1234.5")!!, 0.01)
        assertEquals(-90.0, Money.parse("-90")!!, 0.01)
        assertEquals(90.0, Money.parse(" 90 ILS ")!!, 0.01)
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun `money formatting groups thousands`() {
        assertEquals("₪186,420", Money.format(186_420.0))
        assertEquals("₪0", Money.format(0.0))
        assertEquals("₪-1,500", Money.format(-1_500.0))
        assertEquals("₪1,234.50", Money.format(1_234.5, decimals = true))
    }

    @Test
    fun `compact formatting shortens large numbers`() {
        assertEquals("₪186.4K", Money.compact(186_420.0))
        assertEquals("₪1.5M", Money.compact(1_500_000.0))
        assertEquals("₪420", Money.compact(420.0))
    }
}
