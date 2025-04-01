package inu.appcenter.walkman.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import inu.appcenter.walkman.local.entity.SensorReadingEntity

/**
 * 센서 읽기 데이터 DAO
 */
@Dao
interface SensorReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SensorReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<SensorReadingEntity>)

    @Query("SELECT * FROM sensor_readings WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getReadingsBySessionId(sessionId: String): List<SensorReadingEntity>

    @Query("DELETE FROM sensor_readings WHERE session_id = :sessionId")
    suspend fun deleteReadingsBySessionId(sessionId: String)
}