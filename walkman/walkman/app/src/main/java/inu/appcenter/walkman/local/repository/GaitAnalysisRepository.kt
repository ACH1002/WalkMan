package inu.appcenter.walkman.local.repository

import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import kotlinx.coroutines.flow.Flow
import java.util.Date


/**
 * 보행 분석 Repository 인터페이스
 */
interface GaitAnalysisRepository {
    /**
     * 녹화 세션 보행 분석
     */
    suspend fun analyzeGaitData(session: RecordingSession): GaitScoreData?

    /**
     * 보행 분석 결과 저장
     */
    suspend fun saveGaitAnalysis(gaitScoreData: GaitScoreData): Boolean

    /**
     * 가장 최근 보행 분석 결과 가져오기
     */
    suspend fun getLatestGaitAnalysis(): GaitScoreData?

    /**
     * 특정 모드의 가장 최근 세션 가져오기
     */
    suspend fun getLatestSessionByMode(mode: RecordingMode): RecordingSession?

    /**
     * 특정 세션의 보행 분석 결과 가져오기
     */
    suspend fun getGaitAnalysisBySessionId(sessionId: String): GaitScoreData?

    /**
     * 특정 기간의 보행 분석 결과들 가져오기
     */
    suspend fun getGaitAnalysisFromDate(fromDate: Date): List<GaitScoreData>

    /**
     * 모든 보행 분석 결과들을 Flow로 가져오기
     */
    fun getAllGaitAnalysisFlow(): Flow<List<GaitScoreData>>

    /**
     * 특정 녹화 세션 저장하기
     */
    suspend fun saveRecordingSession(session: RecordingSession): Boolean
}