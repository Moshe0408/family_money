package com.familymoney.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.familymoney.data.prefs.SettingsStore
import com.familymoney.ui.heroBrush
import com.familymoney.util.Security

/** Section 41: biometric / PIN gate shown before any financial data renders. */
@Composable
fun LockScreen(settings: SettingsStore, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric() {
        if (activity == null || !biometricAvailable) return
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (settings.pinHash == null) {
                        error = errString.toString()
                    }
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("הכסף שלנו")
                .setSubtitle("אימות כדי לפתוח את האפליקציה")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }

    LaunchedEffect(Unit) {
        if (settings.biometricLock && biometricAvailable) promptBiometric()
    }

    Box(
        Modifier.fillMaxSize().background(heroBrush()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                "הכסף שלנו",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (settings.pinHash != null) "הזינו קוד כדי להמשיך"
                else "נדרש אימות כדי להמשיך",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            if (settings.pinHash != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { i ->
                        Box(
                            Modifier
                                .size(18.dp)
                                .background(
                                    if (i < pin.length) Color.White
                                    else Color.White.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
                PinPad(
                    onDigit = { d ->
                        if (pin.length < 4) {
                            pin += d
                            if (pin.length == 4) {
                                if (Security.hashPin(pin) == settings.pinHash) {
                                    onUnlocked()
                                } else {
                                    error = "קוד שגוי"
                                    pin = ""
                                }
                            }
                        }
                    },
                    onBackspace = { pin = pin.dropLast(1) },
                    onBiometric = if (settings.biometricLock && biometricAvailable) {
                        { promptBiometric() }
                    } else null
                )
            } else {
                TextButton(onClick = { promptBiometric() }) {
                    Text("אימות ביומטרי", color = Color.White)
                }
            }

            error?.let {
                Spacer(Modifier.height(18.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (onBiometric != null) "☝" else "", "0", "⌫")
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .size(70.dp)
                            .clickable(enabled = key.isNotEmpty()) {
                                when (key) {
                                    "⌫" -> onBackspace()
                                    "☝" -> onBiometric?.invoke()
                                    "" -> Unit
                                    else -> onDigit(key)
                                }
                            },
                        shape = CircleShape,
                        color = if (key.isEmpty()) Color.Transparent
                        else Color.White.copy(alpha = 0.16f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                key,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
