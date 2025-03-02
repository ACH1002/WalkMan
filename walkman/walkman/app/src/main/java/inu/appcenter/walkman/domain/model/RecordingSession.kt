package inu.appcenter.walkman.domain.model

data class RecordingSession(
    val id: String = "",
    val userId: String = "",
    val mode: RecordingMode,
    val startTime: Long,
    val endTime: Long? = null,
    val readings: List<SensorReading> = emptyList()
)
