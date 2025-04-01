package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.DailyGaitData
import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.local.repository.GaitAnalysisRepository
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
    private val sensorRepository: SensorRepository,
    private val gaitAnalysisRepository: GaitAnalysisRepository
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

                // 가장 최근 보행 분석 결과 가져오기
                val latestGaitAnalysis = gaitAnalysisRepository.getLatestGaitAnalysis()

                if (latestGaitAnalysis != null) {
                    // 최근 7일간의 보행 분석 결과 가져오기
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    val weeklyGaitAnalysis = gaitAnalysisRepository.getGaitAnalysisFromDate(calendar.time)

                    // 날짜별로 그룹화하여 일별 평균 점수 계산
                    val dailyGaitData = calculateDailyGaitData(weeklyGaitAnalysis)

                    // UI 상태 업데이트
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stabilityScore = latestGaitAnalysis.stabilityScore,
                            rhythmScore = latestGaitAnalysis.rhythmScore,
                            overallScore = latestGaitAnalysis.overallScore,
                            weeklyData = dailyGaitData,
                            lastUpdated = latestGaitAnalysis.analysisDate,
                            stabilityDetails = latestGaitAnalysis.stabilityDetails,
                            rhythmDetails = latestGaitAnalysis.rhythmDetails,
                            error = null
                        )
                    }
                } else {
                    // 저장된 분석 결과가 없으면 최신 VIDEO 세션을 가져와 분석 시도
                    val latestVideoSession = gaitAnalysisRepository.getLatestSessionByMode(RecordingMode.VIDEO)

                    if (latestVideoSession != null) {
                        val gaitScore = gaitAnalysisRepository.analyzeGaitData(latestVideoSession)

                        if (gaitScore != null) {
                            // 분석에 성공하면 UI 업데이트
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    stabilityScore = gaitScore.stabilityScore,
                                    rhythmScore = gaitScore.rhythmScore,
                                    overallScore = gaitScore.overallScore,
                                    weeklyData = getWeeklyGaitData(gaitScore),
                                    lastUpdated = gaitScore.analysisDate,
                                    stabilityDetails = gaitScore.stabilityDetails,
                                    rhythmDetails = gaitScore.rhythmDetails,
                                    error = null
                                )
                            }
                        } else {
                            // 모의 데이터 사용
                            fallbackToMockData()
                        }
                    } else {
                        // 모의 데이터 사용
                        fallbackToMockData()
                    }
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
     * 모의 데이터로 폴백
     */
    private fun fallbackToMockData() {
        val mockStabilityScore = 78
        val mockRhythmScore = 85
        val mockWeeklyData = getMockWeeklyData()

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
    }

    /**
     * 보행 분석 결과 리스트에서 일별 데이터 계산
     */
    private fun calculateDailyGaitData(gaitScores: List<GaitScoreData>): List<DailyGaitData> {
        // 날짜별로 그룹화
        val groupedByDate = gaitScores.groupBy {
            val cal = Calendar.getInstance()
            cal.time = it.analysisDate
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.time
        }

        // 각 날짜별 평균 점수 계산
        return groupedByDate.map { (date, scores) ->
            val avgStabilityScore = scores.map { it.stabilityScore }.average().toInt()
            val avgRhythmScore = scores.map { it.rhythmScore }.average().toInt()
            val avgOverallScore = scores.map { it.overallScore }.average().toInt()

            DailyGaitData(
                date = date,
                stabilityScore = avgStabilityScore,
                rhythmScore = avgRhythmScore,
                overallScore = avgOverallScore
            )
        }.sortedBy { it.date }
    }

    /**
     * 단일 보행 분석 결과로 주간 데이터 생성
     */
    private fun getWeeklyGaitData(gaitScore: GaitScoreData): List<DailyGaitData> {
        // 하나의 분석 결과만 있으면 해당 날짜에만 데이터 표시
        val today = Calendar.getInstance()
        today.time = gaitScore.analysisDate
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return listOf(
            DailyGaitData(
                date = today.time,
                stabilityScore = gaitScore.stabilityScore,
                rhythmScore = gaitScore.rhythmScore,
                overallScore = gaitScore.overallScore
            )
        )
    }

    /**
     * 테스트용 모의 주간 데이터 생성
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
     * 데이터 새로고침 - 사용자가 새로고침 버튼을 클릭했을 때 호출
     */
    fun refreshData() {
        loadGaitAnalysisData()
    }

    /**
     * VIDEO 모드로 측정한 가장 최근 세션으로 보행 분석 실행
     */
    fun analyzeLatestVideoSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // 가장 최근 VIDEO 모드 세션 가져오기
                val session = gaitAnalysisRepository.getLatestSessionByMode(RecordingMode.VIDEO)

                if (session != null) {
                    val gaitScore = gaitAnalysisRepository.analyzeGaitData(session)
                    if (gaitScore != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                stabilityScore = gaitScore.stabilityScore,
                                rhythmScore = gaitScore.rhythmScore,
                                overallScore = gaitScore.overallScore,
                                weeklyData = getWeeklyGaitData(gaitScore),
                                lastUpdated = gaitScore.analysisDate,
                                stabilityDetails = gaitScore.stabilityDetails,
                                rhythmDetails = gaitScore.rhythmDetails,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "보행 데이터 분석에 실패했습니다."
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "분석할 VIDEO 모드 측정 데이터가 없습니다."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "분석 중 오류가 발생했습니다: ${e.message}"
                    )
                }
            }
        }
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
    val stabilityDetails: Map<String, Double> = emptyMap(),
    val rhythmDetails: Map<String, Double> = emptyMap(),
    val error: String? = null
)