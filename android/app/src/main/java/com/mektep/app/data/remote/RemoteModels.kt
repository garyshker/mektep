package com.mektep.app.data.remote

/**
 * Data classes for Firebase Realtime Database serialization.
 *
 * RTDB schema:
 * /families/{familyId}/
 *     info/ { createdAt, inviteCode, inviteExpiresAt }
 *     members/{userId}/ { role, displayName, deviceId, fcmToken, lastSeen }
 *     config/ { dailyLimitMinutes, bedtimeStart, bedtimeEnd, allowedApps/{pkg}/ }
 *     status/{childUserId}/ { balanceSecs, childModeActive, currentApp, lastUpdated }
 *     events/{pushId}/ { type, fromUserId, amountSeconds, timestamp }
 */

data class FamilyInfo(
    val createdAt: Long = 0,
    val inviteCode: String = "",
    val inviteExpiresAt: Long = 0
)

data class FamilyMemberRemote(
    val role: String = "",
    val displayName: String = "",
    val deviceId: String = "",
    val fcmToken: String = "",
    val lastSeen: Long = 0
)

data class RemoteConfig(
    val dailyLimitMinutes: Int = 60,
    val bedtimeStart: String? = null,
    val bedtimeEnd: String? = null,
    val allowedApps: Map<String, RemoteAllowedApp> = emptyMap()
)

data class RemoteAllowedApp(
    val appLabel: String = "",
    val needsEarnedTime: Boolean = true,
    val dailyLimitMinutes: Int? = null
)

data class ChildStatus(
    val balanceSecs: Int = 0,
    val childModeActive: Boolean = false,
    val currentApp: String? = null,
    val lastUpdated: Long = 0
)

data class FamilyEvent(
    val type: String = "", // BONUS_TIME, CONFIG_CHANGED
    val fromUserId: String = "",
    val amountSeconds: Int = 0,
    val timestamp: Long = 0
)
