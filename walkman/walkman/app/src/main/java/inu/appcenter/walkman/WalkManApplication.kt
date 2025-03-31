package inu.appcenter.walkman

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import inu.appcenter.walkman.data.repository.NotificationRepositoryImpl
import inu.appcenter.walkman.domain.repository.NotificationRepository
import inu.appcenter.walkman.util.LanguageManager


@HiltAndroidApp
class WalkManApplication : Application() {
    private lateinit var languageManager: LanguageManager

    // 전역에서 접근 가능한 NotificationRepository 추가
    lateinit var notificationRepository: NotificationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        languageManager = LanguageManager(this)

        // NotificationRepository 초기화
        notificationRepository = NotificationRepositoryImpl(this)
    }

    override fun attachBaseContext(base: Context) {
        // 앱이 시작될 때 저장된 언어 설정 적용
        languageManager = LanguageManager(base)
        super.attachBaseContext(languageManager.updateConfiguration(base))
    }
}