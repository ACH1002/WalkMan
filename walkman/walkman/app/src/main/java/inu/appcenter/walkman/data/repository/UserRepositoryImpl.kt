package inu.appcenter.walkman.data.repository

import android.content.Context
import android.content.SharedPreferences
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    context: Context
) : UserRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _userInfo = MutableStateFlow(
        UserInfo(
            name = prefs.getString(KEY_NAME, "") ?: "",
            gender = prefs.getString(KEY_GENDER, "") ?: "",
            height = prefs.getString(KEY_HEIGHT, "") ?: "",
            weight = prefs.getString(KEY_WEIGHT, "") ?: "",
            mbti = prefs.getString(KEY_MBTI, "") ?: "", // MBTI 추가
            deviceId = getOrCreateDeviceId()
        )
    )

    override fun getUserInfo(): Flow<UserInfo> = _userInfo.asStateFlow()

    override suspend fun saveUserInfo(userInfo: UserInfo) {
        prefs.edit().apply {
            putString(KEY_NAME, userInfo.name)
            putString(KEY_GENDER, userInfo.gender)
            putString(KEY_HEIGHT, userInfo.height)
            putString(KEY_WEIGHT, userInfo.weight)
            putString(KEY_MBTI, userInfo.mbti) // MBTI 저장 추가
            apply()
        }

        _userInfo.value = userInfo
    }

    override suspend fun getUserDeviceId(): String = getOrCreateDeviceId()

    override suspend fun updateUserMetric(height: String, weight: String) {
        val updated = _userInfo.value.copy(
            height = height,
            weight = weight
        )
        saveUserInfo(updated)
    }

    private val _isDataCollectionCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_DATA_COLLECTION_COMPLETED, false)
    )
    override fun isDataCollectionCompleted(): Flow<Boolean> = _isDataCollectionCompleted.asStateFlow()


    override suspend fun setDataCollectionCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_COLLECTION_COMPLETED, completed).apply()
        _isDataCollectionCompleted.value = completed
    }

    private fun getOrCreateDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        return deviceId
    }

    companion object {
        private const val KEY_NAME = "user_name"
        private const val KEY_GENDER = "user_gender"
        private const val KEY_HEIGHT = "user_height"
        private const val KEY_WEIGHT = "user_weight"
        private const val KEY_MBTI = "user_mbti" // MBTI 키 추가
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DATA_COLLECTION_COMPLETED = "data_collection_completed"
    }
}