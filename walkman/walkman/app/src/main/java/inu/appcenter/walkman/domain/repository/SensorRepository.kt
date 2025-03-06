package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun startRecording(mode: RecordingMode): Flow<RecordingSession>
    suspend fun stopRecording(): RecordingSession
    fun getCurrentReadings(): Flow<SensorReading>
    suspend fun saveReading(reading: SensorReading)
    fun clearReadings()
    fun isRecording(): Flow<Boolean>
    suspend fun cancelRecording()
}