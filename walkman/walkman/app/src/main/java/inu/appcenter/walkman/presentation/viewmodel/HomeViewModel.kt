package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.StepCountData
import inu.appcenter.walkman.domain.repository.StepCountRepository
import inu.appcenter.walkman.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepCountRepository: StepCountRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 오늘 날짜 계산
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 오늘의 걸음 수 데이터 관찰
        viewModelScope.launch {
            stepCountRepository.observeStepsForDay(today).collect { stepData ->
                _uiState.update { currentState ->
                    currentState.copy(
                        todaySteps = stepData.steps,
                        todayDistance = stepData.distance,
                        todayCalories = stepData.calories
                    )
                }
            }
        }

        // 주간 걸음 수 데이터 로드
        loadWeeklyData()
    }

    /**
     * 주간 걸음 수 데이터 로드
     */
    fun loadWeeklyData() {
        viewModelScope.launch {
            try {
                // 오늘 날짜 계산
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // 1주일 전 날짜 계산
                val weekAgo = Calendar.getInstance().apply {
                    timeInMillis = today.timeInMillis
                    add(Calendar.DAY_OF_MONTH, -6) // 6일 전 (오늘 포함 7일)
                }

                // 최근 7일 데이터 가져오기
                val weeklyData = stepCountRepository.getStepsForRange(
                    weekAgo.timeInMillis,
                    today.timeInMillis
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        weeklyStepData = weeklyData,
                        isLoading = false
                    )
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
}

/**
 * 홈 화면 UI 상태 클래스
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val todaySteps: Int = 0,
    val todayDistance: Float = 0f,
    val todayCalories: Float = 0f,
    val weeklyStepData: List<StepCountData> = emptyList(),
    val error: String? = null
)