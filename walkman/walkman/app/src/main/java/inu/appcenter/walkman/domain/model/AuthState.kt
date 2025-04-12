package inu.appcenter.walkman.domain.model

/**
 * 인증 상태를 나타내는 Sealed Class
 * 앱의 인증 및 네비게이션 흐름을 제어하는데 사용됨
 */
sealed class AuthState {
    // 초기 로딩 상태
    object Loading : AuthState()

    // 인증되지 않은 상태 (로그인 필요)
    object Unauthenticated : AuthState()

    // 인증됨 (토큰 있음) 그러나 사용자 프로필이 없음 (온보딩 필요)
    object AuthenticatedWithoutProfile : AuthState()

    // 인증되고 사용자 프로필도 완료됨 (메인 화면으로)
    data class Authenticated(val userId: String) : AuthState()
}