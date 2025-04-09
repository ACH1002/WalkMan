import java.util.Date

/**
 * Supabase에 저장할 보행 점수 데이터 모델
 */
data class GaitScoreDto(
    val id: String? = null,              // Supabase에서 생성된 UUID
    val profileId: String,               // 사용자 프로필 식별자 (UserInfo의 고유 식별자)
    val stabilityScore: Int,             // 안정성 점수
    val rhythmScore: Int,                // 리듬성 점수
    val overallScore: Int,               // 종합 점수
    val recordingMode: String,           // 측정 모드 (VIDEO, POCKET, TEXT 등)
    val analysisDate: Long,              // 분석 날짜 (타임스탬프)
    val stabilityDetails: String,        // 안정성 세부 정보 (JSON 문자열)
    val rhythmDetails: String,           // 리듬성 세부 정보 (JSON 문자열)
    val deviceId: String                 // 기기 식별자
)

/**
 * 로컬 GaitScoreData 모델을 Supabase DTO로 변환하는 확장 함수
 */
fun inu.appcenter.walkman.domain.model.GaitScoreData.toDto(profileId: String, deviceId: String): GaitScoreDto {
    return GaitScoreDto(
        id = null,  // Supabase에서 생성될 ID
        profileId = profileId,
        stabilityScore = stabilityScore,
        rhythmScore = rhythmScore,
        overallScore = overallScore,
        recordingMode = recordingMode?.name ?: "UNKNOWN",
        analysisDate = analysisDate.time,
        stabilityDetails = com.google.gson.Gson().toJson(stabilityDetails),
        rhythmDetails = com.google.gson.Gson().toJson(rhythmDetails),
        deviceId = deviceId
    )
}

/**
 * Supabase DTO를 로컬 GaitScoreData 모델로 변환하는 함수
 */
fun GaitScoreDto.toDomain(): inu.appcenter.walkman.domain.model.GaitScoreData {
    val stabilityDetailsMap: Map<String, Double> = try {
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
        com.google.gson.Gson().fromJson(stabilityDetails, type)
    } catch (e: Exception) {
        emptyMap()
    }

    val rhythmDetailsMap: Map<String, Double> = try {
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
        com.google.gson.Gson().fromJson(rhythmDetails, type)
    } catch (e: Exception) {
        emptyMap()
    }

    return inu.appcenter.walkman.domain.model.GaitScoreData(
        sessionId = id ?: "",
        stabilityScore = stabilityScore,
        rhythmScore = rhythmScore,
        overallScore = overallScore,
        analysisDate = Date(analysisDate),
        stabilityDetails = stabilityDetailsMap,
        rhythmDetails = rhythmDetailsMap,
        recordingMode = try {
            inu.appcenter.walkman.domain.model.RecordingMode.valueOf(recordingMode)
        } catch (e: Exception) {
            null
        }
    )
}