package inu.appcenter.walkman.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StepCountRepository
import inu.appcenter.walkman.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.os.Process
import inu.appcenter.walkman.data.repository.SensorRepositoryImpl

@AndroidEntryPoint
class AppUsageTrackingService : Service() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var stepCountRepository: StepCountRepository

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    companion object {
        private const val TAG = "AppUsageTrackingService"
        const val ACTION_START = "START_TRACKING"
        const val ACTION_STOP = "STOP_TRACKING"
        const val ACTION_FOREGROUND_APP_CHANGED = "inu.appcenter.walkman.FOREGROUND_APP_CHANGED"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_IS_SOCIAL_MEDIA = "is_social_media"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "app_usage_tracking_channel"
        private const val STEP_THRESHOLD = 1 // 걸음 감지 임계값 - 이 값 이상이면 걷는 것으로 판단
        private const val STEP_CHECK_PERIOD = 1000L // 걸음 체크 주기 (밀리초)
        // 중요: 이 값을 StepCounterService에서도 동일하게 사용해야 함
        const val STEP_DETECTION_ACTION = "inu.appcenter.walkman.STEP_DETECTED"
        const val ACTION_STEP_UPDATED = "inu.appcenter.walkman.STEP_UPDATED"
        const val EXTRA_STEP_COUNT = "step_count"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    private var isTracking = false
    private var lastTrackedPackage = ""
    private var lastTrackedTime = 0L
    private var lastStepCount = 0
    private var isWalking = false

    private var lastStepDetectionTime = 0L
    // 1. WALKING_TIMEOUT 값을 더 길게 설정
    private val WALKING_TIMEOUT = 30 * 1000L // 10초 -> 30초로 연장
    private var lastWarningTime = 0L
    private val WARNING_INTERVAL = 60 * 1000L

    // 걸음 감지 브로드캐스트 리시버
    private val stepDetectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "브로드캐스트 리시버 액션 수신: $action")

            when (action) {
                STEP_DETECTION_ACTION, ACTION_STEP_UPDATED -> {
                    // Try multiple keys for step count to ensure compatibility
                    val stepCount = intent.getIntExtra("step_count",
                        intent.getIntExtra(EXTRA_STEP_COUNT, 0))

                    Log.d(TAG, """
                브로드캐스트 리시버 - 걸음 수 업데이트:
                - 액션: $action
                - 받은 걸음 수: $stepCount
                - 마지막 저장된 걸음 수: $lastStepCount
                """.trimIndent())

                    // IMPORTANT: Always update the walking state when steps are detected
                    lastStepCount = stepCount
                    lastStepDetectionTime = System.currentTimeMillis()
                    isWalking = true

                    Log.d(TAG, "걷기 상태 변경됨: true (걸음 수: $stepCount)")
                }
                "DIRECT_WALKING_STATE_CHANGE" -> {
                    isWalking = intent.getBooleanExtra("is_walking", false)
                    lastStepDetectionTime = System.currentTimeMillis()
                    Log.d(TAG, "직접 걷기 상태 변경 수신됨: $isWalking")
                }
                else -> Log.d(TAG, "알 수 없는 액션 수신: $action")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppUsageTrackingService onCreate")
        // WakeLock 설정
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AppUsageTracking:WakeLock"
            )
            Log.d(TAG, "WakeLock setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up WakeLock", e)
        }

        // 걸음 감지 브로드캐스트 리시버 등록
        try {
            val intentFilter = IntentFilter().apply {
                addAction(STEP_DETECTION_ACTION)
                addAction(ACTION_STEP_UPDATED) // 이 행을 추가
            }
            intentFilter.addAction("DIRECT_WALKING_STATE_CHANGE")


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(stepDetectionReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(stepDetectionReceiver, intentFilter)
            }

            Log.d(TAG, "Step detection broadcast receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register step detection receiver", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")

        // 사용 통계 권한 확인
        if (!hasUsageStatsPermission()) {
            Log.e(TAG, "사용 통계 권한이 없습니다.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Android 14 이상에서 포그라운드 서비스 타입 명시
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // 기존 로직 유지
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AppUsageTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 사용 모니터링")
            .setContentText("걷는 중 소셜 미디어 사용 시간을 추적하고 있습니다")
            .setSmallIcon(R.drawable.ic_logo_gaitx)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .setChannelId(CHANNEL_ID)  // 채널 ID 명시적 설정
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "중지", stopPendingIntent)
            .setExtras(Bundle().apply {
                putString("android.substName", "walkman_step_counter")
            })
            .build()
    }

    private fun startTracking() {
        if (isTracking) return

        // 권한 확인 강화
        if (!hasUsageStatsPermission()) {
            Log.e(TAG, "Cannot start tracking: No usage stats permission")
            stopSelf()
            return
        }

        Log.d(TAG, "Starting tracking")

        // 포그라운드 서비스 시작
        try {
            startForeground(NOTIFICATION_ID, createForegroundNotification())
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }

        // WakeLock 획득
        try {
            wakeLock?.acquire(10 * 60 * 1000L) // 10분 동안 WakeLock 획득
            Log.d(TAG, "WakeLock acquired for 10 minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }

        isTracking = true

        // 걸음 수 변화 감지를 위한 초기 값 설정
        serviceScope.launch {
            try {
                // 현재 걸음 수 가져오기
                lastStepCount = (sensorRepository as? SensorRepositoryImpl)?.getCurrentStepCount() ?: 0
                Log.d(TAG, "Initial step count: $lastStepCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting initial step count", e)
            }
        }

        // 지속적으로 걸음 수 변화 감지 및 앱 사용 추적
        serviceScope.launch {
            try {
                Log.d(TAG, "앱 사용 추적 시작됨")
                lastTrackedPackage = ""
                lastTrackedTime = 0L

                // 걸음 감지 스레드 시작
                launch {
                    monitorStepChanges()
                }

                // 걷기 상태 체크 스레드 시작
                launch {
                    while (isTracking) {
                        checkWalkingStatus()
                        delay(1000) // 1초마다 걷기 상태 확인
                    }
                }

                // 앱 사용 모니터링 스레드
                while (isTracking) {
                    try {
                        Log.d(TAG, "현재 걷는 중: $isWalking")

                        if (isWalking) {
                            val currentPackage = getForegroundPackageName()
                            val currentTime = System.currentTimeMillis()

                            if (currentPackage.isNotEmpty()) {
                                Log.d(TAG, "현재 사용 중인 앱: $currentPackage")

                                // 앱이 변경된 경우
                                if (currentPackage != lastTrackedPackage) {
                                    // 이전 앱 사용 시간 기록
                                    if (lastTrackedPackage.isNotEmpty() && lastTrackedTime > 0) {
                                        val duration = currentTime - lastTrackedTime
                                        Log.d(TAG, "이전 앱($lastTrackedPackage) 사용 시간: ${duration}ms")

                                        if (isSocialMediaApp(lastTrackedPackage)) {
                                            appUsageRepository.trackAppUsage(
                                                lastTrackedPackage,
                                                getAppName(lastTrackedPackage),
                                                duration
                                            )
                                        }
                                    }

                                    // 새 앱 추적 시작
                                    lastTrackedPackage = currentPackage
                                    lastTrackedTime = currentTime
                                    Log.d(TAG, "새 앱 추적 시작: $currentPackage")

                                    // 앱 변경 브로드캐스트 전송
                                    broadcastForegroundAppChange(currentPackage)

                                    // 소셜 미디어 앱 사용 중이면 경고 표시
                                    if (isSocialMediaApp(currentPackage)) {
                                        Log.d(TAG, "소셜미디어 앱 사용 중 경고 표시")
                                        showWalkingWarning(currentPackage)
                                        lastWarningTime = currentTime
                                    }
                                } else {
                                    // 같은 앱을 계속 사용 중인 경우

                                    // 소셜 미디어 앱을 지속적으로 사용 중이면서 마지막 경고로부터 일정 시간이 지났으면 다시 경고
                                    if (isSocialMediaApp(currentPackage) &&
                                        (currentTime - lastWarningTime > WARNING_INTERVAL)) {
                                        Log.d(TAG, "소셜미디어 앱 지속 사용 중 - 주기적 경고 표시")
                                        showWalkingWarning(currentPackage)
                                        lastWarningTime = currentTime
                                    }

                                    // 주기적으로 사용 시간 저장 (기존 코드)
                                    if (currentTime - lastTrackedTime > 30000) { // 30초마다 저장
                                        val duration = currentTime - lastTrackedTime

                                        if (isSocialMediaApp(currentPackage)) {
                                            Log.d(TAG, "소셜미디어 앱($currentPackage) 사용 중, 시간 업데이트: ${duration}ms")
                                            appUsageRepository.trackAppUsage(
                                                currentPackage,
                                                getAppName(currentPackage),
                                                duration
                                            )

                                            // 시간 갱신
                                            lastTrackedTime = currentTime

                                            // 브로드캐스트 다시 전송 (갱신용)
                                            broadcastForegroundAppChange(currentPackage)
                                        }
                                    }
                                }
                            }
                        } else if (lastTrackedPackage.isNotEmpty() && lastTrackedTime > 0) {
                            // 걷기 중단된 경우, 마지막 앱 사용 기록 저장
                            val duration = System.currentTimeMillis() - lastTrackedTime
                            Log.d(TAG, "걷기 중단됨, 마지막 앱($lastTrackedPackage) 사용 시간: ${duration}ms")

                            if (isSocialMediaApp(lastTrackedPackage)) {
                                appUsageRepository.trackAppUsage(
                                    lastTrackedPackage,
                                    getAppName(lastTrackedPackage),
                                    duration
                                )
                            }

                            // 추적 초기화
                            lastTrackedPackage = ""
                            lastTrackedTime = 0L
                            lastWarningTime = 0L

                            // 추적 중단 브로드캐스트 전송
                            broadcastForegroundAppChange("")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "앱 사용 확인 중 오류: ${e.message}", e)
                    }

                    delay(1000) // 1초마다 확인
                }
            } catch (e: Exception) {
                Log.e(TAG, "앱 사용 추적 중 오류: ${e.message}", e)
            }
        }
    }

    // 걸음 수 모니터링 함수
    private suspend fun monitorStepChanges() {
        while (isTracking) {
            try {
                // 직접 SensorRepositoryImpl의 메서드 호출 대신
                // 다른 방법으로 현재 걸음 수 가져오기
                val sensorRepo = sensorRepository
                val currentStepCount = if (sensorRepo is SensorRepositoryImpl) {
                    sensorRepo.getCurrentStepCount()
                } else {
                    Log.e(TAG, "SensorRepository가 SensorRepositoryImpl 타입이 아닙니다.")
                    0
                }

                val prevStepCount = lastStepCount
                val stepDiff = currentStepCount - prevStepCount

                Log.d(TAG, """
                걸음 수 상세 로그:
                - 이전 걸음 수: $prevStepCount
                - 현재 걸음 수: $currentStepCount
                - 걸음 차이: $stepDiff
                - 현재 걷기 상태: $isWalking
                - sensorRepository 타입: ${sensorRepository.javaClass.simpleName}
            """.trimIndent())

                if (stepDiff >= STEP_THRESHOLD) {
                    val prevWalkingState = isWalking
                    isWalking = true
                    lastStepDetectionTime = System.currentTimeMillis()

                    // 상태 변경 시 명시적으로 로그 추가
                    Log.d(TAG, "걸음 수 변화로 걷기 상태 강제 변경: false -> true")

                    // 현재 소셜미디어 앱 사용 중이면 즉시 경고 표시
                    val currentPackage = getForegroundPackageName()
                    if (isSocialMediaApp(currentPackage)) {
                        showWalkingWarning(currentPackage)
                    }
                }

                if (stepDiff > 0) {
                    lastStepCount = currentStepCount
                }
            } catch (e: Exception) {
                Log.e(TAG, "걸음 수 모니터링 중 오류: ${e.message}", e)
            }

            delay(STEP_CHECK_PERIOD)
        }
    }

    // 걷기 상태 체크 함수
    private fun checkWalkingStatus() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastStep = currentTime - lastStepDetectionTime

        // 상태 변경 전 로그 추가
        if (timeSinceLastStep > WALKING_TIMEOUT * 0.8 && isWalking) {
            Log.d(TAG, "걷기 중단 임박: ${timeSinceLastStep}ms 동안 걸음 없음 (임계값: ${WALKING_TIMEOUT}ms)")
        }

        if (timeSinceLastStep > WALKING_TIMEOUT && isWalking) {
            val prevState = isWalking
            isWalking = false
            Log.d(TAG, "걷기 중단 감지: ${timeSinceLastStep}ms 동안 걸음 없음, 상태 변경: $prevState -> $isWalking")
        }
    }

    private fun stopTracking() {
        if (!isTracking) return

        Log.d(TAG, "Stopping tracking")
        isTracking = false

        // 마지막 앱 사용 시간 기록
        if (lastTrackedPackage.isNotEmpty() && lastTrackedTime > 0) {
            val duration = System.currentTimeMillis() - lastTrackedTime
            serviceScope.launch {
                try {
                    if (isSocialMediaApp(lastTrackedPackage)) {
                        appUsageRepository.trackAppUsage(
                            lastTrackedPackage,
                            getAppName(lastTrackedPackage),
                            duration
                        )
                    }
                    Log.d(TAG, "마지막 앱 사용 기록 저장 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "마지막 앱 사용 기록 저장 실패", e)
                }
            }
        }

        // WakeLock 해제
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WakeLock", e)
        }

        try {
            stopForeground(true)
            Log.d(TAG, "Foreground service stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop foreground service", e)
        }

        stopSelf()
    }

    private fun getForegroundPackageName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val time = System.currentTimeMillis()

                // 시간 범위를 더 넓게 설정
                val appList = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    time - 1000 * 60 * 5, // 5분 전부터
                    time
                )

                // 로깅 강화
                Log.d(TAG, "UsageStatsManager 쿼리 결과:")
                Log.d(TAG, "현재 시간: $time")
                Log.d(TAG, "쿼리 시작 시간: ${time - 1000 * 60 * 5}")

                if (appList != null && appList.isNotEmpty()) {
                    Log.d(TAG, "발견된 앱 사용 통계 엔트리 수: ${appList.size}")

                    // 최근 사용된 앱을 기준으로 정렬
                    val sortedApps = appList.sortedByDescending { it.lastTimeUsed }

                    // 첫 번째 앱이 현재 사용 중인 앱일 가능성이 높음
                    val topApp = sortedApps.firstOrNull()
                    if (topApp != null) {
                        Log.d(TAG, "최상위 앱 선택: ${topApp.packageName}, 마지막 사용 시간: ${topApp.lastTimeUsed}")
                        return topApp.packageName
                    }
                } else {
                    Log.w(TAG, "앱 사용 통계를 찾을 수 없음")
                }
            }

            // 대체 방법 (API 21 미만 또는 다른 방법이 실패한 경우)
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val foregroundApp = am.runningAppProcesses?.firstOrNull {
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }?.processName

            if (foregroundApp != null) {
                Log.d(TAG, "ActivityManager에서 포그라운드 앱 발견: $foregroundApp")
                return foregroundApp
            }

            ""
        } catch (e: Exception) {
            Log.e(TAG, "포그라운드 앱 확인 중 예외 발생", e)
            e.printStackTrace()
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

            val hasPermission = mode == android.app.AppOpsManager.MODE_ALLOWED
            Log.d(TAG, "Usage stats permission: $hasPermission")
            hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            false
        }
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

    private fun isSocialMediaApp(packageName: String): Boolean {
        val socialApps = listOf(
            "com.google.android.youtube",         // YouTube
            "com.instagram.android",              // Instagram
            "com.twitter.android",                // Twitter
            "com.zhiliaoapp.musically",           // TikTok
            "com.facebook.android",               // Facebook
            "com.instagram.barcelona",            // Threads
            "com.snapchat.android",               // Snapchat
            "com.linkedin.android",               // LinkedIn
            "com.pinterest",                      // Pinterest
            "com.whatsapp",                       // WhatsApp
            "com.facebook.orca",                  // Facebook Messenger
            "com.vkontakte.android",              // VK
            "com.reddit.frontpage",               // Reddit
            "org.telegram.messenger",             // Telegram
            "com.discord"                         // Discord
        )

        val isSocialApp = socialApps.contains(packageName)

        // 로그 추가
        if (isSocialApp) {
            Log.d(TAG, "Social media app detected: $packageName")
        }

        return isSocialApp
    }

    // 걷기 중 소셜 미디어 사용 경고 표시
    private fun showWalkingWarning(packageName: String = "") {
        // Only show warning if actually walking
        if (!isWalking) {
            Log.d(TAG, "걷기 중이 아니므로 경고 표시 안함")
            return
        }

        serviceScope.launch {
            try {
                val appName = if (packageName.isNotEmpty()) getAppName(packageName) else ""

                val intent = Intent(this@AppUsageTrackingService, WalkingWarningService::class.java).apply {
                    action = WalkingWarningService.ACTION_SHOW_WARNING
                    // 앱 이름 전달
                    putExtra(WalkingWarningService.EXTRA_APP_NAME, appName)
                }
                startService(intent)
                Log.d(TAG, "Walking warning service started for app: $appName")

                // Reset warning time to prevent too frequent warnings
                lastWarningTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start walking warning service", e)
            }
        }
    }

    private fun broadcastStepDetected(stepCount: Int) {
        try {
            // Make sure to use consistent extra name - "step_count" in both intents
            val intent1 = Intent(STEP_DETECTION_ACTION)
            intent1.putExtra("step_count", stepCount)
            sendBroadcast(intent1)

            val intent2 = Intent(ACTION_STEP_UPDATED)
            intent2.putExtra("step_count", stepCount) // Use same extra name
            sendBroadcast(intent2)

            Log.d(TAG, "걸음 감지 브로드캐스트 전송: ${stepCount}걸음")
        } catch (e: Exception) {
            Log.e(TAG, "걸음 감지 브로드캐스트 전송 실패", e)
        }
    }

    // 앱 변경 시 브로드캐스트 전송
    private fun broadcastForegroundAppChange(packageName: String) {
        try {
            val appName = getAppName(packageName)
            val isSocial = isSocialMediaApp(packageName)

            val intent = Intent(ACTION_FOREGROUND_APP_CHANGED).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_IS_SOCIAL_MEDIA, isSocial)
            }

            sendBroadcast(intent)
            Log.d(TAG, "앱 변경 브로드캐스트 전송: $packageName, $appName, 소셜미디어: $isSocial")
        } catch (e: Exception) {
            Log.e(TAG, "앱 변경 브로드캐스트 전송 실패", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "앱 사용 추적",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "걷는 중 소셜 미디어 사용 시간을 추적합니다"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channel", e)
            }
        }
    }

    private fun createForegroundNotification(): android.app.Notification {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AppUsageTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 사용 모니터링")
            .setContentText("걷는 중 소셜 미디어 사용 시간을 추적하고 있습니다")
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
        Log.d(
            TAG,
            "Service onDestroy"
        )

        try {
            // 브로드캐스트 리시버 해제
            try {
                unregisterReceiver(stepDetectionReceiver)
                Log.d(
                    TAG,
                    "Step detection receiver unregistered"
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error unregistering step detection receiver",
                    e
                )
            }

            stopTracking()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error in stopTracking during onDestroy",
                e
            )
        }
    }
}