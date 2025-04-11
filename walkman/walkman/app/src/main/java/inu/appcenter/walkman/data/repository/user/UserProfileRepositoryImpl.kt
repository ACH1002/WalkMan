package inu.appcenter.walkman.data.repository.user

import android.util.Log
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.data.model.UserProfileEntity
import inu.appcenter.walkman.data.model.toDomain
import inu.appcenter.walkman.data.model.toEntity
import inu.appcenter.walkman.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : UserProfileRepository {

    private val TAG = "UserProfileRepository"

    private val _selectedProfile = MutableStateFlow<UserProfile?>(null)

    override fun getSelectedProfileFlow(): Flow<UserProfile?> = _selectedProfile.asStateFlow()

    override suspend fun setSelectedProfile(profile: UserProfile?) {
        _selectedProfile.value = profile
    }

    override suspend fun getUserProfiles(): List<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.supabase.postgrest["user_profiles"]
                .select()
                .decodeList<UserProfileEntity>()

            Log.d(TAG, "Fetched ${result.size} user profiles")

            val profiles = result.map { it.toDomain() }

            // If there's no selected profile yet but we have profiles, select the first one
            if (_selectedProfile.value == null && profiles.isNotEmpty()) {
                _selectedProfile.value = profiles.first()
            }

            profiles
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profiles", e)
            emptyList()
        }
    }

    override suspend fun getUserProfileById(profileId: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.supabase
                .from("user_profiles")
                .select() {
                    filter {
                        eq("id", profileId)
                    }
                }
                .decodeSingle<UserProfileEntity>()

            result.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile by ID", e)
            null
        }
    }

    override suspend fun createUserProfile(profile: UserProfile): UserProfile? = withContext(Dispatchers.IO) {
        try {
            // Get current user ID if not provided
            val profileToCreate = if (profile.accountId == null) {
                val userId = supabaseClient.getCurrentUserId()
                profile.copy(accountId = userId)
            } else {
                profile
            }

            val entity = profileToCreate.toEntity()

            val result = supabaseClient.supabase.postgrest["user_profiles"]
                .insert(entity)
                .decodeSingle<UserProfileEntity>()

            val createdProfile = result.toDomain()

            // If this is the first profile, set it as selected
            if (_selectedProfile.value == null) {
                _selectedProfile.value = createdProfile
            }

            createdProfile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile", e)
            null
        }
    }

    // 업데이트 메서드
    override suspend fun updateUserProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            require(profile.id != null) { "Profile ID must not be null for update" }

            val entity = profile.toEntity()

            supabaseClient.supabase
                .from("user_profiles")
                .update(entity) {
                    filter {
                        eq("id", profile.id)
                    }
                }

            _selectedProfile.update {
                if (it?.id == profile.id) profile else it
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile", e)
            false
        }
    }

    // 삭제 메서드
    override suspend fun deleteUserProfile(profileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.supabase
                .from("user_profiles")
                .delete {
                    filter {
                        eq("id", profileId)
                    }
                }

            // Clear selected profile if it's the one being deleted
            _selectedProfile.update {
                if (it?.id == profileId) null else it
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user profile", e)
            false
        }
    }
}