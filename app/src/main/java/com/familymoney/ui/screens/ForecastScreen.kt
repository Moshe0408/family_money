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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.familymoney.data.model.TxType
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.DisclaimerNote
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.heroBrush
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.MoneyLarge
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money

/** Sections 7, 8 and 50: when to save, and how the month ends. */
@Composable
fun ForecastScreen(vm: MainViewModel, navController: NavHostController) {
    val snapshot by vm.snapshot.collectAsState()
    val forecast by vm.forecast.collectAsState()
    val recurring by vm.recurring.collectAsState()
    val cards by vm.cards.collectAsState()
    val recommendations by vm.recommendations.collectAsState()

    val s = snapshot
    val f = forecast
    val today = Dates.today()

    DetailScaffold("מתי כדאי לחסוך?", navController) {
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
                            "סכום פנוי להעברה לחיסכון",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            Money.format(s?.availableToSave ?: 0.0),
                            style = MoneyLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "מחושב אחרי חיובים צפויים, הוצאות שוטפות וכרית ביטחון של " +
                                Money.format(s?.safetyBuffer ?: 0.0),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Section 8: the month-end table.
            item { SectionHeader("איך ייראה סוף החודש?", emoji = "📅") }
            item {
                SectionCard {
                    f?.lines?.forEach { line ->
                        if (line.isTotal) {
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        AmountRow(
                            label = line.label,
                            value = Money.format(line.amount),
                            emphasize = line.isTotal,
                            valueColor = when {
                                line.isTotal && line.amount < (s?.safetyBuffer ?: 0.0) ->
                                    MaterialTheme.colorScheme.error
                                line.isTotal -> Success
                                line.amount < 0 -> MaterialTheme.colorScheme.error
                                line.amount > 0 -> Success
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "התחזית מתעדכנת אוטומטית מהעסקאות, ההוראות הקבועות וחיובי האשראי.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 7: the arithmetic laid out explicitly.
            item { SectionHeader("איך חישבנו", emoji = "🧮") }
            item {
                SectionCard {
                    AmountRow("היום", Dates.formatDay(Dates.nowMillis()))
                    AmountRow("יתרה נוכחית", Money.format(s?.liquidBalance ?: 0.0))
                    AmountRow("חיובים צפויים", Money.format(s?.upcomingCharges ?: 0.0))
                    AmountRow(
                        "הוצאות ממוצעות עד סוף החודש",
                        Money.format(s?.projectedRemainingSpend ?: 0.0)
                    )
                    AmountRow(
                        "קצב הוצאה יומי",
                        Money.format(s?.averageDailySpend ?: 0.0)
                    )
                    AmountRow("ימים שנותרו בחודש", "${s?.daysLeftInMonth ?: 0}")
                    AmountRow("כרית ביטחון", Money.format(s?.safetyBuffer ?: 0.0))
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.height(6.dp))
                    AmountRow(
                        "סכום פנוי משוער",
                        Money.format(s?.availableToSave ?: 0.0),
                        emphasize = true,
                        valueColor = Success
                    )
                }
            }

            // Section 50: the upcoming flow, in order.
            item { SectionHeader("מה צפוי לקרות", emoji = "🔮") }
            item {
                SectionCard {
                    val upcomingRecurring = recurring
                        .filter { it.active && it.dayOfMonth > today.dayOfMonth }
                        .sortedBy { it.dayOfMonth }
                    val upcomingCards = cards
                        .filter { !it.archived && it.nextChargeAmount > 0 }

                    if (upcomingRecurring.isEmpty() && upcomingCards.isEmpty()) {
                        Text(
                            "אין חיובים ידועים עד סוף החודש.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    upcomingCards.forEach { card ->
                        val daysAway = if (card.nextChargeDate > 0)
                            ((card.nextChargeDate - Dates.nowMillis()) / 86_400_000L).toInt()
                        else null
                        TimelineRow(
                            emoji = "💳",
                            title = "${card.provider} ••${card.last4}",
                            subtitle = if (daysAway != null && daysAway >= 0)
                                "חיוב בעוד $daysAway ימים" else "חיוב קרוב",
                            amount = -card.nextChargeAmount
                        )
                    }

                    upcomingRecurring.forEach { r ->
                        TimelineRow(
                            emoji = if (r.type == TxType.INCOME) "💰" else "🔁",
                            title = r.name,
                            subtitle = "ב־${r.dayOfMonth} בחודש",
                            amount = if (r.type == TxType.INCOME) r.amount else -r.amount
                        )
                    }
                }
            }

            val savingRec = recommendations.firstOrNull {
                it.category == com.familymoney.data.model.RecCategory.SAVING
            }
            if (savingRec != null) {
                item {
                    SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            "המלצה",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            savingRec.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            savingRec.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { navController.navigate(Routes.ACCOUNTS) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(savingRec.actionLabel) }
                    }
                }
            }

            item {
                DisclaimerNote(
                    "התחזית מבוססת על הנתונים שהוזנו ועל קצב ההוצאה הנוכחי. " +
                        "היא אינה מהווה התחייבות, ואינה ייעוץ פיננסי או ייעוץ השקעות."
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(emoji: String, title: String, subtitle: String, amount: Double) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(0.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            Money.signed(amount),
            style = MoneySmall,
            color = if (amount >= 0) Success else MaterialTheme.colorScheme.error
        )
    }
}
