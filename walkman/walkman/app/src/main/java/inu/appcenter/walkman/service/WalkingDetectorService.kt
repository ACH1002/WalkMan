package inu.appcenter.walkman.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 걷기 상태를 감지하고 소셜 미디어 앱 사용 시 경고를 제공하는 서비스
 * - 걸음수 계산 없이 단순히 걷고 있는지만 감지
 * - 걷는 중 소셜 미디어 앱 사용 시 경고 표시
 */
@AndroidEntryPoint
class WalkingDetectorService : Service(), SensorEventListener {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    companion object {
        private const val TAG = "WalkingDetectorService"
        const val ACTION_START = "START_SERVICE"
        const val ACTION_STOP = "STOP_SERVICE"

        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "walking_detector_channel"

        // 소셜 미디어 앱 변경 관련 액션
        const val ACTION_FOREGROUND_APP_CHANGED = "inu.appcenter.walkman.FOREGROUND_APP_CHANGED"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_IS_SOCIAL_MEDIA = "is_social_media"

        // 걷기 감지 관련 설정
        private const val MIN_STEP_INTERVAL = 250L     // 걸음 감지 간의 최소 간격(ms)
        private const val WALKING_TIMEOUT = 10000L     // 걷기 중단으로 판단하는 시간(ms)

        // 경고 관련 설정
        private const val WARNING_INTERVAL = 60000L    // 동일 앱에 대한 경고 표시 간격(ms)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var sensorManager: SensorManager
    private var stepDetector: Sensor? = null
    private var accelerometer: Sensor? = null

    private var isRunning = false
    private var isWalking = false
    private var lastStepTime = 0L
    private var lastCheckedAppTime = 0L
    private var lastWarningTime = 0L
    private var currentSocialMediaApp = ""

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // 센서 설정
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        // 만약 STEP_DETECTOR가 없다면 가속도계 사용
        if (stepDetector == null) {
            Log.d(TAG, "Step detector not available, using accelerometer")
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        // WakeLock 설정
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WalkingDetector:WakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> startService()
            ACTION_STOP -> stopService()
            // 테스트용 강제 걷기 상태 설정
            "FORCE_WALKING" -> {
                val forced = intent.getBooleanExtra("is_walking", false)
                isWalking = forced
                lastStepTime = System.currentTimeMillis()
                Log.d(TAG, "강제 걷기 상태 설정: $isWalking")
            }
        }

        return START_STICKY
    }

