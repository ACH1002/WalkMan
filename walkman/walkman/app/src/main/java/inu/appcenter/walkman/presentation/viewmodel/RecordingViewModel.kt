package inu.appcenter.walkman.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.data.repository.PendingUploadManager
import inu.appcenter.walkman.data.repository.gaitanalysis.SupabaseGaitAnalysisRepository
import inu.appcenter.walkman.data.repository.user.UserProfileRepository
import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.NetworkStatus
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.SensorReading
import inu.appcenter.walkman.domain.repository.FeedbackRepository
import inu.appcenter.walkman.domain.repository.NetworkMonitor
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StorageRepository
import inu.appcenter.walkman.local.repository.GaitAnalysisRepository
import kotlinx.coroutines.delay
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
    private val storageRepository: StorageRepository,
    private val gaitAnalysisRepository: GaitAnalysisRepository,
    private val networkMonitor: NetworkMonitor,
    private val pendingUploadManager: PendingUploadManager,
    private val feedbackRepository: FeedbackRepository,
    private val profileRepository: UserProfileRepository,
    private val gaitAnalysisRepoSupabase: SupabaseGaitAnalysisRepository
) : ViewModel() {

    // 기존 UI 상태 클래스에 네트워크 관련 필드 추가
    data class RecordingUiState(
        val isRecording: Boolean = false,
        val isUploading: Boolean = false,
        val selectedMode: RecordingMode? = null,
        val uploadedFiles: List<File> = emptyList(),
        val successMessage: String? = null,
        val errorMessage: String? = null,
        val completedModes: Set<RecordingMode> = emptySet(),
        val allModesCompleted: Boolean = false,
        val networkError: String? = null,
        val pendingUpload: Boolean = false,
        val pendingUploadCount: Int = 0,
        val uploadCompleted: Boolean = false
    )
    // 프로필 관련 상태 추가
    private val _selectedProfile = MutableStateFlow<UserProfile?>(null)
    val selectedProfile: StateFlow<UserProfile?> = _selectedProfile.asStateFlow()


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

    // 네트워크 상태
    private val _networkState = MutableStateFlow<NetworkStatus>(NetworkStatus.Disconnected)
    val networkState: StateFlow<NetworkStatus> = _networkState.asStateFlow()

    // 현재 사이클 상태
    private val _cycleState = MutableStateFlow(CycleState())
    val cycleState: StateFlow<CycleState> = _cycleState.asStateFlow()

    // 업로드 상태
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    init {

        viewModelScope.launch {
            profileRepository.getSelectedProfileFlow().collect { profile ->
                _selectedProfile.value = profile
            }
        }
        // 녹화 상태 관찰
        viewModelScope.launch {
            sensorRepository.isRecording().collect { isRecording ->
                _uiState.update { it.copy(isRecording = isRecording) }
            }
        }

        // 센서 데이터 관찰
        viewModelScope.launch {
            sensorRepository.getCurrentReadings().collect { reading ->
                _currentReading.value = reading
            }
        }

        // 네트워크 상태 관찰
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                _networkState.value = status

                // 네트워크 상태가 변경될 때 UI 업데이트
                when (status) {
                    is NetworkStatus.Connected -> {
                        // 네트워크 연결 시 에러 메시지 제거
                        _uiState.update { it.copy(networkError = null) }

                        // 대기 중인 업로드 개수 업데이트
                        updatePendingUploadCount()
                    }
                    is NetworkStatus.Disconnected -> {
                        // 네트워크 연결 해제 시 메시지 표시
                        if (_uiState.value.isRecording) {
                            _uiState.update { it.copy(
                                networkError = "네트워크 연결이 끊겼습니다. 측정은 계속되지만 데이터는 나중에 업로드됩니다."
                            ) }
                        }
                    }
                }
            }
        }

        // PendingUploadManager의 업로드 완료 이벤트 관찰
        observePendingUploadCompletion()

        // 대기 중인 업로드 개수 업데이트
        updatePendingUploadCount()
    }

    /**
     * PendingUploadManager 인스턴스를 반환하는 메서드
     * RecordingScreen에서 업로드 진행 상태를 표시하기 위해 사용
     */
    fun getPendingUploadManager(): PendingUploadManager {
        return pendingUploadManager
    }

    // 기존의 observePendingUploadCompletion 메서드 수정 (collectLatest -> collect)
    private fun observePendingUploadCompletion() {
        viewModelScope.launch {
            pendingUploadManager.uploadCompletionFlow.collect { completed ->
                if (completed) {
                    // 업로드 완료 UI 상태 업데이트
                    _uiState.update { it.copy(
                        isUploading = false,
                        uploadCompleted = true,
                        pendingUpload = false,
                        successMessage = "대기 중이던 데이터가 성공적으로 업로드되었습니다."
                    ) }

                    _uploadState.value = UploadState.Success("대기 중이던 데이터가 성공적으로 업로드되었습니다.")

                    // PendingUpload 개수 업데이트
                    updatePendingUploadCount()

                    // 성공 메시지 자동 제거 타이머 (3초)
                    delay(3000)
                    clearMessages()
                    _uploadState.value = UploadState.Idle
                }
            }
        }
    }

    /**
     * 네트워크 연결 시 대기 중인 업로드 확인
     */
    private fun checkPendingUploads() {
        viewModelScope.launch {
            if (_uiState.value.pendingUpload && !_uiState.value.isUploading) {
                _uiState.update { it.copy(
                    isUploading = true,
                    errorMessage = null,
                    successMessage = null
                ) }

                _uploadState.value = UploadState.Uploading

                // 명시적으로 pendingUploadManager에 처리 요청 추가
                pendingUploadManager.processPendingUploads()
            }
        }
    }

    /**
     * 대기 중인 업로드 개수 업데이트
     */
    private fun updatePendingUploadCount() {
        val count = pendingUploadManager.getPendingUploadCount()
        _uiState.update { it.copy(
            pendingUploadCount = count,
            pendingUpload = count > 0
        ) }
    }

    /**
     * 사이클 상태 데이터 클래스
     */
    data class CycleState(
        val currentCycle: Int = 1,
        val totalCycles: Int = 3,
        val completedInCurrentCycle: Int = 0
    )

    /**
     * 업로드 상태를 나타내는 sealed class
     */
    sealed class UploadState {
        object Idle : UploadState()
        object Uploading : UploadState()
        data class Success(val message: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    /**
     * 타이머 완료 시 호출하는 함수
     */
    fun onTimerCompleted(mode: RecordingMode) {
        provideFeedback(mode)
        stopRecording()
    }

    /**
     * 측정 모드에 따른 피드백 제공
     */
    private fun provideFeedback(mode: RecordingMode) {
        when (mode) {
            RecordingMode.POCKET -> {
                // 주머니 모드는 진동 패턴을 강하게 설정
                feedbackRepository.vibrate(longArrayOf(0, 500, 300, 500, 300, 500))
            }
            else -> {
                // 다른 모드는 기본 진동 패턴 사용
                feedbackRepository.vibrate(longArrayOf(0, 300, 200, 300))
            }
        }
    }

    /**
     * 보행 분석 수행
     */
    private fun performGaitAnalysis(session: RecordingSession) {
        viewModelScope.launch {
            try {
                // 보행 분석 결과 계산
                val gaitScore = gaitAnalysisRepository.analyzeGaitData(session)

                // 분석 결과 저장
                if (gaitScore != null) {
                    _gaitAnalysisResult.value = gaitScore

                    // ProfileGaitViewModel을 통해 Supabase에도 분석 결과 저장
                    saveGaitAnalysisToSupabase(session)
                }
            } catch (e: Exception) {
                // 분석 실패 시 처리
                Log.e("RecordingViewModel", "보행 분석 실패", e)
                _gaitAnalysisResult.value = null
            }
        }
    }

    // 보행 분석 결과 저장 메서드 수정
    private fun saveGaitAnalysisToSupabase(session: RecordingSession) {
        viewModelScope.launch {
            val selectedProfile = getSelectedUserProfile() ?: return@launch

            try {
                gaitAnalysisRepoSupabase.analyzeAndSaveGaitData(
                    session = session,
                    userProfileId = selectedProfile.id ?: return@launch
                )
            } catch (e: Exception) {
                Log.e("RecordingViewModel", "Supabase 보행 분석 저장 실패", e)
            }
        }
    }

    /**
     * 선택된 사용자 프로필 가져오기
     */
    private fun getSelectedUserProfile(): UserProfile? {
        return _selectedProfile.value
    }

    /**
     * 기록 시작
     */
    fun startRecording(mode: RecordingMode) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedMode = mode,
                    errorMessage = null,
                    isUploading = false,
                    networkError = null
                )
            }

            _uploadState.value = UploadState.Idle

            sensorRepository.startRecording(mode)
        }
    }

    /**
     * 기록 중지 및 저장
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUploading = true) }
                _uploadState.value = UploadState.Uploading

                // 선택된 사용자 프로필 가져오기
                val selectedProfile = getSelectedUserProfile()

                if (selectedProfile == null) {
                    _uploadState.value = UploadState.Error("선택된 사용자 프로필이 없습니다.")
                    _uiState.update { it.copy(
                        isUploading = false,
                        errorMessage = "선택된 사용자 프로필이 없습니다. 프로필을 선택하고 다시 시도해주세요."
                    ) }
                    return@launch
                }

                // 센서 데이터 중지
                val session = sensorRepository.stopRecording()

                // 세션 저장 - Room 데이터베이스에 저장 추가
                _lastCompletedSession.value = session
                gaitAnalysisRepository.saveRecordingSession(session)

                // CSV 파일 생성 (수정된 부분: UserProfile 사용)
                val csvFile = storageRepository.createCsvFileWithProfile(session, selectedProfile)

                // 네트워크 상태 확인
                val isUploadAllowed = networkMonitor.isUploadAllowed().first()
                val isConnected = _networkState.value is NetworkStatus.Connected

                if (!isConnected || !isUploadAllowed) {
                    // 네트워크 연결이 없거나 업로드가 허용되지 않은 경우
                    // 로컬에만 저장하고 나중에 업로드하도록 표시
                    pendingUploadManager.addPendingUpload(session, csvFile, selectedProfile.id ?: "")

                    _uploadState.value = UploadState.Success("측정이 완료되었습니다. 네트워크 연결 시 자동으로 업로드됩니다.")
                    _uiState.update { it.copy(
                        isUploading = false,
                        pendingUpload = true,
                        successMessage = "측정이 완료되었습니다. 네트워크 연결 시 자동으로 업로드됩니다."
                    ) }

                    // 측정 완료 상태 업데이트 - 네트워크 상태와 관계없이 진행
                    updateCompletedMode(session.mode)
                    return@launch
                }

                // 네트워크 연결이 있는 경우 - 정상적으로 업로드 진행
                try {
                    // 파일 업로드 (Supabase 스토리지 또는 구글 드라이브)
                    val csvFileId = storageRepository.uploadFileToDrive(csvFile)

                    // 업로드 성공 상태로 전환 - 3초 동안 유지
                    _uploadState.value = UploadState.Success("데이터가 성공적으로 업로드되었습니다.")

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

                    // 업로드 성공 상태를 3초 동안 유지 후 Idle 상태로 변경
                    delay(3000)
                    _uploadState.value = UploadState.Idle

                } catch (e: Exception) {
                    Log.e("RecordingViewModel", "파일 업로드 실패", e)

                    // 업로드 실패 시 대기 업로드 목록에 추가
                    pendingUploadManager.addPendingUpload(session, csvFile, selectedProfile.id ?: "")

                    _uploadState.value = UploadState.Error("업로드 실패: ${e.message}. 네트워크 연결 시 자동으로 재시도합니다.")
                    _uiState.update { it.copy(
                        isUploading = false,
                        errorMessage = "업로드 실패: ${e.message}. 네트워크 연결 시 자동으로 재시도합니다.",
                        pendingUpload = true
                    ) }

                    // 업로드 실패해도 측정 완료 상태는 업데이트
                    updateCompletedMode(session.mode)
                }

            } catch (e: Exception) {
                Log.e("RecordingViewModel", "측정 중지 중 오류 발생", e)

                _uploadState.value = UploadState.Error("오류가 발생했습니다: ${e.message}")
                _uiState.update { it.copy(
                    isUploading = false,
                    errorMessage = "오류가 발생했습니다: ${e.message}"
                ) }
            }
        }
    }

    /**
     * 측정 완료 상태 업데이트
     */
    private fun updateCompletedMode(mode: RecordingMode) {
        _uiState.update { currentState ->
            val updatedCompletedModes = currentState.completedModes.toMutableSet()
            updatedCompletedModes.add(mode)

            // 사이클 진행 상태 계산
            val completedCount = updatedCompletedModes.size
            val currentCycle = (completedCount / 3) + 1  // 3개 모드가 1 사이클
            val totalCycles = 3

            _cycleState.update {
                it.copy(
                    currentCycle = minOf(currentCycle, totalCycles),
                    totalCycles = totalCycles,
                    completedInCurrentCycle = completedCount % 3
                )
            }

            val allCompleted = updatedCompletedModes.size >= 9  // 3개 모드 × 3 사이클

            currentState.copy(
                completedModes = updatedCompletedModes,
                allModesCompleted = updatedCompletedModes.size >= 9  // 총 9개 측정 완료 확인
            )
        }
    }

    /**
     * 모드별 완료 상태 확인
     */
    fun isModeCompleted(mode: RecordingMode): Boolean {
        // 모드별 완료 횟수 계산
        val modeCompletedCount = _uiState.value.completedModes.count { it == mode }

        // 현재 사이클에서 해당 모드가 이미 완료되었는지 확인
        val currentCycle = _cycleState.value.currentCycle
        val cycleCompleted = modeCompletedCount >= currentCycle

        return cycleCompleted
    }

    /**
     * 현재 사이클에서 모드별 완료 횟수 확인
     */
    fun getModeCompletedCount(mode: RecordingMode): Int {
        return _uiState.value.completedModes.count { it == mode }
    }

    /**
     * 측정 취소
     */
    fun cancelRecording() {
        viewModelScope.launch {
            try {
                // 데이터 저장 없이 센서 데이터 수집만 중지
                sensorRepository.cancelRecording()

                _uiState.update { it.copy(
                    isRecording = false,
                    isUploading = false
                ) }

                _uploadState.value = UploadState.Idle
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRecording = false,
                    isUploading = false,
                    errorMessage = "측정이 중단되었습니다."
                ) }
            }
        }
    }

    /**
     * 메시지 초기화
     */
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null, networkError = null) }
    }

    /**
     * 모든 모드 측정 완료 상태 리셋
     */
    fun resetCompletionStatus() {
        _uiState.update {
            it.copy(
                completedModes = emptySet(),
                allModesCompleted = false
            )
        }

        _cycleState.update {
            it.copy(
                currentCycle = 1,
                completedInCurrentCycle = 0
            )
        }

        // 보행 분석 결과도 초기화
        _gaitAnalysisResult.value = null
    }
}