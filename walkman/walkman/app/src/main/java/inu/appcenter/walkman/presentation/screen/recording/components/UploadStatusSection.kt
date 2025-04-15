package inu.appcenter.walkman.presentation.screen.recording.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

@Composable
fun UploadStatusSection(
    viewModel: RecordingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val pendingUploadManager = viewModel.getPendingUploadManager()
    val uploadProgress by pendingUploadManager.uploadProgress.collectAsState()
    val isUploading by pendingUploadManager.isUploading.collectAsState()

    // 향상된 업로드 상태 인디케이터 사용
    EnhancedUploadStatusIndicator(
        uploadState = uploadState,
        uploadProgress = uploadProgress,
        isPendingUpload = uiState.pendingUpload && isUploading
    )
}


