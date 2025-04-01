package inu.appcenter.walkman.domain.model

import java.util.Date

/**
 * 보행 점수 데이터 모델
 */
data class GaitScoreData(
    val stabilityScore: Int = 0,
    val rhythmScore: Int = 0,
    val overallScore: Int = 0,
    val analysisDate: Date = Date(),
    val stabilityDetails: Map<String, Double> = emptyMap(),
    val rhythmDetails: Map<String, Double> = emptyMap(),
    val recordingMode: RecordingMode? = null,
    val sessionId: String = ""
)