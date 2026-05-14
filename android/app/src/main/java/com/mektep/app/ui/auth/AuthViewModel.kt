package com.mektep.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mektep.app.data.local.TokenStore
import com.mektep.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val role: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    val isLoggedIn: Flow<Boolean> = tokenStore.accessToken.map { !it.isNullOrEmpty() }
    val userRole: Flow<String?> = tokenStore.userRole

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = authRepository.login(email, password)
                _uiState.value = AuthUiState(success = true, role = response.user.role)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Login failed")
            }
        }
    }

    fun register(
        email: String,
        password: String,
        role: String,
        language: String,
        displayName: String? = null,
        gradeLevel: Int? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = authRepository.register(email, password, role, language, displayName, gradeLevel)
                _uiState.value = AuthUiState(success = true, role = response.user.role)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
