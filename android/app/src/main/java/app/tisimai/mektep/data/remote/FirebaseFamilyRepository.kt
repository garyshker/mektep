package app.tisimai.mektep.data.remote

import com.google.firebase.database.*
import app.tisimai.mektep.data.local.*
import app.tisimai.mektep.data.models.AllowedApp
import app.tisimai.mektep.data.models.ParentalConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFamilyRepository @Inject constructor(
    private val parentalConfigDao: ParentalConfigDao,
    private val allowedAppDao: AllowedAppDao,
    private val parentalPrefsStore: ParentalPrefsStore,
    private val tokenStore: TokenStore
) {
    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    // ── Parent-side methods ──

    suspend fun createFamily(displayName: String, userId: String): String {
        val familyId = UUID.randomUUID().toString().take(12)
        val inviteCode = generateInviteCode()

        val info = FamilyInfo(
            createdAt = System.currentTimeMillis(),
            inviteCode = inviteCode,
            inviteExpiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24h
        )
        db.child("families").child(familyId).child("info").setValue(info).await()

        val member = FamilyMemberRemote(
            role = "PARENT",
            displayName = displayName,
            deviceId = UUID.randomUUID().toString().take(8),
            lastSeen = System.currentTimeMillis()
        )
        db.child("families").child(familyId).child("members").child(userId).setValue(member).await()

        // Save locally
        parentalPrefsStore.setFamilyId(familyId)
        parentalConfigDao.upsertConfig(
            ParentalConfig(
                id = familyId,
                mode = "REMOTE_PARENT",
                familyId = familyId,
                inviteCode = inviteCode
            )
        )

        return inviteCode
    }

    suspend fun refreshInviteCode(familyId: String): String {
        val code = generateInviteCode()
        val expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        db.child("families").child(familyId).child("info").child("inviteCode").setValue(code).await()
        db.child("families").child(familyId).child("info").child("inviteExpiresAt").setValue(expiresAt).await()
        return code
    }

    suspend fun pushConfig(familyId: String, config: RemoteConfig) {
        db.child("families").child(familyId).child("config").setValue(config).await()
    }

    suspend fun pushAllowedApps(familyId: String, apps: List<AllowedApp>) {
        val remoteApps = apps.associate { it.packageName to RemoteAllowedApp(it.appLabel, it.needsEarnedTime) }
        db.child("families").child(familyId).child("config").child("allowedApps").setValue(remoteApps).await()
    }

    suspend fun grantBonusTime(familyId: String, childId: String, seconds: Int, fromUserId: String) {
        val event = FamilyEvent(
            type = "BONUS_TIME",
            fromUserId = fromUserId,
            amountSeconds = seconds,
            timestamp = System.currentTimeMillis()
        )
        db.child("families").child(familyId).child("events").push().setValue(event).await()
    }

    fun observeChildStatuses(familyId: String): Flow<Map<String, ChildStatus>> = callbackFlow {
        val ref = db.child("families").child(familyId).child("status")
        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statuses = mutableMapOf<String, ChildStatus>()
                snapshot.children.forEach { child ->
                    child.getValue(ChildStatus::class.java)?.let {
                        statuses[child.key ?: ""] = it
                    }
                }
                trySend(statuses)
            }
            override fun onCancelled(error: DatabaseError) { }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Child-side methods ──

    suspend fun joinFamily(inviteCode: String, userId: String, displayName: String): String? {
        // Find family by invite code
        val snapshot = db.child("families").get().await()
        for (familySnap in snapshot.children) {
            val info = familySnap.child("info").getValue(FamilyInfo::class.java)
            if (info != null && info.inviteCode == inviteCode && info.inviteExpiresAt > System.currentTimeMillis()) {
                val familyId = familySnap.key ?: continue

                // Add child as member
                val member = FamilyMemberRemote(
                    role = "CHILD",
                    displayName = displayName,
                    deviceId = UUID.randomUUID().toString().take(8),
                    lastSeen = System.currentTimeMillis()
                )
                db.child("families").child(familyId).child("members").child(userId).setValue(member).await()

                // Save locally
                parentalPrefsStore.setFamilyId(familyId)
                parentalPrefsStore.setDeviceMode("REMOTE_CHILD")
                parentalConfigDao.upsertConfig(
                    ParentalConfig(id = familyId, mode = "REMOTE_CHILD", familyId = familyId)
                )

                return familyId
            }
        }
        return null // code not found or expired
    }

    fun listenForConfig(familyId: String): Flow<RemoteConfig> = callbackFlow {
        val ref = db.child("families").child(familyId).child("config")
        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(RemoteConfig::class.java)?.let { trySend(it) }
            }
            override fun onCancelled(error: DatabaseError) { }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    fun listenForEvents(familyId: String): Flow<FamilyEvent> = callbackFlow {
        val ref = db.child("families").child(familyId).child("events")
        val listener = ref.orderByChild("timestamp")
            .startAt(System.currentTimeMillis().toDouble())
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.getValue(FamilyEvent::class.java)?.let { trySend(it) }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun reportChildStatus(familyId: String, childId: String, status: ChildStatus) {
        db.child("families").child(familyId).child("status").child(childId).setValue(status).await()
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no I/O/0/1 to avoid confusion
        return (1..6).map { chars.random() }.joinToString("")
    }
}
