package inu.appcenter.walkman.presentation.screen.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    mode: RecordingMode,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentReading by viewModel.currentReading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var recordingTimeElapsed by remember { mutableStateOf(0) }
    val recordingTimeMax = 30 // 최대 녹화 시간(초)

    // 녹화 시간 카운터
    LaunchedEffect(uiState.isRecording) {
        recordingTimeElapsed = 0
        if (uiState.isRecording) {
            while (recordingTimeElapsed < recordingTimeMax) {
                delay(1000) // 1초 대기
                recordingTimeElapsed++
            }
            // 시간이 다 되면 자동 중지
            if (recordingTimeElapsed >= recordingTimeMax) {
                viewModel.stopRecording()
            }
        }
    }

    // 성공 또는 오류 메시지 표시
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        if (uiState.successMessage != null || uiState.errorMessage != null) {
            delay(3000) // 3초 후 메시지 사라짐
            viewModel.clearMessages()
        }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(mode) {
                            RecordingMode.POCKET -> "주머니에 넣고 측정"
                            RecordingMode.VIDEO -> "영상 보면서 측정"
                            RecordingMode.TEXT -> "걸으면서 텍스트 입력"
                        },
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = WalkManColors.TextPrimary
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
            // 모드별 콘텐츠
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when(mode) {
                        RecordingMode.VIDEO -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "보행 안내 동영상",
                                    color = WalkManColors.TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "영상을 보고 자연스럽게 걸어주세요",
                                color = WalkManColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        RecordingMode.POCKET -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Person",
                                        tint = WalkManColors.TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "휴대폰을 주머니에 넣고\n30초간 걸어주세요",
                                        color = WalkManColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        RecordingMode.TEXT -> {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = { Text("입력해주세요", color = WalkManColors.TextSecondary) },
                                placeholder = { Text("걸으면서 자유롭게 입력해주세요", color = WalkManColors.TextSecondary) },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    unfocusedTextColor = WalkManColors.TextPrimary,
                                    focusedTextColor = WalkManColors.TextPrimary,
                                    cursorColor = WalkManColors.Primary,
                                    focusedBorderColor = WalkManColors.Primary,
                                    unfocusedBorderColor = WalkManColors.TextSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                maxLines = 8
                            )
                        }
                    }
                }
            }

            // 센서 데이터 표시 (디버깅용, 실제 앱에서는 숨김 가능)
            if (currentReading != null && uiState.isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "센서 데이터",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = WalkManColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val reading = currentReading!!

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("가속도계", fontWeight = FontWeight.Bold, color = WalkManColors.TextPrimary)
                                Text("X: ${String.format("%.2f", reading.accX)} m/s²", color = WalkManColors.TextSecondary)
                                Text("Y: ${String.format("%.2f", reading.accY)} m/s²", color = WalkManColors.TextSecondary)
                                Text("Z: ${String.format("%.2f", reading.accZ)} m/s²", color = WalkManColors.TextSecondary)
                            }

                            Column {
                                Text("자이로스코프", fontWeight = FontWeight.Bold, color = WalkManColors.TextPrimary)
                                Text("X: ${String.format("%.2f", reading.gyroX)} rad/s", color = WalkManColors.TextSecondary)
                                Text("Y: ${String.format("%.2f", reading.gyroY)} rad/s", color = WalkManColors.TextSecondary)
                                Text("Z: ${String.format("%.2f", reading.gyroZ)} rad/s", color = WalkManColors.TextSecondary)
                            }
                        }
                    }
                }
            }

            // 녹화 진행 상태
            if (uiState.isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { recordingTimeElapsed.toFloat() / recordingTimeMax.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = WalkManColors.Primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "데이터 수집 중입니다",
                                color = WalkManColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "남은 시간: ${recordingTimeMax - recordingTimeElapsed}초",
                                color = WalkManColors.Primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 성공/오류 메시지
            if (uiState.successMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Success.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = uiState.successMessage!!,
                        color = WalkManColors.Success,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = WalkManColors.Error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 측정 버튼
            Button(
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording(mode)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRecording) WalkManColors.Error else WalkManColors.Primary,
                    disabledContainerColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isUploading
            ) {
                Text(
                    text = if (uiState.isRecording) "측정 중지" else "측정 시작",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 상태 표시
            if (uiState.isRecording) {
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
                        "데이터 기록 중...",
                        color = WalkManColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.isUploading) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = WalkManColors.Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = WalkManColors.Primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "데이터 업로드 중...",
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}