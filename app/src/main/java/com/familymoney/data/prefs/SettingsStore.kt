package com.familymoney.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App settings. Anything sensitive (AI keys, family sync id) lives in
 * EncryptedSharedPreferences, backed by the Android keystore.
 */
class SettingsStore(context: Context) {

    private val plain: SharedPreferences =
        context.getSharedPreferences("family_money_settings", Context.MODE_PRIVATE)

    private val secure: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "family_money_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as SharedPreferences
    }.getOrElse {
        // Some OEM keystores fail; fall back so the app still runs.
        context.getSharedPreferences("family_money_secure_fallback", Context.MODE_PRIVATE)
    }

    // ------------------------------------------------------------ appearance

    private val _darkMode = MutableStateFlow(plain.getString(KEY_THEME, "system")!!)
    val darkMode: StateFlow<String> = _darkMode

    fun setThemeMode(mode: String) {
        plain.edit().putString(KEY_THEME, mode).apply()
        _darkMode.value = mode
    }

    // -------------------------------------------------------------- security

    var biometricLock: Boolean
        get() = plain.getBoolean(KEY_BIOMETRIC, false)
        set(v) = plain.edit().putBoolean(KEY_BIOMETRIC, v).apply()

    var autoLockMinutes: Int
        get() = plain.getInt(KEY_AUTOLOCK, 5)
        set(v) = plain.edit().putInt(KEY_AUTOLOCK, v).apply()

    var pinHash: String?
        get() = secure.getString(KEY_PIN, null)
        set(v) = secure.edit().putString(KEY_PIN, v).apply()

    var lastUnlockAt: Long
        get() = plain.getLong(KEY_LAST_UNLOCK, 0L)
        set(v) = plain.edit().putLong(KEY_LAST_UNLOCK, v).apply()

    fun isLockRequired(): Boolean {
        if (!biometricLock && pinHash == null) return false
        if (autoLockMinutes <= 0) return true
        val elapsed = System.currentTimeMillis() - lastUnlockAt
        return elapsed > autoLockMinutes * 60_000L
    }

    // -------------------------------------------------------------------- AI

    /** Keys shipped with the build; the user can override them in Settings. */
    var aiKeysOverride: String?
        get() = secure.getString(KEY_AI_KEYS, null)
        set(v) = secure.edit().putString(KEY_AI_KEYS, v).apply()

    var aiEnabled: Boolean
        get() = plain.getBoolean(KEY_AI_ENABLED, true)
        set(v) = plain.edit().putBoolean(KEY_AI_ENABLED, v).apply()

    var aiModel: String
        get() = plain.getString(KEY_AI_MODEL, "openai/gpt-oss-120b")!!
        set(v) = plain.edit().putString(KEY_AI_MODEL, v).apply()

    // ------------------------------------------------------------ family sync

    var familyId: String?
        get() = secure.getString(KEY_FAMILY_ID, null)
        set(v) = secure.edit().putString(KEY_FAMILY_ID, v).apply()

    var familyName: String
        get() = plain.getString(KEY_FAMILY_NAME, "") ?: ""
        set(v) = plain.edit().putString(KEY_FAMILY_NAME, v).apply()

    var syncEnabled: Boolean
        get() = plain.getBoolean(KEY_SYNC_ENABLED, false)
        set(v) = plain.edit().putBoolean(KEY_SYNC_ENABLED, v).apply()

    var lastSyncAt: Long
        get() = plain.getLong(KEY_LAST_SYNC, 0L)
        set(v) = plain.edit().putLong(KEY_LAST_SYNC, v).apply()

    // ------------------------------------------------------------------ misc

    var notificationsEnabled: Boolean
        get() = plain.getBoolean(KEY_NOTIFICATIONS, true)
        set(v) = plain.edit().putBoolean(KEY_NOTIFICATIONS, v).apply()

    var demoDataLoaded: Boolean
        get() = plain.getBoolean(KEY_DEMO, false)
        set(v) = plain.edit().putBoolean(KEY_DEMO, v).apply()

    var lastSnapshotMonth: String
        get() = plain.getString(KEY_LAST_SNAPSHOT, "") ?: ""
        set(v) = plain.edit().putString(KEY_LAST_SNAPSHOT, v).apply()

    var autoCheckUpdates: Boolean
        get() = plain.getBoolean(KEY_AUTO_UPDATE, true)
        set(v) = plain.edit().putBoolean(KEY_AUTO_UPDATE, v).apply()

    var lastUpdateCheckAt: Long
        get() = plain.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(v) = plain.edit().putLong(KEY_LAST_UPDATE_CHECK, v).apply()

    /** Version the user chose to skip, so we stop nagging about it. */
    var dismissedUpdateCode: Int
        get() = plain.getInt(KEY_DISMISSED_UPDATE, 0)
        set(v) = plain.edit().putInt(KEY_DISMISSED_UPDATE, v).apply()

    /** At most one background check per day. */
    fun shouldCheckForUpdate(): Boolean {
        if (!autoCheckUpdates) return false
        return System.currentTimeMillis() - lastUpdateCheckAt > 24 * 60 * 60 * 1000L
    }

    fun clearAll() {
        plain.edit().clear().apply()
        secure.edit().clear().apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_BIOMETRIC = "biometric_lock"
        const val KEY_AUTOLOCK = "auto_lock_minutes"
        const val KEY_PIN = "pin_hash"
        const val KEY_LAST_UNLOCK = "last_unlock_at"
        const val KEY_AI_KEYS = "ai_keys"
        const val KEY_AI_ENABLED = "ai_enabled"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_FAMILY_ID = "family_id"
        const val KEY_FAMILY_NAME = "family_name"
        const val KEY_SYNC_ENABLED = "sync_enabled"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DEMO = "demo_data_loaded"
        const val KEY_LAST_SNAPSHOT = "last_snapshot_month"
        const val KEY_AUTO_UPDATE = "auto_check_updates"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check_at"
        const val KEY_DISMISSED_UPDATE = "dismissed_update_code"
    }
}
