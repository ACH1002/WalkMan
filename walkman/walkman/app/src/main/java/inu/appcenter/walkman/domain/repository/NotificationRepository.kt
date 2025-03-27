package inu.appcenter.walkman.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 알림 설정을 관리하는 Repository 인터페이스
 */
interface NotificationRepository {

    /**
     * 알림 활성화 상태 가져오기
     * @return 알림 활성화 상태 Flow
     */
    fun isNotificationEnabled(): Flow<Boolean>

    /**
     * 알림 활성화 상태 설정
     * @param enabled 활성화 여부
     */
    suspend fun setNotificationEnabled(enabled: Boolean)


}