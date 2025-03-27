package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.AppUsageData
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 앱 사용 데이터
    private val _appUsageData = MutableStateFlow<List<AppUsageData>>(emptyList())
    val appUsageData: StateFlow<List<AppUsageData>> = _appUsageData.asStateFlow()

    init {
        // 앱 사용 데이터 로드
        loadAppUsageData()
    }

    private fun loadAppUsageData() {
        viewModelScope.launch {
            try {
                appUsageRepository.getAppUsageData().collect { usageData ->
                    _appUsageData.value = usageData

                    // 총 사용 시간 계산
                    val totalDuration = usageData.sumOf { it.usageDurationMs }
                    _uiState.update { currentState ->
                        currentState.copy(
                            totalSocialMediaUsage = totalDuration,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    // 앱 사용 데이터 리셋
    fun resetAppUsage() {
        viewModelScope.launch {
            appUsageRepository.resetAppUsage()
        }
    }
}

/**
 * 홈 화면 UI 상태 클래스
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val totalSocialMediaUsage: Long = 0L, // 총 소셜 미디어 사용 시간(ms)
    val error: String? = null
)