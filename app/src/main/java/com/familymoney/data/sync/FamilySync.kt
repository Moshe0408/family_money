package com.familymoney.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * Section 2-3: real-time family sharing.
 *
 * Two implementations exist and the build picks one automatically:
 *  - Firestore-backed, compiled when app/google-services.json is present.
 *  - Local no-op, so the app is fully usable without any backend.
 */
interface FamilySync {

    val available: Boolean

    val state: Flow<SyncState>

    /** Creates a family document and returns its id plus a joinable invite code. */
    suspend fun createFamily(familyName: String, ownerName: String): SyncResult<FamilyHandle>

    /** Joins an existing family using a code produced by [createInvite]. */
    suspend fun joinFamily(inviteCode: String, memberName: String): SyncResult<FamilyHandle>

    /** Issues a fresh, single-use invite code for an existing family. */
    suspend fun createInvite(familyId: String): SyncResult<String>

    suspend fun revokeInvite(inviteCode: String): SyncResult<Unit>

    suspend fun leaveFamily(familyId: String): SyncResult<Unit>

    suspend fun removeMember(familyId: String, memberUid: String): SyncResult<Unit>

    /** Pushes local rows upward. Called after every local write when sync is on. */
    suspend fun push(familyId: String, payload: SyncPayload): SyncResult<Unit>

    /** Emits whenever the other partner changes something. */
    fun observe(familyId: String): Flow<SyncPayload>

    suspend fun pullOnce(familyId: String): SyncResult<SyncPayload>
}

data class FamilyHandle(
    val familyId: String,
    val familyName: String,
    val inviteCode: String,
    val memberUid: String,
    val members: List<SyncMember>
)

data class SyncMember(
    val uid: String,
    val name: String,
    val role: String,
    val joinedAt: Long
)

/**
 * Whole-collection payload. The dataset for one household is small (thousands of
 * rows at most), so replacing it wholesale is simpler and safer than a delta
 * protocol, and Firestore batches it in a single write.
 */
data class SyncPayload(
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "",
    val transactions: List<Map<String, Any?>> = emptyList(),
    val accounts: List<Map<String, Any?>> = emptyList(),
    val cards: List<Map<String, Any?>> = emptyList(),
    val budgets: List<Map<String, Any?>> = emptyList(),
    val goals: List<Map<String, Any?>> = emptyList(),
    val investments: List<Map<String, Any?>> = emptyList(),
    val children: List<Map<String, Any?>> = emptyList(),
    val recurring: List<Map<String, Any?>> = emptyList(),
    val liabilities: List<Map<String, Any?>> = emptyList(),
    val members: List<SyncMember> = emptyList()
)

sealed interface SyncState {
    data object Disabled : SyncState
    data object Connecting : SyncState
    data class Connected(val familyName: String, val memberCount: Int, val lastSyncAt: Long) : SyncState
    data class Failed(val message: String) : SyncState
}

sealed interface SyncResult<out T> {
    data class Ok<T>(val value: T) : SyncResult<T>
    data class Err(val message: String) : SyncResult<Nothing>
}

inline fun <T> SyncResult<T>.onOk(block: (T) -> Unit): SyncResult<T> {
    if (this is SyncResult.Ok) block(value)
    return this
}

inline fun <T> SyncResult<T>.onErr(block: (String) -> Unit): SyncResult<T> {
    if (this is SyncResult.Err) block(message)
    return this
}
