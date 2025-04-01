package inu.appcenter.walkman.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 보행 분석 결과 테이블
 */
@Entity(
    tableName = "gait_analysis",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GaitAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "stability_score") val stabilityScore: Int,
    @ColumnInfo(name = "rhythm_score") val rhythmScore: Int,
    @ColumnInfo(name = "overall_score") val overallScore: Int,
    @ColumnInfo(name = "analysis_date") val analysisDate: Long, // Date를 Long으로 저장
    @ColumnInfo(name = "stability_details") val stabilityDetails: String, // Map을 JSON으로 변환
    @ColumnInfo(name = "rhythm_details") val rhythmDetails: String // Map을 JSON으로 변환
)
