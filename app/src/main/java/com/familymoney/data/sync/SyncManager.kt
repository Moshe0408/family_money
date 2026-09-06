package com.familymoney.data.sync

import com.familymoney.data.db.AppDatabase
import com.familymoney.data.prefs.SettingsStore
import com.familymoney.data.repo.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bridges the local Room database and [FamilySync]. Owns the debounce, the
 * conflict rule and the "who wrote last" bookkeeping so screens never have to.
 */
class SyncManager(
    private val sync: FamilySync,
    private val repo: FinanceRepository,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val scope: CoroutineScope
) {

    private val _status = MutableStateFlow<SyncState>(SyncState.Disabled)
    val status: Flow<SyncState> = _status.asStateFlow()

    private var listenJob: Job? = null

    /** True when this build can talk to a backend at all. */
    val available: Boolean get() = sync.available

    fun startIfConfigured() {
        val familyId = settings.familyId
        if (!sync.available || !settings.syncEnabled || familyId.isNullOrBlank()) {
            _status.value = SyncState.Disabled
            return
        }
        listen(familyId)
    }

    private fun listen(familyId: String) {
        listenJob?.cancel()
        _status.value = SyncState.Connecting
        listenJob = scope.launch {
            sync.observe(familyId).collect { payload ->
                // Ignore the echo of our own push.
                if (payload.updatedBy == currentActor() && payload.updatedAt <= settings.lastSyncAt) {
                    return@collect
                }
                applyRemote(payload)
                settings.lastSyncAt = payload.updatedAt
                _status.value = SyncState.Connected(
                    settings.familyName,
                    payload.members.size.coerceAtLeast(1),
                    payload.updatedAt
                )
            }
        }
    }

    private suspend fun currentActor(): String = repo.getProfile()?.name ?: ""

    suspend fun createFamily(familyName: String, ownerName: String): SyncResult<FamilyHandle> {
        val result = sync.createFamily(familyName, ownerName)
        if (result is SyncResult.Ok) {
            settings.familyId = result.value.familyId
            settings.familyName = familyName
            settings.syncEnabled = true
            pushNow()
            listen(result.value.familyId)
        }
        return result
    }

    suspend fun joinFamily(code: String, memberName: String): SyncResult<FamilyHandle> {
        val result = sync.joinFamily(code, memberName)
        if (result is SyncResult.Ok) {
            settings.familyId = result.value.familyId
            settings.familyName = result.value.familyName
            settings.syncEnabled = true
            // The joining device adopts the family's data rather than pushing
            // its own, so a fresh install never wipes the partner's history.
            when (val pulled = sync.pullOnce(result.value.familyId)) {
                is SyncResult.Ok -> applyRemote(pulled.value)
                is SyncResult.Err -> Unit
            }
            listen(result.value.familyId)
        }
        return result
    }

    suspend fun createInvite(): SyncResult<String> {
        val familyId = settings.familyId ?: return SyncResult.Err("לא מוגדרת משפחה.")
        return sync.createInvite(familyId)
    }

    suspend fun revokeInvite(code: String) = sync.revokeInvite(code)

    suspend fun removeMember(uid: String): SyncResult<Unit> {
        val familyId = settings.familyId ?: return SyncResult.Err("לא מוגדרת משפחה.")
        return sync.removeMember(familyId, uid)
    }

    suspend fun leaveFamily(): SyncResult<Unit> {
        val familyId = settings.familyId ?: return SyncResult.Err("לא מוגדרת משפחה.")
        val result = sync.leaveFamily(familyId)
        listenJob?.cancel()
        settings.familyId = null
        settings.syncEnabled = false
        settings.familyName = ""
        _status.value = SyncState.Disabled
        return result
    }

    /** Uploads the full local dataset. Called after local writes. */
    fun schedulePush() {
        if (!sync.available || !settings.syncEnabled) return
        scope.launch { pushNow() }
    }

    suspend fun pushNow(): SyncResult<Unit> {
        val familyId = settings.familyId ?: return SyncResult.Err("לא מוגדרת משפחה.")
        val payload = SyncPayload(
            updatedAt = System.currentTimeMillis(),
            updatedBy = currentActor(),
            transactions = db.transactionDao().recent(5000).map { SyncMapper.toMap(it) },
            accounts = db.accountDao().all().map { SyncMapper.toMap(it) },
            cards = db.cardDao().all().map { SyncMapper.toMap(it) },
            budgets = db.budgetDao().all().map { SyncMapper.toMap(it) },
            goals = db.goalDao().all().map { SyncMapper.toMap(it) },
            investments = db.investmentDao().all().map { SyncMapper.toMap(it) },
            children = db.childDao().all().map { SyncMapper.toMap(it) },
            recurring = db.recurringDao().all().map { SyncMapper.toMap(it) },
            liabilities = db.liabilityDao().all().map { SyncMapper.toMap(it) }
        )
        val result = sync.push(familyId, payload)
        if (result is SyncResult.Ok) settings.lastSyncAt = payload.updatedAt
        return result
    }

    /**
     * Replaces local rows with the remote set. Whole-collection replace is the
     * conflict rule: the most recent writer wins for the household as a whole,
     * which matches how two partners actually use this — one edits at a time.
     */
    private suspend fun applyRemote(payload: SyncPayload) {
        if (payload.updatedAt == 0L) return

        db.transactionDao().clear()
        db.transactionDao().upsertAll(payload.transactions.map { SyncMapper.toTransaction(it) })

        db.accountDao().clear()
        db.accountDao().upsertAll(payload.accounts.map { SyncMapper.toAccount(it) })

        db.cardDao().clear()
        db.cardDao().upsertAll(payload.cards.map { SyncMapper.toCard(it) })

        db.budgetDao().clear()
        db.budgetDao().upsertAll(payload.budgets.map { SyncMapper.toBudget(it) })

        db.goalDao().clear()
        db.goalDao().upsertAll(payload.goals.map { SyncMapper.toGoal(it) })

        db.investmentDao().clear()
        db.investmentDao().upsertAll(payload.investments.map { SyncMapper.toInvestment(it) })

        db.childDao().clear()
        db.childDao().upsertAll(payload.children.map { SyncMapper.toChild(it) })

        db.recurringDao().clear()
        db.recurringDao().upsertAll(payload.recurring.map { SyncMapper.toRecurring(it) })

        db.liabilityDao().clear()
        db.liabilityDao().upsertAll(payload.liabilities.map { SyncMapper.toLiability(it) })

        if (payload.members.isNotEmpty()) {
            val familyId = settings.familyId ?: ""
            db.familyMemberDao().clear()
            db.familyMemberDao().upsertAll(
                payload.members.map {
                    com.familymoney.data.db.FamilyMemberEntity(
                        id = it.uid,
                        familyId = familyId,
                        userId = it.uid,
                        name = it.name,
                        role = runCatching {
                            com.familymoney.data.model.MemberRole.valueOf(it.role)
                        }.getOrDefault(com.familymoney.data.model.MemberRole.PARTNER),
                        joinedAt = it.joinedAt
                    )
                }
            )
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
    }
}
