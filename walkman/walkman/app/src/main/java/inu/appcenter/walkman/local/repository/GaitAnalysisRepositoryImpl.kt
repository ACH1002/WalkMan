package inu.appcenter.walkman.local.repository

import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.local.database.WalkManDatabase
import inu.appcenter.walkman.local.utils.toDomainModel
import inu.appcenter.walkman.local.utils.toEntity
import inu.appcenter.walkman.util.GaitAnalysisUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GaitAnalysisRepositoryImpl @Inject constructor(
    private val database: WalkManDatabase
) : GaitAnalysisRepository {

    private val recordingSessionDao = database.recordingSessionDao()
    private val sensorReadingDao = database.sensorReadingDao()
    private val gaitAnalysisDao = database.gaitAnalysisDao()

    /**
     * 녹화 세션 보행 분석
     */
    override suspend fun analyzeGaitData(session: RecordingSession): GaitScoreData? = withContext(Dispatchers.Default) {
        if (session.readings.isEmpty()) {
            return@withContext null
        }

        try {
            // 센서 데이터를 분석에 적합한 형태로 변환
            val accelerationData = GaitAnalysisUtils.convertSensorReadingsToAccelerationData(session.readings)

            // 보행 안정성 계산
            val stabilityResults = GaitAnalysisUtils.calculateGaitStability(accelerationData)
            val stabilityScore = stabilityResults["stability_score"]?.toInt() ?: 0

            // 보행 리듬성 계산 (샘플링 레이트는 기기에 따라 조정 필요)
            val samplingRate = 100.0 // 기본값 100Hz
            val rhythmResults = GaitAnalysisUtils.calculateGaitRhythm(accelerationData, samplingRate)
            val rhythmScore = rhythmResults["rhythm_score"]?.toInt() ?: 0

            // 종합 점수 계산
            val overallScore = GaitAnalysisUtils.calculateOverallGaitScore(stabilityScore.toDouble(), rhythmScore.toDouble())

            // 결과 생성
            val gaitScore = GaitScoreData(
                stabilityScore = stabilityScore,
                rhythmScore = rhythmScore,
                overallScore = overallScore,
                analysisDate = Date(),
                stabilityDetails = stabilityResults,
                rhythmDetails = rhythmResults,
                recordingMode = session.mode,
                sessionId = session.id
            )

            // 분석 결과 저장
            saveGaitAnalysis(gaitScore)

            return@withContext gaitScore
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * 보행 분석 결과 저장
     */
    override suspend fun saveGaitAnalysis(gaitScoreData: GaitScoreData): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = gaitScoreData.toEntity()
            gaitAnalysisDao.insertGaitAnalysis(entity)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 가장 최근 보행 분석 결과 가져오기
     */
    override suspend fun getLatestGaitAnalysis(): GaitScoreData? = withContext(Dispatchers.IO) {
        try {
            val gaitAnalysisEntity = gaitAnalysisDao.getLatestGaitAnalysis() ?: return@withContext null
            return@withContext gaitAnalysisEntity.toDomainModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * 특정 모드의 가장 최근 세션 가져오기
     */
    override suspend fun getLatestSessionByMode(mode: RecordingMode): RecordingSession? = withContext(Dispatchers.IO) {
        try {
            val sessionWithReadings = recordingSessionDao.getLatestSessionWithReadingsByMode(mode.name)
            return@withContext sessionWithReadings?.toDomainModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * 특정 세션의 보행 분석 결과 가져오기
     */
    override suspend fun getGaitAnalysisBySessionId(sessionId: String): GaitScoreData? = withContext(Dispatchers.IO) {
        try {
            val gaitAnalysisEntity = gaitAnalysisDao.getGaitAnalysisBySessionId(sessionId) ?: return@withContext null
            return@withContext gaitAnalysisEntity.toDomainModel()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * 특정 기간의 보행 분석 결과들 가져오기
     */
    override suspend fun getGaitAnalysisFromDate(fromDate: Date): List<GaitScoreData> = withContext(Dispatchers.IO) {
        try {
            val fromTimeMillis = fromDate.time
            val gaitAnalysisEntities = gaitAnalysisDao.getGaitAnalysisFromDate(fromTimeMillis)
            return@withContext gaitAnalysisEntities.map { it.toDomainModel() }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * 모든 보행 분석 결과들을 Flow로 가져오기
     */
    override fun getAllGaitAnalysisFlow(): Flow<List<GaitScoreData>> {
        return gaitAnalysisDao.getAllGaitAnalysisFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * 특정 녹화 세션 저장하기
     */
    override suspend fun saveRecordingSession(session: RecordingSession): Boolean = withContext(Dispatchers.IO) {
        try {
            // 세션 엔티티 저장
            val sessionEntity = session.toEntity()
            recordingSessionDao.insertSession(sessionEntity)

            // 센서 데이터 엔티티 저장
            val readingEntities = session.readings.map { it.toEntity(session.id) }
            sensorReadingDao.insertReadings(readingEntities)

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}