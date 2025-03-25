package inu.appcenter.walkman

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import inu.appcenter.walkman.util.LanguageManager


@HiltAndroidApp
class WalkManApplication : Application() {
    private lateinit var languageManager: LanguageManager

    override fun onCreate() {
        super.onCreate()
        languageManager = LanguageManager(this)
        // 필요한 초기화 코드는 여기에
    }

    override fun attachBaseContext(base: Context) {
        // 앱이 시작될 때 저장된 언어 설정 적용
        languageManager = LanguageManager(base)
        super.attachBaseContext(languageManager.updateConfiguration(base))
    }
}