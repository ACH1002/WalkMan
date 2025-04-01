package inu.appcenter.walkman.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import inu.appcenter.walkman.local.entity.RecordingSessionEntity
import inu.appcenter.walkman.local.entity.SessionWithReadings
import kotlinx.coroutines.flow.Flow

/**
 * 녹화 세션 DAO
 */
@Dao
interface RecordingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSessionEntity)

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): RecordingSessionEntity?

    @Query("SELECT * FROM recording_sessions ORDER BY start_time DESC")
    fun getAllSessionsFlow(): Flow<List<RecordingSessionEntity>>

    @Query("SELECT * FROM recording_sessions WHERE mode = :mode ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestSessionByMode(mode: String): RecordingSessionEntity?

    @Transaction
    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getSessionWithReadings(sessionId: String): SessionWithReadings?

    @Transaction
    @Query("SELECT * FROM recording_sessions WHERE mode = :mode ORDER BY start_time DESC LIMIT 1")
    suspend fun getLatestSessionWithReadingsByMode(mode: String): SessionWithReadings?

    @Transaction
    @Query("SELECT * FROM recording_sessions WHERE mode = :mode ORDER BY start_time DESC")
    fun getAllSessionsWithReadingsByModeFlow(mode: String): Flow<List<SessionWithReadings>>
}