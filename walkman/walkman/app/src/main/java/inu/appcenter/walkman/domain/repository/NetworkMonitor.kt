// domain/repository/NetworkMonitor.kt
package inu.appcenter.walkman.domain.repository

import kotlinx.coroutines.flow.Flow
import inu.appcenter.walkman.domain.model.NetworkStatus

/**
 * 네트워크 상태를 모니터링하는 인터페이스
 */
interface NetworkMonitor {
    /**
     * 네트워크 상태를 Flow로 반환
     */
    val networkStatus: Flow<NetworkStatus>

    /**
     * 인터넷 연결 가능 여부를 확인
     * @return 인터넷 연결 가능 여부
     */
    suspend fun isInternetReachable(): Boolean

    /**
     * 업로드가 가능한 네트워크 연결 상태인지 확인
     * (WiFi에 연결되어 있거나, 모바일 데이터 사용이 허용된 경우)
     * @return 업로드 가능 여부
     */
    fun isUploadAllowed(): Flow<Boolean>
}