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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.data.bank.Providers
import com.familymoney.data.db.CardEntity
import com.familymoney.data.model.CardType
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.StatTile
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money

/** Sections 14-15: credit cards, limits and upcoming charges. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(vm: MainViewModel, navController: NavHostController) {
    val cards by vm.cards.collectAsState()
    var editing by remember { mutableStateOf<CardEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showConnect by remember { mutableStateOf(false) }

    val totalNextCharge = cards.sumOf { it.nextChargeAmount }
    val totalLimit = cards.sumOf { it.creditLimit }
    val totalUsed = cards.sumOf { it.usedCredit }

    DetailScaffold(
        title = "💳 כרטיסי אשראי",
        navController = navController,
        actions = {
            IconButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "כרטיס חדש")
            }
        }
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (cards.isEmpty()) {
                item {
                    EmptyState(
                        "💳",
                        "אין כרטיסים",
                        "הוסיפו כרטיס כדי לעקוב אחרי החיוב הקרוב, המסגרת והניצול."
                    ) {
                        Button(onClick = { creating = true }, shape = RoundedCornerShape(14.dp)) {
                            Text("הוספת כרטיס")
                        }
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            "חיוב קרוב", Money.format(totalNextCharge), "📅",
                            MaterialTheme.colorScheme.error, Modifier.weight(1f)
                        )
                        StatTile(
                            "ניצול מסגרת",
                            if (totalLimit > 0) Money.percent(totalUsed / totalLimit * 100) else "—",
                            "📊", Warning, Modifier.weight(1f)
                        )
                    }
                }

                items(cards, key = { it.id }) { card ->
                    CreditCardTile(card) { editing = card }
                }
            }

            item {
                SectionCard(onClick = { showConnect = true }) {
                    Text("🔗 חיבור אוטומטי לחברת אשראי", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "סנכרון עסקאות וחיובים אוטומטי דורש חיבור לספק מורשה. לחצו לפרטים.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SectionCard(onClick = { navController.navigate(Routes.WALLET) }) {
                    Text("📱 תשלום דרך האפליקציה", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "מצב NFC במכשיר, ומה נדרש כדי לבצע תשלום מגע. לחצו לפרטים.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showConnect) {
        ConnectProviderSheet(onDismiss = { showConnect = false }, cardMode = true)
    }

    if (creating) {
        CardEditorSheet(
            initial = null,
            onDismiss = { creating = false },
            onSave = { vm.saveCard(it); creating = false },
            onDelete = null
        )
    }

    editing?.let { card ->
        CardEditorSheet(
            initial = card,
            onDismiss = { editing = null },
            onSave = { vm.saveCard(it); editing = null },
            onDelete = { vm.deleteCard(card); editing = null }
        )
    }
}

@Composable
private fun CreditCardTile(card: CardEntity, onClick: () -> Unit) {
    val usage = if (card.creditLimit <= 0) 0f else (card.usedCredit / card.creditLimit).toFloat()
    val gradient = Brush.linearGradient(
        listOf(
            CategoryPalette[card.last4.hashCode().mod(CategoryPalette.size)],
            CategoryPalette[(card.last4.hashCode() + 5).mod(CategoryPalette.size)]
        )
    )

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(22.dp))
                .clickable(onClick = onClick)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        card.provider,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        card.type.he,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    "•••• •••• •••• ${card.last4}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "חיוב קרוב",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            Money.format(card.nextChargeAmount),
                            style = MoneySmall,
                            color = Color.White
                        )
                    }
                    if (card.ownerName.isNotBlank()) {
                        Text(
                            card.ownerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        SectionCard(onClick = onClick) {
            if (card.nextChargeDate > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "תאריך חיוב",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(Dates.formatDay(card.nextChargeDate),
                        style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(10.dp))
            }
            if (card.creditLimit > 0) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "מסגרת",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${Money.format(card.usedCredit)} / ${Money.format(card.creditLimit)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(10.dp))
                ProgressBar(usage)
                Spacer(Modifier.height(6.dp))
                Text(
                    "נוצל ${Money.percent(usage * 100.0)} · פנוי " +
                        Money.format(card.creditLimit - card.usedCredit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardEditorSheet(
    initial: CardEntity?,
    onDismiss: () -> Unit,
    onSave: (CardEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var provider by remember { mutableStateOf(initial?.provider ?: "") }
    var last4 by remember { mutableStateOf(initial?.last4 ?: "") }
    var owner by remember { mutableStateOf(initial?.ownerName ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: CardType.CREDIT) }
    var limit by remember { mutableStateOf(trimNumber(initial?.creditLimit ?: 0.0)) }
    var used by remember { mutableStateOf(trimNumber(initial?.usedCredit ?: 0.0)) }
    var nextCharge by remember { mutableStateOf(trimNumber(initial?.nextChargeAmount ?: 0.0)) }
    var chargeDay by remember {
        mutableStateOf(
            initial?.nextChargeDate?.takeIf { it > 0 }
                ?.let { Dates.toLocalDate(it).dayOfMonth.toString() } ?: "10"
        )
    }

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
                if (initial == null) "כרטיס חדש" else "עריכת כרטיס",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "מוזנות רק 4 הספרות האחרונות. האפליקציה אינה שומרת מספר כרטיס מלא.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CardType.entries.toList()) { t ->
                    Pill(t.he, MaterialTheme.colorScheme.secondary, selected = type == t) {
                        type = t
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("חברת אשראי", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Providers.cardIssuers) { p ->
                    Pill(p.name, MaterialTheme.colorScheme.primary, selected = provider == p.name) {
                        provider = p.name
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = provider, onValueChange = { provider = it },
                label = { Text("שם החברה") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = last4,
                    onValueChange = { last4 = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("4 ספרות אחרונות") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = owner, onValueChange = { owner = it },
                    label = { Text("בעל הכרטיס") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1.2f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter { c -> c.isDigit() } },
                    label = { Text("מסגרת") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = used,
                    onValueChange = { used = it.filter { c -> c.isDigit() } },
                    label = { Text("נוצל") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nextCharge,
                    onValueChange = { nextCharge = it.filter { c -> c.isDigit() } },
                    label = { Text("חיוב קרוב") }, prefix = { Text("₪") }, singleLine = true,
                    textStyle = MoneyMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = chargeDay,
                    onValueChange = { chargeDay = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("יום חיוב") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    onSave(
                        (initial ?: CardEntity()).copy(
                            provider = provider.trim().ifBlank { "כרטיס" },
                            last4 = last4,
                            ownerName = owner.trim(),
                            type = type,
                            creditLimit = Money.parse(limit) ?: 0.0,
                            usedCredit = Money.parse(used) ?: 0.0,
                            nextChargeAmount = Money.parse(nextCharge) ?: 0.0,
                            nextChargeDate = chargeDay.toIntOrNull()
                                ?.let { Dates.nextOccurrence(it) } ?: 0L
                        )
                    )
                },
                enabled = provider.isNotBlank() && last4.length == 4,
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
                ) { Text("הסרת הכרטיס") }
            }
        }
    }
}
