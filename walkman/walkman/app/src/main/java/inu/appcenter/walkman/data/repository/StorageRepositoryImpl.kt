// data/repository/StorageRepositoryImpl.kt
package inu.appcenter.walkman.data.repository

import android.content.Context
import inu.appcenter.walkman.data.datasource.DriveServiceHelper
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.RecordingSession
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

    /**
     * UserProfile을 사용하여 CSV 파일 생성
     */
    override suspend fun createCsvFileWithProfile(
        session: RecordingSession,
        userProfile: UserProfile
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        // 파일명 형식: MBTI_모드_키_날짜
        val mbtiPrefix = userProfile.mbti ?: "None"

        // 모드를 영어로 변환
        val modeText = when(session.mode) {
            RecordingMode.POCKET -> "Pocket"
            RecordingMode.VIDEO -> "Look"
            RecordingMode.TEXT -> "Text"
        }

        val height = userProfile.height.toString().replace(" ", "")

        // 파일명 형식: MBTI_모드_키_날짜_프로필ID
        val fileName = "${mbtiPrefix}_${modeText}_${height}_${timestamp}_${userProfile.id}.csv"
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

    /**
     * 기본 파일 업로드
     */
    override suspend fun uploadFileToDrive(file: File): String {
        return driveServiceHelper.uploadFile(file, getMimeType(file))
    }

    /**
     * 프로필 ID와 함께 파일을 드라이브에 업로드
     */
    override suspend fun uploadFileToDrive(file: File, profileId: String): String {
        // 1. 메타데이터 방식 (기본)
        return driveServiceHelper.uploadFile(file, getMimeType(file))

        // 2. 프로필별 폴더 방식 (선택적)
        // return driveServiceHelper.uploadFileToProfileFolder(file, getMimeType(file), profileId)
    }

    /**
     * 파일 유형 확인
     */
    private fun getMimeType(file: File): String {
        return when {
            file.name.endsWith(".csv") -> "text/csv"
            file.name.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}