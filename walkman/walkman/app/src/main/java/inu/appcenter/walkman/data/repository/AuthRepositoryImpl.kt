package inu.appcenter.walkman.data.repository

import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.repository.AuthRepository
import inu.appcenter.walkman.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: SessionManager
) : AuthRepository {


    override fun signUpWithEmail(email: String, password: String): Flow<AuthResponse> = flow {
        try {
            supabaseClient.signUpWithEmail(email, password)
            emit(AuthResponse.Success)
        } catch (e: Exception) {
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    // 다른 로그인 메서드도 유사하게 수정
    override fun signInWithEmail(email: String, password: String): Flow<AuthResponse> = flow {
        try {
            supabaseClient.signInWithEmail(email, password).collect{
                if (it == AuthResponse.Success){
                    emit(AuthResponse.Success)
                } else {
                    emit(AuthResponse.Error(it.toString()))
                }
            }
        } catch (e: Exception) {
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    override fun signInWithGoogle(): Flow<AuthResponse> = flow {
        try {
            supabaseClient.loginGoogleUser().collect{
                if(it == AuthResponse.Success){
                    emit(AuthResponse.Success)
                } else {
                    emit(AuthResponse.Error(it.toString()))
                }
            }
        } catch (e: Exception) {
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.signOut()
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}