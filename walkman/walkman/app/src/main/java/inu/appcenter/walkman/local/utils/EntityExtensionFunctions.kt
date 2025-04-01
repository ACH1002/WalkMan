package inu.appcenter.walkman.local.utils

import inu.appcenter.walkman.domain.model.GaitScoreData
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.SensorReading
import inu.appcenter.walkman.local.entity.GaitAnalysisEntity
import inu.appcenter.walkman.local.entity.RecordingSessionEntity
import inu.appcenter.walkman.local.entity.SensorReadingEntity
import inu.appcenter.walkman.local.entity.SessionWithReadings
import java.util.Date

/**
 * 엔티티 변환 확장 함수
 */
fun RecordingSessionEntity.toDomainModel(): inu.appcenter.walkman.domain.model.RecordingSession {
    return inu.appcenter.walkman.domain.model.RecordingSession(
        id = id,
        userId = userId,
        mode = RecordingMode.valueOf(mode),
        startTime = startTime,
        endTime = endTime,
        readings = emptyList() // 기본 변환에서는 빈 리스트, 필요시 별도 로직으로 채움
    )
}

fun SessionWithReadings.toDomainModel(): inu.appcenter.walkman.domain.model.RecordingSession {
    return inu.appcenter.walkman.domain.model.RecordingSession(
        id = session.id,
        userId = session.userId,
        mode = RecordingMode.valueOf(session.mode),
        startTime = session.startTime,
        endTime = session.endTime,
        readings = readings.map { it.toDomainModel() }
    )
}

fun SensorReadingEntity.toDomainModel(): SensorReading {
    return SensorReading(
        timestamp = timestamp,
        timeSeconds = timeSeconds,
        accX = accX,
        accY = accY,
        accZ = accZ,
        gyroX = gyroX,
        gyroY = gyroY,
        gyroZ = gyroZ,
        magX = magX,
        magY = magY,
        magZ = magZ,
        latitude = latitude,
        longitude = longitude
    )
}

fun RecordingSession.toEntity(): RecordingSessionEntity {
    return RecordingSessionEntity(
        id = id,
        userId = userId,
        mode = mode.name,
        startTime = startTime,
        endTime = endTime
    )
}

fun SensorReading.toEntity(sessionId: String): SensorReadingEntity {
    return SensorReadingEntity(
        sessionId = sessionId,
        timestamp = timestamp,
        timeSeconds = timeSeconds,
        accX = accX,
        accY = accY,
        accZ = accZ,
        gyroX = gyroX,
        gyroY = gyroY,
        gyroZ = gyroZ,
        magX = magX,
        magY = magY,
        magZ = magZ,
        latitude = latitude,
        longitude = longitude
    )
}

fun GaitScoreData.toEntity(): GaitAnalysisEntity {
    return GaitAnalysisEntity(
        sessionId = sessionId,
        stabilityScore = stabilityScore,
        rhythmScore = rhythmScore,
        overallScore = overallScore,
        analysisDate = analysisDate.time,
        stabilityDetails = stabilityDetailsToJson(),
        rhythmDetails = rhythmDetailsToJson()
    )
}

// Map을 JSON 문자열로 변환하는 확장 함수
fun GaitScoreData.stabilityDetailsToJson(): String {
    return com.google.gson.Gson().toJson(stabilityDetails)
}

fun GaitScoreData.rhythmDetailsToJson(): String {
    return com.google.gson.Gson().toJson(rhythmDetails)
}

fun GaitAnalysisEntity.toDomainModel(): GaitScoreData {
    return GaitScoreData(
        sessionId = sessionId,
        stabilityScore = stabilityScore,
        rhythmScore = rhythmScore,
        overallScore = overallScore,
        analysisDate = Date(analysisDate),
        stabilityDetails = jsonToStabilityDetails(),
        rhythmDetails = jsonToRhythmDetails(),
        recordingMode = null // 필요 시 세션에서 추출
    )
}

// JSON 문자열을 Map으로 변환하는 확장 함수
fun GaitAnalysisEntity.jsonToStabilityDetails(): Map<String, Double> {
    val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
    return com.google.gson.Gson().fromJson(stabilityDetails, type)
}

fun GaitAnalysisEntity.jsonToRhythmDetails(): Map<String, Double> {
    val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type
    return com.google.gson.Gson().fromJson(rhythmDetails, type)
}