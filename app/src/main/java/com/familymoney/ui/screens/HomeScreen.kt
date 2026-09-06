package com.familymoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.model.AccountType
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.GoalStatus
import com.familymoney.data.model.RecCategory
import com.familymoney.data.model.TxType
import com.familymoney.engine.Recommendation
import com.familymoney.ui.components.CategoryAvatar
import com.familymoney.ui.components.DonutChart
import com.familymoney.ui.components.LineChart
import com.familymoney.ui.components.DonutSlice
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.components.StatTile
import com.familymoney.ui.heroBrush
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.LocalPalette
import com.familymoney.ui.theme.MoneyLarge
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import java.time.YearMonth

@Composable
fun HomeScreen(vm: MainViewModel, navController: NavHostController) {
    val snapshot by vm.snapshot.collectAsState()
    val recommendations by vm.recommendations.collectAsState()
    val goals by vm.goals.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val snapshots by vm.snapshots.collectAsState()
    val name by vm.displayName.collectAsState()
    val family by vm.familyLabel.collectAsState()

    val s = snapshot

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeHeader(
                name = name,
                family = family,
                onNotifications = { navController.navigate(Routes.INSIGHTS) }
            )
        }

        // Section 4: the headline financial position.
        item {
            HeroCard(
                netWorth = s?.netWorth ?: 0.0,
                changePercent = monthChangePercent(snapshots),
                income = s?.monthIncome ?: 0.0,
                expense = s?.monthExpense ?: 0.0,
                saving = s?.monthSaving ?: 0.0,
                trend = snapshots.takeLast(6).map { it.total }.reversed(),
                trendLabels = snapshots.takeLast(6).reversed().map { snap ->
                    val parts = snap.yearMonth.split("-")
                    runCatching {
                        Dates.hebrewMonth(YearMonth.of(parts[0].toInt(), parts[1].toInt())).take(3)
                    }.getOrDefault("")
                },
                onClick = { navController.navigate(Routes.NET_WORTH) }
            )
        }

        // Section 5: "what should we do now?"
        if (recommendations.isNotEmpty()) {
            item {
                SmartRecommendationCard(
                    top = recommendations.first(),
                    onDetails = { navController.navigate(Routes.FORECAST) },
                    onAsk = { navController.navigate(Routes.AI) }
                )
            }
        }

        // Section 29: the recommendation feed.
        if (recommendations.size > 1) {
            item {
                SectionHeader(
                    "המלצות בשבילכם",
                    emoji = "💡",
                    actionLabel = "הכל",
                    onAction = { navController.navigate(Routes.INSIGHTS) }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recommendations.drop(1).take(6)) { rec ->
                        RecommendationChipCard(rec) {
                            navController.navigate(routeForAction(rec.suggestedAction))
                        }
                    }
                }
            }
        }

        // Section 4: money by place.
        item {
            SectionHeader(
                "הכסף שלנו",
                emoji = "💰",
                actionLabel = "פירוט",
                onAction = { navController.navigate(Routes.NET_WORTH) }
            )
        }
        item {
            MoneyByPlaceGrid(
                liquid = s?.liquidBalance ?: 0.0,
                savings = s?.savingsBalance ?: 0.0,
                investments = s?.investmentValue ?: 0.0,
                children = s?.childrenValue ?: 0.0,
                onAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onInvestments = { navController.navigate(Routes.INVESTMENTS) },
                onChildren = { navController.navigate(Routes.CHILDREN) }
            )
        }

        // Section 26: where the money went this month.
        val categorySpend = s?.categorySpendThisMonth.orEmpty()
        if (categorySpend.isNotEmpty()) {
            item {
                SectionHeader(
                    "הוצאות ${Dates.hebrewMonth(YearMonth.now())}",
                    emoji = "📊",
                    actionLabel = "דוחות",
                    onAction = { navController.navigate(Routes.REPORTS) }
                )
            }
            item { SpendingDonutCard(categorySpend, s?.monthExpense ?: 0.0) }
        }

        // Section 20: goals.
        val activeGoals = goals.filter { it.status == GoalStatus.ACTIVE }
        if (activeGoals.isNotEmpty()) {
            item {
                SectionHeader(
                    "יעדים",
                    emoji = "🎯",
                    actionLabel = "הכל",
                    onAction = { navController.navigate(Routes.GOALS) }
                )
            }
            item {
                SectionCard {
                    activeGoals.take(3).forEachIndexed { i, g ->
                        if (i > 0) Spacer(Modifier.height(16.dp))
                        GoalMiniRow(
                            emoji = g.emoji,
                            name = g.name,
                            current = g.currentAmount,
                            target = g.targetAmount
                        )
                    }
                }
            }
        }

        // Section 21-22: investments summary.
        item {
            SectionHeader(
                "השקעות",
                emoji = "📈",
                actionLabel = "פירוט",
                onAction = { navController.navigate(Routes.INVESTMENTS) }
            )
        }
        item {
            InvestmentSummaryCard(
                mine = s?.investmentValue ?: 0.0,
                children = s?.childrenValue ?: 0.0,
                onMine = { navController.navigate(Routes.INVESTMENTS) },
                onChildren = { navController.navigate(Routes.CHILDREN) }
            )
        }

        // Recent activity.
        if (transactions.isNotEmpty()) {
            item {
                SectionHeader(
                    "פעילות אחרונה",
                    emoji = "🧾",
                    actionLabel = "הכל",
                    onAction = { navController.navigate(Routes.TRANSACTIONS) }
                )
            }
            item {
                SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    transactions.take(5).forEach { tx ->
                        TransactionRow(
                            merchant = tx.merchant,
                            categoryKey = tx.category,
                            isIncome = tx.type == TxType.INCOME,
                            amount = tx.amount,
                            date = tx.date,
                            onClick = { navController.navigate(Routes.TRANSACTIONS) }
                        )
                    }
                }
            }
        }

        item {
            AskAiBanner(onClick = { navController.navigate(Routes.AI) })
        }

        if (accounts.isEmpty()) {
            item {
                SectionCard {
                    Text("עוד לא חיברתם חשבונות", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "הוסיפו חשבון בנק, כרטיס אשראי או ייבאו דף חשבון כדי לקבל תמונה מלאה.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { navController.navigate(Routes.ACCOUNTS) },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("הוספת חשבון") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- components

@Composable
private fun HomeHeader(name: String, family: String, onNotifications: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "שלום $name 👋",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (family.isNotBlank()) {
                Text(
                    family,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(44.dp).clickable(onClick = onNotifications)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "התראות",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    netWorth: Double,
    changePercent: Double?,
    income: Double,
    expense: Double,
    saving: Double,
    trend: List<Double>,
    trendLabels: List<String>,
    onClick: () -> Unit
) {
    val p = LocalPalette.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, p.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "סך הכל יתרה",
                style = MaterialTheme.typography.bodyMedium,
                color = p.textMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(Money.format(netWorth), style = MoneyLarge, color = p.text)

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // The chart carries the trend; the pills carry the detail.
                Box(Modifier.weight(1f)) {
                    if (trend.size >= 2) {
                        LineChart(
                            values = trend,
                            labels = trendLabels,
                            lineColor = p.accent,
                            height = 96.dp,
                            showDots = false
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    TrendPill(
                        label = "הכנסות",
                        value = Money.format(income),
                        delta = changePercent,
                        color = p.positive
                    )
                    Spacer(Modifier.height(10.dp))
                    TrendPill(
                        label = "הוצאות",
                        value = Money.format(expense),
                        delta = null,
                        color = p.negative
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.outlineSoft))
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "חיסכון החודש",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.textMuted
                )
                Text(
                    Money.signed(saving),
                    style = MoneySmall,
                    color = if (saving >= 0) p.positive else p.negative
                )
            }
        }
    }
}

/** Vertical accent bar plus a value — the compact readout in the hero card. */
@Composable
private fun TrendPill(
    label: String,
    value: String,
    delta: Double?,
    color: androidx.compose.ui.graphics.Color
) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = p.textMuted)
            Text(value, style = MoneySmall, color = p.text)
            if (delta != null) {
                Text(
                    Money.percent(delta, signed = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (delta >= 0) p.positive else p.negative
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .width(5.dp)
                .height(if (delta != null) 44.dp else 34.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

@Composable
private fun SmartRecommendationCard(
    top: Recommendation,
    onDetails: () -> Unit,
    onAsk: () -> Unit
) {
    SectionCard(containerColor = MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🧠", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Text(
                "מה כדאי לעשות עכשיו?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            top.title,
            style = MaterialTheme.typography.headlineSmall,
            color = colorForRecommendation(top.category)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            top.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onDetails,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) { Text("הצג פירוט") }
            Button(
                onClick = onAsk,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("שאל AI")
            }
        }
    }
}

@Composable
private fun RecommendationChipCard(rec: Recommendation, onClick: () -> Unit) {
    val accent = colorForRecommendation(rec.category)
    Card(
        modifier = Modifier.width(250.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${rec.category.emoji} ${rec.category.he}", style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(Modifier.height(8.dp))
            Text(
                rec.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                rec.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Spacer(Modifier.height(12.dp))
            Text(rec.actionLabel, style = MaterialTheme.typography.labelMedium, color = accent)
        }
    }
}

@Composable
private fun MoneyByPlaceGrid(
    liquid: Double,
    savings: Double,
    investments: Double,
    children: Double,
    onAccounts: () -> Unit,
    onInvestments: () -> Unit,
    onChildren: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("עו\"ש", Money.format(liquid), "🏦", MaterialTheme.colorScheme.primary,
                Modifier.weight(1f), onAccounts)
            StatTile("חיסכון", Money.format(savings), "🐷", Success,
                Modifier.weight(1f), onAccounts)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("השקעות", Money.format(investments), "📈",
                MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onInvestments)
            StatTile("ילדים", Money.format(children), "👨‍👩‍👧", Warning,
                Modifier.weight(1f), onChildren)
        }
    }
}

@Composable
private fun SpendingDonutCard(categorySpend: Map<String, Double>, total: Double) {
    val slices = categorySpend.entries
        .sortedByDescending { it.value }
        .take(8)
        .mapIndexed { i, (key, value) ->
            val cat = ExpenseCategory.fromKey(key)
            DonutSlice(cat.he, value, CategoryPalette[i % CategoryPalette.size], cat.emoji)
        }
    SectionCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DonutChart(slices, "סה\"כ", Money.format(total))
        }
        Spacer(Modifier.height(16.dp))
        com.familymoney.ui.components.DonutLegend(slices, maxItems = 5)
    }
}

@Composable
private fun GoalMiniRow(emoji: String, name: String, current: Double, target: Double) {
    val progress = if (target <= 0) 0f else (current / target).toFloat()
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$emoji $name", style = MaterialTheme.typography.titleSmall)
            Text(
                "${Money.format(current)} / ${Money.format(target)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(progress, colorOverride = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            Money.percent(progress * 100.0),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InvestmentSummaryCard(
    mine: Double,
    children: Double,
    onMine: () -> Unit,
    onChildren: () -> Unit
) {
    SectionCard {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onMine).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar("📈", MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Text("התיק שלי", style = MaterialTheme.typography.titleSmall)
            }
            Text(Money.format(mine), style = MoneySmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onChildren).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar("👨‍👩‍👧", Warning)
                Spacer(Modifier.width(12.dp))
                Text("השקעות ילדים", style = MaterialTheme.typography.titleSmall)
            }
            Text(Money.format(children), style = MoneySmall)
        }
    }
}

