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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.AppContainer
import com.familymoney.data.sync.SyncState
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.heroBrush
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/** Section 39-40: profile hub and the entry point to every settings area. */
@Composable
fun ProfileScreen(vm: MainViewModel, navController: NavHostController, container: AppContainer) {
    val profile by vm.profile.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val cards by vm.cards.collectAsState()
    val members by vm.members.collectAsState()
    val snapshot by vm.snapshot.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()

    val name = profile?.name.orEmpty().ifBlank { "משתמש" }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(heroBrush(), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                name.take(1),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        val familyName = profile?.familyName.orEmpty()
                        Text(
                            familyName.ifBlank { "חשבון אישי" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountTile("שותפים", members.size.coerceAtLeast(1).toString(), "👥", Modifier.weight(1f)) {
                    navController.navigate(Routes.FAMILY)
                }
                CountTile("חשבונות", accounts.size.toString(), "🏦", Modifier.weight(1f)) {
                    navController.navigate(Routes.ACCOUNTS)
                }
                CountTile("כרטיסים", cards.size.toString(), "💳", Modifier.weight(1f)) {
                    navController.navigate(Routes.CARDS)
                }
            }
        }

        item {
            SectionCard(onClick = { navController.navigate(Routes.NET_WORTH) }) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "שווי פיננסי כולל",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            Money.format(snapshot?.netWorth ?: 0.0),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            MenuGroup("הכסף שלי") {
                MenuRow("💰", "הכסף שלנו", "פירוט שווי, נזיל, חיסכון והשקעות") {
                    navController.navigate(Routes.NET_WORTH)
                }
                MenuRow("📊", "תקציב חודשי", "מעקב מול תקרות לפי קטגוריה") {
                    navController.navigate(Routes.BUDGET)
                }
                MenuRow("📄", "דוחות", "סיכום חודשי ושנתי") {
                    navController.navigate(Routes.REPORTS)
                }
                MenuRow("🔁", "הוצאות קבועות ומנויים", "מה יוצא אוטומטית כל חודש") {
                    navController.navigate(Routes.RECURRING)
                }
                MenuRow("🧮", "סימולטור ריבית דריבית", "כמה יהיה בעוד X שנים") {
                    navController.navigate(Routes.SIMULATOR)
                }
                MenuRow("🏦", "התחייבויות והלוואות", "משכנתא, הלוואות ותשלומים") {
                    navController.navigate(Routes.LIABILITIES)
                }
            }
        }

        item {
            MenuGroup("חיבורים") {
                MenuRow("🏦", "חשבונות בנק", "${accounts.size} חשבונות מחוברים") {
                    navController.navigate(Routes.ACCOUNTS)
                }
                MenuRow("💳", "כרטיסי אשראי", "${cards.size} כרטיסים") {
                    navController.navigate(Routes.CARDS)
                }
                MenuRow("📥", "ייבוא דף חשבון", "CSV מהבנק או מחברת האשראי") {
                    navController.navigate(Routes.IMPORT)
                }
                MenuRow("📱", "ארנק ותשלומים", "מצב NFC ותשלום מגע") {
                    navController.navigate(Routes.WALLET)
                }
            }
        }

        item {
            MenuGroup("משפחה ושיתוף") {
                MenuRow(
                    "👨‍👩‍👧‍👦",
                    "ניהול משפחה",
                    when (val s = syncStatus) {
                        is SyncState.Connected -> "מסונכרן · ${s.memberCount} שותפים"
                        is SyncState.Connecting -> "מתחבר…"
                        is SyncState.Failed -> "שגיאת סנכרון"
                        SyncState.Disabled ->
                            if (container.syncAvailable) "לא מחובר — הזמינו שותף"
                            else "לא זמין בגרסה זו"
                    }
                ) { navController.navigate(Routes.FAMILY) }
                MenuRow("🔐", "פרטיות והרשאות", "מי רואה מה, ומה ה־AI מעבד") {
                    navController.navigate(Routes.PRIVACY)
                }
            }
        }

        item {
            MenuGroup("כללי") {
                MenuRow("✨", "עוזר פיננסי AI", if (vm.aiConfigured) "פעיל" else "לא מוגדר מפתח") {
                    navController.navigate(Routes.AI)
                }
                MenuRow("🧠", "תובנות והמלצות", "מה המערכת מצאה החודש") {
                    navController.navigate(Routes.INSIGHTS)
                }
                MenuRow("⚙️", "הגדרות", "אבטחה, מראה, התראות, נתונים") {
                    navController.navigate(Routes.SETTINGS)
                }
                MenuRow("⬆️", "עדכון אפליקציה", "בדיקה והתקנה של גרסה חדשה") {
                    navController.navigate(Routes.UPDATE)
                }
            }
        }

        item {
            Text(
                "הכסף שלך. המשפחה שלך. ההחלטות שלך.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CountTile(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MoneySmall)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MenuGroup(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)) {
            content()
        }
    }
}

@Composable
fun MenuRow(emoji: String, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
