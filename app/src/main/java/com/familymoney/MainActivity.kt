package com.familymoney

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.familymoney.ui.AppRoot
import com.familymoney.ui.theme.FamilyMoneyTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as FamilyMoneyApp).container
        // Deep link: familymoney://join?code=ABCD1234
        val inviteCode = intent?.data
            ?.takeIf { it.scheme == "familymoney" && it.host == "join" }
            ?.getQueryParameter("code")

        setContent {
            val themeMode by container.settings.darkMode.collectAsState()
            FamilyMoneyTheme(themeMode = themeMode) {
                AppRoot(container = container, pendingInviteCode = inviteCode)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val settings = (application as FamilyMoneyApp).container.settings
        if (settings.biometricLock || settings.pinHash != null) {
            // Stamp the moment we left so the auto-lock window starts counting.
            settings.lastUnlockAt = settings.lastUnlockAt.coerceAtMost(System.currentTimeMillis())
        }
    }
}
