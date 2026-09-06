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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.model.AccountType
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.CompositionBar
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.LineChart
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.components.StatTile
import com.familymoney.ui.heroBrush
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneyLarge
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import java.time.YearMonth

/** Sections 34-36: "how much do we have?" */
@Composable
fun NetWorthScreen(vm: MainViewModel, navController: NavHostController) {
    val snapshot by vm.snapshot.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val investments by vm.investments.collectAsState()
    val children by vm.children.collectAsState()
    val liabilities by vm.liabilities.collectAsState()

    val s = snapshot

    DetailScaffold("💰 הכסף שלנו", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(heroBrush(), RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        Text(
                            "שווי פיננסי כולל",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            Money.format(s?.netWorth ?: 0.0),
                            style = MoneyLarge,
                            color = Color.White
                        )
                        if (snapshots.size >= 2) {
                            val last = snapshots.last().total
                            val prev = snapshots[snapshots.size - 2].total
                            val delta = last - prev
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${if (delta >= 0) "▲" else "▼"} ${Money.signed(delta)} מהחודש הקודם",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (snapshots.size >= 2) {
                item { SectionHeader("מגמה", emoji = "📈") }
                item {
                    SectionCard {
                        LineChart(
                            values = snapshots.map { it.total }.takeLast(12).reversed(),
                            labels = snapshots.takeLast(12).reversed().map { snap ->
                                val parts = snap.yearMonth.split("-")
                                runCatching {
                                    Dates.hebrewMonth(
                                        YearMonth.of(parts[0].toInt(), parts[1].toInt())
                                    ).take(3)
                                }.getOrDefault("")
                            }
                        )
                    }
                }
            }

            item { SectionHeader("מאיפה מגיע השווי", emoji = "🧩") }
            item {
                val segments = listOf(
                    "נזיל" to (s?.liquidBalance ?: 0.0),
                    "חיסכון" to (s?.savingsBalance ?: 0.0),
                    "השקעות" to (s?.investmentValue ?: 0.0),
                    "ילדים" to (s?.childrenValue ?: 0.0)
                ).filter { it.second > 0 }

                SectionCard {
                    CompositionBar(segments)
                    Spacer(Modifier.height(16.dp))
                    segments.forEachIndexed { i, (label, value) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(10.dp).background(
                                    CategoryPalette[i % CategoryPalette.size], CircleShape
                                )
                            )
                            Spacer(Modifier.height(0.dp))
                            Text(
                                label,
                                Modifier.weight(1f).padding(start = 10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(Money.format(value), style = MoneySmall)
                        }
                    }
                    if ((s?.liabilities ?: 0.0) > 0) {
                        Spacer(Modifier.height(8.dp))
                        AmountRow(
                            "− התחייבויות",
                            Money.format(s?.liabilities ?: 0.0),
                            MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "נזיל", Money.format(s?.liquidBalance ?: 0.0), "🏦",
                        MaterialTheme.colorScheme.primary, Modifier.weight(1f)
                    ) { navController.navigate(Routes.ACCOUNTS) }
                    StatTile(
                        "חיסכון", Money.format(s?.savingsBalance ?: 0.0), "🐷",
                        Success, Modifier.weight(1f)
                    ) { navController.navigate(Routes.ACCOUNTS) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "השקעות", Money.format(s?.investmentValue ?: 0.0), "📈",
                        MaterialTheme.colorScheme.secondary, Modifier.weight(1f)
                    ) { navController.navigate(Routes.INVESTMENTS) }
                    StatTile(
                        "ילדים", Money.format(s?.childrenValue ?: 0.0), "👨‍👩‍👧",
                        Warning, Modifier.weight(1f)
                    ) { navController.navigate(Routes.CHILDREN) }
                }
            }

            if (accounts.isNotEmpty()) {
                item { SectionHeader("חשבונות", emoji = "🏦", actionLabel = "נהל") {
                    navController.navigate(Routes.ACCOUNTS)
                } }
                item {
                    SectionCard {
                        accounts.forEach { a ->
                            AmountRow(
                                "${a.name}${if (a.provider.isNotBlank()) " · ${a.provider}" else ""}",
                                Money.format(a.balance)
                            )
                        }
                    }
                }
            }

            if (investments.isNotEmpty()) {
                item { SectionHeader("החזקות", emoji = "📊") }
                item {
                    SectionCard {
                        investments.filter { it.childId == null }.forEach { inv ->
                            AmountRow(inv.name, Money.format(inv.currentValue))
                        }
                        children.forEach { child ->
                            val value = investments
                                .filter { it.childId == child.id }
                                .sumOf { it.currentValue }
                            if (value > 0) {
                                AmountRow("${child.emoji} ${child.name}", Money.format(value))
                            }
                        }
                    }
                }
            }

            if (liabilities.isNotEmpty()) {
                item { SectionHeader("התחייבויות", emoji = "🧾", actionLabel = "נהל") {
                    navController.navigate(Routes.LIABILITIES)
                } }
                item {
                    SectionCard {
                        liabilities.forEach { l ->
                            AmountRow(
                                l.name,
                                Money.format(l.remainingAmount),
                                MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        AmountRow(
                            "תשלום חודשי",
                            Money.format(liabilities.sumOf { it.monthlyPayment }),
                            emphasize = true
                        )
                    }
                }
            }
        }
    }
}
