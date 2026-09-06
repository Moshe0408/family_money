package com.familymoney.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.TxType
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import java.time.YearMonth

private enum class TxFilter(val label: String) { ALL("הכל"), EXPENSE("הוצאות"), INCOME("הכנסות") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(vm: MainViewModel, navController: NavHostController) {
    val transactions by vm.transactions.collectAsState()
    val cards by vm.cards.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TxFilter.ALL) }
    var cardFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var selected by remember { mutableStateOf<TransactionEntity?>(null) }

    val filtered = remember(transactions, query, filter, cardFilter, categoryFilter) {
        transactions.filter { tx ->
            (query.isBlank() || tx.merchant.contains(query, ignoreCase = true) ||
                tx.note.contains(query, ignoreCase = true)) &&
                (filter == TxFilter.ALL ||
                    (filter == TxFilter.EXPENSE && tx.type == TxType.EXPENSE) ||
                    (filter == TxFilter.INCOME && tx.type == TxType.INCOME)) &&
                (cardFilter == null || tx.cardId == cardFilter) &&
                (categoryFilter == null || tx.category == categoryFilter?.name)
        }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { Dates.yearMonthKey(it.date) }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("עסקאות", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { navController.navigate(Routes.IMPORT) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "ייבוא")
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("חיפוש בית עסק או הערה") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TxFilter.entries.toList()) { f ->
                    Pill(
                        text = f.label,
                        color = MaterialTheme.colorScheme.primary,
                        selected = filter == f,
                        onClick = { filter = f }
                    )
                }
            }
        }

        // Section 15: filter by whose card the charge landed on.
        if (cards.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Pill(
                            "כל הכרטיסים",
                            MaterialTheme.colorScheme.secondary,
                            selected = cardFilter == null,
                            onClick = { cardFilter = null }
                        )
                    }
                    items(cards) { c ->
                        Pill(
                            text = "${c.provider} ••${c.last4}",
                            color = MaterialTheme.colorScheme.secondary,
                            selected = cardFilter == c.id,
                            onClick = { cardFilter = if (cardFilter == c.id) null else c.id }
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Pill(
                        "כל הקטגוריות",
                        MaterialTheme.colorScheme.tertiary,
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null }
                    )
                }
                items(ExpenseCategory.entries.toList()) { c ->
                    Pill(
                        text = "${c.emoji} ${c.he}",
                        color = CategoryPalette[c.ordinal % CategoryPalette.size],
                        selected = categoryFilter == c,
                        onClick = { categoryFilter = if (categoryFilter == c) null else c }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🧾",
                    title = "אין עסקאות להצגה",
                    body = if (transactions.isEmpty())
                        "הוסיפו עסקה עם כפתור ה־+ או ייבאו דף חשבון מהבנק."
                    else "נסו לשנות את הסינון."
                ) {
                    TextButton(onClick = { navController.navigate(Routes.IMPORT) }) {
                        Text("ייבוא מקובץ")
                    }
                }
            }
        }

        grouped.forEach { (yearMonth, list) ->
            item(key = "header_$yearMonth") {
                MonthHeader(yearMonth, list)
            }
            items(list, key = { it.id }) { tx ->
                SectionCard(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    onClick = { selected = tx }
                ) {
                    TransactionRow(
                        merchant = tx.merchant,
                        categoryKey = tx.category,
                        isIncome = tx.type == TxType.INCOME,
                        amount = tx.amount,
                        date = tx.date,
                        onClick = { selected = tx }
                    )
                }
            }
        }
    }

    selected?.let { tx ->
        TransactionDetailSheet(
            tx = tx,
            vm = vm,
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun MonthHeader(yearMonthKey: String, list: List<TransactionEntity>) {
    val parts = yearMonthKey.split("-")
    val label = runCatching {
        Dates.hebrewMonthYear(YearMonth.of(parts[0].toInt(), parts[1].toInt()))
    }.getOrDefault(yearMonthKey)

    val income = list.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row {
            if (income > 0) {
                Text("+${Money.format(income)}", style = MaterialTheme.typography.labelMedium, color = Success)
                Spacer(Modifier.width(10.dp))
            }
            if (expense > 0) {
                Text(
                    "−${Money.format(expense)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    tx: TransactionEntity,
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val accounts by vm.accounts.collectAsState()
    val cards by vm.cards.collectAsState()
    var category by remember { mutableStateOf(ExpenseCategory.fromKey(tx.category)) }
    val isIncome = tx.type == TxType.INCOME

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                tx.merchant.ifBlank { if (isIncome) "הכנסה" else "הוצאה" },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                (if (isIncome) "+" else "−") + Money.format(tx.amount, decimals = true),
                style = MaterialTheme.typography.displaySmall,
                color = if (isIncome) Success else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(18.dp))

            DetailLine("תאריך", Dates.formatDay(tx.date))
            DetailLine("קטגוריה", "${category.emoji} ${category.he}")
            accounts.firstOrNull { it.id == tx.accountId }?.let { DetailLine("חשבון", it.name) }
            cards.firstOrNull { it.id == tx.cardId }?.let {
                DetailLine("כרטיס", "${it.provider} ••${it.last4}")
            }
            if (tx.isRecurring) DetailLine("סוג", "עסקה חוזרת")
            if (tx.note.isNotBlank()) DetailLine("הערה", tx.note)
            DetailLine(
                "מקור",
                when (tx.source) {
                    com.familymoney.data.model.TxSource.MANUAL -> "הזנה ידנית"
                    com.familymoney.data.model.TxSource.IMPORT -> "ייבוא מקובץ"
                    com.familymoney.data.model.TxSource.BANK -> "חשבון בנק"
                    com.familymoney.data.model.TxSource.CARD -> "כרטיס אשראי"
                    com.familymoney.data.model.TxSource.RECURRING -> "הוראת קבע"
                }
            )

            if (!isIncome) {
                Spacer(Modifier.height(18.dp))
                Text("שינוי קטגוריה", style = MaterialTheme.typography.titleSmall)
                Text(
                    "המערכת תזכור את הבחירה לעסקאות עתידיות מאותו בית עסק.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseCategory.entries.toList()) { c ->
                        Pill(
                            text = "${c.emoji} ${c.he}",
                            color = CategoryPalette[c.ordinal % CategoryPalette.size],
                            selected = category == c,
                            onClick = {
                                category = c
                                vm.updateTransaction(tx.copy(category = c.name))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vm.deleteTransaction(tx)
                        onDismiss()
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "מחיקת העסקה",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
