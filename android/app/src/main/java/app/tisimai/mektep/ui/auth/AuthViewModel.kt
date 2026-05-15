package app.tisimai.mektep.ui.auth

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import app.tisimai.mektep.data.local.ParentalPrefsStore
import app.tisimai.mektep.data.local.TokenStore
import app.tisimai.mektep.data.local.UserDao
import app.tisimai.mektep.data.models.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val needsOnboarding: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userDao: UserDao,
    private val tokenStore: TokenStore,
    private val parentalPrefsStore: ParentalPrefsStore
) : ViewModel() {

    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Flow<Boolean> = tokenStore.accessToken.map { !it.isNullOrEmpty() }
    val userRole: Flow<String?> = tokenStore.userRole
    val setupCompleted: Flow<Boolean> = parentalPrefsStore.setupCompleted

    init {
        // Check if already signed in (gracefully handle missing Play Services)
        try {
            val currentUser = auth?.currentUser
            if (currentUser != null) {
                _uiState.value = AuthUiState(isLoggedIn = true)
            }
        } catch (_: Exception) { }
    }

    fun handleGoogleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val firebaseAuth = auth ?: throw Exception("Firebase Auth not available")
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val user = authResult.user!!

                // Check if profile exists
                val existing = userDao.getProfileOnce()
                if (existing == null) {
                    // First time — create profile
                    val profile = UserProfile(
                        id = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "Student",
                        photoUrl = user.photoUrl?.toString()
                    )
                    userDao.upsertProfile(profile)
                    tokenStore.saveAuth(
                        accessToken = user.uid,
                        refreshToken = "",
                        userId = user.uid,
                        role = "CHILD"
                    )
                    _uiState.value = AuthUiState(isLoggedIn = true, needsOnboarding = true)
                } else {
                    tokenStore.saveAuth(
                        accessToken = user.uid,
                        refreshToken = "",
                        userId = existing.id,
                        role = existing.role
                    )
                    _uiState.value = AuthUiState(isLoggedIn = true)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Sign in failed")
            }
        }
    }

    fun skipSignIn(language: String = "en") {
        // Offline mode — create a local-only profile
        viewModelScope.launch {
            val profile = UserProfile(
                id = "local_user",
                email = "",
                displayName = "Student",
                language = language
            )
            userDao.upsertProfile(profile)
            tokenStore.saveAuth(
                accessToken = "local",
                refreshToken = "",
                userId = "local_user",
                role = "CHILD"
            )
            tokenStore.saveLanguage(language)
            _uiState.value = AuthUiState(isLoggedIn = true, needsOnboarding = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try { auth?.signOut() } catch (_: Exception) { }
            tokenStore.clear()
            userDao.clear()
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
