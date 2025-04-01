package inu.appcenter.walkman.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 센서 저장소 구현체 - 걷기 감지와 센서 데이터 수집 기능 모두 제공
 */
@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val context: Context
) : SensorRepository, SensorEventListener, LocationListener {

    private val TAG = "SensorRepositoryImpl"

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 센서 정의
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // 걷기 상태 관련
    private val _isWalking = MutableStateFlow(false)
    private var lastStepTimestamp = 0L
    private val MIN_STEP_INTERVAL = 250L // 최소 걸음 간격 (밀리초)
    private val WALKING_TIMEOUT = 10000L // 걷기 중단으로 판단하는 시간(ms)
    private val ACCELERATION_THRESHOLD = 11.0f // 걷기 감지를 위한 가속도 임계값

    // 센서 데이터 수집 관련
    private val _isRecording = MutableStateFlow(false)
    private val _currentReading = MutableStateFlow<SensorReading?>(null)
    private val _currentSession = MutableStateFlow<RecordingSession?>(null)
    private val sensorReadings = mutableListOf<SensorReading>()
    private var recordingStartTime = 0L

    // 위치, 자기장 데이터
    private var currentLocation: Location? = null
    private var magneticValues: FloatArray? = null

    // 걷기 상태 Flow
    override fun isWalking(): Flow<Boolean> = _isWalking.asStateFlow()

    // 기록 중 상태 Flow
    override fun isRecording(): Flow<Boolean> = _isRecording.asStateFlow()

    // 현재 센서 데이터 Flow
    override fun getCurrentReadings(): Flow<SensorReading> =
        _currentReading.asStateFlow() as Flow<SensorReading>

    /**
     * 걷기 감지 시작
     */
    override fun startWalkingDetection() {
        // 걸음 감지 센서 등록 (있는 경우)
        if (stepDetector != null) {
            sensorManager.registerListener(
                this,
                stepDetector,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "걸음 감지 센서 등록 완료")
        }

        // 가속도계 센서 등록 (걸음 센서가 없거나 백업용)
        if (accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "가속도계 센서 등록 완료")
        }
    }

    /**
     * 걷기 감지 종료
     */
    override fun stopWalkingDetection() {
        if (!_isRecording.value) {
            // 데이터 수집 중이 아닌 경우에만 센서 리스너 해제
            sensorManager.unregisterListener(this)
        }
        _isWalking.value = false
        Log.d(TAG, "걷기 감지 종료")
    }

    /**
     * 센서 데이터 수집 시작
     */
    override fun startRecording(mode: RecordingMode): Flow<RecordingSession> {
        clearReadings()

        recordingStartTime = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()

        val newSession = RecordingSession(
            id = sessionId,
            mode = mode,
            startTime = recordingStartTime
        )

        _currentSession.value = newSession
        requestLastKnownLocation()

        // 센서 리스너 등록
        registerSensors()

        // 위치 업데이트 요청
        requestLocationUpdates()

        _isRecording.value = true
        return _currentSession.asStateFlow() as Flow<RecordingSession>
    }

    /**
     * 센서 리스너 등록
     */
    private fun registerSensors() {
        // 가속도계
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // 자이로스코프
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // 자기장 센서
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // 걸음 감지 센서
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /**
     * 마지막 알려진 위치 요청
     */
    private fun requestLastKnownLocation() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "위치 권한 없음")
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
        } catch (e: SecurityException) {
            Log.e(TAG, "위치 정보 접근 권한 오류", e)
        }
    }

    /**
     * 위치 업데이트 요청
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "위치 업데이트 요청 중 오류", e)
        }
    }

    /**
     * 위치 권한 확인
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 위치 서비스 활성화 확인
     */
    private fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e(TAG, "위치 서비스 확인 중 오류", e)
            false
        }
    }

    /**
     * 센서 데이터 수집 중지
     */
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

        Log.d(TAG, "Recording stopped with ${sensorReadings.size} readings")
        return updatedSession
    }

    /**
     * 센서 데이터 저장
     */
    override suspend fun saveReading(reading: SensorReading) {
        sensorReadings.add(reading)
        _currentReading.value = reading
    }

    /**
     * 센서 데이터 초기화
     */
    override fun clearReadings() {
        sensorReadings.clear()
        _currentReading.value = null
        currentLocation = null
    }

    /**
     * 센서 데이터 수집 취소
     */
    override suspend fun cancelRecording() {
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        _isRecording.value = false
        clearReadings()
    }

    /**
     * 센서 데이터 변경 이벤트
     */
    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 가속도계 데이터 처리
                processAccelerometerData(event, currentTime)

                // 걷기 중단 감지 (일정 시간 동안 걸음이 없으면)
                checkWalkingTimeout(currentTime)
            }
            Sensor.TYPE_GYROSCOPE -> {
                // 자이로스코프 데이터 처리
                if (_isRecording.value) {
                    processGyroscopeData(event)
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // 자기장 데이터 저장
                magneticValues = event.values.clone()
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                // 걸음 감지 센서 - 각 걸음마다 이벤트 발생
                if (currentTime - lastStepTimestamp > MIN_STEP_INTERVAL) {
                    processStepDetection(currentTime)
                }
            }
        }
    }

    /**
     * 가속도계 데이터 처리
     */
    private fun processAccelerometerData(event: SensorEvent, currentTime: Long) {
        // 녹화 중이면 센서 데이터 저장
        if (_isRecording.value) {
            val timeSeconds = (currentTime - recordingStartTime) / 1000f

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

        // 가속도 기반 걷기 감지 (걸음 감지 센서가 없는 경우 백업)
        if (stepDetector == null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // 가속도 벡터의 크기 계산
            val magnitude = Math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()

            // 걷기 패턴 감지
            if (magnitude > ACCELERATION_THRESHOLD &&
                currentTime - lastStepTimestamp > MIN_STEP_INTERVAL) {
                processStepDetection(currentTime)
            }
        }
    }

    /**
     * 자이로스코프 데이터 처리
     */
    private fun processGyroscopeData(event: SensorEvent) {
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

    /**
     * 걸음 감지 처리
     */
    private fun processStepDetection(currentTime: Long) {
        val wasWalking = _isWalking.value
        _isWalking.value = true
        lastStepTimestamp = currentTime

        if (!wasWalking) {
            Log.d(TAG, "걷기 시작 감지됨")
        }
    }

    /**
     * 걷기 중단 감지
     */
    private fun checkWalkingTimeout(currentTime: Long) {
        if (_isWalking.value && currentTime - lastStepTimestamp > WALKING_TIMEOUT) {
            _isWalking.value = false
            Log.d(TAG, "걷기 중단 감지: ${currentTime - lastStepTimestamp}ms 동안 걸음 없음")
        }
    }

    /**
     * 위치 변경 이벤트
     */
    override fun onLocationChanged(location: Location) {
        // 위치 정보 유효성 검증
        if (location.latitude == 0.0 && location.longitude == 0.0) return
        if (location.accuracy > 50) return

        // 이전 위치보다 더 정확한 위치인 경우에만 업데이트
        currentLocation?.let {
            if (location.accuracy >= it.accuracy) return
        }

        currentLocation = location
    }

    /**
     * 센서 정확도 변경 이벤트
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 필요한 경우에만 처리
    }

    // 위치 제공자 상태 변경 관련 메서드들
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}