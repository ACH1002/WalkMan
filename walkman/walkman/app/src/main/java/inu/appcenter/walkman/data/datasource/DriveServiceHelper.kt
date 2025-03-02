package inu.appcenter.walkman.data.datasource


import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveServiceHelper @Inject constructor(
    private val context: Context,
    private val folderId: String
) {

    private val drive: Drive by lazy { createDriveService() }

    private fun createDriveService(): Drive {
        try {
            // 서비스 계정 JSON 키 파일을 assets 폴더에서 로드
            val credentialStream = context.assets.open("service-account-key.json")

            val credential = GoogleCredential.fromStream(credentialStream)
                .createScoped(listOf(DriveScopes.DRIVE_FILE))

            return Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("GAITX")
                .build()
        } catch (e: Exception) {
            throw IOException("Drive 서비스 초기화 실패: ${e.message}")
        }
    }

    suspend fun uploadFile(file: File, mimeType: String): String = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = com.google.api.services.drive.model.File()
                .setName(file.name)
                .setParents(listOf(folderId))

            val mediaContent = FileContent(mimeType, file)

            val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()

            return@withContext uploadedFile.id
        } catch (e: Exception) {
            throw IOException("파일 업로드 실패: ${e.message}")
        }
    }
}
