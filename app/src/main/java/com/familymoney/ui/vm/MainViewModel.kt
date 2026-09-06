package com.familymoney.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familymoney.AppContainer
import com.familymoney.data.db.AccountEntity
import com.familymoney.data.db.BudgetEntity
import com.familymoney.data.db.CardEntity
import com.familymoney.data.db.ChildEntity
import com.familymoney.data.db.FamilyMemberEntity
import com.familymoney.data.db.GoalEntity
import com.familymoney.data.db.InvestmentEntity
import com.familymoney.data.db.LiabilityEntity
import com.familymoney.data.db.NetWorthSnapshotEntity
import com.familymoney.data.db.ProfileEntity
import com.familymoney.data.db.RecurringEntity
import com.familymoney.data.db.TransactionEntity
import com.familymoney.data.model.TxType
import com.familymoney.data.repo.MonthTotal
import com.familymoney.data.seed.DemoData
import com.familymoney.data.sync.SyncResult
import com.familymoney.data.sync.SyncState
import com.familymoney.engine.CashFlowForecast
import com.familymoney.engine.FinanceSnapshot
import com.familymoney.engine.Insight
import com.familymoney.engine.Recommendation
import com.familymoney.engine.RecommendationEngine
import com.familymoney.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository
    val settings = container.settings

    // ------------------------------------------------------------- raw streams

    val profile: StateFlow<ProfileEntity?> =
        repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val transactions: StateFlow<List<TransactionEntity>> =
        repo.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val accounts: StateFlow<List<AccountEntity>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cards: StateFlow<List<CardEntity>> =
        repo.cards.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val budgets: StateFlow<List<BudgetEntity>> =
        repo.budgets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val goals: StateFlow<List<GoalEntity>> =
        repo.goals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val investments: StateFlow<List<InvestmentEntity>> =
        repo.investments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val children: StateFlow<List<ChildEntity>> =
        repo.children.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recurring: StateFlow<List<RecurringEntity>> =
        repo.recurring.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val members: StateFlow<List<FamilyMemberEntity>> =
        repo.members.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val liabilities: StateFlow<List<LiabilityEntity>> =
        repo.liabilities.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val snapshots: StateFlow<List<NetWorthSnapshotEntity>> =
        repo.snapshots.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val auditLogFlow: StateFlow<List<com.familymoney.data.db.AuditEntity>> =
        repo.auditLog.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthlyTotals: StateFlow<List<MonthTotal>> =
        repo.monthlyTotals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ------------------------------------------------------- derived analytics

    val snapshot: StateFlow<FinanceSnapshot?> =
        repo.snapshot.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val recommendations: StateFlow<List<Recommendation>> = repo.snapshot
        .map { RecommendationEngine.analyze(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val insights: StateFlow<List<Insight>> = repo.snapshot
        .map { RecommendationEngine.insights(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val forecast: StateFlow<CashFlowForecast?> = repo.snapshot
        .map { RecommendationEngine.forecast(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val syncStatus: StateFlow<SyncState> =
        container.syncManager.status.stateIn(viewModelScope, SharingStarted.Eagerly, SyncState.Disabled)

    val syncAvailable: Boolean get() = container.syncAvailable
    val aiConfigured: Boolean get() = container.aiConfigured

    // --------------------------------------------------------------- UI state

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun showToast(message: String) { _toast.value = message }
    fun clearToast() { _toast.value = null }

    val onboardingDone: StateFlow<Boolean> = profile
        .map { it?.onboardingDone == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val displayName: StateFlow<String> = profile
        .map { it?.name?.ifBlank { "משתמש" } ?: "משתמש" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val familyLabel: StateFlow<String> = combine(profile, members) { p, m ->
        val name = p?.familyName.orEmpty()
        when {
            name.isBlank() -> ""
            m.size > 1 -> "$name · ${m.size} שותפים"
            else -> name
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // ------------------------------------------------------------------ writes

    private fun afterWrite() {
        container.syncManager.schedulePush()
    }

    fun saveProfile(update: (ProfileEntity) -> ProfileEntity) = viewModelScope.launch {
        val current = repo.getProfile() ?: ProfileEntity()
        repo.saveProfile(update(current))
        afterWrite()
    }

    fun addTransaction(tx: TransactionEntity) = viewModelScope.launch {
        repo.addTransaction(tx.copy(createdByName = profile.value?.name.orEmpty()))
        repo.refreshRecurringDetection()
        afterWrite()
        showToast(if (tx.type == TxType.INCOME) "ההכנסה נוספה" else "ההוצאה נוספה")
    }

    fun updateTransaction(tx: TransactionEntity) = viewModelScope.launch {
        repo.updateTransaction(tx)
        afterWrite()
        showToast("העסקה עודכנה")
    }

    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch {
        repo.deleteTransaction(tx)
        afterWrite()
        showToast("העסקה נמחקה")
    }

    fun importTransactions(list: List<TransactionEntity>) = viewModelScope.launch {
        repo.importTransactions(list)
        repo.refreshRecurringDetection()
        afterWrite()
        showToast("יובאו ${list.size} עסקאות")
    }

    fun saveAccount(a: AccountEntity) = viewModelScope.launch {
        repo.saveAccount(a); afterWrite(); showToast("החשבון נשמר")
    }

    fun deleteAccount(a: AccountEntity) = viewModelScope.launch {
        repo.deleteAccount(a); afterWrite(); showToast("החשבון הוסר")
    }

    fun saveCard(c: CardEntity) = viewModelScope.launch {
        repo.saveCard(c); afterWrite(); showToast("הכרטיס נשמר")
    }

    fun deleteCard(c: CardEntity) = viewModelScope.launch {
        repo.deleteCard(c); afterWrite(); showToast("הכרטיס הוסר")
    }

    fun saveBudget(b: BudgetEntity) = viewModelScope.launch {
        repo.saveBudget(b); afterWrite()
    }

    fun deleteBudget(b: BudgetEntity) = viewModelScope.launch {
        repo.deleteBudget(b); afterWrite()
    }

    fun saveGoal(g: GoalEntity) = viewModelScope.launch {
        repo.saveGoal(g); afterWrite(); showToast("היעד נשמר")
    }

    fun deleteGoal(g: GoalEntity) = viewModelScope.launch {
        repo.deleteGoal(g); afterWrite(); showToast("היעד נמחק")
    }

    fun contributeToGoal(goal: GoalEntity, amount: Double, fromAccountId: String?) =
        viewModelScope.launch {
            repo.contributeToGoal(goal, amount, fromAccountId, profile.value?.name.orEmpty())
            afterWrite()
            showToast("נוספו ${com.familymoney.util.Money.format(amount)} ל${goal.name}")
        }

    fun saveInvestment(i: InvestmentEntity) = viewModelScope.launch {
        repo.saveInvestment(i); afterWrite(); showToast("ההשקעה נשמרה")
    }

    fun deleteInvestment(i: InvestmentEntity) = viewModelScope.launch {
        repo.deleteInvestment(i); afterWrite()
    }

    fun saveChild(c: ChildEntity) = viewModelScope.launch {
        repo.saveChild(c); afterWrite(); showToast("נשמר")
    }

    fun deleteChild(c: ChildEntity) = viewModelScope.launch {
        repo.deleteChild(c); afterWrite()
    }

    fun saveRecurring(r: RecurringEntity) = viewModelScope.launch {
        repo.saveRecurring(r); afterWrite()
    }

    fun deleteRecurring(r: RecurringEntity) = viewModelScope.launch {
        repo.deleteRecurring(r); afterWrite()
    }

    fun saveLiability(l: LiabilityEntity) = viewModelScope.launch {
        repo.saveLiability(l); afterWrite()
    }

    fun deleteLiability(l: LiabilityEntity) = viewModelScope.launch {
        repo.deleteLiability(l); afterWrite()
    }

    suspend fun suggestCategory(merchant: String) = repo.suggestCategory(merchant)

    // ------------------------------------------------------------ family sync

    fun createFamily(name: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        _busy.value = true
        val ownerName = profile.value?.name.orEmpty().ifBlank { "בעל החשבון" }
        when (val r = container.syncManager.createFamily(name, ownerName)) {
            is SyncResult.Ok -> {
                saveProfile { it.copy(familyId = r.value.familyId, familyName = name) }
                onDone(r.value.inviteCode)
                showToast("המשפחה נוצרה")
            }
            is SyncResult.Err -> { onDone(null); showToast(r.message) }
        }
        _busy.value = false
    }

    fun joinFamily(code: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        _busy.value = true
        val myName = profile.value?.name.orEmpty().ifBlank { "שותף" }
        when (val r = container.syncManager.joinFamily(code, myName)) {
            is SyncResult.Ok -> {
                saveProfile {
                    it.copy(
                        familyId = r.value.familyId,
                        familyName = r.value.familyName,
                        role = com.familymoney.data.model.MemberRole.PARTNER
                    )
                }
                onDone(true)
                showToast("הצטרפת ל${r.value.familyName}")
            }
            is SyncResult.Err -> { onDone(false); showToast(r.message) }
        }
        _busy.value = false
    }

    fun createInvite(onDone: (String?) -> Unit) = viewModelScope.launch {
        when (val r = container.syncManager.createInvite()) {
            is SyncResult.Ok -> onDone(r.value)
            is SyncResult.Err -> { onDone(null); showToast(r.message) }
        }
    }

    fun revokeInvite(code: String) = viewModelScope.launch {
        container.syncManager.revokeInvite(code)
        showToast("קוד ההזמנה בוטל")
    }

    fun removeMember(uid: String) = viewModelScope.launch {
        container.syncManager.removeMember(uid)
        showToast("השותף הוסר")
    }

    fun leaveFamily() = viewModelScope.launch {
        container.syncManager.leaveFamily()
        saveProfile { it.copy(familyId = "", familyName = "") }
        showToast("יצאת מהמשפחה")
    }

    fun syncNow() = viewModelScope.launch {
        _busy.value = true
        when (val r = container.syncManager.pushNow()) {
            is SyncResult.Ok -> showToast("סונכרן")
            is SyncResult.Err -> showToast(r.message)
        }
        _busy.value = false
    }

    // ------------------------------------------------------------------- demo

    fun loadDemoData() = viewModelScope.launch {
        _busy.value = true
        DemoData.populate(container.database, profile.value?.name.orEmpty().ifBlank { "משה" })
        repo.refreshRecurringDetection()
        settings.demoDataLoaded = true
        afterWrite()
        _busy.value = false
        showToast("נתוני הדגמה נטענו")
    }

    fun clearAllData() = viewModelScope.launch {
        _busy.value = true
        repo.wipeAll()
        settings.demoDataLoaded = false
        _busy.value = false
        showToast("כל הנתונים נמחקו")
    }

    /** Section 34: keep one net-worth point per month for the trend chart. */
    fun captureSnapshotIfNeeded() = viewModelScope.launch {
        val s = snapshot.value ?: return@launch
        val key = Dates.yearMonthKey(s.today)
        if (settings.lastSnapshotMonth == key) return@launch
        repo.captureNetWorthSnapshot(s)
        settings.lastSnapshotMonth = key
    }

    fun rescanRecurring() = viewModelScope.launch {
        repo.refreshRecurringDetection()
        showToast("הסריקה הסתיימה")
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container) as T
    }
}
