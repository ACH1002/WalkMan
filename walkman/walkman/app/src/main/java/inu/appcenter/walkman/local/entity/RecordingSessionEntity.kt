package inu.appcenter.walkman.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * 녹화 세션 테이블
 */
@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "mode") val mode: String, // Enum은 String으로 변환
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long?
)
