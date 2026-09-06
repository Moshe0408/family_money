package com.familymoney.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familymoney.data.db.GoalEntity
import com.familymoney.ui.heroBrush
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.vm.MainViewModel
import com.familymoney.util.Dates
import com.familymoney.util.Money

/** Section 38: six-step onboarding. */
@Composable
fun OnboardingScreen(vm: MainViewModel, onFinished: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var expense by remember { mutableStateOf("") }
    var buffer by remember { mutableStateOf("3000") }
    var salaryDay by remember { mutableStateOf("10") }
    val selectedGoals = remember { mutableStateListOf<OnboardingGoal>() }
    var loadDemo by remember { mutableStateOf(true) }

    val totalSteps = 6

    fun finish() {
        val incomeValue = Money.parse(income) ?: 0.0
        val expenseValue = Money.parse(expense) ?: 0.0
        vm.saveProfile {
            it.copy(
                name = name.trim().ifBlank { "משתמש" },
                familyName = familyName.trim(),
                monthlyIncomeEstimate = incomeValue,
                monthlyExpenseEstimate = expenseValue,
                safetyBuffer = Money.parse(buffer) ?: 3000.0,
                salaryDayOfMonth = salaryDay.toIntOrNull()?.coerceIn(1, 28) ?: 10,
                onboardingDone = true
            )
        }
        selectedGoals.forEach { g ->
            vm.saveGoal(
                GoalEntity(
                    name = g.label,
                    emoji = g.emoji,
                    targetAmount = g.suggestedTarget,
                    currentAmount = 0.0,
                    targetDate = Dates.toMillis(Dates.today().plusMonths(g.suggestedMonths)),
                    monthlyContribution = g.suggestedTarget / g.suggestedMonths
                )
            )
        }
        if (loadDemo) vm.loadDemoData()
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            StepProgress(step, totalSteps)

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { -it } + fadeIn())
                        .togetherWith(slideOutHorizontally { it } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "onboarding"
            ) { current ->
                when (current) {
                    0 -> WelcomeStep(
                        name = name,
                        onName = { name = it },
                        familyName = familyName,
                        onFamilyName = { familyName = it }
                    )
                    1 -> AmountStep(
                        title = "כמה אתם מכניסים בחודש?",
                        subtitle = "סכום ההכנסה נטו של משק הבית. אפשר לשנות בכל רגע.",
                        emoji = "💼",
                        value = income,
                        onValue = { income = it },
                        hint = "לדוגמה 18,600"
                    )
                    2 -> AmountStep(
                        title = "כמה אתם מוציאים בערך?",
                        subtitle = "הערכה גסה מספיקה — האפליקציה תדייק אותה מהעסקאות.",
                        emoji = "🧾",
                        value = expense,
                        onValue = { expense = it },
                        hint = "לדוגמה 12,450"
                    )
                    3 -> GoalsStep(selectedGoals)
                    4 -> BufferStep(
                        buffer = buffer,
                        onBuffer = { buffer = it },
                        salaryDay = salaryDay,
                        onSalaryDay = { salaryDay = it }
                    )
                    else -> ReadyStep(
                        loadDemo = loadDemo,
                        onToggleDemo = { loadDemo = it }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) { Text("חזרה") }
                }
                Button(
                    onClick = { if (step < totalSteps - 1) step++ else finish() },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = step != 0 || name.isNotBlank()
                ) {
                    Text(
                        if (step < totalSteps - 1) "המשך" else "בואו נתחיל",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StepProgress(step: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (i <= step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun StepShell(
    emoji: String,
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)
    ) {
        item {
            Text(emoji, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Column(content = content)
        }
    }
}

@Composable
private fun WelcomeStep(
    name: String,
    onName: (String) -> Unit,
    familyName: String,
    onFamilyName: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(heroBrush(), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💰", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "בואו נעשה סדר בכסף",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        "הכסף שלך. המשפחה שלך. ההחלטות שלך.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                label = { Text("איך קוראים לך?") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = familyName,
                onValueChange = onFamilyName,
                label = { Text("שם המשפחה (אופציונלי)") },
                placeholder = { Text("לדוגמה: משפחת איסקוב") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AmountStep(
    title: String,
    subtitle: String,
    emoji: String,
    value: String,
    onValue: (String) -> Unit,
    hint: String
) {
    StepShell(emoji, title, subtitle) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
            label = { Text("סכום בשקלים") },
            placeholder = { Text(hint) },
            prefix = { Text("₪") },
            singleLine = true,
            textStyle = MoneyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GoalsStep(selected: androidx.compose.runtime.snapshots.SnapshotStateList<OnboardingGoal>) {
    StepShell("🎯", "מה המטרה שלכם?", "בחרו כל מה שרלוונטי — ניצור עבורכם יעדים מוכנים.") {
        onboardingGoals.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { goal ->
                    val isSelected = selected.contains(goal)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (isSelected) selected.remove(goal) else selected.add(goal)
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = if (isSelected)
                            androidx.compose.foundation.BorderStroke(
                                2.dp, MaterialTheme.colorScheme.primary
                            ) else null
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(goal.emoji, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(8.dp))
                            Text(goal.label, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BufferStep(
    buffer: String,
    onBuffer: (String) -> Unit,
    salaryDay: String,
    onSalaryDay: (String) -> Unit
) {
    StepShell(
        "🛟",
        "כמה להשאיר בצד?",
        "כרית הביטחון היא הסכום שתמיד נשאר בעו\"ש. רק כסף מעליה ייחשב פנוי לחיסכון."
    ) {
        OutlinedTextField(
            value = buffer,
            onValueChange = { onBuffer(it.filter { c -> c.isDigit() }) },
            label = { Text("כרית ביטחון") },
            prefix = { Text("₪") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = salaryDay,
            onValueChange = { onSalaryDay(it.filter { c -> c.isDigit() }.take(2)) },
            label = { Text("באיזה יום בחודש נכנסת המשכורת?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ReadyStep(loadDemo: Boolean, onToggleDemo: (Boolean) -> Unit) {
    StepShell("🚀", "הכול מוכן", "אפשר להתחיל. תמיד אפשר לחבר חשבונות ולהזמין שותף בהמשך.") {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onToggleDemo(!loadDemo) },
            shape = RoundedCornerShape(18.dp),
            color = if (loadDemo) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (loadDemo) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (loadDemo) "✅" else "⬜", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("טען נתוני הדגמה", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "8 חודשי עסקאות, תקציבים, יעדים והשקעות — כדי לראות איך הכול עובד. אפשר למחוק בהגדרות.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        com.familymoney.ui.components.DisclaimerNote(
            "האפליקציה מספקת חישובים, ניתוח נתונים והמלצות כלליות לניהול תקציב. " +
                "היא אינה מהווה ייעוץ השקעות, ייעוץ פנסיוני או שיווק פיננסי."
        )
    }
}

// ---------------------------------------------------------------------- model

data class OnboardingGoal(
    val label: String,
    val emoji: String,
    val suggestedTarget: Double,
    val suggestedMonths: Long
)

private val onboardingGoals = listOf(
    OnboardingGoal("לחסוך", "🐷", 20_000.0, 24),
    OnboardingGoal("להשקיע", "📈", 50_000.0, 36),
    OnboardingGoal("קרן חירום", "🛟", 40_000.0, 24),
    OnboardingGoal("ילדים", "🧒", 30_000.0, 60),
    OnboardingGoal("רכב", "🚗", 90_000.0, 36),
    OnboardingGoal("דירה", "🏠", 400_000.0, 96),
    OnboardingGoal("חופשה", "✈️", 15_000.0, 12),
    OnboardingGoal("שיפוץ", "🔨", 60_000.0, 24)
)
