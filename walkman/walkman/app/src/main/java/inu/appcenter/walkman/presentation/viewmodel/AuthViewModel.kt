package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.repository.AuthRepository
import inu.appcenter.walkman.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager // SessionManager 주입
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    init {
        // 앱 시작 시 세션 매니저에서 로그인 상태 불러오기
        val isLoggedIn = sessionManager.isLoggedIn()
        _authState.update { it.copy(isLoggedIn = isLoggedIn) }

        // 현재 사용자 ID 가져오기 (선택적)
        val userId = sessionManager.getUserId()
        _authState.update { it.copy(userId = userId) }

        // 리포지토리 로그인 상태 관찰 (필요한 경우)
        viewModelScope.launch {
            authRepository.isUserLoggedIn().collect { repoLoggedIn ->
                // 세션 매니저와 싱크 유지
                if (repoLoggedIn != sessionManager.isLoggedIn()) {
                    sessionManager.saveLoginState(repoLoggedIn)
                }
                _authState.update { it.copy(isLoggedIn = repoLoggedIn) }
            }
        }
    }
    // Google 로그인
    fun signInWithGoogle() {
        _authState.update { it.copy(isLoading = true, error = null) }

        authRepository.signInWithGoogle()
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
                        // 로그인 성공 시 세션 매니저에 상태 저장
                        sessionManager.saveLoginState(true)

                        // 사용자 ID도 저장 (있는 경우)
                        authRepository.getCurrentUserId()?.let { userId ->
                            sessionManager.saveUserId(userId)
                        }

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
            .launchIn(viewModelScope)
    }

    // AuthViewModel.kt의 signUpWithEmail 메서드 확인
    fun signUpWithEmail(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        authRepository.signUpWithEmail(email, password)
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
                        // 회원가입 성공 시 세션 매니저에 상태 저장
                        sessionManager.saveLoginState(true)

                        // 사용자 ID 저장 (있는 경우)
                        authRepository.getCurrentUserId()?.let { userId ->
                            sessionManager.saveUserId(userId)
                        }

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
            .launchIn(viewModelScope)
    }

    // 이메일 로그인
    fun signInWithEmail(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        authRepository.signInWithEmail(email, password)
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
                        // 로그인 성공 시 세션 매니저에 상태 저장
                        sessionManager.saveLoginState(true)

                        // 사용자 ID도 저장 (있는 경우)
                        authRepository.getCurrentUserId()?.let { userId ->
                            sessionManager.saveUserId(userId)
                        }

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
            .launchIn(viewModelScope)
    }

    // 로그아웃
    fun signOut() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }

            authRepository.signOut()
                .onSuccess {
                    // 세션 매니저에서 세션 클리어
                    sessionManager.clearSession()

                    _authState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            userId = null
                        )
                    }
                }
                .onFailure { exception ->
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    // 온보딩 완료 상태 체크
    fun isOnboardingCompleted(): Boolean {
        return sessionManager.isOnboardingCompleted()
    }

    // 온보딩 완료 표시
    fun setOnboardingCompleted() {
        sessionManager.setOnboardingCompleted(true)
    }

    // 오류 메시지 초기화
    fun clearError() {
        _authState.update { it.copy(error = null) }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val error: String? = null
)