package inu.appcenter.walkman.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import inu.appcenter.walkman.local.entity.GaitAnalysisEntity
import inu.appcenter.walkman.local.entity.SessionWithGaitAnalysis
import kotlinx.coroutines.flow.Flow

/**
 * 보행 분석 DAO
 */
@Dao
interface GaitAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGaitAnalysis(gaitAnalysis: GaitAnalysisEntity): Long

    @Query("SELECT * FROM gait_analysis WHERE session_id = :sessionId")
    suspend fun getGaitAnalysisBySessionId(sessionId: String): GaitAnalysisEntity?

    @Query("SELECT * FROM gait_analysis ORDER BY analysis_date DESC LIMIT 1")
    suspend fun getLatestGaitAnalysis(): GaitAnalysisEntity?

    @Query("SELECT * FROM gait_analysis ORDER BY analysis_date DESC")
    fun getAllGaitAnalysisFlow(): Flow<List<GaitAnalysisEntity>>

    @Query("SELECT * FROM gait_analysis WHERE analysis_date >= :fromDate ORDER BY analysis_date ASC")
    suspend fun getGaitAnalysisFromDate(fromDate: Long): List<GaitAnalysisEntity>

    @Transaction
    @Query("SELECT * FROM recording_sessions WHERE id IN (SELECT session_id FROM gait_analysis)")
    fun getSessionsWithGaitAnalysisFlow(): Flow<List<SessionWithGaitAnalysis>>
}