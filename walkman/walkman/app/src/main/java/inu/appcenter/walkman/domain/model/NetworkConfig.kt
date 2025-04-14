// domain/model/NetworkConfig.kt
package inu.appcenter.walkman.domain.model

/**
 * 네트워크 설정을 나타내는 데이터 클래스
 */
data class NetworkConfig(
    /**
     * WiFi 연결 시에만 업로드 허용
     */
    val wifiOnlyUpload: Boolean = false,

    /**
     * 네트워크 상태 변경 시 자동 재시도 횟수
     */
    val maxRetryCount: Int = 3,

    /**
     * 네트워크 연결 시간 초과 (밀리초)
     */
    val connectionTimeoutMs: Long = 10000
)