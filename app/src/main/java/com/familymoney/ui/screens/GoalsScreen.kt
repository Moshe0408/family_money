package com.familymoney.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.db.GoalEntity
import com.familymoney.data.model.GoalStatus
import com.familymoney.engine.Compound
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(vm: MainViewModel, navController: NavHostController) {
    val goals by vm.goals.collectAsState()
    var editing by remember { mutableStateOf<GoalEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var contributing by remember { mutableStateOf<GoalEntity?>(null) }

    val active = goals.filter { it.status == GoalStatus.ACTIVE }
    val completed = goals.filter { it.status == GoalStatus.COMPLETED }

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
                Column {
                    Text("יעדים", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${active.size} פעילים · סה\"כ נחסך ${Money.format(goals.sumOf { it.currentAmount })}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "יעד חדש")
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                EmptyState("🎯", "עוד אין יעדים", "יעד עוזר לדעת כמה לחסוך כל חודש ומתי תגיעו אליו.") {
                    Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                        Text("יצירת יעד ראשון")
                    }
                }
            }
        }

        items(active, key = { it.id }) { goal ->
            GoalCard(
                goal = goal,
                onEdit = { editing = goal },
                onContribute = { contributing = goal }
            )
        }

        if (completed.isNotEmpty()) {
            item {
                Text(
                    "יעדים שהושלמו",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            items(completed, key = { it.id }) { goal ->
                GoalCard(goal = goal, onEdit = { editing = goal }, onContribute = null)
            }
        }
    }

    if (creating) {
        GoalEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { vm.saveGoal(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { goal ->
        GoalEditorSheet(
            initial = goal,
            onDismiss = { editing = null },
            onSave = { vm.saveGoal(it); editing = null },
            onDelete = { vm.deleteGoal(goal); editing = null }
        )
    }

    contributing?.let { goal ->
        ContributeSheet(
            goal = goal,
            vm = vm,
            onDismiss = { contributing = null }
        )
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, onEdit: () -> Unit, onContribute: (() -> Unit)?) {
    val progress = if (goal.targetAmount <= 0) 0f
    else (goal.currentAmount / goal.targetAmount).toFloat()
    val monthsLeft = Dates.monthsBetween(Dates.nowMillis(), goal.targetDate)
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    val requiredMonthly = if (monthsLeft <= 0) remaining else remaining / monthsLeft
    val onTrack = goal.monthlyContribution >= requiredMonthly

    SectionCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(goal.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (monthsLeft > 0) "יעד: ${Dates.formatDay(goal.targetDate)} · $monthsLeft חודשים"
                    else "תאריך היעד עבר",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(Money.percent(progress * 100.0), style = MoneySmall,
                color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(14.dp))
        ProgressBar(progress, colorOverride = MaterialTheme.colorScheme.primary, height = 12.dp)
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(Money.format(goal.currentAmount), style = MoneySmall)
            Text(
                Money.format(goal.targetAmount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (remaining > 0) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "נדרש לחודש",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        Money.format(requiredMonthly),
                        style = MoneySmall,
                        color = if (onTrack) Success else com.familymoney.ui.theme.Warning
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "חסר ליעד",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(Money.format(remaining), style = MoneySmall)
                }
            }

            if (onContribute != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onContribute,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("הוסף הפקדה") }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text("🎉 היעד הושלם", style = MaterialTheme.typography.titleSmall, color = Success)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorSheet(
    initial: GoalEntity?,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🎯") }
    var target by remember { mutableStateOf(initial?.targetAmount?.toLong()?.toString() ?: "") }
    var current by remember { mutableStateOf(initial?.currentAmount?.toLong()?.toString() ?: "0") }
    var months by remember {
        mutableStateOf(
            initial?.let { Dates.monthsBetween(Dates.nowMillis(), it.targetDate).toString() } ?: "12"
        )
    }

    val emojiChoices = listOf("🎯", "✈️", "🚗", "🏠", "🛟", "🎓", "💍", "🔨", "🐷", "🏖️", "💻", "🎁")
    val targetValue = Money.parse(target) ?: 0.0
    val currentValue = Money.parse(current) ?: 0.0
    val monthsValue = months.toIntOrNull() ?: 12
    val suggested = Compound.requiredMonthly(targetValue, currentValue, monthsValue, 0.0)

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
                if (initial == null) "יעד חדש" else "עריכת יעד",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(emojiChoices) { e ->
                    Pill(
                        text = e,
                        color = MaterialTheme.colorScheme.primary,
                        selected = emoji == e,
                        onClick = { emoji = e }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם היעד") },
                placeholder = { Text("לדוגמה: חופשה משפחתית") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter { c -> c.isDigit() } },
                label = { Text("סכום היעד") }, prefix = { Text("₪") },
                singleLine = true, textStyle = MoneyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = current,
                onValueChange = { current = it.filter { c -> c.isDigit() } },
                label = { Text("כמה כבר נחסך") }, prefix = { Text("₪") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = months,
                onValueChange = { months = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("בעוד כמה חודשים") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )

            if (suggested > 0) {
                Spacer(Modifier.height(16.dp))
                SectionCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    Text(
                        "כדי להגיע ליעד בזמן",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${Money.format(suggested)} לחודש",
                        style = MoneyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSave(
                        (initial ?: GoalEntity(
                            name = "", targetAmount = 0.0, targetDate = 0L
                        )).copy(
                            name = name.trim().ifBlank { "יעד" },
                            emoji = emoji,
                            targetAmount = targetValue,
                            currentAmount = currentValue,
                            targetDate = Dates.toMillis(
                                Dates.today().plusMonths(monthsValue.toLong())
                            ),
                            monthlyContribution = suggested,
                            status = if (currentValue >= targetValue && targetValue > 0)
                                GoalStatus.COMPLETED else GoalStatus.ACTIVE
                        )
                    )
                },
                enabled = name.isNotBlank() && targetValue > 0,
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
                ) { Text("מחיקת היעד") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContributeSheet(goal: GoalEntity, vm: MainViewModel, onDismiss: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    var amount by remember { mutableStateOf(goal.monthlyContribution.toLong().toString()) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("הפקדה ל${goal.name}", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "חסרים ${Money.format(goal.targetAmount - goal.currentAmount)} ליעד.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("סכום") }, prefix = { Text("₪") },
                singleLine = true, textStyle = MoneyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )

            if (accounts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("מאיזה חשבון", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { a ->
                        Pill(
                            text = "${a.name} · ${Money.compact(a.balance)}",
                            color = MaterialTheme.colorScheme.primary,
                            selected = accountId == a.id,
                            onClick = { accountId = a.id }
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val value = Money.parse(amount) ?: return@Button
                    vm.contributeToGoal(goal, value, accountId)
                    onDismiss()
                },
                enabled = (Money.parse(amount) ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("אישור הפקדה") }
        }
    }
}
