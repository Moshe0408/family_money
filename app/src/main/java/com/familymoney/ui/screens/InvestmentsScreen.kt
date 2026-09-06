package com.familymoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.db.InvestmentEntity
import com.familymoney.data.model.InvestmentType
import com.familymoney.ui.components.CompositionBar
import com.familymoney.ui.components.DisclaimerNote
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.heroBrush
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.MoneyLarge
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/** Section 21: general investments. Child portfolios live in ChildrenScreen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(vm: MainViewModel, navController: NavHostController) {
    val investments by vm.investments.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var editing by remember { mutableStateOf<InvestmentEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val mine = investments.filter { it.childId == null }
    val totalValue = mine.sumOf { it.currentValue }
    val totalInvested = mine.sumOf { it.investedAmount }
    val profit = totalValue - totalInvested
    val returnPercent = if (totalInvested <= 0) 0.0 else profit / totalInvested * 100
    val dividends = mine.sumOf { it.dividendsYtd }
    val interest = mine.sumOf { it.interestYtd } +
        accounts.filter { it.interestRatePercent > 0 }
            .sumOf { it.balance * it.interestRatePercent / 100 }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("השקעות", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "השקעה חדשה")
                }
            }
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(heroBrush(), RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Column {
                    Text(
                        "שווי התיק",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(Money.format(totalValue), style = MoneyLarge, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        HeroMini("תשואה", Money.percent(returnPercent, signed = true), Modifier.weight(1f))
                        HeroMini("רווח/הפסד", Money.signed(profit), Modifier.weight(1f))
                        HeroMini("הופקד", Money.format(totalInvested), Modifier.weight(1f))
                    }
                }
            }
        }

        if (mine.isNotEmpty()) {
            item {
                SectionCard {
                    Text("פיזור התיק", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                    val byType = mine.groupBy { it.type }
                        .map { (t, list) -> t.he to list.sumOf { it.currentValue } }
                        .sortedByDescending { it.second }
                    CompositionBar(byType)
                    Spacer(Modifier.height(14.dp))
                    byType.forEachIndexed { i, (label, value) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .width(10.dp).height(10.dp)
                                        .background(
                                            com.familymoney.ui.theme.CategoryPalette[i % 16],
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(Money.format(value), style = MoneySmall)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.familymoney.ui.components.StatTile(
                        "דיבידנדים השנה", Money.format(dividends), "💵",
                        Success, Modifier.weight(1f)
                    )
                    com.familymoney.ui.components.StatTile(
                        "ריבית שנתית", Money.format(interest), "📈",
                        MaterialTheme.colorScheme.primary, Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { navController.navigate(Routes.SIMULATOR) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)
                ) { Text("סימולטור") }
                Button(
                    onClick = { navController.navigate(Routes.CHILDREN) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) { Text("השקעות ילדים") }
            }
        }

        if (mine.isEmpty()) {
            item {
                EmptyState("📈", "עוד אין השקעות", "הוסיפו מניה, קרן, ETF או פיקדון כדי לעקוב אחרי השווי והתשואה.") {
                    Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                        Text("הוספת השקעה")
                    }
                }
            }
        } else {
            item { SectionHeader("ההחזקות שלי", emoji = "💼") }
            items(mine, key = { it.id }) { inv ->
                InvestmentCard(inv) { editing = inv }
            }
        }

        item {
            DisclaimerNote(
                "הנתונים מוזנים ידנית ומשמשים למעקב וחישוב בלבד. " +
                    "אין באמור ייעוץ השקעות, שיווק השקעות או המלצה לביצוע פעולה בניירות ערך."
            )
        }
    }

    if (creating) {
        InvestmentEditorSheet(
            initial = null, childId = null,
            onDismiss = { creating = false },
            onSave = { vm.saveInvestment(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { inv ->
        InvestmentEditorSheet(
            initial = inv, childId = inv.childId,
            onDismiss = { editing = null },
            onSave = { vm.saveInvestment(it); editing = null },
            onDelete = { vm.deleteInvestment(inv); editing = null }
        )
    }
}

@Composable
private fun HeroMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(value, style = MoneySmall, color = Color.White)
    }
}

@Composable
fun InvestmentCard(inv: InvestmentEntity, onClick: () -> Unit) {
    val positive = inv.profit >= 0
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(inv.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(inv.type.he)
                        if (inv.symbol.isNotBlank()) append(" · ${inv.symbol}")
                        if (inv.quantity != 1.0) append(" · ${trimNumber(inv.quantity)} יח'")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(inv.currentValue), style = MoneySmall)
                Text(
                    Money.percent(inv.returnPercent, signed = true),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (positive) Success else MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("מחיר רכישה", Money.format(inv.averagePrice, decimals = true))
            MiniStat("מחיר נוכחי", Money.format(inv.currentPrice, decimals = true))
            MiniStat(
                "רווח/הפסד",
                Money.signed(inv.profit),
                if (positive) Success else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color? = null) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentEditorSheet(
    initial: InvestmentEntity?,
    childId: String?,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var symbol by remember { mutableStateOf(initial?.symbol ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: InvestmentType.ETF) }
    var quantity by remember { mutableStateOf(trimNumber(initial?.quantity ?: 1.0)) }
    var avgPrice by remember { mutableStateOf(trimNumber(initial?.averagePrice ?: 0.0)) }
    var curPrice by remember { mutableStateOf(trimNumber(initial?.currentPrice ?: 0.0)) }
    var dividends by remember { mutableStateOf(trimNumber(initial?.dividendsYtd ?: 0.0)) }

    val qty = Money.parse(quantity) ?: 1.0
    val avg = Money.parse(avgPrice) ?: 0.0
    val cur = Money.parse(curPrice) ?: 0.0
    val value = qty * cur
    val invested = qty * avg
    val profit = value - invested

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (initial == null) "השקעה חדשה" else "עריכת השקעה",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InvestmentType.entries.toList()) { t ->
                    Pill(
                        text = t.he,
                        color = MaterialTheme.colorScheme.secondary,
                        selected = type == t,
                        onClick = { type = t }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם ההשקעה") },
                placeholder = { Text("לדוגמה: מחקה S&P 500") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = symbol, onValueChange = { symbol = it.uppercase() },
                label = { Text("סימול (אופציונלי)") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("כמות") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = avgPrice,
                    onValueChange = { avgPrice = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("מחיר רכישה") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = curPrice,
                    onValueChange = { curPrice = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("מחיר נוכחי") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dividends,
                    onValueChange = { dividends = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("דיבידנד השנה") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }

            if (value > 0) {
                Spacer(Modifier.height(16.dp))
                SectionCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("שווי", Money.format(value))
                        MiniStat("הושקע", Money.format(invested))
                        MiniStat(
                            "רווח",
                            Money.signed(profit),
                            if (profit >= 0) Success else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSave(
                        (initial ?: InvestmentEntity(name = "")).copy(
                            name = name.trim().ifBlank { type.he },
                            symbol = symbol.trim(),
                            type = type,
                            quantity = qty,
                            averagePrice = avg,
                            currentPrice = cur,
                            dividendsYtd = Money.parse(dividends) ?: 0.0,
                            childId = childId,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                enabled = name.isNotBlank() && cur > 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("שמירה") }

            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("מחיקה") }
            }
        }
    }
}

internal fun trimNumber(v: Double): String =
    if (v == kotlin.math.floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()
