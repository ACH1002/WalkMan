package inu.appcenter.walkman.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.repository.StepCountRepository
import inu.appcenter.walkman.domain.repository.UserRepository
import inu.appcenter.walkman.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "StepCounterChannel"
    }

    @Inject
    lateinit var stepCountRepository: StepCountRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepCounter: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var initialSteps: Int = -1
    private var previousTotalSteps: Int = 0
    private var currentSteps: Int = 0
    private var lastSaveTimeMillis: Long = 0
    private var strideLength: Float = 0.75f  // 기본값 (미터 단위)
    private var userWeight: Float = 60f      // 기본값 (kg 단위)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // 센서 매니저 초기화
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // CPU 절전 모드에서도 서비스 실행을 위한 Wake Lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StepCounter:WakeLock"
        ).apply {
            acquire(10*60*1000L) // 10분 동안 WakeLock 유지
        }

        // 사용자 정보 로드
        serviceScope.launch {
            try {
                val userInfo = userRepository.getUserInfo().first()
                userWeight = userInfo.weight.toFloatOrNull() ?: 60f

                // 보폭 계산 (키의 약 0.45배로 추정)
                val height = userInfo.height.toFloatOrNull() ?: 170f
                strideLength = (height * 0.45f) / 100f  // cm에서 m로 변환

                Log.d(TAG, "User weight: $userWeight kg, stride length: $strideLength m")

                // 이전 저장된 걸음 수 데이터 로드
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val dailySteps = stepCountRepository.getStepsForDay(today)
                previousTotalSteps = dailySteps.steps
                Log.d(TAG, "Loaded previous steps: $previousTotalSteps")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        // 이 부분을 아래 코드로 교체
        // startForeground(NOTIFICATION_ID, createNotification("걸음 수 측정 중..."))

        // 포그라운드 서비스로 실행 (Android 14 이상용 타입 지정)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("걸음 수 측정 중..."),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("걸음 수 측정 중..."))
        }

        // 센서 리스너 등록
        stepCounter?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step counter sensor registered")
        } ?: run {
            Log.e(TAG, "Step counter sensor not available on this device")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")

        // 진행 중인 데이터 저장
        saveCurrentSteps()

        // 센서 리스너 해제
        sensorManager.unregisterListener(this)

        // WakeLock 해제
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()

            // 초기 걸음 수 설정 (첫 실행 시)
            if (initialSteps == -1) {
                initialSteps = totalSteps
                Log.d(TAG, "Initial steps: $initialSteps")
            }

            // 현재 세션의 걸음 수 계산
            currentSteps = totalSteps - initialSteps + previousTotalSteps

            // 걸음 수를 주기적으로 저장 (30초마다)
            val currentTimeMillis = System.currentTimeMillis()
            if (currentTimeMillis - lastSaveTimeMillis > 30000) {
                lastSaveTimeMillis = currentTimeMillis
                saveCurrentSteps()
            }

            // 노티피케이션 업데이트
            updateNotification()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 정확도 변경 시 처리 (필요한 경우)
    }

    private fun saveCurrentSteps() {
        serviceScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val distance = calculateDistance(currentSteps.toFloat())
                val calories = calculateCalories(currentSteps.toFloat(), distance)

                // 오늘 날짜 기준 (자정)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // 데이터 저장
                stepCountRepository.saveStepData(
                    date = today,
                    steps = currentSteps,
                    distance = distance,
                    calories = calories
                )

                Log.d(TAG, "Saved steps: $currentSteps, distance: $distance km, calories: $calories kcal")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving step data", e)
            }
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

    private fun createNotification(text: String): Notification {
        // 알림 채널 생성 (Android O 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "걸음 수 측정",
                NotificationManager.IMPORTANCE_LOW
            )
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

        // 알림 생성
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GAITX")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val distance = calculateDistance(currentSteps.toFloat())
        val calories = calculateCalories(currentSteps.toFloat(), distance)

        val text = "걸음 수: $currentSteps | 거리: ${String.format("%.2f", distance)} km | 칼로리: ${String.format("%.1f", calories)} kcal"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(text))
    }
}