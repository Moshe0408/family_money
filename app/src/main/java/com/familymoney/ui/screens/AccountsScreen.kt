package com.familymoney.ui.screens

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
import com.familymoney.data.bank.Providers
import com.familymoney.data.db.AccountEntity
import com.familymoney.data.model.AccountType
import com.familymoney.data.model.SyncStatus
import com.familymoney.ui.components.CategoryAvatar
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/** Sections 12-13: bank accounts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(vm: MainViewModel, navController: NavHostController) {
    val accounts by vm.accounts.collectAsState()
    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showConnect by remember { mutableStateOf(false) }

    val total = accounts.sumOf { it.balance }

    DetailScaffold(
        title = "🏦 חשבונות",
        navController = navController,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "חשבון חדש")
            }
        }
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        "🏦",
                        "אין חשבונות",
                        "הוסיפו חשבון ידנית או ייבאו דף חשבון כדי לראות תמונה מלאה."
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                                Text("הוספה ידנית")
                            }
                            Button(
                                onClick = { navController.navigate(Routes.IMPORT) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) { Text("ייבוא CSV") }
                        }
                    }
                }
            } else {
                item {
                    SectionCard {
                        Text(
                            "סך היתרות",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(Money.format(total), style = MaterialTheme.typography.displaySmall)
                    }
                }

                AccountType.entries.forEach { type ->
                    val group = accounts.filter { it.type == type }
                    if (group.isNotEmpty()) {
                        item(key = "hdr_${type.name}") { SectionHeader(type.he, emoji = emojiFor(type)) }
                        items(group, key = { it.id }) { account ->
                            AccountRow(account) { editing = account }
                        }
                    }
                }
            }

            item { SectionHeader("חיבור אוטומטי", emoji = "🔗") }
            item {
                SectionCard(onClick = { showConnect = true }) {
                    Text("חיבור חשבון בנק", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "סנכרון אוטומטי של יתרות ותנועות דורש ספק Open Banking מורשה. " +
                            "הארכיטקטורה מוכנה לחיבור — לחצו לפרטים.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SectionCard(onClick = { navController.navigate(Routes.IMPORT) }) {
                    Text("📥 ייבוא דף חשבון", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "עובד היום: הורידו CSV מהבנק או מחברת האשראי, והמערכת תסווג את " +
                            "העסקאות אוטומטית.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showConnect) {
        ConnectProviderSheet(onDismiss = { showConnect = false })
    }

    if (creating) {
        AccountEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { vm.saveAccount(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { account ->
        AccountEditorSheet(
            initial = account,
            onDismiss = { editing = null },
            onSave = { vm.saveAccount(it); editing = null },
            onDelete = { vm.deleteAccount(account); editing = null }
        )
    }
}

private fun emojiFor(type: AccountType) = when (type) {
    AccountType.CHECKING -> "🏦"
    AccountType.SAVINGS -> "🐷"
    AccountType.DEPOSIT -> "🔒"
    AccountType.INVESTMENT -> "📈"
    AccountType.CHILD -> "🧒"
    AccountType.CASH -> "💵"
}

@Composable
private fun AccountRow(account: AccountEntity, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(emojiFor(account.type), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        if (account.provider.isNotBlank()) append(account.provider)
                        if (account.ownerName.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(account.ownerName)
                        }
                        if (account.interestRatePercent > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("ריבית ${Money.percent(account.interestRatePercent)}")
                        }
                    }.ifBlank { account.type.he },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(account.balance), style = MoneySmall)
                Text(
                    when (account.status) {
                        SyncStatus.CONNECTED -> "מסונכרן"
                        SyncStatus.ERROR -> "שגיאה"
                        SyncStatus.EXPIRED -> "הרשאה פגה"
                        SyncStatus.LOCAL -> "ידני"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (account.status) {
                        SyncStatus.CONNECTED -> Success
                        SyncStatus.ERROR, SyncStatus.EXPIRED -> MaterialTheme.colorScheme.error
                        SyncStatus.LOCAL -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditorSheet(
    initial: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var provider by remember { mutableStateOf(initial?.provider ?: "") }
    var owner by remember { mutableStateOf(initial?.ownerName ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: AccountType.CHECKING) }
    var balance by remember { mutableStateOf(trimNumber(initial?.balance ?: 0.0)) }
    var interest by remember { mutableStateOf(trimNumber(initial?.interestRatePercent ?: 0.0)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (initial == null) "חשבון חדש" else "עריכת חשבון",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(18.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountType.entries.toList()) { t ->
                    Pill(
                        "${emojiFor(t)} ${t.he}",
                        MaterialTheme.colorScheme.primary,
                        selected = type == t
                    ) { type = t }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם החשבון") },
                placeholder = { Text("לדוגמה: עו\"ש משותף") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text("בנק", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Providers.banks) { b ->
                    Pill(
                        "${b.emoji} ${b.name}",
                        MaterialTheme.colorScheme.secondary,
                        selected = provider == b.name
                    ) { provider = if (provider == b.name) "" else b.name }
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = owner, onValueChange = { owner = it },
                label = { Text("בעל החשבון (אופציונלי)") },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("יתרה") }, prefix = { Text("₪") }, singleLine = true,
                    textStyle = MoneyMedium,
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
                    onSave(
                        (initial ?: AccountEntity(name = "")).copy(
                            name = name.trim().ifBlank { type.he },
                            provider = provider,
                            ownerName = owner.trim(),
                            type = type,
                            balance = Money.parse(balance) ?: 0.0,
                            interestRatePercent = Money.parse(interest) ?: 0.0
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
                ) { Text("מחיקת החשבון") }
            }
        }
    }
}

/** Section 12: the connect flow, honest about what it needs to go live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectProviderSheet(onDismiss: () -> Unit, cardMode: Boolean = false) {
    val providers = if (cardMode) Providers.cardIssuers else Providers.banks
    var selected by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (cardMode) "חיבור כרטיס אשראי" else "חיבור חשבון בנק",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))

            providers.forEach { p ->
                SectionCard(
                    containerColor = if (selected == p.id)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                    onClick = { selected = p.id }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Text("מה נדרש כדי להפעיל", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                listOf(
                    "חיבור לספק שירות מידע פיננסי מורשה (רישיון בנק ישראל).",
                    "הסכמת המשתמש דרך מסך ההזדהות של הבנק עצמו.",
                    "האפליקציה לעולם אינה מבקשת או שומרת סיסמאות בנק.",
                    "ההרשאה ניתנת לביטול בכל רגע ופגה אוטומטית."
                ).forEach {
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "עד להשלמת החיבור, השתמשו בייבוא CSV או בהזנה ידנית — כל שאר " +
                        "יכולות האפליקציה עובדות באופן מלא.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("הבנתי") }
        }
    }
}
