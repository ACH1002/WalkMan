package inu.appcenter.walkman.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 센서 읽기 데이터 테이블
 */
@Entity(
    tableName = "sensor_readings",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "time_seconds") val timeSeconds: Float,
    @ColumnInfo(name = "acc_x") val accX: Float,
    @ColumnInfo(name = "acc_y") val accY: Float,
    @ColumnInfo(name = "acc_z") val accZ: Float,
    @ColumnInfo(name = "gyro_x") val gyroX: Float,
    @ColumnInfo(name = "gyro_y") val gyroY: Float,
    @ColumnInfo(name = "gyro_z") val gyroZ: Float,
    @ColumnInfo(name = "mag_x") val magX: Float = 0f,
    @ColumnInfo(name = "mag_y") val magY: Float = 0f,
    @ColumnInfo(name = "mag_z") val magZ: Float = 0f,
    @ColumnInfo(name = "latitude") val latitude: Float = 0f,
    @ColumnInfo(name = "longitude") val longitude: Float = 0f
)
