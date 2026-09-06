package com.familymoney

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.familymoney.data.db.AppDatabase
import com.familymoney.data.db.ProfileEntity
import com.familymoney.data.prefs.SettingsStore
import com.familymoney.data.repo.FinanceRepository
import com.familymoney.data.seed.DemoData
import com.familymoney.engine.RecommendationEngine
import com.familymoney.ui.theme.FamilyMoneyTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device checks that the pieces the unit tests cannot reach actually work:
 * Room schema creation, the demo seed, the snapshot pipeline, and that the
 * theme composes without throwing (fonts, colours, RTL).
 */
@RunWith(AndroidJUnit4::class)
class ScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repo: FinanceRepository
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = AppDatabase.get(context)
        repo = FinanceRepository(context)
        runBlocking { repo.wipeAll() }
    }

    @Test
    fun roomSchemaCreatesAndAcceptsAProfile() = runBlocking {
        repo.saveProfile(ProfileEntity(name = "משה", familyName = "משפחת בדיקה", onboardingDone = true))
        val stored = repo.getProfile()
        assertNotNull(stored)
        assertEquals("משה", stored!!.name)
        assertTrue(stored.onboardingDone)
    }

    @Test
    fun demoSeedPopulatesEveryTable() = runBlocking {
        DemoData.populate(db, "משה")

        assertTrue("expected transactions", db.transactionDao().count() > 100)
        assertTrue("expected accounts", db.accountDao().all().size >= 3)
        assertTrue("expected cards", db.cardDao().all().size >= 3)
        assertTrue("expected budgets", db.budgetDao().all().size >= 5)
        assertTrue("expected goals", db.goalDao().all().size >= 3)
        assertTrue("expected investments", db.investmentDao().all().size >= 5)
        assertTrue("expected children", db.childDao().all().size >= 2)
        assertTrue("expected standing orders", db.recurringDao().all().size >= 8)
    }

    @Test
    fun snapshotAndEngineProduceRealOutputForSeededData() = runBlocking {
        DemoData.populate(db, "משה")
        repo.saveProfile(ProfileEntity(name = "משה", safetyBuffer = 3000.0, onboardingDone = true))

        val snapshot = repo.buildSnapshot(
            profile = repo.getProfile(),
            txs = db.transactionDao().recent(2000),
            accounts = db.accountDao().all(),
            cards = db.cardDao().all(),
            budgets = db.budgetDao().all(),
            goals = db.goalDao().all(),
            investments = db.investmentDao().all(),
            children = db.childDao().all(),
            recurring = db.recurringDao().all(),
            liabilities = db.liabilityDao().all()
        )

        assertTrue("net worth should be positive", snapshot.netWorth > 0)
        assertTrue("month should have income", snapshot.monthIncome > 0)
        assertTrue("month should have expenses", snapshot.monthExpense > 0)
        assertTrue("categories should be populated", snapshot.categorySpendThisMonth.isNotEmpty())
        assertTrue("budgets should be loaded", snapshot.budgets.isNotEmpty())
        assertTrue("goals should be loaded", snapshot.goals.isNotEmpty())

        val recommendations = RecommendationEngine.analyze(snapshot)
        assertTrue("engine should produce recommendations", recommendations.isNotEmpty())
        recommendations.forEach {
            assertTrue("every recommendation needs a title", it.title.isNotBlank())
            assertTrue("every recommendation needs a reason", it.reason.isNotBlank())
        }

        val insights = RecommendationEngine.insights(snapshot)
        assertTrue("engine should produce insights", insights.isNotEmpty())

        val forecast = RecommendationEngine.forecast(snapshot)
        assertEquals(5, forecast.lines.size)
    }

    @Test
    fun categoryLearningRemembersTheUsersChoice() = runBlocking {
        repo.learnCategory("וולט", com.familymoney.data.model.ExpenseCategory.RESTAURANTS.name)
        assertEquals(
            com.familymoney.data.model.ExpenseCategory.RESTAURANTS,
            repo.suggestCategory("וולט")
        )
        // Unseen merchants still fall back to the built-in Israeli keyword table.
        assertEquals(
            com.familymoney.data.model.ExpenseCategory.GROCERY,
            repo.suggestCategory("שופרסל דיל")
        )
    }

    @Test
    fun recurringDetectionRunsAgainstSeededHistory() = runBlocking {
        DemoData.populate(db, "משה")
        repo.refreshRecurringDetection()
        val recurring = db.recurringDao().all()
        assertTrue("standing orders should survive detection", recurring.isNotEmpty())
        assertTrue("subscriptions should be flagged", recurring.any { it.isSubscription })
    }

    @Test
    fun themeComposesWithHebrewTextAndBundledFont() {
        // Exercises font loading, the colour scheme and the RTL layout direction —
        // the combination that crashed on the first device run.
        compose.setContent {
            FamilyMoneyTheme(themeMode = "light") {
                androidx.compose.material3.Text(
                    text = "המצב הפיננסי שלנו ₪186,420",
                    style = com.familymoney.ui.theme.MoneyLarge
                )
            }
        }
        compose.onNodeWithText("המצב הפיננסי שלנו ₪186,420").assertIsDisplayed()
    }

    @Test
    fun darkThemeAlsoComposes() {
        compose.setContent {
            FamilyMoneyTheme(themeMode = "dark") {
                androidx.compose.material3.Text("מצב כהה")
            }
        }
        compose.onNodeWithText("מצב כהה").assertIsDisplayed()
    }

    @Test
    fun settingsStoreEncryptsAndReadsBack() {
        val settings = SettingsStore(context)
        settings.aiKeysOverride = "gsk_instrumentation_test_key"
        assertEquals("gsk_instrumentation_test_key", settings.aiKeysOverride)

        settings.pinHash = com.familymoney.util.Security.hashPin("1234")
        assertEquals(com.familymoney.util.Security.hashPin("1234"), settings.pinHash)
        assertTrue(settings.pinHash != com.familymoney.util.Security.hashPin("4321"))

        settings.aiKeysOverride = null
        settings.pinHash = null
    }

    @Test
    fun qrCodeGeneratesForAnInviteLink() {
        val bitmap = com.familymoney.util.Qr.generate("familymoney://join?code=ABCD1234", 256)
        assertNotNull("QR generation should succeed", bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)
    }
}
