package inu.appcenter.walkman.data.repository.gaitanalysis

import inu.appcenter.walkman.data.model.GaitAnalysis
import inu.appcenter.walkman.domain.model.RecordingSession

interface SupabaseGaitAnalysisRepository {
    // Get all gait analysis records for a specific user profile
    suspend fun getGaitAnalysisForProfile(profileId: String): List<GaitAnalysis>

    // Get the latest gait analysis for a user profile
    suspend fun getLatestGaitAnalysisForProfile(profileId: String): GaitAnalysis?

    // Save gait analysis data
    suspend fun saveGaitAnalysis(analysis: GaitAnalysis): GaitAnalysis?

    // Analyze recording session and save the result
    suspend fun analyzeAndSaveGaitData(session: RecordingSession, userProfileId: String): GaitAnalysis?

    // Delete gait analysis record
    suspend fun deleteGaitAnalysis(analysisId: String): Boolean
}