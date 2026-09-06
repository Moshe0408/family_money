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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.components.SectionHeader
import com.familymoney.ui.vm.MainViewModel

/** Sections 28-29: insights feed and the full recommendation list. */
@Composable
fun InsightsScreen(vm: MainViewModel, navController: NavHostController) {
    val insights by vm.insights.collectAsState()
    val recommendations by vm.recommendations.collectAsState()

    DetailScaffold("🧠 התובנות שלי", navController) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (insights.isEmpty() && recommendations.isEmpty()) {
                item {
                    EmptyState(
                        "🧠",
                        "אין מספיק נתונים",
                        "הוסיפו כמה עסקאות והמערכת תתחיל לזהות דפוסים, חריגות והזדמנויות חיסכון."
                    )
                }
            }

            if (recommendations.isNotEmpty()) {
                item { SectionHeader("המלצות בשבילכם", emoji = "💡") }
                items(recommendations, key = { it.id }) { rec ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rec.category.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                rec.category.he,
                                style = MaterialTheme.typography.labelMedium,
                                color = colorForRecommendation(rec.category)
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "עדיפות ${rec.priority.he}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(rec.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            rec.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { navController.navigate(routeForAction(rec.suggestedAction)) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(rec.actionLabel) }
                    }
                }
            }

            if (insights.isNotEmpty()) {
                item {
                    SectionHeader(
                        "תובנות",
                        emoji = "📌",
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                items(insights) { insight ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(insight.emoji, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(insight.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    insight.body,
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
}
