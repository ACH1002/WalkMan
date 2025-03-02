package inu.appcenter.walkman.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // UI 상태를 위한 StateFlow
    private val _uiState = MutableStateFlow(UserInfoUiState())
    val uiState: StateFlow<UserInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUserInfo().collect { userInfo ->
                _uiState.update {
                    it.copy(
                        name = userInfo.name,
                        gender = userInfo.gender,
                        height = userInfo.height,
                        weight = userInfo.weight,
                        mbti = userInfo.mbti,
                        isComplete = userInfo.name.isNotBlank() &&
                                userInfo.gender.isNotBlank() &&
                                userInfo.height.isNotBlank() &&
                                userInfo.weight.isNotBlank()
                    )
                }
            }
        }
    }

    // 사용자 이름 변경
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
        checkCompletion()
    }

    // 성별 변경
    fun updateGender(gender: String) {
        _uiState.update { it.copy(gender = gender) }
        checkCompletion()
    }

    // 키 변경
    fun updateHeight(height: String) {
        _uiState.update { it.copy(height = height) }
        checkCompletion()
    }

    // 몸무게 변경
    fun updateWeight(weight: String) {
        _uiState.update { it.copy(weight = weight) }
        checkCompletion()
    }

    // MBTI 변경
    fun updateMbti(mbti: String) {
        _uiState.update { it.copy(mbti = mbti) }
    }

    // 완성도 체크
    private fun checkCompletion() {
        _uiState.update {
            it.copy(
                isComplete = it.name.isNotBlank() &&
                        it.gender.isNotBlank() &&
                        it.height.isNotBlank() &&
                        it.weight.isNotBlank()
            )
        }
    }

    // 사용자 정보 저장
    fun saveUserInfo() {
        viewModelScope.launch {
            val userInfo = UserInfo(
                name = uiState.value.name,
                gender = uiState.value.gender,
                height = uiState.value.height,
                weight = uiState.value.weight,
                mbti = uiState.value.mbti,
                deviceId = userRepository.getUserDeviceId()
            )
            userRepository.saveUserInfo(userInfo)
        }
    }
}

// UI 상태 클래스
data class UserInfoUiState(
    val name: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val mbti: String = "",
    val isComplete: Boolean = false
)