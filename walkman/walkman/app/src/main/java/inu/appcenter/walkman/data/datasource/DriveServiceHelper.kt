// data/datasource/DriveServiceHelper.kt
package inu.appcenter.walkman.data.datasource

import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /**
     * 파일을 구글 드라이브에 업로드
     */
    suspend fun uploadFile(file: java.io.File, mimeType: String): String = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = File()
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

    /**
     * 메타데이터와 함께 파일을 업로드
     */
    suspend fun uploadFileWithMetadata(
        file: java.io.File,
        mimeType: String,
        metadata: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = File()
                .setName(file.name)
                .setParents(listOf(folderId))

            // 사용자 정의 속성 추가
            if (metadata.isNotEmpty()) {
                fileMetadata.properties = metadata
            }

            val mediaContent = FileContent(mimeType, file)

            val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink, properties")
                .execute()

            return@withContext uploadedFile.id
        } catch (e: Exception) {
            throw IOException("파일 업로드 실패: ${e.message}")
        }
    }

    /**
     * 특정 프로필 ID에 대한 폴더 생성 또는 가져오기
     */
    suspend fun getOrCreateProfileFolder(profileId: String): String = withContext(Dispatchers.IO) {
        try {
            // 프로필 ID로 폴더 검색
            val query = "mimeType='application/vnd.google-apps.folder' and name='$profileId' and '$folderId' in parents"
            val folderList = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            // 폴더가 이미 존재하면 반환
            if (folderList.files.isNotEmpty()) {
                return@withContext folderList.files.first().id
            }

            // 폴더가 없으면 새로 생성
            val folderMetadata = File()
                .setName(profileId)
                .setMimeType("application/vnd.google-apps.folder")
                .setParents(listOf(folderId))

            val folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute()

            return@withContext folder.id
        } catch (e: Exception) {
            throw IOException("프로필 폴더 생성 실패: ${e.message}")
        }
    }

    /**
     * 특정 프로필 폴더에 파일 업로드
     */
    suspend fun uploadFileToProfileFolder(
        file: java.io.File,
        mimeType: String,
        profileId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // 프로필 폴더 가져오기 또는 생성
            val profileFolderId = getOrCreateProfileFolder(profileId)

            val fileMetadata = File()
                .setName(file.name)
                .setParents(listOf(profileFolderId))
                .setProperties(mapOf("profileId" to profileId))

            val mediaContent = FileContent(mimeType, file)

            val uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()

            return@withContext uploadedFile.id
        } catch (e: Exception) {
            throw IOException("프로필 폴더에 파일 업로드 실패: ${e.message}")
        }
    }
}