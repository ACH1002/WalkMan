package inu.appcenter.walkman.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.repository.AppUsageRepository
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
    }

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    private var notificationUpdateJob: Job? = null

    @Inject
    lateinit var stepCountRepository: StepCountRepository

    @Inject
    lateinit var userRepository: UserRepository

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
                    kotlinx.coroutines.delay(WAKE_LOCK_TIMEOUT / 2)
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

        // 알림 업데이트 시작 (추가된 부분)
        startNotificationUpdates()

        // 서비스가 죽었을 때 자동으로 재시작
        return START_STICKY
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
        notificationUpdateJob?.cancel()
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

    private fun createNotification(text: String = "걸음 측정 및 앱 사용 모니터링 중"): Notification {
        // 알림 채널 생성 (Android O 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "걸음 측정 및 앱 사용 모니터링",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setShowBadge(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_SECRET

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

        // 알림 생성
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GAITX 서비스 실행 중")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_logo_gaitx) // 적절한 아이콘으로 변경
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN) // 최소 우선순위
            .setOngoing(true) // 사용자가 스와이프로 제거할 수 없게 설정
            .setSilent(true) // 소리 없음
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // 잠금화면에서 숨김
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "중지", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification("걸음 측정 및 앱 사용 모니터링 중"))
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob = serviceScope.launch {
            while (true) {
                try {
                    // 소셜 미디어 앱 사용 데이터 가져오기
                    val appUsageData = appUsageRepository.getAppUsageData().first()

                    // 총 사용 시간 계산
                    val totalUsageTime = appUsageData.sumOf { it.usageDurationMs }
                    val formattedTime = formatDuration(totalUsageTime)

                    // 알림 업데이트
                    val notificationText = if (totalUsageTime > 0) {
                        "걷는 중 소셜 미디어 사용 시간: $formattedTime"
                    } else {
                        "걸음 측정 및 앱 사용 모니터링 중"
                    }

                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, createNotification(notificationText))

                    delay(30000) // 30초마다 업데이트
                } catch (e: Exception) {
                    Log.e(TAG, "알림 업데이트 오류", e)
                    delay(60000) // 오류 발생 시 1분 후 재시도
                }
            }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            else -> "${seconds}초"
        }
    }

    // 현재 걸음 수를 위한 Flow 제공
    fun getStepCountFlow() = _stepCount.asStateFlow()
}