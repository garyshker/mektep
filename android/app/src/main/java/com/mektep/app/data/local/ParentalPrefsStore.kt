package com.mektep.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.parentalDataStore by preferencesDataStore(name = "mektep_parental")

/**
 * Quick-access DataStore for parental control settings.
 * Services (AppBlockerService, ScreenTimeService) read from here
 * because they can't easily access Room via Hilt.
 */
@Singleton
class ParentalPrefsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DEVICE_MODE = stringPreferencesKey("device_mode") // NONE, SAME_DEVICE, REMOTE_PARENT, REMOTE_CHILD
        private val CHILD_MODE_ACTIVE = booleanPreferencesKey("child_mode_active")
        private val PIN_HASH = stringPreferencesKey("pin_hash")
        private val PIN_SALT = stringPreferencesKey("pin_salt")
        private val FAMILY_ID = stringPreferencesKey("family_id")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val deviceMode: Flow<String> = context.parentalDataStore.data.map { it[DEVICE_MODE] ?: "NONE" }
    val childModeActive: Flow<Boolean> = context.parentalDataStore.data.map { it[CHILD_MODE_ACTIVE] ?: false }
    val pinHash: Flow<String?> = context.parentalDataStore.data.map { it[PIN_HASH] }
    val pinSalt: Flow<String?> = context.parentalDataStore.data.map { it[PIN_SALT] }
    val familyId: Flow<String?> = context.parentalDataStore.data.map { it[FAMILY_ID] }
    val setupCompleted: Flow<Boolean> = context.parentalDataStore.data.map { it[SETUP_COMPLETED] ?: false }

    suspend fun setDeviceMode(mode: String) {
        context.parentalDataStore.edit { it[DEVICE_MODE] = mode }
    }

    suspend fun setChildModeActive(active: Boolean) {
        context.parentalDataStore.edit { it[CHILD_MODE_ACTIVE] = active }
    }

    suspend fun savePin(hash: String, salt: String) {
        context.parentalDataStore.edit {
            it[PIN_HASH] = hash
            it[PIN_SALT] = salt
        }
    }

    suspend fun setFamilyId(id: String) {
        context.parentalDataStore.edit { it[FAMILY_ID] = id }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.parentalDataStore.edit { it[SETUP_COMPLETED] = completed }
    }

    suspend fun clear() {
        context.parentalDataStore.edit { it.clear() }
    }
}
