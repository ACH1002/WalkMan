package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 사용자 정보 로드
            userRepository.getUserInfo().collect { userInfo ->
                _uiState.update {
                    it.copy(
                        isLoading = false, // 로딩 완료 표시
                        isUserProfileComplete = userInfo.name.isNotBlank() &&
                                userInfo.gender.isNotBlank(),
                        shouldShowOnboarding = !it.onboardingShown && userInfo.name.isBlank()
                    )
                }
            }
        }
    }

    // 온보딩 완료 표시
    fun markOnboardingShown() {
        _uiState.update { it.copy(onboardingShown = true) }
    }
}

// UI 상태 클래스
data class MainUiState(
    val isLoading: Boolean = true,
    val isUserProfileComplete: Boolean = false,
    val shouldShowOnboarding: Boolean = true,
    val onboardingShown: Boolean = false
)