package inu.appcenter.walkman.presentation.viewmodel

import android.content.Context
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
    private val sessionManager: SessionManager,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    fun isUserLoggedIn(
    ) {
        viewModelScope.launch {
            try {
                _authState.update { it.copy(isLoading = true, error = null) }

                val token = sessionManager.getToken()
                Log.d("isUserLoggedIn", token.toString())
                if(token.isNullOrEmpty()) {
                    _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = false) }

                } else {
                    supabaseClient.supabase.auth.retrieveUser(token)
                    supabaseClient.supabase.auth.refreshCurrentSession()
                    sessionManager.saveToken(supabaseClient.supabase.auth.currentAccessTokenOrNull())
                    _authState.update { it.copy(isLoading = false, error = null, isLoggedIn = true) }

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
                .onSuccess {
                    sessionManager.clearToken()
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            userId = null,
                            logoutCompleted = true // ✅ 로그아웃 완료
                        )
                    }
                }
                .onFailure { exception ->
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message,
                            logoutCompleted = false
                        )
                    }
                }
        }
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