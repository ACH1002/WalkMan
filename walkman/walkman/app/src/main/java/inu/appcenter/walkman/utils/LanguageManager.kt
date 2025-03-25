package inu.appcenter.walkman.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import inu.appcenter.walkman.presentation.MainActivity
import java.util.Locale

class LanguageManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "app_language"

        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_KOREAN = "ko"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 현재 설정된 언어 코드 가져오기
    fun getLanguageCode(): String {
        return prefs.getString(KEY_LANGUAGE, getSystemLanguage()) ?: LANGUAGE_ENGLISH
    }

    // 시스템 언어 가져오기 (기본값으로 사용)
    private fun getSystemLanguage(): String {
        return if (Locale.getDefault().language == "ko") LANGUAGE_KOREAN else LANGUAGE_ENGLISH
    }

    // 언어 설정 저장
    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    // 현재 설정된 언어로 Configuration 업데이트
    fun updateConfiguration(context: Context): Context {
        val languageCode = getLanguageCode()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    // 앱 재시작 함수
    fun restartApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}