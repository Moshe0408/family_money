package com.familymoney.ui.screens

import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.heroBrush
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money

/**
 * Sections 16-17.
 *
 * Contactless payment is deliberately not implemented. Emulating a payment card
 * over NFC requires an HCE service certified by the card schemes, a tokenisation
 * provider (TSP), an issuer agreement and PCI scope — none of which an app can
 * obtain on its own. This screen therefore reports NFC capability honestly and
 * hands off to the certified wallet already on the device.
 */
@Composable
fun WalletScreen(vm: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val cards by vm.cards.collectAsState()

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val hasNfc = nfcAdapter != null
    val nfcEnabled = nfcAdapter?.isEnabled == true
    val hasHceFeature = remember {
        context.packageManager.hasSystemFeature("android.hardware.nfc.hce")
    }

    DetailScaffold("📱 ארנק ותשלומים", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(heroBrush(), RoundedCornerShape(24.dp))
                        .padding(22.dp)
                ) {
                    Column {
                        Text("💳", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "מצב תשלום מגע",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                !hasNfc -> "במכשיר זה אין חומרת NFC"
                                !nfcEnabled -> "NFC קיים אך מכובה"
                                hasHceFeature -> "NFC פעיל, והמכשיר תומך ב־HCE"
                                else -> "NFC פעיל"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            item { SectionHeader("בדיקת המכשיר", emoji = "🔍") }
            item {
                SectionCard {
                    CapabilityRow("חומרת NFC", hasNfc)
                    CapabilityRow("NFC מופעל", nfcEnabled)
                    CapabilityRow("תמיכה ב־HCE (הדמיית כרטיס)", hasHceFeature)
                    CapabilityRow("כרטיס מוגדר באפליקציה", cards.isNotEmpty())
                    CapabilityRow("תשתית תשלום מוסמכת", false)

                    if (hasNfc && !nfcEnabled) {
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("הפעלת NFC בהגדרות") }
                    }
                }
            }

            item { SectionHeader("למה אי אפשר לשלם מכאן", emoji = "ℹ️") }
            item {
                SectionCard {
                    Text(
                        "תשלום מגע אמיתי אינו עניין של NFC בלבד. כדי שכרטיס יעבוד " +
                            "מול קורא בחנות נדרשים:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "טוקניזציה (TSP) — הכרטיס האמיתי מוחלף באסימון ייעודי למכשיר.",
                        "הסמכה מול Visa / Mastercard לפי תקן EMV Contactless.",
                        "הסכם עם המנפיק (חברת האשראי) שמאשר את הארנק.",
                        "עמידה בתקן PCI DSS ובדרישות רגולטוריות בישראל.",
                        "Secure Element או HCE מוסמך לאחסון האסימון."
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
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "אלה תהליכי הסמכה מסחריים שנמשכים חודשים ואינם ניתנים למימוש " +
                            "בקוד בלבד. לכן האפליקציה אינה שומרת פרטי כרטיס מלאים ואינה " +
                            "מנסה להדמות כרטיס תשלום.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { SectionHeader("מה כן אפשר", emoji = "✅") }
            item {
                SectionCard {
                    Text("שלמו דרך הארנק המוסמך שכבר במכשיר", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Google Wallet כבר עבר את כל ההסמכות. הוסיפו שם את הכרטיס, " +
                            "ורשמו כאן את ההוצאה — או ייבאו את דף האשראי בסוף החודש.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val intent = context.packageManager
                                .getLaunchIntentForPackage("com.google.android.apps.walletnfcrel")
                                ?: Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://play.google.com/store/apps/details?id=" +
                                            "com.google.android.apps.walletnfcrel"
                                    )
                                )
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("פתח Google Wallet") }
                }
            }

            if (cards.isNotEmpty()) {
                item { SectionHeader("הכרטיסים שלכם", emoji = "💳") }
                item {
                    SectionCard {
                        cards.forEach { card ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💳", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${card.provider} •••• ${card.last4}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "מעקב בלבד · ללא יכולת תשלום",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(Money.format(card.nextChargeAmount), style = MoneySmall)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("מפת הדרכים", emoji = "🗺️") }
            item {
                SectionCard {
                    RoadmapRow("V1", "ניהול כסף, חיבור חשבונות, מעקב עסקאות, המלצות", true)
                    RoadmapRow("V2", "טוקניזציה, ארנק, תשלום NFC, רישום אוטומטי של העסקה", false)
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, available: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (available) "✅" else "❌", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (available) "זמין" else "לא זמין",
            style = MaterialTheme.typography.labelMedium,
            color = if (available) Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoadmapRow(phase: String, description: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .background(
                    if (done) Success else Warning,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(phase, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                if (done) "פעיל" else "עתידי",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
