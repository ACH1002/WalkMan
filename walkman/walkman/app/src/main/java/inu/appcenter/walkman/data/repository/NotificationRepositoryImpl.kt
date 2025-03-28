package inu.appcenter.walkman.data.repository

import android.content.Context
import android.content.SharedPreferences
import inu.appcenter.walkman.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    context: Context
) : NotificationRepository {
    companion object {
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _notificationEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    )

    private val _trackingEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_TRACKING_ENABLED, true)
    )

    override fun isNotificationEnabled(): Flow<Boolean> = _notificationEnabled.asStateFlow()

    // 추적 상태도 함께 관리
    override fun isTrackingEnabled(): Flow<Boolean> = _trackingEnabled.asStateFlow()

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
            .putBoolean(KEY_TRACKING_ENABLED, enabled) // 둘 다 동기화
            .apply()

        _notificationEnabled.value = enabled
        _trackingEnabled.value = enabled
    }
}