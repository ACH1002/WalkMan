package inu.appcenter.walkman.data.repository

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.SensorReading
import inu.appcenter.walkman.domain.repository.SensorRepository
import inu.appcenter.walkman.domain.repository.StepCountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val context: Context,
    private val stepCountRepository: StepCountRepository
) : SensorRepository, SensorEventListener, LocationListener {

    private val TAG = "SensorRepositoryImpl"
    private val PREFS_NAME = "sensor_repository_prefs"
    private val KEY_DAILY_STEPS = "daily_steps"
    private val KEY_LAST_RESET_DATE = "last_reset_date"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // 센서 정의
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // 걸음 감지 센서 (각 걸음마다 이벤트 발생)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    // 백업용 걸음 수 센서
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _isRecording = MutableStateFlow(false)
    override fun isRecording(): Flow<Boolean> = _isRecording.asStateFlow()

    private val _currentReading = MutableStateFlow<SensorReading?>(null)
    override fun getCurrentReadings(): Flow<SensorReading> = _currentReading.asStateFlow() as Flow<SensorReading>

    private val _currentSession = MutableStateFlow<RecordingSession?>(null)

    // 초기화 시 null이 아닌 기본값으로 MutableStateFlow 초기화
    private val _totalDailySteps = MutableStateFlow(0)
    private val _currentStepCount = MutableStateFlow(0)

    private val sensorReadings = mutableListOf<SensorReading>()
    private var recordingStartTime = 0L
    private var sessionSteps = 0 // 현재 세션의 걸음 수

    private var currentLocation: Location? = null
    private var magneticValues: FloatArray? = null

    // 걸음 감지 관련 변수
    private var lastStepTimestamp = 0L
    private val MIN_STEP_INTERVAL = 250L // 최소 걸음 간격 (밀리초)

    companion object {
        const val STEP_DETECTION_ACTION = "inu.appcenter.walkman.STEP_DETECTED"
        const val ACTION_STEP_UPDATED = "inu.appcenter.walkman.STEP_UPDATED"
        const val EXTRA_STEP_COUNT = "step_count"
    }

    init {
        // 필드 초기화 후에 함수 호출
        val dailySteps = getDailyStepsFromPrefs()
        _totalDailySteps.value = dailySteps
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
            // MutableStateFlow에 접근하지 않고 SharedPreferences만 초기화
            prefs.edit()
                .putInt(KEY_DAILY_STEPS, 0)
                .putLong(KEY_LAST_RESET_DATE, currentDate)
                .apply()

            // 당일 걸음 수 데이터 저장소도 초기화
            serviceScope.launch {
                stepCountRepository.resetStepsForDay(currentDate)
            }

            Log.d(TAG, "Daily steps reset for date: $currentDate")
        }
    }

    // 걸음 수 초기화
    // 변경된 resetDailySteps 메서드
    private fun resetDailySteps(currentDate: Long) {
        prefs.edit()
            .putInt(KEY_DAILY_STEPS, 0)
            .putLong(KEY_LAST_RESET_DATE, currentDate)
            .apply()

        // 이제 초기화된 MutableStateFlow에 안전하게 접근
        _totalDailySteps.value = 0
        _currentStepCount.value = 0

        Log.d(TAG, "Daily steps reset for date: $currentDate")

        serviceScope.launch {
            stepCountRepository.resetStepsForDay(currentDate)
        }
    }

    // 저장된 일일 걸음 수 가져오기
    private fun getDailyStepsFromPrefs(): Int {
        // 먼저 날짜 변경 확인만 하고, MutableStateFlow에는 접근하지 않음
        checkDateChangeAndResetSteps()
        return prefs.getInt(KEY_DAILY_STEPS, 0)
    }


    // 일일 걸음 수 업데이트 및 저장
    private fun updateAndSaveDailySteps(newSteps: Int) {
        val currentTotalSteps = _totalDailySteps.value
        if (newSteps > currentTotalSteps) {
            prefs.edit().putInt(KEY_DAILY_STEPS, newSteps).apply()
            _totalDailySteps.value = newSteps

            // 걸음 수 데이터 저장소에도 업데이트
            saveDailyStepsToRepository()
        }
    }

    // 걸음 수 데이터 저장소에 저장
    private fun saveDailyStepsToRepository() {
        coroutineScope.launch {
            val currentDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val steps = _totalDailySteps.value
            // 사용자 키와 체중을 기준으로 거리와 칼로리 계산
            // 여기서는 샘플 값을 사용
            val distance = calculateDistance(steps.toFloat(), 0.75f) // 보폭 0.75m 가정
            val calories = calculateCalories(steps.toFloat(), distance, 70f) // 체중 70kg 가정

            stepCountRepository.saveStepData(
                date = currentDate,
                steps = steps,
                distance = distance,
                calories = calories
            )

            Log.d(TAG, "Saved daily steps to repository: $steps")
        }
    }

    // 거리 계산 (km)
    private fun calculateDistance(steps: Float, strideLength: Float): Float {
        return steps * strideLength / 1000f
    }

    // 칼로리 계산 (kcal)
    private fun calculateCalories(steps: Float, distance: Float, weightKg: Float): Float {
        val walkingMet = 3.5f // 보통 걷기의 MET 값
        val timeHours = distance / 5.0f // 평균 5km/h 속도 가정
        return walkingMet * weightKg * timeHours
    }

    // 위치 권한 확인
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 위치 서비스 활성화 확인
    fun isLocationEnabled(): Boolean {
        return try {
            isGpsProviderEnabled() || isNetworkProviderEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location providers", e)
            false
        }
    }

    private fun isGpsProviderEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking GPS provider", e)
            false
        }
    }

    private fun isNetworkProviderEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Network provider", e)
            false
        }
    }

    // 위치 설정 로그
    private fun logLocationProviders() {
        Log.d(TAG, "Checking location settings:")
        Log.d(TAG, "Location Permission Granted: ${hasLocationPermission()}")
        Log.d(TAG, "GPS Provider Enabled: ${isGpsProviderEnabled()}")
        Log.d(TAG, "Network Provider Enabled: ${isNetworkProviderEnabled()}")

        val providers = locationManager.allProviders
        Log.d(TAG, "Available Location Providers:")
        providers.forEach { provider ->
            try {
                val isEnabled = locationManager.isProviderEnabled(provider)
                Log.d(TAG, "$provider - Enabled: $isEnabled")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking provider $provider", e)
            }
        }
    }

    // 마지막 알려진 위치 요청
    private fun requestLastKnownLocation() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission")
            return
        }

        try {
            val lastKnownLocationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownLocationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            currentLocation = when {
                lastKnownLocationGps != null && lastKnownLocationNetwork != null ->
                    if (lastKnownLocationGps.accuracy < lastKnownLocationNetwork.accuracy)
                        lastKnownLocationGps
                    else
                        lastKnownLocationNetwork
                lastKnownLocationGps != null -> lastKnownLocationGps
                lastKnownLocationNetwork != null -> lastKnownLocationNetwork
                else -> null
            }

            logLocationInfo(currentLocation)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting last known location", e)
        }
    }

    // 위치 정보 로그
    private fun logLocationInfo(location: Location?) {
        location?.let {
            Log.d(TAG, "Location Info:")
            Log.d(TAG, "Provider: ${it.provider}")
            Log.d(TAG, "Accuracy: ${it.accuracy}")
            Log.d(TAG, "Latitude: ${it.latitude}")
            Log.d(TAG, "Longitude: ${it.longitude}")
            Log.d(TAG, "Time: ${it.time}")
            Log.d(TAG, "Altitude: ${it.altitude}")
            Log.d(TAG, "Speed: ${it.speed}")
            Log.d(TAG, "Bearing: ${it.bearing}")
        } ?: Log.d(TAG, "No location available")
    }

    // 기록 시작
    override fun startRecording(mode: RecordingMode): Flow<RecordingSession> {
        logLocationProviders()
        clearReadings()

        recordingStartTime = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        sessionSteps = 0 // 세션 걸음 수 초기화

        val newSession = RecordingSession(
            id = sessionId,
            mode = mode,
            startTime = recordingStartTime
        )

        _currentSession.value = newSession
        requestLastKnownLocation()

        // 센서 리스너 등록
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // 걸음 감지 센서 등록 - 우선적으로 사용
        if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step detector sensor registered")
        } else if (stepCounter != null) {
            // 대체 수단으로 걸음 수 센서 사용
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step counter sensor registered (detector not available)")
        } else {
            Log.e(TAG, "No step detection sensors available on this device")
        }

        // 위치 업데이트 요청
        try {
            if (hasLocationPermission() && isLocationEnabled()) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1초마다 업데이트
                    1f,   // 1미터마다 업데이트
                    this
                )

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1f,
                    this
                )
            } else {
                Log.w(TAG, "Location permission not granted or location disabled")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in location updates", e)
        }

        _isRecording.value = true
        return _currentSession.asStateFlow() as Flow<RecordingSession>
    }

    // 위치 변경 감지
    override fun onLocationChanged(location: Location) {
        // 위치 정보 유효성 검사
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Log.w(TAG, "Invalid location coordinates")
            return
        }

        // 정확도가 낮은 위치 데이터 제외
        if (location.accuracy > 50) {
            Log.w(TAG, "Low accuracy location")
            return
        }

        // 오래된 위치 제외 (5분 이내)
        val currentTime = System.currentTimeMillis()
        if (currentTime - location.time > 5 * 60 * 1000) {
            Log.d(TAG, "Skipping outdated location")
            return
        }

        // 이전 위치보다 더 정확한 위치인 경우에만 업데이트
        currentLocation?.let {
            if (location.accuracy >= it.accuracy) {
                Log.d(TAG, "Skipping less accurate location")
                return
            }
        }

        currentLocation = location
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
    }

    // 기록 중지
    override suspend fun stopRecording(): RecordingSession {
        // 센서 리스너 해제
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)

        // 세션 종료 정보 업데이트
        val endTime = System.currentTimeMillis()
        val updatedSession = _currentSession.value?.copy(
            endTime = endTime,
            readings = sensorReadings.toList()
        ) ?: throw IllegalStateException("Recording session not found")

        _currentSession.value = updatedSession
        _isRecording.value = false

        Log.d(TAG, "Recording stopped. Session steps: $sessionSteps, Total steps: ${_totalDailySteps.value}")
        return updatedSession
    }

    // 센서 데이터 변경 감지
    override fun onSensorChanged(event: SensorEvent) {
        if (!_isRecording.value) return

        val currentTime = System.currentTimeMillis()
        val timeSeconds = (currentTime - recordingStartTime) / 1000f

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 가속도계 데이터 처리
                val reading = SensorReading(
                    timestamp = currentTime,
                    timeSeconds = timeSeconds,
                    accX = event.values[0],
                    accY = event.values[1],
                    accZ = event.values[2],
                    gyroX = 0f,
                    gyroY = 0f,
                    gyroZ = 0f,
                    magX = magneticValues?.get(0) ?: 0f,
                    magY = magneticValues?.get(1) ?: 0f,
                    magZ = magneticValues?.get(2) ?: 0f,
                    latitude = currentLocation?.latitude?.toFloat() ?: 0f,
                    longitude = currentLocation?.longitude?.toFloat() ?: 0f
                )
                sensorReadings.add(reading)
                _currentReading.value = reading
            }
            Sensor.TYPE_GYROSCOPE -> {
                // 자이로스코프 데이터 업데이트
                if (sensorReadings.isNotEmpty()) {
                    val lastIndex = sensorReadings.size - 1
                    val updatedReading = sensorReadings[lastIndex].copy(
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2]
                    )
                    sensorReadings[lastIndex] = updatedReading
                    _currentReading.value = updatedReading
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // 자기장 데이터 저장
                magneticValues = event.values.clone()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                // 걸음 감지 센서 - 각 걸음마다 이벤트 발생
                // 디바운싱 - 너무 빠른 걸음 감지는 무시
                if (currentTime - lastStepTimestamp >= MIN_STEP_INTERVAL) {
                    sessionSteps++
                    val newTotalSteps = _totalDailySteps.value + 1
                    _totalDailySteps.value = newTotalSteps

                    // 저장
                    updateAndSaveDailySteps(newTotalSteps)

                    // 걸음 감지 브로드캐스트 전송 (이 부분 추가)
                    broadcastStepDetected(newTotalSteps)

                    lastStepTimestamp = currentTime
                    Log.d(TAG, "Step detected. Session steps: $sessionSteps, Total steps: $newTotalSteps")
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                // 걸음 수 센서는 걸음 감지 센서가 없을 때 백업으로 사용
                // (이 코드는 일반적으로 실행되지 않지만, 걸음 감지 센서가 없는 기기를 위한 백업임)
                Log.d(TAG, "Step counter event received: ${event.values[0]}")
                // 여기서 필요한 처리 추가 (필요한 경우)
            }
        }
    }

    private fun broadcastStepDetected(stepCount: Int) {
        try {
            // 서비스와 연동하려면 상수값이 일치해야 함
            val intent1 = Intent("inu.appcenter.walkman.STEP_DETECTED")
            intent1.putExtra("step_count", stepCount)
            context.sendBroadcast(intent1)

            // 새 형식 (AppUsageTrackingService의 상수와 일치하게)
            val intent2 = Intent("inu.appcenter.walkman.STEP_UPDATED")
            intent2.putExtra("step_count", stepCount)
            context.sendBroadcast(intent2)

            Log.d(TAG, "걸음 감지 브로드캐스트 전송: ${stepCount}걸음")
        } catch (e: Exception) {
            Log.e(TAG, "걸음 감지 브로드캐스트 전송 실패", e)
        }
    }

    // 센서 정확도 변경
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor ${sensor?.name} accuracy changed to $accuracy")
    }

    // 위치 제공자 활성화
    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Provider enabled: $provider")
    }

    // 위치 제공자 비활성화
    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Provider disabled: $provider")
    }

    // 위치 제공자 상태 변경
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d(TAG, "Provider status changed: $provider, status: $status")
    }

    // 센서 데이터 저장
    override suspend fun saveReading(reading: SensorReading) {
        sensorReadings.add(reading)
        _currentReading.value = reading
    }

    // 센서 데이터 초기화
    override fun clearReadings() {
        sensorReadings.clear()
        _currentReading.value = null
        currentLocation = null
    }

    // 기록 취소
    override suspend fun cancelRecording() {
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        _isRecording.value = false
        clearReadings()
    }

    // 현재 걸음 수 가져오기
    fun getCurrentStepCount(): Int {
        return _totalDailySteps.value
    }

    // 걸음 수 관찰 Flow 얻기
    fun observeStepCount(): Flow<Int> {
        return _totalDailySteps.asStateFlow()
    }
}