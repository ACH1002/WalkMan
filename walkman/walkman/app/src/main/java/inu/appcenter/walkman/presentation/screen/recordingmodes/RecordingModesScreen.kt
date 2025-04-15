package inu.appcenter.walkman.presentation.screen.recordingmodes

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.domain.model.NetworkStatus
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.presentation.components.NetworkStatusCard
import inu.appcenter.walkman.presentation.screen.recordingmodes.components.CycleProgressCard
import inu.appcenter.walkman.presentation.screen.recordingmodes.components.ModeCard
import inu.appcenter.walkman.presentation.screen.recordingmodes.components.UserInfoCard
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingModesScreen(
    viewModel: RecordingViewModel,
    profileGaitViewModel: ProfileGaitViewModel = hiltViewModel(),
    onNavigateToRecording: (RecordingMode) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToUserInfo: (UserProfile) -> Unit,
    onBackPressed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val profileUiState by profileGaitViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 네트워크 연결 상태에 따라 측정 시작 가능 여부 결정
    val canStartRecording = networkState is NetworkStatus.Connected

    val scrollState = rememberScrollState()

    // 뒤로가기 처리
    BackHandler {
        onBackPressed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.recording_modes_title),
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // 뒤로가기 버튼 추가
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back),
                            tint = WalkManColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                )
            )
        },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 온보딩 진행 상태 표시
//            Row(
//                modifier = Modifier.padding(vertical = 16.dp),
//                horizontalArrangement = Arrangement.Center
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(12.dp)
//                        .background(Color.LightGray, shape = RoundedCornerShape(6.dp))
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Box(
//                    modifier = Modifier
//                        .size(12.dp)
//                        .background(Color.LightGray, shape = RoundedCornerShape(6.dp))
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Box(
//                    modifier = Modifier
//                        .size(12.dp)
//                        .background(WalkManColors.Primary, shape = RoundedCornerShape(6.dp))
//                )
//            }
            // 네트워크 상태 표시
            NetworkStatusCard(
                networkState = networkState,
                showConnectButton = networkState is NetworkStatus.Disconnected,
                onConnectClick = {
                    // 네트워크 설정으로 이동
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // 사용자 정보 카드
            Spacer(modifier = Modifier.height(8.dp))
            // 사용자 정보 카드
            UserInfoCard(
                userProfile = profileUiState.selectedProfile,
                onEditClick = { profileUiState.selectedProfile?.let { onNavigateToUserInfo(it) } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val cycleState by viewModel.cycleState.collectAsState()
            CycleProgressCard(
                cycleState = cycleState,
                completedModes = uiState.completedModes
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 측정 완료 상태 표시
            Text(
                text = stringResource(id = R.string.recording_completed, uiState.completedModes.size),
                style = MaterialTheme.typography.titleMedium,
                color = WalkManColors.TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 영상 보기 모드
            ModeCard(
                title = stringResource(id = R.string.recording_video_mode),
                description = stringResource(id = R.string.recording_video_desc),
                isCompleted = viewModel.isModeCompleted(RecordingMode.VIDEO),
                onClick = { onNavigateToRecording(RecordingMode.VIDEO) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 주머니 모드
            ModeCard(
                title = stringResource(id = R.string.recording_pocket_mode),
                description = stringResource(id = R.string.recording_pocket_desc),
                isCompleted = viewModel.isModeCompleted(RecordingMode.POCKET),
                onClick = { onNavigateToRecording(RecordingMode.POCKET) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 텍스트 입력 모드
            ModeCard(
                title = stringResource(id = R.string.recording_text_mode),
                description = stringResource(id = R.string.recording_text_desc),
                isCompleted = viewModel.isModeCompleted(RecordingMode.TEXT),
                onClick = { onNavigateToRecording(RecordingMode.TEXT) }
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))


            // 네트워크 연결이 안 되어 있을 경우 안내 메시지
            if (!canStartRecording) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.connect_network_to_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.Error,
                    fontSize = 14.sp
                )
            }

            // 현재 기록 중이면 중지 버튼 표시
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.stopRecording() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalkManColors.Error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_stop_recording),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = WalkManColors.Error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(WalkManColors.Error, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.recording_in_progress),
                        color = WalkManColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



