package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * 보행 분석 화면의 UI 상태를 관리하는 ViewModel
 */
@HiltViewModel
class GaitAnalysisViewModel @Inject constructor(
    // 필요한 repository 주입
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(GaitAnalysisUiState())
    val uiState: StateFlow<GaitAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadGaitAnalysisData()
    }

    /**
     * 보행 분석 데이터 로드
     */
    private fun loadGaitAnalysisData() {
        viewModelScope.launch {
            try {
                // 로딩 상태로 업데이트
                _uiState.update { it.copy(isLoading = true) }

                // 데이터 로드 로직 구현
                // 실제로는 Repository에서 데이터를 가져올 예정
                // 모의 데이터로 대체
                val mockStabilityScore = 78
                val mockRhythmScore = 85
                val mockWeeklyData = getMockWeeklyData()

                // UI 상태 업데이트
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stabilityScore = mockStabilityScore,
                        rhythmScore = mockRhythmScore,
                        overallScore = (mockStabilityScore + mockRhythmScore) / 2,
                        weeklyData = mockWeeklyData,
                        lastUpdated = Calendar.getInstance().time,
                        error = null
                    )
                }
            } catch (e: Exception) {
                // 오류 처리
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "데이터 로드 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 테스트용 주간 데이터 생성
     */
    private fun getMockWeeklyData(): List<DailyGaitData> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6) // 일주일 전부터 시작

        return List(7) { index ->
            val date = calendar.time
            val stability = (60..95).random()
            val rhythm = (65..90).random()

            calendar.add(Calendar.DAY_OF_YEAR, 1) // 다음 날로 이동

            DailyGaitData(
                date = Date(date.time),
                stabilityScore = stability,
                rhythmScore = rhythm,
                overallScore = (stability + rhythm) / 2
            )
        }
    }

    /**
     * 데이터 새로고침
     */
    fun refreshData() {
        loadGaitAnalysisData()
    }
}

/**
 * 보행 분석 화면의 UI 상태
 */
data class GaitAnalysisUiState(
    val isLoading: Boolean = false,
    val stabilityScore: Int = 0,
    val rhythmScore: Int = 0,
    val overallScore: Int = 0,
    val weeklyData: List<DailyGaitData> = emptyList(),
    val lastUpdated: Date? = null,
    val error: String? = null
)

/**
 * 일별 보행 데이터
 */
data class DailyGaitData(
    val date: Date,
    val stabilityScore: Int,
    val rhythmScore: Int,
    val overallScore: Int
)