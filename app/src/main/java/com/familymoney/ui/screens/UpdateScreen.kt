package com.familymoney.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.familymoney.AppContainer
import com.familymoney.data.update.ReleaseInfo
import com.familymoney.data.update.UpdateState
import com.familymoney.ui.components.AmountRow
import com.familymoney.ui.components.DetailScaffold
import com.familymoney.ui.components.ProgressBar
import com.familymoney.ui.components.SectionCard
import com.familymoney.ui.heroBrush
import com.familymoney.ui.theme.MoneySmall
import com.familymoney.ui.theme.Success
import com.familymoney.ui.theme.Warning
import kotlinx.coroutines.launch

/**
 * Checks Supabase for a newer build, downloads it with progress, verifies the
 * hash and opens the system installer.
 */
@Composable
fun UpdateScreen(container: AppContainer, navController: NavHostController) {
    val updater = container.updateManager
    val state by updater.state.collectAsState()
    val scope = rememberCoroutineScope()
    var permissionNeeded by remember { mutableStateOf(false) }

    // Check as soon as the screen opens, unless a download is already running.
    LaunchedEffect(Unit) {
        if (state is UpdateState.Idle || state is UpdateState.Failed) {
            updater.check()
        }
    }

    DetailScaffold("⬆️ עדכון אפליקציה", navController) {
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
                            "הגרסה המותקנת",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            updater.currentVersionName,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Version Code ${updater.currentVersionCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                when (val s = state) {
                    UpdateState.Idle, UpdateState.Checking -> CheckingCard()

                    is UpdateState.UpToDate -> UpToDateCard(
                        version = s.currentVersion,
                        onRecheck = { scope.launch { updater.check() } }
                    )

                    is UpdateState.Available -> AvailableCard(
                        release = s.release,
                        onDownload = { scope.launch { updater.download(s.release) } }
                    )

                    is UpdateState.Downloading -> DownloadingCard(s)

                    is UpdateState.ReadyToInstall -> ReadyCard(
                        release = s.release,
                        onInstall = {
                            if (updater.canInstallPackages()) {
                                permissionNeeded = false
                                updater.install(s.filePath)
                            } else {
                                permissionNeeded = true
                            }
                        }
                    )

                    is UpdateState.Failed -> FailedCard(
                        message = s.message,
                        onRetry = {
                            scope.launch {
                                if (s.release != null) updater.download(s.release)
                                else updater.check()
                            }
                        }
                    )
                }
            }

            if (permissionNeeded) {
                item {
                    SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            "נדרשת הרשאת התקנה",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "אנדרואיד דורש אישור חד־פעמי כדי שהאפליקציה תוכל להתקין " +
                                "עדכונים. אשרו את ההרשאה וחזרו לכאן.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { updater.openInstallPermissionSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("פתח הגדרות") }
                    }
                }
            }

            item {
                SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Text("איך זה עובד", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "האפליקציה שואלת את השרת מה הגרסה האחרונה שפורסמה.",
                        "אם יש חדשה — היא מורידה אותה ומאמתת חתימת SHA-256.",
                        "אם החתימה לא תואמת, הקובץ נמחק וההתקנה מבוטלת.",
                        "ההתקנה עצמה תמיד עוברת דרך מסך האישור של אנדרואיד."
                    ).forEach {
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("• ", style = MaterialTheme.typography.bodySmall)
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- state cards

@Composable
private fun CheckingCard() {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(14.dp))
            Text("בודק אם יש עדכון…", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun UpToDateCard(version: String, onRecheck: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✅", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("האפליקציה מעודכנת", style = MaterialTheme.typography.titleMedium)
                Text(
                    "גרסה $version היא האחרונה שפורסמה.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onRecheck, modifier = Modifier.fillMaxWidth()) {
            Text("בדוק שוב")
        }
    }
}

@Composable
private fun AvailableCard(release: ReleaseInfo, onDownload: () -> Unit) {
    SectionCard(
        containerColor = if (release.mandatory) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.tertiaryContainer
    ) {
        val onColor = if (release.mandatory) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onTertiaryContainer

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (release.mandatory) "⚠️" else "🎉", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (release.mandatory) "עדכון חובה זמין" else "יש גרסה חדשה",
                    style = MaterialTheme.typography.titleMedium,
                    color = onColor
                )
                Text(
                    "גרסה ${release.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onColor
                )
            }
        }

        if (release.releaseNotes.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text("מה חדש", style = MaterialTheme.typography.titleSmall, color = onColor)
            Spacer(Modifier.height(6.dp))
            Text(
                release.releaseNotes,
                style = MaterialTheme.typography.bodyMedium,
                color = onColor
            )
        }

        if (release.fileSize > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "גודל ההורדה: ${humanSize(release.fileSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = onColor
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("הורד והתקן") }
    }
}

@Composable
private fun DownloadingCard(s: UpdateState.Downloading) {
    val animated by animateFloatAsState(
        s.percent / 100f, tween(300), label = "download"
    )
    SectionCard {
        Text("מוריד גרסה ${s.release.versionName}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))
        ProgressBar(animated, height = 12.dp, colorOverride = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${s.percent}%", style = MoneySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "${humanSize(s.bytesDownloaded)} / ${humanSize(s.totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadyCard(release: ReleaseInfo, onInstall: () -> Unit) {
    SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📦", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "הגרסה הורדה ואומתה",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "גרסה ${release.versionName} מוכנה להתקנה.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onInstall,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("התקן עכשיו") }
        Spacer(Modifier.height(8.dp))
        Text(
            "אנדרואיד יבקש את אישורכם לפני ההתקנה. הנתונים שלכם נשמרים.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FailedCard(message: String, onRetry: () -> Unit) {
    SectionCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) { Text("נסה שוב") }
    }
}

internal fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val mb = bytes / 1_048_576.0
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}
