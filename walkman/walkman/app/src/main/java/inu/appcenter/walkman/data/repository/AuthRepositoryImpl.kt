package inu.appcenter.walkman.data.repository

import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(false)

    init {
        // 로그인 상태 초기화
        _isLoggedIn.value = supabaseClient.getSessionStatus() != null
    }

    override fun isUserLoggedIn(): Flow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun getCurrentUserId(): String? {
        return supabaseClient.getCurrentUserId()
    }

    override fun signUpWithEmail(email: String, password: String): Flow<AuthResponse> {
        return supabaseClient.signUpWithEmail(email, password)
    }

    override fun signInWithEmail(email: String, password: String): Flow<AuthResponse> {
        return supabaseClient.signInWithEmail(email, password)
    }

    override fun signInWithGoogle(): Flow<AuthResponse> {
        return supabaseClient.loginGoogleUser()
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.signOut()
            _isLoggedIn.update { false }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}