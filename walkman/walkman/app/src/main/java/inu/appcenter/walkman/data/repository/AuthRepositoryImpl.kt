package inu.appcenter.walkman.data.repository

import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.repository.AuthRepository
import inu.appcenter.walkman.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(false)

    init {
        // 초기 세션 상태 확인
        val sessionStatus = supabaseClient.getSessionStatus()
        _isLoggedIn.value = sessionStatus != null

        // SessionManager 상태와 동기화
        if (sessionStatus != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val userId = supabaseClient.getCurrentUserId()
                if (userId != null) {
                    // SessionManager 업데이트
                    sessionManager.saveLoginState(true)
                    sessionManager.saveUserId(userId)
                }
            }
        }
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