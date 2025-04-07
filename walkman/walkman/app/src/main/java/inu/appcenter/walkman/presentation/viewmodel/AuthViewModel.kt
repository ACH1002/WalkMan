package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.AuthResponse
import inu.appcenter.walkman.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    init {
        // 로그인 상태 관찰
        viewModelScope.launch {
            authRepository.isUserLoggedIn().collect { isLoggedIn ->
                _authState.update { it.copy(isLoggedIn = isLoggedIn) }
            }
        }

        // 현재 사용자 ID 가져오기
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            _authState.update { it.copy(userId = userId) }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        authRepository.signUpWithEmail(email, password)
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
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

    fun signInWithEmail(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        authRepository.signInWithEmail(email, password)
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
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

    fun signInWithGoogle() {
        _authState.update { it.copy(isLoading = true, error = null) }

        authRepository.signInWithGoogle()
            .onEach { response ->
                when (response) {
                    is AuthResponse.Success -> {
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

    fun signOut() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }

            authRepository.signOut()
                .onSuccess {
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