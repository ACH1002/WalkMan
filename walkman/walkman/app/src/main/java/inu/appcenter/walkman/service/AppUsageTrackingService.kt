package inu.appcenter.walkman.service

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppUsageTrackingService : Service() {

    companion object {
        private const val TAG = "AppUsageTrackingService"
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "app_usage_tracking_channel"
    }

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var sensorRepository: SensorRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    private var isTracking = false
    private var lastTrackedPackage = ""
    private var lastTrackedTime = 0L

    override fun onCreate() {
        super.onCreate()

        // WakeLock 설정
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AppUsageTracking:WakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }

        return START_STICKY
    }

    private fun startTracking() {
        if (isTracking) return

        // 포그라운드 서비스 시작
        startForeground(NOTIFICATION_ID, createForegroundNotification())

        wakeLock?.acquire(10 * 60 * 1000L) // 10분 동안 WakeLock 획득
        isTracking = true

        serviceScope.launch {
            try {
                // 사용 권한 확인
                if (!hasUsageStatsPermission()) {
                    Log.e(TAG, "사용 통계 권한이 없습니다.")
                    stopSelf()
                    return@launch
                }

                // 걸음 측정 중일 때만 앱 사용 추적
                while (isTracking) {
                    val isWalking = sensorRepository.isRecording().first()

                    if (isWalking) {
                        val currentPackage = getForegroundPackageName()
                        val currentTime = System.currentTimeMillis()

                        if (currentPackage.isNotEmpty() && currentPackage != lastTrackedPackage) {
                            // 이전 앱 사용 시간 기록
                            if (lastTrackedPackage.isNotEmpty() && lastTrackedTime > 0) {
                                val duration = currentTime - lastTrackedTime
                                appUsageRepository.trackAppUsage(
                                    lastTrackedPackage,
                                    getAppName(lastTrackedPackage),
                                    duration
                                )
                            }

                            // 새 앱 추적 시작
                            lastTrackedPackage = currentPackage
                            lastTrackedTime = currentTime

                            // 소셜 미디어 앱 사용 중이면 경고 표시
                            if (isSocialMediaApp(currentPackage)) {
                                showWalkingWarning()
                            }
                        }
                    }

                    delay(1000) // 1초마다 확인
                }
            } catch (e: Exception) {
                Log.e(TAG, "앱 사용 추적 중 오류: ${e.message}")
            }
        }
    }

    private fun stopTracking() {
        isTracking = false

        // 마지막 앱 사용 시간 기록
        if (lastTrackedPackage.isNotEmpty() && lastTrackedTime > 0) {
            val duration = System.currentTimeMillis() - lastTrackedTime
            serviceScope.launch {
                appUsageRepository.trackAppUsage(
                    lastTrackedPackage,
                    getAppName(lastTrackedPackage),
                    duration
                )
            }
        }

        wakeLock?.release()
        stopForeground(true)
        stopSelf()
    }

    private fun getForegroundPackageName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val time = System.currentTimeMillis()
                val appList = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    time - 1000 * 10, // 10초 전부터
                    time
                )

                if (appList != null && appList.isNotEmpty()) {
                    appList.sortByDescending { it.lastTimeUsed }
                    return appList[0].packageName
                }
            }

            // 대체 방법 (API 21 미만)
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.runningAppProcesses?.firstOrNull {
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }?.processName ?: ""

        } catch (e: Exception) {
            Log.e(TAG, "포그라운드 앱 확인 실패: ${e.message}")
            ""
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 60, // 1분 전부터
                time
            )
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun isSocialMediaApp(packageName: String): Boolean {
        val socialApps = listOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.facebook.android",
            "com.instagram.barcelona"
        )
        return socialApps.contains(packageName)
    }

    private fun showWalkingWarning() {
        // 걷기 중 소셜 미디어 사용 경고 표시
        serviceScope.launch {
            val intent = Intent(this@AppUsageTrackingService, WalkingWarningService::class.java).apply {
                action = WalkingWarningService.ACTION_SHOW_WARNING
            }
            startService(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "앱 사용 추적",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "걷는 중 소셜 미디어 사용 시간을 추적합니다"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): android.app.Notification {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("걷기 중 앱 사용 모니터링")
            .setContentText("걷는 중 소셜 미디어 사용 시간을 추적하고 있습니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 적절한 아이콘으로 변경
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }
}