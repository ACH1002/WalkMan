package inu.appcenter.walkman.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.domain.repository.NotificationRepository
import inu.appcenter.walkman.service.WalkingDetectorService
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
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 앱 사용 데이터
    private val _appUsageData = MutableStateFlow<List<Any>>(emptyList())
    val appUsageData: StateFlow<List<Any>> = _appUsageData.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.isTrackingEnabled().collect { enabled ->
                _uiState.update { it.copy(isTrackingEnabled = enabled) }
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