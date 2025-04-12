package inu.appcenter.walkman.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.data.repository.user.UserProfileRepository
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.model.AuthState
import inu.appcenter.walkman.domain.repository.AuthRepository
import inu.appcenter.walkman.utils.SessionManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 인증 관련 ViewModel
 * 로그인, 회원가입, 로그아웃 및 인증 상태 확인 등의 기능 제공
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    // 인증 상태 관리
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // UI 상태 관리
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // 앱 시작 시 인증 상태 확인
        checkAuthState()
    }

    /**
     * 현재 인증 상태 확인
     * - 토큰 유효성 검증
     * - 사용자 프로필 존재 여부 확인
     */
    fun checkAuthState() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 로그아웃 직후라면 바로 Unauthenticated 상태로 설정
                if (sessionManager.wasJustLoggedOut()) {
                    Log.d("AuthViewModel", "로그아웃 직후 상태")
                    sessionManager.setJustLoggedOut(false) // 로그아웃 플래그 초기화
                    _authState.value = AuthState.Unauthenticated
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                    return@launch
                }

                // 토큰 확인
                val token = sessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.d("AuthViewModel", "토큰이 없음 - 로그인 필요")
                    _authState.value = AuthState.Unauthenticated
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                    return@launch
                }

                try {
                    // 토큰 유효성 검증
                    supabaseClient.supabase.auth.retrieveUser(token)
                    supabaseClient.supabase.auth.refreshCurrentSession()

                    // 토큰 갱신
                    val newToken = supabaseClient.supabase.auth.currentAccessTokenOrNull()
                    if (newToken.isNullOrEmpty()) {
                        Log.d("AuthViewModel", "토큰 갱신 실패")
                        sessionManager.clearToken()
                        _authState.value = AuthState.Unauthenticated
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                        return@launch
                    }

                    // 토큰 저장
                    sessionManager.saveToken(newToken)

                    // 사용자 ID 가져오기
                    val userId = supabaseClient.getCurrentUserId()

                    // 사용자 프로필 확인
                    val userProfiles = userProfileRepository.getUserProfiles()

                    if (userProfiles.isEmpty()) {
                        Log.d("AuthViewModel", "인증됨 - 프로필 없음")
                        _authState.value = AuthState.AuthenticatedWithoutProfile
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isUserInfoCompleted = false) }
                    } else {
                        Log.d("AuthViewModel", "인증됨 - 프로필 있음")
                        _authState.value = AuthState.Authenticated(userId ?: "")
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isUserInfoCompleted = true) }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "토큰 검증 오류: ${e.message}")
                    sessionManager.clearToken()
                    _authState.value = AuthState.Unauthenticated
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = false, error = "인증 세션이 만료되었습니다") }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "인증 상태 확인 중 오류: ${e.message}")
                _authState.value = AuthState.Unauthenticated
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * 이메일로 회원가입
     */
    fun signUpWithEmail(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signUpWithEmail(email, password)
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            // 토큰 저장
                            sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())

                            // 회원가입 성공 - 프로필 없는 상태로 설정
                            _authState.value = AuthState.AuthenticatedWithoutProfile
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    isUserInfoCompleted = false
                                )
                            }
                        }
                        is AuthResponse.Error -> {
                            _uiState.update {
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

    /**
     * 이메일로 로그인
     */
    fun signInWithEmail(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signInWithEmail(email, password)
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            // 토큰 저장
                            sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())

                            // 로그인 성공 후 프로필 확인
                            val userProfiles = userProfileRepository.getUserProfiles()
                            val userId = supabaseClient.getCurrentUserId()

                            if (userProfiles.isEmpty()) {
                                // 프로필이 없는 경우
                                _authState.value = AuthState.AuthenticatedWithoutProfile
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = true,
                                        isUserInfoCompleted = false
                                    )
                                }
                            } else {
                                // 프로필이 있는 경우
                                _authState.value = AuthState.Authenticated(userId ?: "")
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = true,
                                        isUserInfoCompleted = true
                                    )
                                }
                            }
                        }
                        is AuthResponse.Error -> {
                            _uiState.update {
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

    /**
     * Google로 로그인
     */
    fun signInWithGoogle() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.signInWithGoogle()
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            // 토큰 저장
                            sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())

                            // 로그인 성공 후 프로필 확인
                            val userProfiles = userProfileRepository.getUserProfiles()
                            val userId = supabaseClient.getCurrentUserId()

                            if (userProfiles.isEmpty()) {
                                // 프로필이 없는 경우
                                _authState.value = AuthState.AuthenticatedWithoutProfile
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = true,
                                        isUserInfoCompleted = false
                                    )
                                }
                            } else {
                                // 프로필이 있는 경우
                                _authState.value = AuthState.Authenticated(userId ?: "")
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = true,
                                        isUserInfoCompleted = true
                                    )
                                }
                            }
                        }
                        is AuthResponse.Error -> {
                            _uiState.update {
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

    /**
     * 로그아웃
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, logoutCompleted = false) }

            authRepository.signOut()
                .collect { response ->
                    when (response) {
                        is AuthResponse.Success -> {
                            // 세션 매니저에서 토큰 삭제 및 로그아웃 상태 설정
                            sessionManager.clearToken()
                            sessionManager.setJustLoggedOut(true)

                            _authState.value = AuthState.Unauthenticated
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = false,
                                    userId = null,
                                    logoutCompleted = true
                                )
                            }
                        }
                        is AuthResponse.Error -> {
                            _uiState.update {
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

    /**
     * 사용자 프로필 생성 완료 알림
     */
    fun notifyProfileCreated() {
        viewModelScope.launch {
            val userId = supabaseClient.getCurrentUserId()
            _authState.value = AuthState.Authenticated(userId ?: "")
            _uiState.update { it.copy(isUserInfoCompleted = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetLogoutCompleted() {
        _uiState.update { it.copy(logoutCompleted = false) }
    }
}

/**
 * 인증 UI 상태 클래스
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val logoutCompleted: Boolean = false,
    val isUserInfoCompleted: Boolean = false,
    val userId: String? = null,
    val error: String? = null
)