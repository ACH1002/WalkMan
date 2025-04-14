// domain/repository/FeedbackRepository.kt
package inu.appcenter.walkman.domain.repository

/**
 * 사용자에게 피드백을 제공하는 Repository 인터페이스
 */
interface FeedbackRepository {
    /**
     * 진동 패턴으로 피드백 제공
     * @param pattern 진동 패턴 (밀리초 단위)
     */
    fun vibrate(pattern: LongArray)


}