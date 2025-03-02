package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.model.UserInfo
import java.io.File

interface StorageRepository {
    suspend fun createCsvFile(
        session: RecordingSession,
        userInfo: UserInfo
    ): File

    suspend fun uploadFileToDrive(file: File): String
}