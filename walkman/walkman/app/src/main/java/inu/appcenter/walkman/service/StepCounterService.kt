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
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import inu.appcenter.walkman.data.repository.SensorRepositoryImpl
import inu.appcenter.walkman.domain.repository.AppUsageRepository
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StepCountRepository
import inu.appcenter.walkman.domain.repository.UserRepository
import inu.appcenter.walkman.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    companion object {
        const val TAG = "StepCounterService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "StepCounterChannel"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"

        // 센서 관련 SharedPreferences
        private const val PREFS_NAME = "step_counter_service_prefs"
        private const val KEY_DAILY_STEPS = "daily_steps"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_LAST_SAVE_TIME = "last_save_time"

        // WakeLock 최대 시간 (30분)
        private const val WAKE_LOCK_TIMEOUT = 1_800_000L

        // 데이터 저장 주기 (15초)
        private const val SAVE_INTERVAL = 15 * 1000L

        // 최소 걸음 간격 (밀리초) - 너무 빠른 걸음 감지 방지
        private const val MIN_STEP_INTERVAL = 250L

        const val STEP_DETECTION_ACTION = "inu.appcenter.walkman.STEP_DETECTED"
        const val ACTION_STEP_UPDATED = "inu.appcenter.walkman.STEP_UPDATED"
        const val EXTRA_STEP_COUNT = "step_count"

        const val ACTION_SIMULATE_WALKING_START = "SIMULATE_WALKING_START"
        const val ACTION_SIMULATE_WALKING_STOP = "SIMULATE_WALKING_STOP"
    }

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var stepCountRepository: StepCountRepository

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var userRepository: UserRepository

    private var notificationUpdateJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepDetector: Sensor? = null
    private var stepCounter: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var prefs: SharedPreferences

    // 걸음 수 관련 변수
    private val _stepCount = MutableStateFlow(0)
    private var lastStepTimestamp = 0L
    private var lastSaveTimeMillis = 0L
    private var strideLength: Float = 0.75f  // 기본값 (미터 단위)
    private var userWeight: Float = 60f      // 기본값 (kg 단위)

    // 현재 사용 중인 소셜미디어 앱 정보를 저장하는 변수
    private var currentSocialMediaApp: String = ""
    private var currentSocialMediaStartTime: Long = 0L

    // 알림 갱신 주기(밀리초)
    private val NOTIFICATION_UPDATE_INTERVAL = 5000L // 5초

    // 앱 사용 상태 변경 수신 브로드캐스트 리시버
    private lateinit var foregroundAppReceiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // 센서 매니저 초기화
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // SharedPreferences 초기화
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Wake Lock 설정 (30분 제한, 주기적으로 갱신)
        setupWakeLock()

        // 날짜 변경 확인 및 걸음 수 초기화
        checkDateChangeAndResetSteps()

        // 저장된 걸음 수 복원
        _stepCount.value = prefs.getInt(KEY_DAILY_STEPS, 0)
        Log.d(TAG, "Service created with steps: ${_stepCount.value}")

        // 사용자 정보 로드
        loadUserInfo()

        // 브로드캐스트 리시버 등록
        foregroundAppReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AppUsageTrackingService.ACTION_FOREGROUND_APP_CHANGED) {
                    val packageName = intent.getStringExtra(AppUsageTrackingService.EXTRA_PACKAGE_NAME) ?: ""
                    val appName = intent.getStringExtra(AppUsageTrackingService.EXTRA_APP_NAME) ?: ""
                    val isSocialMedia = intent.getBooleanExtra(AppUsageTrackingService.EXTRA_IS_SOCIAL_MEDIA, false)

                    Log.d(TAG, "앱 변경 감지: $packageName, $appName, 소셜미디어: $isSocialMedia")

                    if (isSocialMedia) {
                        currentSocialMediaApp = packageName
                        currentSocialMediaStartTime = System.currentTimeMillis()

                        // 즉시 알림 업데이트
                        updateNotification(packageName, 0)
                    } else {
                        // 소셜미디어 앱이 아닌 경우
                        currentSocialMediaApp = ""
                        currentSocialMediaStartTime = 0

                        // 알림 업데이트
                        updateNotification()
                    }
                }
            }
        }

        // 브로드캐스트 리시버 등록 시 Android 13 이상에서는 RECEIVER_NOT_EXPORTED 사용
        val intentFilter = IntentFilter(AppUsageTrackingService.ACTION_FOREGROUND_APP_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                foregroundAppReceiver,
                intentFilter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(foregroundAppReceiver, intentFilter)
        }

        Log.d(TAG, "Broadcast receiver registered for foreground app changes")
    }

    // 날짜 변경 확인 및 걸음 수 초기화
    private fun checkDateChangeAndResetSteps() {
        val currentDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0L)

        if (currentDate > lastResetDate) {
            // 날짜가 변경되었으면 걸음 수 초기화
            resetDailySteps(currentDate)
        }
    }

    // 걸음 수 초기화
    private fun resetDailySteps(currentDate: Long) {
        prefs.edit()
            .putInt(KEY_DAILY_STEPS, 0)
            .putLong(KEY_LAST_RESET_DATE, currentDate)
            .apply()

        _stepCount.value = 0
        Log.d(TAG, "Daily steps reset for date: $currentDate")

        // 당일 걸음 수 데이터 저장소도 초기화
        serviceScope.launch {
            stepCountRepository.resetStepsForDay(currentDate)
        }
    }

    private fun setupWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StepCounter:WakeLock"
            )

            // 30분 동안 WakeLock 획득
            wakeLock?.acquire(WAKE_LOCK_TIMEOUT)

            // 주기적인 WakeLock 갱신을 위한 작업 예약
            serviceScope.launch {
                while (true) {
                    delay(WAKE_LOCK_TIMEOUT / 2)
                    renewWakeLock()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up WakeLock", e)
        }
    }

    private fun renewWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing WakeLock", e)
                }
            }
            try {
                it.acquire(WAKE_LOCK_TIMEOUT)
                Log.d(TAG, "WakeLock renewed for ${WAKE_LOCK_TIMEOUT / 1000} seconds")
            } catch (e: Exception) {
                Log.e(TAG, "Error renewing WakeLock", e)
            }
        }
    }

    private fun loadUserInfo() {
        serviceScope.launch {
            try {
                val userInfo = userRepository.getUserInfo().first()
                userWeight = userInfo.weight.toFloatOrNull() ?: 60f

                // 보폭 계산 (키의 약 0.45배로 추정)
                val height = userInfo.height.toFloatOrNull() ?: 170f
                strideLength = (height * 0.45f) / 100f  // cm에서 m로 변환

                Log.d(TAG, "User weight: $userWeight kg, stride length: $strideLength m")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        // STOP_SERVICE 액션 처리
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "Received stop service action")
            stopSelf()
            return START_NOT_STICKY
        }

        //시뮬레이션
        if (intent?.action == ACTION_SIMULATE_WALKING_START) {
            Log.d(TAG, "Received simulation start request")
            // Force walking state and generate steps
            simulateWalking(true)
            return START_STICKY
        } else if (intent?.action == ACTION_SIMULATE_WALKING_STOP) {
            Log.d(TAG, "Received simulation stop request")
            simulateWalking(false)
            return START_STICKY
        }

        // 포그라운드 서비스로 실행 (Android 14 이상용 타입 지정)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("걸음 측정 및 앱 사용 모니터링 중"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("걸음 측정 및 앱 사용 모니터링 중"))
        }

        // 센서 리스너 등록
        registerStepSensor()

        // 알림 업데이트 시작
        startNotificationUpdates()

        // 서비스가 죽었을 때 자동으로 재시작
        return START_STICKY
    }

    private fun notifyWalkingStateChanged(isWalking: Boolean) {
        val intent = Intent("DIRECT_WALKING_STATE_CHANGE")
        intent.putExtra("is_walking", isWalking)
        intent.setPackage(packageName) // 같은 앱 내에서만 받도록 설정
        sendBroadcast(intent)
        Log.d(TAG, "직접 걷기 상태 변경 브로드캐스트 전송: $isWalking")
    }

    // StepCounterService.kt의 simulateWalking 메소드 수정
    private fun simulateWalking(isWalking: Boolean) {
        // SensorRepositoryImpl의 simulateWalking 메소드 직접 호출
        if (sensorRepository is SensorRepositoryImpl) {
            (sensorRepository as SensorRepositoryImpl).simulateWalking(isWalking)
            Log.d(TAG, "SensorRepositoryImpl.simulateWalking($isWalking) 호출됨")
        }

        // 기존 시뮬레이션 코드는 필요하다면 유지
        if (isWalking) {
            // Start a coroutine to simulate steps
            serviceScope.launch {
                // Trigger step events every 1 second for the simulation
                while (true) {
                    val newStepCount = _stepCount.value + 1
                    _stepCount.value = newStepCount

                    broadcastStepDetected(newStepCount)

                    lastStepTimestamp = System.currentTimeMillis()
                    Log.d(TAG, "시뮬레이션: 걸음 감지됨. 총 걸음 수: $newStepCount")

                    // Save the step data
                    saveDailySteps(newStepCount)

                    delay(1000) // Wait 1 second between steps
                }
            }
        }
    }

    private fun registerStepSensor() {
        // 우선적으로 STEP_DETECTOR 센서 사용 (각 걸음마다 이벤트 발생)
        if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step detector sensor registered")
        } else if (stepCounter != null) {
            // 대체 수단으로 STEP_COUNTER 센서 사용 (누적 걸음 수 제공)
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step counter sensor registered (detector not available)")
        } else {
            Log.e(TAG, "No step detection sensors available on this device")
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")

        try {
            // 알림 업데이트 작업 취소
            notificationUpdateJob?.cancel()

            // 센서 리스너 해제
            sensorManager.unregisterListener(this)

            // 브로드캐스트 리시버 해제
            unregisterReceiver(foregroundAppReceiver)

            // WakeLock 해제
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                // 걸음 감지 센서 - 각 걸음마다 이벤트 발생 (값은 항상 1.0f)
                // 디바운싱 - 너무 빠른 걸음 감지는 무시 (250ms 이내)
                if (currentTime - lastStepTimestamp > MIN_STEP_INTERVAL) {
                    val newStepCount = _stepCount.value + 1
                    _stepCount.value = newStepCount

                    // 걸음 수 저장
                    saveDailySteps(newStepCount)

                    // 알림 업데이트
                    updateNotification()

                    // 걸음 감지 브로드캐스트 전송 (이 부분이 추가됨)
                    broadcastStepDetected(newStepCount)

                    lastStepTimestamp = currentTime
                    Log.d(TAG, "Step detected. Total steps: $newStepCount")
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                // 걸음 수 센서 - 기기 부팅 후 총 누적 걸음 수 제공
                // STEP_DETECTOR가 없는 경우에만 사용
                // 여기서는 단순 로깅만 수행 (구현 필요시 추가)
                Log.d(TAG, "Step counter value: ${event.values[0]}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 센서 정확도 변경 로깅
        Log.d(TAG, "Sensor accuracy changed: $accuracy")
    }

    // 걸음 수 저장
    private fun saveDailySteps(steps: Int) {
        // 너무 자주 저장하지 않도록 제한 (15초마다)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSaveTimeMillis < SAVE_INTERVAL && steps != 1) {
            // 첫 걸음이거나 충분한 시간이 지났을 때만 저장
            return
        }

        // SharedPreferences에 저장
        prefs.edit()
            .putInt(KEY_DAILY_STEPS, steps)
            .putLong(KEY_LAST_SAVE_TIME, currentTime)
            .apply()

        lastSaveTimeMillis = currentTime

        // 걸음 수 데이터 저장소에도 저장
        serviceScope.launch {
            val currentDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // 거리와 칼로리 계산
            val distance = calculateDistance(steps.toFloat())
            val calories = calculateCalories(steps.toFloat(), distance)

            stepCountRepository.saveStepData(
                date = currentDate,
                steps = steps,
                distance = distance,
                calories = calories
            )

            Log.d(TAG, "Saved steps: $steps, distance: $distance km, calories: $calories kcal")
        }
    }

    private fun calculateDistance(steps: Float): Float {
        // 걸음 수에서 거리 계산 (km 단위)
        return steps * strideLength / 1000f
    }

    private fun calculateCalories(steps: Float, distanceKm: Float): Float {
        // 칼로리 계산
        // MET(대사 당량)을 이용한 계산: 걷기는 약 3.0-3.5 MET
        val walkingMet = 3.5f
        val timeHours = (distanceKm / 5.0f) // 평균 5km/h 속도 가정

        // 칼로리 계산 공식: MET * 체중(kg) * 시간(hour)
        return walkingMet * userWeight * timeHours
    }

    private fun createNotification(text: String = "걸음 측정 및 앱 사용 모니터링 중", socialMediaInfo: String? = null): Notification {
        // 알림 채널 생성 (Android O 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "걸음 측정 및 앱 사용 모니터링",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "걷는 중 소셜 미디어 사용 시간을 추적합니다"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // 메인 액티비티로 이동하는 인텐트
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 서비스 중지 인텐트
        val stopIntent = Intent(this, StepCounterService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 알림 빌더 생성
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GAITX 서비스 실행 중")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_logo_gaitx)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "중지", stopPendingIntent)
            .setExtras(Bundle().apply {
                putString("android.substName", "walkman_step_counter")
            })

        // 소셜미디어 사용 정보가 있으면 확장된 알림 스타일 적용
        if (socialMediaInfo != null) {
            val style = NotificationCompat.BigTextStyle()
                .bigText("$text\n\n$socialMediaInfo")
            builder.setStyle(style)
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setColorized(true)
            builder.setColor(Color.parseColor("#3F51B5"))
        }

        return builder.build()
    }

    // 오늘 총 소셜미디어 사용 시간 가져오기
    private suspend fun getTotalSocialMediaUsageToday(): Long {
        try {
            val appUsageData = appUsageRepository.getAppUsageData().first()
            return appUsageData.sumOf { it.usageDurationMs }
        } catch (e: Exception) {
            Log.e(TAG, "소셜미디어 사용 시간 조회 실패", e)
            return 0
        }
    }

    // 알림 업데이트 메서드 - suspend 키워드 제거됨
    private fun updateNotification(currentSocialApp: String? = null, usageDuration: Long = 0) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 기본 알림 텍스트
            var notificationText = "걸음 측정 및 앱 사용 모니터링 중"
            var socialMediaInfo: String? = null

            // 현재 소셜미디어 앱 사용 중인 경우
            if (currentSocialApp != null && currentSocialApp.isNotEmpty()) {
                val appName = getAppName(currentSocialApp)
                val durationText = formatDuration(usageDuration)

                notificationText = "현재 $appName 사용 중"
                socialMediaInfo = "$appName 사용 시간: $durationText\n걸으면서 소셜미디어를 오래 사용하면 주의력이 분산될 수 있습니다."
            } else {
                // 소셜미디어를 사용하지 않는 경우에는 기본 메시지만 표시
                // 총 사용시간은 별도의 코루틴에서 업데이트
                serviceScope.launch {
                    try {
                        val totalUsage = getTotalSocialMediaUsageToday()
                        if (totalUsage > 0) {
                            val updatedText = "오늘 걸은 중 소셜미디어 사용 시간: ${formatDuration(totalUsage)}"
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                createNotification(updatedText)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "총 사용시간 업데이트 실패: ${e.message}", e)
                    }
                }
            }

            notificationManager.notify(NOTIFICATION_ID, createNotification(notificationText, socialMediaInfo))
        } catch (e: Exception) {
            Log.e(TAG, "알림 업데이트 실패: ${e.message}", e)
        }
    }

    // 앱 이름 가져오기 함수
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

        // 매핑된 이름이 있으면 사용, 없으면 패키지 매니저에서 이름 가져오기
        return appNames[packageName] ?: try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            // 앱 이름을 가져올 수 없는 경우 패키지 이름의 마지막 부분 사용
            packageName.substringAfterLast('.')
        }
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob = serviceScope.launch {
            while (true) {
                try {
                    // 현재 걷기 중인지 확인
                    val isWalking = sensorRepository.isRecording().first()

                    if (isWalking) {
                        // 현재 사용 중인 앱 확인
                        val currentPackage = getCurrentForegroundApp()

                        if (isSocialMediaApp(currentPackage)) {
                            // 소셜미디어 앱을 사용 중인 경우
                            if (currentSocialMediaApp != currentPackage) {
                                // 새로운 소셜미디어 앱으로 전환된 경우
                                currentSocialMediaApp = currentPackage
                                currentSocialMediaStartTime = System.currentTimeMillis()
                            }

                            // 현재 사용 시간 계산
                            val usageDuration = System.currentTimeMillis() - currentSocialMediaStartTime

                            // 알림 업데이트
                            updateNotification(currentSocialMediaApp, usageDuration)
                        } else {
                            // 소셜미디어 앱을 사용하지 않는 경우
                            if (currentSocialMediaApp.isNotEmpty()) {
                                // 이전에 소셜미디어 앱을 사용하다가 중단한 경우
                                currentSocialMediaApp = ""
                                currentSocialMediaStartTime = 0L
                            }

                            // 알림 업데이트 (기본 알림 표시)
                            updateNotification()
                        }
                    } else {
                        // 걷지 않는 중인 경우
                        currentSocialMediaApp = ""
                        currentSocialMediaStartTime = 0L

                        // 기본 알림 표시
                        updateNotification()
                    }

                    delay(NOTIFICATION_UPDATE_INTERVAL) // 5초마다 업데이트
                } catch (e: Exception) {
                    Log.e(TAG, "알림 업데이트 오류", e)
                    delay(NOTIFICATION_UPDATE_INTERVAL * 2) // 오류 발생 시 10초 후 재시도
                }
            }
        }
    }

    private fun getCurrentForegroundApp(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val time = System.currentTimeMillis()

                // 최근 1분 간의 앱 사용 기록 조회
                val appList = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    time - 60 * 1000, // 1분 전부터
                    time
                )

                if (appList != null && appList.isNotEmpty()) {
                    // 최근 사용된 앱을 기준으로 정렬
                    val sortedApps = appList.sortedByDescending { it.lastTimeUsed }

                    // 최근 사용된 앱 반환
                    sortedApps.firstOrNull()?.packageName ?: ""
                } else {
                    ""
                }
            } else {
                // API 21 미만에서는 ActivityManager 사용
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.runningAppProcesses?.firstOrNull {
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                }?.processName ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "현재 앱 확인 실패: ${e.message}", e)
            ""
        }
    }

    // 소셜미디어 앱인지 확인하는 함수
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

    private fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return when {
            hours > 0 -> "${hours}시간 ${minutes}분 ${seconds}초"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            else -> "${seconds}초"
        }
    }

    private fun broadcastStepDetected(stepCount: Int) {
        try {
            // 기존 방식
            val intent1 = Intent(STEP_DETECTION_ACTION)
            intent1.putExtra("step_count", stepCount)
            sendBroadcast(intent1)

            // 새로운 방식 추가
            val intent2 = Intent(ACTION_STEP_UPDATED)
            intent2.putExtra(EXTRA_STEP_COUNT, stepCount)
            sendBroadcast(intent2)

            Log.d(TAG, "걸음 감지 브로드캐스트 전송: ${stepCount}걸음")
        } catch (e: Exception) {
            Log.e(TAG, "걸음 감지 브로드캐스트 전송 실패", e)
        }
    }

    // 현재 걸음 수를 위한 Flow 제공
    fun getStepCountFlow() = _stepCount.asStateFlow()
}
