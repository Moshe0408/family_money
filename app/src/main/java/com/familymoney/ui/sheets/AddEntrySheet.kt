package com.familymoney.ui.sheets

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.IncomeCategory
import com.familymoney.data.model.TxType
import com.familymoney.ui.components.Pill
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money
import kotlinx.coroutines.delay

/** Section 37: the FAB menu — expense, income, saving, investment, goal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    vm: MainViewModel,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf<TxType?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        when (mode) {
            null -> QuickMenu(
                onExpense = { mode = TxType.EXPENSE },
                onIncome = { mode = TxType.INCOME },
                onSaving = { onNavigate(Routes.ACCOUNTS) },
                onInvestment = { onNavigate(Routes.INVESTMENTS) },
                onGoal = { onNavigate(Routes.GOALS) }
            )
            else -> TransactionForm(
                vm = vm,
                type = mode!!,
                onSaved = onDismiss,
                onBack = { mode = null }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickMenu(
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onSaving: () -> Unit,
    onInvestment: () -> Unit,
    onGoal: () -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("מה להוסיף?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        val items = listOf(
            QuickItem("הוצאה", "💸", "רכישה, חשבון או חיוב", MaterialTheme.colorScheme.error, onExpense),
            QuickItem("הכנסה", "💰", "משכורת, בונוס או ריבית", Success, onIncome),
            QuickItem("חיסכון", "🐷", "העברה לחשבון חיסכון", MaterialTheme.colorScheme.primary, onSaving),
            QuickItem("השקעה", "📈", "מניה, קרן או פיקדון", MaterialTheme.colorScheme.secondary, onInvestment),
            QuickItem("יעד", "🎯", "מטרה חדשה לחסוך אליה", com.familymoney.ui.theme.Warning, onGoal)
        )

        items.forEach { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable(onClick = item.onClick),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.familymoney.ui.components.CategoryAvatar(item.emoji, item.color)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private data class QuickItem(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val color: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

@Composable
private fun TransactionForm(
    vm: MainViewModel,
    type: TxType,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val accounts by vm.accounts.collectAsState()
    val cards by vm.cards.collectAsState()

    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var incomeCategory by remember { mutableStateOf(IncomeCategory.SALARY) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var cardId by remember { mutableStateOf<String?>(null) }
    var isRecurring by remember { mutableStateOf(false) }
    var categoryTouched by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }

    // Section 11/31: suggest a category from what the user picked before.
    LaunchedEffect(merchant) {
        if (type == TxType.EXPENSE && merchant.length >= 2 && !categoryTouched) {
            delay(350)
            expenseCategory = vm.suggestCategory(merchant)
        }
    }

    val isIncome = type == TxType.INCOME
    val accent = if (isIncome) Success else MaterialTheme.colorScheme.error
    val valid = (Money.parse(amount) ?: 0.0) > 0.0

    Column(
        Modifier
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isIncome) "הכנסה חדשה" else "הוצאה חדשה",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                "החלף",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onBack)
            )
        }
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("סכום") },
            prefix = { Text("₪") },
            singleLine = true,
            textStyle = MoneyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text(if (isIncome) "מקור ההכנסה" else "בית עסק") },
            placeholder = { Text(if (isIncome) "לדוגמה: משכורת" else "לדוגמה: שופרסל") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Text("קטגוריה", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (isIncome) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(IncomeCategory.entries.toList()) { c ->
                    Pill(
                        text = "${c.emoji} ${c.he}",
                        color = Success,
                        selected = incomeCategory == c,
                        onClick = { incomeCategory = c; categoryTouched = true }
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategory.entries.toList()) { c ->
                    Pill(
                        text = "${c.emoji} ${c.he}",
                        color = CategoryPalette[c.ordinal % CategoryPalette.size],
                        selected = expenseCategory == c,
                        onClick = { expenseCategory = c; categoryTouched = true }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (accounts.isNotEmpty()) {
            Text("חשבון", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts) { a ->
                    Pill(
                        text = a.name,
                        color = MaterialTheme.colorScheme.primary,
                        selected = accountId == a.id,
                        onClick = { accountId = a.id; cardId = null }
                    )
                }
            }
        }

        if (!isIncome && cards.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("כרטיס (אופציונלי)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards) { c ->
                    Pill(
                        text = "${c.provider} ••${c.last4}",
                        color = MaterialTheme.colorScheme.secondary,
                        selected = cardId == c.id,
                        onClick = {
                            cardId = if (cardId == c.id) null else c.id
                            if (cardId != null) accountId = null
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("עסקה חוזרת", style = MaterialTheme.typography.titleSmall)
                Text(
                    "תיכלל בתחזית התזרים כל חודש",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("הערה (אופציונלי)") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val value = Money.parse(amount) ?: return@Button
                vm.addTransaction(
                    TransactionEntity(
                        accountId = accountId,
                        cardId = cardId,
                        type = type,
                        amount = value,
                        merchant = merchant.trim(),
                        category = if (isIncome) incomeCategory.name else expenseCategory.name,
                        date = Dates.nowMillis(),
                        isRecurring = isRecurring,
                        note = note.trim()
                    )
                )
                onSaved()
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("שמור ${if (isIncome) "הכנסה" else "הוצאה"}")
        }
    }
}
