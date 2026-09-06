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
import com.familymoney.data.db.ChildEntity
import com.familymoney.data.db.InvestmentEntity
import com.familymoney.engine.Compound
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.DisclaimerNote
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.LineChart
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.heroBrush
import com.familymoney.ui.theme.MoneyLarge
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/** Sections 22-23: children's portfolios and the age-18 projection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenScreen(vm: MainViewModel, navController: NavHostController) {
    val children by vm.children.collectAsState()
    val investments by vm.investments.collectAsState()

    var editing by remember { mutableStateOf<ChildEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var addingHolding by remember { mutableStateOf<ChildEntity?>(null) }

    fun valueOf(child: ChildEntity): Double =
        investments.filter { it.childId == child.id }.sumOf { it.currentValue }
            .takeIf { it > 0 } ?: child.initialAmount

    fun investedIn(child: ChildEntity): Double =
        investments.filter { it.childId == child.id }.sumOf { it.investedAmount }

    val totalValue = children.sumOf { valueOf(it) }
    val totalInvested = children.sumOf { investedIn(it) }.takeIf { it > 0 } ?: totalValue
    val totalReturn = if (totalInvested <= 0) 0.0
    else (totalValue - totalInvested) / totalInvested * 100

    DetailScaffold(
        title = "👨‍👩‍👧 השקעות ילדים",
        navController = navController,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "הוספת ילד")
            }
        }
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (children.isEmpty()) {
                item {
                    EmptyState(
                        "🧒",
                        "עוד לא הוספתם ילדים",
                        "הגדירו תיק לכל ילד וראו כמה יצטבר עד גיל 18."
                    ) {
                        Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("הוספת ילד")
                        }
                    }
                }
                return@LazyColumn
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
                            "שווי כולל",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(Money.format(totalValue), style = MoneyLarge, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "תשואה ${Money.percent(totalReturn, signed = true)} · " +
                                "${children.size} ילדים · " +
                                "${Money.format(children.sumOf { it.monthlyContribution })} לחודש",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            items(children, key = { it.id }) { child ->
                val value = valueOf(child)
                val invested = investedIn(child)
                val projection = Compound.childProjection(
                    currentAge = child.currentAge,
                    targetAge = child.targetAge,
                    existing = value,
                    monthly = child.monthlyContribution,
                    annualRatePercent = child.expectedAnnualReturn
                )
                val yearsLeft = (child.targetAge - child.currentAge).coerceAtLeast(0)
                val holdings = investments.filter { it.childId == child.id }

                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(child.emoji, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "גיל ${child.currentAge} · יעד גיל ${child.targetAge}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Money.format(value), style = MoneyMedium)
                            if (invested > 0) {
                                Text(
                                    Money.percent(
                                        (value - invested) / invested * 100, signed = true
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (value >= invested) Success
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    ProgressBar(
                        progress = if (child.targetAge <= 0) 0f
                        else child.currentAge.toFloat() / child.targetAge,
                        colorOverride = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "עוד $yearsLeft שנים לגיל ${child.targetAge}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Section 23: what it becomes at the target age.
                    Spacer(Modifier.height(18.dp))
                    SectionCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        Text(
                            "תחזית לגיל ${child.targetAge}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(10.dp))
                        AmountRow("סכום משוער", Money.format(projection.finalValue), emphasize = true)
                        AmountRow("סה\"כ הפקדות", Money.format(projection.totalContributed))
                        AmountRow("רווח משוער", Money.format(projection.totalProfit), Success)
                        AmountRow("הפקדה חודשית", Money.format(child.monthlyContribution))
                        AmountRow(
                            "תשואה שנתית מונחת",
                            Money.percent(child.expectedAnnualReturn)
                        )

                        if (projection.points.size > 2) {
                            Spacer(Modifier.height(14.dp))
                            LineChart(
                                values = projection.points.map { it.value }.reversed(),
                                lineColor = MaterialTheme.colorScheme.secondary,
                                showDots = false
                            )
                        }
                    }

                    if (holdings.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("החזקות", style = MaterialTheme.typography.titleSmall)
                        holdings.forEach { h ->
                            AmountRow(h.name, Money.format(h.currentValue))
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { addingHolding = child },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("הוסף החזקה") }
                        Button(
                            onClick = { editing = child },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("ערוך") }
                    }
                }
            }

            item {
                DisclaimerNote(
                    "התחזית היא חישוב מתמטי לפי תשואה שנתית מונחת קבועה, ואינה הבטחת תשואה."
                )
            }
        }
    }

    if (creating) {
        ChildEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { vm.saveChild(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { child ->
        ChildEditorSheet(
            initial = child,
            onDismiss = { editing = null },
            onSave = { vm.saveChild(it); editing = null },
            onDelete = { vm.deleteChild(child); editing = null }
        )
    }

    addingHolding?.let { child ->
        InvestmentEditorSheet(
            initial = null,
            childId = child.id,
            onDismiss = { addingHolding = null },
            onSave = { vm.saveInvestment(it); addingHolding = null },
            onDelete = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildEditorSheet(
    initial: ChildEntity?,
    onDismiss: () -> Unit,
    onSave: (ChildEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🧒") }
    var currentAge by remember { mutableStateOf((initial?.currentAge ?: 0).toString()) }
    var targetAge by remember { mutableStateOf((initial?.targetAge ?: 18).toString()) }
    var amount by remember { mutableStateOf(trimNumber(initial?.initialAmount ?: 0.0)) }
    var monthly by remember { mutableStateOf(trimNumber(initial?.monthlyContribution ?: 500.0)) }
    var rate by remember { mutableStateOf(trimNumber(initial?.expectedAnnualReturn ?: 6.0)) }

    val emojiChoices = listOf("🧒", "👦", "👧", "🍼", "⭐", "🌟", "🎈", "🧸")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (initial == null) "ילד חדש" else "עריכה",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(emojiChoices) { e ->
                    Pill(e, MaterialTheme.colorScheme.secondary, selected = emoji == e) {
                        emoji = e
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentAge,
                    onValueChange = { currentAge = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("גיל נוכחי") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetAge,
                    onValueChange = { targetAge = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("גיל יעד") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("סכום קיים") }, prefix = { Text("₪") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monthly,
                    onValueChange = { monthly = it.filter { c -> c.isDigit() } },
                    label = { Text("הפקדה חודשית") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("תשואה %") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    onSave(
                        (initial ?: ChildEntity(name = "")).copy(
                            name = name.trim().ifBlank { "ילד" },
                            emoji = emoji,
                            currentAge = currentAge.toIntOrNull()?.coerceIn(0, 30) ?: 0,
                            targetAge = targetAge.toIntOrNull()?.coerceIn(1, 40) ?: 18,
                            initialAmount = Money.parse(amount) ?: 0.0,
                            monthlyContribution = Money.parse(monthly) ?: 0.0,
                            expectedAnnualReturn = Money.parse(rate) ?: 6.0
                        )
                    )
                },
                enabled = name.isNotBlank(),
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
