package com.familymoney

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.familymoney.data.ai.AiAssistant
import com.familymoney.data.ai.GroqClient
import com.familymoney.data.db.AppDatabase
import com.familymoney.data.prefs.SettingsStore
import com.familymoney.data.repo.FinanceRepository
import com.familymoney.data.sync.FamilySyncFactory
import com.familymoney.data.sync.SyncManager
import com.familymoney.data.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class FamilyMoneyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // PdfBox ships its fonts and CMaps as assets and must be pointed at
        // them before any PDF is opened.
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        createNotificationChannel()
        container.syncManager.startIfConfigured()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "התראות פיננסיות",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "משכורת, חריגות תקציב, חיובי אשראי ותזכורות יעדים"
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "family_money_alerts"
    }
}

/**
 * Hand-rolled dependency container. The graph is small enough that a DI
 * framework would cost more than it saves.
 */
class AppContainer(app: Application) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = SettingsStore(app)
    val database: AppDatabase = AppDatabase.get(app)
    val repository = FinanceRepository(app)

    val updateManager = UpdateManager(app)

    private val familySync = FamilySyncFactory.create(app)

    val syncManager = SyncManager(
        sync = familySync,
        repo = repository,
        db = database,
        settings = settings,
        scope = scope
    )

    val groqClient: GroqClient
        get() = GroqClient(apiKeys = activeAiKeys(), model = settings.aiModel)

    val assistant: AiAssistant get() = AiAssistant(groqClient)

    /** Extracts transactions from statement text pasted out of a PDF. */
    val statementParser: com.familymoney.data.ai.StatementParser
        get() = com.familymoney.data.ai.StatementParser(groqClient)

    /** User-supplied keys win over the ones baked in at build time. */
    fun activeAiKeys(): List<String> {
        val override = settings.aiKeysOverride
        val raw = if (!override.isNullOrBlank()) override else BuildConfig.GROQ_KEYS
        return raw.split(',', '\n', ' ')
            .map { it.trim() }
            .filter { it.startsWith("gsk_") }
    }

    val aiConfigured: Boolean get() = activeAiKeys().isNotEmpty()

    val syncAvailable: Boolean get() = familySync.available
}
