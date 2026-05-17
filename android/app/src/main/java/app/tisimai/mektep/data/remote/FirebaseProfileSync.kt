package app.tisimai.mektep.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import app.tisimai.mektep.data.local.ChildProfileDao
import app.tisimai.mektep.data.models.ChildProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs child profiles to/from Firebase Realtime Database.
 * Path: /users/{userId}/children/{childId}
 *
 * Write-through: local writes also push to Firebase.
 * Pull on login: reads all children from Firebase and upserts locally.
 */
@Singleton
class FirebaseProfileSync @Inject constructor(
    private val childProfileDao: ChildProfileDao
) {
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private fun currentUserId(): String? =
        try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }

    /** Push a single child profile to Firebase */
    suspend fun pushChild(child: ChildProfile) {
        val userId = currentUserId() ?: return
        try {
            val data = mapOf(
                "name" to child.name,
                "birthDate" to child.birthDate,
                "avatarEmoji" to child.avatarEmoji,
                "gradeLevel" to child.gradeLevel,
                "xpTotal" to child.xpTotal,
                "currentStreak" to child.currentStreak,
                "longestStreak" to child.longestStreak,
                "lastActiveDate" to (child.lastActiveDate ?: ""),
                "screenTimeBalanceSecs" to child.screenTimeBalanceSecs,
                "dailyLimitMinutes" to child.dailyLimitMinutes,
                "createdAt" to child.createdAt
            )
            db.child("users").child(userId).child("children").child(child.id)
                .setValue(data).await()
        } catch (_: Exception) {
            // Offline — will sync next time
        }
    }

    /** Delete a child profile from Firebase */
    suspend fun deleteChild(childId: String) {
        val userId = currentUserId() ?: return
        try {
            db.child("users").child(userId).child("children").child(childId)
                .removeValue().await()
        } catch (_: Exception) { }
    }

    /** Pull all children from Firebase and upsert into local DB */
    suspend fun pullChildren(): Int {
        val userId = currentUserId() ?: return 0
        return try {
            val snapshot = db.child("users").child(userId).child("children").get().await()
            var count = 0
            for (childSnap in snapshot.children) {
                val childId = childSnap.key ?: continue
                val name = childSnap.child("name").getValue(String::class.java) ?: continue
                val birthDate = childSnap.child("birthDate").getValue(String::class.java) ?: ""
                val avatarEmoji = childSnap.child("avatarEmoji").getValue(String::class.java) ?: "\uD83E\uDDD2"
                val gradeLevel = childSnap.child("gradeLevel").getValue(Int::class.java) ?: 1
                val xpTotal = childSnap.child("xpTotal").getValue(Int::class.java) ?: 0
                val currentStreak = childSnap.child("currentStreak").getValue(Int::class.java) ?: 0
                val longestStreak = childSnap.child("longestStreak").getValue(Int::class.java) ?: 0
                val lastActiveDate = childSnap.child("lastActiveDate").getValue(String::class.java)?.ifEmpty { null }
                val screenTimeBalanceSecs = childSnap.child("screenTimeBalanceSecs").getValue(Int::class.java) ?: 0
                val dailyLimitMinutes = childSnap.child("dailyLimitMinutes").getValue(Int::class.java) ?: 60
                val createdAt = childSnap.child("createdAt").getValue(Long::class.java) ?: 0L

                val existing = childProfileDao.getChild(childId)
                if (existing == null) {
                    // New child from another device — insert locally
                    childProfileDao.insert(
                        ChildProfile(
                            id = childId,
                            parentUserId = userId,
                            name = name,
                            birthDate = birthDate,
                            avatarEmoji = avatarEmoji,
                            gradeLevel = gradeLevel,
                            xpTotal = xpTotal,
                            currentStreak = currentStreak,
                            longestStreak = longestStreak,
                            lastActiveDate = lastActiveDate,
                            screenTimeBalanceSecs = screenTimeBalanceSecs,
                            dailyLimitMinutes = dailyLimitMinutes,
                            createdAt = createdAt
                        )
                    )
                    count++
                } else {
                    // Exists locally — update with remote data if remote is newer
                    if (createdAt >= existing.createdAt) {
                        childProfileDao.update(existing.copy(
                            name = name,
                            birthDate = birthDate,
                            avatarEmoji = avatarEmoji,
                            gradeLevel = gradeLevel,
                            dailyLimitMinutes = dailyLimitMinutes
                        ))
                    }
                }
            }
            count
        } catch (_: Exception) {
            0 // Offline or Firebase not configured
        }
    }

    /** Push all local children to Firebase (for initial sync after first login) */
    suspend fun pushAllChildren(parentUserId: String) {
        try {
            val children = childProfileDao.getChildrenForParentOnce(parentUserId)
            for (child in children) {
                pushChild(child)
            }
        } catch (_: Exception) { }
    }
}
