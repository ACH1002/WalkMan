// domain/model/NetworkError.kt
package inu.appcenter.walkman.domain.model

/**
 * 네트워크 오류를 나타내는 sealed class
 */
sealed class NetworkError {
    /**
     * 인터넷 연결 없음
     */
    object NoConnection : NetworkError()

    /**
     * 연결 시간 초과
     */
    object Timeout : NetworkError()

    /**
     * 서버 오류
     * @param code HTTP 상태 코드
     * @param message 오류 메시지
     */
    data class ServerError(val code: Int, val message: String) : NetworkError()

    /**
     * 기타 예외
     * @param throwable 발생한 예외
     */
    data class Other(val throwable: Throwable) : NetworkError()
}