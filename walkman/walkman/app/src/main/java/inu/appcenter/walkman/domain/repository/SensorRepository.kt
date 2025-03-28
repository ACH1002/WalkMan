package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

/**
 * 센서 저장소 인터페이스 - 걷기 감지와 센서 데이터 수집 기능 모두 포함
 */
interface SensorRepository {
    /**
     * 현재 걷기 상태 관찰
     */
    fun isWalking(): Flow<Boolean>

    /**
     * 걷기 감지 시작
     */
    fun startWalkingDetection()

    /**
     * 걷기 감지 종료
     */
    fun stopWalkingDetection()

    /**
     * 센서 데이터 수집 중인지 여부 (기존 isRecording)
     */
    fun isRecording(): Flow<Boolean>

    /**
     * 현재 센서 데이터 값 관찰
     */
    fun getCurrentReadings(): Flow<SensorReading>

    /**
     * 특정 모드로 센서 데이터 수집 시작
     */
    fun startRecording(mode: RecordingMode): Flow<RecordingSession>

    /**
     * 센서 데이터 수집 중지 및 세션 반환
     */
    suspend fun stopRecording(): RecordingSession

    /**
     * 센서 데이터 저장
     */
    suspend fun saveReading(reading: SensorReading)

    /**
     * 센서 데이터 초기화
     */
    fun clearReadings()

    /**
     * 데이터 수집 취소
     */
    suspend fun cancelRecording()
}