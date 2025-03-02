package inu.appcenter.walkman.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.SensorReading
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StorageRepository
import inu.appcenter.walkman.domain.repository.UserRepository
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
    private val storageRepository: StorageRepository
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    // 현재 센서 데이터
    private val _currentReading = MutableStateFlow<SensorReading?>(null)
    val currentReading: StateFlow<SensorReading?> = _currentReading.asStateFlow()

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

                // 사용자 정보 가져오기
                val userInfo = userRepository.getUserInfo().first()

                // CSV 파일 생성
                val csvFile = storageRepository.createCsvFile(session, userInfo)

                // 구글 드라이브에 업로드
                val csvFileId = storageRepository.uploadFileToDrive(csvFile)

                // 측정 완료 상태 업데이트
                updateCompletedMode(session.mode)

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

    // 측정 완료 상태 업데이트
    private fun updateCompletedMode(mode: RecordingMode) {
        _uiState.update { currentState ->
            val updatedCompletedModes = currentState.completedModes.toMutableSet()
            updatedCompletedModes.add(mode)

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
    }
}

// UI 상태 클래스
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