    private fun startService() {
        if (isRunning) return

        Log.d(TAG, "Starting service")
        isRunning = true

        // 사용 통계 권한 확인
        if (!hasUsageStatsPermission()) {
            Log.e(TAG, "사용 통계 권한이 없습니다")
            stopSelf()
            return
        }

        // 포그라운드 서비스 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }

        // WakeLock 획득
        wakeLock?.acquire(10 * 60 * 1000L) // 10분

        // 센서 리스너 등록
        if (stepDetector != null) {
            sensorManager.registerListener(
                this,
                stepDetector,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Step detector registered")
        } else if (accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Accelerometer registered (fallback)")
        }

        // 앱 사용 모니터링 시작
        startAppMonitoring()
    }

    private fun stopService() {
        if (!isRunning) return

        Log.d(TAG, "Stopping service")
        isRunning = false

        // 센서 리스너 해제
        sensorManager.unregisterListener(this)

        // WakeLock 해제
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        stopForeground(true)
        stopSelf()
    }

    private fun startAppMonitoring() {
        serviceScope.launch {
            try {
                Log.d(TAG, "앱 모니터링 시작")

                // 걷기 상태 확인 루프
                launch {
                    while (isRunning) {
                        checkWalkingState()
                        delay(1000)
                    }
                }

                // 앱 사용 확인 루프
                while (isRunning) {
                    try {
                        val now = System.currentTimeMillis()

                        // 현재 걷는 중인지 로그
//                        Log.d(TAG, "현재 걷는 중: $isWalking (마지막 걸음: ${now - lastStepTime}ms 전)")

                        if (isWalking) {
                            // 3초마다 앱 확인
                            if (now - lastCheckedAppTime > 3000) {
                                val currentApp = getForegroundPackageName()
                                lastCheckedAppTime = now

                                if (currentApp.isNotEmpty()) {
                                    val isSocialMedia = isSocialMediaApp(currentApp)
                                    val appName = getAppName(currentApp)

                                    Log.d(TAG, "현재 앱: $appName, 소셜미디어: $isSocialMedia")

                                    if (isSocialMedia) {
                                        // 처음 감지되거나 일정 시간 지난 후 경고
                                        if (currentApp != currentSocialMediaApp ||
                                            now - lastWarningTime > WARNING_INTERVAL) {

                                            currentSocialMediaApp = currentApp
                                            lastWarningTime = now

                                            Log.d(TAG, "소셜미디어 앱 사용 감지 - 경고 표시: $appName")
                                            showWalkingWarning(appName)
                                        }
                                    } else {
                                        currentSocialMediaApp = ""
                                    }
                                }
                            }
                        } else {
                            // 걷지 않는 경우 추적 초기화
                            currentSocialMediaApp = ""
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "앱 모니터링 오류", e)
                    }

                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "앱 모니터링 태스크 오류", e)
            }
        }
    }

    private fun checkWalkingState() {
        val now = System.currentTimeMillis()
        val timeSinceLastStep = now - lastStepTime

        // 일정 시간 동안 걸음이 감지되지 않으면 걷기 중단으로 판단
        if (isWalking && timeSinceLastStep > WALKING_TIMEOUT) {
            isWalking = false
            Log.d(TAG, "걷기 중단 감지: ${timeSinceLastStep}ms 동안 걸음 없음")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                // 최소 걸음 간격 확인 (디바운싱)
                if (currentTime - lastStepTime > MIN_STEP_INTERVAL) {
                    handleStepDetected(currentTime)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // 가속도계 기반 걸음 감지는 더 복잡하지만 여기서는
                // 간단한 임계값 기반 감지만 구현 (필요시 개선)
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // 가속도 벡터의 크기 계산
                val magnitude = Math.sqrt((x*x + y*y + z*z).toDouble())

                // 가속도 변화 임계값으로 걷기 감지 (간단한 구현)
                // 실제로는 더 정교한 알고리즘이 필요할 수 있음
                if (magnitude > 12 && currentTime - lastStepTime > MIN_STEP_INTERVAL) {
                    handleStepDetected(currentTime)
                }
            }
        }
    }

    private fun handleStepDetected(currentTime: Long) {
        val wasWalking = isWalking
        isWalking = true
        lastStepTime = currentTime

        if (!wasWalking) {
            Log.d(TAG, "걷기 시작 감지")

            // 걷기 시작 시 현재 앱 바로 확인
            serviceScope.launch {
                try {
                    val currentApp = getForegroundPackageName()
                    if (isSocialMediaApp(currentApp)) {
                        val appName = getAppName(currentApp)
                        Log.d(TAG, "걷기 시작 시 소셜미디어 사용 중 - 경고 표시: $appName")
                        currentSocialMediaApp = currentApp
                        lastWarningTime = System.currentTimeMillis()
                        showWalkingWarning(appName)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "걷기 시작 시 앱 확인 오류", e)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 정확도 변경 처리
    }

    private fun getForegroundPackageName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val time = System.currentTimeMillis()

                // 최근 5분간의 앱 사용 통계 조회
                val appList = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    time - 1000 * 60 * 5,
                    time
                )

                if (appList != null && appList.isNotEmpty()) {
                    // 가장 최근에 사용된 앱 찾기
                    val sortedApps = appList.sortedByDescending { it.lastTimeUsed }
                    sortedApps.firstOrNull()?.packageName ?: ""
                } else {
                    Log.w(TAG, "앱 사용 통계를 찾을 수 없음")
                    ""
                }
            } else {
                // 구 버전 API용 대체 방식
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.runningAppProcesses?.firstOrNull {
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }?.processName ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "포그라운드 앱 확인 중 오류", e)
            ""
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            }

            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "사용 통계 권한 확인 오류", e)
            false
        }
    }

    private fun isSocialMediaApp(packageName: String): Boolean {
        val socialApps = listOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically",
            "com.facebook.android",
            "com.instagram.barcelona",
            "com.snapchat.android",
            "com.linkedin.android",
            "com.pinterest",
            "com.whatsapp",
            "com.facebook.orca",
            "com.vkontakte.android",
            "com.reddit.frontpage",
            "org.telegram.messenger",
            "com.discord"
        )

        return socialApps.contains(packageName)
    }

    private fun getAppName(packageName: String): String {
        // 일반적인 소셜미디어 앱 이름 매핑
        val appNames = mapOf(
            "com.google.android.youtube" to "YouTube",
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "Twitter",
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

        return appNames[packageName] ?: try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun showWalkingWarning(appName: String = "") {
        try {
            val intent = Intent(this, WalkingWarningService::class.java).apply {
                action = WalkingWarningService.ACTION_SHOW_WARNING
                putExtra(WalkingWarningService.EXTRA_APP_NAME, appName)
            }
            startService(intent)
            Log.d(TAG, "경고 서비스 시작: $appName")
        } catch (e: Exception) {
            Log.e(TAG, "경고 서비스 시작 실패", e)
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "걷기 감지 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "걷는 중 소셜 미디어 사용 시 경고를 제공합니다"
                setShowBadge(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, WalkingDetectorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("걷기 감지 서비스")
            .setContentText("걷는 중 소셜 미디어 사용 시 경고합니다")
            .setSmallIcon(R.drawable.ic_logo_gaitx)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "중지", stopPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")

        // 센서 리스너 해제
        sensorManager.unregisterListener(this)

        // WakeLock 해제
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}