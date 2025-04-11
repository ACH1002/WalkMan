package inu.appcenter.walkman.data.repository.gaitanalysis

import android.util.Log
import inu.appcenter.walkman.data.model.GaitAnalysis
import inu.appcenter.walkman.data.model.GaitAnalysisEntity
import inu.appcenter.walkman.data.model.toDomain
import inu.appcenter.walkman.data.model.toEntity
import inu.appcenter.walkman.data.remote.SupabaseClient
import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.util.GaitAnalysisUtils
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseGaitAnalysisRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : SupabaseGaitAnalysisRepository {

    private val TAG = "GaitAnalysisRepository"

    override suspend fun getGaitAnalysisForProfile(profileId: String): List<GaitAnalysis> = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.supabase
                .from("gait_analysis")
                .select() {
                    filter {
                        eq("user_profile_id", profileId)
                    }
                    order("analysis_date", Order.DESCENDING)
                }
                .decodeList<GaitAnalysisEntity>()

            result.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching gait analysis for profile", e)
            emptyList()
        }
    }

    override suspend fun getLatestGaitAnalysisForProfile(profileId: String): GaitAnalysis? = withContext(Dispatchers.IO) {
        try {
            val result = supabaseClient.supabase
                .from("gait_analysis")
                .select() {
                    filter {
                        eq("user_profile_id", profileId)
                    }
                    order("analysis_date", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<GaitAnalysisEntity>()

            if (result.isNotEmpty()) result.first().toDomain() else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest gait analysis", e)
            null
        }
    }


    override suspend fun saveGaitAnalysis(analysis: GaitAnalysis): GaitAnalysis? = withContext(Dispatchers.IO) {
        try {
            val entity = analysis.toEntity()

            val result = if (analysis.id == null) {
                // Create new record
                supabaseClient.supabase
                    .from("gait_analysis")
                    .insert(entity)
                    .decodeSingle<GaitAnalysisEntity>()
            } else {
                // Update existing record
                supabaseClient.supabase
                    .from("gait_analysis")
                    .update(entity) {
                        filter {
                            eq("id", analysis.id)
                        }
                    }
                    .decodeSingle<GaitAnalysisEntity>()
            }

            result.toDomain()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving gait analysis", e)
            null
        }
    }

    override suspend fun analyzeAndSaveGaitData(session: RecordingSession, userProfileId: String): GaitAnalysis? = withContext(Dispatchers.IO) {
        try {
            if (session.readings.isEmpty()) {
                return@withContext null
            }

            // Convert sensor readings to acceleration data format
            val accelerationData = GaitAnalysisUtils.convertSensorReadingsToAccelerationData(session.readings)

            // Calculate gait stability
            val stabilityResults = GaitAnalysisUtils.calculateGaitStability(accelerationData)
            val stabilityScore = stabilityResults["stability_score"]?.toInt() ?: 0

            // Calculate gait rhythm (sampling rate may need adjustment based on device)
            val samplingRate = 100.0 // Default 100Hz
            val rhythmResults = GaitAnalysisUtils.calculateGaitRhythm(accelerationData, samplingRate)
            val rhythmScore = rhythmResults["rhythm_score"]?.toInt() ?: 0

            // Calculate overall score
            val overallScore = GaitAnalysisUtils.calculateOverallGaitScore(
                stabilityScore.toDouble(),
                rhythmScore.toDouble()
            )

            // Create gait analysis object
            val gaitAnalysis = GaitAnalysis(
                userProfileId = userProfileId,
                stabilityScore = stabilityScore,
                rhythmScore = rhythmScore,
                overallScore = overallScore,
                analysisDate = Date(),
                stabilityDetails = stabilityResults,
                rhythmDetails = rhythmResults,
                recordingMode = session.mode.name,
                sessionId = session.id
            )

            // Save to Supabase
            saveGaitAnalysis(gaitAnalysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing and saving gait data", e)
            null
        }
    }

    override suspend fun deleteGaitAnalysis(analysisId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            supabaseClient.supabase
                .from("gait_analysis")
                .delete {
                    filter {
                        eq("id", analysisId)
                    }
                }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting gait analysis", e)
            false
        }
    }

    // Helper function to convert from app's domain model to repository model
    private fun GaitScoreData.toGaitAnalysis(userProfileId: String): GaitAnalysis {
        return GaitAnalysis(
            userProfileId = userProfileId,
            stabilityScore = stabilityScore,
            rhythmScore = rhythmScore,
            overallScore = overallScore,
            analysisDate = analysisDate,
            stabilityDetails = stabilityDetails,
            rhythmDetails = rhythmDetails,
            recordingMode = recordingMode?.name,
            sessionId = sessionId
        )
    }
}