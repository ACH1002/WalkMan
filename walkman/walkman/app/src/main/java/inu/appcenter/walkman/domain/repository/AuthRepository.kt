package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.AuthResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signUpWithEmail(email: String, password: String): Flow<AuthResponse>
    fun signInWithEmail(email: String, password: String): Flow<AuthResponse>
    fun signInWithGoogle(): Flow<AuthResponse>
    suspend fun signOut(): Result<Unit>
}