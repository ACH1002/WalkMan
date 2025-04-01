package inu.appcenter.walkman.local.entity

import androidx.room.Embedded
import androidx.room.Relation


/**
 * 세션과 보행 분석 결과 관계를 위한 데이터 클래스
 */
data class SessionWithGaitAnalysis(
    @Embedded val session: RecordingSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val gaitAnalysis: GaitAnalysisEntity?
)
