package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 화면 뷰모델 - 간소화된 버전
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 앱 사용 데이터
    private val _appUsageData = MutableStateFlow<List<Any>>(emptyList())
    val appUsageData: StateFlow<List<Any>> = _appUsageData.asStateFlow()

    init {
        // 앱 추적 활성화 상태 확인
        checkTrackingEnabled()

        // 로딩 완료 상태로 설정
        _uiState.update { it.copy(isLoading = false) }
    }

    /**
     * 앱 추적 활성화 상태 확인
     */
    private fun checkTrackingEnabled() {
        viewModelScope.launch {
            try {
                appUsageRepository.isTrackingEnabled().collect { enabled ->
                    _uiState.update { it.copy(isTrackingEnabled = enabled) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * 앱 추적 활성화/비활성화
     */
    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                appUsageRepository.setTrackingEnabled(enabled)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}

/**
 * 홈 화면 UI 상태 클래스 - 간소화된 버전
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isTrackingEnabled: Boolean = true,
    val error: String? = null
)