package inu.appcenter.walkman.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 소셜 미디어 앱 사용 감지 및 경고를 위한 간소화된 인터페이스
 */
interface AppUsageRepository {
    /**
     * 앱 사용 추적 활성화 상태 조회
     */
    fun isTrackingEnabled(): Flow<Boolean>

    /**
     * 앱 사용 추적 활성화 상태 설정
     */
    suspend fun setTrackingEnabled(enabled: Boolean)

    /**
     * 앱 사용 데이터 조회 - 간소화 버전에서는 사용하지 않음
     */
    fun getAppUsageData(): Flow<List<Any>>

    /**
     * 앱 사용 시간 기록 - 간소화 버전에서는 사용하지 않음
     */
    suspend fun trackAppUsage(packageName: String, appName: String, durationMs: Long)

    /**
     * 앱 사용 데이터 초기화 - 간소화 버전에서는 사용하지 않음
     */
    suspend fun resetAppUsage()
}