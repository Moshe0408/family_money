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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.familymoney.data.model.ExpenseCategory
import com.familymoney.data.model.TxSource
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.components.StatTile
import com.familymoney.ui.theme.LocalPalette
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import java.time.YearMonth

/**
 * Targeted data removal.
 *
 * A single "wipe everything" button is a blunt instrument — the common needs
 * are undoing a bad import, clearing the demo data, or dropping one month.
 * Every option states exactly how many rows it will remove before asking.
 */
@Composable
fun DataScreen(vm: MainViewModel, navController: NavHostController) {
    val transactions by vm.transactions.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val goals by vm.goals.collectAsState()
    val investments by vm.investments.collectAsState()

    var pending by remember { mutableStateOf<PendingDelete?>(null) }

    val imported = transactions.count { it.source == TxSource.IMPORT }
    val thisMonth = YearMonth.now()
    val thisMonthCount = transactions.count {
        it.date in Dates.monthStart(thisMonth)..Dates.monthEnd(thisMonth)
    }
    val lastMonth = thisMonth.minusMonths(1)
    val lastMonthCount = transactions.count {
        it.date in Dates.monthStart(lastMonth)..Dates.monthEnd(lastMonth)
    }
    val byCategory = transactions
        .filter { it.type == com.familymoney.data.model.TxType.EXPENSE }
        .groupingBy { it.category }.eachCount()
        .entries.sortedByDescending { it.value }

    DetailScaffold("🗂️ ניהול נתונים", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "עסקאות", transactions.size.toString(), "🧾",
                        MaterialTheme.colorScheme.primary, Modifier.weight(1f)
                    )
                    StatTile(
                        "חשבונות", accounts.size.toString(), "🏦",
                        MaterialTheme.colorScheme.secondary, Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        "יעדים", goals.size.toString(), "🎯",
                        LocalPalette.current.positive, Modifier.weight(1f)
                    )
                    StatTile(
                        "השקעות", investments.size.toString(), "📈",
                        LocalPalette.current.warning, Modifier.weight(1f)
                    )
                }
            }

            item { SectionHeader("מחיקה לפי תקופה", emoji = "📅") }
            item {
                SectionCard {
                    DeleteRow(
                        "עסקאות החודש",
                        "$thisMonthCount עסקאות ב${Dates.hebrewMonth(thisMonth)}",
                        enabled = thisMonthCount > 0
                    ) {
                        pending = PendingDelete(
                            title = "למחוק את עסקאות ${Dates.hebrewMonth(thisMonth)}?",
                            body = "$thisMonthCount עסקאות יימחקו. חשבונות, יעדים " +
                                "ותקציבים יישארו.",
                            action = {
                                vm.deleteRange(
                                    Dates.monthStart(thisMonth),
                                    Dates.monthEnd(thisMonth)
                                )
                            }
                        )
                    }
                    DeleteRow(
                        "עסקאות החודש שעבר",
                        "$lastMonthCount עסקאות ב${Dates.hebrewMonth(lastMonth)}",
                        enabled = lastMonthCount > 0
                    ) {
                        pending = PendingDelete(
                            title = "למחוק את עסקאות ${Dates.hebrewMonth(lastMonth)}?",
                            body = "$lastMonthCount עסקאות יימחקו.",
                            action = {
                                vm.deleteRange(
                                    Dates.monthStart(lastMonth),
                                    Dates.monthEnd(lastMonth)
                                )
                            }
                        )
                    }
                    DeleteRow(
                        "עסקאות ישנות משנה",
                        "כל מה שלפני ${Dates.hebrewMonth(thisMonth.minusMonths(12))}",
                        enabled = transactions.any { it.date < Dates.monthStart(thisMonth.minusMonths(12)) }
                    ) {
                        val cutoff = Dates.monthStart(thisMonth.minusMonths(12))
                        val n = transactions.count { it.date < cutoff }
                        pending = PendingDelete(
                            title = "למחוק עסקאות ישנות?",
                            body = "$n עסקאות מלפני יותר משנה יימחקו.",
                            action = { vm.deleteRange(0L, cutoff) }
                        )
                    }
                }
            }

            item { SectionHeader("מחיקה לפי מקור", emoji = "📥") }
            item {
                SectionCard {
                    DeleteRow(
                        "עסקאות מיובאות",
                        if (imported > 0) "$imported עסקאות שיובאו מקובץ או מ־PDF"
                        else "אין עסקאות מיובאות",
                        enabled = imported > 0
                    ) {
                        pending = PendingDelete(
                            title = "למחוק עסקאות מיובאות?",
                            body = "$imported עסקאות שהגיעו מייבוא יימחקו. " +
                                "עסקאות שהזנתם ידנית יישארו.",
                            action = { vm.deleteImported() }
                        )
                    }
                    DeleteRow(
                        "נתוני הדגמה",
                        "מוחק הכול ומאפס לחשבון ריק",
                        enabled = transactions.isNotEmpty()
                    ) {
                        pending = PendingDelete(
                            title = "למחוק את נתוני ההדגמה?",
                            body = "כל העסקאות, החשבונות, הכרטיסים, היעדים וההשקעות " +
                                "יימחקו. תתחילו מחשבון ריק.",
                            action = { vm.clearAllData() }
                        )
                    }
                }
            }

            if (byCategory.isNotEmpty()) {
                item { SectionHeader("מחיקה לפי קטגוריה", emoji = "🏷️") }
                item {
                    SectionCard {
                        Text(
                            "הקישו על קטגוריה כדי למחוק את כל העסקאות שלה.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(byCategory) { (key, count) ->
                                val cat = ExpenseCategory.fromKey(key)
                                Pill(
                                    "${cat.emoji} ${cat.he} ($count)",
                                    MaterialTheme.colorScheme.error
                                ) {
                                    pending = PendingDelete(
                                        title = "למחוק את ${cat.he}?",
                                        body = "$count עסקאות בקטגוריה זו יימחקו.",
                                        action = { vm.deleteCategory(key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("מחיקה מלאה", emoji = "⚠️") }
            item {
                SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        "מחיקת כל העסקאות",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "מוחק את ${transactions.size} העסקאות וההוצאות הקבועות, " +
                            "אך משאיר את החשבונות, הכרטיסים, היעדים וההשקעות.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            pending = PendingDelete(
                                title = "למחוק את כל העסקאות?",
                                body = "${transactions.size} עסקאות יימחקו. " +
                                    "ההגדרות, החשבונות והיעדים יישארו.",
                                action = { vm.deleteAllTransactions() }
                            )
                        },
                        enabled = transactions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text("מחק את כל העסקאות") }
                }
            }

            item {
                com.familymoney.ui.components.DisclaimerNote(
                    "מחיקה היא סופית ואינה ניתנת לשחזור. אם השיתוף המשפחתי פעיל, " +
                        "המחיקה תסונכרן גם למכשיר של השותף."
                )
            }
        }
    }

    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(p.title) },
            text = { Text(p.body) },
            confirmButton = {
                TextButton(
                    onClick = { p.action(); pending = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("מחק") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("ביטול") }
            }
        )
    }
}

private data class PendingDelete(
    val title: String,
    val body: String,
    val action: () -> Unit
)

@Composable
private fun DeleteRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        TextButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) { Text("מחק") }
    }
}
