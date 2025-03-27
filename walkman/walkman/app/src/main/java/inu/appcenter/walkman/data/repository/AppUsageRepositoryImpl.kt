package inu.appcenter.walkman.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import inu.appcenter.walkman.domain.model.AppUsageData
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.service.StepCounterService.Companion.TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    context: Context
) : AppUsageRepository {

    companion object {
        private const val PREFS_NAME = "app_usage_prefs"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_APP_USAGE_PREFIX = "app_usage_"
        private const val KEY_APP_NAME_PREFIX = "app_name_"
        private const val KEY_LAST_USED_PREFIX = "last_used_"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 추적할 앱 패키지 목록
    private val trackedApps = mapOf(
        "com.google.android.youtube" to "YouTube",
        "com.instagram.android" to "Instagram",
        "com.twitter.android" to "X",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.facebook.android" to "Facebook",
        "com.instagram.barcelona" to "Threads"
    )

    private val _appUsageData = MutableStateFlow<List<AppUsageData>>(emptyList())
    private val _isTrackingEnabled = MutableStateFlow(prefs.getBoolean(KEY_TRACKING_ENABLED, true))

    init {
        loadSavedData()
    }

    private fun loadSavedData() {
        val appUsageList = mutableListOf<AppUsageData>()

        trackedApps.forEach { (packageName, defaultName) ->
            val duration = prefs.getLong("${KEY_APP_USAGE_PREFIX}$packageName", 0)
            if (duration > 0) {
                val appName = prefs.getString("${KEY_APP_NAME_PREFIX}$packageName", defaultName) ?: defaultName
                val lastUsed = prefs.getLong("${KEY_LAST_USED_PREFIX}$packageName", 0)

                appUsageList.add(
                    AppUsageData(
                        packageName = packageName,
                        appName = appName,
                        usageDurationMs = duration,
                        lastUsedTimestamp = lastUsed
                    )
                )
            }
        }

        _appUsageData.value = appUsageList
    }

    override fun getAppUsageData(): Flow<List<AppUsageData>> = _appUsageData.asStateFlow()

    override suspend fun trackAppUsage(packageName: String, appName: String, durationMs: Long) {
        // 추적 대상 앱인 경우에만 기록
        if (!trackedApps.containsKey(packageName)) {
            Log.d(TAG, "추적 대상이 아닌 앱: $packageName, 무시됨")
            return
        }

        // 너무 짧은 사용 시간 필터링
        if (durationMs < 500) {
            Log.d(TAG, "너무 짧은 사용 시간: ${durationMs}ms, 무시됨")
            return
        }

        try {
            val currentData = _appUsageData.value.toMutableList()
            val existingDataIndex = currentData.indexOfFirst { it.packageName == packageName }

            if (existingDataIndex >= 0) {
                // 기존 데이터 업데이트
                val existingData = currentData[existingDataIndex]
                val newDuration = existingData.usageDurationMs + durationMs

                currentData[existingDataIndex] = existingData.copy(
                    usageDurationMs = newDuration,
                    lastUsedTimestamp = System.currentTimeMillis()
                )

                Log.d(TAG, "앱 사용 데이터 업데이트: $appName, 총 ${newDuration}ms")
            } else {
                // 새 데이터 추가
                currentData.add(
                    AppUsageData(
                        packageName = packageName,
                        appName = appName,
                        usageDurationMs = durationMs,
                        lastUsedTimestamp = System.currentTimeMillis()
                    )
                )

                Log.d(TAG, "새 앱 사용 데이터 추가: $appName, ${durationMs}ms")
            }

            _appUsageData.value = currentData

            // SharedPreferences에 저장
            val editor = prefs.edit()
            val updated = currentData.find { it.packageName == packageName }

            if (updated != null) {
                editor.putLong("${KEY_APP_USAGE_PREFIX}${updated.packageName}", updated.usageDurationMs)
                editor.putString("${KEY_APP_NAME_PREFIX}${updated.packageName}", updated.appName)
                editor.putLong("${KEY_LAST_USED_PREFIX}${updated.packageName}", updated.lastUsedTimestamp)
                editor.apply()

                Log.d(TAG, "앱 사용 데이터 저장 완료: $appName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "앱 사용 데이터 저장 중 오류: ${e.message}", e)
        }
    }

    override suspend fun resetAppUsage() {
        val editor = prefs.edit()

        trackedApps.keys.forEach { packageName ->
            editor.putLong("${KEY_APP_USAGE_PREFIX}$packageName", 0)
        }

        editor.apply()
        _appUsageData.value = emptyList()
    }

    override fun isTrackingEnabled(): Flow<Boolean> = _isTrackingEnabled.asStateFlow()

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply()
        _isTrackingEnabled.value = enabled
    }
}