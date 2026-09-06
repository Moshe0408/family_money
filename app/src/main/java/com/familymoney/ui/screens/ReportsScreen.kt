package com.familymoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.IncomeCategory
import com.familymoney.data.model.TxType
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.BarPoint
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.DonutChart
import com.familymoney.ui.components.DonutLegend
import com.familymoney.ui.components.DonutSlice
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.GaugeArc
import com.familymoney.ui.components.GroupedBarChart
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import java.time.YearMonth

private enum class ReportRange(val label: String, val months: Int) {
    MONTH("החודש", 1), QUARTER("3 חודשים", 3), HALF("חצי שנה", 6), YEAR("שנה", 12)
}

/** Section 26: monthly and yearly reports. */
@Composable
fun ReportsScreen(vm: MainViewModel, navController: NavHostController) {
    val transactions by vm.transactions.collectAsState()
    val monthlyTotals by vm.monthlyTotals.collectAsState()
    var range by remember { mutableStateOf(ReportRange.MONTH) }

    val now = YearMonth.now()
    val from = Dates.monthStart(now.minusMonths((range.months - 1).toLong()))
    val to = Dates.monthEnd(now)

    val inRange = transactions.filter { it.date in from..to }
    val income = inRange.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = inRange.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val saving = income - expense
    val savingRate = if (income <= 0) 0.0 else saving / income * 100

    val byCategory = inRange
        .filter { it.type == TxType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }

    val byIncomeCategory = inRange
        .filter { it.type == TxType.INCOME }
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }

    DetailScaffold("דוחות", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportRange.entries.forEach { r ->
                        Pill(
                            text = r.label,
                            color = MaterialTheme.colorScheme.primary,
                            selected = range == r,
                            onClick = { range = r }
                        )
                    }
                }
            }

            if (inRange.isEmpty()) {
                item {
                    EmptyState("📄", "אין נתונים לתקופה זו", "הוסיפו עסקאות או בחרו טווח אחר.")
                }
                return@LazyColumn
            }

            item {
                SectionCard {
                    Text(
                        if (range == ReportRange.MONTH) "סיכום ${Dates.hebrewMonth(now)}"
                        else "סיכום ${range.label}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(14.dp))
                    AmountRow("הכנסות", Money.format(income), Success)
                    AmountRow("הוצאות", Money.format(expense), MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.height(4.dp))
                    AmountRow(
                        "חיסכון",
                        Money.format(saving),
                        if (saving >= 0) Success else MaterialTheme.colorScheme.error,
                        emphasize = true
                    )
                }
            }

            item {
                SectionCard {
                    Text("שיעור חיסכון", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            GaugeArc(
                                percent = savingRate,
                                modifier = Modifier.size(width = 200.dp, height = 100.dp),
                                color = if (savingRate >= 20) Success
                                else com.familymoney.ui.theme.Warning
                            )
                            Text(
                                Money.percent(savingRate),
                                style = MoneyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            savingRate >= 25 -> "מצוין — אתם חוסכים מעל הרף המקובל של 20%."
                            savingRate >= 10 -> "סביר. יעד מקובל הוא 20% מההכנסה."
                            savingRate > 0 -> "נמוך. שווה לבדוק את הקטגוריות הגדולות."
                            else -> "ההוצאות עלו על ההכנסות בתקופה זו."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (monthlyTotals.size >= 2) {
                item { SectionHeader("הכנסות מול הוצאות", emoji = "📊") }
                item {
                    SectionCard {
                        val points = monthlyTotals.takeLast(8).map { m ->
                            val parts = m.yearMonth.split("-")
                            val label = runCatching {
                                Dates.hebrewMonth(
                                    YearMonth.of(parts[0].toInt(), parts[1].toInt())
                                ).take(3)
                            }.getOrDefault(m.yearMonth.takeLast(2))
                            BarPoint(label, m.income, m.expense)
                        }
                        GroupedBarChart(points.reversed())
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot("הכנסות", Success)
                            LegendDot("הוצאות", MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (byCategory.isNotEmpty()) {
                item { SectionHeader("הוצאות לפי קטגוריה", emoji = "🍰") }
                item {
                    val slices = byCategory.take(10).mapIndexed { i, e ->
                        val cat = ExpenseCategory.fromKey(e.key)
                        DonutSlice(cat.he, e.value, CategoryPalette[i % 16], cat.emoji)
                    }
                    SectionCard {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DonutChart(slices, "הוצאות", Money.format(expense))
                        }
                        Spacer(Modifier.height(16.dp))
                        DonutLegend(slices, maxItems = 10)
                    }
                }
            }

            if (byIncomeCategory.isNotEmpty()) {
                item { SectionHeader("מקורות הכנסה", emoji = "💰") }
                item {
                    SectionCard {
                        byIncomeCategory.forEach { e ->
                            val cat = IncomeCategory.fromKey(e.key)
                            AmountRow("${cat.emoji} ${cat.he}", Money.format(e.value), Success)
                        }
                    }
                }
            }

            item { SectionHeader("בתי העסק הגדולים", emoji = "🏪") }
            item {
                SectionCard {
                    inRange.filter { it.type == TxType.EXPENSE && it.merchant.isNotBlank() }
                        .groupBy { it.merchant }
                        .mapValues { (_, v) -> v.sumOf { it.amount } }
                        .entries.sortedByDescending { it.value }
                        .take(8)
                        .forEach { (merchant, total) ->
                            AmountRow(merchant, Money.format(total))
                        }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.padding(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
