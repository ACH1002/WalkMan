package inu.appcenter.walkman.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import inu.appcenter.walkman.data.model.GaitAnalysis
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.data.repository.gaitanalysis.SupabaseGaitAnalysisRepository
import inu.appcenter.walkman.data.repository.user.UserProfileRepository
import inu.appcenter.walkman.domain.model.RecordingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileGaitViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val gaitAnalysisRepository: SupabaseGaitAnalysisRepository
) : ViewModel() {

    // UI state
    private val _uiState = MutableStateFlow(ProfileGaitUiState())
    val uiState: StateFlow<ProfileGaitUiState> = _uiState.asStateFlow()

    init {
        // Load user profiles
        loadUserProfiles()

        // Observe selected profile changes
        viewModelScope.launch {
            userProfileRepository.getSelectedProfileFlow().collectLatest { profile ->
                _uiState.update { state ->
                    state.copy(selectedProfile = profile)
                }

                // Load gait analysis data for selected profile
                profile?.let {
                    loadGaitAnalysisForProfile(it.id!!)
                }
            }
        }
    }

    // Load all user profiles
    fun loadUserProfiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val profiles = userProfileRepository.getUserProfiles()
                _uiState.update {
                    it.copy(
                        userProfiles = profiles,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load profiles: ${e.message}"
                    )
                }
            }
        }
    }

    // Create new user profile
    fun createUserProfile(name: String, gender: String, height: String, weight: String, mbti: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val newProfile = UserProfile(
                    name = name,
                    gender = gender,
                    height = height,
                    weight = weight,
                    mbti = mbti
                )

                val createdProfile = userProfileRepository.createUserProfile(newProfile)

                if (createdProfile != null) {
                    // Reload the list
                    loadUserProfiles()
                    // Set as selected
                    selectUserProfile(createdProfile)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to create user profile"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error creating profile: ${e.message}"
                    )
                }
            }
        }
    }

    // Update user profile
    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val success = userProfileRepository.updateUserProfile(profile)

                if (success) {
                    // Reload the list
                    loadUserProfiles()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to update user profile"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error updating profile: ${e.message}"
                    )
                }
            }
        }
    }

    // Delete user profile
    fun deleteUserProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val success = userProfileRepository.deleteUserProfile(profileId)

                if (success) {
                    // Reload the list
                    loadUserProfiles()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete user profile"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error deleting profile: ${e.message}"
                    )
                }
            }
        }
    }

    // Select user profile
    fun selectUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            userProfileRepository.setSelectedProfile(profile)

            // Load gait analysis for this profile
            if (profile.id != null) {
                loadGaitAnalysisForProfile(profile.id)
            }
        }
    }

    // Load gait analysis for a profile
    private fun loadGaitAnalysisForProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGaitData = true, error = null) }

            try {
                val latestAnalysis = gaitAnalysisRepository.getLatestGaitAnalysisForProfile(profileId)

                _uiState.update {
                    it.copy(
                        latestGaitAnalysis = latestAnalysis,
                        isLoadingGaitData = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingGaitData = false,
                        error = "Failed to load gait analysis: ${e.message}"
                    )
                }
            }
        }
    }

    // Analyze and save gait data from a recording session
    fun analyzeAndSaveGaitData(session: RecordingSession) {
        val selectedProfile = _uiState.value.selectedProfile ?: return

        if (selectedProfile.id == null) {
            _uiState.update {
                it.copy(error = "No valid profile selected for analysis")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGaitData = true, error = null) }

            try {
                val analysis = gaitAnalysisRepository.analyzeAndSaveGaitData(
                    session = session,
                    userProfileId = selectedProfile.id
                )

                if (analysis != null) {
                    _uiState.update {
                        it.copy(
                            latestGaitAnalysis = analysis,
                            isLoadingGaitData = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingGaitData = false,
                            error = "Failed to analyze gait data"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingGaitData = false,
                        error = "Error analyzing gait data: ${e.message}"
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

    // Clear error
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// UI state data class
data class ProfileGaitUiState(
    val name: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val mbti: String = "",
    val isComplete: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingGaitData: Boolean = false,
    val userProfiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val latestGaitAnalysis: GaitAnalysis? = null,
    val error: String? = null
)