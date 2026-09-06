package com.familymoney.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.AppContainer
import com.familymoney.data.ai.GroqClient
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.EmptyState
import com.familymoney.ui.components.Pill
import com.familymoney.ui.nav.Routes
import com.familymoney.ui.vm.MainViewModel
import kotlinx.coroutines.launch

private data class ChatBubble(val text: String, val fromUser: Boolean, val isError: Boolean = false)

/** Section 27: "שאל את הכסף שלי". */
@Composable
fun AiChatScreen(vm: MainViewModel, container: AppContainer, navController: NavHostController) {
    val snapshot by vm.snapshot.collectAsState()
    val recommendations by vm.recommendations.collectAsState()
    val profile by vm.profile.collectAsState()

    val assistant = remember { container.assistant }
    val bubbles = remember { mutableStateListOf<ChatBubble>() }
    val history = remember { mutableStateListOf<GroqClient.Message>() }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val aiEnabled = container.settings.aiEnabled && container.aiConfigured

    fun send(question: String) {
        if (question.isBlank() || thinking) return
        val s = snapshot ?: return
        bubbles += ChatBubble(question, fromUser = true)
        input = ""
        thinking = true

        scope.launch {
            val context = assistant.buildContext(
                s = s,
                recommendations = recommendations,
                familyName = profile?.familyName.orEmpty(),
                userName = profile?.name.orEmpty().ifBlank { "המשתמש" }
            )
            when (val result = assistant.ask(question, context, history.toList())) {
                is GroqClient.Result.Ok -> {
                    bubbles += ChatBubble(result.text, fromUser = false)
                    history += GroqClient.Message("user", question)
                    history += GroqClient.Message("assistant", result.text)
                }
                is GroqClient.Result.Error -> {
                    bubbles += ChatBubble(result.message, fromUser = false, isError = true)
                }
            }
            thinking = false
        }
    }

    LaunchedEffect(bubbles.size, thinking) {
        if (bubbles.isNotEmpty()) listState.animateScrollToItem(bubbles.size)
    }

    DetailScaffold("✨ שאל את הכסף שלי", navController) {
        Column(Modifier.fillMaxSize().imePadding()) {
            if (!aiEnabled) {
                EmptyState(
                    emoji = "🔌",
                    title = "העוזר אינו פעיל",
                    body = if (!container.aiConfigured)
                        "לא הוגדר מפתח AI. אפשר להוסיף מפתח Groq במסך ההגדרות."
                    else "העוזר כובה בהגדרות הפרטיות."
                ) {
                    Pill("פתח הגדרות", MaterialTheme.colorScheme.primary) {
                        navController.navigate(Routes.SETTINGS)
                    }
                }
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bubbles.isEmpty()) {
                    item { WelcomeCard(profile?.name.orEmpty().ifBlank { "שלום" }) }
                    item {
                        Text(
                            "נסו לשאול",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(assistant.starterQuestions) { q ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { send(q) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💬", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(10.dp))
                                Text(q, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                items(bubbles) { bubble -> ChatRow(bubble) }

                if (thinking) {
                    item { ThinkingRow() }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("שאלו כל דבר על הכסף שלכם") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { send(input) },
                        enabled = input.isNotBlank() && !thinking,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (input.isNotBlank() && !thinking)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "שלח",
                            tint = if (input.isNotBlank() && !thinking) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(name: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(com.familymoney.ui.heroBrush(), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column {
            Text("✨", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "היי $name",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "אני מכיר את התמונה הפיננסית שלכם — הכנסות, הוצאות, יעדים והשקעות. " +
                    "שאלו כל דבר ואענה על סמך הנתונים בלבד.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ChatRow(bubble: ChatBubble) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (bubble.fromUser) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (bubble.fromUser) 6.dp else 20.dp,
                bottomEnd = if (bubble.fromUser) 20.dp else 6.dp
            ),
            color = when {
                bubble.isError -> MaterialTheme.colorScheme.errorContainer
                bubble.fromUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Text(
                bubble.text,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    bubble.isError -> MaterialTheme.colorScheme.onErrorContainer
                    bubble.fromUser -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "חושב על זה…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
