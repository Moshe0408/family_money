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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.AppContainer
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.Pill
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Money
import com.familymoney.util.Security

/** Section 40-41: settings, security and data management. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, container: AppContainer, navController: NavHostController) {
    val settings = container.settings
    val profile by vm.profile.collectAsState()
    val themeMode by settings.darkMode.collectAsState()

    var biometric by remember { mutableStateOf(settings.biometricLock) }
    var notifications by remember { mutableStateOf(settings.notificationsEnabled) }
    var aiEnabled by remember { mutableStateOf(settings.aiEnabled) }
    var autoLock by remember { mutableStateOf(settings.autoLockMinutes) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showAiKeySheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    DetailScaffold("⚙️ הגדרות", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MenuGroup("פרופיל") {
                    MenuRow(
                        "👤", "פרטים אישיים",
                        "${profile?.name.orEmpty().ifBlank { "לא הוגדר" }} · " +
                            "כרית ${Money.format(profile?.safetyBuffer ?: 0.0)}"
                    ) { showProfileSheet = true }
                    MenuRow("👨‍👩‍👧‍👦", "ניהול משפחה", profile?.familyName.orEmpty().ifBlank { "לא מחובר" }) {
                        navController.navigate(Routes.FAMILY)
                    }
                }
            }

            item {
                MenuGroup("מראה") {
                    Column(Modifier.padding(12.dp)) {
                        Text("ערכת נושא", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "system" to "לפי המערכת",
                                "light" to "☀️ בהיר",
                                "dark" to "🌙 כהה"
                            ).forEach { (key, label) ->
                                Pill(
                                    label,
                                    MaterialTheme.colorScheme.primary,
                                    selected = themeMode == key
                                ) { settings.setThemeMode(key) }
                            }
                        }
                    }
                }
            }

            item {
                MenuGroup("אבטחה") {
                    ToggleRow(
                        "🔐", "נעילה ביומטרית",
                        "טביעת אצבע או זיהוי פנים בפתיחת האפליקציה",
                        biometric
                    ) {
                        biometric = it
                        settings.biometricLock = it
                    }
                    MenuRow(
                        "🔢", "קוד PIN",
                        if (settings.pinHash != null) "מוגדר · לחצו לשינוי או הסרה"
                        else "לא מוגדר"
                    ) { showPinDialog = true }
                    Column(Modifier.padding(12.dp)) {
                        Text("נעילה אוטומטית", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "מיד", 1 to "דקה", 5 to "5 דק'", 30 to "30 דק'").forEach { (m, l) ->
                                Pill(l, MaterialTheme.colorScheme.primary, selected = autoLock == m) {
                                    autoLock = m
                                    settings.autoLockMinutes = m
                                }
                            }
                        }
                    }
                }
            }

            item {
                MenuGroup("עוזר AI") {
                    ToggleRow(
                        "✨", "עוזר פיננסי פעיל",
                        if (container.aiConfigured) "מודל: ${settings.aiModel}"
                        else "לא הוגדר מפתח",
                        aiEnabled
                    ) {
                        aiEnabled = it
                        settings.aiEnabled = it
                    }
                    MenuRow(
                        "🔑", "מפתחות API",
                        "${container.activeAiKeys().size} מפתחות מוגדרים"
                    ) { showAiKeySheet = true }
                    MenuRow("🔐", "פרטיות והרשאות", "מה ה־AI מעבד") {
                        navController.navigate(Routes.PRIVACY)
                    }
                }
            }

            item {
                MenuGroup("התראות") {
                    ToggleRow(
                        "🔔", "התראות פיננסיות",
                        "משכורת, חריגות תקציב, חיובי אשראי ויעדים",
                        notifications
                    ) {
                        notifications = it
                        settings.notificationsEnabled = it
                    }
                }
            }

            item {
                MenuGroup("נתונים") {
                    MenuRow("🎭", "טעינת נתוני הדגמה", "8 חודשי נתונים לדוגמה") {
                        vm.loadDemoData()
                    }
                    MenuRow("🔍", "סריקת הוצאות קבועות", "זיהוי מחדש של חיובים חוזרים") {
                        vm.rescanRecurring()
                    }
                    MenuRow("🗑️", "מחיקת כל הנתונים", "פעולה בלתי הפיכה") {
                        confirmWipe = true
                    }
                }
            }

            item {
                MenuGroup("אודות") {
                    MenuRow(
                        "⬆️", "עדכון אפליקציה",
                        "גרסה ${container.updateManager.currentVersionName} · " +
                            "build ${container.updateManager.currentVersionCode}"
                    ) { navController.navigate(Routes.UPDATE) }
                }
            }

            item {
                Text(
                    "הכסף שלנו · גרסה ${container.updateManager.currentVersionName}\n" +
                        "האפליקציה מספקת חישובים והמלצות כלליות לניהול " +
                        "תקציב בלבד, ואינה מהווה ייעוץ השקעות או ייעוץ פנסיוני.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("למחוק את כל הנתונים?") },
            text = {
                Text(
                    "כל העסקאות, החשבונות, היעדים וההשקעות יימחקו מהמכשיר. " +
                        "לא ניתן לשחזר."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.clearAllData(); confirmWipe = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("מחק הכול") }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("ביטול") }
            }
        )
    }

    if (showPinDialog) {
        PinDialog(
            hasPin = settings.pinHash != null,
            onDismiss = { showPinDialog = false },
            onSet = { pin ->
                settings.pinHash = Security.hashPin(pin)
                settings.lastUnlockAt = System.currentTimeMillis()
                vm.showToast("הקוד נשמר")
                showPinDialog = false
            },
            onClear = {
                settings.pinHash = null
                vm.showToast("הקוד הוסר")
                showPinDialog = false
            }
        )
    }

    if (showAiKeySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiKeySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            var keys by remember { mutableStateOf(settings.aiKeysOverride.orEmpty()) }
            var model by remember { mutableStateOf(settings.aiModel) }

            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("מפתחות AI", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "מפתחות Groq Cloud, מופרדים בפסיק. השאירו ריק כדי להשתמש במפתחות " +
                        "שהוטמעו בבנייה. המפתחות נשמרים מוצפנים במכשיר.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = keys,
                    onValueChange = { keys = it },
                    label = { Text("gsk_...") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(Modifier.height(12.dp))
                Text("מודל", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "openai/gpt-oss-120b" to "GPT-OSS 120B",
                        "qwen/qwen3.8-27b" to "Qwen 3.8",
                        "openai/gpt-oss-20b" to "GPT-OSS 20B"
                    ).forEach { (id, label) ->
                        Pill(label, MaterialTheme.colorScheme.secondary, selected = model == id) {
                            model = id
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        settings.aiKeysOverride = keys.trim().ifBlank { null }
                        settings.aiModel = model
                        vm.showToast("נשמר")
                        showAiKeySheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("שמירה") }
            }
        }
    }

    if (showProfileSheet) {
        ProfileEditorSheet(
            vm = vm,
            onDismiss = { showProfileSheet = false }
        )
    }
}

@Composable
private fun ToggleRow(
    emoji: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PinDialog(
    hasPin: Boolean,
    onDismiss: () -> Unit,
    onSet: (String) -> Unit,
    onClear: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length == 4 && pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPin) "שינוי קוד" else "הגדרת קוד") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("קוד בן 4 ספרות") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("אימות הקוד") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp)
                )
                if (pin.isNotEmpty() && confirm.isNotEmpty() && pin != confirm) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "הקודים אינם תואמים",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(pin) }, enabled = valid) { Text("שמור") }
        },
        dismissButton = {
            Row {
                if (hasPin) {
                    TextButton(
                        onClick = onClear,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("הסר קוד") }
                }
                TextButton(onClick = onDismiss) { Text("ביטול") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditorSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val profile by vm.profile.collectAsState()
    var name by remember { mutableStateOf(profile?.name.orEmpty()) }
    var familyName by remember { mutableStateOf(profile?.familyName.orEmpty()) }
    var buffer by remember { mutableStateOf((profile?.safetyBuffer ?: 3000.0).toLong().toString()) }
    var salaryDay by remember { mutableStateOf((profile?.salaryDayOfMonth ?: 10).toString()) }
    var income by remember {
        mutableStateOf((profile?.monthlyIncomeEstimate ?: 0.0).toLong().toString())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("פרטים אישיים", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("שם") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = familyName, onValueChange = { familyName = it },
                label = { Text("שם המשפחה") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = buffer,
                onValueChange = { buffer = it.filter { c -> c.isDigit() } },
                label = { Text("כרית ביטחון") }, prefix = { Text("₪") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "רק כסף מעל הסכום הזה ייחשב פנוי להמלצת חיסכון.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = income,
                    onValueChange = { income = it.filter { c -> c.isDigit() } },
                    label = { Text("הכנסה חודשית") }, prefix = { Text("₪") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = salaryDay,
                    onValueChange = { salaryDay = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("יום משכורת") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    vm.saveProfile {
                        it.copy(
                            name = name.trim().ifBlank { "משתמש" },
                            familyName = familyName.trim(),
                            safetyBuffer = Money.parse(buffer) ?: 3000.0,
                            salaryDayOfMonth = salaryDay.toIntOrNull()?.coerceIn(1, 28) ?: 10,
                            monthlyIncomeEstimate = Money.parse(income) ?: 0.0
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("שמירה") }
        }
    }
}
