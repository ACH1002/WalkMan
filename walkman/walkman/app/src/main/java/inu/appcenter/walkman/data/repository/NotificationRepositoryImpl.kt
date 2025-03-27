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
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _notificationEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true) // 기본값은 true로 설정
    )

    override fun isNotificationEnabled(): Flow<Boolean> = _notificationEnabled.asStateFlow()

    override suspend fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        _notificationEnabled.value = enabled
    }
}