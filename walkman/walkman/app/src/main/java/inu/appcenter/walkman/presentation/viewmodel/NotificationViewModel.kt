package inu.appcenter.walkman.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import inu.appcenter.walkman.domain.repository.NotificationRepository
import inu.appcenter.walkman.service.WalkingDetectorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 설정 화면을 위한 UI 상태
 */
data class NotificationUiState(
    val isNotificationEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 저장된 알림 설정 가져오기
            notificationRepository.isNotificationEnabled().collect { isEnabled ->
                _uiState.update { it.copy(isNotificationEnabled = isEnabled) }
            }
        }
    }

    /**
     * 알림 활성화
     */
    fun enableNotifications(context: Context) {
        viewModelScope.launch {
            try {
                // 저장소에 설정 저장
                notificationRepository.setNotificationEnabled(true)

                // 걷기 감지 서비스 시작
                startWalkingDetectorService(context)

                _uiState.update { it.copy(isNotificationEnabled = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * 알림 비활성화
     */
    fun disableNotifications(context: Context) {
        viewModelScope.launch {
            try {
                // 저장소에 설정 저장
                notificationRepository.setNotificationEnabled(false)

                // 걷기 감지 서비스 중지
                stopWalkingDetectorService(context)

                _uiState.update { it.copy(isNotificationEnabled = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * 배터리 최적화 설정 화면 열기
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val packageName = context.packageName
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    // 이미 배터리 최적화가 제외된 경우
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                } else {
                    // 배터리 최적화 제외 요청
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                }
            } else {
                // Android M 미만에서는 앱 정보 화면으로 이동
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    /**
     * 걷기 감지 서비스 시작
     */
    private fun startWalkingDetectorService(context: Context) {
        val serviceIntent = Intent(context, WalkingDetectorService::class.java).apply {
            action = WalkingDetectorService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /**
     * 걷기 감지 서비스 중지
     */
    private fun stopWalkingDetectorService(context: Context) {
        val stopIntent = Intent(context, WalkingDetectorService::class.java).apply {
            action = WalkingDetectorService.ACTION_STOP
        }

        context.startService(stopIntent)
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationRepository.setNotificationEnabled(enabled)

                // 서비스 제어
                val intent = Intent(context, WalkingDetectorService::class.java).apply {
                    action = if (enabled)
                        WalkingDetectorService.ACTION_START
                    else
                        WalkingDetectorService.ACTION_STOP
                }

                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
}