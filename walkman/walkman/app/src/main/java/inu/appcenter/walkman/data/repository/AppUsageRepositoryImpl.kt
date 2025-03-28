package inu.appcenter.walkman.data.repository

import android.content.Context
import android.content.SharedPreferences
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 사용 데이터 관련 저장소 - 간소화된 버전
 * 걷기 중 소셜 미디어 경고 기능을 위한 최소한의 기능만 유지
 */
@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    context: Context
) : AppUsageRepository {

    companion object {
        private const val PREFS_NAME = "app_usage_prefs"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 추적할 앱 패키지 목록 - 소셜 미디어 앱 목록만 유지
    val socialMediaApps = mapOf(
        "com.google.android.youtube" to "YouTube",
        "com.instagram.android" to "Instagram",
        "com.twitter.android" to "X",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.facebook.android" to "Facebook",
        "com.instagram.barcelona" to "Threads",
        "com.snapchat.android" to "Snapchat",
        "com.linkedin.android" to "LinkedIn",
        "com.pinterest" to "Pinterest",
        "com.whatsapp" to "WhatsApp",
        "com.facebook.orca" to "Messenger",
        "com.vkontakte.android" to "VK",
        "com.reddit.frontpage" to "Reddit",
        "org.telegram.messenger" to "Telegram",
        "com.discord" to "Discord"
    )

    // 추적 활성화 상태만 유지
    private val _isTrackingEnabled = MutableStateFlow(prefs.getBoolean(KEY_TRACKING_ENABLED, true))

    /**
     * 패키지명으로 소셜 미디어 앱인지 확인
     */
    fun isSocialMediaApp(packageName: String): Boolean {
        return socialMediaApps.containsKey(packageName)
    }

    /**
     * 패키지명에 해당하는 앱 이름 반환
     */
    fun getAppName(packageName: String): String {
        return socialMediaApps[packageName] ?: packageName.substringAfterLast('.')
    }

    /**
     * 추적 기능 활성화 상태 반환
     */
    override fun isTrackingEnabled(): Flow<Boolean> = _isTrackingEnabled.asStateFlow()

    /**
     * 추적 기능 활성화 상태 설정
     */
    override suspend fun setTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply()
        _isTrackingEnabled.value = enabled
    }

    // 사용하지 않는 메서드들은 빈 구현으로 남겨둠
    // 인터페이스 호환성을 위해 유지
    override fun getAppUsageData() = MutableStateFlow(emptyList<Any>()).asStateFlow()
    override suspend fun trackAppUsage(packageName: String, appName: String, durationMs: Long) {}
    override suspend fun resetAppUsage() {}
}