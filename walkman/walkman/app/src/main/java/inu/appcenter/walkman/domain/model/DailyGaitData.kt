package inu.appcenter.walkman.domain.model

import java.util.Date

/**
 * 일별 보행 데이터
 */
data class DailyGaitData(
    val date: Date,
    val stabilityScore: Int,
    val rhythmScore: Int,
    val overallScore: Int
)
