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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.db.BudgetEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.ui.components.CategoryAvatar
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import java.time.YearMonth
import kotlin.math.roundToLong

/** Section 18-19: family budget and overspend detection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(vm: MainViewModel, navController: NavHostController) {
    val budgets by vm.budgets.collectAsState()
    val snapshot by vm.snapshot.collectAsState()
    var editing by remember { mutableStateOf<BudgetEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val spend = snapshot?.categorySpendThisMonth.orEmpty()
    val totalBudget = budgets.sumOf { it.limitAmount }
    val totalSpent = budgets.sumOf { spend[it.category] ?: 0.0 }

    DetailScaffold(
        title = "תקציב ${Dates.hebrewMonth(YearMonth.now())}",
        navController = navController,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "קטגוריה חדשה")
            }
        }
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (budgets.isEmpty()) {
                item {
                    EmptyState(
                        "📊",
                        "עוד לא הגדרתם תקציב",
                        "קבעו תקרה חודשית לכל קטגוריה, והמערכת תתריע לפני חריגה."
                    ) {
                        Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("הוספת קטגוריה")
                        }
                    }
                }
            } else {
                item {
                    SectionCard {
                        Text(
                            "סך התקציב החודשי",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(Money.format(totalSpent), style = MoneyMedium)
                            Text(
                                " / ${Money.format(totalBudget)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        ProgressBar(
                            if (totalBudget <= 0) 0f else (totalSpent / totalBudget).toFloat(),
                            height = 12.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        val remaining = totalBudget - totalSpent
                        Text(
                            if (remaining >= 0)
                                "נשארו ${Money.format(remaining)} לחודש"
                            else "חריגה של ${Money.format(-remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remaining >= 0) Success else MaterialTheme.colorScheme.error
                        )
                    }
                }

                items(
                    budgets.sortedByDescending { (spend[it.category] ?: 0.0) / it.limitAmount },
                    key = { it.id }
                ) { budget ->
                    BudgetRow(
                        budget = budget,
                        spent = spend[budget.category] ?: 0.0,
                        average = snapshot?.categoryAverage?.get(budget.category),
                        onClick = { editing = budget }
                    )
                }
            }

            // Categories with real spending but no budget yet.
            val unbudgeted = spend.keys - budgets.map { it.category }.toSet()
            if (unbudgeted.isNotEmpty()) {
                item {
                    Text(
                        "קטגוריות ללא תקציב",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                item {
                    SectionCard {
                        unbudgeted.sortedByDescending { spend[it] ?: 0.0 }.forEach { key ->
                            val cat = ExpenseCategory.fromKey(key)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${cat.emoji} ${cat.he}", Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(Money.format(spend[key] ?: 0.0), style = MoneySmall)
                                Spacer(Modifier.width(12.dp))
                                Pill("הגדר", MaterialTheme.colorScheme.primary) {
                                    editing = BudgetEntity(
                                        id = "budget_$key", category = key, limitAmount = 0.0
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        BudgetEditorSheet(
            initial = null,
            usedCategories = budgets.map { it.category }.toSet(),
            onDismiss = { creating = false },
            onSave = { vm.saveBudget(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { budget ->
        BudgetEditorSheet(
            initial = budget,
            usedCategories = emptySet(),
            onDismiss = { editing = null },
            onSave = { vm.saveBudget(it); editing = null },
            onDelete = { vm.deleteBudget(budget); editing = null }
        )
    }
}

@Composable
private fun BudgetRow(
    budget: BudgetEntity,
    spent: Double,
    average: Double?,
    onClick: () -> Unit
) {
    val cat = ExpenseCategory.fromKey(budget.category)
    val ratio = if (budget.limitAmount <= 0) 0f else (spent / budget.limitAmount).toFloat()
    val over = spent > budget.limitAmount

    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(
                cat.emoji,
                CategoryPalette[cat.ordinal % CategoryPalette.size]
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cat.he, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Money.format(spent)} / ${Money.format(budget.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${(ratio * 100).roundToLong()}%",
                style = MoneySmall,
                color = when {
                    over -> MaterialTheme.colorScheme.error
                    ratio >= 0.85f -> Warning
                    else -> Success
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(ratio, height = 10.dp)

        // Section 19: explain what changed versus the historical average.
        if (average != null && average > 0) {
            val delta = (spent - average) / average * 100
            if (kotlin.math.abs(delta) >= 15) {
                Spacer(Modifier.height(10.dp))
                Text(
                    if (delta > 0)
                        "⚠️ גבוה ב־${delta.roundToLong()}% מהממוצע (${Money.format(average)})"
                    else
                        "✅ נמוך ב־${(-delta).roundToLong()}% מהממוצע (${Money.format(average)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (delta > 0) Warning else Success
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorSheet(
    initial: BudgetEntity?,
    usedCategories: Set<String>,
    onDismiss: () -> Unit,
    onSave: (BudgetEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var category by remember {
        mutableStateOf(
            initial?.let { ExpenseCategory.fromKey(it.category) }
                ?: ExpenseCategory.entries.firstOrNull { it.name !in usedCategories }
                ?: ExpenseCategory.OTHER
        )
    }
    var limit by remember {
        mutableStateOf(initial?.limitAmount?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                if (initial == null) "תקציב חדש" else "עריכת תקציב",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            if (initial == null) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ExpenseCategory.entries.filter { it.name !in usedCategories }) { c ->
                        Pill(
                            text = "${c.emoji} ${c.he}",
                            color = CategoryPalette[c.ordinal % CategoryPalette.size],
                            selected = category == c,
                            onClick = { category = c }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Text(
                    "${category.emoji} ${category.he}",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = limit,
                onValueChange = { limit = it.filter { c -> c.isDigit() } },
                label = { Text("תקרה חודשית") },
                prefix = { Text("₪") },
                singleLine = true,
                textStyle = MoneyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    onSave(
                        BudgetEntity(
                            id = initial?.id ?: "budget_${category.name}",
                            familyId = initial?.familyId.orEmpty(),
                            category = category.name,
                            yearMonth = "",
                            limitAmount = Money.parse(limit) ?: 0.0
                        )
                    )
                },
                enabled = (Money.parse(limit) ?: 0.0) > 0,
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
                ) { Text("הסרת התקציב") }
            }
        }
    }
}
