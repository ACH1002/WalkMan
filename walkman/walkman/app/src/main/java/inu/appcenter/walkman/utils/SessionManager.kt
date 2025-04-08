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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = getUserId()

        Log.d("SessionManager", "isLoggedIn: $loggedIn, UserId: $userId")

        // 로그인 상태를 더 엄격하게 확인
        return loggedIn && !userId.isNullOrBlank()
    }

    fun saveLoginState(isLoggedIn: Boolean) {
        Log.d("SessionManager", "Saving login state: $isLoggedIn")
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            // 온보딩 상태는 유지 (사용자가 다시 온보딩을 볼 필요가 없도록)
        }.apply()
    }
}