package inu.appcenter.walkman.data.repository

import android.content.Context
import inu.appcenter.walkman.data.datasource.DriveServiceHelper
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.domain.repository.StorageRepository
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val context: Context,
    private val driveServiceHelper: DriveServiceHelper
) : StorageRepository {

    override suspend fun createCsvFile(
        session: RecordingSession,
        userInfo: UserInfo
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        // 새로운 파일명 형식: MBTI_모드_키_날짜
        // MBTI가 없으면 "None"으로 표시
        val mbtiPrefix = if (userInfo.mbti.isBlank()) "None" else userInfo.mbti

        // 모드를 영어로 변환
        val modeText = when(session.mode) {
            RecordingMode.POCKET -> "Pocket"
            RecordingMode.VIDEO -> "Look"
            RecordingMode.TEXT -> "Text"
        }

        val height = userInfo.height.replace(" ", "")

        val fileName = "${mbtiPrefix}_${modeText}_${height}_$timestamp.csv"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            // CSV 헤더 작성
            writer.append("Time(s),AccX,AccY,AccZ,GyroX,GyroY,GyroZ,MagX,MagY,MagZ,Latitude,Longitude\n")

            // 센서 데이터 작성
            for (reading in session.readings) {
                writer.append("${reading.timeSeconds},${reading.accX},${reading.accY},${reading.accZ},")
                writer.append("${reading.gyroX},${reading.gyroY},${reading.gyroZ},")
                writer.append("${reading.magX},${reading.magY},${reading.magZ},")
                writer.append("${reading.latitude},${reading.longitude}\n")
            }

            writer.flush()
        }

        return file
    }

    override suspend fun uploadFileToDrive(file: File): String {
        return driveServiceHelper.uploadFile(file, getMimeType(file))
    }



    private fun getMimeType(file: File): String {
        return when {
            file.name.endsWith(".csv") -> "text/csv"
            file.name.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}