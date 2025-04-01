package inu.appcenter.walkman.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 세션과 센서 데이터 관계를 위한 데이터 클래스
 */
data class SessionWithReadings(
    @Embedded val session: RecordingSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val readings: List<SensorReadingEntity>
)
