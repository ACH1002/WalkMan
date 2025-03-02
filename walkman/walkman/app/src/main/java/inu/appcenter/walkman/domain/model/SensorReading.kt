package inu.appcenter.walkman.domain.model

data class SensorReading(
    val timestamp: Long,
    val timeSeconds: Float,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val latitude: Float = 0f,
    val longitude: Float = 0f
)
