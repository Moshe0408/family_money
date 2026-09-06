package com.familymoney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.engine.Compound
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.DisclaimerNote
import com.familymoney.ui.components.LineChart
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.heroBrush
import com.familymoney.ui.theme.MoneyLarge
import com.familymoney.ui.theme.MoneyMedium
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.util.Money
import kotlin.math.roundToInt

/** Sections 24-25: compound interest simulator with scenario ranges. */
@Composable
fun SimulatorScreen(navController: NavHostController) {
    var initial by remember { mutableStateOf("10000") }
    var monthly by remember { mutableStateOf("1000") }
    var years by remember { mutableStateOf(10f) }
    var rate by remember { mutableStateOf(6f) }

    val initialValue = Money.parse(initial) ?: 0.0
    val monthlyValue = Money.parse(monthly) ?: 0.0
    val yearsValue = years.roundToInt()
    val rateValue = rate.toDouble()

    val projection = remember(initialValue, monthlyValue, yearsValue, rateValue) {
        Compound.project(initialValue, monthlyValue, rateValue, yearsValue)
    }
    val scenarios = remember(initialValue, monthlyValue, yearsValue) {
        Compound.scenarios(initialValue, monthlyValue, yearsValue)
    }

    DetailScaffold("🧮 סימולטור ריבית דריבית", navController) {
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
                        Text(
                            "סכום עתידי משוער בעוד $yearsValue שנים",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            Money.format(projection.finalValue),
                            style = MoneyLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "סה\"כ הפקדות",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    Money.format(projection.totalContributed),
                                    style = MoneySmall, color = Color.White
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "רווח משוער",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    Money.format(projection.totalProfit),
                                    style = MoneySmall, color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    OutlinedTextField(
                        value = initial,
                        onValueChange = { initial = it.filter { c -> c.isDigit() } },
                        label = { Text("סכום התחלתי") }, prefix = { Text("₪") },
                        singleLine = true, textStyle = MoneyMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = monthly,
                        onValueChange = { monthly = it.filter { c -> c.isDigit() } },
                        label = { Text("הפקדה חודשית") }, prefix = { Text("₪") },
                        singleLine = true, textStyle = MoneyMedium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))
                    SliderRow(
                        label = "תקופה",
                        value = "$yearsValue שנים",
                        sliderValue = years,
                        range = 1f..40f,
                        steps = 38,
                        onChange = { years = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    SliderRow(
                        label = "תשואה שנתית משוערת",
                        value = Money.percent(rateValue),
                        sliderValue = rate,
                        range = 0f..15f,
                        steps = 29,
                        onChange = { rate = it }
                    )
                }
            }

            item { SectionHeader("צמיחה לאורך זמן", emoji = "📈") }
            item {
                SectionCard {
                    val points = projection.points.filter { it.year > 0 }
                    LineChart(
                        values = points.map { it.value }.reversed(),
                        labels = points
                            .filterIndexed { i, _ ->
                                points.size <= 10 || i % (points.size / 8).coerceAtLeast(1) == 0
                            }
                            .map { "ש${it.year}" }
                            .reversed(),
                        showDots = points.size <= 15
                    )
                }
            }

            // Section 25: conservative / moderate / high side by side.
            item { SectionHeader("תרחישים", emoji = "🎲") }
            item {
                SectionCard {
                    val labels = mapOf(4.0 to "שמרני", 6.0 to "בינוני", 8.0 to "גבוה")
                    scenarios.forEach { (r, p) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    labels[r] ?: Money.percent(r),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "${Money.percent(r)} לשנה",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Money.format(p.finalValue), style = MoneySmall)
                                Text(
                                    "רווח ${Money.format(p.totalProfit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Success
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("פירוט לפי שנים", emoji = "📋") }
            item {
                SectionCard {
                    projection.points
                        .filter { it.year > 0 }
                        .filterIndexed { i, p ->
                            p.year <= 3 || p.year % 5 == 0 || i == projection.points.size - 2
                        }
                        .forEach { p ->
                            AmountRow("שנה ${p.year}", Money.format(p.value))
                        }
                }
            }

            item {
                DisclaimerNote(
                    "זהו חישוב מתמטי בלבד המבוסס על הנחת תשואה קבועה. " +
                        "תשואות בפועל משתנות ואינן מובטחות. אין באמור ייעוץ או שיווק השקעות."
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    sliderValue: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = sliderValue,
            onValueChange = onChange,
            valueRange = range,
            steps = steps
        )
    }
}
