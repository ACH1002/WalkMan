package inu.appcenter.walkman.data.repository.user

import inu.appcenter.walkman.data.model.UserProfile
import kotlinx.coroutines.flow.Flow


interface UserProfileRepository {
    // Get all profiles for the current account
    suspend fun getUserProfiles(): List<UserProfile>

    // Get profile by ID
    suspend fun getUserProfileById(profileId: String): UserProfile?

    // Create a new profile
    suspend fun createUserProfile(profile: UserProfile): UserProfile?

    // Update existing profile
    suspend fun updateUserProfile(profile: UserProfile): Boolean

    // Delete profile
    suspend fun deleteUserProfile(profileId: String): Boolean

    // Get currently selected profile (for UI)
    fun getSelectedProfileFlow(): Flow<UserProfile?>

    // Set selected profile
    suspend fun setSelectedProfile(profile: UserProfile?)
}