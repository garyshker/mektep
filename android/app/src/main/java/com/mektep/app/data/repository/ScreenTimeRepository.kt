package com.mektep.app.data.repository

import com.mektep.app.data.api.MektepApi
import com.mektep.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeRepository @Inject constructor(
    private val api: MektepApi
) {
    suspend fun getBalance(childId: String): ScreenTimeBalance =
        api.getScreenTimeBalance(childId)

    suspend fun spend(childId: String, seconds: Int, appName: String): ScreenTimeBalance =
        api.spendScreenTime(childId, SpendRequest(seconds, appName))

    suspend fun grantBonus(childId: String, minutes: Int, reason: String): ScreenTimeBalance =
        api.grantBonus(childId, BonusRequest(minutes, reason))

    suspend fun getConfig(childId: String): ScreenTimeConfig =
        api.getScreenTimeConfig(childId)

    suspend fun updateConfig(childId: String, config: ScreenTimeConfig): ScreenTimeConfig =
        api.updateScreenTimeConfig(childId, config)
}
