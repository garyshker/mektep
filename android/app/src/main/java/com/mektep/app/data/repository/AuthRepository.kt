package com.mektep.app.data.repository

import com.mektep.app.data.api.MektepApi
import com.mektep.app.data.local.TokenStore
import com.mektep.app.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: MektepApi,
    private val tokenStore: TokenStore
) {
    suspend fun register(
        email: String,
        password: String,
        role: String,
        language: String,
        displayName: String? = null,
        gradeLevel: Int? = null
    ): AuthResponse {
        val response = api.register(
            RegisterRequest(
                email = email,
                password = password,
                role = role,
                languagePreference = language,
                displayName = displayName,
                gradeLevel = gradeLevel
            )
        )
        tokenStore.saveAuth(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
            role = response.user.role
        )
        return response
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response = api.login(LoginRequest(email, password))
        tokenStore.saveAuth(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userId = response.user.id,
            role = response.user.role
        )
        return response
    }

    suspend fun logout() {
        tokenStore.clear()
    }
}
