package inu.appcenter.walkman.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class UserProfileEntity(
    val id: String? = null,
    val account_id: String? = null,
    val name: String,
    val gender: String,
    val height: String,
    val weight: String,
    val mbti: String? = "",
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class GaitAnalysisEntity(
    val id: String? = null,
    val user_profile_id: String,
    val stability_score: Int,
    val rhythm_score: Int,
    val overall_score: Int,
    val analysis_date: String? = null,
    val stability_details: String = "{}",
    val rhythm_details: String = "{}",
    val recording_mode: String? = null,
    val session_id: String? = null,
    val created_at: String? = null
)

// Domain models
data class UserProfile(
    val id: String? = null,
    val accountId: String? = null,
    val name: String,
    val gender: String,
    val height: String,
    val weight: String,
    val mbti: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class GaitAnalysis(
    val id: String? = null,
    val userProfileId: String,
    val stabilityScore: Int,
    val rhythmScore: Int,
    val overallScore: Int,
    val analysisDate: Date? = Date(),
    val stabilityDetails: Map<String, Double> = emptyMap(),
    val rhythmDetails: Map<String, Double> = emptyMap(),
    val recordingMode: String? = null,
    val sessionId: String? = null
)

// Extension functions for conversion
fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        accountId = account_id,
        name = name,
        gender = gender,
        height = height,
        weight = weight,
        mbti = mbti ?: "",
        // Convert string dates to Date objects if needed
        createdAt = created_at?.let { DateConverter.fromIsoString(it) },
        updatedAt = updated_at?.let { DateConverter.fromIsoString(it) }
    )
}

fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        account_id = accountId,
        name = name,
        gender = gender,
        height = height,
        weight = weight,
        mbti = mbti
    )
}

// 변환 함수들
fun GaitAnalysis.toEntity(): GaitAnalysisEntity {
    val gson = Gson()
    return GaitAnalysisEntity(
        id = id,
        user_profile_id = userProfileId,
        stability_score = stabilityScore,
        rhythm_score = rhythmScore,
        overall_score = overallScore,
        stability_details = gson.toJson(stabilityDetails),  // Gson 사용
        rhythm_details = gson.toJson(rhythmDetails),        // Gson 사용
        recording_mode = recordingMode,
        session_id = sessionId
    )
}

fun GaitAnalysisEntity.toDomain(): GaitAnalysis {
    val gson = Gson()
    val stabilityType = object : TypeToken<Map<String, Double>>() {}.type
    val rhythmType = object : TypeToken<Map<String, Double>>() {}.type

    return GaitAnalysis(
        id = id,
        userProfileId = user_profile_id,
        stabilityScore = stability_score,
        rhythmScore = rhythm_score,
        overallScore = overall_score,
        analysisDate = analysis_date?.let { DateConverter.fromIsoString(it) } ?: Date(),
        stabilityDetails = try {
            gson.fromJson(stability_details, stabilityType)
        } catch (e: Exception) {
            emptyMap()
        },
        rhythmDetails = try {
            gson.fromJson(rhythm_details, rhythmType)
        } catch (e: Exception) {
            emptyMap()
        },
        recordingMode = recording_mode,
        sessionId = session_id
    )
}

// Helper for date conversion
object DateConverter {
    fun fromIsoString(isoString: String): Date {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(isoString)
        } catch (e: Exception) {
            Date()
        }
    }

    fun toIsoString(date: Date): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(date)
    }
}

