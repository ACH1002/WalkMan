// domain/model/NetworkStatus.kt
package inu.appcenter.walkman.domain.model

/**
 * 네트워크 상태를 나타내는 sealed class
 */
sealed class NetworkStatus {
    /**
     * 네트워크 연결 상태
     * @param type 연결 타입 (WiFi, 모바일 데이터 등)
     */
    data class Connected(val type: ConnectionType) : NetworkStatus()

    /**
     * 네트워크 연결 해제 상태
     */
    object Disconnected : NetworkStatus()
}

/**
 * 네트워크 연결 타입을 나타내는 enum
 */
enum class ConnectionType {
    WIFI,
    MOBILE,
    ETHERNET,
    OTHER
}