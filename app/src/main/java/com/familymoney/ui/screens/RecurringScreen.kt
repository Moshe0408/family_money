package com.familymoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.db.RecurringEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.TxType
import com.familymoney.ui.components.CategoryAvatar
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.components.StatTile
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/** Sections 32-33: fixed expenses and subscriptions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(vm: MainViewModel, navController: NavHostController) {
    val recurring by vm.recurring.collectAsState()
    var editing by remember { mutableStateOf<RecurringEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val expenses = recurring.filter { it.type == TxType.EXPENSE }
    val incomes = recurring.filter { it.type == TxType.INCOME }
    val subscriptions = expenses.filter { it.isSubscription }
    val monthlyTotal = expenses.sumOf { it.amount }
    val subsTotal = subscriptions.sumOf { it.amount }

    DetailScaffold(
        title = "🔁 הוצאות קבועות",
        navController = navController,
        actions = {
            IconButton(onClick = { vm.rescanRecurring() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "סרוק מחדש")
            }
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "הוספה")
            }
        }
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recurring.isEmpty()) {
                item {
                    EmptyState(
                        "🔁",
                        "לא זוהו הוצאות קבועות",
                        "המערכת מזהה אוטומטית חיובים שחוזרים כל חודש. אפשר גם להוסיף ידנית."
                    ) {
                        Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("הוספה ידנית")
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "סה\"כ בחודש", Money.format(monthlyTotal), "📅",
                        MaterialTheme.colorScheme.error, Modifier.weight(1f)
                    )
                    StatTile(
                        "בשנה", Money.format(monthlyTotal * 12), "📆",
                        Warning, Modifier.weight(1f)
                    )
                }
            }

            // Section 33: subscription round-up.
            if (subscriptions.isNotEmpty()) {
                item {
                    SectionCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            "💡 מצאנו ${subscriptions.size} מנויים",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "עלות חודשית ${Money.format(subsTotal)} — כלומר " +
                                "${Money.format(subsTotal * 12)} בשנה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (incomes.isNotEmpty()) {
                item { SectionHeader("הכנסות קבועות", emoji = "💰") }
                items(incomes, key = { it.id }) { r ->
                    RecurringRow(r) { editing = r }
                }
            }

            item { SectionHeader("חיובים קבועים", emoji = "🧾") }
            items(expenses.sortedBy { it.dayOfMonth }, key = { it.id }) { r ->
                RecurringRow(r) { editing = r }
            }
        }
    }

    if (creating) {
        RecurringEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { vm.saveRecurring(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { r ->
        RecurringEditorSheet(
            initial = r,
            onDismiss = { editing = null },
            onSave = { vm.saveRecurring(it); editing = null },
            onDelete = { vm.deleteRecurring(r); editing = null }
        )
    }
}

@Composable
private fun RecurringRow(r: RecurringEntity, onClick: () -> Unit) {
    val isIncome = r.type == TxType.INCOME
    val cat = ExpenseCategory.fromKey(r.category)
    SectionCard(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(
                if (isIncome) "💰" else if (r.isSubscription) "📺" else cat.emoji,
                if (isIncome) Success else CategoryPalette[cat.ordinal % CategoryPalette.size]
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, style = MaterialTheme.typography.titleSmall)
                    if (r.detected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "· זוהה אוטומטית",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "ב־${r.dayOfMonth} בחודש · ${if (isIncome) "הכנסה" else cat.he}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (isIncome) "+" else "−") + Money.format(r.amount),
                style = MoneySmall,
                color = if (isIncome) Success else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringEditorSheet(
    initial: RecurringEntity?,
    onDismiss: () -> Unit,
    onSave: (RecurringEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(trimNumber(initial?.amount ?: 0.0)) }
    var day by remember { mutableStateOf((initial?.dayOfMonth ?: 1).toString()) }
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var category by remember { mutableStateOf(ExpenseCategory.fromKey(initial?.category)) }
    var isSubscription by remember { mutableStateOf(initial?.isSubscription ?: false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                if (initial == null) "הוצאה קבועה חדשה" else "עריכה",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("הוצאה", MaterialTheme.colorScheme.error,
                    selected = type == TxType.EXPENSE) { type = TxType.EXPENSE }
                Pill("הכנסה", Success, selected = type == TxType.INCOME) { type = TxType.INCOME }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם") },
                placeholder = { Text("לדוגמה: שכר דירה") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("סכום") }, prefix = { Text("₪") }, singleLine = true,
                    textStyle = MoneyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("יום") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }

            if (type == TxType.EXPENSE) {
                Spacer(Modifier.height(16.dp))
                Text("קטגוריה", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseCategory.entries.toList()) { c ->
                        Pill(
                            "${c.emoji} ${c.he}",
                            CategoryPalette[c.ordinal % CategoryPalette.size],
                            selected = category == c
                        ) { category = c }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("זהו מנוי", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = isSubscription, onCheckedChange = { isSubscription = it })
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSave(
                        (initial ?: RecurringEntity(name = "", amount = 0.0)).copy(
                            name = name.trim().ifBlank { "הוצאה קבועה" },
                            type = type,
                            amount = Money.parse(amount) ?: 0.0,
                            category = if (type == TxType.EXPENSE) category.name else "SALARY",
                            dayOfMonth = day.toIntOrNull()?.coerceIn(1, 28) ?: 1,
                            isSubscription = isSubscription && type == TxType.EXPENSE,
                            active = true
                        )
                    )
                },
                enabled = name.isNotBlank() && (Money.parse(amount) ?: 0.0) > 0,
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
