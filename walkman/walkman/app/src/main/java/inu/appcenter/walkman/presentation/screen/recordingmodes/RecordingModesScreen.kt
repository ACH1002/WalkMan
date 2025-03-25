package inu.appcenter.walkman.presentation.screen.recordingmodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingModesScreen(
    viewModel: RecordingViewModel,
    onNavigateToRecording: (RecordingMode) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToUserInfo: () -> Unit,
    onBackPressed: () -> Unit = {},
    userInfo: UserInfo? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    // 모든 측정이 완료되면 결과 페이지로 자동 이동
    LaunchedEffect(uiState.allModesCompleted) {
        if (uiState.allModesCompleted) {
            onNavigateToResults()
        }
    }

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 온보딩 진행 상태 표시
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(WalkManColors.Primary, shape = RoundedCornerShape(6.dp))
                )
            }

            // 사용자 정보 카드
            UserInfoCard(
                userInfo = userInfo,
                onEditClick = onNavigateToUserInfo
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.recording_modes_title),
                style = MaterialTheme.typography.headlineSmall,
                color = WalkManColors.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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

            // 모든 측정이 완료된 경우에만 활성화되는 결과 페이지 이동 버튼
            Button(
                onClick = onNavigateToResults,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalkManColors.Primary,
                    disabledContainerColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = uiState.completedModes.size == 3 // 모든 모드가 완료된 경우에만 활성화
            ) {
                Text(
                    text = stringResource(id = R.string.btn_complete_recording),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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

@Composable
fun UserInfoCard(
    userInfo: UserInfo?,
    onEditClick: () -> Unit
) {
    // 현재 언어에 맞게 성별 텍스트 변환
    val genderText = when(userInfo?.gender) {
        stringResource(id = R.string.gender_male) -> stringResource(id = R.string.gender_male)
        stringResource(id = R.string.gender_female) -> stringResource(id = R.string.gender_female)
        "남성" -> stringResource(id = R.string.gender_male)
        "여성" -> stringResource(id = R.string.gender_female)
        "Male" -> stringResource(id = R.string.gender_male)
        "Female" -> stringResource(id = R.string.gender_female)
        else -> userInfo?.gender ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 사용자 아이콘
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(id = R.string.profile),
                tint = WalkManColors.Primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 사용자 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userInfo?.name ?: stringResource(id = R.string.default_user),
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Wrap을 구현하는 FlowRow 대신 텍스트를 개별적으로 나열
                // 첫 줄: 성별
                if (!userInfo?.gender.isNullOrBlank()) {
                    Text(
                        text = genderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextSecondary
                    )
                }

                // 두 번째 줄: 키와 몸무게 나란히
                Row(
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (!userInfo?.height.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.profile_height, userInfo?.height ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WalkManColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!userInfo?.weight.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.profile_weight, userInfo?.weight ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WalkManColors.TextSecondary
                        )
                    }
                }
            }

            // 편집 버튼
            IconButton(
                onClick = onEditClick
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_profile),
                    tint = WalkManColors.Primary
                )
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextSecondary
                )
            }

            // 완료된 경우 체크 아이콘 표시
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(id = R.string.recording_completed_item),
                    tint = WalkManColors.Success,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}