package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.UserInfo
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserInfo(): Flow<UserInfo>
    suspend fun saveUserInfo(userInfo: UserInfo)
    suspend fun getUserDeviceId(): String
    suspend fun updateUserMetric(height: String, weight: String)
}