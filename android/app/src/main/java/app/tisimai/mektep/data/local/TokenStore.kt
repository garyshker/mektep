package app.tisimai.mektep.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "mektep_tokens")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Quick static access to last known language for non-Compose contexts */
        @Volatile var lastLanguage: String = "en"

        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val CHILD_ID = stringPreferencesKey("child_id")
        private val LANGUAGE = stringPreferencesKey("language")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE] }
    val childId: Flow<String?> = context.dataStore.data.map { it[CHILD_ID] }
    val language: Flow<String> = context.dataStore.data.map { (it[LANGUAGE] ?: "en").also { lang -> lastLanguage = lang } }

    suspend fun saveAuth(accessToken: String, refreshToken: String, userId: String, role: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN] = accessToken
            it[REFRESH_TOKEN] = refreshToken
            it[USER_ID] = userId
            it[USER_ROLE] = role
        }
    }

    suspend fun saveChildId(childId: String) {
        context.dataStore.edit { it[CHILD_ID] = childId }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
