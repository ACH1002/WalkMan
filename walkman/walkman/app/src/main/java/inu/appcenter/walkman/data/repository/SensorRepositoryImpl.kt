package inu.appcenter.walkman.data.repository

import android.Manifest
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val context: Context
) : SensorRepository, SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _isRecording = MutableStateFlow(false)
    override fun isRecording(): Flow<Boolean> = _isRecording.asStateFlow()

    private val _currentReading = MutableStateFlow<SensorReading?>(null)
    override fun getCurrentReadings(): Flow<SensorReading> = _currentReading.asStateFlow() as Flow<SensorReading>

    private val _currentSession = MutableStateFlow<RecordingSession?>(null)

    private val sensorReadings = mutableListOf<SensorReading>()
    private var recordingStartTime = 0L

    private var currentLocation: Location? = null
    private var magneticValues: FloatArray? = null

    // Location Permission and Service Checks
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        return try {
            isGpsProviderEnabled() || isNetworkProviderEnabled()
        } catch (e: Exception) {
            Log.e("LocationTracking", "Error checking location providers", e)
            false
        }
    }

    private fun isGpsProviderEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            Log.e("LocationTracking", "Error checking GPS provider", e)
            false
        }
    }

    private fun isNetworkProviderEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.e("LocationTracking", "Error checking Network provider", e)
            false
        }
    }

    // Comprehensive Location Providers Logging
    private fun logLocationProviders() {
        Log.d("LocationTracking", "Checking location settings:")
        Log.d("LocationTracking", "Location Permission Granted: ${hasLocationPermission()}")
        Log.d("LocationTracking", "GPS Provider Enabled: ${isGpsProviderEnabled()}")
        Log.d("LocationTracking", "Network Provider Enabled: ${isNetworkProviderEnabled()}")

        val providers = locationManager.allProviders
        Log.d("LocationTracking", "Available Location Providers:")
        providers.forEach { provider ->
            try {
                val isEnabled = locationManager.isProviderEnabled(provider)
                Log.d("LocationTracking", "$provider - Enabled: $isEnabled")
            } catch (e: Exception) {
                Log.e("LocationTracking", "Error checking provider $provider", e)
            }
        }
    }

    // Request Last Known Location
    private fun requestLastKnownLocation() {
        if (!hasLocationPermission()) {
            Log.w("LocationTracking", "No location permission")
            return
        }

        try {
            // 마지막으로 알려진 위치 가져오기
            val lastKnownLocationGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownLocationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // 더 정확한 위치 선택
            currentLocation = when {
                lastKnownLocationGps != null && lastKnownLocationNetwork != null ->
                    if (lastKnownLocationGps.accuracy > lastKnownLocationNetwork.accuracy)
                        lastKnownLocationGps
                    else
                        lastKnownLocationNetwork
                lastKnownLocationGps != null -> lastKnownLocationGps
                lastKnownLocationNetwork != null -> lastKnownLocationNetwork
                else -> null
            }

            // 초기 위치 로깅
            logLocationInfo(currentLocation)
        } catch (e: SecurityException) {
            Log.e("LocationTracking", "Security exception when getting last known location", e)
        }
    }

    // Detailed Location Info Logging
    private fun logLocationInfo(location: Location?) {
        location?.let {
            Log.d("LocationTracking", "Location Info:")
            Log.d("LocationTracking", "Provider: ${it.provider}")
            Log.d("LocationTracking", "Accuracy: ${it.accuracy}")
            Log.d("LocationTracking", "Latitude: ${it.latitude}")
            Log.d("LocationTracking", "Longitude: ${it.longitude}")
            Log.d("LocationTracking", "Time: ${it.time}")
            Log.d("LocationTracking", "Altitude: ${it.altitude}")
            Log.d("LocationTracking", "Speed: ${it.speed}")
            Log.d("LocationTracking", "Bearing: ${it.bearing}")
        } ?: Log.d("LocationTracking", "No location available")
    }

    override fun startRecording(mode: RecordingMode): Flow<RecordingSession> {
        // 위치 설정 확인 및 로깅
        logLocationProviders()

        // 기존 데이터 초기화
        clearReadings()

        recordingStartTime = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()

        val newSession = RecordingSession(
            id = sessionId,
            mode = mode,
            startTime = recordingStartTime
        )

        _currentSession.value = newSession

        // 마지막으로 알려진 위치 요청
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

        // 위치 리스너 등록 (다중 제공자)
        try {
            if (hasLocationPermission() && isLocationEnabled()) {
                // GPS 제공자
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1초마다 업데이트
                    1f,   // 1미터마다 업데이트
                    this
                )

                // 네트워크 제공자 추가
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    1f,
                    this
                )
            } else {
                Log.w("LocationTracking", "Location permission not granted or location disabled")
            }
        } catch (e: SecurityException) {
            Log.e("LocationTracking", "Security exception when requesting location updates", e)
        } catch (e: Exception) {
            Log.e("LocationTracking", "Unexpected error in location updates", e)
        }

        _isRecording.value = true

        return _currentSession.asStateFlow() as Flow<RecordingSession>
    }

    override fun onLocationChanged(location: Location) {
        // 로그를 통해 실제로 위치 정보가 들어오는지 확인
        Log.d("LocationTracking", "Location Update Received:")
        Log.d("LocationTracking", "Provider: ${location.provider}")
        Log.d("LocationTracking", "Latitude: ${location.latitude}")
        Log.d("LocationTracking", "Longitude: ${location.longitude}")
        Log.d("LocationTracking", "Accuracy: ${location.accuracy}")
        Log.d("LocationTracking", "Time: ${location.time}")

        // 위치 정보 유효성 검사
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Log.w("LocationTracking", "Invalid location coordinates")
            return
        }

        // 정확도가 낮은 위치 데이터 제외
        if (location.accuracy > 50) {
            Log.w("LocationTracking", "Low accuracy location")
            return
        }

        // 오래된 위치 제외 (5분 이내)
        val currentTime = System.currentTimeMillis()
        if (currentTime - location.time > 5 * 60 * 1000) {
            Log.d("LocationTracking", "Skipping outdated location")
            return
        }

        // 이전 위치보다 더 정확한 위치인 경우에만 업데이트
        currentLocation?.let {
            if (location.accuracy >= it.accuracy) {
                Log.d("LocationTracking", "Skipping less accurate location")
                return
            }
        }

        // 위치 업데이트
        currentLocation = location
    }

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

        return updatedSession
    }

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
                // 자이로스코프 데이터가 들어왔을 때 마지막 센서값 업데이트
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
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 정확도 변화 로깅
        Log.d("SensorTracking", "Sensor ${sensor?.name} accuracy changed to $accuracy")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("LocationTracking", "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d("LocationTracking", "Provider disabled: $provider")
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d("LocationTracking", "Provider status changed: $provider, status: $status")
    }

    override suspend fun saveReading(reading: SensorReading) {
        sensorReadings.add(reading)
        _currentReading.value = reading
    }

    override fun clearReadings() {
        sensorReadings.clear()
        _currentReading.value = null
        currentLocation = null
    }

    override suspend fun cancelRecording() {
        // 센서 리스너 해제 (stopRecording과 동일)
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)

        // 세션 정보 업데이트하지 않고 상태만 변경
        _isRecording.value = false

        // 수집된 데이터 버리기
        clearReadings()
    }
}