@Composable
fun TransactionRow(
    merchant: String,
    categoryKey: String,
    isIncome: Boolean,
    amount: Double,
    date: Long,
    onClick: () -> Unit
) {
    val category = ExpenseCategory.fromKey(categoryKey)
    val emoji = if (isIncome) "💰" else category.emoji
    val label = if (isIncome) "הכנסה" else category.he
    val color = if (isIncome) Success else MaterialTheme.colorScheme.onSurface

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(emoji, if (isIncome) Success else CategoryPalette[category.ordinal % CategoryPalette.size])
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                merchant.ifBlank { label },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Text(
                "$label · ${Dates.formatShort(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            (if (isIncome) "+" else "−") + Money.format(amount),
            style = MoneySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AskAiBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.background(heroBrush()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "שאל את הכסף שלי",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "\"כמה אפשר להוציא החודש?\" · \"למה לא מצליחים לחסוך?\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------- helpers

@Composable
fun colorForRecommendation(category: RecCategory): Color = when (category) {
    RecCategory.SAVING -> Success
    RecCategory.SPENDING -> Warning
    RecCategory.CASHFLOW -> MaterialTheme.colorScheme.primary
    RecCategory.GOAL -> MaterialTheme.colorScheme.primary
    RecCategory.INVESTMENT -> MaterialTheme.colorScheme.secondary
    RecCategory.BUDGET -> MaterialTheme.colorScheme.primary
    RecCategory.ALERT -> MaterialTheme.colorScheme.error
}

fun routeForAction(action: String): String = when {
    action.startsWith("transfer_to_savings") -> Routes.ACCOUNTS
    action.startsWith("reduce_spending") -> Routes.TRANSACTIONS
    action.startsWith("review_category") -> Routes.REPORTS
    action.startsWith("open_budget") -> Routes.BUDGET
    action.startsWith("contribute_goal") -> Routes.GOALS
    action.startsWith("create_goal") -> Routes.GOALS
    action.startsWith("open_recurring") -> Routes.RECURRING
    action.startsWith("open_simulator") -> Routes.SIMULATOR
    else -> Routes.INSIGHTS
}

private fun monthChangePercent(
    snapshots: List<com.familymoney.data.db.NetWorthSnapshotEntity>
): Double? {
    if (snapshots.size < 2) return null
    val last = snapshots.last()
    val prev = snapshots[snapshots.size - 2]
    if (prev.total <= 0.0) return null
    return (last.total - prev.total) / prev.total * 100.0
}
