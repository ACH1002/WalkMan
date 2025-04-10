package inu.appcenter.walkman.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.repository.AuthRepository
import inu.appcenter.walkman.utils.SessionManager
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    fun isUserLoggedIn() {
        viewModelScope.launch {
            try {
                Log.d("isUserLoggedIn", authState.value.toString())

                // 로그아웃 직후라면 추가 검증 없이 로그아웃 상태 유지
                if (sessionManager.wasJustLoggedOut()) {
                    Log.d("isUserLoggedIn", "로그아웃 직후 상태 감지")
                    _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = false) }
                    return@launch
                }

                _authState.update { it.copy(isLoading = true, error = null, isLoggedIn = false) }

                val token = sessionManager.getToken()
                if(token.isNullOrEmpty()) {
                    _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = false) }
                } else {
                    try {
                        Log.d("isUserLoggedIn", "토큰 검증: $token")
                        supabaseClient.supabase.auth.retrieveUser(token)
                        supabaseClient.supabase.auth.refreshCurrentSession()
                        val newToken = supabaseClient.supabase.auth.currentAccessTokenOrNull()

                        if (newToken.isNullOrEmpty()) {
                            // 토큰 갱신 실패 - 로그아웃 처리
                            Log.d("isUserLoggedIn", "토큰 갱신 실패")
                            sessionManager.clearToken()
                            _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = false) }
                        } else {
                            // 토큰 갱신 성공 - 로그인 처리
                            Log.d("isUserLoggedIn", "토큰 갱신 성공")
                            sessionManager.saveToken(newToken)
                            _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = true) }
                        }
                    } catch (e: Exception) {
                        // 예외 발생 - 로그아웃 처리
                        Log.e("isUserLoggedIn", "토큰 검증 오류: ${e.message}")
                        sessionManager.clearToken()
                        _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = false) }
                    }
                }
            } catch (e: RestException) {
                _authState.update { it.copy(isLoading = false, error = e.message, isLoggedIn = false) }
            }
        }
    }



    fun signInWithGoogle() {
        _authState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signInWithGoogle()
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true
                                )
                            }
                        }
                        is AuthResponse.Error -> {
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            }
                        }
                    }
                }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signUpWithEmail(email, password)
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true
                                )
                            }
                        }
                        is AuthResponse.Error -> {
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            }
                        }
                    }
                }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signInWithEmail(email, password)
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true
                                )
                            }
                        }
                        is AuthResponse.Error -> {
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    error = response.message
                                )
                            }
                        }
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, logoutCompleted = false) }

            authRepository.signOut()
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            // 세션 매니저에서 토큰 삭제 및 로그아웃 상태 설정
                            sessionManager.clearToken()
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = false,
                                    userId = null,
                                    logoutCompleted = true
                                )
                            }
                        }
                        is AuthResponse.Error -> {
                            _authState.update {
                                it.copy(
                                    isLoading = false,
                                    error = it.error ?: response.message,
                                    logoutCompleted = false
                                )
                            }
                        }
                    }
                }
        }
    }

    // SessionManager의 로그아웃 상태 확인 함수 추가
    fun wasJustLoggedOut(): Boolean {
        return sessionManager.wasJustLoggedOut()
    }

    // SessionManager의 로그아웃 상태 초기화 함수 추가
    fun clearLogoutFlag() {
        sessionManager.setJustLoggedOut(false)
    }


    fun isOnboardingCompleted(): Boolean {
        return sessionManager.isOnboardingCompleted()
    }

    fun setOnboardingCompleted() {
        sessionManager.setOnboardingCompleted(true)
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }

    fun resetLogoutCompleted() {
        _authState.update { it.copy(logoutCompleted = false) }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val logoutCompleted: Boolean = false,
    val isUserInfoCompleted: Boolean = false,
    val userId: String? = null,
    val error: String? = null
)