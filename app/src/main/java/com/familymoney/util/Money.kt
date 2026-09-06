package com.familymoney.util

import kotlin.math.abs
import kotlin.math.round

object Money {
    const val SHEKEL = "₪"

    fun format(value: Double, withSymbol: Boolean = true, decimals: Boolean = false): String {
        val body = if (decimals) formatWithDecimals(value) else grouped(round(value).toLong())
        return if (withSymbol) "$SHEKEL$body" else body
    }

    fun signed(value: Double): String {
        val sign = if (value >= 0) "+" else "−"
        return "$sign$SHEKEL${grouped(round(abs(value)).toLong())}"
    }

    /** ₪186,420 -> ₪186.4K, for tight spots like chart axes. */
    fun compact(value: Double): String {
        val a = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            a >= 1_000_000 -> "$sign$SHEKEL${oneDecimal(a / 1_000_000)}M"
            a >= 1_000 -> "$sign$SHEKEL${oneDecimal(a / 1_000)}K"
            else -> "$sign$SHEKEL${round(a).toLong()}"
        }
    }

    fun percent(value: Double, signed: Boolean = false): String {
        val p = oneDecimal(abs(value))
        val prefix = when {
            !signed -> ""
            value >= 0 -> "+"
            else -> "−"
        }
        return "$prefix$p%"
    }

    private fun formatWithDecimals(value: Double): String {
        val neg = value < 0
        val cents = round(abs(value) * 100).toLong()
        val whole = cents / 100
        val frac = cents % 100
        return (if (neg) "-" else "") + grouped(whole) + "." + frac.toString().padStart(2, '0')
    }

    private fun oneDecimal(v: Double): String {
        val r = round(v * 10) / 10.0
        return if (r == kotlin.math.floor(r)) r.toLong().toString() else r.toString()
    }

    fun grouped(value: Long): String {
        val neg = value < 0
        val digits = abs(value).toString()
        val sb = StringBuilder()
        for ((i, c) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
            sb.append(c)
        }
        return (if (neg) "-" else "") + sb
    }

    /** Tolerant parser for user input and CSV cells: "1,234.50", "₪90", "-90". */
    fun parse(text: String): Double? {
        val cleaned = text.trim()
            .replace(SHEKEL, "")
            .replace("ILS", "", ignoreCase = true)
            .replace("NIS", "", ignoreCase = true)
            .replace(",", "")
            .replace("‏", "")
            .replace("‎", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        return cleaned.toDoubleOrNull()
    }
}
