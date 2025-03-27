package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.AppUsageData
import kotlinx.coroutines.flow.Flow

interface AppUsageRepository {
    fun getAppUsageData(): Flow<List<AppUsageData>>
    suspend fun trackAppUsage(packageName: String, appName: String, durationMs: Long)
    suspend fun resetAppUsage()
    fun isTrackingEnabled(): Flow<Boolean>
    suspend fun setTrackingEnabled(enabled: Boolean)
}