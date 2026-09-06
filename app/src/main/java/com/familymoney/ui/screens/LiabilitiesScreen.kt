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
import com.familymoney.data.db.LiabilityEntity
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.StatTile
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/** Section 35: loans, mortgage and monthly commitments. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiabilitiesScreen(vm: MainViewModel, navController: NavHostController) {
    val liabilities by vm.liabilities.collectAsState()
    var editing by remember { mutableStateOf<LiabilityEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    val totalRemaining = liabilities.sumOf { it.remainingAmount }
    val totalMonthly = liabilities.sumOf { it.monthlyPayment }

    DetailScaffold(
        title = "🏦 התחייבויות",
        navController = navController,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "הוספה")
            }
        }
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (liabilities.isEmpty()) {
                item {
                    EmptyState(
                        "🏦",
                        "אין התחייבויות",
                        "הוסיפו משכנתא, הלוואה או תשלומים כדי שהשווי הפיננסי יהיה מדויק."
                    ) {
                        Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("הוספת התחייבות")
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "יתרה לתשלום", Money.format(totalRemaining), "📉",
                        MaterialTheme.colorScheme.error, Modifier.weight(1f)
                    )
                    StatTile(
                        "תשלום חודשי", Money.format(totalMonthly), "📅",
                        Warning, Modifier.weight(1f)
                    )
                }
            }

            items(liabilities, key = { it.id }) { l ->
                val paid = (l.totalAmount - l.remainingAmount).coerceAtLeast(0.0)
                val progress = if (l.totalAmount <= 0) 0f else (paid / l.totalAmount).toFloat()

                SectionCard(onClick = { editing = l }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(l.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${Money.format(l.monthlyPayment)} לחודש" +
                                    if (l.interestRatePercent > 0)
                                        " · ריבית ${Money.percent(l.interestRatePercent)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            Money.format(l.remainingAmount),
                            style = MoneySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(progress, colorOverride = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "שולמו ${Money.format(paid)} מתוך ${Money.format(l.totalAmount)} " +
                            "(${Money.percent(progress * 100.0)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (creating) {
        LiabilityEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { vm.saveLiability(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { l ->
        LiabilityEditorSheet(
            initial = l,
            onDismiss = { editing = null },
            onSave = { vm.saveLiability(it); editing = null },
            onDelete = { vm.deleteLiability(l); editing = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiabilityEditorSheet(
    initial: LiabilityEntity?,
    onDismiss: () -> Unit,
    onSave: (LiabilityEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var total by remember { mutableStateOf(trimNumber(initial?.totalAmount ?: 0.0)) }
    var remaining by remember { mutableStateOf(trimNumber(initial?.remainingAmount ?: 0.0)) }
    var monthly by remember { mutableStateOf(trimNumber(initial?.monthlyPayment ?: 0.0)) }
    var interest by remember { mutableStateOf(trimNumber(initial?.interestRatePercent ?: 0.0)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                if (initial == null) "התחייבות חדשה" else "עריכה",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם") },
                placeholder = { Text("לדוגמה: משכנתא") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = total,
                    onValueChange = { total = it.filter { c -> c.isDigit() } },
                    label = { Text("סכום מקורי") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = remaining,
                    onValueChange = { remaining = it.filter { c -> c.isDigit() } },
                    label = { Text("יתרה") }, prefix = { Text("₪") }, singleLine = true,
                    textStyle = MoneyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = monthly,
                    onValueChange = { monthly = it.filter { c -> c.isDigit() } },
                    label = { Text("תשלום חודשי") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = interest,
                    onValueChange = { interest = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("ריבית %") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val totalValue = Money.parse(total) ?: 0.0
                    val remainingValue = Money.parse(remaining) ?: 0.0
                    onSave(
                        (initial ?: LiabilityEntity(
                            name = "", totalAmount = 0.0, remainingAmount = 0.0, monthlyPayment = 0.0
                        )).copy(
                            name = name.trim().ifBlank { "התחייבות" },
                            totalAmount = maxOf(totalValue, remainingValue),
                            remainingAmount = remainingValue,
                            monthlyPayment = Money.parse(monthly) ?: 0.0,
                            interestRatePercent = Money.parse(interest) ?: 0.0
                        )
                    )
                },
                enabled = name.isNotBlank() && (Money.parse(remaining) ?: 0.0) > 0,
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
