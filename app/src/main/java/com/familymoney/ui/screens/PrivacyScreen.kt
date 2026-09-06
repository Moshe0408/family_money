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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.AppContainer
import com.familymoney.data.sync.SyncState
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.theme.Success
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates

/** Section 42: what is connected, who can see it, what the AI processes. */
@Composable
fun PrivacyScreen(vm: MainViewModel, container: AppContainer, navController: NavHostController) {
    val accounts by vm.accounts.collectAsState()
    val cards by vm.cards.collectAsState()
    val members by vm.members.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()
    val auditLog by vm.auditLogFlow.collectAsState()

    DetailScaffold("🔐 פרטיות והרשאות", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionHeader("מה מחובר", emoji = "🔌") }
            item {
                SectionCard {
                    StatusLine(
                        "🏦", "חשבונות בנק",
                        if (accounts.isEmpty()) "לא מחובר" else "${accounts.size} חשבונות (ידני)",
                        accounts.isNotEmpty()
                    )
                    StatusLine(
                        "💳", "כרטיסי אשראי",
                        if (cards.isEmpty()) "לא מחובר" else "${cards.size} כרטיסים (ידני)",
                        cards.isNotEmpty()
                    )
                    StatusLine(
                        "🤖", "עוזר AI",
                        if (container.settings.aiEnabled && container.aiConfigured) "פעיל"
                        else "כבוי",
                        container.settings.aiEnabled && container.aiConfigured
                    )
                    StatusLine(
                        "👨‍👩‍👧", "שיתוף משפחתי",
                        when (val s = syncStatus) {
                            is SyncState.Connected -> "פעיל · ${s.memberCount} שותפים"
                            else -> "לא פעיל"
                        },
                        syncStatus is SyncState.Connected
                    )
                    StatusLine(
                        "🔒", "נעילת אפליקציה",
                        if (container.settings.biometricLock || container.settings.pinHash != null)
                            "מופעלת" else "כבויה",
                        container.settings.biometricLock || container.settings.pinHash != null
                    )
                }
            }

            item { SectionHeader("מה ה־AI מעבד", emoji = "🤖") }
            item {
                SectionCard {
                    Text(
                        "כשאתם שואלים את העוזר, נשלח אליו סיכום מצטבר בלבד:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "✅ סכומי הכנסות והוצאות לפי קטגוריה",
                        "✅ יתרות מצטברות ושווי כולל",
                        "✅ יעדים, תקציבים והוצאות קבועות",
                        "❌ שורות עסקאות בודדות",
                        "❌ מספרי חשבון או מספרי כרטיס",
                        "❌ שמות בתי עסק ספציפיים",
                        "❌ פרטים מזהים של בני המשפחה"
                    ).forEach {
                        Text(
                            it,
                            modifier = Modifier.padding(vertical = 3.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "אפשר לכבות את העוזר לחלוטין בהגדרות. כשהוא כבוי, שום נתון " +
                            "אינו יוצא מהמכשיר לצורך AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SectionHeader("איך המידע מאובטח", emoji = "🛡️") }
            item {
                SectionCard {
                    listOf(
                        "כל הנתונים נשמרים במסד נתונים מקומי במכשיר.",
                        "מפתחות ה־AI וקוד ה־PIN מוצפנים במאגר המפתחות של אנדרואיד.",
                        "האפליקציה אינה מבקשת ואינה שומרת סיסמאות בנק.",
                        "נשמרות רק 4 הספרות האחרונות של כרטיס — לעולם לא המספר המלא.",
                        "גיבוי ענן אוטומטי של אנדרואיד מושבת עבור נתונים פיננסיים.",
                        "כל התעבורה לשרת מוצפנת ב־TLS."
                    ).forEach {
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Text("• ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (members.isNotEmpty()) {
                item { SectionHeader("מי רואה את הנתונים", emoji = "👥") }
                item {
                    SectionCard {
                        members.forEach { m ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(m.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${m.role.he} · הצטרף ${Dates.formatDay(m.joinedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 41: audit log.
            if (auditLog.isNotEmpty()) {
                item { SectionHeader("יומן פעילות", emoji = "📋") }
                items(auditLog.take(25)) { entry ->
                    SectionCard(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    hebrewAction(entry.action),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (entry.detail.isNotBlank()) {
                                    Text(
                                        entry.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    entry.actor.ifBlank { "מערכת" },
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    Dates.formatDay(entry.at),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(emoji: String, label: String, status: String, active: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text(
            status,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun hebrewAction(action: String) = when (action) {
    "add_transaction" -> "עסקה נוספה"
    "delete_transaction" -> "עסקה נמחקה"
    "import_transactions" -> "ייבוא עסקאות"
    "goal_contribution" -> "הפקדה ליעד"
    else -> action
}
