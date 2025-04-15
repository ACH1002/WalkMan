// data/repository/PendingUploadManager.kt
package inu.appcenter.walkman.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import inu.appcenter.walkman.domain.model.NetworkStatus
import inu.appcenter.walkman.domain.model.RecordingSession
import inu.appcenter.walkman.domain.repository.NetworkMonitor
import inu.appcenter.walkman.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 네트워크 연결이 없을 때 업로드 대기 상태를 관리하는 클래스
 */
@Singleton
class PendingUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val storageRepository: StorageRepository
) {
    companion object {
        private const val TAG = "PendingUploadManager"
        private const val PENDING_UPLOADS_FILE = "pending_uploads.dat"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingUploads = mutableListOf<PendingUpload>()

    init {
        // 앱 시작 시 저장된 대기 업로드 불러오기
        loadPendingUploads()

        // 네트워크 상태 모니터링
        coroutineScope.launch {
            networkMonitor.networkStatus.collect { status ->
                if (status is NetworkStatus.Connected) {
                    // 네트워크 연결 시 대기 중인 업로드 처리
                    processPendingUploads()
                }
            }
        }
    }

    /**
     * 세션을 대기 업로드 목록에 추가 (프로필 ID 포함)
     * @param session 업로드할 세션
     * @param localFile 로컬에 저장된 CSV 파일
     * @param profileId 사용자 프로필 ID
     */
    fun addPendingUpload(session: RecordingSession, localFile: File, profileId: String) {
        val pendingUpload = PendingUpload(
            sessionId = session.id,
            localFilePath = localFile.absolutePath,
            profileId = profileId,
            timestamp = System.currentTimeMillis()
        )

        pendingUploads.add(pendingUpload)
        savePendingUploads()

        Log.d(TAG, "Added pending upload: ${pendingUpload.sessionId} for profile: $profileId")
    }

    /**
     * 모든 대기 업로드 처리
     */
    private fun processPendingUploads() {
        if (pendingUploads.isEmpty()) return

        coroutineScope.launch {
            Log.d(TAG, "Processing ${pendingUploads.size} pending uploads")

            // 업로드 가능한 네트워크 상태인지 확인
            val isUploadAllowed = networkMonitor.isUploadAllowed().first()
            if (!isUploadAllowed) {
                Log.d(TAG, "Upload not allowed by network settings")
                return@launch
            }

            // 실제 인터넷 연결 가능 여부 확인
            val isConnected = networkMonitor.isInternetReachable()
            if (!isConnected) {
                Log.d(TAG, "Internet not reachable")
                return@launch
            }

            // 각 대기 업로드를 처리
            val iterator = pendingUploads.iterator()
            while (iterator.hasNext()) {
                val upload = iterator.next()

                try {
                    val file = File(upload.localFilePath)
                    if (!file.exists() || !file.isFile) {
                        Log.e(TAG, "File not found: ${upload.localFilePath}")
                        iterator.remove()
                        continue
                    }

                    // 프로필 ID가 있으면 해당 프로필로 업로드, 없으면 기본 메서드 사용
                    val fileId = if (upload.profileId.isNotEmpty()) {
                        storageRepository.uploadFileToDrive(file)
                    } else {
                        storageRepository.uploadFileToDrive(file)
                    }

                    Log.d(
                        TAG,
                        "Successfully uploaded file to Drive: $fileId for profile: ${upload.profileId}"
                    )

                    // 성공적으로 업로드된 항목 제거
                    iterator.remove()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload file: ${upload.localFilePath}", e)
                    // 실패한 업로드는 다음 시도에서 다시 시도
                }
            }

            // 변경된 대기 업로드 목록 저장
            savePendingUploads()
        }
    }

    /**
     * 대기 업로드 목록이 비어있는지 확인
     * @return 목록이 비어있으면 true
     */
    fun isPendingUploadEmpty(): Boolean {
        return pendingUploads.isEmpty()
    }

    /**
     * 대기 중인 업로드 개수 반환
     * @return 대기 중인 업로드 개수
     */
    fun getPendingUploadCount(): Int {
        return pendingUploads.size
    }

    /**
     * 대기 업로드 목록 불러오기
     */
    @Suppress("UNCHECKED_CAST")
    private fun loadPendingUploads() {
        val file = File(context.filesDir, PENDING_UPLOADS_FILE)
        if (!file.exists()) return

        try {
            ObjectInputStream(FileInputStream(file)).use { stream ->
                val loadedList = stream.readObject() as? List<PendingUpload>
                pendingUploads.clear()
                loadedList?.let { pendingUploads.addAll(it) }
            }
            Log.d(TAG, "Loaded ${pendingUploads.size} pending uploads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pending uploads", e)
            // 로드 실패 시 파일 삭제
            file.delete()
        }
    }

    /**
     * 대기 업로드 목록 저장
     */
    private fun savePendingUploads() {
        val file = File(context.filesDir, PENDING_UPLOADS_FILE)

        try {
            ObjectOutputStream(FileOutputStream(file)).use { stream ->
                stream.writeObject(ArrayList(pendingUploads))
            }
            Log.d(TAG, "Saved ${pendingUploads.size} pending uploads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save pending uploads", e)
        }
    }

    /**
     * 대기 업로드 정보를 나타내는 직렬화 가능한 데이터 클래스
     */
    data class PendingUpload(
        val sessionId: String,
        val localFilePath: String,
        val profileId: String = "", // 프로필 ID 필드 추가
        val timestamp: Long
    ) : Serializable
}