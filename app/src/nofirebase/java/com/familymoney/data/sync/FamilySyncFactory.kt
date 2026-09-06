package com.familymoney.data.sync

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Compiled when app/google-services.json is absent. The app stays fully usable;
 * only real-time family sharing is unavailable.
 */
object FamilySyncFactory {
    fun create(context: Context): FamilySync = LocalOnlySync()
}

private const val UNAVAILABLE =
    "סנכרון משפחתי אינו פעיל בגרסה זו. יש להוסיף את google-services.json ולבנות מחדש."

class LocalOnlySync : FamilySync {
    override val available = false
    override val state: Flow<SyncState> = MutableStateFlow(SyncState.Disabled)

    override suspend fun createFamily(familyName: String, ownerName: String) =
        SyncResult.Err(UNAVAILABLE)

    override suspend fun joinFamily(inviteCode: String, memberName: String) =
        SyncResult.Err(UNAVAILABLE)

    override suspend fun createInvite(familyId: String) = SyncResult.Err(UNAVAILABLE)
    override suspend fun revokeInvite(inviteCode: String) = SyncResult.Err(UNAVAILABLE)
    override suspend fun leaveFamily(familyId: String) = SyncResult.Err(UNAVAILABLE)
    override suspend fun removeMember(familyId: String, memberUid: String) = SyncResult.Err(UNAVAILABLE)
    override suspend fun push(familyId: String, payload: SyncPayload) = SyncResult.Err(UNAVAILABLE)
    override fun observe(familyId: String): Flow<SyncPayload> = emptyFlow()
    override suspend fun pullOnce(familyId: String) = SyncResult.Err(UNAVAILABLE)
}
