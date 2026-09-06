package com.familymoney.data.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Compiled only when app/google-services.json is present.
 *
 * Layout in Firestore:
 *   families/{familyId}                      -> name, ownerUid, memberUids[], members[]
 *   families/{familyId}/data/current         -> the whole household payload
 *   invites/{code}                           -> familyId, createdAt, revoked
 *
 * Identity is Firebase anonymous auth: no password, no signup, and each device
 * gets a stable uid that the security rules check against memberUids.
 */
object FamilySyncFactory {
    fun create(context: Context): FamilySync = FirestoreSync()
}

class FirestoreSync : FamilySync {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val _state = MutableStateFlow<SyncState>(SyncState.Disabled)

    override val available = true
    override val state: Flow<SyncState> = _state

    private suspend fun uid(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonymous sign-in returned no user")
    }

    override suspend fun createFamily(familyName: String, ownerName: String): SyncResult<FamilyHandle> =
        runCatching {
            _state.value = SyncState.Connecting
            val myUid = uid()
            val familyRef = db.collection("families").document()
            val member = SyncMember(myUid, ownerName, "OWNER", System.currentTimeMillis())
            val code = generateCode()

            familyRef.set(
                mapOf(
                    "name" to familyName,
                    "ownerUid" to myUid,
                    "memberUids" to listOf(myUid),
                    "members" to listOf(SyncMapper.toMap(member)),
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            db.collection("invites").document(code).set(
                mapOf(
                    "familyId" to familyRef.id,
                    "familyName" to familyName,
                    "createdAt" to System.currentTimeMillis(),
                    "revoked" to false
                )
            ).await()

            _state.value = SyncState.Connected(familyName, 1, System.currentTimeMillis())
            FamilyHandle(familyRef.id, familyName, code, myUid, listOf(member))
        }.fold({ SyncResult.Ok(it) }, { fail(it) })

    override suspend fun joinFamily(inviteCode: String, memberName: String): SyncResult<FamilyHandle> =
        runCatching {
            _state.value = SyncState.Connecting
            val code = inviteCode.trim().uppercase()
            val invite = db.collection("invites").document(code).get().await()
            require(invite.exists()) { "קוד ההזמנה לא נמצא." }
            require(invite.getBoolean("revoked") != true) { "קוד ההזמנה בוטל." }

            val familyId = invite.getString("familyId") ?: error("הזמנה פגומה.")
            val myUid = uid()
            val familyRef = db.collection("families").document(familyId)
            val member = SyncMember(myUid, memberName, "PARTNER", System.currentTimeMillis())

            familyRef.set(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(myUid),
                    "members" to FieldValue.arrayUnion(SyncMapper.toMap(member))
                ),
                SetOptions.merge()
            ).await()

            val snap = familyRef.get().await()
            val name = snap.getString("name") ?: invite.getString("familyName") ?: "משפחה"
            val members = readMembers(snap.get("members"))

            _state.value = SyncState.Connected(name, members.size, System.currentTimeMillis())
            FamilyHandle(familyId, name, code, myUid, members)
        }.fold({ SyncResult.Ok(it) }, { fail(it) })

    override suspend fun createInvite(familyId: String): SyncResult<String> = runCatching {
        uid()
        val familySnap = db.collection("families").document(familyId).get().await()
        val code = generateCode()
        db.collection("invites").document(code).set(
            mapOf(
                "familyId" to familyId,
                "familyName" to (familySnap.getString("name") ?: ""),
                "createdAt" to System.currentTimeMillis(),
                "revoked" to false
            )
        ).await()
        code
    }.fold({ SyncResult.Ok(it) }, { fail(it) })

    override suspend fun revokeInvite(inviteCode: String): SyncResult<Unit> = runCatching {
        uid()
        db.collection("invites").document(inviteCode.trim().uppercase())
            .set(mapOf("revoked" to true), SetOptions.merge()).await()
        Unit
    }.fold({ SyncResult.Ok(it) }, { fail(it) })

    override suspend fun leaveFamily(familyId: String): SyncResult<Unit> = runCatching {
        val myUid = uid()
        removeUid(familyId, myUid)
        _state.value = SyncState.Disabled
        Unit
    }.fold({ SyncResult.Ok(it) }, { fail(it) })

    override suspend fun removeMember(familyId: String, memberUid: String): SyncResult<Unit> =
        runCatching {
            uid()
            removeUid(familyId, memberUid)
            Unit
        }.fold({ SyncResult.Ok(it) }, { fail(it) })

    private suspend fun removeUid(familyId: String, target: String) {
        val ref = db.collection("families").document(familyId)
        val snap = ref.get().await()
        val members = readMembers(snap.get("members")).filter { it.uid != target }
        ref.set(
            mapOf(
                "memberUids" to FieldValue.arrayRemove(target),
                "members" to members.map { SyncMapper.toMap(it) }
            ),
            SetOptions.merge()
        ).await()
    }

    override suspend fun push(familyId: String, payload: SyncPayload): SyncResult<Unit> =
        runCatching {
            uid()
            db.collection("families").document(familyId)
                .collection("data").document("current")
                .set(payloadToMap(payload)).await()
            _state.value = SyncState.Connected(
                (_state.value as? SyncState.Connected)?.familyName ?: "",
                payload.members.size.coerceAtLeast(1),
                System.currentTimeMillis()
            )
            Unit
        }.fold({ SyncResult.Ok(it) }, { fail(it) })

    override fun observe(familyId: String): Flow<SyncPayload> = callbackFlow {
        val registration = db.collection("families").document(familyId)
            .collection("data").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.value = SyncState.Failed(error.message ?: "שגיאת סנכרון")
                    return@addSnapshotListener
                }
                val data = snapshot?.data ?: return@addSnapshotListener
                trySend(mapToPayload(data))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun pullOnce(familyId: String): SyncResult<SyncPayload> = runCatching {
        uid()
        val snap = db.collection("families").document(familyId)
            .collection("data").document("current").get().await()
        mapToPayload(snap.data ?: emptyMap())
    }.fold({ SyncResult.Ok(it) }, { fail(it) })

    // ------------------------------------------------------------------ plumbing

    private fun payloadToMap(p: SyncPayload): Map<String, Any?> = mapOf(
        "updatedAt" to p.updatedAt,
        "updatedBy" to p.updatedBy,
        "transactions" to p.transactions,
        "accounts" to p.accounts,
        "cards" to p.cards,
        "budgets" to p.budgets,
        "goals" to p.goals,
        "investments" to p.investments,
        "children" to p.children,
        "recurring" to p.recurring,
        "liabilities" to p.liabilities,
        "members" to p.members.map { SyncMapper.toMap(it) }
    )

    @Suppress("UNCHECKED_CAST")
    private fun mapToPayload(m: Map<String, Any?>): SyncPayload {
        fun rows(key: String) = (m[key] as? List<Map<String, Any?>>) ?: emptyList()
        return SyncPayload(
            updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: 0L,
            updatedBy = m["updatedBy"] as? String ?: "",
            transactions = rows("transactions"),
            accounts = rows("accounts"),
            cards = rows("cards"),
            budgets = rows("budgets"),
            goals = rows("goals"),
            investments = rows("investments"),
            children = rows("children"),
            recurring = rows("recurring"),
            liabilities = rows("liabilities"),
            members = rows("members").map { SyncMapper.toMember(it) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun readMembers(raw: Any?): List<SyncMember> =
        (raw as? List<Map<String, Any?>>)?.map { SyncMapper.toMember(it) } ?: emptyList()

    private fun <T> fail(t: Throwable): SyncResult<T> {
        val message = t.message ?: "שגיאת סנכרון לא ידועה"
        _state.value = SyncState.Failed(message)
        return SyncResult.Err(message)
    }

    /** Ambiguous characters (0/O, 1/I) are excluded so codes read cleanly aloud. */
    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }
}
