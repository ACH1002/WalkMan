package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.SensorReading
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StorageRepository
import inu.appcenter.walkman.domain.repository.UserRepository
import inu.appcenter.walkman.local.repository.GaitAnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val gaitAnalysisRepository: GaitAnalysisRepository // 추가된 의존성
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    // 현재 센서 데이터
    private val _currentReading = MutableStateFlow<SensorReading?>(null)
    val currentReading: StateFlow<SensorReading?> = _currentReading.asStateFlow()

    // 가장 최근 완료된 세션
    private val _lastCompletedSession = MutableStateFlow<RecordingSession?>(null)
    val lastCompletedSession: StateFlow<RecordingSession?> = _lastCompletedSession.asStateFlow()

    // 보행 분석 결과
    private val _gaitAnalysisResult = MutableStateFlow<GaitScoreData?>(null)
    val gaitAnalysisResult: StateFlow<GaitScoreData?> = _gaitAnalysisResult.asStateFlow()

    init {
        viewModelScope.launch {
            sensorRepository.isRecording().collect { isRecording ->
                _uiState.update { it.copy(isRecording = isRecording) }
            }
        }

        viewModelScope.launch {
            sensorRepository.getCurrentReadings().collect { reading ->
                _currentReading.value = reading
            }
        }
    }

    // 기록 시작
    fun startRecording(mode: RecordingMode) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedMode = mode,
                    errorMessage = null,
                    isUploading = false
                )
            }

            sensorRepository.startRecording(mode)
        }
    }

    // 기록 중지 및 저장
    fun stopRecording() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUploading = true) }

                // 센서 데이터 중지
                val session = sensorRepository.stopRecording()

                // 세션 저장 - Room 데이터베이스에 저장 추가
                _lastCompletedSession.value = session
                gaitAnalysisRepository.saveRecordingSession(session)

                // 사용자 정보 가져오기
                val userInfo = userRepository.getUserInfo().first()

                // CSV 파일 생성
                val csvFile = storageRepository.createCsvFile(session, userInfo)

                // 구글 드라이브에 업로드
                val csvFileId = storageRepository.uploadFileToDrive(csvFile)

                // 측정 완료 상태 업데이트
                updateCompletedMode(session.mode)

                // VIDEO 모드인 경우 보행 분석 수행
                if (session.mode == RecordingMode.VIDEO) {
                    performGaitAnalysis(session)
                }

                _uiState.update { it.copy(
                    isUploading = false,
                    successMessage = "데이터가 성공적으로 업로드되었습니다.",
                    uploadedFiles = listOf(csvFile)
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isUploading = false,
                    errorMessage = "오류가 발생했습니다: ${e.message}"
                ) }
            }
        }
    }

    // 보행 분석 수행
    private fun performGaitAnalysis(session: RecordingSession) {
        viewModelScope.launch {
            try {
                // 보행 분석 결과 계산
                val gaitScore = gaitAnalysisRepository.analyzeGaitData(session)

                // 분석 결과 저장
                if (gaitScore != null) {
                    _gaitAnalysisResult.value = gaitScore
                }
            } catch (e: Exception) {
                // 분석 실패 시 처리
                _gaitAnalysisResult.value = null
            }
        }
    }

    // 측정 완료 상태 업데이트
    private fun updateCompletedMode(mode: RecordingMode) {
        _uiState.update { currentState ->
            val updatedCompletedModes = currentState.completedModes.toMutableSet()
            updatedCompletedModes.add(mode)

            val allCompleted = updatedCompletedModes.size >= 3 // 3가지 모드 모두 완료 확인

            // 모든 모드가 완료되었다면 데이터 수집 완료 상태 저장
            if (allCompleted) {
                viewModelScope.launch {
                    userRepository.setDataCollectionCompleted(true)
                }
            }

            currentState.copy(
                completedModes = updatedCompletedModes,
                allModesCompleted = updatedCompletedModes.size >= 3 // 3가지 모드 모두 완료 확인
            )
        }
    }

    // 메시지 초기화
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    // 특정 모드가 완료되었는지 확인
    fun isModeCompleted(mode: RecordingMode): Boolean {
        return uiState.value.completedModes.contains(mode)
    }

    // 모든 모드 측정 완료 상태 리셋
    fun resetCompletionStatus() {
        _uiState.update {
            it.copy(
                completedModes = emptySet(),
                allModesCompleted = false
            )
        }

        // 보행 분석 결과도 초기화
        _gaitAnalysisResult.value = null
    }

    fun cancelRecording() {
        viewModelScope.launch {
            try {
                // 데이터 저장 없이 센서 데이터 수집만 중지
                sensorRepository.cancelRecording()

                _uiState.update { it.copy(
                    isRecording = false,
                    isUploading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRecording = false,
                    isUploading = false,
                    errorMessage = "측정이 중단되었습니다."
                ) }
            }
        }
    }

    // 보행 분석 결과 초기화
    fun clearGaitAnalysisResult() {
        _gaitAnalysisResult.value = null
    }

    // 마지막 완료된 세션 가져오기
    fun getLastCompletedVideoSession(): RecordingSession? {
        return if (_lastCompletedSession.value?.mode == RecordingMode.VIDEO) {
            _lastCompletedSession.value
        } else {
            // 데이터베이스에서 가장 최근 VIDEO 세션 로드
            viewModelScope.launch {
                val latestVideoSession = gaitAnalysisRepository.getLatestSessionByMode(RecordingMode.VIDEO)
                if (latestVideoSession != null) {
                    _lastCompletedSession.value = latestVideoSession
                }
            }
            null
        }
    }

    // 가장 최근 VIDEO 세션의 보행 분석 결과 로드
    fun loadLatestGaitAnalysisResult() {
        viewModelScope.launch {
            try {
                val latestGaitAnalysis = gaitAnalysisRepository.getLatestGaitAnalysis()
                if (latestGaitAnalysis != null) {
                    _gaitAnalysisResult.value = latestGaitAnalysis
                } else {
                    // 저장된 분석 결과가 없으면 최신 VIDEO 세션 가져와 분석
                    val latestVideoSession = gaitAnalysisRepository.getLatestSessionByMode(RecordingMode.VIDEO)
                    if (latestVideoSession != null) {
                        performGaitAnalysis(latestVideoSession)
                    }
                }
            } catch (e: Exception) {
                // 로드 실패 시 기존 값 유지
            }
        }
    }
}

// UI 상태 클래스는 기존과 동일하게 유지
data class RecordingUiState(
    val isRecording: Boolean = false,
    val isUploading: Boolean = false,
    val selectedMode: RecordingMode? = null,
    val uploadedFiles: List<File> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val completedModes: Set<RecordingMode> = emptySet(),
    val allModesCompleted: Boolean = false
)