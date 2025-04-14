// domain/repository/StorageRepository.kt
package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.domain.model.RecordingSession
import java.io.File

interface StorageRepository {
    /**
     * 사용자 프로필로 CSV 파일 생성
     */
    suspend fun createCsvFileWithProfile(
        session: RecordingSession,
        userProfile: UserProfile
    ): File

    /**
     * 파일을 드라이브에 업로드
     */
    suspend fun uploadFileToDrive(file: File): String

    /**
     * 프로필 ID와 함께 파일을 드라이브에 업로드
     */
    suspend fun uploadFileToDrive(file: File, profileId: String): String
}