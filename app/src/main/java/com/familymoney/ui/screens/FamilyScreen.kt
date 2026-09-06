package com.familymoney.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.AppContainer
import com.familymoney.data.model.MemberRole
import com.familymoney.data.sync.SyncState
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.heroBrush
import com.familymoney.util.Qr
import com.familymoney.ui.vm.MainViewModel

/** Sections 2-3: create a family, invite a partner, manage permissions. */
@Composable
fun FamilyScreen(
    vm: MainViewModel,
    container: AppContainer,
    navController: NavHostController,
    pendingInviteCode: String?
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val profile by vm.profile.collectAsState()
    val members by vm.members.collectAsState()
    val syncStatus by vm.syncStatus.collectAsState()
    val busy by vm.busy.collectAsState()

    var familyNameInput by remember { mutableStateOf(profile?.familyName.orEmpty()) }
    var joinCode by remember { mutableStateOf(pendingInviteCode.orEmpty()) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(if (pendingInviteCode != null) "join" else "menu") }

    val connected = syncStatus is SyncState.Connected ||
        (container.settings.syncEnabled && container.settings.familyId != null)

    LaunchedEffect(pendingInviteCode) {
        if (pendingInviteCode != null) mode = "join"
    }

    DetailScaffold("👨‍👩‍👧‍👦 המשפחה שלנו", navController) {
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
                        Text("👨‍👩‍👧‍👦", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            profile?.familyName.orEmpty().ifBlank { "ניהול כספים משותף" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (val s = syncStatus) {
                                is SyncState.Connected ->
                                    "מסונכרן · ${s.memberCount} שותפים רואים את אותם נתונים"
                                is SyncState.Connecting -> "מתחבר…"
                                is SyncState.Failed -> "שגיאה: ${s.message}"
                                SyncState.Disabled ->
                                    if (container.syncAvailable)
                                        "צרו משפחה או הצטרפו לקיימת"
                                    else "סנכרון אינו זמין בגרסה זו"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (!container.syncAvailable) {
                item {
                    SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            "סנכרון לא מוגדר בבנייה זו",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "כדי להפעיל שיתוף בזמן אמת יש להוסיף google-services.json " +
                                "לתיקיית app ולבנות מחדש.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                return@LazyColumn
            }

            if (connected) {
                item { SectionHeader("שותפים", emoji = "👥") }
                item {
                    SectionCard {
                        if (members.isEmpty()) {
                            MemberRow(
                                name = profile?.name.orEmpty().ifBlank { "אני" },
                                role = MemberRole.OWNER,
                                isMe = true,
                                onRemove = null
                            )
                        }
                        members.forEach { m ->
                            MemberRow(
                                name = m.name,
                                role = m.role,
                                isMe = m.name == profile?.name,
                                onRemove = if (m.role != MemberRole.OWNER &&
                                    profile?.role == MemberRole.OWNER
                                ) {
                                    { vm.removeMember(m.userId) }
                                } else null
                            )
                        }
                    }
                }

                item { SectionHeader("הזמנת שותף", emoji = "🔗") }
                item {
                    SectionCard {
                        if (inviteCode == null) {
                            Text(
                                "צרו קוד הזמנה חד־פעמי ושלחו אותו לבן/בת הזוג. " +
                                    "לאחר ההצטרפות שניכם תראו את אותם נתונים בזמן אמת.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { vm.createInvite { inviteCode = it } },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("צור קישור הזמנה") }
                        } else {
                            InviteBlock(
                                code = inviteCode!!,
                                onCopy = {
                                    clipboard.setText(AnnotatedString(inviteLink(inviteCode!!)))
                                    vm.showToast("הקישור הועתק")
                                },
                                onShare = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "הוזמנת לנהל איתי את הכספים המשפחתיים.\n" +
                                                "קוד הצטרפות: ${inviteCode}\n" +
                                                inviteLink(inviteCode!!)
                                        )
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(intent, "שיתוף הזמנה")
                                        )
                                    }
                                },
                                onRevoke = {
                                    vm.revokeInvite(inviteCode!!)
                                    inviteCode = null
                                }
                            )
                        }
                    }
                }

                item { SectionHeader("סנכרון", emoji = "🔄") }
                item {
                    SectionCard {
                        Text(
                            when (val s = syncStatus) {
                                is SyncState.Connected -> "עודכן לאחרונה: " +
                                    com.familymoney.util.Dates.formatDay(s.lastSyncAt)
                                else -> "טרם סונכרן"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { vm.syncNow() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (busy) "מסנכרן…" else "סנכרן עכשיו") }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { vm.leaveFamily() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("יציאה מהמשפחה") }
                    }
                }
            } else {
                when (mode) {
                    "create" -> item {
                        SectionCard {
                            Text("יצירת משפחה", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = familyNameInput,
                                onValueChange = { familyNameInput = it },
                                label = { Text("שם המשפחה") },
                                placeholder = { Text("לדוגמה: משפחת איסקוב") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    vm.createFamily(familyNameInput.trim()) { code ->
                                        inviteCode = code
                                    }
                                },
                                enabled = familyNameInput.isNotBlank() && !busy,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(if (busy) "יוצר…" else "צור משפחה") }
                            Spacer(Modifier.height(6.dp))
                            TextButton(
                                onClick = { mode = "menu" },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("חזרה") }
                        }
                    }

                    "join" -> item {
                        SectionCard {
                            Text("הצטרפות למשפחה", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "הזינו את קוד ההזמנה שקיבלתם.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase().take(8) },
                                label = { Text("קוד הזמנה") },
                                placeholder = { Text("ABCD1234") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "⚠️ ההצטרפות תחליף את הנתונים המקומיים בנתוני המשפחה.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { vm.joinFamily(joinCode) { } },
                                enabled = joinCode.length >= 6 && !busy,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text(if (busy) "מצטרף…" else "הצטרף למשפחה") }
                            Spacer(Modifier.height(6.dp))
                            TextButton(
                                onClick = { mode = "menu" },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("חזרה") }
                        }
                    }

                    else -> {
                        item {
                            SectionCard(onClick = { mode = "create" }) {
                                Text("➕ יצירת משפחה", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "אתם הבעלים. תוכלו להזמין שותף ולנהל הרשאות.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item {
                            SectionCard(onClick = { mode = "join" }) {
                                Text("🔗 הצטרפות עם קוד", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "קיבלתם קוד הזמנה מבן/בת הזוג? הצטרפו כאן.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (inviteCode != null) {
                    item {
                        SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(
                                "המשפחה נוצרה 🎉",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(12.dp))
                            InviteBlock(
                                code = inviteCode!!,
                                onCopy = {
                                    clipboard.setText(AnnotatedString(inviteLink(inviteCode!!)))
                                    vm.showToast("הקישור הועתק")
                                },
                                onShare = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "הוזמנת לנהל איתי את הכספים המשפחתיים.\n" +
                                                "קוד הצטרפות: ${inviteCode}\n" +
                                                inviteLink(inviteCode!!)
                                        )
                                    }
                                    runCatching {
                                        context.startActivity(
                                            Intent.createChooser(intent, "שיתוף הזמנה")
                                        )
                                    }
                                },
                                onRevoke = null
                            )
                        }
                    }
                }
            }

            item { SectionHeader("איך זה עובד", emoji = "🔐") }
            item {
                SectionCard {
                    listOf(
                        "לכל שותף יש זהות נפרדת במכשיר שלו.",
                        "הקישור הוא הזמנה בלבד — הוא אינו מנגנון ההתחברות.",
                        "אפשר לבטל קוד הזמנה או להסיר שותף בכל רגע.",
                        "הנתונים מוצפנים בתעבורה, וההרשאות נאכפות בשרת.",
                        "רק מי שהצטרף עם קוד תקף רואה את נתוני המשפחה."
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
        }
    }
}

private fun inviteLink(code: String) = "familymoney://join?code=$code"

@Composable
private fun InviteBlock(
    code: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRevoke: (() -> Unit)?
) {
    val qr = remember(code) { Qr.generate(inviteLink(code), 480) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (qr != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                modifier = Modifier.padding(4.dp)
            ) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "קוד QR להזמנה",
                    modifier = Modifier.size(190.dp).padding(10.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                code,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("שתף קישור") }
            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) { Text("העתק") }
        }

        if (onRevoke != null) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onRevoke,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("בטל את הקוד") }
        }
    }
}

@Composable
private fun MemberRow(
    name: String,
    role: MemberRole,
    isMe: Boolean,
    onRemove: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.take(1).ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name.ifBlank { "שותף" } + if (isMe) " (אני)" else "",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                role.he,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onRemove != null) {
            TextButton(
                onClick = onRemove,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("הסר") }
        }
    }
}
