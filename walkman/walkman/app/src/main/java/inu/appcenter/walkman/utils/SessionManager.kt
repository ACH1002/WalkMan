package inu.appcenter.walkman.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "accessToken"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_JUST_LOGGED_OUT = "just_logged_out"
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveToken(token: String?) {
        Log.d("saveToken", token.toString())
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).commit()
        // 토큰 저장 시 로그아웃 상태 해제
        setJustLoggedOut(false)
    }

    fun clearToken() {
        Log.d("clearToken", "토큰 삭제 시작")
        // 토큰만 삭제하고 다른 설정은 유지
        val result = prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .putBoolean(KEY_JUST_LOGGED_OUT, true)
            .commit()
        Log.d("clearToken", "토큰 삭제 완료: $result")
    }

    fun getToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    // 로그아웃 상태 관리 함수
    fun setJustLoggedOut(loggedOut: Boolean) {
        prefs.edit().putBoolean(KEY_JUST_LOGGED_OUT, loggedOut).commit()
    }

    fun wasJustLoggedOut(): Boolean {
        return prefs.getBoolean(KEY_JUST_LOGGED_OUT, false)
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            // 온보딩 상태는 유지 (사용자가 다시 온보딩을 볼 필요가 없도록)
        }.apply()
    }
}